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
        // Ensure offline cache is on (safe to set once)
        try {
            val settings = FirebaseFirestoreSettings.Builder()
                .setPersistenceEnabled(true)
                .build()
            if (db.firestoreSettings != settings) {
                db.firestoreSettings = settings
            }
        } catch (_: Exception) {
            // ignore if settings already applied
        }
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

    /** Prefer SERVER, else fall back to CACHE. */
    suspend fun getUserProfile(uid: String = currentUid()): UserProfile? {
        if (uid.isBlank()) return null
        return try {
            val snap = users().document(uid).get(Source.SERVER).await()
            snap.toObject(UserProfile::class.java)
        } catch (e: Exception) {
            Log.w("FirestoreRepo", "getUserProfile server failed, using cache: ${e.message}")
            val cached = users().document(uid).get(Source.CACHE).await()
            cached.toObject(UserProfile::class.java)
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
            val qs = groups().whereArrayContains("members", uid)
                .get(Source.SERVER).await()
            qs.documents.mapNotNull { it.toObject(Group::class.java) }
        } catch (e: Exception) {
            Log.w("FirestoreRepo", "myGroups server failed, using cache: ${e.message}")
            val qs = groups().whereArrayContains("members", uid)
                .get(Source.CACHE).await()
            qs.documents.mapNotNull { it.toObject(Group::class.java) }
        }
    }

    suspend fun joinGroup(groupId: String) {
        val uid = currentUid()
        if (uid.isBlank()) return
        groups().document(groupId)
            .update("members", FieldValue.arrayUnion(uid))
            .await()
    }

    // ------------------ EXPENSES ------------------

    suspend fun addExpense(groupId: String, e: Expense): Expense {
        val id = UUID.randomUUID().toString()
        val toSave = e.copy(id = id, groupId = groupId)
        expenses(groupId).document(id).set(toSave).await()
        return toSave
    }

    /** Prefer SERVER, else CACHE. */
    suspend fun listExpenses(groupId: String): List<Expense> {
        return try {
            val qs = expenses(groupId).get(Source.SERVER).await()
            qs.documents.mapNotNull { it.toObject(Expense::class.java) }
        } catch (e: Exception) {
            Log.w("FirestoreRepo", "listExpenses server failed, using cache: ${e.message}")
            val qs = expenses(groupId).get(Source.CACHE).await()
            qs.documents.mapNotNull { it.toObject(Expense::class.java) }
        }
    }
}
