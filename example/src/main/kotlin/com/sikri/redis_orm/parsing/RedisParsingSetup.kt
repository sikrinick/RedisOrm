package com.sikri.redis_orm.parsing

import com.sikri.redis_orm.parsing.parsers.*
import java.util.*
import kotlin.collections.HashMap
import kotlin.reflect.KClass
import kotlin.reflect.KType
import kotlin.reflect.jvm.jvmErasure


object RedisParsingSetup {

    private val primitiveParsers = mapOf(
        Boolean::class to RedisBooleanParser,
        Byte::class to RedisByteParser,
        Char::class to RedisCharParser,
        Int::class to RedisIntParser,
        Long::class to RedisLongParser,
        Float::class to RedisFloatParser,
        Double::class to RedisDoubleParser,
        String::class to RedisStringParser
    )

    val primitivesSupported = primitiveParsers.keys

    val parsers = HashMap(primitiveParsers)

    fun updateParsers(vararg newParsers: Pair<KClass<*>, RedisTypeParser>) = parsers.putAll(newParsers)
}
