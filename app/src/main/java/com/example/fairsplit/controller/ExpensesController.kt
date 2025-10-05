package com.example.fairsplit.controller

import com.example.fairsplit.model.dto.Expense
import com.example.fairsplit.model.remote.FirestoreRepository
import kotlinx.coroutines.*

class ExpensesController(
    private val repo: FirestoreRepository = FirestoreRepository(),
    private val ui: (Action) -> Unit
) {
    sealed class Action {
        data class Loading(val on: Boolean) : Action()
        data class Error(val msg: String) : Action()
        data class Expenses(val items: List<Expense>) : Action()
        object Added : Action()
    }

    private val io = CoroutineScope(Dispatchers.IO)
    private fun post(a: Action) = CoroutineScope(Dispatchers.Main).launch { ui(a) }

    fun load(groupId: String) {
        post(Action.Loading(true))
        io.launch {
            try { post(Action.Expenses(repo.listExpenses(groupId))) }
            catch (ex: Exception) { post(Action.Error(ex.message ?: "Load failed")) }
            finally { post(Action.Loading(false)) }
        }
    }

    fun add(groupId: String, expense: Expense) {
        post(Action.Loading(true))
        io.launch {
            try {
                repo.addExpense(groupId, expense)
                post(Action.Added)
                post(Action.Expenses(repo.listExpenses(groupId)))
            } catch (ex: Exception) {
                post(Action.Error(ex.message ?: "Add failed"))
            } finally { post(Action.Loading(false)) }
        }
    }
}
