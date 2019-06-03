package com.sikri.redis_orm.parsing.parsers

import com.sikri.redis_orm.module.RedisId
import com.sikri.redis_orm.module.RedisKey
import com.sikri.redis_orm.parsing.ParsingContext
import com.sikri.redis_orm.parsing.RedisParsingSetup
import com.sikri.redis_orm.parsing.getConstructorParametersWith
import com.sikri.redis_orm.parsing.jvmType
import java.util.*
import kotlin.reflect.KClass

class RedisClassParser(
        private val clazz: KClass<*>
): RedisTypeParser() {

    private val idParam = clazz
            .getConstructorParametersWith<RedisId>()
            ?.firstOrNull()
            ?.first

    private val parameters = clazz
            .getConstructorParametersWith<RedisKey>()
            ?.map { (kparam, redisKey) -> RedisClassParameter(kparam, redisKey.name) }
            ?: emptyList()

    val hasId = idParam != null

    private val paramParsers = parameters
        .map { param ->
            param.redisKeyName to RedisParamParser(
                    param.kparam,
                    when (val type = param.kparam.jvmType) {
                        in RedisParsingSetup.parsers -> RedisParsingSetup.parsers.getValue(type)
                        Map::class -> createMapParser(param.kparam.type)
                        else -> RedisClassParser(type)
                    }
            )
        }
        .toMap()

    override fun parse(keys: Queue<String>, value: String): ParsingContext? {
        val id = if (hasId) keys.poll() else null
        val paramParser = paramParsers[keys.poll()] ?: return null

        val idPair = if (idParam != null && id != null) ParsingContext.Id(idParam, id) else null

        val (kparam, result) = paramParser.parse(keys, value)

        return ParsingContext(
                clazz,
                idPair,
                kparam,
                result
        )
    }
}