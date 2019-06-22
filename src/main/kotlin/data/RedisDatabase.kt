package data

import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.flow
import kotlin.reflect.KClass

@FlowPreview
class RedisDatabase(
        vararg classes: KClass<*>
) {

    private val observables = mutableMapOf<Pair<KClass<*>, String?>, MutableList<FlowCollector<Any?>>>()
    private val db = classes
            .map { clazz -> clazz to RedisTable<Any>() }
            .toMap()

    @Suppress("UNCHECKED_CAST")
    fun get(clazz: KClass<*>, id: String?)= db[clazz]?.get(id)

    suspend fun set(clazz: KClass<*>, id: String?, new: Any?) {
        val old = db[clazz]?.get(id)
        db[clazz]?.set(id, new)
        if (old != new) {
            observables[Pair(clazz, null)]?.forEach {
                it.emit(new)
            }
            if (id != null) {
                observables[Pair(clazz, id)]?.forEach {
                    it.emit(new)
                }
            }
        }
    }

    fun subscribe(clazz: KClass<*>, id: String? = null) = flow<Any?> {
        if (id != null) {
            get(clazz, id)?.let { emit(it) }
        } else {
            db[clazz]?.getAll()?.forEach { emit(it) }
        }
        val key = Pair(clazz, id)
        observables.getOrPut(key) { mutableListOf() }.add(this)
    }
}

class RedisTable<T: Any> {

    private val table = mutableMapOf<String?, T?>()

    fun getAll(): Collection<T?> = table.values
    operator fun get(id: String?) = table[id]
    operator fun set(id: String?, obj: T?) = table.set(id, obj)

}