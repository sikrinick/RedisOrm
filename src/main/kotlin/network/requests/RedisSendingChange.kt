package network.requests

sealed class RedisSendingChange(open val key: String) {
    data class DelField(override val key: String): RedisSendingChange(key)
    data class SetField(override val key: String, val value: String): RedisSendingChange(key)
}