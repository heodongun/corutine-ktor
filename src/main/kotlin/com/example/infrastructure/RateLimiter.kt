package com.example.infrastructure

import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withLock
import org.slf4j.LoggerFactory
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * Rate Limiter - 요청 속도 제한
 *
 * 학습 포인트:
 * - Semaphore를 사용한 동시 실행 제어
 * - Mutex를 사용한 상태 동기화
 * - 시간 기반 슬라이딩 윈도우 알고리즘
 */
class RateLimiter(
    private val maxRequests: Int,
    private val timeWindow: Duration = 1.seconds
) {
    private val logger = LoggerFactory.getLogger(RateLimiter::class.java)
    private val semaphore = Semaphore(maxRequests)
    private val timestamps = mutableListOf<Long>()
    private val mutex = Mutex()

    /**
     * Rate limiting이 적용된 블록 실행
     *
     * @param block 실행할 suspend 함수
     * @return 함수 실행 결과
     *
     * 동작 방식:
     * 1. Semaphore로 동시 실행 수 제한
     * 2. 시간 윈도우 내 요청 수 체크
     * 3. 제한 초과 시 대기 후 실행
     */
    suspend fun <T> execute(block: suspend () -> T): T {
        semaphore.acquire()
        try {
            mutex.withLock {
                val now = System.currentTimeMillis()

                // 시간 윈도우를 벗어난 타임스탬프 제거
                timestamps.removeAll { now - it > timeWindow.inWholeMilliseconds }

                // 요청 제한 확인
                if (timestamps.size >= maxRequests) {
                    val oldestTimestamp = timestamps.first()
                    val waitTime = timeWindow.inWholeMilliseconds - (now - oldestTimestamp)

                    if (waitTime > 0) {
                        logger.debug("⏱️ Rate limit reached, waiting ${waitTime}ms")
                        delay(waitTime)
                        timestamps.clear()
                    }
                }

                timestamps.add(System.currentTimeMillis())
            }

            return block()
        } finally {
            semaphore.release()
        }
    }

    /**
     * 현재 상태 조회
     */
    suspend fun getStatus(): RateLimiterStatus {
        return mutex.withLock {
            val now = System.currentTimeMillis()
            timestamps.removeAll { now - it > timeWindow.inWholeMilliseconds }

            RateLimiterStatus(
                currentRequests = timestamps.size,
                maxRequests = maxRequests,
                availablePermits = semaphore.availablePermits
            )
        }
    }

    /**
     * 상태 초기화
     */
    suspend fun reset() {
        mutex.withLock {
            timestamps.clear()
            logger.info("🔄 Rate limiter reset")
        }
    }
}

data class RateLimiterStatus(
    val currentRequests: Int,
    val maxRequests: Int,
    val availablePermits: Int
)
