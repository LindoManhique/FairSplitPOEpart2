package com.example.fairsplit.util

import android.app.Activity
import android.content.Intent
import com.example.fairsplit.R
import com.example.fairsplit.view.ExpensesActivity
import com.example.fairsplit.view.GroupsActivity
import com.example.fairsplit.view.SettingsActivity
import com.google.android.material.bottomnavigation.BottomNavigationView

object Nav {

    fun setup(
        activity: Activity,
        bottomNav: BottomNavigationView,
        selectedTabId: Int,
        groupId: String?,
        groupName: String?
    ) {
        bottomNav.selectedItemId = selectedTabId

        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.tab_groups -> {
                    if (selectedTabId != R.id.tab_groups) {
                        activity.startActivity(Intent(activity, GroupsActivity::class.java))
                        activity.finish()
                    }
                    true
                }
                R.id.tab_expenses -> {
                    if (selectedTabId != R.id.tab_expenses) {
                        if (!groupId.isNullOrBlank()) {
                            val i = Intent(activity, ExpensesActivity::class.java)
                                .putExtra("groupId", groupId)
                                .putExtra("groupName", groupName ?: "")
                            activity.startActivity(i)
                            activity.finish()
                        } else {
                            // No group yet, send to Groups
                            bottomNav.selectedItemId = R.id.tab_groups
                            activity.startActivity(Intent(activity, GroupsActivity::class.java))
                            activity.finish()
                        }
                    }
                    true
                }
                R.id.tab_settings -> {
                    if (selectedTabId != R.id.tab_settings) {
                        activity.startActivity(Intent(activity, SettingsActivity::class.java))
                        activity.finish()
                    }
                    true
                }
                else -> false
            }
        }
    }
}
