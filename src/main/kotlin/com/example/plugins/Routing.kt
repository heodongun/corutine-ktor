package com.example.plugins

import com.example.application.DashboardApplication
import com.example.application.UserApplication
import com.example.controller.dashboardRoutes
import com.example.controller.orderRoutes
import com.example.controller.userRoutes
import com.example.repository.*
import com.example.service.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.slf4j.LoggerFactory

fun Application.configureRouting() {
    val logger = LoggerFactory.getLogger("Routing")
    
    // Repository 인스턴스 생성
    val userRepository: UserRepository = UserRepositoryImpl()
    val orderRepository: OrderRepository = OrderRepositoryImpl()
    val notificationRepository: NotificationRepository = NotificationRepositoryImpl()
    
    // 샘플 데이터 초기화
    DataInitializer.initialize(userRepository, orderRepository, notificationRepository)
    
    // Service 인스턴스 생성
    val userService: UserService = UserServiceImpl(userRepository)
    val orderService: OrderService = OrderServiceImpl(orderRepository)
    val notificationService: NotificationService = NotificationServiceImpl(notificationRepository)
    
    // Application 인스턴스 생성
    val userApplication = UserApplication(userService, orderService, notificationService)
    val dashboardApplication = DashboardApplication(userService, orderService)
    
    logger.info("✅ All layers initialized successfully!")
    logger.info("📚 Repository → Service → Application → Controller")
    logger.info("🚀 Server ready to handle requests with coroutines!")
    
    routing {
        get("/") {
            call.respondText("""
                🎓 Ktor Coroutine Learning Project
                
                Available endpoints:
                - GET  /api/users
                - POST /api/users
                - GET  /api/users/{id}
                - GET  /api/users/{id}/details (병렬 처리 예제)
                - PUT  /api/users/{id}
                - DELETE /api/users/{id}
                
                - GET  /api/orders
                - POST /api/orders
                - GET  /api/orders/{id}
                - GET  /api/orders/user/{userId}
                - PUT  /api/orders/{id}/status
                
                - GET  /api/dashboard (병렬 처리 예제)
                - GET  /api/dashboard/stats
                
                💡 로그를 확인하여 코루틴의 실행 흐름을 학습하세요!
            """.trimIndent())
        }
        
        route("/api") {
            userRoutes(userApplication)
            orderRoutes(orderService)
            dashboardRoutes(dashboardApplication)
        }
    }
}
