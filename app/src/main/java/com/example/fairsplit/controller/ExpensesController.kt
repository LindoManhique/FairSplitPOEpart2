package com.example.fairsplit.controller

import android.util.Log
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

    private val main = CoroutineScope(Dispatchers.Main)

    fun load(groupId: String) {
        main.launch {
            ui(Action.Loading(true))
            try {
                Log.d("ExpensesController", "load: $groupId")
                val items = withContext(Dispatchers.IO) { repo.listExpenses(groupId) }
                ui(Action.Expenses(items))
            } catch (e: Exception) {
                ui(Action.Error(e.message ?: "Load failed"))
            } finally {
                ui(Action.Loading(false))
            }
        }
    }

    fun add(groupId: String, e: Expense) {
        main.launch {
            ui(Action.Loading(true))
            try {
                Log.d("ExpensesController", "add: ${e.title} ${e.amount}")
                withContext(Dispatchers.IO) { repo.addExpense(groupId, e) }
                ui(Action.Added)
                val items = withContext(Dispatchers.IO) { repo.listExpenses(groupId) }
                ui(Action.Expenses(items))
            } catch (ex: Exception) {
                ui(Action.Error(ex.message ?: "Add failed"))
            } finally {
                ui(Action.Loading(false))
            }
        }
    }
}
