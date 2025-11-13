package com.example.service

import com.example.domain.exception.OrderNotFoundException
import com.example.domain.model.*
import com.example.infrastructure.CoroutineInfrastructure
import com.example.infrastructure.EventBus
import com.example.repository.OrderRepository
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import org.slf4j.LoggerFactory
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

/**
 * 고급 주문 서비스 - Channel, Select, 복잡한 Flow 패턴
 *
 * 학습 포인트:
 * 1. Channel을 사용한 producer-consumer 패턴
 * 2. select를 사용한 다중 채널 처리
 * 3. Flow 고급 연산자 (combine, zip, merge)
 * 4. 상태 머신 패턴
 * 5. Batch processing with Flow
 */
class AdvancedOrderService(
    private val orderRepository: OrderRepository
) {
    private val logger = LoggerFactory.getLogger(AdvancedOrderService::class.java)

    // 주문 처리 채널
    private val orderProcessingChannel = Channel<Order>(Channel.BUFFERED)

    /**
     * 주문 처리 워커 시작
     *
     * 패턴:
     * - Channel: Producer-Consumer 패턴
     * - 백그라운드에서 지속적으로 주문 처리
     */
    fun startOrderProcessor(): Job {
        return CoroutineInfrastructure.backgroundScope.launch {
            logger.info("[Processor] Starting order processor")

            try {
                for (order in orderProcessingChannel) {
                    processOrder(order)
                }
            } catch (e: Exception) {
                logger.error("[Processor] Order processor error", e)
            } finally {
                logger.info("[Processor] Order processor stopped")
            }
        }
    }

    /**
     * 주문 제출 (Channel에 전송)
     */
    suspend fun submitOrder(order: Order) {
        logger.info("[Submit] Submitting order: ${order.id}")
        orderProcessingChannel.send(order)
        EventBus.updateOrderState(OrderProcessingState.Processing(order.id, 0))
    }

    /**
     * 주문 처리 로직
     */
    private suspend fun processOrder(order: Order) = supervisorScope {
        logger.info("[Process] Processing order: ${order.id}")

        try {
            // 상태 업데이트: 처리 중
            EventBus.updateOrderState(OrderProcessingState.Processing(order.id, 30))
            delay(100.milliseconds) // 시뮬레이션

            // 1. 재고 확인 (시뮬레이션)
            val inventoryAvailable = async(Dispatchers.IO) {
                delay(200.milliseconds)
                true
            }

            EventBus.updateOrderState(OrderProcessingState.Processing(order.id, 60))

            // 2. 결제 처리 (시뮬레이션)
            val paymentProcessed = async(Dispatchers.IO) {
                delay(300.milliseconds)
                true
            }

            // 두 작업이 모두 완료될 때까지 대기
            if (inventoryAvailable.await() && paymentProcessed.await()) {
                EventBus.updateOrderState(OrderProcessingState.Processing(order.id, 90))

                // 주문 상태 업데이트
                withContext(Dispatchers.IO) {
                    orderRepository.updateStatus(order.id, OrderStatus.COMPLETED)
                }

                // 완료 이벤트
                EventBus.updateOrderState(OrderProcessingState.Completed(order.id, true, "Order completed successfully"))
                EventBus.emit(
                    SystemEvent.OrderStatusChanged(
                        order.id,
                        order.status,
                        OrderStatus.COMPLETED
                    )
                )

                logger.info("[Process] ✅ Order ${order.id} completed")
            } else {
                throw Exception("Inventory or payment failed")
            }

        } catch (e: Exception) {
            logger.error("[Process] ❌ Order ${order.id} failed", e)
            EventBus.updateOrderState(OrderProcessingState.Error(order.id, e.message ?: "Unknown error"))

            withContext(Dispatchers.IO) {
                orderRepository.updateStatus(order.id, OrderStatus.FAILED)
            }
        }
    }

    /**
     * 주문 스트리밍 with Flow operators
     *
     * 패턴:
     * - Flow 고급 연산자 활용
     * - 데이터 변환 파이프라인
     */
    fun streamOrdersWithAnalysis(): Flow<OrderAnalysis> {
        return flow {
            val orders = withContext(Dispatchers.IO) {
                orderRepository.findAll()
            }
            orders.forEach { emit(it) }
        }
            .filter { it.status == OrderStatus.COMPLETED }
            .map { order ->
                OrderAnalysis(
                    order = order,
                    profitMargin = calculateProfit(order),
                    riskScore = calculateRisk(order)
                )
            }
            .filter { it.riskScore < 0.5 } // 저위험 주문만
            .onEach { analysis ->
                logger.debug("[Analysis] Order ${analysis.order.id}: profit=${analysis.profitMargin}, risk=${analysis.riskScore}")
            }
    }

    /**
     * 여러 Flow 결합 - combine operator
     *
     * 패턴:
     * - combine: 여러 Flow의 최신 값을 결합
     */
    fun monitorOrderMetrics(): Flow<OrderMetrics> {
        val totalOrdersFlow = flow {
            while (currentCoroutineContext().isActive) {
                val count = withContext(Dispatchers.IO) {
                    orderRepository.findAll().size
                }
                emit(count)
                delay(2.seconds)
            }
        }

        val processingStateFlow = EventBus.orderProcessingState

        return combine(totalOrdersFlow, processingStateFlow) { total, state ->
            OrderMetrics(
                totalOrders = total,
                processingState = state,
                timestamp = System.currentTimeMillis()
            )
        }
    }

    /**
     * Batch 주문 생성 with Flow
     *
     * 패턴:
     * - Flow.chunked: 배치 단위로 묶어서 처리
     * - flatMapMerge: 병렬 처리
     */
    fun createOrdersBatch(orders: List<Order>): Flow<Result<Order>> {
        return orders.asFlow()
            .map { order ->
                flow {
                    try {
                        val created = withContext(Dispatchers.IO) {
                            orderRepository.create(order)
                        }
                        emit(Result.success(created))

                        // 이벤트 발행
                        EventBus.emit(
                            SystemEvent.OrderCreated(
                                created.id,
                                created.userId,
                                created.amount
                            )
                        )
                    } catch (e: Exception) {
                        logger.error("[Batch Create] Failed to create order", e)
                        emit(Result.failure<Order>(e))
                    }
                }
            }
            .flattenMerge(concurrency = 5) // 동시에 5개까지 처리
    }

    /**
     * 통계 계산 with 병렬 처리
     */
    suspend fun calculateStatisticsAdvanced(): OrderStatistics = coroutineScope {
        val orders = async(Dispatchers.IO) {
            orderRepository.findAll()
        }.await()

        // CPU 집약적 계산은 Default Dispatcher
        withContext(Dispatchers.Default) {
            val totalOrders = orders.size
            val totalAmount = orders.sumOf { it.amount }
            val averageAmount = if (totalOrders > 0) totalAmount / totalOrders else 0.0

            OrderStatistics(totalOrders, totalAmount, averageAmount)
        }
    }

    /**
     * 사용자별 주문 Flow
     */
    fun getUserOrdersFlow(userId: Long): Flow<Order> = flow {
        val orders = withContext(Dispatchers.IO) {
            orderRepository.findByUserId(userId)
        }
        orders.forEach { emit(it) }
    }
        .onStart {
            logger.info("[Stream] Starting order stream for user: $userId")
        }
        .onCompletion {
            logger.info("[Stream] Completed order stream for user: $userId")
        }

    /**
     * SharedFlow로 실시간 주문 업데이트 브로드캐스트
     */
    private val _realtimeOrders = MutableSharedFlow<Order>(replay = 10, extraBufferCapacity = 100)
    val realtimeOrders: SharedFlow<Order> = _realtimeOrders.asSharedFlow()

    suspend fun broadcastOrder(order: Order) {
        _realtimeOrders.emit(order)
    }

    // Helper functions
    private fun calculateProfit(order: Order): Double = order.amount * 0.2
    private fun calculateRisk(order: Order): Double = if (order.amount > 10000) 0.7 else 0.3

    fun shutdown() {
        orderProcessingChannel.close()
        logger.info("[Shutdown] Order service channels closed")
    }
}

data class OrderAnalysis(
    val order: Order,
    val profitMargin: Double,
    val riskScore: Double
)

data class OrderMetrics(
    val totalOrders: Int,
    val processingState: OrderProcessingState,
    val timestamp: Long
)
