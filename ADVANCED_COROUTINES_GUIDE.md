# 고급 코루틴 실전 가이드

이 프로젝트는 **Kotlin 코루틴의 고급 패턴**을 실전에서 활용하는 방법을 보여줍니다.

## 📚 학습 목표

1. ✅ 구조화된 동시성 (Structured Concurrency) 실전 적용
2. ✅ Flow를 활용한 비동기 스트림 처리
3. ✅ StateFlow/SharedFlow로 이벤트 기반 아키텍처 구현
4. ✅ Retry, Rate Limiting 등 복원력 패턴
5. ✅ 적절한 Dispatcher 선택과 컨텍스트 전환
6. ✅ Channel을 활용한 Producer-Consumer 패턴
7. ✅ supervisorScope로 독립적 작업 격리

## 🏗️ 프로젝트 구조

```
src/main/kotlin/com/example/
├── infrastructure/              # 코루틴 인프라
│   ├── CoroutineInfrastructure.kt  # 스코프 관리
│   ├── RetryPolicy.kt              # 재시도 로직
│   ├── RateLimiter.kt              # 속도 제한
│   └── EventBus.kt                 # 이벤트 브로드캐스팅
├── service/
│   ├── AdvancedUserService.kt      # 고급 사용자 서비스
│   ├── AdvancedOrderService.kt     # 고급 주문 서비스
│   └── monitoring/
│       └── MetricsMonitor.kt       # 실시간 모니터링
├── application/
│   └── AdvancedDashboard.kt        # 통합 대시보드
└── domain/model/
    └── Events.kt                    # 이벤트 정의
```

## 🎯 핵심 패턴 및 예제

### 1. 구조화된 동시성과 명시적 스코프 관리

**안티패턴 (기존 코드)**:
```kotlin
// ❌ GlobalScope 사용 - 생명주기 관리 불가
GlobalScope.launch {
    someWork()
}
```

**개선된 패턴**:
```kotlin
// ✅ 명시적 스코프 관리
object CoroutineInfrastructure {
    val ioScope: CoroutineScope = CoroutineScope(
        Dispatchers.IO +
        SupervisorJob() +
        CoroutineExceptionHandler { _, exception ->
            logger.error("Unhandled exception", exception)
        }
    )
}

// 사용
CoroutineInfrastructure.ioScope.launch {
    someWork()
}

// 종료 시
CoroutineInfrastructure.shutdown()
```

**위치**: `infrastructure/CoroutineInfrastructure.kt`

**학습 포인트**:
- GlobalScope 대신 명시적 스코프 사용
- SupervisorJob으로 자식 코루틴 격리
- CoroutineExceptionHandler로 예외 중앙 처리
- 애플리케이션 생명주기와 코루틴 생명주기 연동

---

### 2. Retry with Exponential Backoff

**안티패턴**:
```kotlin
// ❌ 단순 재시도 - 외부 서비스 부담
suspend fun fetchData() {
    repeat(3) {
        try {
            return api.fetch()
        } catch (e: Exception) {
            // 즉시 재시도
        }
    }
}
```

**개선된 패턴**:
```kotlin
// ✅ 지수 백오프 재시도
suspend fun fetchDataWithRetry() {
    return RetryPolicy.retryWithExponentialBackoff(
        times = 3,
        initialDelay = 100.milliseconds,
        maxDelay = 1.seconds,
        factor = 2.0
    ) {
        api.fetch()
    }
}
```

**위치**: `infrastructure/RetryPolicy.kt`

**동작 방식**:
- 1차 실패 → 100ms 대기 후 재시도
- 2차 실패 → 200ms 대기 후 재시도
- 3차 실패 → 400ms 대기 후 재시도
- 모두 실패 → 예외 발생

**적용 예시**: `AdvancedUserService.getUserWithRetry()`

---

### 3. Rate Limiting

**문제 상황**: API 호출 속도 제한 필요

**해결책**:
```kotlin
val rateLimiter = RateLimiter(
    maxRequests = 10,      // 최대 요청 수
    timeWindow = 1.seconds  // 시간 윈도우
)

suspend fun callAPI() {
    rateLimiter.execute {
        // 초당 최대 10회 실행 보장
        httpClient.get("url")
    }
}
```

**위치**: `infrastructure/RateLimiter.kt`

**동작 원리**:
- Semaphore로 동시 실행 제어
- Mutex로 타임스탬프 동기화
- 슬라이딩 윈도우 알고리즘

**적용 예시**: `AdvancedUserService` 모든 메서드에 적용

---

### 4. Cache with Mutex (Thread-Safe Caching)

**안티패턴**:
```kotlin
// ❌ Race condition 위험
private val cache = mutableMapOf<Long, User>()

suspend fun getUser(id: Long): User {
    return cache[id] ?: fetchFromDB(id).also { cache[id] = it }
}
```

**개선된 패턴**:
```kotlin
// ✅ Mutex로 동기화
private val cache = mutableMapOf<Long, User>()
private val mutex = Mutex()

suspend fun getUserCached(id: Long): User {
    cache[id]?.let { return it }

    return mutex.withLock {
        // Double-check locking
        cache[id]?.let { return it }

        val user = fetchFromDB(id)
        cache[id] = user
        user
    }
}
```

**위치**: `AdvancedUserService.getUserCached()`

**학습 포인트**:
- Mutex를 사용한 상호 배제
- Double-check locking 패턴
- 코루틴 안전한 캐싱

---

### 5. supervisorScope - 독립적 작업 실행

**안티패턴**:
```kotlin
// ❌ 하나의 실패가 모두 취소
suspend fun fetchMultiple(ids: List<Long>) = coroutineScope {
    ids.map { id ->
        async { fetchUser(id) } // 하나 실패 시 모두 취소!
    }.awaitAll()
}
```

**개선된 패턴**:
```kotlin
// ✅ supervisorScope - 독립적 실행
suspend fun fetchMultiple(ids: List<Long>) = supervisorScope {
    ids.map { id ->
        async {
            try {
                Result.success(fetchUser(id))
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }.awaitAll()
}
```

**위치**: `AdvancedUserService.getUsersBatch()`

**차이점**:
- `coroutineScope`: 하나 실패 → 모두 취소
- `supervisorScope`: 각 작업 독립적

---

### 6. Flow - 비동기 스트림 처리

**안티패턴**:
```kotlin
// ❌ 모든 데이터를 메모리에 로드
suspend fun processAllUsers(): List<Result> {
    val users = userRepository.findAll() // 수만 건 로드!
    return users.map { process(it) }
}
```

**개선된 패턴**:
```kotlin
// ✅ Flow로 스트리밍 처리
fun streamUsers(): Flow<User> = flow {
    var offset = 0
    val batchSize = 100

    while (currentCoroutineContext().isActive) {
        val batch = repository.findAll()
            .drop(offset)
            .take(batchSize)

        if (batch.isEmpty()) break

        batch.forEach { emit(it) }
        offset += batchSize
    }
}
    .onEach { user -> process(user) }
    .catch { e -> logger.error("Error", e) }
```

**위치**: `AdvancedUserService.streamAllUsers()`

**Flow 연산자 활용**:
```kotlin
streamUsers()
    .filter { it.active }        // 필터링
    .map { it.toDto() }          // 변환
    .onEach { sendNotification(it) }  // 부수 효과
    .buffer(capacity = 100)      // 버퍼링
    .collect()                   // 수집
```

---

### 7. StateFlow & SharedFlow - 이벤트 브로드캐스팅

**StateFlow (상태 관리)**:
```kotlin
// 현재 상태를 유지하는 Hot Flow
private val _metrics = MutableStateFlow(SystemMetrics())
val metrics: StateFlow<SystemMetrics> = _metrics.asStateFlow()

// 상태 업데이트
_metrics.value = newMetrics

// 구독 (항상 최신 상태를 받음)
metrics.collect { currentMetrics ->
    updateUI(currentMetrics)
}
```

**SharedFlow (이벤트 스트림)**:
```kotlin
// 이벤트 브로드캐스트용 Hot Flow
private val _events = MutableSharedFlow<SystemEvent>(
    replay = 0,                    // 새 구독자는 이전 이벤트 받지 않음
    extraBufferCapacity = 100,
    onBufferOverflow = BufferOverflow.DROP_OLDEST
)
val events: SharedFlow<SystemEvent> = _events.asSharedFlow()

// 이벤트 발행
_events.emit(UserCreatedEvent(userId))

// 구독 (여러 구독자 가능)
events.collect { event ->
    handleEvent(event)
}
```

**위치**: `infrastructure/EventBus.kt`

**차이점**:
- StateFlow: 항상 값을 가지며, 최신 상태 유지
- SharedFlow: 이벤트 스트림, replay 설정 가능

---

### 8. Channel - Producer-Consumer 패턴

```kotlin
// Channel 생성
private val orderChannel = Channel<Order>(Channel.BUFFERED)

// Producer
suspend fun submitOrder(order: Order) {
    orderChannel.send(order)
}

// Consumer
fun startProcessor(): Job = scope.launch {
    for (order in orderChannel) {
        processOrder(order)
    }
}
```

**위치**: `AdvancedOrderService.startOrderProcessor()`

**사용 사례**:
- 비동기 작업 큐
- 백그라운드 작업 처리
- 버퍼링된 이벤트 처리

---

### 9. Flow 고급 연산자

**combine - 여러 Flow 결합**:
```kotlin
val dashboardFlow = combine(
    metricsFlow,
    orderStateFlow,
    eventFlow
) { metrics, orderState, events ->
    Dashboard(metrics, orderState, events)
}
```

**flatMapMerge - 병렬 처리**:
```kotlin
orders.asFlow()
    .flatMapMerge(concurrency = 5) { order ->
        flow { emit(processOrder(order)) }
    }
    .collect()
```

**conflate - 최신 값만 유지**:
```kotlin
realtimeData
    .conflate() // 느린 구독자는 중간 값 건너뛰기
    .collect { latest -> updateUI(latest) }
```

**위치**: `AdvancedDashboard.kt`, `AdvancedOrderService.kt`

---

### 10. 적절한 Dispatcher 선택

**Dispatcher 선택 가이드**:

| Dispatcher | 사용 사례 | 스레드 풀 크기 |
|-----------|---------|-------------|
| `Dispatchers.IO` | DB 쿼리, 파일 I/O, 네트워크 | 64개 |
| `Dispatchers.Default` | CPU 집약적 작업 (계산, 정렬) | CPU 코어 수 |
| `Dispatchers.Main` | UI 업데이트 (Android/Desktop) | 1개 |

**예시**:
```kotlin
suspend fun complexOperation() {
    // DB 조회
    val data = withContext(Dispatchers.IO) {
        database.query()
    }

    // CPU 집약적 처리
    val processed = withContext(Dispatchers.Default) {
        data.map { heavy(it) }.sorted()
    }

    return processed
}
```

**위치**: 모든 서비스 클래스

---

### 11. 실시간 모니터링

```kotlin
fun monitorMetrics(): Flow<SystemMetrics> = flow {
    while (isActive) {
        val metrics = supervisorScope {
            // 병렬로 데이터 수집
            val users = async { userService.getCount() }
            val orders = async { orderService.getCount() }

            SystemMetrics(users.await(), orders.await())
        }

        emit(metrics)
        delay(5.seconds)
    }
}
    .onEach { EventBus.updateMetrics(it) }
    .catch { logger.error("Monitoring error", it) }
```

**위치**: `monitoring/MetricsMonitor.kt`

---

## 🚀 실행 가이드

### 1. 기본 예제 실행

```kotlin
fun main() = runBlocking {
    // 1. 재시도 패턴
    val user = RetryPolicy.retryWithExponentialBackoff {
        userService.getUser(1L)
    }

    // 2. Flow 스트리밍
    userService.streamAllUsers()
        .take(10)
        .collect { user ->
            println("User: ${user.name}")
        }

    // 3. 이벤트 수신
    EventBus.systemEvents
        .collect { event ->
            println("Event: $event")
        }
}
```

### 2. 대시보드 모니터링

```kotlin
val dashboard = AdvancedDashboard(userService, orderService, monitor)

// 실시간 대시보드 스트림
launch {
    dashboard.getDashboardStream()
        .collect { snapshot ->
            println("Dashboard: $snapshot")
        }
}

// 이벤트 리스닝
val eventJob = dashboard.startEventListener()
```

### 3. 배치 처리

```kotlin
// 사용자 배치 조회
val results = advancedUserService.getUsersBatch(listOf(1L, 2L, 3L))
results.forEach { result ->
    result.onSuccess { user -> println("Success: $user") }
          .onFailure { error -> println("Failed: $error") }
}

// Flow 기반 배치 처리
advancedUserService.processUsersInBatches(
    processor = { user -> sendEmail(user) },
    batchSize = 50
)
```

---

## 📊 성능 비교

### 순차 vs 병렬 처리

**순차 처리**:
```kotlin
// 총 시간: 1000ms + 2000ms + 1500ms = 4500ms
suspend fun sequential() {
    val users = fetchUsers()     // 1000ms
    val orders = fetchOrders()   // 2000ms
    val stats = fetchStats()     // 1500ms
}
```

**병렬 처리**:
```kotlin
// 총 시간: max(1000ms, 2000ms, 1500ms) = 2000ms
suspend fun parallel() = coroutineScope {
    val usersDeferred = async { fetchUsers() }    // 1000ms
    val ordersDeferred = async { fetchOrders() }  // 2000ms
    val statsDeferred = async { fetchStats() }    // 1500ms

    Triple(usersDeferred.await(), ordersDeferred.await(), statsDeferred.await())
}
```

**성능 향상**: 2.25배 빠름!

---

## 🎓 학습 체크리스트

- [ ] GlobalScope 대신 명시적 스코프 사용
- [ ] supervisorScope vs coroutineScope 이해
- [ ] Dispatcher 적절히 선택 (IO vs Default)
- [ ] Retry with Exponential Backoff 구현
- [ ] Rate Limiting 적용
- [ ] Mutex로 Thread-Safe Cache 구현
- [ ] Flow 기본 연산자 활용
- [ ] StateFlow/SharedFlow 차이 이해
- [ ] Channel로 Producer-Consumer 구현
- [ ] CoroutineExceptionHandler 설정
- [ ] 구조화된 동시성 원칙 적용
- [ ] 적절한 타임아웃 설정
- [ ] Flow 고급 연산자 (combine, flatMapMerge, conflate) 활용

---

## 🔍 디버깅 팁

### 1. 코루틴 이름 지정
```kotlin
val scope = CoroutineScope(
    Dispatchers.Default +
    CoroutineName("MyService")
)

scope.launch(CoroutineName("ProcessOrder-$orderId")) {
    // 로그에서 코루틴 이름 확인 가능
}
```

### 2. 로깅으로 흐름 추적
```kotlin
suspend fun process() {
    logger.info("Start: ${Thread.currentThread().name}")

    withContext(Dispatchers.IO) {
        logger.info("IO: ${Thread.currentThread().name}")
    }

    logger.info("End: ${Thread.currentThread().name}")
}
```

### 3. 상태 모니터링
```kotlin
// EventBus 상태 조회
val metrics = EventBus.getCurrentMetrics()
val orderState = EventBus.getCurrentOrderState()

// Rate Limiter 상태
val status = rateLimiter.getStatus()
println("Available: ${status.availablePermits}")
```

---

## 📚 참고 자료

- [Kotlin Coroutines 공식 가이드](https://kotlinlang.org/docs/coroutines-guide.html)
- [Flow 공식 문서](https://kotlinlang.org/docs/flow.html)
- [Coroutines Best Practices](https://developer.android.com/kotlin/coroutines/coroutines-best-practices)
- [구조화된 동시성](https://kotlinlang.org/docs/coroutines-basics.html#structured-concurrency)

---

## 🎯 다음 단계

1. ✅ 모든 패턴을 실제 프로젝트에 적용
2. ✅ 성능 테스트로 병렬 처리 효과 측정
3. ✅ 모니터링 대시보드 구축
4. 🔄 프로덕션 환경 적용 및 튜닝
5. 🔄 부하 테스트 및 최적화

---

**이 프로젝트는 초급 코루틴 사용법에서 고급 실전 패턴으로의 전환을 보여줍니다!**
