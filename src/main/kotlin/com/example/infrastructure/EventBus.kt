package com.example.infrastructure

import com.example.domain.model.SystemEvent
import com.example.domain.model.SystemMetrics
import com.example.domain.model.OrderProcessingState
import kotlinx.coroutines.flow.*
import org.slf4j.LoggerFactory

/**
 * 이벤트 버스 - StateFlow와 SharedFlow를 사용한 이벤트 브로드캐스팅
 *
 * 학습 포인트:
 * - StateFlow: 현재 상태를 유지하는 Hot Flow
 * - SharedFlow: 이벤트 브로드캐스트용 Hot Flow
 * - Flow의 backpressure 처리
 * - 여러 구독자에게 동시 이벤트 전송
 */
object EventBus {
    private val logger = LoggerFactory.getLogger(EventBus::class.java)

    /**
     * StateFlow: 시스템 메트릭스 상태
     * - 항상 최신 상태를 유지
     * - 새로운 구독자는 즉시 현재 상태를 받음
     */
    private val _systemMetrics = MutableStateFlow(SystemMetrics())
    val systemMetrics: StateFlow<SystemMetrics> = _systemMetrics.asStateFlow()

    /**
     * StateFlow: 주문 처리 상태
     */
    private val _orderProcessingState = MutableStateFlow<OrderProcessingState>(OrderProcessingState.Idle)
    val orderProcessingState: StateFlow<OrderProcessingState> = _orderProcessingState.asStateFlow()

    /**
     * SharedFlow: 시스템 이벤트 스트림
     * - replay: 0 (새로운 구독자는 이전 이벤트 받지 않음)
     * - extraBufferCapacity: 100 (버퍼링으로 느린 구독자 대응)
     */
    private val _systemEvents = MutableSharedFlow<SystemEvent>(
        replay = 0,
        extraBufferCapacity = 100,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val systemEvents: SharedFlow<SystemEvent> = _systemEvents.asSharedFlow()

    /**
     * 이벤트 발행
     */
    suspend fun emit(event: SystemEvent) {
        logger.debug("📢 Broadcasting event: ${event::class.simpleName}")
        _systemEvents.emit(event)
    }

    /**
     * 메트릭스 업데이트
     */
    fun updateMetrics(metrics: SystemMetrics) {
        _systemMetrics.value = metrics
        logger.debug("📊 Metrics updated: totalUsers=${metrics.totalUsers}, totalOrders=${metrics.totalOrders}")
    }

    /**
     * 주문 처리 상태 업데이트
     */
    fun updateOrderState(state: OrderProcessingState) {
        _orderProcessingState.value = state
        logger.debug("🔄 Order state: ${state::class.simpleName}")
    }

    /**
     * 현재 상태 조회
     */
    fun getCurrentMetrics(): SystemMetrics = _systemMetrics.value
    fun getCurrentOrderState(): OrderProcessingState = _orderProcessingState.value

    /**
     * 상태 초기화
     */
    fun reset() {
        _systemMetrics.value = SystemMetrics()
        _orderProcessingState.value = OrderProcessingState.Idle
        logger.info("🔄 EventBus reset")
    }
}
