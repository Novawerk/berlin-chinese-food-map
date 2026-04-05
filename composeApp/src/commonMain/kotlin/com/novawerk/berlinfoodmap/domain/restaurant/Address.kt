package com.novawerk.berlinfoodmap.domain.restaurant

data class Address(
    val addressLine1: String,
    val addressLine2: String? = null,
    val note: String? = null,
    val postalCode: String,
    val district: String,
    val city: String = "Berlin",
    val country: String = "Germany",
)
