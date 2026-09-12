package com.footix.tv.data.remote

import com.footix.tv.core.AppConfig
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

object HttpClientFactory {

    fun create(): OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(AppConfig.HTTP_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .readTimeout(AppConfig.HTTP_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .callTimeout(AppConfig.HTTP_TIMEOUT_SECONDS * 2, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .addInterceptor { chain ->
            val request = chain.request().newBuilder()
                .header("User-Agent", AppConfig.HTTP_USER_AGENT)
                .header("Accept", "application/json")
                .build()
            chain.proceed(request)
        }
        .build()
}
