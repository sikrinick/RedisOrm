package module

import parsing.ParsingContext
import parsing.RedisParsingSetup
import parsing.jvmType
import parsing.parsers.RedisClassParser
import kotlin.reflect.KClass
import kotlin.reflect.jvm.jvmErasure

class RedisSubscribeCache(
    private val classParsers: Map<KClass<*>, RedisClassParser>
) {

    private val tables = classParsers
        .map { (clazz, classParser) -> clazz to RedisSubscribeTableCache(classParser, classParsers) }
        .toMap()

    fun addToCache(parsingContext: ParsingContext) = tables[parsingContext.clazz]?.addToCache(parsingContext)
    fun getFrom(parsingContext: ParsingContext) = tables[parsingContext.clazz]?.getFromCache(parsingContext)

}

class RedisSubscribeTableCache(
    private val classParser: RedisClassParser,
    private val otherParsers: Map<KClass<*>, RedisClassParser>
) {
    private val table = mutableMapOf<String?, MutableList<ParsingContext>>()

    fun addToCache(parsingContext: ParsingContext) = table
        .getOrPut(parsingContext.id?.value) { mutableListOf() }
        .add(parsingContext)

    fun test() {
        classParser.parameters
            .map { it.kparam }
            .filter { !it.isOptional }
            .map { kparam ->
                //when (val paramClass = kparam.jvmType) {
                //    in RedisParsingSetup.primitivesSupported -> listOf(kparam)
                //    Map::class -> {
                //        val (_, objType) = kparam.type.arguments
                //        otherParsers.
                //        create(objType.type!!.jvmErasure, redisParam.kparam.name)
                //    }
                //    else -> {

                    }
                //}
            }

    fun getFromCache(parsingContext: ParsingContext): MutableList<ParsingContext>? = table[parsingContext.id?.value]
}