package com.bennybokki.frientrip.data

data class SharedExpense(
    val id: String = "",
    val description: String = "",
    val amount: Double = 0.0,
    val category: String = "misc",        // "supply" or "misc"
    val splitMethod: String = "even",      // "even" or "byNights"
    val submittedByUid: String = "",
    val submittedByName: String = "",
    val approved: Boolean = false,
    val linkedSupplyId: String? = null,
    val createdAt: Long = 0L
)
