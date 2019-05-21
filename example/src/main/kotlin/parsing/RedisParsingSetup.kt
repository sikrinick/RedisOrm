package parsing

import java.util.*
import kotlin.collections.HashMap
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

    fun createMapParser(mapType: KType): RedisTypeParser {
        val (_, objType) = mapType.arguments
        val entity = RedisClassParser.create(objType.type!!.jvmErasure)
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
