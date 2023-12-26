package com.beldex.libsignal.utilities

data class ForkInfo(val hf: Int, val sf: Int) {
    companion object {
        const val DEFAULT_HF = 17
        const val DEFAULT_SF = 0
        val DEFAULT = ForkInfo(DEFAULT_HF, DEFAULT_SF)
        val baseTable = arrayOf(10,100,1000,10000,100000)
    }

    operator fun compareTo(other: ForkInfo): Int {
        val base = baseTable.first { it > sf && it > other.sf }
        return (hf*base - other.hf*base) + (sf - other.sf)
    }

}

// add info here for when various features are active
fun ForkInfo.hasNamespaces() = hf >= 18
fun ForkInfo.defaultRequiresAuth() = hf >= 18 && sf >= 0