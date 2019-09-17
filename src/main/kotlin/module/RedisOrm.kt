package module

import data.RedisLocalCache
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import network.RedisClient
import network.RedisNetwork
import javax.xml.ws.Dispatch
import kotlin.reflect.KClass

@FlowPreview
@ExperimentalCoroutinesApi
class RedisOrm(
    redisClient: RedisClient,
    vararg classes: KClass<*>
) {
    val redisCache = RedisLocalCache(*classes)
    val redisNetwork = RedisNetwork(redisClient, *classes)
    private val ioScope = CoroutineScope(Dispatchers.IO)

    fun start() {
        redisNetwork.observeAll()
            .map {
                it to (getFromLocalCache(it.clazz, it.id?.value) ?: createObject(it.clazz, it.id))
            }
            .onEach { (change, obj) ->
                unsafeLocalCacheSet(change.clazz, change.id?.value, obj.copyWith(change))
            }
            .launchIn(ioScope)
    }

    private suspend fun unsafeLocalCacheSet(clazz: KClass<*>, id: String? = null, new: Any) = redisCache.unsafeSet(clazz, id, new)

    fun <T : Any> getFromLocalCache(clazz: KClass<T>, id: String? = null) = redisCache.get(clazz, id)
    inline fun <reified T : Any> getFromLocalCache(id: String? = null) : T? = getFromLocalCache(T::class, id)

    suspend fun <T : Any> setInLocalCache(clazz: KClass<T>, id: String? = null, new: T) = redisCache.set(clazz, id, new)
    suspend inline fun <reified T : Any> setInLocalCache(id: String? = null, new: T) = setInLocalCache(T::class, id, new)

    fun <T : Any> deleteFromLocalCache(clazz: KClass<T>, id: String? = null) = redisCache.delete(clazz, id)
    inline fun <reified T : Any> deleteFromLocalCache(id: String? = null) = deleteFromLocalCache(T::class, id)


    inline fun <reified T: Any> observe(id: String? = null) = redisCache.observe(T::class, id)
    inline fun <reified T: Any> observeAll() = redisCache.observeAll(T::class)

    suspend inline fun <reified T : Any> update(id: String? = null, new: T?) {
        val old = getFromLocalCache<T>(id)
        if (new != null) {
            redisNetwork.change(old = old, new = new)
            setInLocalCache(id, new)
        } else if (old != null) {
            redisNetwork.delete(old)
            deleteFromLocalCache<T>(id)
        }
    }
}