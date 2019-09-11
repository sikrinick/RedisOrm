package network

import parsing.ParsingContext

sealed class RedisReceivedChange {
    class AddObject(newObj: Any?)
    class ChangeObject(parsingContext: ParsingContext)
}