package com.example.currencydashboard.di

import com.example.currencydashboard.BuildConfig
import com.example.currencydashboard.data.remote.ExchangeRateApi
import com.example.currencydashboard.data.remote.ExchangeRateDataSource
import com.google.gson.GsonBuilder
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import org.koin.dsl.module
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

val networkModule = module {
    // OkHttpClient
    single {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
        
        OkHttpClient.Builder()
            .addInterceptor { chain ->
                val original = chain.request()
                val originalUrl = original.url
                
                val url = originalUrl.newBuilder()
                    .addQueryParameter("access_key", BuildConfig.API_KEY)
                    .build()
                    
                val request = original.newBuilder()
                    .url(url)
                    .build()
                    
                chain.proceed(request)
            }
            .addInterceptor(loggingInterceptor)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()
    }
    
    // Gson
    single { GsonBuilder().create() }
    
    // Retrofit
    single {
        Retrofit.Builder()
            .baseUrl(BuildConfig.BASE_URL)
            .client(get())
            .addConverterFactory(GsonConverterFactory.create(get()))
            .build()
    }
    
    // API Services
    single { get<Retrofit>().create(ExchangeRateApi::class.java) }
    
    // Data Sources
    single { ExchangeRateDataSource(get()) }
} 