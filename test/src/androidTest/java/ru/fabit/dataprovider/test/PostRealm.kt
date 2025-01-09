package ru.fabit.dataprovider.test

import io.realm.kotlin.types.RealmObject
import io.realm.kotlin.types.annotations.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
open class PostRealm : RealmObject {
    @PrimaryKey
    var id = 0
    var userId = 0
    var title: String? = null
    var body: String? = null

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as PostRealm

        if (id != other.id) return false
        if (userId != other.userId) return false
        if (title != other.title) return false
        if (body != other.body) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id
        result = 31 * result + userId
        result = 31 * result + title.hashCode()
        result = 31 * result + body.hashCode()
        return result
    }
}