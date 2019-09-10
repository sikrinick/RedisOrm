package parsing

import module.RedisClass
import network.requests.AddRequestFactory
import network.requests.DeleteRequestFactory
import network.requests.GetRequestFactory
import network.requests.UpdateRequestFactory
import parsing.parsers.RedisClassParser
import java.util.*
import kotlin.reflect.KClass

class RedisParser(
        classes: List<Pair<KClass<*>, RedisClass>>,
        private val delimiter: String,
        allSymbol: String
) {
    private val classParsers = classes
            .map { (clazz, redisClass) -> redisClass.name to RedisClassParser(clazz) }
            .toMap()

    private val getRequestFactory = GetRequestFactory(
        classParsers = classParsers,
        delimiter = delimiter,
        allSymbol = allSymbol
    )

    private val addRequestFactory = AddRequestFactory(
        classParsers = classParsers,
        delimiter = delimiter
    )

    private val deleteRequestFactory = DeleteRequestFactory(
        classParsers = classParsers,
        delimiter = delimiter
    )

    private val updateRequestFactory = UpdateRequestFactory(
        classParsers = classParsers,
        delimiter = delimiter,
        addRequestFactory = addRequestFactory,
        deleteRequestFactory = deleteRequestFactory
    )


    fun <T: Any> createGetRequest(clazz: KClass<T>, id: String? = null) = getRequestFactory.create(clazz, id)
    fun <T: Any> createUpdateRequest(old: T, new: T) = updateRequestFactory.create(old, new)
    fun <T: Any> createAddRequest(new: T) = addRequestFactory.create(new)
    fun <T: Any> createDeleteRequest(old: T) = deleteRequestFactory.create(old)

    fun parseFromRedis(redisKey: String, redisValue: String): ParsingContext? {
        val keys = LinkedList(redisKey.split(delimiter))
        return classParsers[keys.poll()]?.parseFromRedis(keys, redisValue)
    }

}