package network.requests

import module.RedisClass
import parsing.RedisParsingSetup
import parsing.findAnnotation
import parsing.getProperty
import parsing.jvmType
import parsing.parsers.RedisClassParser
import kotlin.reflect.KProperty1
import kotlin.reflect.jvm.jvmErasure

class DeleteRequestFactory(
    private val classParsers: Map<String, RedisClassParser>,
    private val delimiter: String
) {

    fun <T : Any> create(
        old: T
    ): List<String> {
        val clazz = old::class
        val name = clazz.findAnnotation<RedisClass>()?.name
        val classParser = classParsers[name]
        if (name == null || classParser == null) return emptyList()

        val idParam = classParser.idParam
        val redisId = idParam?.let { clazz.getProperty(idParam.name).get(old) }

        val classPart = name + if (classParser.hasId) "$delimiter$redisId$delimiter" else delimiter

        return classParser.parameters
            .map { redisParam ->
                when (redisParam.kparam.jvmType) {
                    in RedisParsingSetup.primitivesSupported -> listOf(redisParam.redisKeyName)
                    Map::class -> {
                        val (_, objType) = redisParam.kparam.type.arguments
                        val prop = clazz.getProperty(redisParam.kparam.name).get(old)
                        create(prop!!)
                    }
                    else -> {
                        val prop = clazz.getProperty(redisParam.kparam.name).get(old)
                        create(prop!!)
                    }
                }
            }
            .flatten()
    }

}