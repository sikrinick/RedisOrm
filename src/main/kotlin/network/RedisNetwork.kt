package network

import kotlinx.coroutines.channels.filterNotNull
import kotlinx.coroutines.channels.map
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import module.RedisClass
import network.requests.GetRequest
import network.requests.UpdateRequest
import parsing.RedisParser
import kotlin.reflect.KClass

class RedisNetwork(
    private val redisClient: RedisClient,
    redisClasses: List<Pair<KClass<*>, RedisClass>>
) {
    private val redisParser = RedisParser(redisClasses)

    private val allRequest by lazy { redisClasses
            .map { (clazz, redisClass) -> clazz to redisClass.name }
            .map { (clazz, redisName) ->
                GetRequest(clazz, redisName).create()
            }
            .flatten()
    }

    suspend fun getAll() = redisParser.parse(
        redisClient.get(allRequest)
    )

    suspend fun subscribe() = redisClient.subscribe(allRequest)
        .map { (key, value) -> redisParser.parse(key, value) }
        .filterNotNull()


    suspend fun update(old: Any, new: Any) {
        val changes = UpdateRequest(old, new).create()
        redisClient.put(changes)
    }

    suspend fun add(obj: Any) {
        //todo
    }

    suspend fun delete(obj: Any) {
        //todo
    }

    companion object {
        const val DELIMITER = ":"
        const val ALL = "*"
    }
}