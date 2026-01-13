package io.beldex.bchat.wallet.utils.common

// To parse the JSON, install kotlin's serialization plugin and do:
//
// val json    = Json(JsonConfiguration.Stable)
// val welcome = json.parse(Welcome.serializer(), jsonString)

import kotlinx.serialization.Serializable


@Serializable
data class FiatCurrencyPrice (
    val aud: Double = 0.0,//currencyList.add("AUD")
    val bgn: Double = 0.0,//currencyList.add("BGN")
    val brl: Double = 0.0,//currencyList.add("BRL")
    val cad: Double = 0.0,//currencyList.add("CAD")
    val chf: Double = 0.0,//currencyList.add("CHF")
    val cny: Double = 0.0,//currencyList.add("CNY")
    val czk: Double = 0.0,//currencyList.add("CZK")
    val eur: Double = 0.0,//currencyList.add("EUR")
    val dkk: Double = 0.0,//currencyList.add("DKK")
    val gbp: Double = 0.0,//currencyList.add("GBP")
    val hkd: Double = 0.0,//currencyList.add("HKD")
    val hrk: Double = 0.0,//currencyList.add("HRK")
    val huf: Double = 0.0,//currencyList.add("HUF")
    val idr: Double = 0.0,//currencyList.add("IDR")
    val ils: Double = 0.0,//currencyList.add("ILS")
    val inr: Double = 0.0,//currencyList.add("INR")
    val isk: Double = 0.0,//currencyList.add("ISK")
    val jpy: Double = 0.0,//currencyList.add("JPY")
    val krw: Double = 0.0,//currencyList.add("KRW")
    val mxn: Double = 0.0,//currencyList.add("MXN")
    val myr: Double = 0.0,//currencyList.add("MYR")
    val nok: Double = 0.0,//currencyList.add("NOK")
    val nzd: Double = 0.0,//currencyList.add("NZD")
    val php: Double = 0.0,//currencyList.add("PHP")
    val pln: Double = 0.0,//currencyList.add("PLN")
    val ron: Double = 0.0,//currencyList.add("RON")
    val rub: Double = 0.0,//currencyList.add("RUB")
    val sek: Double = 0.0,//currencyList.add("SEK")
    val sgd: Double = 0.0,//currencyList.add("SGD")
    val thb: Double = 0.0,//currencyList.add("THB")
    val usd: Double = 0.0,//currencyList.add("USD")
    val vef: Double = 0.0,//currencyList.add("VEF")
    val zar: Double = 0.0//currencyList.add("ZAR")
)