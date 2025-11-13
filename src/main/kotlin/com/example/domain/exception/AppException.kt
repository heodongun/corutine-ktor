package com.example.domain.exception

sealed class AppException(message: String) : Exception(message)

class UserNotFoundException(userId: Long) : AppException("User not found: $userId")

class OrderNotFoundException(orderId: Long) : AppException("Order not found: $orderId")

class InvalidRequestException(message: String) : AppException(message)

class ServiceException(message: String, cause: Throwable? = null) : AppException(message) {
    init {
        if (cause != null) {
            initCause(cause)
        }
    }
}
