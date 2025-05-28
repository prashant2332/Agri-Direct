            package com.example.finalyearprojectwithfirebase.network



            import com.example.finalyearprojectwithfirebase.model.MandiResponse
            import retrofit2.Call
            import retrofit2.http.GET
            import retrofit2.http.Query

            val apikey="579b464db66ec23bdd00000146157ce6f0444e2c5d49c2544f6016ce"

            interface MandiApiService {

                @GET("resource/9ef84268-d588-465a-a308-a864a43d0070")
                fun getMandiData(
                    @Query("api-key") apiKey: String = apikey,
                    @Query("format") format: String = "json",
                    @Query("limit") limit: Int = 100000,
                    @Query("filters[state]") state: String,
                    @Query("filters[district]") district: String,
                    @Query("filters[commodity]") commodity: String,
                    @Query("filters[arrival_date]") arrivalDate: String
                ): Call<MandiResponse>

                @GET("resource/9ef84268-d588-465a-a308-a864a43d0070")
                fun getMandiDatawithproductname(
                    @Query("api-key") apiKey: String = apikey,
                    @Query("format") format: String = "json",
                    @Query("limit") limit: Int = 100000,
                    @Query("filters[commodity]") commodity: String,
                    @Query("filters[arrival_date]") arrivalDate: String
                ): Call<MandiResponse>

                @GET("resource/9ef84268-d588-465a-a308-a864a43d0070")
                fun getMandiDatawithproductnameandstate(
                    @Query("api-key") apiKey: String = apikey,
                    @Query("format") format: String = "json",
                    @Query("limit") limit: Int = 100000,
                    @Query("filters[state]") state: String,
                    @Query("filters[commodity]") commodity: String,
                    @Query("filters[arrival_date]") arrivalDate: String
                ): Call<MandiResponse>

            }



