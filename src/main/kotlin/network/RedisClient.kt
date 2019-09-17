package network

import kotlinx.coroutines.flow.Flow
import network.requests.RedisSendingChange


interface RedisClient {

    fun getAll(keys: List<String>): Flow<Pair<String, String?>>

    fun subscribe(keys: List<String>): Flow<Pair<String, String?>>

    suspend fun get(key: String): Pair<String, String>

    suspend fun applyChange(changes: List<RedisSendingChange>)

    val delimiter: String
    val allSymbol: String
}