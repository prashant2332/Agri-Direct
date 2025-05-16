package com.example.finalyearprojectwithfirebase.model


data class MandiRecord(
    val state: String,
    val district: String,
    val market: String,
    val commodity: String,
    val variety: String,
    val grade: String,
    val arrival_date: String,
    val min_price: Double,
    val max_price: Double,
    val modal_price: Double
)

