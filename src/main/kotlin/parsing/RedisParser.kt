package parsing

import module.RedisClass
import network.requests.AllKeysFactory
import network.requests.ObjectChangesFactory
import network.requests.ObjectDeleteFactory
import parsing.parsers.RedisClassParser
import java.util.*
import kotlin.reflect.KClass

class RedisParser(
        private val delimiter: String,
        private val allSymbol: String,
        vararg classes: KClass<*>
) {

    private val parsersByRedisName = classes.filterByAnnotation<RedisClass>()
            .map { (clazz, redisClass) -> redisClass.name to RedisClassParser(clazz) }
            .toMap()

    private val parsersByClass = parsersByRedisName
        .map { (_, parser) -> parser.clazz to parser }
        .toMap()

    private val allKeysFactory = AllKeysFactory(
        classParsers = parsersByClass,
        delimiter = delimiter,
        allSymbol = allSymbol
    )

    private val objectDeleteFactory = ObjectDeleteFactory(
        allKeysFactory = allKeysFactory
    )

    private val setRequestFactory = ObjectChangesFactory(
        classParsers = parsersByClass,
        delimiter = delimiter,
        objectDeleteFactory = objectDeleteFactory
    )


    fun <T: Any> createGetRequest(clazz: KClass<T>, id: String? = null) = allKeysFactory.create(clazz, id)
    fun <T: Any> createChangeRequest(old: T?, new: T) = setRequestFactory.create(old, new)
    fun <T: Any> createDeleteRequest(old: T) = objectDeleteFactory.create(old)

    fun parseFromRedis(redisKey: String, redisValue: String?): ParsingContext? {
        val keys = LinkedList(redisKey.split(delimiter))
        return parsersByRedisName[keys.poll()]?.parseFromRedis(keys, redisValue)
    }

}