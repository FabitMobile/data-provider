package ru.fabit.dataprovider

fun interface Mapper<in InputType, out ReturnType> {
    fun map(value: InputType): ReturnType

    fun map(value: List<InputType>): List<ReturnType> {
        return value.map(::map)
    }

    companion object {
        @Suppress("UNCHECKED_CAST")
        fun <InputType, ReturnType> asIs() = Mapper<InputType, ReturnType> {
            it as ReturnType
        }
    }
}