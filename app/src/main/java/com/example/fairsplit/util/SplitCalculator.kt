package com.example.fairsplit.util

object SplitCalculator {
    fun equalOwedPerPerson(total: Double, count: Int): Double {
        if (count <= 0) return 0.0
        return kotlin.math.round(total / count * 100) / 100.0
    }
}
