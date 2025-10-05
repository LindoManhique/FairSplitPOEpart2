package com.example.fairsplit.model.dto

data class Group(
    val id: String = "",
    val name: String = "",
    val members: List<String> = emptyList()
)
