package com.example.fairsplit.model.remote

import com.example.fairsplit.model.dto.RateResponse
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path

interface RatesApi {
    // Example call: https://open.er-api.com/v6/latest/ZAR
    @GET("v6/latest/{base}")
    suspend fun latest(@Path("base") base: String): Response<RateResponse>
}
