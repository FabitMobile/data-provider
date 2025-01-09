package ru.fabit.dataprovider.localservice

import io.realm.kotlin.Realm
import io.realm.kotlin.UpdatePolicy
import io.realm.kotlin.query.RealmQuery
import io.realm.kotlin.schema.RealmStorageType
import io.realm.kotlin.types.RealmObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import ru.fabit.dataprovider.local.Aggregate
import ru.fabit.dataprovider.local.AggregationPair
import ru.fabit.dataprovider.local.LocalService
import ru.fabit.dataprovider.local.Query
import ru.fabit.dataprovider.local.QueryScope
import ru.fabit.dataprovider.local.Sort
import ru.fabit.dataprovider.local.SortPair
import kotlin.reflect.KClass

open class LocalServiceRealm(
    private val realm: Realm
) : LocalService<RealmObject> {

    override val context = Dispatchers.IO

    //region ======================== Inline support ========================

    suspend inline fun <reified T : RealmObject> get(query: Query? = null, sort: SortPair? = null) =
        get(T::class, query, sort)

    suspend inline fun <reified T : RealmObject> get(
        sort: SortPair? = null,
        query: QueryScope.() -> Unit,
    ) = get(T::class, ru.fabit.dataprovider.local.query(query), sort)

    suspend inline fun <reified T : RealmObject> update(
        query: Query? = null,
        noinline action: (T) -> Unit
    ) = update(T::class, query, action)

    suspend inline fun <reified T : RealmObject> update(
        query: QueryScope.() -> Unit,
        noinline action: (T) -> Unit
    ) = update(T::class, ru.fabit.dataprovider.local.query(query), action)

    suspend inline fun <reified T : RealmObject> delete(
        query: Query? = null
    ) = delete(T::class, query)

    suspend inline fun <reified T : RealmObject> delete(
        query: QueryScope.() -> Unit
    ) = delete(T::class, ru.fabit.dataprovider.local.query(query))

    suspend inline fun <reified T : RealmObject> deleteAndStoreObjects(
        values: List<T>,
        query: QueryScope.() -> Unit
    ) = deleteAndStoreObjects(T::class, ru.fabit.dataprovider.local.query(query), values)

    suspend inline fun <reified T : RealmObject> deleteAndStoreObjects(
        values: List<T>
    ) = deleteAndStoreObjects(T::class, query = null, values)

    suspend inline fun <reified T : RealmObject> count(
        query: Query? = null
    ) = count(T::class, query)

    suspend inline fun <reified T : RealmObject> count(
        query: QueryScope.() -> Unit
    ) = count(T::class, ru.fabit.dataprovider.local.query(query))

    /**
     * @return [Byte], [Char], [Short], [Int] types as [Long]
     *
     * [Float] and [Double] as is
     */
    suspend inline fun <reified T : RealmObject> getWithAggregation(
        aggregate: AggregationPair
    ) = getWithAggregation(T::class, query = null, aggregate)

    /**
     * @return [Byte], [Char], [Short], [Int] types as [Long]
     *
     * [Float] and [Double] as is
     */
    suspend inline fun <reified T : RealmObject> getWithAggregation(
        aggregate: AggregationPair,
        query: QueryScope.() -> Unit
    ) = getWithAggregation(T::class, ru.fabit.dataprovider.local.query(query), aggregate)

    //endregion

    override suspend fun <T : RealmObject> get(
        clazz: KClass<T>,
        query: Query?,
        sort: SortPair?
    ): Flow<List<T>> {
        val flow: Flow<List<T>>

        withContext(context) {
            var predicate = query.build(clazz)

            if (sort != null)
                predicate = predicate.sort(sort.field, sort.order())

            flow = predicate.asFlow().map { it.list }

        }
        return flow.flowOn(context)
    }

    /**
     * @return [Byte], [Char], [Short], [Int] types as [Long]
     *
     * [Float] and [Double] as is
     */
    override suspend fun <T : RealmObject> getWithAggregation(
        clazz: KClass<T>,
        query: Query?,
        aggregate: AggregationPair
    ): Flow<Number?> {
        val flow: Flow<Number?>

        withContext(context) {
            val predicate = query.build(clazz)

            val type =
                realm.schema()[clazz.simpleName!!]?.get(aggregate.field)?.type?.storageType
            val fieldType = when (type) {
                RealmStorageType.FLOAT -> Float::class
                RealmStorageType.DOUBLE -> Double::class
                RealmStorageType.INT -> Long::class
                else -> if (aggregate.function == Aggregate.SIZE)
                    Long::class
                else
                    throw IllegalArgumentException("aggregationField must be one of (Float, Double, Long), but was ${type?.name}")
            }

            flow = when (aggregate.function) {
                Aggregate.MAX -> predicate.max(aggregate.field, fieldType)
                Aggregate.MIN -> predicate.min(aggregate.field, fieldType)
                Aggregate.SUM -> predicate.sum(aggregate.field, fieldType)
                else -> predicate.count()
            }.asFlow()
        }
        return flow.flowOn(context)
    }

    override suspend fun <T : RealmObject> count(clazz: KClass<T>, query: Query?): Flow<Int> {
        val flow: Flow<Int>

        withContext(context) {
            val predicate = query.build(clazz)

            flow = predicate.count().asFlow().map { it.toInt() }
        }
        return flow.flowOn(context)
    }

    override suspend fun <T : RealmObject> storeObject(value: T) {
        withContext(context) {
            realm.write {
                copyToRealm(value, UpdatePolicy.ALL)
            }
        }
    }

    override suspend fun <T : RealmObject> storeObjects(values: List<T>) {
        withContext(context) {
            realm.write {
                for (value in values)
                    copyToRealm(value as RealmObject, UpdatePolicy.ALL)
            }
        }
    }

    override suspend fun <T : RealmObject> update(
        clazz: KClass<T>,
        query: Query?,
        action: (T) -> Unit
    ) {
        withContext(context) {
            val predicate = query.build(clazz)

            val results = predicate.find()
            realm.write {
                for (realmObject in results)
                    findLatest(realmObject)?.let(action)
            }
        }
    }

    override suspend fun <T : RealmObject> delete(clazz: KClass<T>, query: Query?) {
        withContext(context) {
            if (query == null) {
                realm.write {
                    delete(clazz)
                }
            } else {
                val predicate = query.build(clazz)

                val results = predicate.find()
                if (results.isNotEmpty())
                    realm.write {
                        for (realmObject in results)
                            findLatest(realmObject)?.let { obj ->
                                delete(obj)
                            }
                    }
            }
        }
    }

    override suspend fun <T : RealmObject> deleteAndStoreObjects(
        clazz: KClass<T>,
        query: Query?,
        values: List<T>
    ) {
        withContext(context) {
            if (query == null) {
                realm.write {
                    delete(clazz)
                    for (value in values)
                        copyToRealm(value)
                }
            } else {
                val predicate = query.build(clazz)

                val results = predicate.find()
                if (results.isNotEmpty())
                    realm.write {
                        for (realmObject in results)
                            findLatest(realmObject)?.let { obj ->
                                delete(obj)
                            }
                        for (value in values)
                            copyToRealm(value)
                    }
            }
        }
    }

    private fun <T : RealmObject> Query?.build(clazz: KClass<T>): RealmQuery<T> {
        return if (this == null)
            realm.query(clazz)
        else
            realm.query(clazz, this.query, *(this.args ?: arrayOf()))
    }

    private fun SortPair.order() = when (order) {
        Sort.ASCENDING -> io.realm.kotlin.query.Sort.ASCENDING
        Sort.DESCENDING -> io.realm.kotlin.query.Sort.DESCENDING
    }
}