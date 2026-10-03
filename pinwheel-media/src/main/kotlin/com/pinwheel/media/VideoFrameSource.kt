package com.pinwheel.media

import com.pinwheel.core.RgbaFrame
import com.pinwheel.core.model.StudioProject
import java.net.URI
import java.nio.file.Path
import java.time.Duration
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

enum class RenderMode { PREVIEW, EXPORT }
interface VideoFrameEvaluator : AutoCloseable {
    val project: StudioProject
    fun render(timeUs: Long): RgbaFrame
    fun resetHistory()
}

fun localMediaPath(uri: String): Path {
    val parsed = URI(uri)
    require(parsed.scheme == null || parsed.scheme.equals("file", true)) { "Import or relink media into the Windows project first: $uri" }
    return if (parsed.scheme == null) Path.of(uri) else Path.of(parsed)
}

/** Small LRU of persistent video-only cursors, including still pixels and one look-ahead frame. */
class VideoFrameSource(private val factory: MediaDecoderFactory, private val config: DecodeConfig,
    private val maxOpen: Int = 8) : AutoCloseable {
    init { require(config.decodeVideo && !config.decodeAudio && maxOpen in 1..32) }
    private class Cursor(val uri:String,val decoder: MediaDecoder) {
        var current: DecodedVideo? = null
        var next: DecodedVideo? = null
        var requested: Long = -1
    }
    private val cursors = LinkedHashMap<String, Cursor>(16, .75f, true)
    private val preparing = LinkedHashMap<String, CompletableFuture<Cursor>>()
    private val closed = AtomicBoolean()
    private val cleanupFailures = ConcurrentLinkedQueue<Throwable>()
    private val preparation = Executors.newSingleThreadExecutor { task -> Thread(task,"pinwheel-video-prefetch").apply { isDaemon=true } }
    val descriptions get() = cursors.mapValues { it.value.decoder.description }
    fun description(uri: String,identity:String=uri): MediaDescription = cursor(uri,identity).decoder.description
    /** At most two pending inputs, counted inside maxOpen. Existing active inputs are not evicted. */
    fun prefetch(uri:String,sourceTimeUs:Long=0,identity:String=uri) {
        check(!closed.get());require(sourceTimeUs>=0)
        val key="$identity\u0000$uri"
        if(key in cursors || key in preparing || preparing.size>=minOf(2,maxOpen-1) || cursors.size+preparing.size>=maxOpen)return
        val future=CompletableFuture<Cursor>();preparing[key]=future
        preparation.execute {
            var decoder:MediaDecoder?=null
            try {
                if(closed.get())return@execute
                decoder=factory.open(localMediaPath(uri),config)
                if(closed.get() || future.isCancelled) { decoder.close();decoder=null;return@execute }
                val cursor=Cursor(uri,decoder)
                if(!decoder.description.still)decoder.seek(sourceTimeUs)
                cursor.current=requireNotNull(decoder.pollVideo(Duration.ofSeconds(5))) { "Prefetched input has no frame: $uri" }
                cursor.requested=sourceTimeUs
                if(!future.complete(cursor)) { decoder.close();decoder=null }
            } catch(failure:Throwable) {
                try { decoder?.close() } catch(cleanup:Throwable) { failure.addSuppressed(cleanup);cleanupFailures.add(cleanup) }
                future.completeExceptionally(failure)
            }
        }
    }
    private fun cursor(uri: String,identity:String): Cursor {
        check(!closed.get())
        val still=cursors.entries.firstOrNull { it.value.uri==uri && it.value.decoder.description.still }?.key
        val key=still ?: "$identity\u0000$uri"
        return cursors.getOrPut(key) {
            preparing[key]?.let { future ->
                // Retain the future until acquisition succeeds, including timeout/failure cleanup.
                val prepared=future.get(5,TimeUnit.SECONDS);preparing.remove(key);return@getOrPut prepared
            }
            while(cursors.size+preparing.size>=maxOpen) { val oldest=cursors.keys.first();cursors.remove(oldest)!!.decoder.close() }
            Cursor(uri,factory.open(localMediaPath(uri),config))
        }
    }
    fun frame(uri: String, sourceTimeUs: Long, timeout: Duration = Duration.ofSeconds(5),identity:String=uri): RgbaFrame {
        require(sourceTimeUs >= 0)
        val c = cursor(uri,identity)
        if (c.decoder.description.still) {
            c.current?.let { return it.pixels }
            c.current = requireNotNull(c.decoder.pollVideo(timeout)) { "Still has no frame: $uri" }
            return c.current!!.pixels
        }
        if (c.current != null && sourceTimeUs >= c.requested && sourceTimeUs <= c.current!!.presentationTimeUs) { c.requested=sourceTimeUs;return c.current!!.pixels }
        if (c.requested < 0 || sourceTimeUs < c.requested || sourceTimeUs - c.requested > 250_000) {
            c.decoder.seek(sourceTimeUs); c.current = null; c.next = null
        }
        c.requested = sourceTimeUs
        val deadline = System.nanoTime() + timeout.toNanos()
        while (true) {
            val next = c.next ?: c.decoder.pollVideo(Duration.ofNanos((deadline - System.nanoTime()).coerceAtLeast(0)))
            c.next = next
            if (next == null) break
            if (c.current != null && next.presentationTimeUs > sourceTimeUs) break
            c.current = next; c.next = null
            if (next.presentationTimeUs >= sourceTimeUs) break
        }
        return requireNotNull(c.current) { "No frame at $sourceTimeUs us: $uri" }.pixels
    }
    fun reset() { cursors.values.forEach { it.requested = -1; it.current = null; it.next = null } }
    override fun close() {
        if(!closed.compareAndSet(false,true))return
        preparing.values.forEach { future ->
            if(!future.cancel(false) && !future.isCompletedExceptionally)future.getNow(null)?.let { try { it.decoder.close() } catch(failure:Throwable) { cleanupFailures.add(failure) } }
        }
        preparing.clear();preparation.shutdownNow()
        cursors.values.forEach { try { it.decoder.close() } catch(failure:Throwable) { cleanupFailures.add(failure) } };cursors.clear()
        check(preparation.awaitTermination(10,TimeUnit.SECONDS)) { "Video prefetch did not stop" }
        cleanupFailures.peek()?.let { throw IllegalStateException("Video decoder cleanup failed",it) }
    }
}
