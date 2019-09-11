package network.requests

import parsing.RedisParsingSetup
import parsing.getProperty
import parsing.parsers.RedisClassParser
import kotlin.reflect.KClass
import kotlin.reflect.KProperty1

class ObjectChangesFactory(
    private val classParsers: Map<KClass<*>, RedisClassParser>,
    private val delimiter: String,
    private val objectDeleteFactory: ObjectDeleteFactory
) {

    fun <T : Any> create(
        old: T? = null,
        new: T
    ): List<RedisSendingChange> {
        val clazz = new::class
        val classParser = classParsers[clazz] ?: return emptyList()

        val idParam = classParser.idParam
        val redisId = idParam?.let { clazz.getProperty(idParam.name).get(new) }

        val classPart = classParser.redisClassName + if (classParser.hasId) "$delimiter$redisId$delimiter" else delimiter

        return classParser.parameters
            .asSequence()
            .map { parameter ->
                val prop = clazz.getProperty(parameter.kparam.name) as KProperty1<Any, Any?>
                Triple(
                    parameter.redisKeyName, old?.let{ prop.get(it) }, prop.get(new))
            }
            .filter { (_, oldParam, newParam) -> oldParam != newParam }
            .mapNotNull { (redisName, oldParam, newParam) ->
                if (newParam != null) {
                    if (newParam::class in RedisParsingSetup.primitivesSupported) {
                        listOf(
                            RedisSendingChange.SetField(
                                key = classPart + redisName,
                                value = newParam.toString()
                            )
                        )
                    } else {
                        create(oldParam, newParam)
                            .map { change ->
                                when(change) {
                                    is RedisSendingChange.SetField -> change.copy(key = classPart + redisName + delimiter + change.key)
                                    is RedisSendingChange.DelField -> change.copy(key = classPart + redisName + delimiter + change.key)
                                }
                            }
                    }
                } else if (oldParam != null) {
                    objectDeleteFactory.create(oldParam).map { it.copy(key = classPart + redisName + delimiter + it.key) }
                } else null
            }
            .flatten()
            .toList()
    }

}