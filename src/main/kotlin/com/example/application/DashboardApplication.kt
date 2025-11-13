package com.example.application

import com.example.domain.exception.ServiceException
import com.example.domain.model.DashboardData
import com.example.service.OrderService
import com.example.service.UserService
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import org.slf4j.LoggerFactory

/**
 * Dashboard Application: 대시보드 데이터를 위한 복합 조회
 * 
 * 코루틴 학습 포인트:
 * - 여러 독립적인 서비스를 병렬로 호출하여 성능 최적화
 * - coroutineScope를 사용한 구조화된 동시성
 * - 병렬 실행의 실제 성능 이점 확인
 */
class DashboardApplication(
    private val userService: UserService,
    private val orderService: OrderService
) {
    private val logger = LoggerFactory.getLogger(DashboardApplication::class.java)

    /**
     * 대시보드 데이터 조회 (병렬 처리의 핵심 예제)
     * 
     * 코루틴 패턴: 여러 독립적인 작업을 async로 병렬 실행
     * - 사용자 목록, 주문 목록, 통계를 동시에 조회
     * - 순차 실행 시: 150ms + 200ms + 200ms = 550ms
     * - 병렬 실행 시: max(150ms, 200ms, 200ms) = 200ms
     * - 약 2.75배의 성능 향상!
     */
    suspend fun getDashboardData(): DashboardData = coroutineScope {
        logger.info("[Dashboard] 🚀 Starting getDashboardData")
        logger.info("[Dashboard] 📊 Launching 3 parallel coroutines...")
        
        val startTime = System.currentTimeMillis()
        
        try {
            // 세 가지 독립적인 작업을 병렬로 시작
            val usersDeferred = async {
                logger.debug("[Dashboard] 👥 Fetching all users...")
                userService.getAllUsers()
            }
            
            val ordersDeferred = async {
                logger.debug("[Dashboard] 📦 Fetching all orders...")
                orderService.getAllOrders()
            }
            
            val statsDeferred = async {
                logger.debug("[Dashboard] 📈 Calculating statistics...")
                orderService.getStatistics()
            }
            
            logger.info("[Dashboard] ⏳ Waiting for all data...")
            
            // 모든 결과를 기다림
            val result = DashboardData(
                users = usersDeferred.await(),
                orders = ordersDeferred.await(),
                stats = statsDeferred.await()
            )
            
            val duration = System.currentTimeMillis() - startTime
            logger.info("[Dashboard] ✅ Completed in ${duration}ms")
            logger.info("[Dashboard] 💡 병렬 실행으로 성능 최적화! (순차 실행 대비 약 ${550.0 / duration}배 빠름)")
            
            result
        } catch (e: Exception) {
            logger.error("[Dashboard] ❌ Failed to get dashboard data", e)
            throw ServiceException("Failed to get dashboard data", e)
        }
    }

    /**
     * 통계 데이터만 조회 (단순 위임)
     */
    suspend fun getStatistics() = orderService.getStatistics()
}
