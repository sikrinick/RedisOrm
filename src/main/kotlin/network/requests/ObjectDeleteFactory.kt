package network.requests

class ObjectDeleteFactory(
    private val allKeysFactory: AllKeysFactory
) {
    fun <T : Any> create(obj: T) = allKeysFactory.create(obj).map { RedisSendingChange.DelField(it) }
}