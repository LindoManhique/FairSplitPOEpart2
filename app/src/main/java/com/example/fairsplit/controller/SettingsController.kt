package com.example.fairsplit.controller

import com.example.fairsplit.model.dto.UserProfile
import com.example.fairsplit.model.remote.FirestoreRepository
import com.example.fairsplit.model.remote.RatesService
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.*

class SettingsController(
    private val repo: FirestoreRepository = FirestoreRepository(),
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val ui: (Action) -> Unit
) {
    sealed class Action {
        data class Loading(val on: Boolean) : Action()
        data class Error(val msg: String) : Action()
        data class Loaded(val profile: UserProfile) : Action()
        data class Rate(val text: String) : Action()
        object Saved : Action()
    }

    private val main = CoroutineScope(Dispatchers.Main)

    fun load() {
        main.launch {
            ui(Action.Loading(true))
            try {
                val uid = auth.currentUser?.uid ?: ""
                val profile = withContext(Dispatchers.IO) { repo.getUserProfile(uid) }
                    ?: UserProfile(uid = uid, displayName = "", defaultCurrency = "ZAR")
                ui(Action.Loaded(profile))
            } catch (e: Exception) {
                ui(Action.Error(e.message ?: "Load failed"))
            } finally {
                ui(Action.Loading(false))
            }
        }
    }

    fun save(profile: UserProfile) {
        main.launch {
            ui(Action.Loading(true))
            try {
                withContext(Dispatchers.IO) { repo.saveUserProfile(profile) }
                ui(Action.Saved)
            } catch (e: Exception) {
                ui(Action.Error(e.message ?: "Save failed"))
            } finally {
                ui(Action.Loading(false))
            }
        }
    }

    fun fetchRateZarToUsd() {
        main.launch {
            ui(Action.Loading(true))
            try {
                val rate = withContext(Dispatchers.IO) {
                    val resp = RatesService.api.latest("ZAR")
                    if (!resp.isSuccessful) throw Exception("HTTP ${resp.code()}")
                    resp.body()?.rates?.get("USD") ?: 0.0
                }
                ui(Action.Rate("1 ZAR = $rate USD"))
            } catch (e: Exception) {
                ui(Action.Error(e.message ?: "Rate fetch failed"))
            } finally {
                ui(Action.Loading(false))
            }
        }
    }
}
