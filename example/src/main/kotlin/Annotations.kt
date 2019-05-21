
import kotlin.reflect.KClass
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.primaryConstructor


@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class RedisClass(
    val name: String
)

@Target(AnnotationTarget.PROPERTY, AnnotationTarget.VALUE_PARAMETER)
@Retention(AnnotationRetention.RUNTIME)
annotation class RedisId

@Target(AnnotationTarget.PROPERTY, AnnotationTarget.VALUE_PARAMETER)
@Retention(AnnotationRetention.RUNTIME)
annotation class RedisKey(
    val name: String
)

inline fun <reified T: Annotation> KClass<*>.getConstructorParametersWith() =
    primaryConstructor?.parameters
        ?.map { it to it.findAnnotation<T>() }
        ?.filter { (_, annotation) -> annotation != null }
        ?.map { (clazz, annotation) -> clazz to annotation!! }
