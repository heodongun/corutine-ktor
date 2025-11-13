package com.example.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class Order(
    val id: Long,
    val userId: Long,
    val productName: String,
    val amount: Double,
    val status: OrderStatus,
    val createdAt: Long = System.currentTimeMillis()
)

@Serializable
enum class OrderStatus {
    PENDING, PROCESSING, COMPLETED, CANCELLED
}
