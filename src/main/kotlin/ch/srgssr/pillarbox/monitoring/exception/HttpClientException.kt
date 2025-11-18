package ch.srgssr.pillarbox.monitoring.exception

/**
 * Exception thrown when an HTTP client request fails or returns an unexpected response.
 *
 * @param message A descriptive message explaining the reason for the exception.
 * @param cause The underlying exception that caused the failur, if available. This can be null.
 */
class HttpClientException(
  message: String,
  cause: Throwable? = null,
) : RuntimeException(message, cause)
