package com.example.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class Notification(
    val id: Long,
    val userId: Long,
    val message: String,
    val type: NotificationType,
    val sentAt: Long = System.currentTimeMillis()
)

@Serializable
enum class NotificationType {
    EMAIL, SMS, PUSH
}
