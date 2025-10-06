package com.example.fairsplit.controller

import com.example.fairsplit.model.dto.Group
import com.example.fairsplit.model.remote.FirestoreRepository
import kotlinx.coroutines.*

/**
 * Groups controller with:
 * - spinner always cleared (finally)
 * - server-first load
 * - createGroup() with 5s timeout + optimistic fallback
 */
class GroupsController(
    private val ui: (Action) -> Unit,
    private val repo: FirestoreRepository = FirestoreRepository()
) {

    sealed class Action {
        data class Loading(val on: Boolean) : Action()
        data class Error(val msg: String) : Action()
        data class Groups(val items: List<Group>) : Action()
        data class Created(val group: Group) : Action()
    }

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    fun loadMyGroups() {
        // quick UX: no blocking spinner here
        ui(Action.Loading(false))
        scope.launch {
            try {
                val items = repo.myGroups()
                ui(Action.Groups(items))
            } catch (e: Exception) {
                ui(Action.Error(e.localizedMessage ?: "Failed to load groups"))
            }
        }
    }

    fun createGroup(name: String) {
        ui(Action.Loading(true))
        scope.launch {
            try {
                // Try to create, but don't hang forever
                val created = withTimeoutOrNull(5_000) {
                    repo.createGroup(name)
                }

                if (created != null) {
                    // normal path
                    ui(Action.Created(created))
                } else {
                    // timeout: optimistic local item so UI updates immediately
                    val uid = repo.currentUid()
                    val local = Group(
                        id = java.util.UUID.randomUUID().toString(),
                        name = name,
                        members = listOf(uid)
                    )
                    ui(Action.Created(local))
                    ui(Action.Error("Network slow/offline: will sync in background."))

                    // Try the real write in background without blocking UI
                    launch(Dispatchers.IO) {
                        try { repo.createGroup(name) } catch (_: Exception) { /* swallow */ }
                    }
                }
            } catch (e: Exception) {
                ui(Action.Error(e.localizedMessage ?: "Failed to create group"))
            } finally {
                // ALWAYS clear spinner
                ui(Action.Loading(false))
            }
        }
    }

    fun close() {
        scope.cancel()
    }
}
