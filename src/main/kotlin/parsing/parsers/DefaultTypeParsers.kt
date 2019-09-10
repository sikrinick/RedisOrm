package parsing.parsers

import java.util.*
import kotlin.reflect.KType
import kotlin.reflect.jvm.jvmErasure

abstract class RedisTypeParser<T : Any?> {
    abstract fun parseFromRedis(keys: Queue<String>, value: String): T
    abstract fun parseToRedis(keys: Queue<String>, value: T): String

    companion object {
        fun createMapParser(mapType: KType): RedisTypeParser<Any?> {
            val (_, objType) = mapType.arguments
            val entity = RedisClassParser(objType.type!!.jvmErasure)
            if (!entity.hasId) {
                throw RuntimeException("Parameter of type Map should have a key!")
            }
            return object : RedisTypeParser<Any?>() {
                override fun parseFromRedis(keys: Queue<String>, value: String): Any? {
                    return entity.parseFromRedis(keys, value)
                }
                override fun parseToRedis(keys: Queue<String>, value: Any?): String {
                    TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
                }
            }
        }
    }
}

object RedisBooleanParser: RedisTypeParser<Boolean>() {
    override fun parseFromRedis(keys: Queue<String>, value: String) = value.toBoolean()
    override fun parseToRedis(keys: Queue<String>, value: Boolean) = value.toString()
}

object RedisByteParser: RedisTypeParser<Byte>() {
    override fun parseFromRedis(keys: Queue<String>, value: String) = value.toByte()
    override fun parseToRedis(keys: Queue<String>, value: Byte) = value.toString()
}

object RedisCharParser: RedisTypeParser<Char>() {
    override fun parseFromRedis(keys: Queue<String>, value: String) = if (value.isNotEmpty()) value[0] else 0.toChar()
    override fun parseToRedis(keys: Queue<String>, value: Char) = value.toString()
}

object RedisIntParser: RedisTypeParser<Int>() {
    override fun parseFromRedis(keys: Queue<String>, value: String) = value.toInt()
    override fun parseToRedis(keys: Queue<String>, value: Int) = value.toString()
}

object RedisLongParser: RedisTypeParser<Long>() {
    override fun parseFromRedis(keys: Queue<String>, value: String) = value.toLong()
    override fun parseToRedis(keys: Queue<String>, value: Long) = value.toString()
}

object RedisFloatParser: RedisTypeParser<Float>() {
    override fun parseFromRedis(keys: Queue<String>, value: String) = value.toFloat()
    override fun parseToRedis(keys: Queue<String>, value: Float) = value.toString()
}

object RedisDoubleParser: RedisTypeParser<Double>() {
    override fun parseFromRedis(keys: Queue<String>, value: String) = value.toDouble()
    override fun parseToRedis(keys: Queue<String>, value: Double) = value.toString()
}

object RedisStringParser: RedisTypeParser<String>() {
    override fun parseFromRedis(keys: Queue<String>, value: String) = value
    override fun parseToRedis(keys: Queue<String>, value: String) = value
}