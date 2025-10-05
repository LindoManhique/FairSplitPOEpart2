package com.example.fairsplit.view

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.fairsplit.controller.ExpensesController
import com.example.fairsplit.controller.GroupsController
import com.example.fairsplit.databinding.ActivityGroupsBinding
import com.example.fairsplit.model.dto.Expense
import com.example.fairsplit.model.dto.Group
import com.google.firebase.auth.FirebaseAuth

class GroupsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityGroupsBinding
    private lateinit var groupsCtrl: GroupsController
    private lateinit var expensesCtrl: ExpensesController

    private var groups: List<Group> = emptyList()
    private var lastCreatedGroupId: String? = null  // used by the demo "Add expense" button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityGroupsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // --- Controllers
        groupsCtrl = GroupsController { action ->
            when (action) {
                is GroupsController.Action.Loading ->
                    binding.progress.visibility = if (action.on) View.VISIBLE else View.GONE

                is GroupsController.Action.Error ->
                    Toast.makeText(this, action.msg, Toast.LENGTH_SHORT).show()

                is GroupsController.Action.Groups -> {
                    groups = action.items
                    val names = groups.map { it.name }
                    binding.listGroups.adapter =
                        ArrayAdapter(this, android.R.layout.simple_list_item_1, names)
                }

                is GroupsController.Action.Created -> {
                    lastCreatedGroupId = action.group.id
                    Toast.makeText(
                        this,
                        "Group created: ${action.group.name}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }

        expensesCtrl = ExpensesController { action ->
            // Keep it simple; we just want a toast on success/failure for the demo helper
            when (action) {
                is ExpensesController.Action.Error ->
                    Toast.makeText(this, action.msg, Toast.LENGTH_SHORT).show()
                is ExpensesController.Action.Added ->
                    Toast.makeText(this, "Expense added", Toast.LENGTH_SHORT).show()
                is ExpensesController.Action.Loading -> {
                    // optional spinner reuse
                    binding.progress.visibility = if (action.on) View.VISIBLE else View.GONE
                }
                else -> Unit
            }
        }

        // --- Existing "create from text" button
        binding.btnCreateGroup.setOnClickListener {
            val name = binding.etGroupName.text.toString().trim()
            if (name.isNotEmpty()) groupsCtrl.createGroup(name)
        }

        // --- DEMO helper: quick default group
        binding.btnAddGroup.setOnClickListener {
            groupsCtrl.createGroup("Demo Trip")
        }

        // --- DEMO helper: quick expense into a known group
        binding.btnAddExpense.setOnClickListener {
            val gid = lastCreatedGroupId ?: groups.firstOrNull()?.id
            if (gid == null || gid.isBlank()) {
                Toast.makeText(this, "Create a group first", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val uid = FirebaseAuth.getInstance().currentUser?.uid ?: ""
            val demo = Expense(
                title = "Demo Lunch",
                amount = 150.0,
                payerUid = uid,
                participants = listOf(uid)
            )
            expensesCtrl.add(gid, demo)
        }

        // --- Tap a group to open its expense list screen (your existing flow)
        binding.listGroups.setOnItemClickListener { _, _, position, _ ->
            val g = groups[position]
            startActivity(
                Intent(this, ExpensesActivity::class.java)
                    .putExtra("groupId", g.id)
                    .putExtra("groupName", g.name)
            )
        }
    }

    // Load the list each time the screen becomes visible
    override fun onStart() {
        super.onStart()
        groupsCtrl.loadMyGroups()
    }

    // (Optional but useful) Menu for Settings + Logout
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(com.example.fairsplit.R.menu.menu_groups, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            com.example.fairsplit.R.id.action_settings -> {
                startActivity(Intent(this, SettingsActivity::class.java))
                true
            }
            com.example.fairsplit.R.id.action_logout -> {
                FirebaseAuth.getInstance().signOut()
                startActivity(Intent(this, LoginActivity::class.java))
                finish()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}
