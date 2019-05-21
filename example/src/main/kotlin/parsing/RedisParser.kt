package parsing

import RedisId
import RedisKey
import getConstructorParametersWith
import java.util.*
import kotlin.reflect.KClass
import kotlin.reflect.KParameter
import kotlin.reflect.full.primaryConstructor
import kotlin.reflect.jvm.jvmErasure

class RedisParser(
    val classes: Map<String, RedisClassParser>
) {
    companion object {
        private const val DELIMITER = ":"
        private const val ALL = "*"
    }

    fun parse(response: Map<String, String>) = response
        .map { (redisKey, redisValue) -> parse(redisKey, redisValue) }
        .filterNotNull()
        .groupBy { it.clazz }
        .toMap()
        .map { (clazz, contexts) ->
            clazz to createMapOfEntities(clazz, contexts).toMutableMap()
        }
        .toMap()

    fun parse(redisKey: String, redisValue: String): ParsingContext? {
        val keys = LinkedList(redisKey.split(DELIMITER))
        return classes[keys.poll()]?.parse(keys, redisValue)
    }


    private fun <T: Any> createMapOfEntities(clazz: KClass<T>,
                                             contexts: List<ParsingContext>): Map<String?, T> = contexts
        .groupBy { it.id }
        .map { it.key?.second to createSingleEntity(clazz, it.value, it.key) }
        .toMap()

    private fun <T: Any> createSingleEntity(clazz: KClass<T>,
                                            contexts: List<ParsingContext>,
                                            idParam: Pair<KParameter, String>? = null): T = contexts
        .groupBy  { it.result is ParsingContext }
        .map { (notParsingContexts, contexts) ->
            if (!notParsingContexts) {
                contexts.map { it.param to it.result }
            } else {
                contexts
                    .groupBy({ it.param }) { it.result as ParsingContext }
                    .map { (param, contexts) ->
                        val type = param.jvmType
                        param to if (type == Map::class) {
                            createMapOfEntities(
                                param.type.arguments[1].type!!.jvmErasure,
                                contexts
                            )
                        } else {
                            createSingleEntity(type, contexts)
                        }
                    }
            }
        }
        .flatten()
        .toMap()
        .let { map ->
            idParam?.let { map + idParam } ?: map
        }
        .let {
            clazz.primaryConstructor!!.callBy(it)
        }


    fun prepareGetAllRequest() = classes
        .map { (key, value) -> key to value.clazz }
        .toMap()
        .map { (redisClass, clazz) ->
            createGetRequests(clazz, redisClass)
        }
        .flatten()

    private fun createGetRequests(clazz: KClass<*>, redisClassName: String): List<String> {
        val (_, redisId) = clazz.getConstructorParametersWith<RedisId>()
            ?.firstOrNull() //todo exception if > 1
            ?: Pair(null, null)

        val classPart = redisClassName + if (redisId != null) "$DELIMITER$ALL$DELIMITER" else DELIMITER

        return clazz.getConstructorParametersWith<RedisKey>()
            ?.map { (param, redisKey) ->
                when (val paramClass = param.jvmType) {
                    in RedisParsingSetup.primitivesSupported -> listOf(classPart + redisKey.name)
                    Map::class -> {
                        val (_, objType) = param.type.arguments
                        createGetRequests(objType.type!!.jvmErasure, redisKey.name)
                            .map {
                                classPart + it
                            }
                    }
                    else -> createGetRequests(paramClass, redisKey.name)
                        .map {
                            classPart + it
                        }
                }
            }
            ?.flatten()
            ?: emptyList()
    }
}

class RedisParamParser(
    private val kParameter: KParameter,
    private val typeParser: RedisTypeParser
) {
    fun parse(keys: Queue<String>, value: String) =
        RedisParsingResult(kParameter, typeParser.parse(keys, value))
}

data class RedisParsingResult<T>(
    val param: KParameter,
    val result: T
)

val KParameter.jvmType
    get() = type.jvmErasure