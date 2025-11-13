package com.example.repository

import com.example.domain.model.Notification
import kotlinx.coroutines.delay
import org.slf4j.LoggerFactory
import java.util.concurrent.atomic.AtomicLong

class NotificationRepositoryImpl : NotificationRepository {
    private val logger = LoggerFactory.getLogger(NotificationRepositoryImpl::class.java)
    private val notifications = mutableMapOf<Long, Notification>()
    private val idCounter = AtomicLong(0)

    override suspend fun findAll(): List<Notification> {
        logger.debug("[Repository] Finding all notifications - simulating DB delay...")
        delay(100) // 데이터베이스 조회 시뮬레이션
        logger.debug("[Repository] Found ${notifications.size} notifications")
        return notifications.values.toList()
    }

    override suspend fun findById(id: Long): Notification? {
        logger.debug("[Repository] Finding notification by id: $id - simulating DB delay...")
        delay(80) // 데이터베이스 조회 시뮬레이션
        val notification = notifications[id]
        logger.debug("[Repository] Notification found: ${notification != null}")
        return notification
    }

    override suspend fun findRecentByUserId(userId: Long, limit: Int): List<Notification> {
        logger.debug("[Repository] Finding recent notifications for user: $userId - simulating DB delay...")
        delay(130) // 데이터베이스 조회 시뮬레이션
        val userNotifications = notifications.values
            .filter { it.userId == userId }
            .sortedByDescending { it.sentAt }
            .take(limit)
        logger.debug("[Repository] Found ${userNotifications.size} recent notifications for user $userId")
        return userNotifications
    }

    override suspend fun create(notification: Notification): Notification {
        logger.debug("[Repository] Creating notification: ${notification.message} - simulating DB delay...")
        delay(150) // 데이터베이스 삽입 시뮬레이션
        val newNotification = notification.copy(id = idCounter.incrementAndGet())
        notifications[newNotification.id] = newNotification
        logger.debug("[Repository] Notification created with id: ${newNotification.id}")
        return newNotification
    }
}
