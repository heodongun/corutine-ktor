package com.example.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class UserDetails(
    val user: User,
    val orders: List<Order>,
    val recentNotifications: List<Notification>
)

@Serializable
data class UserCreationResult(
    val user: User,
    val welcomeNotification: Notification
)

@Serializable
data class DashboardData(
    val users: List<User>,
    val orders: List<Order>,
    val stats: OrderStatistics
)

@Serializable
data class OrderStatistics(
    val totalOrders: Int,
    val totalAmount: Double,
    val averageAmount: Double
)
