package ru.fabit.dataprovider

import ru.fabit.dataprovider.remote.RequestMethod

data class ImportRequest<Data, Domain>(
    override val requestMethod: RequestMethod,
    override val relativePath: String,
    val domainMapper: Mapper<String?, Domain?>,
    val remoteDataMapper: Mapper<String, Data?>,
    override val params: Map<String, Any>? = null,
    override val headers: Map<String, String>? = null,
    override val baseUrl: String? = null,
) : RemoteRequest<Domain> {
    override val dataMapper: Mapper<String, Domain>
        get() = remoteDataMapper as  Mapper<String, Domain>
}

data class SyncRequest<Data>(
    override val requestMethod: RequestMethod,
    override val relativePath: String,
    override val dataMapper: Mapper<String, Data?>,
    override val params: Map<String, Any>? = null,
    override val headers: Map<String, String>? = null,
    override val baseUrl: String? = null,
) : RemoteRequest<Data>

sealed interface RemoteRequest<Data> {
    val requestMethod: RequestMethod
    val relativePath: String
    val dataMapper: Mapper<String, Data?>
    val params: Map<String, Any>?
    val headers: Map<String, String>?
    val baseUrl: String?
}