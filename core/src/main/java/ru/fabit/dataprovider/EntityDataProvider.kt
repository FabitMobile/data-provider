package ru.fabit.dataprovider

import kotlinx.coroutines.flow.Flow
import ru.fabit.dataprovider.local.AggregationPair
import ru.fabit.dataprovider.local.Query
import ru.fabit.dataprovider.local.SortPair
import kotlin.reflect.KClass

interface EntityDataProvider<Entity : Any> {

    suspend fun <Domain> getRemoteObject(importRequest: RemoteRequest<Domain>): Domain?

    suspend fun <Domain> getRemoteList(importRequest: RemoteRequest<List<Domain>>): List<Domain>

    suspend fun syncRemoteObject(syncRequest: SyncRequest<Entity>)

    suspend fun syncRemoteList(syncRequest: SyncRequest<List<Entity>>)

    suspend fun <Data : Entity, Domain : Any> getLocalList(
        clazz: KClass<Data>,
        query: Query? = null,
        sort: SortPair? = null,
        dataToDomainMapper: Mapper<Data, Domain> = Mapper.asIs()
    ): Flow<List<Domain>>

    suspend fun <Data : Entity> getWithAggregation(
        clazz: KClass<Data>,
        query: Query? = null,
        aggregate: AggregationPair
    ): Flow<Number?>

    suspend fun <Data : Entity> count(
        clazz: KClass<Data>,
        query: Query? = null,
    ): Flow<Int>

    suspend fun <Data : Entity> update(
        clazz: KClass<Data>,
        query: Query? = null,
        action: (Data) -> Unit
    )

    suspend fun <Data : Entity> delete(
        clazz: KClass<Data>,
        query: Query? = null,
    )
}