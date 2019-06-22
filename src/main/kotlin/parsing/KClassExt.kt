package parsing

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlin.reflect.KClass
import kotlin.reflect.KParameter
import kotlin.reflect.KProperty1
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.memberProperties
import kotlin.reflect.jvm.jvmErasure

val KParameter.jvmType
    get() = type.jvmErasure

inline fun <reified T: Annotation> Array<out KClass<*>>.filterByAnnotation() = this
        .map { it to it.findAnnotation<T>() }
        .filter { (_, annotation) -> annotation != null }
        .map { (clazz, annotation) -> clazz to annotation!! }

inline fun <reified T: Annotation> KClass<*>.getConstructorParametersWith() =
    constructors.firstOrNull()
        ?.parameters
        ?.map { it to it.findAnnotation<T>() }
        ?.filter { (_, annotation) -> annotation != null }
        ?.map { (clazz, annotation) -> clazz to annotation!! }

inline fun <reified T: Annotation> KClass<*>.findAnnotation() =
    annotations.firstOrNull { it is T } as T?

fun KClass<*>.getProperty(name: String?): KProperty1<Any, *> {
    try {
        return this.memberProperties.first { it.name == name } as KProperty1<Any, *>
    } catch (ex: NoSuchElementException) {
        throw NoSuchElementException("There is no property \"$name\" in $this")
    } catch (ex: Exception) {
        throw Exception("Property \"$name\" in $this should be PUBLIC")
    }
}

fun <F, S> Flow<Pair<F?, S?>>.filterPairNotNull() = this
        .filter { (f, s) -> f != null && s != null }
        .map { (f, s) -> Pair(f!!, s!!) }