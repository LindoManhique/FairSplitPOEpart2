package com.example.fairsplit.model.remote

import android.util.Log
import com.example.fairsplit.model.dto.Expense
import com.example.fairsplit.model.dto.Group
import com.example.fairsplit.model.dto.UserProfile
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.firestore.Source
import kotlinx.coroutines.tasks.await
import java.util.UUID

class FirestoreRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    init {
        // Enable offline cache (safe if called more than once)
        try {
            val settings = FirebaseFirestoreSettings.Builder()
                .setPersistenceEnabled(true)
                .build()
            if (db.firestoreSettings != settings) db.firestoreSettings = settings
        } catch (_: Exception) { /* already set */ }
    }

    private fun users() = db.collection("users")
    private fun groups() = db.collection("groups")
    private fun expenses(groupId: String) = groups().document(groupId).collection("expenses")

    fun currentUid(): String = auth.currentUser?.uid ?: ""

    // ------------------ USER PROFILE ------------------

    suspend fun saveUserProfile(profile: UserProfile) {
        if (profile.uid.isBlank()) return
        users().document(profile.uid).set(profile).await()
    }

    /** Prefer SERVER, fall back to CACHE. */
    suspend fun getUserProfile(uid: String = currentUid()): UserProfile? {
        if (uid.isBlank()) return null
        return try {
            users().document(uid).get(Source.SERVER).await()
                .toObject(UserProfile::class.java)
        } catch (e: Exception) {
            Log.w("FirestoreRepo", "getUserProfile server failed: ${e.message}; using cache")
            users().document(uid).get(Source.CACHE).await()
                .toObject(UserProfile::class.java)
        }
    }

    // ------------------ GROUPS ------------------

    suspend fun createGroup(name: String): Group {
        val id = UUID.randomUUID().toString()
        val g = Group(id = id, name = name, members = listOf(currentUid()))
        groups().document(id).set(g).await()
        return g
    }

    /** Prefer SERVER, else CACHE. Returns [] if not signed in. */
    suspend fun myGroups(): List<Group> {
        val uid = currentUid()
        if (uid.isBlank()) return emptyList()
        return try {
            groups().whereArrayContains("members", uid).get(Source.SERVER).await()
                .documents.mapNotNull { it.toObject(Group::class.java) }
        } catch (e: Exception) {
            Log.w("FirestoreRepo", "myGroups server failed: ${e.message}; using cache")
            groups().whereArrayContains("members", uid).get(Source.CACHE).await()
                .documents.mapNotNull { it.toObject(Group::class.java) }
        }
    }

    suspend fun joinGroup(groupId: String) {
        val uid = currentUid()
        if (uid.isBlank()) return
        groups().document(groupId)
            .update("members", FieldValue.arrayUnion(uid))
            .await()
    }

    /** NEW: Delete a group and its expenses (best-effort cascade). */
    suspend fun deleteGroup(groupId: String) {
        // Delete subcollection 'expenses' first (best effort), then the group doc.
        try {
            val docs = expenses(groupId).get(Source.SERVER).await().documents
            // If server fails (offline), attempt cache:
            val toDelete = if (docs.isNotEmpty()) docs else runCatching {
                expenses(groupId).get(Source.CACHE).await().documents
            }.getOrDefault(emptyList())

            // Firestore doesn't support true batched subcollection deletes cross-collection in one atomic op,
            // so we do sequential deletes. This is fine for a student app / small lists.
            for (d in toDelete) {
                expenses(groupId).document(d.id).delete().await()
            }
        } catch (e: Exception) {
            Log.w("FirestoreRepo", "deleteGroup: failed to list/delete expenses: ${e.message} — continuing")
        }

        // Finally delete the group document
        groups().document(groupId).delete().await()
    }

    // ------------------ EXPENSES ------------------

    suspend fun addExpense(groupId: String, e: Expense): Expense {
        val id = UUID.randomUUID().toString()
        val toSave = e.copy(id = id, groupId = groupId)
        expenses(groupId).document(id).set(toSave).await()
        return toSave
    }

    /** Prefer SERVER, else CACHE. Safe if offline. */
    suspend fun listExpenses(groupId: String): List<Expense> {
        return try {
            expenses(groupId).get(Source.SERVER).await()
                .documents.mapNotNull { it.toObject(Expense::class.java) }
        } catch (e: Exception) {
            Log.w("FirestoreRepo", "listExpenses server failed: ${e.message}; using cache")
            expenses(groupId).get(Source.CACHE).await()
                .documents.mapNotNull { it.toObject(Expense::class.java) }
        }
    }

    /** Delete an expense; returns true on success, false otherwise. */
    suspend fun deleteExpense(groupId: String, expenseId: String): Boolean {
        return try {
            expenses(groupId).document(expenseId).delete().await()
            true
        } catch (e: Exception) {
            Log.w("FirestoreRepo", "deleteExpense failed: ${e.message}")
            false
        }
    }
}
