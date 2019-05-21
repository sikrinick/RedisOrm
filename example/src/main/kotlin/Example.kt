
import com.tylerthrailkill.helpers.prettyprint.pp
import parsing.RedisClassParser
import parsing.RedisParser
import kotlin.reflect.KClass
import kotlin.reflect.full.instanceParameter
import kotlin.reflect.full.memberFunctions

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

fun main() {
    val classes = RedisClassParser.create(
        Device::class,
        Scene::class,
        Room::class,
        ComplicatedDevice::class,
        School::class
    )

    val redisParser = RedisParser(classes)
    val db = getAllExample(redisParser)
    subscribeExample(redisParser, db)
}

fun getAllExample(redisParser: RedisParser): RedisDatabase {

    val requests = redisParser.prepareGetAllRequest()
    val response = sendRequest(requests)

    return redisParser.parse(response)
        .apply {
            print()
        }
}

//todo make class
typealias RedisDatabase = Map<KClass<*>, MutableMap<String?, Any>>

fun sendRequest(getRequest: List<String>) = mapOf(
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
    "school:2:room:2:name" to "polish"
)

fun subscribeExample(redisParser: RedisParser, db: RedisDatabase) {
    val redisKey = "device:0-123:name"
    val redisValue = "Big fucking change!"

    //todo add parse method
    val parsingContext = redisParser.parse(redisKey, redisValue)

    parsingContext?.clazz?.let { _ ->
        db[parsingContext.clazz]
            ?.get(parsingContext.id?.second)
            ?.let { obj ->
                val copyConstructor = obj::class.memberFunctions.first { it.name == "copy" }
                copyConstructor.callBy(
                    mapOf(
                        copyConstructor.instanceParameter!! to obj,
                        //todo fixme to parsingcontext!
                        copyConstructor.parameters.first { it.name == parsingContext.param.name!! } to parsingContext.result
                    )
                )
            }
            ?.let {
                db[parsingContext.clazz]?.put(parsingContext.id?.second, it) to it
            }
            ?.let {
                println("\n\nChanged object:\n")
                pp(it.first)
                println("\nto object:\n")
                pp(it.second)
                println("\n\n")
                db.print()
            }
    }
}

class IndentedPrinter(indent: Int = 0): Appendable {
    private val indent = if (indent > 0) " ".repeat(indent) else ""

    override fun append(csq: CharSequence?): Appendable {
        return System.out.append(indent).append(csq)
    }

    override fun append(csq: CharSequence?, start: Int, end: Int): Appendable {
        return System.out.append(indent).append(csq, start ,end)
    }

    override fun append(c: Char): Appendable {
        return System.out.append(indent).append(c)
    }

}

fun RedisDatabase.print() = forEach { clazz, table ->
    println("Table ${clazz.simpleName} : (")
    table.forEach { (_, obj) -> pp(obj, writeTo = IndentedPrinter(2)) }
    println(")")
    println()
}