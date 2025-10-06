package com.example.fairsplit.controller

import com.example.fairsplit.model.dto.Expense
import com.example.fairsplit.model.remote.FirestoreRepository
import kotlinx.coroutines.*

class ExpensesController(
    private val ui: (Action) -> Unit,
    private val repo: FirestoreRepository = FirestoreRepository()
) {
    // UI messages to the Activity
    sealed class Action {
        data class Loading(val on: Boolean) : Action()
        data class Error(val msg: String) : Action()
        data class Items(val items: List<Expense>) : Action()
        data class Added(val item: Expense) : Action()
        data class Removed(val id: String) : Action()
    }

    private val main = CoroutineScope(Dispatchers.Main)
    private val TIMEOUT = 20_000L

    /** Load all expenses for a group */
    fun load(groupId: String) {
        main.launch {
            ui(Action.Loading(true))
            try {
                val items = withTimeout(TIMEOUT) {
                    withContext(Dispatchers.IO) { repo.listExpenses(groupId) }
                }
                ui(Action.Items(items))
            } catch (_: TimeoutCancellationException) {
                ui(Action.Error("Loading expenses timed out."))
            } catch (e: Exception) {
                ui(Action.Error(e.message ?: "Failed to load expenses"))
            } finally {
                ui(Action.Loading(false))
            }
        }
    }

    /**
     * Add an expense with *optimistic UI*:
     * - show it immediately
     * - turn spinner OFF immediately
     * - save + refresh in the background (no spinner)
     */
    fun add(groupId: String, expense: Expense) {
        main.launch {
            // Optimistic UI now
            ui(Action.Loading(true))
            ui(Action.Added(expense))
            ui(Action.Loading(false))   // IMPORTANT: stop spinner right away

            // Background save + refresh (no spinner)
            launch(Dispatchers.IO) {
                // Save (timeout quietly if slow/offline)
                withTimeoutOrNull(TIMEOUT) {
                    repo.addExpense(groupId, expense)
                }

                // Refresh list (keep current list if it fails)
                val fresh = runCatching { repo.listExpenses(groupId) }
                    .getOrElse { emptyList() }

                withContext(Dispatchers.Main) {
                    if (fresh.isNotEmpty()) ui(Action.Items(fresh))
                    // No loading toggle here; we already hid it
                }
            }
        }
    }

    /** Delete an expense (with confirmation handled in Activity) */
    fun delete(groupId: String, expenseId: String) {
        main.launch {
            ui(Action.Loading(true))
            try {
                withContext(Dispatchers.IO) {
                    withTimeoutOrNull(TIMEOUT) { repo.deleteExpense(groupId, expenseId) }
                }
                ui(Action.Removed(expenseId))

                // Refresh after delete
                val fresh = withContext(Dispatchers.IO) {
                    runCatching { repo.listExpenses(groupId) }.getOrElse { emptyList() }
                }
                ui(Action.Items(fresh))
            } catch (e: Exception) {
                ui(Action.Error(e.message ?: "Could not delete expense"))
            } finally {
                ui(Action.Loading(false))
            }
        }
    }
}
