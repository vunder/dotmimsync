package com.mimetis.dotmim.sync.interceptors

import com.mimetis.dotmim.sync.args.ProgressArgs
import java.util.concurrent.ConcurrentHashMap
import kotlin.reflect.KType
import kotlin.reflect.typeOf

class Interceptors {
    @PublishedApi
    internal val dictionary = ConcurrentHashMap<KType, Lazy<ISyncInterceptor>>()

    @Suppress("EXPERIMENTAL_API_USAGE_ERROR", "UNCHECKED_CAST")
    inline fun <reified T : ProgressArgs> getInterceptor(): InterceptorWrapper<T> {
        val typeOfT = typeOf<T>()
        val lazyInterceptor = dictionary.getOrPut(typeOfT) { lazy { InterceptorWrapper<T>() } }
        return lazyInterceptor.value as InterceptorWrapper<T>
    }

    /**
     * Gets a boolean returning true if an interceptor of type T, exists
     */
    @Suppress("EXPERIMENTAL_API_USAGE_ERROR")
    inline fun <reified T : ProgressArgs> contains(): Boolean =
        dictionary.containsKey(typeOf<T>())
}
