package network.requests

import module.RedisClass
import module.RedisId
import module.RedisKey
import network.RedisNetwork.Companion.DELIMITER
import parsing.RedisParsingSetup
import parsing.findAnnotation
import parsing.getConstructorParametersWith
import parsing.getProperty
import kotlin.reflect.KProperty1

class UpdateRequest(
        private val old: Any,
        private val new: Any
) {

    private val clazz = old::class

    fun create(): List<Pair<String, String>> {
        val (idParam, _) = clazz.getConstructorParametersWith<RedisId>()
                ?.firstOrNull() //todo exception if > 1
                ?: Pair(null, null)

        val redisId = idParam?.let { clazz.getProperty(idParam.name).get(old) }

        val classPart = clazz.findAnnotation<RedisClass>()!!.name +
                if (redisId != null)
                    "$DELIMITER$redisId$DELIMITER"
                else
                    DELIMITER

        return clazz.getConstructorParametersWith<RedisKey>()
                ?.map { (kparam, redisKey) ->
                    val prop = clazz.getProperty(kparam.name) as KProperty1<Any, Any?>

                    Triple(redisKey.name, prop.get(old), prop.get(new))
                }
                ?.filter { (_, old, new) -> old != new }
                ?.mapNotNull { (redisName, old, new) ->
                    return@mapNotNull if (new != null && new::class in RedisParsingSetup.primitivesSupported) {
                        listOf(redisName to new.toString())
                    } else {
                        when {
                            old == null && new != null -> AddRequest(new).create()
                            old != null && new == null -> DeleteRequest(old).create()
                            old != null && new != null -> UpdateRequest(old, new).create()
                            else -> null
                        }?.map { (key, value) -> (redisName + DELIMITER + key) to value }
                    }
                }
                ?.flatten()
                ?.map { (key, value) -> (classPart + key) to value }
                ?: emptyList()
    }

}