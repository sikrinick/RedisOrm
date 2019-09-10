package data

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.launch
import kotlin.reflect.KClass

@FlowPreview
class RedisLocalCache(
    vararg classes: KClass<*>
) {

    private val ioScope = CoroutineScope(Dispatchers.IO)
    private val cache = classes
            .map { clazz -> clazz to RedisTable<Any>(ioScope) }
            .toMap()

    fun <T : Any> get(clazz: KClass<T>, id: String?) = table(clazz)[id]
    fun <T : Any> getAll(clazz: KClass<T>) = table(clazz).getAll()
    fun <T : Any> set(clazz: KClass<T>, id: String?, new: T) = table(clazz).set(id, new)
    fun <T : Any> delete(clazz: KClass<T>, id: String?) = table(clazz).delete(id)

    fun <T : Any> observe(clazz: KClass<T>, id: String? = null) = table(clazz).observe(id)
    fun <T : Any> observeAll(clazz: KClass<T>) = table(clazz).observeAll()

    private fun <T : Any> table(clazz: KClass<T>) = (cache[clazz] ?: error("There is not such class ${clazz.simpleName}")) as RedisTable<T>

    fun unsafeSet(clazz: KClass<*>, id: String?, new: Any) = unsafeTable(clazz).set(id, new)
    private fun unsafeTable(clazz: KClass<*>) = (cache[clazz] ?: error("There is not such class ${clazz.simpleName}"))
}

@FlowPreview
class RedisTable<T : Any>(
    private val ioScope: CoroutineScope
) {

    private val table = mutableMapOf<String?, T>()
    private val allCollectors = mutableSetOf<FlowCollector<Collection<T>>>()
    private val collectors = mutableMapOf<String?, MutableSet<FlowCollector<T>>>()

    fun getAll(): Collection<T> = table.values

    operator fun get(id: String?) = table[id]

    operator fun set(id: String?, new: T) {
        val old = table[id]
        table[id] = new
        if (old != new) {
            collectors[id]?.forEach {
                ioScope.launch { it.emit(new) }
            }
            allCollectors.forEach {
                ioScope.launch { it.emit(getAll()) }
            }
        }
    }

    fun delete(id: String?) {
        table.remove(id)
    }

    fun observeAll() = flow {
        emit(getAll())
        allCollectors.add(this)
    }.onCompletion {
        allCollectors.remove(this)
    }.flowOn(ioScope.coroutineContext)


    fun observe(id: String? = null) = flow {
        emit(get(id))
        collectors.getOrPut(id) { mutableSetOf() }.add(this)
    }.onCompletion {
        collectors[id]?.also {
            it.remove(this)
            if (it.isEmpty()) collectors.remove(id)
        }
    }
    .flowOn(ioScope.coroutineContext)

}