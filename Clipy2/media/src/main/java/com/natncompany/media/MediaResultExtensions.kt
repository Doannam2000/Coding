package com.natncompany.media

import kotlinx.coroutines.CancellationException

inline fun <T> mediaResultOf(block: () -> T): MediaResult<T> {
    return try {
        MediaResult.Success(block())
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (throwable: Throwable) {
        MediaResult.Failure(throwable.toMediaError())
    }
}

suspend inline fun <T> mediaResultOfSuspend(crossinline block: suspend () -> T): MediaResult<T> {
    return try {
        MediaResult.Success(block())
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (throwable: Throwable) {
        MediaResult.Failure(throwable.toMediaError())
    }
}

fun Throwable.toMediaError(defaultMessage: String? = null): MediaError {
    return when (this) {
        is CancellationException -> MediaError.Cancelled(defaultMessage ?: message ?: "Operation cancelled")
        else -> MediaError.ExceptionError(this)
    }
}
