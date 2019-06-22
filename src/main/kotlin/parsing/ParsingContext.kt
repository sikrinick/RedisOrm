package parsing

import kotlin.reflect.KClass
import kotlin.reflect.KParameter

data class ParsingContext(
    val clazz: KClass<*>,
    val id: Id?,
    val param: KParameter,
    val result: Any?
) {
    data class Id(val kParameter: KParameter, val value: String?)
}
