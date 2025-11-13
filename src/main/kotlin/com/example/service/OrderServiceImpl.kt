package com.example.service

import com.example.domain.exception.InvalidRequestException
import com.example.domain.exception.OrderNotFoundException
import com.example.domain.model.Order
import com.example.domain.model.OrderStatistics
import com.example.domain.model.OrderStatus
import com.example.repository.OrderRepository
import org.slf4j.LoggerFactory

class OrderServiceImpl(
    private val orderRepository: OrderRepository
) : OrderService {
    private val logger = LoggerFactory.getLogger(OrderServiceImpl::class.java)

    override suspend fun getAllOrders(): List<Order> {
        logger.debug("[Service] Getting all orders")
        return orderRepository.findAll()
    }

    override suspend fun getOrderById(id: Long): Order {
        logger.debug("[Service] Getting order by id: $id")
        return orderRepository.findById(id)
            ?: throw OrderNotFoundException(id)
    }

    override suspend fun getOrdersByUserId(userId: Long): List<Order> {
        logger.debug("[Service] Getting orders for user: $userId")
        return orderRepository.findByUserId(userId)
    }

    override suspend fun createOrder(userId: Long, productName: String, amount: Double): Order {
        logger.debug("[Service] Creating order for user: $userId")
        
        // 비즈니스 로직: 금액 검증
        if (amount <= 0) {
            throw InvalidRequestException("Order amount must be positive: $amount")
        }
        
        val order = Order(0, userId, productName, amount, OrderStatus.PENDING)
        return orderRepository.create(order)
    }

    override suspend fun updateOrderStatus(id: Long, status: OrderStatus): Order {
        logger.debug("[Service] Updating order status: $id to $status")
        return orderRepository.updateStatus(id, status)
            ?: throw OrderNotFoundException(id)
    }

    override suspend fun getStatistics(): OrderStatistics {
        logger.debug("[Service] Calculating order statistics")
        val orders = orderRepository.findAll()
        
        val totalOrders = orders.size
        val totalAmount = orders.sumOf { it.amount }
        val averageAmount = if (totalOrders > 0) totalAmount / totalOrders else 0.0
        
        return OrderStatistics(totalOrders, totalAmount, averageAmount)
    }
}
