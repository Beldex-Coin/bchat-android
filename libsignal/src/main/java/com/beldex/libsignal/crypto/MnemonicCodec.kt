package com.beldex.libsignal.crypto

import android.util.Log
import java.util.zip.CRC32

class MnemonicCodec(private val loadFileContents: (String) -> String) {

    class Language(private val loadFileContents: (String) -> String, private val configuration: Configuration) {

        data class Configuration(val filename: String, val prefixLength: Int) {

            companion object {
                val english = Configuration("english", 3)
                val japanese = Configuration("japanese", 3)
                val portuguese = Configuration("portuguese", 4)
                val spanish = Configuration("spanish", 4)
            }
        }

        companion object {
            internal val wordSetCache = mutableMapOf<Language, List<String>>()
            internal val truncatedWordSetCache = mutableMapOf<Language, List<String>>()
        }

        internal fun loadWordSet(): List<String> {
            val cachedResult = wordSetCache[this]
            if (cachedResult != null) {
                return cachedResult
            } else {
                val contents = loadFileContents(configuration.filename)
                val result = contents.split(",")
                wordSetCache[this] = result
                return result
            }
        }

        internal fun loadTruncatedWordSet(): List<String> {
            val cachedResult = wordSetCache[this]
            if (cachedResult != null) {
                return cachedResult
            } else {
                val prefixLength = configuration.prefixLength
                val result = loadWordSet().map { it.substring(0 until prefixLength) }
                truncatedWordSetCache[this] = result
                return result
            }
        }
    }

            sealed class DecodingError(val description: String) : Exception(description) {
        object Generic : DecodingError("Something went wrong. Please check your mnemonic and try again.")
        object InputTooShort : DecodingError("Looks like you didn't enter enough words. Please check your mnemonic and try again.")
        object MissingLastWord : DecodingError("You seem to be missing the last word of your mnemonic. Please check what you entered and try again.")
        object InvalidWord : DecodingError("There appears to be an invalid word in your mnemonic. Please check what you entered and try again.")
        object VerificationFailed : DecodingError("Your mnemonic couldn't be verified. Please check what you entered and try again.")
    }

    fun encode(hexEncodedString: String, languageConfiguration: Language.Configuration = Language.Configuration.english): String {
        var string = hexEncodedString
        val language = Language(loadFileContents, languageConfiguration)
        val wordSet = language.loadWordSet()
        Log.d("1-> characterCount ",wordSet.toString())
        val prefixLength = languageConfiguration.prefixLength
        val result = mutableListOf<String>()
        val n = wordSet.size.toLong()
        Log.d("1-> characterCount n Value",n.toString())
        val characterCount = string.length
        Log.d("1-> characterCount before for loop ",characterCount.toString())
        for (chunkStartIndex in 0..(characterCount - 8) step 8) {
            val chunkEndIndex = chunkStartIndex + 8
            val p1 = string.substring(0 until chunkStartIndex)
            val p2 = swap(string.substring(chunkStartIndex until chunkEndIndex))
            val p3 = string.substring(chunkEndIndex until characterCount)
            Log.d("1-> characterCount p1",p1)
            Log.d("1-> characterCount p2",p2)
            Log.d("1-> characterCount p3",p3)
            string = p1 + p2 + p3
        }
        Log.d("1-> characterCount after for loop",characterCount.toString())
        for (chunkStartIndex in 0..(characterCount - 8) step 8) {
            val chunkEndIndex = chunkStartIndex + 8
            val x = string.substring(chunkStartIndex until chunkEndIndex).toLong(16)
            val w1 = x % n
            val w2 = ((x / n) + w1) % n
            val w3 = (((x / n) / n) + w2) % n
            Log.d("1-> characterCount w1",w1.toString())
            Log.d("1-> characterCount w2",w2.toString())
            Log.d("1-> characterCount w3",w3.toString())
            result += listOf( wordSet[w1.toInt()], wordSet[w2.toInt()], wordSet[w3.toInt()] )
            Log.d("1-> characterCount wordSet[w1.toInt()]",wordSet[w1.toInt()].toString())
            Log.d("1-> characterCount wordSet[w2.toInt()]",wordSet[w2.toInt()].toString())
            Log.d("1-> characterCount wordSet[w3.toInt()]",wordSet[w3.toInt()].toString())
        }
        val checksumIndex = determineChecksumIndex(result, prefixLength)
        val checksumWord = result[checksumIndex]
        result.add(checksumWord)
        Log.d("1-> characterCount result",result.toString())
        return result.joinToString(" ")
    }

    fun decode(mnemonic: String, languageConfiguration: Language.Configuration = Language.Configuration.english): String {
        val words = mnemonic.split(" ").toMutableList()
        val language = Language(loadFileContents, languageConfiguration)
        val truncatedWordSet = language.loadTruncatedWordSet()
        val prefixLength = languageConfiguration.prefixLength
        var result = ""
        val n = truncatedWordSet.size.toLong()
        // Check preconditions
        if (words.size < 24) { throw DecodingError.InputTooShort
        }
        if (words.size % 3 == 0) { throw DecodingError.MissingLastWord
        }
        // Get checksum word
        val checksumWord = words.removeAt(words.lastIndex)
        // Decode
        for (chunkStartIndex in 0..(words.size - 3) step 3) {
            try {
                val w1 = truncatedWordSet.indexOf(words[chunkStartIndex].substring(0 until prefixLength))
                val w2 = truncatedWordSet.indexOf(words[chunkStartIndex + 1].substring(0 until prefixLength))
                val w3 = truncatedWordSet.indexOf(words[chunkStartIndex + 2].substring(0 until prefixLength))
                val x = w1 + n * ((n - w1 + w2) % n) + n * n * ((n - w2 + w3) % n)
                if (x % n != w1.toLong()) { throw DecodingError.Generic
                }
                val string = "0000000" + x.toString(16)
                result += swap(string.substring(string.length - 8 until string.length))
            } catch (e: Exception) {
                throw DecodingError.InvalidWord
            }
        }
        // Verify checksum
        val checksumIndex = determineChecksumIndex(words, prefixLength)
        val expectedChecksumWord = words[checksumIndex]
        if (expectedChecksumWord.substring(0 until prefixLength) != checksumWord.substring(0 until prefixLength)) { throw DecodingError.VerificationFailed
        }
        // Return
        return result
    }

    private fun swap(x: String): String {
        val p1 = x.substring(6 until 8)
        val p2 = x.substring(4 until 6)
        val p3 = x.substring(2 until 4)
        val p4 = x.substring(0 until 2)
        return p1 + p2 + p3 + p4
    }

    private fun determineChecksumIndex(x: List<String>, prefixLength: Int): Int {
        val bytes = x.joinToString("") { it.substring(0 until prefixLength) }.toByteArray()
        val crc32 = CRC32()
        crc32.update(bytes)
        val checksum = crc32.value
        return (checksum % x.size.toLong()).toInt()
    }
}
