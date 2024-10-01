package ch.srgssr.pillarbox.qos.exception

/**
 * Exception thrown when a retry mechanism has exhausted its allowed number of attempts.
 *
 * The actual exception used by Spring Boot's reactive WebClient for retry exhaustion is private,
 * making it unavailable for type checking. This custom exception is created to provide clarity and
 * allow proper handling of retry exhaustion scenarios.
 *
 * @param message A descriptive message explaining the reason for the exception.
 * @param cause The underlying exception that caused the failur, if available. This can be null.
 */
class RetryExhaustedException(
    message: String,
    cause: Throwable?,
) : RuntimeException(message, cause)
