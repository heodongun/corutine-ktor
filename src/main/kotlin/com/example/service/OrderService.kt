package com.example.service

import com.example.domain.model.Order
import com.example.domain.model.OrderStatistics
import com.example.domain.model.OrderStatus

interface OrderService {
    suspend fun getAllOrders(): List<Order>
    suspend fun getOrderById(id: Long): Order
    suspend fun getOrdersByUserId(userId: Long): List<Order>
    suspend fun createOrder(userId: Long, productName: String, amount: Double): Order
    suspend fun updateOrderStatus(id: Long, status: OrderStatus): Order
    suspend fun getStatistics(): OrderStatistics
}
