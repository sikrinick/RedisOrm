package parsing.parsers

import module.RedisClass
import module.RedisId
import module.RedisKey
import parsing.*
import java.util.*
import kotlin.reflect.KClass
import kotlin.reflect.jvm.jvmErasure

class RedisClassParser(
    val clazz: KClass<*>
): RedisTypeParser<ParsingContext?>() {

    val redisClassName = clazz.findAnnotation<RedisClass>()?.name

    val idParam = clazz
            .getConstructorParametersWith<RedisId>()
            ?.firstOrNull()
            ?.first

    val parameters = clazz
            .getConstructorParametersWith<RedisKey>()
            ?.map { (kparam, redisKey) -> RedisClassParameter(kparam, redisKey.name) }
            ?: emptyList()

    val hasId = idParam != null

    private val paramParsers = parameters
        .map { param ->
            param.redisKeyName to RedisParamParser(
                param.kparam,
                when (val type = param.kparam.jvmType) {
                    in RedisParsingSetup.parsers -> RedisParsingSetup.parsers.getValue(type)
                    Map::class -> createMapParser(param.kparam.type.arguments[1].type!!.jvmErasure)
                    else -> RedisClassParser(type)
                }
            )
        }
        .toMap()

    override fun parseFromRedis(keys: Queue<String>, value: String): ParsingContext? {
        val id = if (hasId) keys.poll() else null
        val paramParser = paramParsers[keys.poll()] ?: return null

        val idPair = if (idParam != null && id != null) ParsingContext.Id(idParam, id) else null

        val (kparam, result) = paramParser.parse(keys, value)

        return ParsingContext(
            clazz,
            idPair,
            kparam,
            result
        )
    }

}