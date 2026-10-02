package com.pinwheel.render

/** Releases partial constructor allocations and normal renderer ownership in reverse order. */
internal class GpuResources : AutoCloseable {
    private val owned = java.util.ArrayDeque<AutoCloseable>()
    private var initialized = false
    fun <T : AutoCloseable> own(create: () -> T): T = try {
        create().also { owned.addLast(it) }
    } catch (failure: Throwable) {
        if (!initialized) try { close() } catch (cleanup: Throwable) { failure.addSuppressed(cleanup) }
        throw failure
    }
    fun initialized() { initialized = true }
    override fun close() {
        var failure: Throwable? = null
        while (owned.isNotEmpty()) try { owned.removeLast().close() } catch (e: Throwable) {
            if (failure == null) failure = e else failure.addSuppressed(e)
        }
        failure?.let { throw it }
    }
}
