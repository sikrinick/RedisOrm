package network.requests

import module.RedisId
import module.RedisKey
import network.RedisNetwork.Companion.ALL
import network.RedisNetwork.Companion.DELIMITER
import parsing.RedisParsingSetup
import parsing.getConstructorParametersWith
import parsing.jvmType
import kotlin.reflect.KClass
import kotlin.reflect.jvm.jvmErasure

class GetRequest(
        private val clazz: KClass<*>,
        private val redisClassName: String,
        private val id: String? = null
) {

    fun create(): List<String> {
        val (_, redisId) = clazz.getConstructorParametersWith<RedisId>()
                ?.firstOrNull() //todo exception if > 1
                ?: Pair(null, null)

        val classPart = redisClassName + if (redisId != null) "$DELIMITER${id ?: ALL}$DELIMITER" else DELIMITER

        return clazz.getConstructorParametersWith<RedisKey>()
                ?.map { (param, redisKey) ->
                    when (val paramClass = param.jvmType) {
                        in RedisParsingSetup.primitivesSupported -> listOf(redisKey.name)
                        Map::class -> {
                            val (_, objType) = param.type.arguments
                            GetRequest(objType.type!!.jvmErasure, redisKey.name).create()
                        }
                        else -> {
                            GetRequest(paramClass, redisKey.name).create()
                        }
                    }
                }
                ?.flatten()
                ?.map { it -> classPart + it }
                ?: emptyList()
    }
}