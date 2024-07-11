package com.thoughtcrimes.securesms.data

import io.beldex.bchat.BuildConfig
import java.io.File
import android.content.Context
import android.util.Log
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.thoughtcrimes.securesms.util.nodelistasync.NodeListConstants
import com.thoughtcrimes.securesms.util.nodelistasync.NodeListJson
import java.lang.reflect.Type

object NetworkNodes  {
    private val mainNetNodes = listOf(
        "publicnode1.rpcnode.stream:29095",
        "publicnode2.rpcnode.stream:29095",
        "publicnode3.rpcnode.stream:29095",
        "publicnode4.rpcnode.stream:29095",
        "publicnode5.rpcnode.stream:29095"
    )
    private val testNetModes = listOf(
        "149.102.156.174:19095"
    )

    fun getNodes(mContext: Context): List<String> {
        if(BuildConfig.USE_TESTNET){
            return testNetModes
        }else{
            try {
                val jsonFile = File(mContext.filesDir,"/${NodeListConstants.downloadNodeListFileName}")
                if(!jsonFile.exists()){
                    return mainNetNodes
                }
                val jsonString = jsonFile.readText()
                val gson = Gson()
                val collectionType: Type = object : TypeToken<List<NodeListJson?>?>() {}.type
                val nodeList: List<NodeListJson> = gson.fromJson(jsonString, collectionType)
                if(nodeList.isNotEmpty()){
                    val nodeUriList: ArrayList<String> = ArrayList()
                    nodeUriList.clear()
                    for(item in nodeList){
                        item.uri?.let { nodeUriList.add(it) }
                    }
                    Log.d("Download Node List Json ->",nodeUriList.toString())
                    return nodeUriList
                }else{
                    Log.d("Download Node List Json ->","empty")
                    return mainNetNodes
                }
            }catch (e : Exception){
                Log.d("Download Node List Json exception ->",e.message.toString())
            }
            return mainNetNodes
        }
    }
}