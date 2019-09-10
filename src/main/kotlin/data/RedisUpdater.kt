package data

import parsing.ParsingContext
import parsing.getProperty
import kotlin.reflect.KFunction
import kotlin.reflect.full.instanceParameter
import kotlin.reflect.full.memberFunctions


fun <T : Any> T.copyWith(parsingContext: ParsingContext): T {
    val copyConstructor = getCopyConstructor<T>()

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