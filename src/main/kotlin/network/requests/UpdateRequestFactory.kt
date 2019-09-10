package network.requests

import module.RedisClass
import parsing.RedisParsingSetup
import parsing.findAnnotation
import parsing.getProperty
import parsing.parsers.RedisClassParser
import kotlin.reflect.KProperty1

class UpdateRequestFactory(
    private val classParsers: Map<String, RedisClassParser>,
    private val delimiter: String,
    private val addRequestFactory: AddRequestFactory,
    private val deleteRequestFactory: DeleteRequestFactory
) {

    fun <T : Any> create(
        old: T,
        new: T
    ): List<Pair<String, String>> {
        val clazz = old::class
        val name = clazz.findAnnotation<RedisClass>()?.name
        val classParser = classParsers[name]
        if (name == null || classParser == null) return emptyList()

        val idParam = classParser.idParam
        val redisId = idParam?.let { clazz.getProperty(idParam.name).get(old) }

        val classPart = name + if (classParser.hasId) "$delimiter$redisId$delimiter" else delimiter

        return classParser.parameters
            .asSequence()
            .map { parameter ->
                val prop = clazz.getProperty(parameter.kparam.name) as KProperty1<Any, Any?>
                Triple(parameter.redisKeyName, prop.get(old), prop.get(new))
            }
            .filter { (_, old, new) -> old != new }
            .mapNotNull { (redisName, old, new) ->
                return@mapNotNull if (new != null && new::class in RedisParsingSetup.primitivesSupported) {
                    listOf(redisName to new.toString())
                } else {
                    when {
                        old == null && new != null -> addRequestFactory.create(new)
                        old != null && new == null -> deleteRequestFactory.create(old).map { it to "" }
                        old != null && new != null -> create(old, new)
                        else -> null
                    }?.map { (key, value) -> (redisName + delimiter + key) to value }
                }
            }
            .flatten()
            .map { (key, value) -> (classPart + key) to value }
            .toList()
    }

}