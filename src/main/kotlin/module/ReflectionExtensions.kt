package module

import parsing.ParsingContext
import parsing.getProperty
import java.lang.IllegalArgumentException
import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.reflect.full.*


fun createObject(clazz: KClass<*>, idParam: ParsingContext.Id?): Any {
    val constructor = clazz.constructors.first()
    val parameters = idParam?.let { mapOf(idParam.kParameter to idParam.value) } ?: emptyMap()
    return constructor.callBy(parameters)
}


// if data class -> copy and change
// if map -> toMutableMap().put(parsingContext.id?.value, tdtd)
// if class -> first constructor

fun Any.copyWith(parsingContext: ParsingContext): Any {
    val value = when(parsingContext.result) {
        is ParsingContext -> {
            val property = this::class.getProperty(parsingContext.param.name)
            property.get(this)!!.copyWith(parsingContext.result)
        }
        else -> parsingContext.result
    }

    return when {
        this::class.isData -> {
            getCopyConstructor().let {
                it.callBy(
                    mapOf(
                        it.instanceParameter!! to this,
                        it.parameters.first { param -> param.name == parsingContext.param.name!! } to value
                    )
                )
            }
        }
        this::class.isSubclassOf(Map::class) -> {
            val id = parsingContext.id?.value
            val map = LinkedHashMap(this as Map<String?, out Any>)
            val obj = map.getOrElse(id) { createObject(parsingContext.clazz, parsingContext.id) }.copyWith(parsingContext)
            map[id] = obj
            return map
        }
        else -> throw IllegalArgumentException("Please, use data class or Map<String?, YourDataClassType> for ${this::class.simpleName}")
    }
}

private fun <T : Any> T.getCopyConstructor(): KFunction<T> {
    try {
        return this::class.memberFunctions.first { it.name == "copy" } as KFunction<T>
    } catch (ex: Exception) {
        throw NoSuchElementException(
            "No \"copy\" constructor for ${this::class}. " +
                    "You should use \"data class\" or provide your own copy constructor")
    }
}