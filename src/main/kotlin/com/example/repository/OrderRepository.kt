package com.example.repository

import com.example.domain.model.Order
import com.example.domain.model.OrderStatus

interface OrderRepository {
    suspend fun findAll(): List<Order>
    suspend fun findById(id: Long): Order?
    suspend fun findByUserId(userId: Long): List<Order>
    suspend fun create(order: Order): Order
    suspend fun updateStatus(id: Long, status: OrderStatus): Order?
}
