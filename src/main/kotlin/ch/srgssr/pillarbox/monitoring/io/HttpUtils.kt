package ch.srgssr.pillarbox.monitoring.io

import ch.srgssr.pillarbox.monitoring.exception.HttpClientException
import io.ktor.client.call.body
import io.ktor.client.statement.HttpResponse
import io.ktor.http.HttpStatusCode
import io.ktor.http.isSuccess

/**
 * Checks whether the HTTP status code represents a client error (4xx).
 *
 * @return `true` if the status code is in the range 400..499, `false` otherwise.
 */
fun HttpStatusCode.is4xxClientError(): Boolean = value in (400..499)

/**
 * Executes the given [block] if the HTTP response status is successful (2xx).
 *
 * @param block The lambda to execute on successful response.
 * @return The original [HttpResponse], allowing chaining of further operations.
 */
inline fun HttpResponse.onSuccess(block: HttpResponse.() -> Unit) = apply { if (status.isSuccess()) block() }

/**
 * Executes the given [block] if the HTTP response status matches the provided [predicate].
 *
 * @param predicate A lambda that returns `true` for the statuses you want to handle.
 * @param block The lambda to execute if [predicate] returns `true`.
 * @return The original [HttpResponse], allowing chaining of further operations.
 */
inline fun HttpResponse.onStatus(
  predicate: (HttpStatusCode) -> Boolean,
  block: HttpResponse.() -> Unit,
) = apply { if (predicate(status)) block() }

/**
 * Executes the given [block] if the HTTP response status is not successful (non-2xx).
 *
 * @param block The lambda to execute on non-successful response.
 * @return The original [HttpResponse], allowing chaining of further operations.
 */
inline fun HttpResponse.onNotSuccess(block: HttpResponse.() -> Unit) = apply { if (!status.isSuccess()) block() }

/**
 * Throws an [HttpClientException] if the HTTP response is not successful.
 *
 * The exception message will include the result of [lazyMessage], the HTTP status code,
 * and the response body (if available).
 *
 * @param lazyMessage A lambda providing a custom error message.
 * @throws HttpClientException if the response status is not successful.
 */
suspend inline fun HttpResponse.throwOnNotSuccess(lazyMessage: () -> String) =
  onNotSuccess {
    val errorBody = runCatching { body<String>() }.getOrDefault("")
    val fullMessage = "${lazyMessage()}: HTTP $status - $errorBody"

    throw HttpClientException(fullMessage)
  }
