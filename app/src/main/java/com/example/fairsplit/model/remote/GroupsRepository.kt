package com.example.fairsplit.model.remote

import com.example.fairsplit.model.dto.Expense
import com.example.fairsplit.model.dto.Group
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.tasks.await

class GroupsRepository {
    private val db = Firebase.firestore

    suspend fun createGroup(ownerUid: String, name: String): String {
        val ref = db.collection("groups").document()
        val g = Group(id = ref.id, name = name, members = listOf(ownerUid))
        ref.set(g).await()
        return ref.id
    }

    suspend fun addExpense(e: Expense) {
        db.collection("groups").document(e.groupId)
            .collection("expenses").document(e.id.ifBlank { db.collection("x").document().id })
            .set(e).await()
    }
}
