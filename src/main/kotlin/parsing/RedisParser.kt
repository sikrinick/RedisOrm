package parsing

import module.RedisClass
import network.RedisNetwork.Companion.DELIMITER
import parsing.parsers.RedisClassParser
import java.util.*
import kotlin.reflect.KClass
import kotlin.reflect.jvm.jvmErasure

class RedisParser(
        classes: List<Pair<KClass<*>, RedisClass>>
) {
    private val classParsers = classes
            .map { (clazz, redisClass) -> redisClass.name to RedisClassParser(clazz) }
            .toMap()

    fun parse(response: Map<String, String>): Map<KClass<*>, MutableMap<String?, out Any>> = response
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
        return classParsers[keys.poll()]?.parse(keys, redisValue)
    }

    private fun <T: Any> createMapOfEntities(clazz: KClass<T>,
                                             contexts: List<ParsingContext>): Map<String?, T> = contexts
        .groupBy { it.id }
        .map { it.key?.value to createSingleEntity(clazz, it.value, it.key) }
        .toMap()

    private fun <T: Any> createSingleEntity(clazz: KClass<T>,
                                            contexts: List<ParsingContext>,
                                            idParam: ParsingContext.Id? = null): T = contexts
        .groupBy  { it.result is ParsingContext }
        .map { (notParsingContexts, contexts) ->
            if (!notParsingContexts) {
                contexts.map { it.param to it.result }
            } else {
                contexts
                    .groupBy({ it.param }) { it.result as ParsingContext }
                    .map { (param, contexts) ->
                        param to when(val type = param.jvmType) {
                            Map::class -> createMapOfEntities(
                                    param.type.arguments[1].type!!.jvmErasure,
                                    contexts
                            )
                            else -> createSingleEntity(type, contexts)
                        }
                    }
            }
        }
        .flatten()
        .toMap()
        .let { map ->
            idParam?.let { map + (idParam.kParameter to idParam.value) } ?: map
        }
        .let {
            clazz.constructors.first().callBy(it)
        }

}