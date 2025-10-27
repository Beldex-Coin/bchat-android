package io.beldex.bchat.util

import io.beldex.bchat.conversation.v2.contact_sharing.capitalizeFirstLetter

fun shortNameAndAddress(name:String, address: String): String {
    return if(name == address) {
        if(address.length >= 7) {
            "${address.take(4).capitalizeFirstLetter()}....${address.takeLast(3)}"
        } else {
            address.capitalizeFirstLetter()
        }
    } else {
        name.capitalizeFirstLetter()
    }
}