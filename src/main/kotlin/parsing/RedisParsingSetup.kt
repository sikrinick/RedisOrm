package parsing

import parsing.parsers.*
import kotlin.collections.HashMap


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

    //fun updateParsers(vararg newParsers: Pair<KClass<*>, RedisTypeParser>)
    //        = parsers.putAll(newParsers)
}
