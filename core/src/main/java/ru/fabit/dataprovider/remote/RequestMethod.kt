package ru.fabit.dataprovider.remote

@JvmInline
value class RequestMethod private constructor(private val value: Byte) {
    companion object {
        val GET = RequestMethod(0)
        val PUT = RequestMethod(1)
        val POST = RequestMethod(2)
        val PATCH = RequestMethod(3)
        val DELETE = RequestMethod(4)
    }
}
