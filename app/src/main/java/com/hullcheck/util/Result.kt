package com.hullcheck.util

/** Lightweight sealed result wrapper for repository/IO operations that can fail. */
sealed interface OpResult<out T> {
    data class Success<T>(val data: T) : OpResult<T>
    data class Failure(val message: String, val cause: Throwable? = null) : OpResult<Nothing>

    val isSuccess: Boolean get() = this is Success
}

inline fun <T> runCatchingResult(block: () -> T): OpResult<T> =
    try {
        OpResult.Success(block())
    } catch (e: Exception) {
        OpResult.Failure(e.message ?: e.javaClass.simpleName, e)
    }

inline fun <T> OpResult<T>.onSuccess(block: (T) -> Unit): OpResult<T> {
    if (this is OpResult.Success) block(data)
    return this
}

inline fun <T> OpResult<T>.onFailure(block: (OpResult.Failure) -> Unit): OpResult<T> {
    if (this is OpResult.Failure) block(this)
    return this
}

fun <T> OpResult<T>.getOrNull(): T? = (this as? OpResult.Success)?.data
