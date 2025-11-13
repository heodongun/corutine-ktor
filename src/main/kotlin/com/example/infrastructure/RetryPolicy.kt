package com.example.infrastructure

import kotlinx.coroutines.delay
import org.slf4j.LoggerFactory
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

/**
 * 재시도 정책 (Retry with Exponential Backoff)
 *
 * 학습 포인트:
 * - 일시적 오류에 대한 복원력(Resilience) 향상
 * - 지수 백오프로 외부 서비스 부하 완화
 * - suspend 함수의 고차 함수 활용
 */
object RetryPolicy {
    private val logger = LoggerFactory.getLogger(RetryPolicy::class.java)

    /**
     * 지수 백오프를 사용한 재시도
     *
     * @param times 최대 재시도 횟수
     * @param initialDelay 초기 대기 시간
     * @param maxDelay 최대 대기 시간
     * @param factor 지수 증가 배율
     * @param block 실행할 suspend 함수
     * @return 성공 시 결과, 모든 시도 실패 시 예외 발생
     *
     * 예시:
     * - 1차 시도 실패 → 100ms 대기
     * - 2차 시도 실패 → 200ms 대기
     * - 3차 시도 실패 → 400ms 대기
     */
    suspend fun <T> retryWithExponentialBackoff(
        times: Int = 3,
        initialDelay: Duration = 100.milliseconds,
        maxDelay: Duration = 1.seconds,
        factor: Double = 2.0,
        block: suspend () -> T
    ): T {
        var currentDelay = initialDelay
        var lastException: Exception? = null

        repeat(times - 1) { attempt ->
            try {
                return block()
            } catch (e: Exception) {
                lastException = e
                logger.warn(
                    "⚠️ Attempt ${attempt + 1}/$times failed, retrying in ${currentDelay}ms: ${e.message}"
                )
                delay(currentDelay)
                currentDelay = (currentDelay * factor).coerceAtMost(maxDelay)
            }
        }

        // 마지막 시도
        return try {
            block()
        } catch (e: Exception) {
            logger.error("❌ All $times retry attempts failed", e)
            throw lastException ?: e
        }
    }

    /**
     * 조건부 재시도
     * - 특정 예외 타입만 재시도
     */
    suspend fun <T> retryOn(
        times: Int = 3,
        vararg exceptions: Class<out Exception>,
        block: suspend () -> T
    ): T {
        repeat(times - 1) { attempt ->
            try {
                return block()
            } catch (e: Exception) {
                if (exceptions.any { it.isInstance(e) }) {
                    logger.warn("⚠️ Retryable exception at attempt ${attempt + 1}/$times: ${e.message}")
                    delay((100 * (attempt + 1)).toLong())
                } else {
                    throw e
                }
            }
        }

        return block()
    }

    /**
     * 타임아웃과 재시도를 결합
     */
    suspend fun <T> retryWithTimeout(
        times: Int = 3,
        timeout: Duration = 5.seconds,
        block: suspend () -> T
    ): T {
        return retryWithExponentialBackoff(times) {
            kotlinx.coroutines.withTimeout(timeout) {
                block()
            }
        }
    }
}
