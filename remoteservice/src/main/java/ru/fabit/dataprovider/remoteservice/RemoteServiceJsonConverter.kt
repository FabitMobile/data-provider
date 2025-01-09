package ru.fabit.dataprovider.remoteservice

import org.json.JSONObject
import retrofit2.Retrofit
import ru.fabit.dataprovider.remote.RemoteServiceConfig
import ru.fabit.dataprovider.remote.RequestMethod
import ru.fabit.dataprovider.remoteservice.converter.JsonConverter

class RemoteServiceJsonConverter(
    config: RemoteServiceConfig,
    retrofitBuilder: Retrofit.Builder,
    errorHandler: RemoteServiceErrorHandler
) : RemoteServiceImpl(
    config,
    retrofitBuilder,
    errorHandler,
    JsonConverter()
) {
    override suspend fun getRemote(
        requestMethod: RequestMethod,
        relativePath: String,
        baseUrl: String?,
        params: Map<String, Any>?,
        headers: Map<String, String>?
    ): JSONObject {
        return super.getRemote(requestMethod, relativePath, baseUrl, params, headers) as JSONObject
    }
}