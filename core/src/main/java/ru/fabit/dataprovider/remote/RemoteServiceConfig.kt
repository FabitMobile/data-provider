package ru.fabit.dataprovider.remote

interface RemoteServiceConfig {
    val baseUrl: String
    val defaultHeaders: Map<String, String>
    val connectTimeoutMillis: Long
    val readTimeoutMillis: Long
    val isLogEnabled: Boolean
}