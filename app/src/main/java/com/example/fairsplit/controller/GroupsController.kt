package com.example.fairsplit.controller

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

    private val io = CoroutineScope(Dispatchers.IO)
    private fun post(a: Action) = CoroutineScope(Dispatchers.Main).launch { ui(a) }

    fun loadMyGroups() {
        post(Action.Loading(true))
        io.launch {
            try { post(Action.Groups(repo.myGroups())) }
            catch (ex: Exception) { post(Action.Error(ex.message ?: "Failed to load groups")) }
            finally { post(Action.Loading(false)) }
        }
    }

    fun createGroup(name: String) {
        post(Action.Loading(true))
        io.launch {
            try {
                val g = repo.createGroup(name)
                post(Action.Created(g))
                post(Action.Groups(repo.myGroups()))
            } catch (ex: Exception) {
                post(Action.Error(ex.message ?: "Create failed"))
            } finally { post(Action.Loading(false)) }
        }
    }
}
