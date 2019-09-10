package webdis

import com.google.gson.*
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.features.json.GsonSerializer
import network.RedisClient
import io.ktor.client.features.json.JsonFeature
import io.ktor.client.request.get
import io.ktor.client.request.post
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL


@FlowPreview
class WebdisClient(private val url: String): RedisClient {

    override val delimiter = ":"
    override val allSymbol = "*"

    private val gson = Gson()

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

    override suspend fun getAll(keys: List<String>) = flow {
        keys
            .map {
                getScript(it)
            }
            .map { httpClient.post<EvalResponse>(url) { body = it } }
            .flatMap { it.result }
            .map { it.split("=") }
            .forEach { (key, value) -> emit(key to value) }
    }

    override suspend fun get(key: String) = key to httpClient.get<GetResponse>("$url/GET/$key").result

    override suspend fun put(values: List<Pair<String, String>>) {
        values
            .map { (key, value) -> "$key/$value" }
            .map { req -> httpClient.get<Any?>("$url/SET/$req") }
    }

    override suspend fun subscribe(keys: List<String>): Flow<Pair<String, String>> {
        val messages = flow {
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
                                        emit(key)
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
        return flow { messages
            .map { key -> getAll(key) }
            .collect { (key, value) -> emit(key to value) }
        }
    }
}