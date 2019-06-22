package parsing.parsers

import kotlin.reflect.KParameter

class RedisClassParameter(
        val kparam: KParameter,
        val redisKeyName: String
)