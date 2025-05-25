        package com.example.finalyearprojectwithfirebase.network

        import okhttp3.OkHttpClient
        import okhttp3.logging.HttpLoggingInterceptor
        import retrofit2.Retrofit
        import retrofit2.converter.gson.GsonConverterFactory
        object RetrofitClient {
            private const val BASE_URL = "https://api.cloudinary.com/v1_1/divuplq83/"

            val api: CloudinaryApi by lazy {
                val logging = HttpLoggingInterceptor().apply {
                    setLevel(HttpLoggingInterceptor.Level.BODY)
                }

                val client = OkHttpClient.Builder()
                    .addInterceptor(logging)
                    .build()

                Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .client(client)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build()
                    .create(CloudinaryApi::class.java)
            }
        }

