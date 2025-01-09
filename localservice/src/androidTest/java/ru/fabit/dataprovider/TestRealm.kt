package ru.fabit.dataprovider

import io.realm.kotlin.Realm
import io.realm.kotlin.RealmConfiguration
import junit.framework.TestCase.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import ru.fabit.dataprovider.local.Aggregate
import ru.fabit.dataprovider.local.Sort
import ru.fabit.dataprovider.local.query
import ru.fabit.dataprovider.localservice.LocalServiceRealm

class TestRealm {
    val config = RealmConfiguration.create(setOf(Data::class))
    val realm = Realm.open(config)

    @Before
    fun before() {
        realm.writeBlocking {
            deleteAll()
        }
    }

    @Test
    fun testSave() = runBlocking {
        val localService = LocalServiceRealm(realm)

        val data = Data().apply {
            balance = 123
        }
        localService.storeObject(data)
        assertTrue(true)
        val savedData = localService.get<Data>().first().first()
        assertEquals(data, savedData)
    }

    @Test
    fun testUpdate() = runBlocking {
        val localService = LocalServiceRealm(realm)
        val data = Data().apply {
            balance = 123
        }
        localService.storeObject(data)
        assertTrue(true)
        val savedData = localService.get<Data>().first().first()
        assertEquals(data, savedData)

        val newBalance = 321L
        localService.update<Data> {
            it.balance = newBalance
        }
        val updatedData = localService.get<Data>().first().first()
        assertEquals(newBalance, updatedData.balance)
    }

    @Test
    fun testDelete() = runBlocking {
        val localService = LocalServiceRealm(realm)
        val data = Data().apply {
            balance = 123
        }
        localService.storeObject(data)
        assertTrue(true)
        val savedData = localService.get<Data>().first().first()
        assertEquals(data, savedData)

        localService.delete<Data>()
        assertEquals(0, localService.get<Data>().first().size)
    }

    @Test(timeout = 1000)
    fun testSaveObjects() = runBlocking {

        val localService = LocalServiceRealm(realm)
        val data1 = Data().apply {
            id = 1
            balance = 123
        }
        val data2 = Data().apply {
            id = 2
            balance = 456
        }
        val data3 = Data().apply {
            id = 3
            balance = 789
        }
        val flow = localService.get<Data>()
        val job = launch {
            var update = 0
            flow.collect {
                when (update) {
                    0 -> assertEquals(0, it.size)
                    1 -> assertEquals(3, it.size)

                    else -> assertTrue(false)
                }
                update++
            }
        }
        launch {
            delay(200)
            job.cancel()
        }
        delay(100)
        localService.storeObjects(
            listOf(
                data1,
                data2,
                data3
            )
        )
        assertTrue(true)
    }

    @Test(timeout = 1000)
    fun testQueryGet() = runBlocking {

        val localService = LocalServiceRealm(realm)
        val id = 2
        val balance = 456L
        val data1 = Data().apply {
            this.id = 1
            this.balance = 123
        }
        val data2 = Data().apply {
            this.id = id
            this.balance = balance
        }
        val data3 = Data().apply {
            this.id = 3
            this.balance = 789
        }
        val flow = localService.get<Data>("id == $0".query(id))
        var update = 0
        val job = launch {
            flow.collect {
                when (update) {
                    0 -> assertEquals(0, it.size)

                    1 -> {
                        assertEquals(1, it.size)
                        assertEquals(id, it.first().id)
                        assertEquals(balance, it.first().balance)
                    }

                    else -> assertTrue(false)
                }
                update++
            }
        }
        launch {
            delay(200)
            job.cancel()
        }
        delay(100)
        localService.storeObjects(
            listOf(
                data1,
                data2,
                data3
            )
        )
        delay(300)
        assertEquals(2, update)
    }

    @Test(timeout = 1000)
    fun testQueryGet2() = runBlocking {

        val localService = LocalServiceRealm(realm)
        val id = 2
        val balance = 456L
        val data1 = Data().apply {
            this.id = 1
            this.balance = 123
        }
        val data2 = Data().apply {
            this.id = id
            this.balance = balance
        }
        val data3 = Data().apply {
            this.id = 3
            this.balance = 789
        }
        val flow = localService.get<Data> { "id == $0 AND balance == $1"(id, 0) }
        var update = 0
        val job = launch {
            flow.collect {
                when (update) {
                    0 -> assertEquals(0, it.size)

                    else -> assertTrue(false)
                }
                update++
            }
        }
        launch {
            delay(200)
            job.cancel()
        }
        delay(100)
        localService.storeObjects(
            listOf(
                data1,
                data2,
                data3
            )
        )
        delay(300)
        assertEquals(1, update)
    }

    @Test(timeout = 1000)
    fun testQueryGet3() = runBlocking {

        val localService = LocalServiceRealm(realm)
        val id = 2
        val balance = 456L
        val data1 = Data().apply {
            this.id = 1
            this.balance = 123
        }
        val data2 = Data().apply {
            this.id = id
            this.balance = balance
        }
        val data3 = Data().apply {
            this.id = 3
            this.balance = 789
        }
        val flow = localService.get<Data> { "id == $0 AND balance == $1"(id, balance) }
        var update = 0
        val job = launch {
            flow.collect {
                when (update) {
                    0 -> assertEquals(0, it.size)

                    1 -> {
                        assertEquals(1, it.size)
                        assertEquals(id, it.first().id)
                        assertEquals(balance, it.first().balance)
                    }

                    else -> assertTrue(false)
                }
                update++
            }
        }
        launch {
            delay(200)
            job.cancel()
        }
        delay(100)
        localService.storeObjects(
            listOf(
                data1,
                data2,
                data3
            )
        )
        delay(300)
        assertEquals(2, update)
    }

    @Test
    fun testDeleteAndSave() = runBlocking {

        val localService = LocalServiceRealm(realm)
        val data1 = Data().apply {
            this.id = 1
            this.balance = 123
        }
        val data2 = Data().apply {
            this.id = 2
            this.balance = 456
        }
        val data3 = Data().apply {
            this.id = 3
            this.balance = 789
        }
        localService.storeObject(data1)
        assertEquals(1, localService.count<Data>().first())
        localService.deleteAndStoreObjects<Data>(listOf(data2, data3))
        assertEquals(2, localService.count<Data>().first())
    }

    @Test
    fun testSum() = runBlocking {

        val localService = LocalServiceRealm(realm)
        val data1 = Data().apply {
            this.id = 1
            this.balance = 123
        }
        val data2 = Data().apply {
            this.id = 2
            this.balance = 456
        }
        val data3 = Data().apply {
            this.id = 3
            this.balance = 789
        }
        localService.storeObjects(listOf(data1, data2, data3))
        assertEquals(
            1368L,
            localService.getWithAggregation<Data>(Aggregate.SUM by "balance").first()
        )
    }

    @Test
    fun testMin() = runBlocking {

        val localService = LocalServiceRealm(realm)
        val data1 = Data().apply {
            this.id = 1
            this.balance = 123
        }
        val data2 = Data().apply {
            this.id = 2
            this.balance = 456
        }
        val data3 = Data().apply {
            this.id = 3
            this.balance = 789
        }
        localService.storeObjects(listOf(data1, data2, data3))
        assertEquals(1L, localService.getWithAggregation<Data>(Aggregate.MIN by "id").first())
    }

    @Test
    fun testMax() = runBlocking {
        val localService = LocalServiceRealm(realm)
        val data1 = Data().apply {
            this.id = 1
            this.latitude = 0.0
        }
        val data2 = Data().apply {
            this.id = 2
            this.latitude = 2.2
        }
        val data3 = Data().apply {
            this.id = 3
            this.latitude = 1.1
        }
        localService.storeObjects(listOf(data1, data2, data3))
        assertEquals(
            2.2,
            localService.getWithAggregation<Data>(Aggregate.MAX by "latitude").first()
        )
    }

    @Test
    fun testSize() = runBlocking {
        val localService = LocalServiceRealm(realm)
        val data1 = Data().apply {
            this.id = 1
        }
        val data2 = Data().apply {
            this.id = 2
        }
        val data3 = Data().apply {
            this.id = 3
        }
        localService.storeObjects(listOf(data1, data2, data3))
        val size = localService.getWithAggregation<Data>(Aggregate.size).first()
        val count = localService.count<Data>().first()
        assertEquals(count.toLong(), size)
        assertEquals(3L, size)
    }

    @Test
    fun testSortAscending() = runBlocking {
        val localService = LocalServiceRealm(realm)
        val data1 = Data().apply {
            this.id = 1
        }
        val data2 = Data().apply {
            this.id = 2
        }
        val data3 = Data().apply {
            this.id = 3
        }
        localService.storeObjects(listOf(data1, data2, data3))
        val ascending = localService.get<Data>(query = null, Sort.ASCENDING by "id").first()
        assertEquals(3, ascending.size)
        assertEquals(1, ascending[0].id)
        assertEquals(2, ascending[1].id)
        assertEquals(3, ascending[2].id)
    }

    @Test
    fun testSortDescending() = runBlocking {
        val localService = LocalServiceRealm(realm)
        val data1 = Data().apply {
            this.id = 1
        }
        val data2 = Data().apply {
            this.id = 2
        }
        val data3 = Data().apply {
            this.id = 3
        }
        localService.storeObjects(listOf(data1, data2, data3))
        val descending = localService.get<Data>(query = null, Sort.DESCENDING by "id").first()
        assertEquals(3, descending.size)
        assertEquals(3, descending[0].id)
        assertEquals(2, descending[1].id)
        assertEquals(1, descending[2].id)
    }
}