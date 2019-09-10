package network.requests

import module.RedisClass
import parsing.RedisParsingSetup
import parsing.findAnnotation
import parsing.getProperty
import parsing.jvmType
import parsing.parsers.RedisClassParser
import kotlin.reflect.KProperty1
import kotlin.reflect.jvm.jvmErasure

class AddRequestFactory(
    private val classParsers: Map<String, RedisClassParser>,
    private val delimiter: String
) {

    fun <T : Any> create(
        new: T
    ): List<Pair<String, String>> {
        val clazz = new::class
        val name = clazz.findAnnotation<RedisClass>()?.name
        val classParser = classParsers[name]
        if (name == null || classParser == null) return emptyList()

        val idParam = classParser.idParam
        val redisId = idParam?.let { clazz.getProperty(idParam.name).get(new) }

        val classPart = name + if (classParser.hasId) "$delimiter$redisId$delimiter" else delimiter

        return classParser.parameters
            .asSequence()
            .map { parameter ->
                val prop = clazz.getProperty(parameter.kparam.name) as KProperty1<Any, Any?>
                parameter.redisKeyName to prop.get(new)
            }
            .mapNotNull { (redisName, property) ->
                if (property != null && property::class in RedisParsingSetup.primitivesSupported) {
                    listOf(redisName to property.toString())
                } else if (property != null) {
                    create(property).map { (key, value) -> (redisName + delimiter + key) to value }
                } else {
                    null
                }
            }
            .flatten()
            .map { (key, value) -> (classPart + key) to value }
            .toList()
    }
}