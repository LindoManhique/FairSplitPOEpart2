package com.example.fairsplit

import com.example.fairsplit.util.SplitCalculator
import org.junit.Assert.assertEquals
import org.junit.Test

class SplitCalculatorTest {

    @Test
    fun equalSplit_isCorrect() {
        val owed = SplitCalculator.equalOwedPerPerson(total = 300.0, count = 3)
        assertEquals(100.0, owed, 0.0)
    }

    @Test
    fun zeroPeople_returnsZero() {
        val owed = SplitCalculator.equalOwedPerPerson(total = 50.0, count = 0)
        assertEquals(0.0, owed, 0.0)
    }
}
