package com.example.fairsplit.controller

import com.example.fairsplit.model.dto.Group
import com.example.fairsplit.model.remote.FirestoreRepository
import kotlinx.coroutines.*


class GroupsController(
    private val ui: (Action) -> Unit,
    private val repo: FirestoreRepository = FirestoreRepository()
) {

    sealed class Action {
        data class Loading(val on: Boolean) : Action()
        data class Error(val msg: String) : Action()
        data class Groups(val items: List<Group>) : Action()
        data class Created(val group: Group) : Action()

        // Emitted when a delete should immediately reflect in UI
        data class Deleted(val groupId: String) : Action()
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
                val created = withTimeoutOrNull(5_000) { repo.createGroup(name) }

                if (created != null) {
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

                    // Real write in background
                    launch(Dispatchers.IO) { runCatching { repo.createGroup(name) } }
                }
            } catch (e: Exception) {
                ui(Action.Error(e.localizedMessage ?: "Failed to create group"))
            } finally {
                ui(Action.Loading(false))
            }
        }
    }


    fun deleteGroup(group: Group) {
        scope.launch {
            // Briefly show spinner (optional), then clear it to avoid "infinite spinner" feel
            ui(Action.Loading(true))

            // Instant UI update
            ui(Action.Deleted(group.id))
            ui(Action.Loading(false))

            // Best-effort backend delete with 4s timeout on IO
            val ok = withContext(Dispatchers.IO) {
                runCatching {
                    withTimeout(4_000) { repo.deleteGroup(group.id) }
                    true
                }.getOrElse { false }
            }

            if (!ok) {
                ui(Action.Error("Network issue: will finish deleting in background when online."))
                // Optional: fire-and-forget retry
                launch(Dispatchers.IO) { runCatching { repo.deleteGroup(group.id) } }
            }
        }
    }

    fun close() {
        scope.cancel()
    }
}
