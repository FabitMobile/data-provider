package ru.fabit.dataprovider.local

import kotlinx.coroutines.flow.Flow
import kotlin.coroutines.CoroutineContext
import kotlin.reflect.KClass

interface LocalService<Bound : Any> {
    val context: CoroutineContext

    suspend fun <T : Bound> get(
        clazz: KClass<T>,
        query: Query? = null,
        sort: SortPair? = null
    ): Flow<List<T>>

    suspend fun <T : Bound> getWithAggregation(
        clazz: KClass<T>,
        query: Query? = null,
        aggregate: AggregationPair
    ): Flow<Number?>

    suspend fun <T : Bound> count(
        clazz: KClass<T>,
        query: Query? = null,
    ): Flow<Int>

    suspend fun <T : Bound> storeObject(value: T)

    suspend fun <T : Bound> storeObjects(values: List<T>)

    suspend fun <T : Bound> update(
        clazz: KClass<T>,
        query: Query? = null,
        action: (T) -> Unit
    )

    suspend fun <T : Bound> delete(
        clazz: KClass<T>,
        query: Query? = null,
    )

    suspend fun <T : Bound> deleteAndStoreObjects(
        clazz: KClass<T>,
        query: Query? = null,
        values: List<T>
    )
}