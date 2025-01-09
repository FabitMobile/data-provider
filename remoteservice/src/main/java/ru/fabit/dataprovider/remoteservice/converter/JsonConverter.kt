package ru.fabit.dataprovider.remoteservice.converter

import org.json.JSONObject
import org.json.JSONTokener
import ru.fabit.dataprovider.remote.RemoteServiceConverter

open class JsonConverter : RemoteServiceConverter<JSONObject> {
    override fun mapResponse(rawResponse: String?): JSONObject {
        val json = rawResponse ?: return JSONObject()
        return when (val item = JSONTokener(json).nextValue()) {
            is JSONObject -> item
            else -> JSONObject().apply {
                put(DEFAULT_KEY, item)
            }
        }
    }

    companion object {
        const val DEFAULT_KEY = "item"
    }
}