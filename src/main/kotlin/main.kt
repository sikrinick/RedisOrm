import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.withContext
import module.RedisOrm
import webdis.WebdisClient
import kotlin.random.Random


@FlowPreview
suspend fun main() {
    withContext(Dispatchers.Default) {
        val redisOrm = RedisOrm(
            WebdisClient("http://192.168.1.220:7379"),
            Room::class
        )
        redisOrm.subscribe<Room>().collect {
            println("Received: $it")
        }

        redisOrm.start()

        redisOrm.subscribe<Room>().filterNotNull().drop(2).take(1).collect {
            val newName = "RedisOrmTest"//Random.nextInt().toString()
            println("Changing $it with name ${it.name} to $newName")
            redisOrm.update(it.id, it.copy(name = newName))
        }
    }
}