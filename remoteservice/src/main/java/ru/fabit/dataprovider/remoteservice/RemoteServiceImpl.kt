package ru.fabit.dataprovider.remoteservice

import android.os.Looper
import kotlinx.coroutines.CancellationException
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.Retrofit
import ru.fabit.dataprovider.remote.RemoteService
import ru.fabit.dataprovider.remote.RemoteServiceConfig
import ru.fabit.dataprovider.remote.RemoteServiceConverter
import ru.fabit.dataprovider.remote.RequestMethod
import ru.fabit.dataprovider.remoteservice.internal.RequestBodyTransformer
import ru.fabit.dataprovider.remoteservice.internal.RetrofitApi
import ru.fabit.error.AuthFailureException
import ru.fabit.error.NoNetworkConnectionException
import ru.fabit.error.RemoteServiceError
import ru.fabit.error.RequestTimeoutError
import java.io.IOException
import java.net.SocketTimeoutException

open class RemoteServiceImpl(
    override val config: RemoteServiceConfig,
    private val retrofitBuilder: Retrofit.Builder,
    private val errorHandler: RemoteServiceErrorHandler,
    private val converter: RemoteServiceConverter<*>
) : RemoteService {

    private val requestBodyTransformer = RequestBodyTransformer()

    private val retrofit by lazy {
        retrofitBuilder
            .baseUrl(config.baseUrl)
            .build()
    }
    private val api by lazy { retrofit.create(RetrofitApi::class.java) }

    override suspend fun getRemote(
        requestMethod: RequestMethod,
        relativePath: String,
        baseUrl: String?,
        params: Map<String, Any>?,
        headers: Map<String, String>?
    ): Any {
        return getRemoteInner(
            requestMethod = requestMethod,
            relativePath = relativePath,
            baseUrl = baseUrl ?: config.baseUrl,
            params = params ?: mapOf(),
            headers = headers ?: mapOf(),
            mapResult = true
        )!!
    }

    override suspend fun getRemoteRaw(
        requestMethod: RequestMethod,
        relativePath: String,
        baseUrl: String?,
        params: Map<String, Any>?,
        headers: Map<String, String>?
    ): String? {
        return getRemoteInner(
            requestMethod = requestMethod,
            relativePath = relativePath,
            baseUrl = baseUrl ?: config.baseUrl,
            params = params ?: mapOf(),
            headers = headers ?: mapOf(),
            mapResult = false
        ) as? String
    }

    private suspend fun getRemoteInner(
        requestMethod: RequestMethod,
        relativePath: String,
        baseUrl: String,
        params: Map<String, Any>,
        headers: Map<String, String>,
        mapResult: Boolean
    ): Any? {
        if (Looper.getMainLooper() == Looper.myLooper())
            throw IllegalThreadStateException()

        val result: Any?
        val url = baseUrl.plus(relativePath)
        val body = requestBodyTransformer.getRequestBody(params)
        try {
            val response = when (requestMethod) {
                RequestMethod.GET -> api.getObject(url, headers, params)
                RequestMethod.PUT -> api.putObject(url, headers, body)
                RequestMethod.POST -> api.postObject(url, headers, body)
                RequestMethod.DELETE -> api.deleteObject(url, headers, params)
                RequestMethod.PATCH -> api.patchObject(url, headers, body)

                else -> throw Throwable("No requestMethod")
            }
            result = mapResponse(response, relativePath, mapResult)
        } catch (t: Throwable) {
            val throwable = onError(t)
            throw throwable
        }
        return result
    }

    private fun onError(t: Throwable): Throwable {
        return when (t) {
            is SocketTimeoutException -> RequestTimeoutError(t.message)
            is IllegalThreadStateException -> IllegalThreadStateException(t.message)
            is IOException -> NoNetworkConnectionException(t.message)
            is AuthFailureException -> t
            is RemoteServiceError -> t
            is CancellationException -> t
            else -> RuntimeException(t.message)
        }
    }

    private fun mapResponse(
        response: Response<ResponseBody>?,
        relativePath: String?,
        mapResult: Boolean
    ): Any? {
        val body = response?.body()
        return response?.code()?.let { code ->
            when (code) {
                in 200..299 -> {
                    if (mapResult)
                        converter.mapResponse(body?.string())
                    else
                        body?.string()
                }

                401,
                403 -> {
                    val remoteError =
                        errorHandler.getError(code, response.errorBody()?.string())
                    val message = remoteError?.detailMessage ?: response.message() ?: ""
                    val error = AuthFailureException(message, code)
                    errorHandler.handleError(error, relativePath)
                    throw error
                }

                in 400..599 -> {
                    val remoteError =
                        errorHandler.getError(code, response.errorBody()?.string())
                    val error = remoteError ?: RemoteServiceError(
                        errorCode = code,
                        detailMessage = response.message()
                    )
                    errorHandler.handleError(error, relativePath)
                    throw error
                }

                else -> {
                    val error = RuntimeException("Unexpected response $response")
                    errorHandler.handleError(error, relativePath)
                    throw error
                }
            }
        }
    }
}
