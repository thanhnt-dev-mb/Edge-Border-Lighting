package com.merryblue.baseapplication.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asCoroutineDispatcher
import java.util.concurrent.Executors
import javax.inject.Qualifier
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    @DefaultDispatcher
    @Singleton
    @Provides
    fun defaultDispatcher(): CoroutineDispatcher =
        Executors.newCachedThreadPool().asCoroutineDispatcher()
    
    @DecodingDispatcher
    @Singleton
    @Provides
    fun decodingDispatcher(): CoroutineDispatcher =
        Executors.newFixedThreadPool(
            2 * Runtime.getRuntime().availableProcessors() + 1
        ).asCoroutineDispatcher()
    
    @EncodingDispatcher
    @Singleton
    @Provides
    fun encodingDispatcher(): CoroutineDispatcher =
        Executors.newSingleThreadExecutor().asCoroutineDispatcher()
    
    @IoDispatcher
    @Singleton
    @Provides
    fun ioDispatcher(): CoroutineDispatcher = Dispatchers.IO
    
    @UiDispatcher
    @Singleton
    @Provides
    fun uiDispatcher(): CoroutineDispatcher = Dispatchers.Main.immediate
}

@Qualifier
@Retention(AnnotationRetention.RUNTIME)
annotation class DefaultDispatcher

@Qualifier
@Retention(AnnotationRetention.RUNTIME)
annotation class EncodingDispatcher

@Qualifier
@Retention(AnnotationRetention.RUNTIME)
annotation class DecodingDispatcher

@Qualifier
@Retention(AnnotationRetention.RUNTIME)
annotation class IoDispatcher

@Qualifier
@Retention(AnnotationRetention.RUNTIME)
annotation class UiDispatcher
