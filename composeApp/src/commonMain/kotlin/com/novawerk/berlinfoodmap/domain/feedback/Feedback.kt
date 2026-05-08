package com.novawerk.berlinfoodmap.domain.feedback

enum class FeedbackCategory {
    BUG,
    IDEA,
    RESTAURANT,
    OTHER;

    val firestoreValue: String
        get() = when (this) {
            BUG -> "bug"
            IDEA -> "idea"
            RESTAURANT -> "restaurant"
            OTHER -> "other"
        }
}
