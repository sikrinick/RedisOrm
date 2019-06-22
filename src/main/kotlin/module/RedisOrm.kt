package module

import data.RedisDatabase
import data.copyWith
import kotlinx.coroutines.*
import network.RedisClient
import network.RedisNetwork
import parsing.filterByAnnotation
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.map
import parsing.filterPairNotNull
import kotlin.reflect.KClass

@FlowPreview
class RedisOrm(
    redisClient: RedisClient,
    vararg classes: KClass<*>
) {

    private val redisClasses = classes.filterByAnnotation<RedisClass>()

    val redisDatabase = RedisDatabase(*classes)
    val redisNetwork = RedisNetwork(redisClient, redisClasses)

    suspend fun start() {
        updateAll()
        GlobalScope.launch {
            subscribeToAll()
        }
    }

    private suspend fun updateAll() {
        redisNetwork.getAll().forEach { (clazz, map) ->
            map.forEach { (id, obj) ->
                set(clazz, id, obj)
            }
        }
    }

    private suspend fun subscribeToAll() {
        redisNetwork.subscribe()
            .map {
                it to get(it.clazz, it.id?.value)
            }
            .filterPairNotNull()
            .map { (context, obj) ->
                /* todo
                return when {
                    obj == null && context.result != null -> {}//cache to add later
                    obj != null && context.result == null -> {} //sign to delete if not null
                    obj != null && context.result != null -> context to obj.copyWith(context)
                }*/
                context to obj.copyWith(context)
            }
            .collect { (context, obj)  ->
                set(context.clazz, context.id?.value, obj)
            }
    }

    fun get(clazz: KClass<*>, id: String? = null) = redisDatabase.get(clazz, id)
    suspend fun set(clazz: KClass<*>, id: String? = null, new: Any?) = redisDatabase.set(clazz, id, new)

    suspend inline fun <reified T: Any> update(id: String? = null, new: T?) {
        val old = get(T::class, id)
        when {
            old == null && new != null -> redisNetwork.add(new)
            old != null && new != null -> redisNetwork.update(old, new)
            old != null && new == null -> redisNetwork.delete(old)
        }
        set(T::class, id, new)
    }

    inline fun <reified T: Any> get(id: String? = null) : T? = get(T::class, id) as T?
    inline fun <reified T: Any> subscribe(id: String? = null): Flow<T?> = redisDatabase.subscribe(T::class, id).map { it as T? }
}