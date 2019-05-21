package parsing

import kotlin.reflect.KClass
import kotlin.reflect.KParameter

data class ParsingContext(
    val clazz: KClass<*>,
    val id: Pair<KParameter, String>?,
    val param: KParameter,
    val result: Any?
)