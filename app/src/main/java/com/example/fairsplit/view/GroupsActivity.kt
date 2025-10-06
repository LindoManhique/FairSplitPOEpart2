package com.example.fairsplit.view

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.fairsplit.controller.ExpensesController
import com.example.fairsplit.controller.GroupsController
import com.example.fairsplit.databinding.ActivityGroupsBinding
import com.example.fairsplit.model.dto.Expense
import com.example.fairsplit.model.dto.Group
import com.example.fairsplit.model.remote.FirestoreRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import kotlinx.coroutines.*

class GroupsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityGroupsBinding
    private lateinit var groupsCtrl: GroupsController
    private lateinit var expensesCtrl: ExpensesController
    private val repo = FirestoreRepository()

    private var groups: List<Group> = emptyList()
    private var lastCreatedGroupId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityGroupsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.tvScreenTitle.text = "Groups"

        fun setLoading(on: Boolean) {
            binding.progress.visibility = if (on) View.VISIBLE else View.GONE
            binding.btnCreateGroup.isEnabled = !on
            binding.btnAddGroup.isEnabled = !on
            binding.btnAddExpense.isEnabled = !on
            binding.btnOpenSettings.isEnabled = !on
            binding.etGroupName.isEnabled = !on
        }

        // ---------- Controllers ----------
        groupsCtrl = GroupsController(
            ui = { action: GroupsController.Action ->
                when (action) {
                    is GroupsController.Action.Loading -> setLoading(action.on)
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
                        Toast.makeText(this, "Group created: ${action.group.name}", Toast.LENGTH_SHORT).show()

                        // optimistic insert to top
                        groups = listOf(action.group) + groups
                        val names = groups.map { it.name }
                        val existing = binding.listGroups.adapter as? ArrayAdapter<String>
                        if (existing != null) {
                            existing.clear(); existing.addAll(names); existing.notifyDataSetChanged()
                        } else {
                            binding.listGroups.adapter =
                                ArrayAdapter(this, android.R.layout.simple_list_item_1, names)
                        }
                        binding.etGroupName.text?.clear()
                    }
                }
            },
            repo = FirestoreRepository()
        )

        expensesCtrl = ExpensesController(
            ui = { action: ExpensesController.Action ->
                when (action) {
                    is ExpensesController.Action.Loading -> setLoading(action.on)
                    is ExpensesController.Action.Error ->
                        Toast.makeText(this, action.msg, Toast.LENGTH_SHORT).show()
                    is ExpensesController.Action.Added ->
                        Toast.makeText(this, "Expense added", Toast.LENGTH_SHORT).show()
                    else -> Unit
                }
            }
        )
        // ---------------------------------

        binding.btnOpenSettings.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }

        binding.btnCreateGroup.setOnClickListener {
            val name = binding.etGroupName.text.toString().trim()
            if (name.isEmpty()) {
                binding.etGroupName.error = "Enter a group name"
            } else {
                groupsCtrl.createGroup(name)
            }
        }

        binding.btnAddGroup.setOnClickListener {
            groupsCtrl.createGroup("Demo Trip")
        }

        binding.btnAddExpense.setOnClickListener {
            val gid = lastCreatedGroupId ?: groups.firstOrNull()?.id
            if (gid.isNullOrBlank()) {
                Toast.makeText(this, "Create a group first", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val uid = FirebaseAuth.getInstance().currentUser?.uid ?: ""
            val e = Expense(
                title = "Demo Lunch",
                amount = 150.0,
                payerUid = uid,
                participants = listOf(uid)
            )
            expensesCtrl.add(gid, e)
        }

        binding.listGroups.setOnItemClickListener { _, _, position, _ ->
            val g = groups[position]
            startActivity(
                Intent(this, ExpensesActivity::class.java)
                    .putExtra("groupId", g.id)
                    .putExtra("groupName", g.name)
            )
        }
    }

    // Refresh welcome + groups whenever screen becomes visible
    override fun onStart() {
        super.onStart()
        refreshWelcome()
        groupsCtrl.loadMyGroups()
    }

    private fun refreshWelcome() {
        lifecycleScope.launch {
            val profile = repo.getUserProfile()
            val fallback = FirebaseAuth.getInstance().currentUser?.email ?: "User"
            binding.tvUserName.text = "Welcome, ${profile?.displayName ?: fallback}"
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(com.example.fairsplit.R.menu.menu_groups, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            com.example.fairsplit.R.id.action_settings -> {
                startActivity(Intent(this, SettingsActivity::class.java)); true
            }
            com.example.fairsplit.R.id.action_logout -> {
                FirebaseAuth.getInstance().signOut()
                startActivity(Intent(this, LoginActivity::class.java))
                finish(); true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}
