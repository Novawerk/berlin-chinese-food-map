package com.novawerk.berlinfoodmap.domain.restaurant

data class Dish(
    val id: String,
    val name: String,
    val nameZh: String,
    val description: String? = null,
    val price: String? = null,
    val photoUrl: String? = null,
)
