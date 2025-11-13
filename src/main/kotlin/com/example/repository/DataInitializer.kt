package com.example.repository

import com.example.domain.model.*
import kotlinx.coroutines.runBlocking
import org.slf4j.LoggerFactory

object DataInitializer {
    private val logger = LoggerFactory.getLogger(DataInitializer::class.java)

    fun initialize(
        userRepository: UserRepository,
        orderRepository: OrderRepository,
        notificationRepository: NotificationRepository
    ) = runBlocking {
        logger.info("Initializing sample data...")

        // Create users
        val alice = userRepository.create(User(0, "Alice", "alice@example.com"))
        val bob = userRepository.create(User(0, "Bob", "bob@example.com"))
        val charlie = userRepository.create(User(0, "Charlie", "charlie@example.com"))

        logger.info("Created ${listOf(alice, bob, charlie).size} users")

        // Create orders
        orderRepository.create(Order(0, alice.id, "Laptop", 1200.0, OrderStatus.COMPLETED))
        orderRepository.create(Order(0, alice.id, "Mouse", 25.0, OrderStatus.COMPLETED))
        orderRepository.create(Order(0, bob.id, "Keyboard", 80.0, OrderStatus.PENDING))
        orderRepository.create(Order(0, bob.id, "Monitor", 350.0, OrderStatus.PROCESSING))
        orderRepository.create(Order(0, charlie.id, "Headphones", 120.0, OrderStatus.COMPLETED))

        logger.info("Created 5 orders")

        // Create notifications
        notificationRepository.create(Notification(0, alice.id, "Welcome to our platform!", NotificationType.EMAIL))
        notificationRepository.create(Notification(0, alice.id, "Your order has been shipped", NotificationType.EMAIL))
        notificationRepository.create(Notification(0, bob.id, "Welcome to our platform!", NotificationType.EMAIL))
        notificationRepository.create(Notification(0, bob.id, "Order confirmed", NotificationType.EMAIL))
        notificationRepository.create(Notification(0, charlie.id, "Welcome to our platform!", NotificationType.EMAIL))

        logger.info("Created 5 notifications")
        logger.info("Sample data initialization completed!")
    }
}
