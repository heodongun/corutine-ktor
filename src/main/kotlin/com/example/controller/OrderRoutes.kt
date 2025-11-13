package com.example.controller

import com.example.domain.model.OrderStatus
import com.example.dto.CreateOrderRequest
import com.example.dto.UpdateOrderStatusRequest
import com.example.service.OrderService
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.slf4j.LoggerFactory

/**
 * Order Routes: 주문 관련 HTTP 엔드포인트
 */
fun Route.orderRoutes(orderService: OrderService) {
    val logger = LoggerFactory.getLogger("OrderRoutes")

    route("/orders") {
        // GET /api/orders - 모든 주문 조회
        get {
            logger.info("[Controller] GET /api/orders")
            val orders = orderService.getAllOrders()
            call.respond(orders)
        }

        // POST /api/orders - 주문 생성
        post {
            logger.info("[Controller] POST /api/orders")
            val request = call.receive<CreateOrderRequest>()
            val order = orderService.createOrder(request.userId, request.productName, request.amount)
            call.respond(HttpStatusCode.Created, order)
        }

        // GET /api/orders/{id} - 특정 주문 조회
        get("/{id}") {
            val id = call.parameters["id"]?.toLongOrNull()
                ?: return@get call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Invalid order ID"))
            
            logger.info("[Controller] GET /api/orders/$id")
            val order = orderService.getOrderById(id)
            call.respond(order)
        }

        // GET /api/orders/user/{userId} - 특정 사용자의 주문 조회
        get("/user/{userId}") {
            val userId = call.parameters["userId"]?.toLongOrNull()
                ?: return@get call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Invalid user ID"))
            
            logger.info("[Controller] GET /api/orders/user/$userId")
            val orders = orderService.getOrdersByUserId(userId)
            call.respond(orders)
        }

        // PUT /api/orders/{id}/status - 주문 상태 변경
        put("/{id}/status") {
            val id = call.parameters["id"]?.toLongOrNull()
                ?: return@put call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Invalid order ID"))
            
            logger.info("[Controller] PUT /api/orders/$id/status")
            val request = call.receive<UpdateOrderStatusRequest>()
            
            val status = try {
                OrderStatus.valueOf(request.status.uppercase())
            } catch (e: IllegalArgumentException) {
                return@put call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Invalid status: ${request.status}"))
            }
            
            val order = orderService.updateOrderStatus(id, status)
            call.respond(order)
        }
    }
}
