package com.sikrinick.redis_orm.result_example

import com.sikrinick.redis_orm.annotations.RedisClass
import com.sikrinick.redis_orm.annotations.RedisId
import com.sikrinick.redis_orm.annotations.RedisKey
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.launch

@RedisClass("device")
data class Device(
    @RedisId
    val id: String,
    @RedisKey("name")
    val name: String,
    @RedisKey("version")
    val version: String
)

@FlowPreview
object RedisOrm {

    private val ioScope = CoroutineScope(Dispatchers.IO)

    private val localCache = object {
        val deviceTable = object {

            val table = mutableMapOf<String, Device>()
            val listObservers = mutableSetOf<FlowCollector<Collection<Device>>>()
            val observers = mutableMapOf<String, MutableSet<FlowCollector<Device>>>()

            private fun getAll(): Collection<Device> = table.values

            operator fun get(id: String) = table[id]

            operator fun set(id: String, new: Device) {
                val old = table[id]
                table[id] = new
                if (old != new) {
                    observers[id]?.forEach {
                        ioScope.launch {
                            it.emit(new)
                        }
                    }
                    listObservers.forEach {
                        ioScope.launch { it.emit(getAll()) }
                    }
                }
            }

            fun observeAll() = flow {
                emit(getAll())
                listObservers.add(this)
            }
                .onCompletion { listObservers.remove(this) }
                .flowOn(ioScope.coroutineContext)

            fun observe(id: String) = flow {
                emit(get(id))
                observers
                    .getOrPut(id) { mutableSetOf() }
                    .add(this)
            }
                .onCompletion { observers[id]?.remove(this) }
                .flowOn(ioScope.coroutineContext)

        }
    }

    private val parser = object {



    }
}