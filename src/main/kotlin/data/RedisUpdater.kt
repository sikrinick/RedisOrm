package data

import parsing.ParsingContext
import parsing.getProperty
import kotlin.reflect.KFunction
import kotlin.reflect.full.instanceParameter
import kotlin.reflect.full.memberFunctions


fun Any.copyWith(parsingContext: ParsingContext): Any {
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

private fun Any.getCopyConstructor(): KFunction<Any> {
    try {
        return this::class.memberFunctions
                .first { it.name == "copyWith" } as KFunction<Any>
    } catch (ex: Exception) {
        throw NoSuchElementException(
                "No \"copyWith\" constructor for ${this::class}. " +
                        "You should use \"data class\" or provide your own copyWith constructor")
    }
}