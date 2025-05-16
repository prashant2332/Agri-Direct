package com.example.finalyearprojectwithfirebase.network


import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object MandiClient {
    private const val BASE_URL = "https://api.data.gov.in/"

    val instance: MandiApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(MandiApiService::class.java)
    }
}