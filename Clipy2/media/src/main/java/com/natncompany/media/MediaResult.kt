package com.natncompany.media

sealed interface MediaResult<out T> {
    data class Success<T>(val value: T) : MediaResult<T>
    data class Failure(val error: MediaError) : MediaResult<Nothing>
}

inline fun <T, R> MediaResult<T>.map(transform: (T) -> R): MediaResult<R> {
    return when (this) {
        is MediaResult.Success -> MediaResult.Success(transform(value))
        is MediaResult.Failure -> this
    }
}

inline fun <T> runMediaCatching(block: () -> T): MediaResult<T> {
    return try {
        MediaResult.Success(block())
    } catch (throwable: Throwable) {
        MediaResult.Failure(MediaError.ExceptionError(throwable))
    }
}

sealed interface MediaError {
    val message: String

    data class InvalidInput(override val message: String) : MediaError
    data class FileAccess(override val message: String) : MediaError
    data class UnsupportedFormat(override val message: String) : MediaError
    data class CorruptMedia(override val message: String) : MediaError
    data class Validation(override val message: String) : MediaError
    data class BackendUnavailable(override val message: String) : MediaError
    data class Cancelled(override val message: String = "Operation cancelled") : MediaError
    data class ExceptionError(val throwable: Throwable) : MediaError {
        override val message: String = throwable.message ?: throwable.javaClass.simpleName
    }
}
