package com.mimetis.dotmim.sync.interceptors

interface ISyncInterceptor : AutoCloseable {
}

interface ISyncInterceptor1<T> : ISyncInterceptor {
    fun run(args: T)
}

interface ISyncInterceptor2 : ISyncInterceptor {
    fun run(args: Any)
}
