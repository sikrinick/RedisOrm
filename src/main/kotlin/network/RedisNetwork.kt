package network

import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import module.RedisSubscribeCache
import parsing.ParsingContext
import parsing.RedisParser
import parsing.jvmType
import kotlin.reflect.KClass
import kotlin.reflect.jvm.jvmErasure

class RedisNetwork(
    private val redisClient: RedisClient,
    vararg classes: KClass<*>
) {

    private val redisParser = RedisParser(
        delimiter = redisClient.delimiter,
        allSymbol = redisClient.allSymbol,
        classes = *classes
    )

    private val allRequest by lazy { classes
            .map { clazz -> redisParser.createGetRequest(clazz) }
            .flatten()
    }
    suspend fun subscribe() = redisClient.subscribe(allRequest)
        .map { (key, value) -> redisParser.parseFromRedis(key, value) }
        .filterNotNull()
        //.map { context ->
        //    subscribeCache.addToCache(context)
        //}

    suspend fun <T : Any> change(old: T?, new: T) = redisClient.applyChange(
        redisParser.createChangeRequest(old, new)
    )

    suspend fun <T : Any> delete(obj: T) = redisClient.applyChange(
        redisParser.createDeleteRequest(obj)
    )

    suspend fun getAll() = redisClient.getAll(allRequest)
        .map { (key, value) -> redisParser.parseFromRedis(key, value) }
        .filterNotNull()
        .toList()
        .groupBy { it.clazz }
        .toMap()
        .map { (clazz, contexts) ->
            clazz to createMapOfEntities(clazz, contexts).toMutableMap()
        }

    private fun <T: Any> createMapOfEntities(
        clazz: KClass<T>,
        contexts: List<ParsingContext>
    ): Map<String?, T> = contexts
        .groupBy { it.id }
        .map { (id, obj) -> id?.value to createSingleEntity(clazz, obj, id) }
        .toMap()

    private fun <T: Any> createSingleEntity(
        clazz: KClass<T>,
        contexts: List<ParsingContext>,
        idParam: ParsingContext.Id? = null
    ): T = contexts
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