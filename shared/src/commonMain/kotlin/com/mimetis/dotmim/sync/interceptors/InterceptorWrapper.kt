package com.mimetis.dotmim.sync.interceptors

class InterceptorWrapper<T> : ISyncInterceptor1<T> where T : Any {
    private var wrapper: ((T) -> Unit)? = Empty

    /**
     * Gets a boolean indicating if the interceptor is not used by user (ie : is Empty)
     */
    val isEmpty
        get() = wrapper == Empty

    /**
     * Set a Func<T, Task> as interceptor
     */
    fun set(run: (T) -> Unit) {
        wrapper = run
    }

    override fun run(args: T) {
        wrapper?.invoke(args)
    }

    override fun close() {
        wrapper = null
    }

    companion object {
        val Empty: ((Any) -> Unit) = { }
    }
}
