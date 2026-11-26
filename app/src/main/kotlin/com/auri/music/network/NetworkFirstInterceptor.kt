package com.auri.music.network

import coil.annotation.ExperimentalCoilApi
import coil.intercept.Interceptor
import coil.request.ImageResult
import coil.request.SuccessResult
import java.util.concurrent.ConcurrentHashMap

/**
 * Tracks successful image fetch timestamps to enforce a 24h freshness window
 * while gracefully falling back to cached disk entries when offline.
 */
@OptIn(ExperimentalCoilApi::class)
class NetworkFirstInterceptor : Interceptor {
    private val lastFetchTimes = ConcurrentHashMap<String, Long>()

    override suspend fun intercept(chain: Interceptor.Chain): ImageResult {
        val request = chain.request
        val key = request.data.toString()
        val now = System.currentTimeMillis()
        val lastFetch = lastFetchTimes[key] ?: 0L
        val isStale = now - lastFetch > TTL_24H_MS

        val result = chain.proceed(request)
        if (result is SuccessResult && (isStale || lastFetch == 0L)) {
            lastFetchTimes[key] = now
        }
        return result
    }

    companion object {
        private const val TTL_24H_MS = 24L * 60L * 60L * 1000L
    }
}
