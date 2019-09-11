package parsing.parsers

import java.util.*
import kotlin.reflect.KClass
import kotlin.reflect.KType
import kotlin.reflect.jvm.jvmErasure

abstract class RedisTypeParser<T : Any?> {
    abstract fun parseFromRedis(keys: Queue<String>, value: String): T

    companion object {
        fun createMapParser(clazz: KClass<*>): RedisTypeParser<Any?> {
            val entity = RedisClassParser(clazz)
            if (!entity.hasId) {
                throw RuntimeException("Parameter of type Map should have a key!")
            }
            return object : RedisTypeParser<Any?>() {
                override fun parseFromRedis(keys: Queue<String>, value: String): Any? {
                    return entity.parseFromRedis(keys, value)
                }
            }
        }
    }
}

object RedisBooleanParser: RedisTypeParser<Boolean>() {
    override fun parseFromRedis(keys: Queue<String>, value: String) = value.toBoolean()
}

object RedisByteParser: RedisTypeParser<Byte>() {
    override fun parseFromRedis(keys: Queue<String>, value: String) = value.toByte()
}

object RedisCharParser: RedisTypeParser<Char>() {
    override fun parseFromRedis(keys: Queue<String>, value: String) = if (value.isNotEmpty()) value[0] else 0.toChar()
}

object RedisIntParser: RedisTypeParser<Int>() {
    override fun parseFromRedis(keys: Queue<String>, value: String) = value.toInt()
}

object RedisLongParser: RedisTypeParser<Long>() {
    override fun parseFromRedis(keys: Queue<String>, value: String) = value.toLong()
}

object RedisFloatParser: RedisTypeParser<Float>() {
    override fun parseFromRedis(keys: Queue<String>, value: String) = value.toFloat()
}

object RedisDoubleParser: RedisTypeParser<Double>() {
    override fun parseFromRedis(keys: Queue<String>, value: String) = value.toDouble()
}

object RedisStringParser: RedisTypeParser<String>() {
    override fun parseFromRedis(keys: Queue<String>, value: String) = value
}