package module

import data.RedisLocalCache
import ext.copyWith
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import network.RedisClient
import network.RedisNetwork
import kotlin.reflect.KClass

@FlowPreview
@ExperimentalCoroutinesApi
class RedisOrm(
    redisClient: RedisClient,
    vararg classes: KClass<*>
) {
    val redisCache = RedisLocalCache(*classes)
    val redisNetwork = RedisNetwork(redisClient, *classes)

    suspend fun start() {
        updateAll()
        subscribeToAll()
    }

    private suspend fun updateAll() {
        redisNetwork.getAll()
            .forEach { (clazz, map) ->
                map.forEach { (id, obj) ->
                    unsafeLocalCacheSet(clazz, id, obj)
                }
            }
        }

    private suspend fun subscribeToAll() {
        redisNetwork.subscribe()
            .map {
                it to getFromLocalCache(it.clazz, it.id?.value)
            }
            .collect { (change, obj) ->
                when (obj) {
                    null -> {} //cache to add later
                    else -> unsafeLocalCacheSet(change.clazz, change.id?.value, obj.copyWith(change))
                }
            }
    }

    private suspend fun unsafeLocalCacheSet(clazz: KClass<*>, id: String? = null, new: Any) = redisCache.unsafeSet(clazz, id, new)

    fun <T : Any> getFromLocalCache(clazz: KClass<T>, id: String? = null) = redisCache.get(clazz, id)
    inline fun <reified T : Any> getFromLocalCache(id: String? = null) : T? = getFromLocalCache(T::class, id)

    suspend fun <T : Any> setInLocaleCache(clazz: KClass<T>, id: String? = null, new: T) = redisCache.set(clazz, id, new)
    suspend inline fun <reified T : Any> setInLocaleCache(id: String? = null, new: T) = setInLocaleCache(T::class, id, new)

    fun <T : Any> deleteFromLocaleCache(clazz: KClass<T>, id: String? = null) = redisCache.delete(clazz, id)
    inline fun <reified T : Any> deleteFromLocaleCache(id: String? = null) = deleteFromLocaleCache(T::class, id)


    inline fun <reified T: Any> observe(id: String? = null) = redisCache.observe(T::class, id)
    inline fun <reified T: Any> observeAll() = redisCache.observeAll(T::class)

    suspend inline fun <reified T : Any> update(id: String? = null, new: T?) {
        val old = getFromLocalCache<T>(id)
        if (new != null) {
            redisNetwork.change(old = old, new = new)
            setInLocaleCache(id, new)
        } else if (old != null) {
            redisNetwork.delete(old)
            deleteFromLocaleCache<T>(id)
        }
    }
}