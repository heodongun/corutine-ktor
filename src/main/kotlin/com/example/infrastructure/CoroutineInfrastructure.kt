package com.example.infrastructure

import kotlinx.coroutines.*
import org.slf4j.LoggerFactory
import kotlin.coroutines.CoroutineContext

/**
 * 고급 코루틴 인프라스트럭처
 *
 * 학습 포인트:
 * - 명시적 CoroutineScope 관리 (GlobalScope 사용 지양)
 * - CoroutineExceptionHandler를 통한 체계적 예외 처리
 * - 구조화된 동시성(Structured Concurrency) 적용
 * - 애플리케이션 생명주기와 코루틴 생명주기 연동
 */
object CoroutineInfrastructure {
    private val logger = LoggerFactory.getLogger(CoroutineInfrastructure::class.java)

    /**
     * 전역 예외 핸들러
     * - 처리되지 않은 코루틴 예외를 로깅
     * - 프로덕션 환경에서는 모니터링 시스템에 전송 가능
     */
    private val exceptionHandler = CoroutineExceptionHandler { context, exception ->
        val coroutineName = context[CoroutineName]?.name ?: "Unknown"
        logger.error("❌ Unhandled coroutine exception in [$coroutineName]", exception)
    }

    /**
     * IO 작업용 스코프
     * - 데이터베이스 쿼리, 파일 I/O, 네트워크 요청
     * - Dispatchers.IO: 64개 스레드 풀 (또는 CPU 코어 수 중 큰 값)
     * - SupervisorJob: 자식 코루틴의 실패가 다른 코루틴에 영향을 주지 않음
     */
    val ioScope: CoroutineScope = CoroutineScope(
        Dispatchers.IO +
        SupervisorJob() +
        exceptionHandler +
        CoroutineName("IO-Scope")
    )

    /**
     * CPU 집약적 작업용 스코프
     * - 데이터 처리, 정렬, 계산
     * - Dispatchers.Default: CPU 코어 수만큼의 스레드 풀
     */
    val computationScope: CoroutineScope = CoroutineScope(
        Dispatchers.Default +
        SupervisorJob() +
        exceptionHandler +
        CoroutineName("Computation-Scope")
    )

    /**
     * 백그라운드 작업용 스코프
     * - 비동기 이벤트 처리, 알림 발송
     * - 메인 비즈니스 로직과 독립적으로 실행
     */
    val backgroundScope: CoroutineScope = CoroutineScope(
        Dispatchers.Default +
        SupervisorJob() +
        exceptionHandler +
        CoroutineName("Background-Scope")
    )

    /**
     * 애플리케이션 종료 시 모든 코루틴 정리
     * - 우아한 종료(Graceful Shutdown) 보장
     * - 실행 중인 작업 완료 대기 또는 취소
     */
    fun shutdown() {
        logger.info("🛑 Shutting down coroutine infrastructure...")

        runBlocking {
            try {
                // 타임아웃을 두고 취소
                withTimeout(5000) {
                    ioScope.cancel()
                    computationScope.cancel()
                    backgroundScope.cancel()

                    logger.info("✅ All coroutine scopes cancelled")
                }
            } catch (e: TimeoutCancellationException) {
                logger.warn("⚠️ Coroutine shutdown timed out, forcing cancellation")
            }
        }
    }

    /**
     * 현재 실행 중인 코루틴 상태 정보
     * - 디버깅 및 모니터링 용도
     */
    fun getStatus(): CoroutineStatus {
        return CoroutineStatus(
            ioScopeActive = ioScope.isActive,
            computationScopeActive = computationScope.isActive,
            backgroundScopeActive = backgroundScope.isActive
        )
    }
}

/**
 * 코루틴 스코프 상태 정보
 */
data class CoroutineStatus(
    val ioScopeActive: Boolean,
    val computationScopeActive: Boolean,
    val backgroundScopeActive: Boolean
)
