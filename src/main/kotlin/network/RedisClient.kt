package network

import kotlinx.coroutines.flow.Flow


interface RedisClient {

    suspend fun getAll(keys: List<String>): Flow<Pair<String, String>>

    suspend fun get(key: String): Pair<String, String>

    suspend fun put(values: List<Pair<String, String>>)
    
    suspend fun delete(values: List<Pair<String, String>>)

    suspend fun subscribe(keys: List<String>): Flow<Pair<String, String>>

    val delimiter: String
    val allSymbol: String
}