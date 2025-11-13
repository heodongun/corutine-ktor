package com.example.application

import com.example.domain.model.SystemEvent
import com.example.infrastructure.CoroutineInfrastructure
import com.example.infrastructure.EventBus
import com.example.service.AdvancedOrderService
import com.example.service.AdvancedUserService
import com.example.service.monitoring.MetricsMonitor
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import org.slf4j.LoggerFactory
import kotlin.time.Duration.Companion.seconds

/**
 * 고급 대시보드 - 모든 코루틴 패턴을 종합적으로 활용
 *
 * 학습 포인트:
 * 1. 여러 Flow를 combine/zip으로 결합
 * 2. StateFlow 구독 및 상태 변화 감지
 * 3. SharedFlow로 이벤트 수신
 * 4. supervisorScope로 독립적 작업 관리
 * 5. 구조화된 동시성의 실전 적용
 */
class AdvancedDashboard(
    private val advancedUserService: AdvancedUserService,
    private val advancedOrderService: AdvancedOrderService,
    private val metricsMonitor: MetricsMonitor
) {
    private val logger = LoggerFactory.getLogger(AdvancedDashboard::class.java)

    /**
     * 대시보드 데이터 스트림
     *
     * 패턴:
     * - combine: 여러 Flow를 결합하여 하나의 통합 데이터 스트림 생성
     * - StateFlow 구독으로 실시간 상태 업데이트
     */
    fun getDashboardStream(): Flow<DashboardSnapshot> {
        return combine(
            metricsMonitor.monitorMetrics(5),
            EventBus.orderProcessingState,
            flowOf(emptyList<SystemEvent>()) // 최근 이벤트는 별도 처리
        ) { metrics, orderState, _ ->
            DashboardSnapshot(
                systemMetrics = metrics,
                orderProcessingState = orderState,
                timestamp = System.currentTimeMillis()
            )
        }
            .onStart {
                logger.info("📊 Dashboard stream started")
            }
            .onCompletion {
                logger.info("📊 Dashboard stream completed")
            }
            .catch { e ->
                logger.error("❌ Dashboard stream error", e)
                emit(DashboardSnapshot.empty())
            }
    }

    /**
     * 이벤트 리스너 시작
     *
     * 패턴:
     * - SharedFlow 구독으로 이벤트 수신
     * - 이벤트 타입별 처리 분기
     */
    fun startEventListener(): Job {
        return CoroutineInfrastructure.backgroundScope.launch {
            EventBus.systemEvents
                .collect { event ->
                    when (event) {
                        is SystemEvent.UserCreated -> {
                            logger.info("🎉 User created: ${event.userName} (ID: ${event.userId})")
                            handleUserCreated(event)
                        }
                        is SystemEvent.OrderCreated -> {
                            logger.info("📦 Order created: ${event.orderId} by user ${event.userId}")
                            handleOrderCreated(event)
                        }
                        is SystemEvent.OrderStatusChanged -> {
                            logger.info("🔄 Order ${event.orderId} status: ${event.oldStatus} → ${event.newStatus}")
                        }
                        is SystemEvent.SystemError -> {
                            logger.error("❌ System error: ${event.message}", event.exception)
                        }
                        else -> {
                            logger.debug("📢 Event: ${event::class.simpleName}")
                        }
                    }
                }
        }
    }

    /**
     * 복잡한 대시보드 데이터 조회
     *
     * 패턴:
     * - supervisorScope: 각 작업이 독립적으로 실패/성공
     * - async/await: 병렬 실행으로 성능 최적화
     * - withTimeout: 각 작업에 타임아웃 적용
     */
    suspend fun getComplexDashboardData(): ComplexDashboardData = supervisorScope {
        logger.info("🚀 Fetching complex dashboard data")

        val startTime = System.currentTimeMillis()

        // 모든 작업을 병렬로 시작
        val usersDeferred = async(Dispatchers.IO + CoroutineName("FetchUsers")) {
            try {
                withTimeout(5.seconds) {
                    advancedUserService.streamAllUsers()
                        .take(100)
                        .toList()
                }
            } catch (e: Exception) {
                logger.warn("⚠️ Failed to fetch users: ${e.message}")
                emptyList()
            }
        }

        val ordersDeferred = async(Dispatchers.IO + CoroutineName("FetchOrders")) {
            try {
                withTimeout(5.seconds) {
                    advancedOrderService.streamOrdersWithAnalysis()
                        .take(100)
                        .toList()
                }
            } catch (e: Exception) {
                logger.warn("⚠️ Failed to fetch orders: ${e.message}")
                emptyList()
            }
        }

        val statsDeferred = async(Dispatchers.Default + CoroutineName("CalculateStats")) {
            try {
                withTimeout(3.seconds) {
                    advancedOrderService.calculateStatisticsAdvanced()
                }
            } catch (e: Exception) {
                logger.warn("⚠️ Failed to calculate stats: ${e.message}")
                null
            }
        }

        val metricsDeferred = async(CoroutineName("FetchMetrics")) {
            try {
                EventBus.getCurrentMetrics()
            } catch (e: Exception) {
                logger.warn("⚠️ Failed to fetch metrics: ${e.message}")
                null
            }
        }

        val healthCheckDeferred = async(Dispatchers.IO + CoroutineName("HealthCheck")) {
            try {
                performHealthCheck()
            } catch (e: Exception) {
                logger.warn("⚠️ Health check failed: ${e.message}")
                HealthCheckResult(healthy = false, message = e.message)
            }
        }

        // 모든 결과 수집
        val result = ComplexDashboardData(
            users = usersDeferred.await(),
            orderAnalyses = ordersDeferred.await(),
            statistics = statsDeferred.await(),
            metrics = metricsDeferred.await(),
            healthCheck = healthCheckDeferred.await(),
            fetchDuration = System.currentTimeMillis() - startTime
        )

        logger.info("✅ Complex dashboard data fetched in ${result.fetchDuration}ms")
        logger.info("📊 Results: ${result.users.size} users, ${result.orderAnalyses.size} orders")

        result
    }

    /**
     * 실시간 통계 모니터링
     *
     * 패턴:
     * - Flow.onEach: 각 값에 대한 부수 효과
     * - Flow.conflate: 느린 구독자를 위한 최신 값만 유지
     */
    fun monitorRealtimeStatistics(): Flow<RealtimeStats> {
        return flow {
            while (currentCoroutineContext().isActive) {
                val stats = calculateRealtimeStats()
                emit(stats)
                delay(2.seconds)
            }
        }
            .conflate() // 느린 구독자는 중간 값 건너뛰기
            .onEach { stats ->
                logger.debug("📈 Realtime stats: avgResponseTime=${stats.avgResponseTime}ms")
            }
    }

    /**
     * 배치 작업 처리
     *
     * 패턴:
     * - Flow.buffer: 생산자-소비자 속도 차이 해결
     * - Flow.chunked: 배치 단위로 묶어서 처리
     */
    suspend fun processBatchOperations() = coroutineScope {
        logger.info("🔄 Starting batch operations")

        launch(Dispatchers.Default + CoroutineName("BatchProcessor")) {
            advancedUserService.streamAllUsers(batchSize = 50)
                .buffer(capacity = 100)
                .collect { user ->
                    // 각 사용자에 대한 처리
                    logger.debug("Processing user: ${user.id}")
                }
        }
    }

    // Helper Methods

    private suspend fun handleUserCreated(event: SystemEvent.UserCreated) {
        // 사용자 생성 후 추가 작업
        logger.debug("Handling user created: ${event.userId}")
    }

    private suspend fun handleOrderCreated(event: SystemEvent.OrderCreated) {
        // 주문 생성 후 추가 작업
        logger.debug("Handling order created: ${event.orderId}")
    }

    private suspend fun performHealthCheck(): HealthCheckResult {
        delay(100) // 시뮬레이션
        return HealthCheckResult(
            healthy = true,
            message = "All systems operational"
        )
    }

    private suspend fun calculateRealtimeStats(): RealtimeStats {
        return RealtimeStats(
            activeUsers = 0, // TODO: 실제 구현
            activeOrders = 0, // TODO: 실제 구현
            avgResponseTime = 0.0, // TODO: 실제 구현
            timestamp = System.currentTimeMillis()
        )
    }

    /**
     * 정리 작업
     */
    fun shutdown() {
        logger.info("🛑 Shutting down advanced dashboard")
    }
}

// Data Models

data class DashboardSnapshot(
    val systemMetrics: com.example.domain.model.SystemMetrics,
    val orderProcessingState: com.example.domain.model.OrderProcessingState,
    val timestamp: Long
) {
    companion object {
        fun empty() = DashboardSnapshot(
            systemMetrics = com.example.domain.model.SystemMetrics(),
            orderProcessingState = com.example.domain.model.OrderProcessingState.Idle,
            timestamp = System.currentTimeMillis()
        )
    }
}

data class ComplexDashboardData(
    val users: List<com.example.domain.model.User>,
    val orderAnalyses: List<com.example.service.OrderAnalysis>,
    val statistics: com.example.domain.model.OrderStatistics?,
    val metrics: com.example.domain.model.SystemMetrics?,
    val healthCheck: HealthCheckResult,
    val fetchDuration: Long
)

data class HealthCheckResult(
    val healthy: Boolean,
    val message: String?
)

data class RealtimeStats(
    val activeUsers: Int,
    val activeOrders: Int,
    val avgResponseTime: Double,
    val timestamp: Long
)
