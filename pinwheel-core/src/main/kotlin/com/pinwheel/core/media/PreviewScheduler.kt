package com.pinwheel.core.media

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.atomic.AtomicLong

/** Small interaction frames finish promptly; expensive refinements are cancelled on every new edit. */
class PreviewScheduler<Recipe, Frame>(
    private val interaction: suspend (Recipe) -> Frame,
    private val refinement: suspend (Recipe) -> Frame,
    private val discard: (Frame) -> Unit,
    private val settleMillis: Long = 180,
) {
    private data class Request<T>(val generation: Long, val recipe: T)

    suspend fun run(recipes: Flow<Recipe>, pending: () -> Unit = {}, failed: (Exception) -> Unit = {}, publish: (Frame, Boolean) -> Unit) = coroutineScope {
        val generation = AtomicLong(0)
        val refined = AtomicLong(-1)
        val interactionCompleted = AtomicLong(-1)
        val queued = Channel<Request<Recipe>>(Channel.CONFLATED)
        val renderer = Mutex()
        var refineJob: Job? = null
        val interactionJob = launch {
            for (request in queued) renderer.withLock {
                if (request.generation <= refined.get()) return@withLock
                try {
                    val frame = interaction(request.recipe)
                    if (request.generation > refined.get()) publish(frame, false) else discard(frame)
                } catch (e: CancellationException) { throw e }
                catch (e: Exception) { failed(e) }
                finally { interactionCompleted.set(request.generation) }
            }
        }
        try {
            recipes.collect { recipe ->
                val request = Request(generation.incrementAndGet(), recipe)
                pending()
                refineJob?.cancel()
                queued.trySend(request)
                refineJob = launch {
                    delay(settleMillis)
                    // A refinement must not jump ahead of the final queued small
                    // frame and leave the last slider position invisible for seconds.
                    while (interactionCompleted.get() < request.generation) {
                        if (request.generation != generation.get()) return@launch
                        delay(8)
                    }
                    renderer.withLock {
                        if (request.generation != generation.get()) return@withLock
                        try {
                            val frame = refinement(request.recipe)
                            if (request.generation == generation.get()) {
                                refined.set(request.generation)
                                publish(frame, true)
                            } else discard(frame)
                        } catch (e: CancellationException) { throw e }
                        catch (e: Exception) { failed(e) }
                    }
                }
            }
            // Finite flows (including tests) still deliver their final refinement.
            refineJob?.join()
        } finally {
            queued.close()
            interactionJob.cancelAndJoin()
            refineJob?.cancelAndJoin()
        }
    }
}
