package mock_example

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import module.RedisOrm
import network.RedisClient
import network.requests.RedisSendingChange

@FlowPreview
@ExperimentalCoroutinesApi
fun main() {

    val job = Job()
    val compScope = CoroutineScope(Dispatchers.Default + job)
    val ioScope = CoroutineScope(Dispatchers.IO + job)

    val redisOrm = RedisOrm(
        MockRedisClient(),

        Device::class,
        ComplicatedDevice::class,

        Room::class,
        Scene::class,
        School::class,

        HAEngine::class,
        Stb::class
    )

    compScope.launch {
        redisOrm.observe<Device>("0-123").collect { device ->
            println(device)
        }
    }

    ioScope.launch {
        redisOrm.start()
    }

    runBlocking { delay(1000) }

    compScope.launch {
        println("Launched")
        redisOrm.observeAll<Device>()
            .onCompletion { println("Finished or cancelled observeAll") }
            .collect { devices ->
                println(devices)
            }
        println("Done")
    }

    compScope.launch {
        delay(1000)
        job.cancel()
    }

    runBlocking { job.join() }

    println("Finished main")
}

class MockRedisClient: RedisClient {

    override val delimiter = ":"
    override val allSymbol = "*"

    override suspend fun getAll(keys: List<String>) = flowOf(
        "device:0-123:name" to "hello, world",
        "device:0-123:version" to "1.0.0",
        "device:0-123:class" to "Camera",

        "scene:1:name" to "runaway",

        "room:R3:name" to "kitchen",
        "room:R1:name" to "toalet",
        "room:R2:name" to "hall",

        "complicated_device:1:state" to "on",
        "complicated_device:1:version" to "1.01",
        "complicated_device:1:meta:name" to "kitchen_cam",
        "complicated_device:1:meta:class" to "camera",

        "school:1:director" to "Victoria",
        "school:1:room:1:teacher" to "Mackey",
        "school:1:room:1:name" to "physics",
        "school:1:room:2:teacher" to "Harrison",
        "school:1:room:2:name" to "chemistry",
        "school:2:director" to "Skinner",
        "school:2:room:1:teacher" to "Krabappel",
        "school:2:room:1:name" to "english",
        "school:2:room:2:teacher" to "Hoover",
        "school:2:room:2:name" to "polish",

        "haengine:version" to "1",
        "stb:upgrade:bluetooth" to "enabled"
    )

    override suspend fun subscribe(keys: List<String>) = flowOf(
        "device:0-123:name" to "Big fucking change!",
        "haengine:version" to "2",
        "stb:upgrade:bluetooth" to "disabled"
    )


    override suspend fun get(key: String) = Pair("mock", "mock")
    override suspend fun applyChange(changes: List<RedisSendingChange>) {
        println("Changes:")
        changes.forEach { println(it) }
        println("\n")
    }
}