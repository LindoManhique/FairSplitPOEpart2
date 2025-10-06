package com.example.fairsplit.util

import android.content.Context
import androidx.core.content.edit

object NavStore {
    private const val PREFS = "nav_store"
    private const val KEY_ID = "last_group_id"
    private const val KEY_NAME = "last_group_name"

    fun saveLastGroup(ctx: Context, id: String, name: String) {
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit {
            putString(KEY_ID, id)
            putString(KEY_NAME, name)
        }
    }

    fun lastGroup(ctx: Context): Pair<String?, String?> {
        val sp = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        return sp.getString(KEY_ID, null) to sp.getString(KEY_NAME, null)
    }
}
