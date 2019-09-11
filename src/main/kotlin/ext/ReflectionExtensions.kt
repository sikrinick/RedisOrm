package ext

import parsing.ParsingContext
import parsing.getProperty
import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.reflect.full.instanceParameter
import kotlin.reflect.full.memberFunctions
import kotlin.reflect.jvm.jvmErasure


fun <T : Any> T.copyWith(parsingContext: ParsingContext): T {
    val copyConstructor = getCopyConstructor()

    val value = when(parsingContext.result) {
        is ParsingContext -> {
            val property = this::class.getProperty(parsingContext.param.name)
            property.get(this)!!.copyWith(parsingContext.result)
        }
        else -> parsingContext.result
    }

    return copyConstructor.callBy(
        mapOf(
            copyConstructor.instanceParameter!! to this,
            copyConstructor.parameters.first { it.name == parsingContext.param.name!! } to value
        )
    )
}

fun createObject(clazz: KClass<*>, contexts: List<ParsingContext>): Any {
    val id = if (contexts.isNotEmpty()) contexts[0].id else null

    val paramList = contexts.groupBy { it.param }
        .map { (param, values) ->
            param to if (values.size == 1) {
                values[0].result
            } else {
                createObject(param.type.jvmErasure, values)
            }
        }
        .let {
            if (id != null) it + (id.kParameter to id.value) else it
        }

    val paramSize = paramList.size + if (id != null) 1 else 0
    val constructor = clazz.constructors.first { it.parameters.size == paramSize }

    return constructor.callBy(paramList.toMap())
}

private fun <T : Any> T.getCopyConstructor(): KFunction<T> {
    try {
        return this::class.memberFunctions
            .first { it.name == "copy" } as KFunction<T>
    } catch (ex: Exception) {
        throw NoSuchElementException(
            "No \"copy\" constructor for ${this::class}. " +
                    "You should use \"data class\" or provide your own copy constructor")
    }
}