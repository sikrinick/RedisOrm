package parsing

import java.util.*

abstract class RedisTypeParser {
    abstract fun parse(keys: Queue<String>, value: String): Any?
}

object RedisBooleanParser: RedisTypeParser() {
    override fun parse(keys: Queue<String>, value: String): Any? {
        return value.toBoolean()
    }
}

object RedisByteParser: RedisTypeParser() {
    override fun parse(keys: Queue<String>, value: String): Any? {
        return value.toByte()
    }
}

object RedisCharParser: RedisTypeParser() {
    override fun parse(keys: Queue<String>, value: String): Any? {
        return if (value.isNotEmpty()) value[0] else 0.toChar()
    }
}

object RedisIntParser: RedisTypeParser() {
    override fun parse(keys: Queue<String>, value: String): Any? {
        return value.toInt()
    }
}

object RedisLongParser: RedisTypeParser() {
    override fun parse(keys: Queue<String>, value: String): Any? {
        return value.toLong()
    }
}

object RedisFloatParser: RedisTypeParser() {
    override fun parse(keys: Queue<String>, value: String): Any? {
        return value.toFloat()
    }
}

object RedisDoubleParser: RedisTypeParser() {
    override fun parse(keys: Queue<String>, value: String): Any? {
        return value.toDouble()
    }
}

object RedisStringParser: RedisTypeParser() {
    override fun parse(keys: Queue<String>, value: String): Any? {
        return value
    }
}