package com.example.fairsplit.controller

import com.example.fairsplit.model.remote.RatesService
import kotlinx.coroutines.*

class RatesController(private val ui: (Action) -> Unit) {
    sealed class Action {
        data class Loading(val on: Boolean) : Action()
        data class Error(val msg: String) : Action()
        data class Rate(val zARtoUSD: Double) : Action()
    }

    private val io = CoroutineScope(Dispatchers.IO)
    private fun post(a: Action) = CoroutineScope(Dispatchers.Main).launch { ui(a) }

    fun fetchZarToUsd() {
        post(Action.Loading(true))
        io.launch {
            try {
                val resp = RatesService.api.latest("ZAR")
                if (resp.isSuccessful) {
                    val usd = resp.body()?.rates?.get("USD") ?: 0.0
                    post(Action.Rate(usd))
                } else {
                    post(Action.Error("HTTP ${resp.code()}"))
                }
            } catch (ex: Exception) {
                post(Action.Error(ex.message ?: "Network error"))
            } finally {
                post(Action.Loading(false))
            }
        }
    }
}
