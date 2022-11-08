package com.thoughtcrimes.securesms.wallet.utils.common

import android.net.Uri
import android.util.Log
import com.beldex.libsignal.utilities.getProperty
import com.fasterxml.jackson.databind.ser.std.StdKeySerializers
import com.google.gson.GsonBuilder
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.json.*
import okhttp3.*
import java.io.IOException

fun fetchPriceFor(fiat: String, callback: Callback): Call {
    val fiatApiAuthority = "api.beldex.io"
    val client = OkHttpClient()
    val apiPath = "/price/$fiat"
    val request = Request.Builder()
        .url("https://$fiatApiAuthority$apiPath")
        .build()

    val call = client.newCall(request)
    call.enqueue(callback)
    return call
}

/*
fun fetchPriceFor(fiat:String):Double {
    var price =0.0
    val fiatApiAuthority = "api.beldex.io"

    try{
        val client = OkHttpClient()
        val apiPath ="/price/$fiat"
        val request = Request.Builder()
            .url("https://$fiatApiAuthority$apiPath")
            .build()

        */
/* val response = client.newCall(request).execute()
         Log.d("Beldex","Fiat ${response.isSuccessful}")
         if(response.isSuccessful){
             val body = response.body?.string()
             println(body)
             val gson = GsonBuilder().create()
             return gson.fromJson(body, FiatCurrencyPrice::class.java).usd

             Log.d("Beldex","Fiat -- ${price}")
         }else{
             return 0.0
         }*//*


        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                price =0.0
            }
            override fun onResponse(call: Call, response: Response){
                Log.d("Beldex","Fiat ${response.isSuccessful}")
                if(response.isSuccessful) {
                    val body = response.body?.string()
                    println(body)
                    val gson = GsonBuilder().create()
                    var fiat = gson.fromJson(body, FiatCurrencyPrice::class.java)
                    price = fiat.usd

                    Log.d("Beldex","Fiat -- ${price}")
                }else{
                    price = 0.0
                }
            }
        })
        return price
    }catch (e:Exception) {
        return price
    }
}*/
