package com.example.fairsplit.view

import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.fairsplit.databinding.ActivityExpensesBinding
import com.example.fairsplit.model.dto.Expense
import com.example.fairsplit.model.remote.FirestoreRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import java.util.Locale

class ExpensesActivity : AppCompatActivity() {

    private lateinit var b: ActivityExpensesBinding
    private val repo = FirestoreRepository()

    private lateinit var groupId: String
    private lateinit var groupName: String

    private val items = mutableListOf<Expense>()
    private lateinit var adapter: ArrayAdapter<String>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityExpensesBinding.inflate(layoutInflater)
        setContentView(b.root)

        // From GroupsActivity intent
        groupId = intent.getStringExtra("groupId") ?: ""
        groupName = intent.getStringExtra("groupName") ?: "Expenses"
        b.tvScreenTitle.text = "Expenses — $groupName"

        // Simple list adapter
        adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, mutableListOf())
        b.listExpenses.adapter = adapter

        // Add expense
        b.btnAdd.setOnClickListener {
            val title = b.etTitle.text?.toString()?.trim().orEmpty()
            val amountText = b.etAmount.text?.toString()?.trim().orEmpty()
            val amount = amountText.toDoubleOrNull()

            var invalid = false
            if (title.isEmpty()) { b.etTitle.error = "Enter a title"; invalid = true }
            if (amount == null || amount <= 0.0) { b.etAmount.error = "Enter a valid amount"; invalid = true }
            if (invalid) return@setOnClickListener

            val amt = amount!!   // safe after validation
            val uid = FirebaseAuth.getInstance().currentUser?.uid ?: ""
            val e = Expense(title = title, amount = amt, payerUid = uid, participants = listOf(uid))

            // Optimistic insert
            items.add(0, e)
            refreshAdapter()

            // Real write
            setLoading(true)
            lifecycleScope.launch {
                try {
                    repo.addExpense(groupId, e)
                    toast("Expense added")
                } catch (_: Exception) {
                    toast("Could not add (offline?) — will resync")
                } finally {
                    setLoading(false)
                }
            }

            // Clear inputs
            b.etTitle.setText("")
            b.etAmount.setText("")
        }

        // Long-press delete
        b.listExpenses.setOnItemLongClickListener { _, _, position, _ ->
            val toDelete = items[position]
            items.removeAt(position)         // optimistic remove
            refreshAdapter()
            lifecycleScope.launch {
                val ok = repo.deleteExpense(groupId, toDelete.id)
                if (!ok) toast("Could not delete (offline?) — will resync")
            }
            true
        }
    }

    override fun onStart() {
        super.onStart()
        setLoading(true)
        lifecycleScope.launch {
            try {
                val list = repo.listExpenses(groupId)  // server then cache
                items.clear()
                items.addAll(list)
                refreshAdapter()
            } finally {
                setLoading(false)
            }
        }
    }

    // Helpers
    private fun refreshAdapter() {
        val rows = items.map { exp ->
            val amt = String.format(Locale.getDefault(), "R %.2f", exp.amount)
            "${exp.title} — $amt"
        }
        adapter.clear()
        adapter.addAll(rows)
        adapter.notifyDataSetChanged()
    }

    private fun setLoading(on: Boolean) {
        b.progress.visibility = if (on) View.VISIBLE else View.GONE
        b.btnAdd.isEnabled = !on
        b.etTitle.isEnabled = !on
        b.etAmount.isEnabled = !on
        b.listExpenses.isEnabled = !on
    }

    private fun toast(msg: String) =
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
}
