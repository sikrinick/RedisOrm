package network.requests

import parsing.RedisParsingSetup
import parsing.getProperty
import parsing.jvmType
import parsing.parsers.RedisClassParameter
import parsing.parsers.RedisClassParser
import kotlin.reflect.KClass
import kotlin.reflect.jvm.jvmErasure

class AllKeysFactory(
    private val classParsers: Map<KClass<*>, RedisClassParser>,
    private val delimiter: String,
    private val allSymbol: String
) {
    fun <T : Any> create(
        obj: T
    ): List<String> {
        val clazz = obj::class
        val classParser = classParsers[clazz] ?: return emptyList()

        val idParam = classParser.idParam
        val redisId = idParam?.let { clazz.getProperty(idParam.name).get(obj) }

        return create(clazz, redisId.toString())
    }

    fun create(
        clazz: KClass<*>,
        id: String? = null
    ): List<String> {
        val classParser = classParsers[clazz] ?: return emptyList()
        val classPart = classParser.redisClassName + if (classParser.hasId) "$delimiter${id ?: allSymbol}$delimiter" else delimiter

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