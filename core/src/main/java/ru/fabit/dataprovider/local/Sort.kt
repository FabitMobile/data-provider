package ru.fabit.dataprovider.local

enum class Sort(private val text: String) {
    ASCENDING("asc"),
    DESCENDING("desc");

    override fun toString(): String {
        return text
    }

    infix fun by(fieldName: String) = SortPair(fieldName, this)
}

data class SortPair(
    val field: String,
    val order: Sort
)
