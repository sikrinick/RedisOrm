package network.requests

import module.RedisClass
import parsing.RedisParsingSetup
import parsing.findAnnotation
import parsing.jvmType
import parsing.parsers.RedisClassParser
import kotlin.reflect.KClass
import kotlin.reflect.jvm.jvmErasure

class GetRequestFactory(
    private val classParsers: Map<String, RedisClassParser>,
    private val delimiter: String,
    private val allSymbol: String
) {

    fun create(
        clazz: KClass<*>,
        id: String? = null
    ): List<String> {
        val name = clazz.findAnnotation<RedisClass>()?.name
        val classParser = classParsers[name]
        if (name == null || classParser == null) return emptyList()

        val classPart = name + if (classParser.hasId) "$delimiter${id ?: allSymbol}$delimiter" else delimiter

        return classParser.parameters
            .map { redisParam ->
                when (val paramClass = redisParam.kparam.jvmType) {
                    in RedisParsingSetup.primitivesSupported -> listOf(redisParam.redisKeyName)
                    Map::class -> {
                        val (_, objType) = redisParam.kparam.type.arguments
                        create(objType.type!!.jvmErasure, redisParam.kparam.name)
                    }
                    else -> {
                        create(paramClass, redisParam.kparam.name)
                    }
                }
            }
            .flatten()
            .map { classPart + it }
    }
}