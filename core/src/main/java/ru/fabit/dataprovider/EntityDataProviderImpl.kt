package ru.fabit.dataprovider

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import ru.fabit.dataprovider.local.AggregationPair
import ru.fabit.dataprovider.local.LocalService
import ru.fabit.dataprovider.local.Query
import ru.fabit.dataprovider.local.QueryScope
import ru.fabit.dataprovider.local.SortPair
import ru.fabit.dataprovider.remote.RemoteService
import kotlin.reflect.KClass

open class EntityDataProviderImpl<Entity : Any>(
    protected open val localService: LocalService<Entity>,
    protected open val remoteService: RemoteService
) : EntityDataProvider<Entity> {

    //region ======================== Inline support ========================

    suspend inline fun <reified Data : Entity, Domain : Any> getLocalList(
        dataToDomainMapper: Mapper<Data, Domain>,
        query: Query? = null,
        sort: SortPair? = null
    ) = getLocalList(Data::class, query, sort, dataToDomainMapper)

    suspend inline fun <reified Data : Entity, Domain : Any> getLocalList(
        dataToDomainMapper: Mapper<Data, Domain>,
        sort: SortPair? = null,
        query: QueryScope.() -> Unit
    ) = getLocalList(
        Data::class,
        ru.fabit.dataprovider.local.query(query),
        sort,
        dataToDomainMapper
    )

    suspend inline fun <reified T : Entity> update(
        query: Query? = null,
        noinline action: (T) -> Unit
    ) = update(T::class, query, action)

    suspend inline fun <reified T : Entity> update(
        query: QueryScope.() -> Unit,
        noinline action: (T) -> Unit
    ) = update(T::class, ru.fabit.dataprovider.local.query(query), action)

    suspend inline fun <reified T : Entity> delete(
        query: Query? = null
    ) = delete(T::class, query)

    suspend inline fun <reified T : Entity> delete(
        query: QueryScope.() -> Unit
    ) = delete(T::class, ru.fabit.dataprovider.local.query(query))

    suspend inline fun <reified T : Entity> count(
        query: Query? = null
    ) = count(T::class, query)

    suspend inline fun <reified T : Entity> count(
        query: QueryScope.() -> Unit
    ) = count(T::class, ru.fabit.dataprovider.local.query(query))

    suspend inline fun <reified T : Entity> getWithAggregation(
        aggregate: AggregationPair
    ) = getWithAggregation(T::class, query = null, aggregate)

    suspend inline fun <reified T : Entity> getWithAggregation(
        aggregate: AggregationPair,
        query: QueryScope.() -> Unit
    ) = getWithAggregation(T::class, ru.fabit.dataprovider.local.query(query), aggregate)

    //endregion

    override suspend fun <Domain> getRemoteObject(importRequest: RemoteRequest<Domain>): Domain? {
        val request = prepareRequest(importRequest as ImportRequest<Entity, Domain>)
        return getRemoteEntity(request)
    }

    override suspend fun <Domain> getRemoteList(importRequest: RemoteRequest<List<Domain>>): List<Domain> {
        val request = prepareRequest(importRequest as ImportRequest<Entity, List<Domain>>)
        return getRemoteEntity(request) ?: listOf()
    }

    override suspend fun syncRemoteObject(syncRequest: SyncRequest<Entity>) {
        syncRemoteEntity(prepareRequest(syncRequest))
    }

    override suspend fun syncRemoteList(syncRequest: SyncRequest<List<Entity>>) {
        syncRemoteEntity(prepareRequest(syncRequest))
    }

    protected open fun <T, V : RemoteRequest<T>> prepareRequest(request: V): V {
        return request
    }

    protected open suspend fun <Data> syncRemoteEntity(syncRequest: SyncRequest<Data>) {
        val remoteString = remoteService.getRemoteRaw(
            requestMethod = syncRequest.requestMethod,
            relativePath = syncRequest.relativePath,
            baseUrl = syncRequest.baseUrl,
            params = syncRequest.params,
            headers = syncRequest.headers
        )
        if (remoteString != null) {
            val mappedData = syncRequest.dataMapper.map(remoteString)
            if (mappedData != null) {
                if (mappedData is List<*>)
                    localService.storeObjects(mappedData as List<Entity>)
                else
                    localService.storeObject(mappedData as Entity)
            }
        }
    }

    protected open suspend fun <Domain> getRemoteEntity(importRequest: ImportRequest<Entity, Domain>): Domain? {
        val dataMapper = importRequest.remoteDataMapper
        val domainMapper = importRequest.domainMapper

        val remoteString = remoteService.getRemoteRaw(
            requestMethod = importRequest.requestMethod,
            relativePath = importRequest.relativePath,
            baseUrl = importRequest.baseUrl,
            params = importRequest.params,
            headers = importRequest.headers
        )
        if (remoteString != null) {
            val mappedData = dataMapper.map(remoteString)
            if (mappedData != null) {
                if (mappedData is List<*>)
                    localService.storeObjects(mappedData as List<Entity>)
                else
                    localService.storeObject(mappedData)
            }
        }
        val domain = domainMapper.map(remoteString)
        return domain
    }

    override suspend fun <Data : Entity, Domain : Any> getLocalList(
        clazz: KClass<Data>,
        query: Query?,
        sort: SortPair?,
        dataToDomainMapper: Mapper<Data, Domain>
    ): Flow<List<Domain>> {
        return localService.get(clazz, query, sort).map {
            dataToDomainMapper.map(it)
        }
    }

    override suspend fun <Data : Entity> getWithAggregation(
        clazz: KClass<Data>,
        query: Query?,
        aggregate: AggregationPair
    ): Flow<Number?> {
        return localService.getWithAggregation(clazz, query, aggregate)
    }

    override suspend fun <Data : Entity> count(
        clazz: KClass<Data>,
        query: Query?,
    ): Flow<Int> {
        return localService.count(clazz, query)
    }

    override suspend fun <Data : Entity> update(
        clazz: KClass<Data>,
        query: Query?,
        action: (Data) -> Unit
    ) {
        localService.update(clazz, query, action)
    }

    override suspend fun <Data : Entity> delete(
        clazz: KClass<Data>,
        query: Query?,
    ) {
        localService.delete(clazz, query)
    }
}