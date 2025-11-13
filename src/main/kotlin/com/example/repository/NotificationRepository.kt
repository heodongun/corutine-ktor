package com.example.repository

import com.example.domain.model.Notification

interface NotificationRepository {
    suspend fun findAll(): List<Notification>
    suspend fun findById(id: Long): Notification?
    suspend fun findRecentByUserId(userId: Long, limit: Int = 5): List<Notification>
    suspend fun create(notification: Notification): Notification
}
