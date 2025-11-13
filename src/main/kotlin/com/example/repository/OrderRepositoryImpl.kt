package com.example.repository

import com.example.domain.model.Order
import com.example.domain.model.OrderStatus
import kotlinx.coroutines.delay
import org.slf4j.LoggerFactory
import java.util.concurrent.atomic.AtomicLong

class OrderRepositoryImpl : OrderRepository {
    private val logger = LoggerFactory.getLogger(OrderRepositoryImpl::class.java)
    private val orders = mutableMapOf<Long, Order>()
    private val idCounter = AtomicLong(0)

    override suspend fun findAll(): List<Order> {
        logger.debug("[Repository] Finding all orders - simulating DB delay...")
        delay(200) // 데이터베이스 조회 시뮬레이션
        logger.debug("[Repository] Found ${orders.size} orders")
        return orders.values.toList()
    }

    override suspend fun findById(id: Long): Order? {
        logger.debug("[Repository] Finding order by id: $id - simulating DB delay...")
        delay(120) // 데이터베이스 조회 시뮬레이션
        val order = orders[id]
        logger.debug("[Repository] Order found: ${order != null}")
        return order
    }

    override suspend fun findByUserId(userId: Long): List<Order> {
        logger.debug("[Repository] Finding orders for user: $userId - simulating DB delay...")
        delay(180) // 데이터베이스 조회 시뮬레이션
        val userOrders = orders.values.filter { it.userId == userId }
        logger.debug("[Repository] Found ${userOrders.size} orders for user $userId")
        return userOrders
    }

    override suspend fun create(order: Order): Order {
        logger.debug("[Repository] Creating order: ${order.productName} - simulating DB delay...")
        delay(250) // 데이터베이스 삽입 시뮬레이션
        val newOrder = order.copy(id = idCounter.incrementAndGet())
        orders[newOrder.id] = newOrder
        logger.debug("[Repository] Order created with id: ${newOrder.id}")
        return newOrder
    }

    override suspend fun updateStatus(id: Long, status: OrderStatus): Order? {
        logger.debug("[Repository] Updating order status id: $id to $status - simulating DB delay...")
        delay(150) // 데이터베이스 업데이트 시뮬레이션
        val existingOrder = orders[id] ?: return null
        val updatedOrder = existingOrder.copy(status = status)
        orders[id] = updatedOrder
        logger.debug("[Repository] Order status updated: $id")
        return updatedOrder
    }
}
