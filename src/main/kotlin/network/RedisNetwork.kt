package network

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onCompletion
import parsing.RedisParser
import kotlin.reflect.KClass

@UseExperimental(ExperimentalCoroutinesApi::class)
class RedisNetwork(
    private val redisClient: RedisClient,
    vararg classes: KClass<*>
) {

    private val redisParser = RedisParser(
        delimiter = redisClient.delimiter,
        allSymbol = redisClient.allSymbol,
        classes = *classes
    )

    private val allRequest = classes
        .map { clazz -> redisParser.createGetRequest(clazz) }
        .flatten()

    fun observeAll() = redisClient
        .getAll(allRequest)
        .onCompletion { emitAll(redisClient.subscribe(allRequest)) }
        .map { (key, value) -> redisParser.parseFromRedis(key, value) }
        .filterNotNull()

    suspend fun <T : Any> change(old: T?, new: T) = redisClient.applyChange(
        redisParser.createChangeRequest(old, new)
    )

    suspend fun <T : Any> delete(obj: T) = redisClient.applyChange(
        redisParser.createDeleteRequest(obj)
    )


}