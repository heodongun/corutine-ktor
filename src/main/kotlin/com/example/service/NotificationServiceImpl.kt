package com.example.service

import com.example.domain.exception.InvalidRequestException
import com.example.domain.model.Notification
import com.example.domain.model.NotificationType
import com.example.repository.NotificationRepository
import org.slf4j.LoggerFactory

class NotificationServiceImpl(
    private val notificationRepository: NotificationRepository
) : NotificationService {
    private val logger = LoggerFactory.getLogger(NotificationServiceImpl::class.java)

    override suspend fun getAllNotifications(): List<Notification> {
        logger.debug("[Service] Getting all notifications")
        return notificationRepository.findAll()
    }

    override suspend fun getNotificationById(id: Long): Notification {
        logger.debug("[Service] Getting notification by id: $id")
        return notificationRepository.findById(id)
            ?: throw InvalidRequestException("Notification not found: $id")
    }

    override suspend fun getRecentByUserId(userId: Long, limit: Int): List<Notification> {
        logger.debug("[Service] Getting recent notifications for user: $userId")
        return notificationRepository.findRecentByUserId(userId, limit)
    }

    override suspend fun sendWelcomeEmail(userId: Long, email: String): Notification {
        logger.debug("[Service] Sending welcome email to user: $userId")
        val notification = Notification(
            id = 0,
            userId = userId,
            message = "Welcome to our platform! We're glad to have you, $email",
            type = NotificationType.EMAIL
        )
        return notificationRepository.create(notification)
    }

    override suspend fun sendOrderConfirmation(userId: Long, orderId: Long): Notification {
        logger.debug("[Service] Sending order confirmation to user: $userId for order: $orderId")
        val notification = Notification(
            id = 0,
            userId = userId,
            message = "Your order #$orderId has been confirmed and is being processed",
            type = NotificationType.EMAIL
        )
        return notificationRepository.create(notification)
    }
}
