package com.example.examples

import com.example.domain.model.SystemEvent
import com.example.infrastructure.CoroutineInfrastructure
import com.example.infrastructure.EventBus
import com.example.infrastructure.RateLimiter
import com.example.infrastructure.RetryPolicy
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import org.slf4j.LoggerFactory
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

/**
 * 코루틴 패턴 실전 예제 모음
 *
 * 이 파일은 각 패턴의 동작을 시연하는 독립적인 예제들을 포함합니다.
 * main 함수를 실행하면 모든 패턴을 순차적으로 확인할 수 있습니다.
 */
object CoroutinePatternExamples {
    private val logger = LoggerFactory.getLogger(CoroutinePatternExamples::class.java)

    /**
     * 예제 1: 구조화된 동시성 (Structured Concurrency)
     */
    suspend fun example1_StructuredConcurrency() {
        logger.info("\n=== 예제 1: 구조화된 동시성 ===")

        try {
            coroutineScope {
                val job1 = launch {
                    logger.info("Job 1 시작")
                    delay(1000)
                    logger.info("Job 1 완료")
                }

                val job2 = launch {
                    logger.info("Job 2 시작")
                    delay(500)
                    throw Exception("Job 2 실패!")
                }

                // 하나의 자식이 실패하면 모든 자식이 취소됨
            }
        } catch (e: Exception) {
            logger.info("❌ coroutineScope 내부 실패: ${e.message}")
        }

        logger.info("✅ 부모 코루틴 계속 실행")
    }

    /**
     * 예제 2: supervisorScope - 독립적 작업
     */
    suspend fun example2_SupervisorScope() {
        logger.info("\n=== 예제 2: supervisorScope ===")

        supervisorScope {
            val job1 = launch {
                logger.info("독립 Job 1 시작")
                delay(1000)
                logger.info("✅ 독립 Job 1 완료")
            }

            val job2 = launch {
                logger.info("독립 Job 2 시작")
                delay(500)
                throw Exception("Job 2 실패!")
            }

            val job3 = launch {
                logger.info("독립 Job 3 시작")
                delay(1500)
                logger.info("✅ 독립 Job 3 완료")
            }

            // Job 2가 실패해도 Job 1, 3은 계속 실행됨
        }

        logger.info("✅ supervisorScope 완료 (Job 1, 3 성공)")
    }

    /**
     * 예제 3: Retry with Exponential Backoff
     */
    suspend fun example3_RetryPolicy() {
        logger.info("\n=== 예제 3: Retry with Exponential Backoff ===")

        var attemptCount = 0

        try {
            RetryPolicy.retryWithExponentialBackoff(
                times = 3,
                initialDelay = 100.milliseconds,
                maxDelay = 1.seconds
            ) {
                attemptCount++
                logger.info("🔄 시도 #$attemptCount")

                if (attemptCount < 3) {
                    throw Exception("일시적 오류 시뮬레이션")
                }

                logger.info("✅ 성공!")
                "성공 결과"
            }
        } catch (e: Exception) {
            logger.error("❌ 모든 재시도 실패", e)
        }
    }

    /**
     * 예제 4: Rate Limiting
     */
    suspend fun example4_RateLimiting() {
        logger.info("\n=== 예제 4: Rate Limiting ===")

        val rateLimiter = RateLimiter(maxRequests = 3, timeWindow = 1.seconds)

        coroutineScope {
            repeat(5) { i ->
                launch {
                    val startTime = System.currentTimeMillis()
                    rateLimiter.execute {
                        val elapsed = System.currentTimeMillis() - startTime
                        logger.info("⚡ 요청 #$i 실행 (대기 시간: ${elapsed}ms)")
                    }
                }
            }
        }

        logger.info("✅ Rate Limiting 완료 (초당 3개 제한)")
    }

    /**
     * 예제 5: Flow 기본
     */
    suspend fun example5_FlowBasics() {
        logger.info("\n=== 예제 5: Flow 기본 ===")

        flow {
            for (i in 1..5) {
                delay(200.milliseconds)
                emit(i)
            }
        }
            .onStart { logger.info("🚀 Flow 시작") }
            .map { it * 2 }
            .filter { it > 4 }
            .onEach { logger.info("📦 값: $it") }
            .onCompletion { logger.info("✅ Flow 완료") }
            .collect()
    }

    /**
     * 예제 6: Flow 고급 연산자
     */
    suspend fun example6_FlowAdvanced() {
        logger.info("\n=== 예제 6: Flow 고급 연산자 ===")

        val flow1 = flow {
            repeat(3) {
                delay(100.milliseconds)
                emit("Flow1-$it")
            }
        }

        val flow2 = flow {
            repeat(3) {
                delay(150.milliseconds)
                emit("Flow2-$it")
            }
        }

        // combine: 두 Flow의 최신 값을 결합
        logger.info("--- combine ---")
        combine(flow1, flow2) { a, b ->
            "$a & $b"
        }
            .take(3)
            .collect { logger.info("🔗 $it") }

        delay(500.milliseconds)

        // zip: 두 Flow의 값을 쌍으로 결합
        logger.info("--- zip ---")
        flow1.zip(flow2) { a, b ->
            "$a + $b"
        }
            .collect { logger.info("🤝 $it") }
    }

    /**
     * 예제 7: StateFlow & SharedFlow
     */
    suspend fun example7_HotFlows() {
        logger.info("\n=== 예제 7: StateFlow & SharedFlow ===")

        // StateFlow
        val stateFlow = MutableStateFlow(0)

        // 구독자 1
        val job1 = CoroutineInfrastructure.backgroundScope.launch {
            stateFlow.collect { value ->
                logger.info("📊 구독자1: 상태 = $value")
            }
        }

        // 구독자 2
        val job2 = CoroutineInfrastructure.backgroundScope.launch {
            delay(500.milliseconds)
            stateFlow.collect { value ->
                logger.info("📊 구독자2: 상태 = $value (나중에 시작)")
            }
        }

        // 상태 변경
        repeat(3) { i ->
            delay(300.milliseconds)
            stateFlow.value = i + 1
        }

        delay(1000.milliseconds)
        job1.cancel()
        job2.cancel()

        logger.info("✅ StateFlow 완료")
    }

    /**
     * 예제 8: Channel (Producer-Consumer)
     */
    suspend fun example8_Channels() {
        logger.info("\n=== 예제 8: Channel ===")

        val channel = Channel<Int>()

        // Producer
        val producer = CoroutineInfrastructure.backgroundScope.launch {
            repeat(5) { i ->
                delay(100.milliseconds)
                channel.send(i)
                logger.info("📤 생산: $i")
            }
            channel.close()
        }

        // Consumer
        val consumer = CoroutineInfrastructure.backgroundScope.launch {
            for (value in channel) {
                delay(200.milliseconds)
                logger.info("📥 소비: $value")
            }
        }

        producer.join()
        consumer.join()

        logger.info("✅ Channel 완료")
    }

    /**
     * 예제 9: EventBus 사용
     */
    suspend fun example9_EventBus() {
        logger.info("\n=== 예제 9: EventBus ===")

        // 이벤트 리스너
        val listenerJob = CoroutineInfrastructure.backgroundScope.launch {
            EventBus.systemEvents
                .take(3)
                .collect { event ->
                    logger.info("📢 이벤트 수신: ${event::class.simpleName}")
                }
        }

        delay(100.milliseconds)

        // 이벤트 발행
        EventBus.emit(SystemEvent.UserCreated(1L, "Alice"))
        delay(100.milliseconds)
        EventBus.emit(SystemEvent.OrderCreated(1L, 1L, 1000.0))
        delay(100.milliseconds)
        EventBus.emit(SystemEvent.SystemError("테스트 에러", null))

        listenerJob.join()
        logger.info("✅ EventBus 완료")
    }

    /**
     * 예제 10: 병렬 처리 vs 순차 처리 성능 비교
     */
    suspend fun example10_PerformanceComparison() {
        logger.info("\n=== 예제 10: 성능 비교 ===")

        suspend fun task1(): String {
            delay(1000.milliseconds)
            return "Task 1"
        }

        suspend fun task2(): String {
            delay(1500.milliseconds)
            return "Task 2"
        }

        suspend fun task3(): String {
            delay(800.milliseconds)
            return "Task 3"
        }

        // 순차 처리
        val sequentialTime = kotlin.system.measureTimeMillis {
            val r1 = task1()
            val r2 = task2()
            val r3 = task3()
            logger.info("순차 결과: $r1, $r2, $r3")
        }
        logger.info("⏱️ 순차 처리 시간: ${sequentialTime}ms")

        // 병렬 처리
        val parallelTime = kotlin.system.measureTimeMillis {
            coroutineScope {
                val d1 = async { task1() }
                val d2 = async { task2() }
                val d3 = async { task3() }

                val r1 = d1.await()
                val r2 = d2.await()
                val r3 = d3.await()
                logger.info("병렬 결과: $r1, $r2, $r3")
            }
        }
        logger.info("⚡ 병렬 처리 시간: ${parallelTime}ms")

        val speedup = sequentialTime.toDouble() / parallelTime
        logger.info("🚀 성능 향상: ${String.format("%.2f", speedup)}배")
    }

    /**
     * 모든 예제 실행
     */
    suspend fun runAllExamples() {
        logger.info("\n" + "=".repeat(50))
        logger.info("🎓 Kotlin 코루틴 고급 패턴 예제 시작")
        logger.info("=".repeat(50))

        try {
            example1_StructuredConcurrency()
            delay(500.milliseconds)

            example2_SupervisorScope()
            delay(500.milliseconds)

            example3_RetryPolicy()
            delay(500.milliseconds)

            example4_RateLimiting()
            delay(500.milliseconds)

            example5_FlowBasics()
            delay(500.milliseconds)

            example6_FlowAdvanced()
            delay(500.milliseconds)

            example7_HotFlows()
            delay(500.milliseconds)

            example8_Channels()
            delay(500.milliseconds)

            example9_EventBus()
            delay(500.milliseconds)

            example10_PerformanceComparison()

            logger.info("\n" + "=".repeat(50))
            logger.info("✅ 모든 예제 완료!")
            logger.info("=".repeat(50))

        } catch (e: Exception) {
            logger.error("❌ 예제 실행 중 오류", e)
        }
    }
}

/**
 * 예제 실행용 main 함수
 */
suspend fun main() {
    CoroutinePatternExamples.runAllExamples()
    CoroutineInfrastructure.shutdown()
}
