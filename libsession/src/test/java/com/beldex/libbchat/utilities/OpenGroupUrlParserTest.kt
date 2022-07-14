package com.beldex.libbchat.utilities

import org.junit.Test
import org.junit.Assert.*

class OpenGroupUrlParserTest {

    @Test
    fun parseUrlTest() {
        val inputUrl = "http://social.beldex.io/crypto?public_key=0cfdbcc8bba5989a6787019c6635c08415c103174609360f9c3e4e764ef48073"

        val expectedHost = "http://social.beldex.io"
        val expectedRoom = "crypto"
        val expectedPublicKey = "0cfdbcc8bba5989a6787019c6635c08415c103174609360f9c3e4e764ef48073"

        val result = OpenGroupUrlParser.parseUrl(inputUrl)
        assertEquals(expectedHost, result.server)
        assertEquals(expectedRoom, result.room)
        assertEquals(expectedPublicKey, result.serverPublicKey)
    }

    @Test
    fun parseUrlNoHttpTest() {
        val inputUrl = "social.beldex.io/crypto?public_key=0cfdbcc8bba5989a6787019c6635c08415c103174609360f9c3e4e764ef48073"

        val expectedHost = "http://social.beldex.io"
        val expectedRoom = "crypto"
        val expectedPublicKey = "0cfdbcc8bba5989a6787019c6635c08415c103174609360f9c3e4e764ef48073"

        val result = OpenGroupUrlParser.parseUrl(inputUrl)
        assertEquals(expectedHost, result.server)
        assertEquals(expectedRoom, result.room)
        assertEquals(expectedPublicKey, result.serverPublicKey)
    }

    @Test
    fun parseUrlWithIpTest() {
        val inputUrl = "http://social.beldex.io/crypto?public_key=0cfdbcc8bba5989a6787019c6635c08415c103174609360f9c3e4e764ef48073"

        val expectedHost = "http://social.beldex.io"
        val expectedRoom = "crypto"
        val expectedPublicKey = "0cfdbcc8bba5989a6787019c6635c08415c103174609360f9c3e4e764ef48073"

        val result = OpenGroupUrlParser.parseUrl(inputUrl)
        assertEquals(expectedHost, result.server)
        assertEquals(expectedRoom, result.room)
        assertEquals(expectedPublicKey, result.serverPublicKey)
    }

    @Test
    fun parseUrlWithIpAndNoHttpTest() {
        val inputUrl = "http://social.beldex.io/crypto?public_key=0cfdbcc8bba5989a6787019c6635c08415c103174609360f9c3e4e764ef48073"

        val expectedHost = "http://social.beldex.io"
        val expectedRoom = "crypto"
        val expectedPublicKey = "0cfdbcc8bba5989a6787019c6635c08415c103174609360f9c3e4e764ef48073"

        val result = OpenGroupUrlParser.parseUrl(inputUrl)
        assertEquals(expectedHost, result.server)
        assertEquals(expectedRoom, result.room)
        assertEquals(expectedPublicKey, result.serverPublicKey)
    }

    @Test(expected = OpenGroupUrlParser.Error.MalformedURL::class)
    fun parseUrlMalformedUrlTest() {
        val inputUrl = "file:social.beldex.io/crypto?public_key=0cfdbcc8bba5989a6787019c6635c08415c103174609360f9c3e4e764ef48073"
        OpenGroupUrlParser.parseUrl(inputUrl)
    }

    @Test(expected = OpenGroupUrlParser.Error.NoRoom::class)
    fun parseUrlNoRoomSpecifiedTest() {
        val inputUrl = "http://social.beldex.io/crypto?public_key=0cfdbcc8bba5989a6787019c6635c08415c103174609360f9c3e4e764ef48073"
        OpenGroupUrlParser.parseUrl(inputUrl)
    }

    @Test(expected = OpenGroupUrlParser.Error.NoPublicKey::class)
    fun parseUrlNoPublicKeySpecifiedTest() {
        val inputUrl = "http://social.beldex.io/crypto"
        OpenGroupUrlParser.parseUrl(inputUrl)
    }

    @Test(expected = OpenGroupUrlParser.Error.InvalidPublicKey::class)
    fun parseUrlInvalidPublicKeyProviedTest() {
        val inputUrl = "http://social.beldex.io/crypto?public_key=0cfdbcc8bba5989a6787019c6635c08415c103174609360f9c3e4e764ef48073"
        OpenGroupUrlParser.parseUrl(inputUrl)
    }
}
