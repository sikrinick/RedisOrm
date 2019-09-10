package network

import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import module.RedisClass
import parsing.RedisParser
import kotlin.reflect.KClass

class RedisNetwork(
    private val redisClient: RedisClient,
    redisClasses: List<Pair<KClass<*>, RedisClass>>
) {
    private val redisParser = RedisParser(
        classes = redisClasses,
        delimiter = redisClient.delimiter,
        allSymbol = redisClient.allSymbol
    )

    private val allRequest by lazy { redisClasses
            .map { (clazz, _) ->
                redisParser.createGetRequest(clazz)
            }
            .flatten()
    }

    suspend fun getAll() = redisClient.getAll(allRequest)
        .map { (key, value) -> redisParser.parseFromRedis(key, value) }

    suspend fun subscribe() = redisClient.subscribe(allRequest)
        .map { (key, value) -> redisParser.parseFromRedis(key, value) }
        .filterNotNull()


    suspend fun <T : Any> update(old: T, new: T) {
        val changes = redisParser.createUpdateRequest(old, new)
        redisClient.put(changes)
    }

    suspend fun <T : Any> add(obj: T) {
        val changes = redisParser.createAddRequest(obj)
        redisClient.put(changes)
        //todo
    }

    suspend fun <T : Any> delete(obj: T) {
        //val changes = redisParser.createDeleteRequest(obj)
        //todo
    }

}