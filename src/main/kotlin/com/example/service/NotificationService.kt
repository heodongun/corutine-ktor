package com.example.service

import com.example.domain.model.Notification

interface NotificationService {
    suspend fun getAllNotifications(): List<Notification>
    suspend fun getNotificationById(id: Long): Notification
    suspend fun getRecentByUserId(userId: Long, limit: Int = 5): List<Notification>
    suspend fun sendWelcomeEmail(userId: Long, email: String): Notification
    suspend fun sendOrderConfirmation(userId: Long, orderId: Long): Notification
}
