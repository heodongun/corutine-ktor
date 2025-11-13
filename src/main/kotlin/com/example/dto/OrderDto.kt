package com.example.dto

import kotlinx.serialization.Serializable

@Serializable
data class CreateOrderRequest(
    val userId: Long,
    val productName: String,
    val amount: Double
)

@Serializable
data class UpdateOrderStatusRequest(
    val status: String
)
