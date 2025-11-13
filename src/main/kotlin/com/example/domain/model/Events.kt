package com.example.domain.model

import java.time.Instant

/**
 * 시스템 이벤트 정의
 * - StateFlow/SharedFlow로 브로드캐스트되는 이벤트들
 */
sealed class SystemEvent {
    data class UserCreated(val userId: Long, val userName: String, val timestamp: Instant = Instant.now()) : SystemEvent()
    data class UserUpdated(val userId: Long, val userName: String, val timestamp: Instant = Instant.now()) : SystemEvent()
    data class UserDeleted(val userId: Long, val timestamp: Instant = Instant.now()) : SystemEvent()

    data class OrderCreated(val orderId: Long, val userId: Long, val amount: Double, val timestamp: Instant = Instant.now()) : SystemEvent()
    data class OrderStatusChanged(val orderId: Long, val oldStatus: OrderStatus, val newStatus: OrderStatus, val timestamp: Instant = Instant.now()) : SystemEvent()

    data class SystemError(val message: String, val exception: Throwable?, val timestamp: Instant = Instant.now()) : SystemEvent()
}

/**
 * 주문 처리 상태
 */
sealed class OrderProcessingState {
    object Idle : OrderProcessingState()
    data class Processing(val orderId: Long, val progress: Int) : OrderProcessingState()
    data class Completed(val orderId: Long, val success: Boolean, val message: String) : OrderProcessingState()
    data class Error(val orderId: Long?, val error: String) : OrderProcessingState()
}

/**
 * 시스템 메트릭스
 */
data class SystemMetrics(
    val totalUsers: Int = 0,
    val totalOrders: Int = 0,
    val activeConnections: Int = 0,
    val avgResponseTime: Double = 0.0,
    val errorRate: Double = 0.0,
    val timestamp: Instant = Instant.now()
)
