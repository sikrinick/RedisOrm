package com.sikri.redis_orm.data

import com.sikri.redis_orm.parsing.ParsingContext
import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.reflect.KParameter
import kotlin.reflect.KProperty1
import kotlin.reflect.full.IllegalCallableAccessException
import kotlin.reflect.full.instanceParameter
import kotlin.reflect.full.memberFunctions
import kotlin.reflect.full.memberProperties

class RedisUpdater(private val obj: Any) {

    fun update(parsingContext: ParsingContext): Any? {
        val copyConstructor = obj.getCopyConstructor()

        val value = when(parsingContext.result) {
            is ParsingContext -> {
                val property = obj.getProperty(parsingContext.param.name)
                RedisUpdater(property).update(parsingContext.result)
            }
            else -> parsingContext.result
        }

        return copyConstructor.callBy(
                mapOf(
                        copyConstructor.instanceParameter!! to obj,
                        copyConstructor.parameters.first { it.name == parsingContext.param.name!! } to value
                )
        )
    }

    private fun Any.getCopyConstructor(): KFunction<*> {
        try {
            return this::class.memberFunctions
                    .first { it.name == "copy" }
        } catch (ex: Exception) {
            throw NoSuchElementException(
                    "No \"copy\" constructor for ${this::class}. " +
                            "You should use \"data class\" or provide your own copy constructor")
        }
    }

    private fun Any.getProperty(name: String?): Any {
        try {
            val property = this::class.memberProperties
                    .first { it.name == name } as KProperty1<Any, *>
            return property.get(this)!!
        } catch (ex: IllegalCallableAccessException) {
            throw IllegalAccessException("Property \"$name\" in ${this::class} should be PUBLIC")
        } catch (ex: NoSuchElementException) {
            throw NoSuchElementException("There is no property \"$name\" in ${this::class}")
        }
    }

}