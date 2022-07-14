package com.beldex.libsignal.utilities

class Mnode(val address: String, val port: Int, val publicKeySet: KeySet?) {
    val ip: String get() = address.removePrefix("https://")

    public enum class Method(val rawValue: String) {
        GetSwarm("get_mnodes_for_pubkey"),
        GetMessages("retrieve"),
        SendMessage("store"),
        DeleteMessage("delete"),
        BeldexDaemonRPCCall("beldexd_request"),
        Info("info"),
        DeleteAll("delete_all")
    }

    data class KeySet(val ed25519Key: String, val x25519Key: String)

    override fun equals(other: Any?): Boolean {
        return if (other is Mnode) {
            address == other.address && port == other.port
        } else {
            false
        }
    }

    override fun hashCode(): Int {
        return address.hashCode() xor port.hashCode()
    }

    override fun toString(): String { return "$address:$port" }
}
