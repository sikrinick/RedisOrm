package parsing.parsers

import java.util.*
import kotlin.reflect.KParameter


class RedisParamParser(
        private val kParameter: KParameter,
        private val typeParser: RedisTypeParser<out Any?>
) {
    fun parse(keys: Queue<String>, value: String) = kParameter to typeParser.parseFromRedis(keys, value)
}