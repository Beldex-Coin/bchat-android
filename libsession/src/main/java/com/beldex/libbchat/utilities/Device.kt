package com.beldex.libbchat.utilities
enum class Device(val value: String, val service: String = value) {
    ANDROID("android", "firebase"),
    HUAWEI("huawei");
}