package com.example.fairsplit.controller

import com.example.fairsplit.model.dto.UserProfile
import com.example.fairsplit.model.remote.FirestoreRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// Passwords are never stored by the app. Firebase Auth transmits over HTTPS and stores passwords as salted/hashed on Google servers.

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

    private fun post(a: Action) = CoroutineScope(Dispatchers.Main).launch { ui(a) }

    fun register(email: String, password: String, displayName: String) {
        post(Action.Loading(true))
        auth.createUserWithEmailAndPassword(email, password).addOnCompleteListener { res ->
            if (!res.isSuccessful) {
                post(Action.Loading(false))
                post(Action.Error(res.exception?.message ?: "Register failed"))
                return@addOnCompleteListener
            }
            // Save profile safely; always stop loading even if it fails
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    repo.saveUserProfile(UserProfile(uid = repo.currentUid(), displayName = displayName))
                    withContext(Dispatchers.Main) {
                        ui(Action.Loading(false))
                        ui(Action.LoginSuccess)
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        ui(Action.Loading(false))
                        ui(Action.Error(e.message ?: "Failed to save profile"))
                    }
                }
            }
        }
    }

    fun login(email: String, password: String) {
        post(Action.Loading(true))
        auth.signInWithEmailAndPassword(email, password).addOnCompleteListener { res ->
            post(Action.Loading(false))
            if (res.isSuccessful) post(Action.LoginSuccess)
            else post(Action.Error(res.exception?.message ?: "Login failed"))
        }
    }
}
