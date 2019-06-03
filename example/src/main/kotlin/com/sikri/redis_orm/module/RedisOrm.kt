package com.sikri.redis_orm.module

import com.sikri.redis_orm.data.RedisDatabase
import com.sikri.redis_orm.data.RedisUpdater
import com.sikri.redis_orm.network.RedisNetwork
import com.sikri.redis_orm.parsing.filterByAnnotation
import com.sikri.redis_orm.parsing.filterPairNotNull
import com.tylerthrailkill.helpers.prettyprint.pp
import kotlin.reflect.KClass

class RedisOrm(
        vararg classes: KClass<*>
) {
    private val redisClasses = classes.filterByAnnotation<RedisClass>()

    private val redisDatabase = RedisDatabase(*classes)
    private val redisNetwork = RedisNetwork(redisClasses)


    fun updateAll() {
        redisNetwork.getAll().forEach { (clazz, map) ->
            map.forEach { (id, obj) ->
                redisDatabase[clazz, id] = obj
            }
        }
        redisDatabase.print()
    }

    fun subscribe() {
        redisNetwork.observe()
                .filterNotNull()
                .map {
                    it to redisDatabase.getByClass(it.clazz, it.id?.value)
                }
                .filterPairNotNull()
                .map {  (context, obj) ->
                    println("\n\nChanged object:\n")
                    pp(obj)
                    context to RedisUpdater(obj).update(context)
                }
                .filterPairNotNull()
                .map { (context, obj) ->
                    redisDatabase[context.clazz, context.id?.value] = obj
                    println("\nto object:\n")
                    pp(obj)
                    println("\n\n")
                }
                .forEach {
                    redisDatabase.print()
                }
    }



}