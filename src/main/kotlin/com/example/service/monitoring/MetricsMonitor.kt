package com.example.service.monitoring

import com.example.domain.model.SystemMetrics
import com.example.infrastructure.EventBus
import com.example.service.OrderService
import com.example.service.UserService
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import org.slf4j.LoggerFactory
import kotlin.time.Duration.Companion.seconds

/**
 * 실시간 메트릭스 모니터링
 *
 * 학습 포인트:
 * - Flow를 사용한 지속적인 데이터 스트림
 * - 주기적인 폴링을 Flow로 구현
 * - Flow 연산자 체이닝 (map, filter, onEach, catch)
 */
class MetricsMonitor(
    private val userService: UserService,
    private val orderService: OrderService,
    private val monitoringScope: CoroutineScope
) {
    private val logger = LoggerFactory.getLogger(MetricsMonitor::class.java)

    /**
     * 실시간 메트릭스 Flow 생성
     *
     * Flow 특징:
     * - Cold Stream: collect 호출 시 시작
     * - 각 구독자마다 독립적인 실행
     */
    fun monitorMetrics(intervalSeconds: Long = 5): Flow<SystemMetrics> = flow {
        logger.info("📊 Starting metrics monitoring (interval: ${intervalSeconds}s)")

        while (currentCoroutineContext().isActive) {
            try {
                // supervisorScope: 하나의 작업 실패해도 다른 작업 계속 실행
                val metrics = supervisorScope {
                    // 병렬로 데이터 수집
                    val usersDeferred = async {
                        try {
                            userService.getAllUsers().size
                        } catch (e: Exception) {
                            logger.warn("Failed to fetch users: ${e.message}")
                            0
                        }
                    }

                    val ordersDeferred = async {
                        try {
                            orderService.getAllOrders().size
                        } catch (e: Exception) {
                            logger.warn("Failed to fetch orders: ${e.message}")
                            0
                        }
                    }

                    val statsDeferred = async {
                        try {
                            orderService.getStatistics()
                        } catch (e: Exception) {
                            logger.warn("Failed to fetch statistics: ${e.message}")
                            null
                        }
                    }

                    SystemMetrics(
                        totalUsers = usersDeferred.await(),
                        totalOrders = ordersDeferred.await(),
                        activeConnections = 0, // TODO: 실제 구현
                        avgResponseTime = 0.0, // TODO: 실제 구현
                        errorRate = 0.0 // TODO: 실제 구현
                    )
                }

                emit(metrics)
                delay(intervalSeconds.seconds)

            } catch (e: CancellationException) {
                logger.info("📊 Metrics monitoring cancelled")
                throw e
            } catch (e: Exception) {
                logger.error("❌ Error in metrics monitoring", e)
                delay(intervalSeconds.seconds)
            }
        }
    }
        .onEach { metrics ->
            // EventBus에 상태 업데이트
            EventBus.updateMetrics(metrics)
        }
        .catch { e ->
            logger.error("❌ Fatal error in metrics flow", e)
            emit(SystemMetrics()) // 에러 발생 시 기본값 방출
        }

    /**
     * 백그라운드 모니터링 시작
     */
    fun startBackgroundMonitoring(intervalSeconds: Long = 10): Job {
        return monitoringScope.launch {
            monitorMetrics(intervalSeconds)
                .collect { metrics ->
                    logger.debug(
                        "📊 Metrics - Users: ${metrics.totalUsers}, " +
                        "Orders: ${metrics.totalOrders}"
                    )
                }
        }
    }

    /**
     * Flow 연산자 예제: 복잡한 변환 체인
     */
    fun monitorWithAnalysis(intervalSeconds: Long = 5): Flow<MetricsAnalysis> {
        return monitorMetrics(intervalSeconds)
            .map { metrics ->
                // 메트릭스 분석
                MetricsAnalysis(
                    metrics = metrics,
                    status = when {
                        metrics.totalOrders > 1000 -> HealthStatus.OVERLOADED
                        metrics.totalOrders > 500 -> HealthStatus.BUSY
                        else -> HealthStatus.HEALTHY
                    },
                    recommendations = generateRecommendations(metrics)
                )
            }
            .filter { analysis ->
                // 특정 조건만 통과
                analysis.status != HealthStatus.HEALTHY
            }
            .onEach { analysis ->
                // 부수 효과: 알림 발송
                if (analysis.status == HealthStatus.OVERLOADED) {
                    logger.warn("⚠️ System overloaded! ${analysis.recommendations}")
                }
            }
    }

    private fun generateRecommendations(metrics: SystemMetrics): List<String> {
        val recommendations = mutableListOf<String>()

        if (metrics.totalOrders > 1000) {
            recommendations.add("Consider scaling up order processing")
        }
        if (metrics.totalUsers > 10000) {
            recommendations.add("Consider implementing caching for user data")
        }

        return recommendations
    }
}

data class MetricsAnalysis(
    val metrics: SystemMetrics,
    val status: HealthStatus,
    val recommendations: List<String>
)

enum class HealthStatus {
    HEALTHY, BUSY, OVERLOADED
}
