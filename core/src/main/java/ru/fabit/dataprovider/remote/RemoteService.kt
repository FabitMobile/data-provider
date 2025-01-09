package ru.fabit.dataprovider.remote

import ru.fabit.error.AuthFailureException
import ru.fabit.error.NoNetworkConnectionException
import ru.fabit.error.RemoteServiceError
import ru.fabit.error.RequestTimeoutError

interface RemoteService {

    val config: RemoteServiceConfig

    @Throws(
        NoNetworkConnectionException::class,
        IllegalThreadStateException::class,
        AuthFailureException::class,
        RequestTimeoutError::class,
        RemoteServiceError::class,
        RuntimeException::class
    )
    suspend fun getRemote(
        requestMethod: RequestMethod,
        relativePath: String,
        baseUrl: String? = null,
        params: Map<String, Any>? = null,
        headers: Map<String, String>? = null
    ): Any

    @Throws(
        NoNetworkConnectionException::class,
        IllegalThreadStateException::class,
        AuthFailureException::class,
        RequestTimeoutError::class,
        RemoteServiceError::class,
        RuntimeException::class
    )
    suspend fun getRemoteRaw(
        requestMethod: RequestMethod,
        relativePath: String,
        baseUrl: String? = null,
        params: Map<String, Any>? = null,
        headers: Map<String, String>? = null
    ): String?
}