package com.example.fairsplit.model.dto

data class Expense(
    val id: String = "",
    val groupId: String = "",
    val title: String = "",
    val amount: Double = 0.0,
    val payerUid: String = "",
    val participants: List<String> = emptyList(),   // <-- needed by your code
    val date: Long = System.currentTimeMillis()     // <-- Firestore ordered by "date"
)
