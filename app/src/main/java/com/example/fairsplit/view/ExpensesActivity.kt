package com.example.fairsplit.view

import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.fairsplit.controller.ExpensesController
import com.example.fairsplit.databinding.ActivityExpensesBinding
import com.example.fairsplit.model.dto.Expense
import com.google.firebase.auth.FirebaseAuth

class ExpensesActivity : AppCompatActivity() {
    private lateinit var binding: ActivityExpensesBinding
    private lateinit var ctrl: ExpensesController
    private lateinit var groupId: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityExpensesBinding.inflate(layoutInflater)
        setContentView(binding.root)

        groupId = intent.getStringExtra("groupId") ?: ""
        binding.tvGroupTitle.text = intent.getStringExtra("groupName") ?: "Group"

        fun setLoading(on: Boolean) {
            binding.progress.visibility = if (on) View.VISIBLE else View.GONE
            binding.btnAdd.isEnabled = !on
            binding.etTitle.isEnabled = !on
            binding.etAmount.isEnabled = !on
        }

        ctrl = ExpensesController { action ->
            when (action) {
                is ExpensesController.Action.Loading -> setLoading(action.on)
                is ExpensesController.Action.Error ->
                    Toast.makeText(this, action.msg, Toast.LENGTH_SHORT).show()
                is ExpensesController.Action.Expenses -> {
                    val lines = action.items.map { "${it.title}: ${it.amount}" }
                    binding.listExpenses.adapter =
                        ArrayAdapter(this, android.R.layout.simple_list_item_1, lines)
                }
                is ExpensesController.Action.Added ->
                    Toast.makeText(this, "Added", Toast.LENGTH_SHORT).show()
            }
        }

        binding.btnAdd.setOnClickListener {
            val title = binding.etTitle.text?.toString()?.trim().orEmpty()
            val amtStr = binding.etAmount.text?.toString()?.trim().orEmpty()
            val amt = amtStr.toDoubleOrNull()

            var ok = true
            if (title.isEmpty()) { binding.etTitle.error = "Title required"; ok = false }
            if (amt == null || amt <= 0.0) { binding.etAmount.error = "Enter a positive amount"; ok = false }
            if (!ok) return@setOnClickListener

            val uid = FirebaseAuth.getInstance().currentUser?.uid ?: ""
            val e = Expense(
                title = title,
                amount = amt!!,
                payerUid = uid,
                participants = listOf(uid)
            )
            ctrl.add(groupId, e)
            // optional: clear inputs after adding
            binding.etTitle.text?.clear()
            binding.etAmount.text?.clear()
        }
    }

    override fun onStart() {
        super.onStart()
        ctrl.load(groupId)
    }
}
