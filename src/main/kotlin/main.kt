import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collect
import module.RedisOrm
import webdis.WebdisClient


@FlowPreview
@UseExperimental(ExperimentalCoroutinesApi::class)
suspend fun main() {
    val redisOrm = RedisOrm(
        WebdisClient("http://192.168.1.220:7379"),
        Room::class
    )
    redisOrm.observeAll<Room>().collect {
        println("Received: $it")
    }

    redisOrm.start()

    redisOrm.observeAll<Room>().collect {
        val room = it.drop(2).single()
        val newName = "RedisOrmTest"//Random.nextInt().toString()
        println("Changing $it with name ${room.name} to $newName")
        redisOrm.update(room.id, room.copy(name = newName))
    }
}