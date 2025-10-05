package com.example.fairsplit.controller

import android.util.Log
import com.example.fairsplit.model.dto.UserProfile
import com.example.fairsplit.model.remote.FirestoreRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.tasks.await   // <-- official await()

class AuthController(
    private val repo: FirestoreRepository = FirestoreRepository(),
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val ui: (Action) -> Unit
) {
    sealed class Action {
        data class Loading(val on: Boolean) : Action()
        data class Error(val msg: String) : Action()
        object LoginSuccess : Action()
    }

    private val main = CoroutineScope(Dispatchers.Main)

    fun register(email: String, password: String, displayName: String) {
        main.launch {
            ui(Action.Loading(true))
            try {
                Log.d("AuthController", "register start: $email")
                val res = auth.createUserWithEmailAndPassword(email, password).await()
                Log.d("AuthController", "register auth ok uid=${res.user?.uid}")

                // Navigate immediately (stop spinner)
                ui(Action.Loading(false))
                ui(Action.LoginSuccess)

                // Save profile in background (best effort)
                withContext(Dispatchers.IO) {
                    val uid = res.user?.uid ?: return@withContext
                    repo.saveUserProfile(UserProfile(uid = uid, displayName = displayName))
                }
            } catch (e: Exception) {
                ui(Action.Error(e.message ?: "Register failed"))
            } finally {
                // Safety: never leave spinner on
                ui(Action.Loading(false))
            }
        }
    }

    fun login(email: String, password: String) {
        main.launch {
            ui(Action.Loading(true))
            try {
                Log.d("AuthController", "login start: $email")
                auth.signInWithEmailAndPassword(email, password).await()
                Log.d("AuthController", "login ok")
                ui(Action.LoginSuccess)
            } catch (e: Exception) {
                ui(Action.Error(e.message ?: "Login failed"))
            } finally {
                ui(Action.Loading(false))
            }
        }
    }
}
