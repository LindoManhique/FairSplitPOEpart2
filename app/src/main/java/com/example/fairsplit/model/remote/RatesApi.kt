package com.example.fairsplit.model.remote

import retrofit2.http.GET
import retrofit2.http.Path

data class RatesResponse(
    val result: String? = null,
    val base_code: String? = null,
    val rates: Map<String, Double>? = null
)

interface RatesApi {
    @GET("v6/latest/{base}")
    suspend fun latest(@Path("base") base: String): RatesResponse
}
