package com.example.service

import com.example.domain.exception.UserNotFoundException
import com.example.domain.model.SystemEvent
import com.example.domain.model.User
import com.example.infrastructure.CoroutineInfrastructure
import com.example.infrastructure.EventBus
import com.example.infrastructure.RateLimiter
import com.example.infrastructure.RetryPolicy
import com.example.repository.UserRepository
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.slf4j.LoggerFactory
import kotlin.time.Duration.Companion.seconds

/**
 * 고급 사용자 서비스 - 실전 코루틴 패턴 적용
 *
 * 학습 포인트:
 * 1. Retry with Exponential Backoff
 * 2. Rate Limiting
 * 3. Cache with Mutex
 * 4. Flow-based streaming
 * 5. StateFlow/SharedFlow event broadcasting
 * 6. supervisorScope for independent tasks
 * 7. Proper Dispatcher selection
 * 8. CoroutineExceptionHandler
 */
class AdvancedUserService(
    private val userRepository: UserRepository
) {
    private val logger = LoggerFactory.getLogger(AdvancedUserService::class.java)

    // Rate Limiter: 초당 최대 10개 요청
    private val rateLimiter = RateLimiter(maxRequests = 10, timeWindow = 1.seconds)

    // Cache with Mutex for thread-safety
    private val cache = mutableMapOf<Long, User>()
    private val cacheMutex = Mutex()

    /**
     * 사용자 조회 with Retry & Rate Limiting
     *
     * 패턴:
     * - RetryPolicy: 일시적 DB 오류 대응
     * - RateLimiter: 외부 API 호출 속도 제한
     * - Dispatchers.IO: DB I/O 작업
     */
    suspend fun getUserWithRetry(id: Long): User = withContext(Dispatchers.IO) {
        logger.debug("[Advanced] Getting user with retry: $id")

        rateLimiter.execute {
            RetryPolicy.retryWithExponentialBackoff(
                times = 3,
                initialDelay = 100.milliseconds,
                maxDelay = 1.seconds
            ) {
                userRepository.findById(id) ?: throw UserNotFoundException(id)
            }
        }
    }

    /**
     * 캐싱된 사용자 조회 with Mutex
     *
     * 패턴:
     * - Mutex: 캐시 동시 접근 동기화
     * - Double-check locking
     */
    suspend fun getUserCached(id: Long): User {
        // 캐시 확인
        cache[id]?.let {
            logger.debug("[Cache] Cache hit for user: $id")
            return it
        }

        // 캐시 미스 - DB 조회
        return cacheMutex.withLock {
            // Double-check: lock 획득 후 재확인
            cache[id]?.let { return it }

            logger.debug("[Cache] Cache miss for user: $id, fetching from DB")
            val user = getUserWithRetry(id)
            cache[id] = user
            user
        }
    }

    /**
     * 여러 사용자 병렬 조회 with supervisorScope
     *
     * 패턴:
     * - supervisorScope: 하나의 조회 실패가 다른 조회에 영향 없음
     * - async/await: 병렬 실행
     */
    suspend fun getUsersBatch(ids: List<Long>): List<Result<User>> = supervisorScope {
        logger.info("[Batch] Fetching ${ids.size} users in parallel")

        ids.map { id ->
            async(Dispatchers.IO) {
                try {
                    Result.success(getUserWithRetry(id))
                } catch (e: Exception) {
                    logger.warn("[Batch] Failed to fetch user $id: ${e.message}")
                    Result.failure(e)
                }
            }
        }.awaitAll()
    }

    /**
     * 사용자 스트리밍 with Flow
     *
     * 패턴:
     * - Flow: 메모리 효율적인 배치 처리
     * - collect로 lazy evaluation
     */
    fun streamAllUsers(batchSize: Int = 100): Flow<User> = flow {
        logger.info("[Stream] Starting user stream with batch size: $batchSize")

        var offset = 0
        while (currentCoroutineContext().isActive) {
            val users = withContext(Dispatchers.IO) {
                userRepository.findAll()
                    .drop(offset)
                    .take(batchSize)
            }

            if (users.isEmpty()) break

            users.forEach { emit(it) }
            offset += batchSize
        }
    }
        .onEach { user ->
            logger.debug("[Stream] Emitted user: ${user.id}")
        }
        .catch { e ->
            logger.error("[Stream] Error in user stream", e)
            EventBus.emit(SystemEvent.SystemError("User stream error", e))
        }

    /**
     * 사용자 생성 with Event Broadcasting
     *
     * 패턴:
     * - SharedFlow: 이벤트 브로드캐스트
     * - 백그라운드 작업 분리 (backgroundScope)
     */
    suspend fun createUserWithEvents(name: String, email: String): User = coroutineScope {
        logger.info("[Create] Creating user: $name")

        val user = withContext(Dispatchers.IO) {
            val newUser = User(0, name, email)
            userRepository.create(newUser)
        }

        // 이벤트 발행 (비동기)
        CoroutineInfrastructure.backgroundScope.launch {
            EventBus.emit(SystemEvent.UserCreated(user.id, user.name))
            logger.debug("[Event] User created event emitted")
        }

        // 캐시 무효화
        invalidateCache(user.id)

        user
    }

    /**
     * 대량 사용자 처리 with Flow & Batch
     *
     * 패턴:
     * - Flow.chunked: 배치 단위 처리
     * - 메모리 효율적 대량 처리
     */
    suspend fun processUsersInBatches(
        processor: suspend (User) -> Unit,
        batchSize: Int = 50
    ) {
        streamAllUsers(batchSize)
            .buffer(capacity = batchSize) // 버퍼링으로 처리 속도 향상
            .collect { user ->
                try {
                    processor(user)
                } catch (e: Exception) {
                    logger.error("[Batch Process] Error processing user ${user.id}", e)
                }
            }
    }

    /**
     * 타임아웃 적용 사용자 조회
     */
    suspend fun getUserWithTimeout(id: Long, timeoutSeconds: Long = 5): User {
        return withTimeout(timeoutSeconds.seconds) {
            getUserWithRetry(id)
        }
    }

    /**
     * 캐시 관리
     */
    suspend fun invalidateCache(id: Long) {
        cacheMutex.withLock {
            cache.remove(id)
            logger.debug("[Cache] Invalidated cache for user: $id")
        }
    }

    suspend fun clearCache() {
        cacheMutex.withLock {
            cache.clear()
            logger.info("[Cache] Cache cleared")
        }
    }

    fun getCacheSize(): Int = cache.size
}
