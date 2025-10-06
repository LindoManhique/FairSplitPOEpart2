package com.example.fairsplit.controller

import com.example.fairsplit.model.dto.UserProfile
import com.example.fairsplit.model.remote.FirestoreRepository
import kotlinx.coroutines.*

class SettingsController(
    private val ui: (Action) -> Unit,
    private val repo: FirestoreRepository = FirestoreRepository()
) {
    sealed class Action {
        data class Loading(val on: Boolean) : Action()
        data class Error(val msg: String) : Action()
        object Saved : Action()
        data class Prefilled(val name: String, val currency: String) : Action()
    }

    private val main = CoroutineScope(Dispatchers.Main)
    private val timeoutMs = 20_000L

    /** Support both `currencyCode` and legacy `currency` field names. */
    private fun readCurrency(profile: UserProfile): String {
        try {
            val f = profile::class.java.getDeclaredField("currencyCode")
            f.isAccessible = true
            (f.get(profile) as? String)?.let { return it }
        } catch (_: Exception) {}
        try {
            val f = profile::class.java.getDeclaredField("currency")
            f.isAccessible = true
            (f.get(profile) as? String)?.let { return it }
        } catch (_: Exception) {}
        return ""
    }

    fun loadProfile() {
        main.launch {
            ui(Action.Loading(true))
            try {
                val uid = repo.currentUid()
                if (uid.isNotBlank()) {
                    val prof = withContext(Dispatchers.IO) {
                        runCatching { repo.getUserProfile(uid) }.getOrNull()
                    }
                    if (prof != null) {
                        ui(Action.Prefilled(prof.displayName, readCurrency(prof)))
                    }
                }
            } finally {
                ui(Action.Loading(false))
            }
        }
    }

    fun saveProfile(displayName: String, currencyCode: String) {
        main.launch {
            ui(Action.Loading(true))
            try {
                val uid = repo.currentUid()
                if (uid.isBlank()) throw Exception("Not signed in.")

                // POSitional args (uid, displayName, currencyCode/currency)
                val profile = UserProfile(
                    uid,
                    displayName,
                    currencyCode.uppercase()
                )

                withContext(Dispatchers.IO) {
                    withTimeoutOrNull(timeoutMs) { repo.saveUserProfile(profile) }
                }
                ui(Action.Saved)
            } catch (e: Exception) {
                ui(Action.Error(e.message ?: "Could not save profile"))
            } finally {
                ui(Action.Loading(false))
            }
        }
    }
}
