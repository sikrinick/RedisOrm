package com.sikri.redis_orm.network

import com.sikri.redis_orm.module.RedisClass
import com.sikri.redis_orm.network.requests.GetAllRequest
import com.sikri.redis_orm.parsing.RedisParser
import kotlin.reflect.KClass

class RedisNetwork(
        redisClasses: List<Pair<KClass<*>, RedisClass>>
) {
    private val redisClient = RedisClient()
    private val redisParser = RedisParser(redisClasses)

    private val allRequest by lazy { redisClasses
            .map { (clazz, redisClass) -> clazz to redisClass.name }
            .map { (clazz, redisName) -> GetAllRequest(clazz, redisName).create() }
            .flatten()
    }

    fun getAll(): Map<KClass<*>, Map<String?, Any>> {
        val response = redisClient.sendGetAllRequest(allRequest)
        return redisParser.parse(response)
    }

    fun observe() = redisClient.subscribeTo(allRequest)
            .map { (key, value) -> redisParser.parse(key, value) }

    companion object {
        const val DELIMITER = ":"
        const val ALL = "*"
    }
}