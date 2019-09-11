package network

import kotlinx.coroutines.flow.Flow
import network.requests.RedisSendingChange


interface RedisClient {

    suspend fun getAll(keys: List<String>): Flow<Pair<String, String>>

    suspend fun get(key: String): Pair<String, String>

    suspend fun applyChange(changes: List<RedisSendingChange>)

    suspend fun subscribe(keys: List<String>): Flow<Pair<String, String>>

    val delimiter: String
    val allSymbol: String
}