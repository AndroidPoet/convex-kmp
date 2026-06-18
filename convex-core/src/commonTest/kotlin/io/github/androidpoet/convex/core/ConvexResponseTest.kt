package io.github.androidpoet.convex.core

import io.github.androidpoet.convex.core.models.ConvexResponse
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ConvexResponseTest {
    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun deserialize_successResponse() {
        val body = """{"status":"success","value":"hello","logLines":["log1"]}"""
        val response = json.decodeFromString<ConvexResponse>(body)

        assertTrue(response.isSuccess)
        assertEquals(JsonPrimitive("hello"), response.value)
        assertEquals(listOf("log1"), response.logLines)
        assertNull(response.errorMessage)
    }

    @Test
    fun deserialize_errorResponse() {
        val body = """{"status":"error","errorMessage":"not found","logLines":[]}"""
        val response = json.decodeFromString<ConvexResponse>(body)

        assertTrue(response.isError)
        assertEquals("not found", response.errorMessage)
        assertNull(response.value)
    }

    @Test
    fun deserialize_errorWithData() {
        val body = """{"status":"error","errorMessage":"validation","errorData":{"field":"name"},"logLines":[]}"""
        val response = json.decodeFromString<ConvexResponse>(body)

        assertTrue(response.isError)
        assertEquals("validation", response.errorMessage)
        assertTrue(response.errorData != null)
    }
}
