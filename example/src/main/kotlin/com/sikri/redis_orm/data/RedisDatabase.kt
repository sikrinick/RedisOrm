package com.sikri.redis_orm.data

import com.sikri.redis_orm.utils.IndentedPrinter
import com.tylerthrailkill.helpers.prettyprint.pp
import kotlin.reflect.KClass

class RedisDatabase(
        vararg classes: KClass<*>
) {

    private val db = classes
            .map { clazz -> clazz to RedisTable<Any>(clazz) }
            .toMap()

    fun <T : Any> setByClass(clazz: KClass<out T>, id: String?, obj: T?) {
        db[clazz]?.set(id, obj)
    }
    @Suppress("UNCHECKED_CAST")
    fun <T : Any> getByClass(clazz: KClass<out T>, id: String?) = db[clazz]?.get(id) as T?

    inline operator fun <reified T: Any> set(clazz: KClass<*>, id: String?, obj: T) = setByClass(clazz, id, obj)
    inline operator fun <reified T: Any> get(id: String?)= getByClass(T::class, id)

    inline fun <reified T: Any> set(obj: T) = setByClass(T::class, null, obj)
    inline fun <reified T: Any> get()= getByClass(T::class, null)

    fun print() = db.forEach { (_, table) ->
        table.print()
        println()
    }
}

class RedisTable<T: Any>(private val clazz: KClass<*>) {

    private val table = mutableMapOf<String?, T?>()

    operator fun get(id: String?) = table[id]
    operator fun set(id: String?, obj: T?) = table.set(id, obj)

    fun print() {
        println("Table ${clazz.simpleName} : (")
        table.forEach { (_, obj) -> pp(obj, writeTo = IndentedPrinter(2)) }
        println(")")
    }
}