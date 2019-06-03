package com.sikri.redis_orm.parsing.parsers

import java.util.*
import kotlin.reflect.KParameter


class RedisParamParser(
        private val kParameter: KParameter,
        private val typeParser: RedisTypeParser
) {
    fun parse(keys: Queue<String>, value: String) = kParameter to typeParser.parse(keys, value)
}