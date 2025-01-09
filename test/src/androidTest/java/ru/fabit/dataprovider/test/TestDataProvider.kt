package ru.fabit.dataprovider.test

import io.realm.kotlin.Realm
import io.realm.kotlin.RealmConfiguration
import junit.framework.TestCase.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonTransformingSerializer
import kotlinx.serialization.serializer
import org.json.JSONArray
import org.json.JSONObject
import org.json.JSONTokener
import org.junit.Before
import org.junit.Test
import ru.fabit.dataprovider.EntityDataProviderImpl
import ru.fabit.dataprovider.ImportRequest
import ru.fabit.dataprovider.Mapper
import ru.fabit.dataprovider.SyncRequest
import ru.fabit.dataprovider.local.Aggregate
import ru.fabit.dataprovider.localservice.LocalServiceRealm
import ru.fabit.dataprovider.remote.RemoteServiceConfig
import ru.fabit.dataprovider.remote.RequestMethod
import ru.fabit.dataprovider.remoteservice.DefaultRemoteServiceErrorHandler
import ru.fabit.dataprovider.remoteservice.RemoteServiceJsonConverter
import ru.fabit.dataprovider.remoteservice.factories.ClientFactory
import ru.fabit.remoteservicecoroutines.factories.RetrofitFactory
import kotlin.reflect.typeOf
import kotlin.time.Duration.Companion.seconds

class TestDataProvider {
    val configLocal = RealmConfiguration.create(setOf(PostRealm::class))
    val realm = Realm.open(configLocal)
    val localService = LocalServiceRealm(realm)

    val configRemote = object : RemoteServiceConfig {
        override val baseUrl = "https://jsonplaceholder.typicode.com"
        override val defaultHeaders = mapOf("content-type" to "application/json; charset=utf-8")
        override val connectTimeoutMillis = 5.seconds.inWholeMilliseconds
        override val readTimeoutMillis = 5.seconds.inWholeMilliseconds
        override val isLogEnabled = true
    }

    val remoteService = RemoteServiceJsonConverter(
        configRemote,
        RetrofitFactory(ClientFactory().create(configRemote)).getRetrofitBuilder(),
        DefaultRemoteServiceErrorHandler()
    )

    val dataProvider = EntityDataProviderImpl(
        localService,
        remoteService
    )

    val domainMapper = Mapper<String?, List<Post>> {
        val json = JSONTokener(it ?: return@Mapper listOf()).nextValue()

        val list = mutableListOf<Post>()
        if (json is JSONArray) {
            for (i in 0..<json.length()) {
                val jsonObject = json.getJSONObject(i)
                list.add(
                    Post(
                        id = jsonObject.getInt("id"),
                        userId = jsonObject.getInt("userId")
                    )
                )
            }
        } else {
            json as JSONObject
            list.add(
                Post(
                    id = json.getInt("id"),
                    userId = json.getInt("userId")
                )
            )
        }
        list
    }

    inline fun <reified T> jsonListDeserializer(): JsonTransformingSerializer<List<T>> {
        return object :
            JsonTransformingSerializer<List<T>>(ListSerializer(serializer(typeOf<T>()) as KSerializer<T>)) {
            override fun transformDeserialize(element: JsonElement): JsonElement {
                return if (element !is JsonArray)
                    JsonArray(listOf(element))
                else
                    element
            }
        }
    }

    val dataMapper = Mapper<String, List<PostRealm>?> {
        Json.decodeFromString(jsonListDeserializer(), it)
    }

    val dataToDomainMapper = Mapper<PostRealm, Post> {
        Post(
            id = it.id,
            userId = it.userId,
            title = it.title,
            body = it.body
        )
    }

    @Before
    fun before() {
        realm.writeBlocking {
            deleteAll()
        }
    }

    @Test
    fun testSave() = runBlocking {

        val remote: List<Post> = dataProvider.getRemoteList(
            ImportRequest(
                requestMethod = RequestMethod.GET,
                relativePath = "/posts",
                domainMapper = domainMapper,
                remoteDataMapper = dataMapper
            )
        )

        assertEquals(100, remote.size)
        assertEquals(2, remote[14].userId)

        val get15 = dataProvider.getLocalList(dataToDomainMapper) {
            "id == $0"(15)
        }.first().first()

        assertEquals(15, get15.id)
        assertEquals(2, get15.userId)

        val count = dataProvider.count(PostRealm::class).first()
        assertEquals(100, count)

        val max =
            dataProvider.getWithAggregation(PostRealm::class, aggregate = Aggregate.MAX by "userId")
                .first()
        assertEquals(10L, max)
    }

    @Test
    fun testSave2() = runBlocking {
        dataProvider.syncRemoteList(
            SyncRequest(
                requestMethod = RequestMethod.GET,
                relativePath = "/posts",
                dataMapper = dataMapper
            )
        )

        val count = dataProvider.count(PostRealm::class).first()
        assertEquals(100, count)

        val remote = dataProvider.getRemoteObject(
            ImportRequest(
                requestMethod = RequestMethod.GET,
                relativePath = "/posts/2",
                domainMapper = domainMapper,
                remoteDataMapper = dataMapper
            )
        )!!

        assertEquals(1, remote.size)
        assertEquals(1, remote[0].userId)

        val get = dataProvider.getLocalList(dataToDomainMapper) {
            "id == $0"(2)
        }.first().first()

        assertEquals(2, get.id)
        assertEquals(1, get.userId)

    }
}