package com.example.application

import com.example.domain.exception.ServiceException
import com.example.domain.model.User
import com.example.domain.model.UserCreationResult
import com.example.domain.model.UserDetails
import com.example.service.NotificationService
import com.example.service.OrderService
import com.example.service.UserService
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import org.slf4j.LoggerFactory

/**
 * Application 레이어: 여러 서비스를 조합하여 복합적인 비즈니스 로직을 처리
 * 
 * 코루틴 학습 포인트:
 * 1. coroutineScope를 사용한 구조화된 동시성
 * 2. async/await를 사용한 병렬 실행
 * 3. 순차 실행과 병렬 실행의 차이
 */
class UserApplication(
    private val userService: UserService,
    private val orderService: OrderService,
    private val notificationService: NotificationService
) {
    private val logger = LoggerFactory.getLogger(UserApplication::class.java)

    /**
     * 사용자 상세 정보 조회 (병렬 처리 예제)
     * 
     * 코루틴 패턴: async/await를 사용한 병렬 실행
     * - 사용자 정보, 주문 목록, 알림 목록을 동시에 조회
     * - 각 작업이 독립적이므로 병렬로 실행하여 성능 향상
     */
    suspend fun getUserWithDetails(userId: Long): UserDetails = coroutineScope {
        logger.info("[Application] 🚀 Starting getUserWithDetails for userId: $userId")
        logger.info("[Application] 📊 Launching parallel coroutines for user, orders, and notifications")
        
        try {
            // async를 사용하여 세 가지 작업을 병렬로 시작
            val userDeferred = async {
                logger.debug("[Application] 👤 Fetching user data...")
                userService.getUserById(userId)
            }
            
            val ordersDeferred = async {
                logger.debug("[Application] 📦 Fetching orders data...")
                orderService.getOrdersByUserId(userId)
            }
            
            val notificationsDeferred = async {
                logger.debug("[Application] 📧 Fetching notifications data...")
                notificationService.getRecentByUserId(userId)
            }
            
            logger.info("[Application] ⏳ Waiting for all parallel operations to complete...")
            
            // await()를 호출하여 모든 결과를 기다림
            val result = UserDetails(
                user = userDeferred.await(),
                orders = ordersDeferred.await(),
                recentNotifications = notificationsDeferred.await()
            )
            
            logger.info("[Application] ✅ Completed getUserWithDetails successfully")
            result
        } catch (e: Exception) {
            logger.error("[Application] ❌ Failed to get user details", e)
            throw ServiceException("Failed to get user details for userId: $userId", e)
        }
    }

    /**
     * 사용자 생성 및 환영 알림 발송 (순차 처리 예제)
     * 
     * 코루틴 패턴: suspend 함수의 순차 실행
     * - 사용자를 먼저 생성한 후, 그 결과를 사용하여 알림 발송
     * - 두 작업이 의존 관계에 있으므로 순차적으로 실행
     */
    suspend fun createUserWithWelcome(name: String, email: String): UserCreationResult {
        logger.info("[Application] 🚀 Starting createUserWithWelcome for: $name")
        logger.info("[Application] 📝 Step 1: Creating user...")
        
        try {
            // 1단계: 사용자 생성 (먼저 완료되어야 함)
            val user = userService.createUser(name, email)
            logger.info("[Application] ✅ User created with id: ${user.id}")
            
            // 2단계: 환영 알림 발송 (사용자 ID가 필요하므로 순차 실행)
            logger.info("[Application] 📧 Step 2: Sending welcome notification...")
            val notification = notificationService.sendWelcomeEmail(user.id, email)
            logger.info("[Application] ✅ Welcome notification sent")
            
            val result = UserCreationResult(user, notification)
            logger.info("[Application] ✅ Completed createUserWithWelcome successfully")
            return result
        } catch (e: Exception) {
            logger.error("[Application] ❌ Failed to create user with welcome", e)
            throw ServiceException("Failed to create user: $name", e)
        }
    }

    /**
     * 모든 사용자 조회 (단순 위임)
     */
    suspend fun getAllUsers(): List<User> {
        logger.debug("[Application] Getting all users")
        return userService.getAllUsers()
    }

    /**
     * 사용자 조회 (단순 위임)
     */
    suspend fun getUserById(userId: Long): User {
        logger.debug("[Application] Getting user by id: $userId")
        return userService.getUserById(userId)
    }

    /**
     * 사용자 수정 (단순 위임)
     */
    suspend fun updateUser(id: Long, name: String, email: String): User {
        logger.debug("[Application] Updating user: $id")
        return userService.updateUser(id, name, email)
    }

    /**
     * 사용자 삭제 (단순 위임)
     */
    suspend fun deleteUser(id: Long): Boolean {
        logger.debug("[Application] Deleting user: $id")
        return userService.deleteUser(id)
    }
}
