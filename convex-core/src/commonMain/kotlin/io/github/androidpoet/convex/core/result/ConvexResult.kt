package io.github.androidpoet.convex.core.result

import kotlinx.coroutines.CancellationException

public sealed interface ConvexResult<out T> {

    public data class Success<T>(val value: T) : ConvexResult<T>

    public data class Failure(val error: ConvexError) : ConvexResult<Nothing>

    public fun getOrNull(): T? = when (this) {
        is Success -> value
        is Failure -> null
    }

    public fun getOrThrow(): T = when (this) {
        is Success -> value
        is Failure -> throw error.toException()
    }

    public fun errorOrNull(): ConvexError? = when (this) {
        is Success -> null
        is Failure -> error
    }

    public val isSuccess: Boolean get() = this is Success
    public val isFailure: Boolean get() = this is Failure

    public companion object {
        public inline fun <T> catching(block: () -> T): ConvexResult<T> =
            try {
                Success(block())
            } catch (e: ConvexException) {
                Failure(e.error)
            } catch (e: Throwable) {
                if (e is CancellationException) throw e
                Failure(
                    ConvexError(
                        message = e.message ?: "Unknown error",
                        data = null,
                    ),
                )
            }
    }
}

public inline fun <T, R> ConvexResult<T>.map(transform: (T) -> R): ConvexResult<R> =
    when (this) {
        is ConvexResult.Success -> ConvexResult.Success(transform(value))
        is ConvexResult.Failure -> this
    }

public inline fun <T, R> ConvexResult<T>.flatMap(
    transform: (T) -> ConvexResult<R>,
): ConvexResult<R> =
    when (this) {
        is ConvexResult.Success -> transform(value)
        is ConvexResult.Failure -> this
    }

public inline fun <T> ConvexResult<T>.onSuccess(action: (T) -> Unit): ConvexResult<T> {
    if (this is ConvexResult.Success) action(value)
    return this
}

public inline fun <T> ConvexResult<T>.onFailure(action: (ConvexError) -> Unit): ConvexResult<T> {
    if (this is ConvexResult.Failure) action(error)
    return this
}

public inline fun <T> ConvexResult<T>.recover(
    transform: (ConvexError) -> T,
): ConvexResult<T> =
    when (this) {
        is ConvexResult.Success -> this
        is ConvexResult.Failure -> ConvexResult.Success(transform(error))
    }

public inline fun <T> ConvexResult<T>.getOrElse(defaultValue: (ConvexError) -> T): T =
    when (this) {
        is ConvexResult.Success -> value
        is ConvexResult.Failure -> defaultValue(error)
    }

public fun <T> ConvexResult<T>.toKotlinResult(): Result<T> = when (this) {
    is ConvexResult.Success -> Result.success(value)
    is ConvexResult.Failure -> Result.failure(error.toException())
}

public inline fun <T> Result<T>.toConvexResult(
    mapThrowable: (Throwable) -> ConvexError = { throwable ->
        val convexException = throwable as? ConvexException
        convexException?.error ?: ConvexError(
            message = throwable.message ?: "Unknown error",
            data = null,
        )
    },
): ConvexResult<T> = fold(
    onSuccess = { ConvexResult.Success(it) },
    onFailure = { throwable ->
        if (throwable is CancellationException) throw throwable
        ConvexResult.Failure(mapThrowable(throwable))
    },
)
