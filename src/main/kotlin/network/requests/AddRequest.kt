package network.requests

class AddRequest<T>(
        val new: T
) {

    fun create(): List<Pair<String, String>> {
        return emptyList()
    }

}