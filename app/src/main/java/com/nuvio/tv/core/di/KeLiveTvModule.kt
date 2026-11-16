package com.nuvio.tv.core.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit
import javax.inject.Qualifier
import javax.inject.Singleton

// KE adapter: qualifiers expected by com.nuvio.tv.livetv, backed by the fork's OkHttpClient.
@Qualifier @Retention(AnnotationRetention.BINARY) annotation class Validated
@Qualifier @Retention(AnnotationRetention.BINARY) annotation class Download

@Module
@InstallIn(SingletonComponent::class)
object KeLiveTvModule {
    @Provides @Singleton @Validated
    fun validated(base: OkHttpClient): OkHttpClient = base

    @Provides @Singleton @Download
    fun download(base: OkHttpClient): OkHttpClient =
        base.newBuilder().callTimeout(0, TimeUnit.MILLISECONDS).readTimeout(60, TimeUnit.SECONDS).build()
}
