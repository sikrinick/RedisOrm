import module.RedisClass
import module.RedisId
import module.RedisKey

@RedisClass("room")
data class Room(
    @RedisId
    val id: String,
    @RedisKey("name")
    val name: String
)