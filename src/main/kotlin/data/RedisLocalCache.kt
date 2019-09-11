package data

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.*
import kotlinx.coroutines.flow.*
import kotlin.reflect.KClass

@FlowPreview
@ExperimentalCoroutinesApi
class RedisLocalCache(
    vararg classes: KClass<*>
) {

    private val cache = classes
            .map { clazz -> clazz to RedisTable<Any>() }
            .toMap()

    fun <T : Any> get(clazz: KClass<T>, id: String?) = table(clazz)[id]
    fun <T : Any> getAll(clazz: KClass<T>) = table(clazz).getAll()
    suspend fun <T : Any> set(clazz: KClass<T>, id: String?, new: T) = table(clazz).set(id, new)
    fun <T : Any> delete(clazz: KClass<T>, id: String?) = table(clazz).delete(id)

    fun <T : Any> observe(clazz: KClass<T>, id: String? = null) = table(clazz).observe(id)
    fun <T : Any> observeAll(clazz: KClass<T>) = table(clazz).observeAll()

    private fun <T : Any> table(clazz: KClass<T>) = (cache[clazz] ?: error("There is not such class ${clazz.simpleName}")) as RedisTable<T>

    suspend fun unsafeSet(clazz: KClass<*>, id: String?, new: Any) = unsafeTable(clazz).set(id, new)
    private fun unsafeTable(clazz: KClass<*>) = (cache[clazz] ?: error("There is not such class ${clazz.simpleName}"))
}

@FlowPreview
@ExperimentalCoroutinesApi
class RedisTable<T : Any> {

    private val table = mutableMapOf<String?, T>()
    private val allChannel: BroadcastChannel<Collection<T>> = ConflatedBroadcastChannel()
    private val channels = mutableMapOf<String?, MutableSet<SendChannel<T>>>()

    fun getAll(): Collection<T> = table.values

    operator fun get(id: String?) = table[id]

    suspend fun set(id: String?, new: T) {
        val old = table[id]
        table[id] = new
        if (old != new) {
            channels[id]?.forEach {
                //ioScope.launch {
                    it.send(new)
                //}
            }
            //ioScope.launch {
                allChannel.send(getAll())
            //}
        }
    }

    fun delete(id: String?) {
        table.remove(id)
    }

    fun observeAll() = allChannel.asFlow()

    fun observe(id: String? = null) = channelFlow<T> {
        channels.getOrPut(id) { mutableSetOf() }.add(channel)
        get(id)?.let { send(it) }
        awaitClose { channels.remove(id) }
    }
        //return channel.consumeAsFlow().onCompletion {
        //    channels[id]?.also {
         //       it.remove(channel)
         //       if (it.isEmpty())
         //   }
       // }
    //}
}