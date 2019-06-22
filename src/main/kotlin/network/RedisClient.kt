package network

import kotlinx.coroutines.flow.Flow


interface RedisClient {

    suspend fun get(keys: List<String>): Map<String, String>

    suspend fun get(key: String): Pair<String, String>

    suspend fun put(values: List<Pair<String, String>>)

    suspend fun subscribe(keys: List<String>): Flow<Pair<String, String>>

}