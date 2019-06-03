package com.sikri.redis_orm.network.requests

import com.sikri.redis_orm.module.RedisId
import com.sikri.redis_orm.module.RedisKey
import com.sikri.redis_orm.network.RedisNetwork.Companion.ALL
import com.sikri.redis_orm.network.RedisNetwork.Companion.DELIMITER
import com.sikri.redis_orm.parsing.RedisParsingSetup
import com.sikri.redis_orm.parsing.getConstructorParametersWith
import com.sikri.redis_orm.parsing.jvmType
import kotlin.reflect.KClass
import kotlin.reflect.jvm.jvmErasure

class GetAllRequest(
        private val clazz: KClass<*>,
        private val redisClassName: String
) {

    fun create(): List<String> {
        val (_, redisId) = clazz.getConstructorParametersWith<RedisId>()
                ?.firstOrNull() //todo exception if > 1
                ?: Pair(null, null)

        val classPart = redisClassName + if (redisId != null) "$DELIMITER$ALL$DELIMITER" else DELIMITER

        return clazz.getConstructorParametersWith<RedisKey>()
                ?.map { (param, redisKey) ->
                    when (val paramClass = param.jvmType) {
                        in RedisParsingSetup.primitivesSupported -> listOf(classPart + redisKey.name)
                        Map::class -> {
                            val (_, objType) = param.type.arguments
                            GetAllRequest(objType.type!!.jvmErasure, redisKey.name).create()
                                    .map {
                                        classPart + it
                                    }
                        }
                        else -> {
                            GetAllRequest(paramClass, redisKey.name).create()
                                    .map {
                                        classPart + it
                                    }
                        }
                    }
                }
                ?.flatten()
                ?: emptyList()
    }
}