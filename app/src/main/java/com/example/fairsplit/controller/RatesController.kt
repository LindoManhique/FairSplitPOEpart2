package com.example.fairsplit.controller

import com.example.fairsplit.model.remote.RatesService
import kotlinx.coroutines.*

class RatesController(
    private val ui: (Action) -> Unit
) {
    sealed class Action {
        data class Loading(val on: Boolean) : Action()
        data class Error(val msg: String) : Action()
        data class Result(val base: String, val target: String, val rate: Double) : Action()
    }

    private val main = CoroutineScope(Dispatchers.Main)
    private val TIMEOUT = 15_000L

    fun fetch(base: String, target: String) {
        main.launch {
            ui(Action.Loading(true))
            try {
                val resp = withTimeout(TIMEOUT) {
                    withContext(Dispatchers.IO) { RatesService.api.latest(base) }
                }
                val rate = resp.rates?.get(target)
                    ?: throw Exception("Rate not available for $target")
                ui(Action.Result(base, target, rate))
            } catch (_: TimeoutCancellationException) {
                ui(Action.Error("Rate fetch timed out."))
            } catch (e: Exception) {
                ui(Action.Error(e.message ?: "Failed to fetch rate"))
            } finally {
                ui(Action.Loading(false))
            }
        }
    }
}
