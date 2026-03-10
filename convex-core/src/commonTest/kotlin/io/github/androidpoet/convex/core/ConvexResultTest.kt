package io.github.androidpoet.convex.core

import io.github.androidpoet.convex.core.result.ConvexError
import io.github.androidpoet.convex.core.result.ConvexResult
import io.github.androidpoet.convex.core.result.flatMap
import io.github.androidpoet.convex.core.result.getOrElse
import io.github.androidpoet.convex.core.result.map
import io.github.androidpoet.convex.core.result.onFailure
import io.github.androidpoet.convex.core.result.onSuccess
import io.github.androidpoet.convex.core.result.recover
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ConvexResultTest {

    @Test
    fun success_returnsValue() {
        val result: ConvexResult<String> = ConvexResult.Success("hello")

        assertTrue(result.isSuccess)
        assertFalse(result.isFailure)
        assertEquals("hello", result.getOrNull())
        assertEquals("hello", result.getOrThrow())
        assertNull(result.errorOrNull())
    }

    @Test
    fun failure_returnsError() {
        val error = ConvexError("something failed", null)
        val result: ConvexResult<String> = ConvexResult.Failure(error)

        assertFalse(result.isSuccess)
        assertTrue(result.isFailure)
        assertNull(result.getOrNull())
        assertEquals(error, result.errorOrNull())
    }

    @Test
    fun map_transformsSuccess() {
        val result = ConvexResult.Success(42).map { it * 2 }
        assertEquals(84, result.getOrNull())
    }

    @Test
    fun map_passesFailureThrough() {
        val error = ConvexError("fail", null)
        val result: ConvexResult<Int> = ConvexResult.Failure(error)
        val mapped = result.map { it * 2 }

        assertTrue(mapped.isFailure)
        assertEquals(error, mapped.errorOrNull())
    }

    @Test
    fun flatMap_chainsResults() {
        val result = ConvexResult.Success(10)
            .flatMap { ConvexResult.Success(it + 5) }
        assertEquals(15, result.getOrNull())
    }

    @Test
    fun flatMap_shortCircuitsOnFailure() {
        val error = ConvexError("fail", null)
        val result: ConvexResult<Int> = ConvexResult.Failure(error)
        val chained = result.flatMap { ConvexResult.Success(it + 5) }

        assertTrue(chained.isFailure)
    }

    @Test
    fun onSuccess_executesOnSuccess() {
        var captured = ""
        ConvexResult.Success("value").onSuccess { captured = it }
        assertEquals("value", captured)
    }

    @Test
    fun onFailure_executesOnFailure() {
        var captured: ConvexError? = null
        val error = ConvexError("fail", null)
        ConvexResult.Failure(error).onFailure { captured = it }
        assertEquals(error, captured)
    }

    @Test
    fun recover_convertsFailureToSuccess() {
        val error = ConvexError("fail", null)
        val result: ConvexResult<String> = ConvexResult.Failure(error)
        val recovered = result.recover { "default" }

        assertTrue(recovered.isSuccess)
        assertEquals("default", recovered.getOrNull())
    }

    @Test
    fun getOrElse_returnsDefaultOnFailure() {
        val error = ConvexError("fail", null)
        val result: ConvexResult<String> = ConvexResult.Failure(error)
        val value = result.getOrElse { "fallback" }

        assertEquals("fallback", value)
    }

    @Test
    fun catching_wrapsExceptionsAsFailure() {
        val result = ConvexResult.catching<String> {
            throw RuntimeException("boom")
        }

        assertTrue(result.isFailure)
        assertEquals("boom", result.errorOrNull()?.message)
    }
}
