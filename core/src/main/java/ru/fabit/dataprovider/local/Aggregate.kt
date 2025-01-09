package ru.fabit.dataprovider.local

enum class Aggregate(private val text: String) {
    MAX("max"),
    MIN("min"),
    SUM("sum"),
    SIZE("size");

    override fun toString(): String {
        return text
    }

    infix fun by(fieldName: String) = AggregationPair(fieldName, this)

    companion object {
        val size: AggregationPair
            get() = AggregationPair("", SIZE)
    }
}

data class AggregationPair(
    val field: String,
    val function: Aggregate
)