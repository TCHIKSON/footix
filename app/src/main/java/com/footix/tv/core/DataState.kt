package com.footix.tv.core

sealed interface DataState<out T> {
    data object Loading : DataState<Nothing>
    data class Success<T>(val data: T) : DataState<T>
    data class Failure(val error: AppError, val detail: String = "") : DataState<Nothing>
}
