package ru.fabit.dataprovider.local

data class Query(
    val query: String,
    val args: Array<out Any>? = null
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Query

        if (query != other.query) return false
        if (!args.contentEquals(other.args)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = query.hashCode()
        result = 31 * result + args.contentHashCode()
        return result
    }
}

inline fun String.query(vararg args: Any) = Query(this, args)

inline fun query(block: QueryScope.() -> Unit): Query {
    val scope = QueryScope()
    block(scope)
    val args = scope.args ?: if (scope.arg != null) arrayOf(scope.arg!!) else arrayOf()
    return Query(scope.query, args)
}

class QueryScope {
    var query: String = ""
        private set
    var arg: Any? = null
        private set
    var args: Array<Any>? = null
        private set

    operator fun String.rangeTo(other: Any) {
        query = this
        arg = other
    }

    infix operator fun String.contains(other: Any): Boolean {
        query = this
        arg = other
        return true
    }

    operator fun String.invoke(arg: Any) {
        query = this
        this@QueryScope.arg = arg
    }

    operator fun String.invoke(arg1: Any, arg2: Any) {
        query = this
        args = arrayOf(arg1, arg2)
    }

    operator fun String.invoke(arg1: Any, arg2: Any, arg3: Any) {
        query = this
        args = arrayOf(arg1, arg2, arg3)
    }

    operator fun String.invoke(arg1: Any, arg2: Any, arg3: Any, arg4: Any) {
        query = this
        args = arrayOf(arg1, arg2, arg3, arg4)
    }

    operator fun String.invoke(arg1: Any, arg2: Any, arg3: Any, arg4: Any, arg5: Any) {
        query = this
        args = arrayOf(arg1, arg2, arg3, arg4, arg5)
    }
}