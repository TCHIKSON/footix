package com.footix.tv.core

import java.io.IOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

/** Erreur remontee par la couche reseau. */
class ApiException(message: String, cause: Throwable? = null) : IOException(message, cause)

/** Le match n'expose aucune source lisible. */
class StreamUnavailableException : IOException("Aucune source lisible")

/**
 * Categories d'erreur exposees a l'interface. La traduction en texte se fait
 * cote UI (voir ErrorMessages) pour garder les chaines dans les ressources.
 */
enum class AppError {
    NETWORK,
    TIMEOUT,
    SERVER,
    NO_SOURCE,
    UNKNOWN;

    companion object {
        /** Detail technique affiche sous le message, pour diagnostiquer sans adb. */
        fun detailOf(throwable: Throwable): String =
            listOfNotNull(throwable.javaClass.simpleName, throwable.message)
                .joinToString(": ")
                .take(160)

        fun from(throwable: Throwable): AppError = when (throwable) {
            is StreamUnavailableException -> NO_SOURCE
            is SocketTimeoutException -> TIMEOUT
            is UnknownHostException, is ConnectException -> NETWORK
            is ApiException -> SERVER
            is IOException -> NETWORK
            else -> UNKNOWN
        }
    }
}
