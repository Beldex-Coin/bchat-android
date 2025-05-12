package com.beldex.libbchat.messaging.open_groups

import com.beldex.libsignal.utilities.JsonUtil
import com.beldex.libsignal.utilities.Log
import okhttp3.HttpUrl
import java.util.Locale

data class OpenGroupV2(
    val server: String,
    val room: String,
    val id: String,
    val name: String,
    var publicKey: String
) {

    constructor(server: String, room: String, name: String, publicKey: String) : this(
        server = server,
        room = room,
        id = "$server.$room",
        name = name,
        publicKey = publicKey,
    )

    companion object {

        fun fromJSON(jsonAsString: String): OpenGroupV2? {
            return try {
                val json = JsonUtil.fromJson(jsonAsString)
                if (!json.has("room")) return null
                val room = json.get("room").asText().toLowerCase(Locale.US)
                val server = json.get("server").asText().toLowerCase(Locale.US)
                val displayName = json.get("displayName").asText()
                val publicKey = json.get("publicKey").asText()
                OpenGroupV2(server, room, displayName, publicKey)
            } catch (e: Exception) {
                Log.w("Beldex", "Couldn't parse social group from JSON: $jsonAsString.", e);
                null
            }
        }

        fun getServer(urlAsString: String): HttpUrl? {
            val url = HttpUrl.parse(urlAsString) ?: return null
            val builder = HttpUrl.Builder().scheme(url.scheme()).host(url.host())
            if (url.port() != 80 || url.port() != 443) {
                // Non-standard port; add to server
                builder.port(url.port())
            }
            return builder.build()
        }

    }

    fun toJson(): Map<String,String> = mapOf(
        "room" to room,
        "server" to server,
        "displayName" to name,
        "publicKey" to publicKey,
    )

    val joinURL: String get() = "$server/$room?public_key=$publicKey"
    val groupId: String get() = "$server.$room"
}