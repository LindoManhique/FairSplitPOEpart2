package com.example.fairsplit.model.dto

data class RateResponse(
    val result: String? = null,
    val base_code: String? = null,
    val time_last_update_utc: String? = null,
    val rates: Map<String, Double>? = null
)
