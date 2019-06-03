package com.sikri.redis_orm.parsing.parsers

import java.util.*
import kotlin.reflect.KType
import kotlin.reflect.jvm.jvmErasure

abstract class RedisTypeParser {
    abstract fun parse(keys: Queue<String>, value: String): Any?

    companion object {
        fun createMapParser(mapType: KType): RedisTypeParser {
            val (_, objType) = mapType.arguments
            val entity = RedisClassParser(objType.type!!.jvmErasure)
            if (!entity.hasId) {
                throw RuntimeException("Parameter of type Map should have a key!")
            }
            return object : RedisTypeParser() {
                override fun parse(keys: Queue<String>, value: String): Any? {
                    return entity.parse(keys, value)
                }
            }
        }
    }
}

object RedisBooleanParser: RedisTypeParser() {
    override fun parse(keys: Queue<String>, value: String): Any? {
        return value.toBoolean()
    }
}

object RedisByteParser: RedisTypeParser() {
    override fun parse(keys: Queue<String>, value: String): Any? {
        return value.toByte()
    }
}

object RedisCharParser: RedisTypeParser() {
    override fun parse(keys: Queue<String>, value: String): Any? {
        return if (value.isNotEmpty()) value[0] else 0.toChar()
    }
}

object RedisIntParser: RedisTypeParser() {
    override fun parse(keys: Queue<String>, value: String): Any? {
        return value.toInt()
    }
}

object RedisLongParser: RedisTypeParser() {
    override fun parse(keys: Queue<String>, value: String): Any? {
        return value.toLong()
    }
}

object RedisFloatParser: RedisTypeParser() {
    override fun parse(keys: Queue<String>, value: String): Any? {
        return value.toFloat()
    }
}

object RedisDoubleParser: RedisTypeParser() {
    override fun parse(keys: Queue<String>, value: String): Any? {
        return value.toDouble()
    }
}

object RedisStringParser: RedisTypeParser() {
    override fun parse(keys: Queue<String>, value: String): Any? {
        return value
    }
}