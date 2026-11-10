package com.example.virus.data.api

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object RetrofitClient {

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    val okHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .connectTimeout(5, TimeUnit.SECONDS)
            .readTimeout(5, TimeUnit.SECONDS)
            .writeTimeout(5, TimeUnit.SECONDS)
            .build()
    }

    private var currentApi: ViralApi? = null
    private var lastBaseUrl: String? = null

    var serverHost: String
        get() = ServerConfigManager.serverAddress
        set(value) {
            ServerConfigManager.serverAddress = value
        }

    fun onServerAddressChanged(newAddress: String) {
        currentApi = null
    }

    @Synchronized
    fun getApi(): ViralApi {
        val baseUrl = ServerConfigManager.getHttpBaseUrl()
        if (currentApi == null || lastBaseUrl != baseUrl) {
            lastBaseUrl = baseUrl
            currentApi = Retrofit.Builder()
                .baseUrl(baseUrl)
                .client(okHttpClient)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(ViralApi::class.java)
        }
        return currentApi!!
    }
}
