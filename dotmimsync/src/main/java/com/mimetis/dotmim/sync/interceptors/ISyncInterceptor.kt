package com.mimetis.dotmim.sync.interceptors

import java.io.Closeable

interface ISyncInterceptor : Closeable {
}

interface ISyncInterceptor1<T> : ISyncInterceptor {
    fun run(args: T)
}

interface ISyncInterceptor2 : ISyncInterceptor {
    fun run(args: Any)
}
