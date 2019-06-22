package mock_example

import module.RedisClass
import module.RedisId
import module.RedisKey


@RedisClass("device")
data class Device(
    @RedisId
    val id: String,
    @RedisKey("name")
    val name: String,
    @RedisKey("version")
    val version: String
)

@RedisClass("scene")
data class Scene(
    @RedisId
    val id: String,
    @RedisKey("name")
    val name: String
)

@RedisClass("room")
data class Room(
    @RedisId
    val roomId: String,
    @RedisKey("name")
    val roomName: String
)


@RedisClass("complicated_device")
data class ComplicatedDevice(
    @RedisId
    val id: String,
    @RedisKey("state")
    val state: String,
    @RedisKey("version")
    val version: String,
    @RedisKey("meta")
    val meta: Meta
) {

    data class Meta(
        @RedisKey("name")
        val name: String,
        @RedisKey("class")
        val clazz: String
    )

}

@RedisClass("school")
data class School(
    @RedisId
    val id: String,
    @RedisKey("director")
    val director: String,
    @RedisKey("room")
    val rooms: Map<String, SchoolRoom>
) {

    data class SchoolRoom(
        @RedisId
        val roomNumber: String,
        @RedisKey("name")
        val name: String,
        @RedisKey("teacher")
        val teacher: String
    )
}

@RedisClass("haengine")
data class HAEngine(
    @RedisKey("version")
    val version: Int? = null
)

@RedisClass("stb")
data class Stb(
    @RedisKey("upgrade")
    val upgrade: Upgrade
) {
    data class Upgrade(
        @RedisKey("bluetooth")
        val bluetooth: String? = null
    )
}