package ru.fabit.dataprovider.remote

fun interface RemoteServiceConverter<T> {
    fun mapResponse(rawResponse: String?): T?
}