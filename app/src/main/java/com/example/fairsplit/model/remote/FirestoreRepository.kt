package com.example.fairsplit.model.remote

import com.example.fairsplit.model.dto.Expense
import com.example.fairsplit.model.dto.Group
import com.example.fairsplit.model.dto.UserProfile
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.util.UUID

class FirestoreRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    private fun users() = db.collection("users")
    private fun groups() = db.collection("groups")
    private fun expenses(groupId: String) = groups().document(groupId).collection("expenses")

    fun currentUid(): String = auth.currentUser?.uid ?: ""

    // ---- User profile ----
    suspend fun saveUserProfile(profile: UserProfile) {
        if (profile.uid.isBlank()) return
        users().document(profile.uid).set(profile).await()
    }

    // Added for Settings screen
    suspend fun getUserProfile(uid: String = currentUid()): UserProfile? {
        if (uid.isBlank()) return null
        val snap = users().document(uid).get().await()
        return snap.toObject(UserProfile::class.java)
    }

    // ---- Groups ----
    suspend fun createGroup(name: String): Group {
        val id = UUID.randomUUID().toString()
        val g = Group(id = id, name = name, members = listOf(currentUid()))
        groups().document(id).set(g).await()
        return g
    }

    suspend fun myGroups(): List<Group> {
        val uid = currentUid()
        if (uid.isBlank()) return emptyList()
        val snap = groups().whereArrayContains("members", uid).get().await()
        return snap.documents.mapNotNull { it.toObject(Group::class.java) }
    }

    suspend fun joinGroup(groupId: String) {
        val uid = currentUid()
        if (uid.isBlank()) return
        groups().document(groupId).update("members", FieldValue.arrayUnion(uid)).await()
    }

    // ---- Expenses ----
    suspend fun addExpense(groupId: String, e: Expense): Expense {
        val id = UUID.randomUUID().toString()
        val toSave = e.copy(id = id, groupId = groupId)
        expenses(groupId).document(id).set(toSave).await()
        return toSave
    }

    suspend fun listExpenses(groupId: String): List<Expense> {
        val snap = expenses(groupId).orderBy("date").get().await()
        return snap.documents.mapNotNull { it.toObject(Expense::class.java) }
    }
}
