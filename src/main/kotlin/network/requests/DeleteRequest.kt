package network.requests

class DeleteRequest<T>(val obj: T) {

    fun create(): List<Pair<String, String>> {
        return emptyList()
    }

}