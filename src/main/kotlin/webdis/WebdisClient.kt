package webdis

import com.google.gson.*
import io.ktor.client.HttpClient
import io.ktor.client.call.call
import io.ktor.client.engine.cio.CIO
import io.ktor.client.features.json.GsonSerializer
import network.RedisClient
import io.ktor.client.features.json.JsonFeature
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.request
import io.ktor.client.response.HttpResponse
import io.ktor.http.cio.decodeChunked
import io.ktor.http.cookies
import io.ktor.http.encodeCookieValue
import io.ktor.network.util.ioCoroutineDispatcher
import io.ktor.util.InternalAPI
import io.ktor.util.decodeString
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.io.ByteReadChannel
import kotlinx.coroutines.io.readUTF8LineTo
import kotlinx.io.core.ExperimentalIoApi
import kotlinx.io.core.readUTF8Line
import kotlinx.io.core.readUTF8LineTo
import network.requests.RedisSendingChange
import java.io.BufferedReader
import java.io.InputStreamReader
import java.lang.StringBuilder
import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.StandardCharsets
import kotlin.coroutines.coroutineContext


@FlowPreview
class WebdisClient(private val url: String): RedisClient {

    override val delimiter = ":"
    override val allSymbol = "*"

    private val gson = Gson()

    private val ioScope = CoroutineScope(Dispatchers.IO)

    private val httpClient = HttpClient(CIO) {
        install(JsonFeature) {
            serializer = GsonSerializer()
        }
    }

    private fun getScript(key: String) =
        """
        EVAL/
        local result = {}
        local keys=redis.call('keys','$key')
        for k,v in ipairs(keys) do
            result[k] = v..'='..redis.call('GET', v)
        end
        return result
        /0
        """.trimIndent()

    override fun getAll(keys: List<String>) = keys
        .asFlow()
        .map { getScript(it) }
        .map { httpClient.post<EvalResponse>(url) { body = it } }
        .flatMapMerge { it.result.asFlow() }
        .map { it.split("=") }
        .map { (key, value) -> key to value }

    override suspend fun get(key: String) = key to httpClient.get<GetResponse>("$url/GET/$key").result

    override suspend fun applyChange(changes: List<RedisSendingChange>) {
        changes.forEach {
            when(it) {
                is RedisSendingChange.SetField -> httpClient.get<Any?>("$url/SET/${it.key}/${it.value}")
                is RedisSendingChange.DelField -> httpClient.get<Any?>("$url/DEL/${it.key}")
            }
        }
    }

    fun subscribe2(keys: List<String>) = flow {
        val keyspace = "__keyspace@0__:"
        val req = keys.map { "$keyspace$it" }.reduce { acc, s -> "$acc/$s" }
        val url = "$url/PSUBSCRIBE/$req"


        val PART_SIZE = 1024
        val BUFFER_SIZE = PART_SIZE * 50
        val part = CharArray(PART_SIZE)
        val buffer = CharArray(BUFFER_SIZE)
        var charsInBuffer = 0
        while (true) {
            val connection = (URL(url).openConnection() as HttpURLConnection).apply {
                setRequestProperty("Cookie", "CUSTOM_KEEPALIVE_TIMEOUT=${Integer.MAX_VALUE}")
                //setChunkedStreamingMode(BUFFER_SIZE)
                doInput = true
                doOutput = true
            }

            val reader = BufferedReader(InputStreamReader(connection.inputStream))

            while (true) {
                val charactersReadFromChunk = reader.read(part)
                if (charactersReadFromChunk > 0) {
                    System.arraycopy(part, 0, buffer, charsInBuffer, charactersReadFromChunk)
                    charsInBuffer += charactersReadFromChunk
                    val bufferString = String(buffer, 0, charsInBuffer)
                    var parsedChars = 0
                    try {
                        val response = gson.fromJson(bufferString, PSubscribeResponse::class.java)
                        parsedChars += bufferString.length
                        val list = response.result
                        if (list.size == 4) {
                            when(list[3]) {
                                "set" -> {
                                    val key = list[2].removePrefix(keyspace)
                                    emit(get(key))
                                }
                            }
                        }
                        charsInBuffer = 0
                    } catch (e: JsonParseException) {
                        //json part - on the end of input - move it to buffer
                        val remainingText = bufferString.substring(parsedChars)
                        charsInBuffer = remainingText.length
                        if (charsInBuffer > 0) {
                            System.arraycopy(
                                remainingText.toCharArray(), 0, buffer, 0,
                                remainingText.length
                            )
                        }
                    } catch (e: Exception) {
                        println(e.printStackTrace())
                    }
                }
            }
        }
    }

    companion object {
        const val KEYSPACE_PREFIX = "__keyspace@0__:"
        const val PSUBSCRIBE_TYPE = "PSUBSCRIBE"
    }

    @ExperimentalCoroutinesApi
    override fun subscribe(keys: List<String>) = channelFlow<Pair<String, String?>> {
        val req = keys.map { "$KEYSPACE_PREFIX$it" }.reduce { acc, s -> "$acc/$s" }
        val url = "$url/$PSUBSCRIBE_TYPE/$req"

        val cache = StringBuilder()

        val responseChannel = httpClient.request<ByteReadChannel>(url) {
            header("Cookie", "CUSTOM_KEEPALIVE_TIMEOUT=${Integer.MAX_VALUE}")
        }
        decodeChunked(responseChannel).channel.read { buffer ->
            val decodedStr = StandardCharsets.UTF_8.decode(buffer).toString()
            cache.append(decodedStr)
            runCatching { gson.fromJson(cache.toString(), PSubscribeResponse::class.java) }
                .onSuccess { response ->
                    cache.clear()
                    val list = response.result
                    if (list.size == 4) {
                        val (messageType, observedKey, receivedKey, method) = list
                        val key = receivedKey.removePrefix(KEYSPACE_PREFIX)
                        when (method) {
                            "set" -> {
                                launch { send(get(key)) }
                            }
                            "del" -> {
                                launch { send(key to null) }
                            }
                        }
                    }
                }
                .onFailure { println(it) }
        }
    }
}