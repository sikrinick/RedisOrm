package parsing.parsers

import module.RedisId
import module.RedisKey
import parsing.ParsingContext
import parsing.RedisParsingSetup
import parsing.getConstructorParametersWith
import parsing.jvmType
import java.util.*
import kotlin.reflect.KClass

class RedisClassParser(
        private val clazz: KClass<*>
): RedisTypeParser<ParsingContext?>() {

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
                    Map::class -> createMapParser(param.kparam.type)
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

    override fun parseToRedis(keys: Queue<String>, value: ParsingContext?): String {

        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}