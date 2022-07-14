package com.thoughtcrimes.securesms.model

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.launch

abstract class AsyncTaskCoroutine<I, O> {
    var result: O? = null
    //private var result: O
    open fun onPreExecute() {}

    open fun onPostExecute(result: O?) {}
    abstract fun doInBackground(vararg params: I): O

    fun <T> execute(vararg input: I) {
        GlobalScope.launch(Dispatchers.Main) {
            onPreExecute()
            callAsync(*input)
        }
    }

    private suspend fun callAsync(vararg input: I) {
        GlobalScope.async(Dispatchers.IO) {
            result = doInBackground(*input)
        }.await()
        GlobalScope.launch(Dispatchers.Main) {

            onPostExecute(result)


        }
    }
}