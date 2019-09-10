package module

import data.RedisLocalCache
import data.copyWith
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import network.RedisClient
import network.RedisNetwork
import parsing.ParsingContext
import parsing.filterByAnnotation
import parsing.jvmType
import kotlin.reflect.KClass
import kotlin.reflect.jvm.jvmErasure

@FlowPreview
class RedisOrm(
    redisClient: RedisClient,
    vararg classes: KClass<*>
) {
    
    val redisNetwork = RedisNetwork(redisClient, classes.filterByAnnotation())

    private val subscribeScope = CoroutineScope(Dispatchers.IO)
    
    suspend fun start() {
        updateAll()
        subscribeScope.launch {
            subscribeToAll()
        }
    }

    private suspend fun updateAll() {
        redisNetwork.getAll()
            .filterNotNull()
            .toList()
            .groupBy { it.clazz }
            .toMap()
            .map { (clazz, contexts) ->
                clazz to createMapOfEntities(clazz, contexts).toMutableMap()
            }
            .forEach { (clazz, map) ->
                map.forEach { (id, obj) ->
                    unsafeCacheSet(clazz, id, obj)
                }
            }
        }

    private fun <T: Any> createSingleEntity(
        clazz: KClass<T>,
        contexts: List<ParsingContext>,
        idParam: ParsingContext.Id? = null
    ): T = contexts
        .groupBy  { it.result is ParsingContext }
        .map { (notParsingContexts, contexts) ->
            if (!notParsingContexts) {
                contexts.map { it.param to it.result }
            } else {
                contexts
                    .groupBy({ it.param }) { it.result as ParsingContext }
                    .map { (param, contexts) ->
                        param to when(val type = param.jvmType) {
                            Map::class -> createMapOfEntities(
                                param.type.arguments[1].type!!.jvmErasure,
                                contexts
                            )
                            else -> createSingleEntity(type, contexts)
                        }
                    }
            }
        }
        .flatten()
        .toMap()
        .let { map ->
            idParam?.let { map + (idParam.kParameter to idParam.value) } ?: map
        }
        .let {
            clazz.constructors.first().callBy(it)
        }

    private fun <T: Any> createMapOfEntities(clazz: KClass<T>,
                                             contexts: List<ParsingContext>): Map<String?, T> = contexts
        .groupBy { it.id }
        .map { it.key?.value to createSingleEntity(clazz, it.value, it.key) }
        .toMap()


    private suspend fun subscribeToAll() {
        redisNetwork.subscribe()
            .map {
                it to getFromCache(it.clazz, it.id?.value)
            }
            .map { (change, obj) ->
                when (obj) {
                    null -> {} //cache to add later
                    else -> unsafeCacheSet(change.clazz, change.id?.value, obj.copyWith(change))
                }
            }
    }

    val redisCache = RedisLocalCache(*classes)

    private fun unsafeCacheSet(clazz: KClass<*>, id: String? = null, new: Any) = redisCache.unsafeSet(clazz, id, new)

    fun <T : Any> getFromCache(clazz: KClass<T>, id: String? = null) = redisCache.get(clazz, id)
    inline fun <reified T : Any> getFromCache(id: String? = null) : T? = getFromCache(T::class, id)

    fun <T : Any> setInCache(clazz: KClass<T>, id: String? = null, new: T) = redisCache.set(clazz, id, new)
    inline fun <reified T : Any> setInCache(id: String? = null, new: T) = setInCache(T::class, id, new)

    fun <T : Any> deleteFromCache(clazz: KClass<T>, id: String? = null) = redisCache.delete(clazz, id)
    inline fun <reified T : Any> deleteFromCache(id: String? = null) = deleteFromCache(T::class, id)


    inline fun <reified T: Any> observe(id: String? = null) = redisCache.observe(T::class, id)
    inline fun <reified T: Any> observeAll() = redisCache.observeAll(T::class)

    suspend inline fun <reified T : Any> update(id: String? = null, new: T?) {
        val old = getFromCache<T>(id)
        if (new != null) {
            if (old != null) {
                redisNetwork.update(old = old, new = new)
            } else {
                redisNetwork.add(new)
            }
            setInCache(id, new)
        } else if (old != null) {
            redisNetwork.delete(old)
            deleteFromCache<T>(id)
        }
    }
}