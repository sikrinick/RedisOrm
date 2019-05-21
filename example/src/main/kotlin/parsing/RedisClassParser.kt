package parsing

import RedisClass
import RedisId
import RedisKey
import getConstructorParametersWith
import java.util.*
import kotlin.reflect.KClass
import kotlin.reflect.KParameter
import kotlin.reflect.full.findAnnotation

class RedisClassParser(
    val clazz: KClass<*>,
    val redisClassName: String?,
    val idParam: KParameter?,
    parameters: List<RedisClassParameter>
): RedisTypeParser() {

    val hasId = idParam != null

    private val paramParsers = parameters
        .map { param ->
            param.redisKeyName to RedisParamParser(
                param.kparam,
                when(val type = param.kparam.jvmType) {
                    in RedisParsingSetup.parsers -> RedisParsingSetup.parsers.getValue(type)
                    Map::class -> RedisParsingSetup.createMapParser(param.kparam.type)
                    else -> create(type)
                }
            )
        }
        .toMap()

    override fun parse(fields: Queue<String>, value: String): ParsingContext? {
        val id = if (hasId) fields.poll() else null
        val paramParser = paramParsers[fields.poll()] ?: return null

        val idPair = if (idParam != null && id != null) idParam to id else null

        val (kparam, result) = paramParser.parse(fields, value)

        return ParsingContext(
            clazz,
            idPair,
            kparam,
            result
        )
    }

    companion object {

        fun create(vararg classes: KClass<*>) = classes
            .map { it to it.findAnnotation<RedisClass>() }
            .filter { (_, annotation) -> annotation != null }
            .map { (clazz, annotation) -> clazz to annotation!! }
            .map { (clazz, redisClass) -> redisClass.name to create(clazz) }
            .toMap()

        fun create(clazz: KClass<*>, redisClassName: String? = null): RedisClassParser {
            val idParam = clazz.getConstructorParametersWith<RedisId>()
                ?.firstOrNull()
                ?.first

            val kparams = clazz.getConstructorParametersWith<RedisKey>()
                ?.map { (kparam, redisKey) -> RedisClassParameter(kparam, redisKey.name) }
                ?: emptyList()

            return RedisClassParser(
                clazz = clazz,
                redisClassName = redisClassName,
                idParam = idParam,
                parameters = kparams
            )
        }
    }
}

class RedisClassParameter(
    val kparam: KParameter,
    val redisKeyName: String
)