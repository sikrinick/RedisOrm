package webdis

import com.google.gson.annotations.SerializedName

class EvalResponse(
    @SerializedName("EVAL")
    val result: List<String>
)

class PSubscribeResponse(
    @SerializedName("PSUBSCRIBE")
    val result: List<String>
)

class GetResponse(
    @SerializedName("GET")
    val result: String
)