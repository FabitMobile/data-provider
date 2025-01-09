package ru.fabit.dataprovider

import io.realm.kotlin.types.RealmObject
import io.realm.kotlin.types.annotations.PrimaryKey

open class Data : RealmObject {
    @PrimaryKey
    var id = 0
    var balance = 0L
    var latitude = 12.34

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Data

        if (id != other.id) return false
        if (balance != other.balance) return false
        if (latitude != other.latitude) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id
        result = 31 * result + balance.hashCode()
        result = 31 * result + latitude.hashCode()
        return result
    }
}