package com.example.fairsplit.controller

import android.util.Log
import com.example.fairsplit.model.dto.Group
import com.example.fairsplit.model.remote.FirestoreRepository
import kotlinx.coroutines.*

class GroupsController(
    private val repo: FirestoreRepository = FirestoreRepository(),
    private val ui: (Action) -> Unit
) {
    sealed class Action {
        data class Loading(val on: Boolean) : Action()
        data class Error(val msg: String) : Action()
        data class Groups(val items: List<Group>) : Action()
        data class Created(val group: Group) : Action()
    }

    private val main = CoroutineScope(Dispatchers.Main)

    fun loadMyGroups() {
        main.launch {
            ui(Action.Loading(true))
            try {
                Log.d("GroupsController", "loadMyGroups")
                val items = withContext(Dispatchers.IO) { repo.myGroups() }
                ui(Action.Groups(items))
            } catch (e: Exception) {
                ui(Action.Error(e.message ?: "Failed to load groups"))
            } finally {
                ui(Action.Loading(false))
            }
        }
    }

    fun createGroup(name: String) {
        main.launch {
            ui(Action.Loading(true))
            try {
                Log.d("GroupsController", "createGroup: $name")
                val g = withContext(Dispatchers.IO) { repo.createGroup(name) }
                ui(Action.Created(g))
                // refresh list
                val items = withContext(Dispatchers.IO) { repo.myGroups() }
                ui(Action.Groups(items))
            } catch (e: Exception) {
                ui(Action.Error(e.message ?: "Create failed"))
            } finally {
                ui(Action.Loading(false))
            }
        }
    }
}
