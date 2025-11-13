# 🎓 Kotlin 코루틴 학습 가이드

이 문서는 Ktor 프로젝트에서 코루틴이 어디서, 어떻게 사용되었는지 상세히 설명합니다.

## 📚 목차

1. [코루틴이란?](#코루틴이란)
2. [이 프로젝트에서 배울 수 있는 것](#이-프로젝트에서-배울-수-있는-것)
3. [레이어별 코루틴 사용](#레이어별-코루틴-사용)
4. [코루틴 패턴 상세 설명](#코루틴-패턴-상세-설명)
5. [실제 코드 예제](#실제-코드-예제)
6. [성능 비교](#성능-비교)
7. [베스트 프랙티스](#베스트-프랙티스)
8. [추가 학습 자료](#추가-학습-자료)

---

## 코루틴이란?

**코루틴(Coroutine)**은 Kotlin의 비동기 프로그래밍을 위한 경량 스레드입니다.

### 주요 특징

- **경량**: 수천 개의 코루틴을 동시에 실행해도 메모리 부담이 적음
- **구조화된 동시성**: 코루틴의 생명주기를 명확하게 관리
- **순차적 코드**: 비동기 코드를 동기 코드처럼 작성 가능
- **취소 가능**: 실행 중인 작업을 안전하게 취소 가능

### 전통적인 스레드 vs 코루틴

```kotlin
// 전통적인 스레드 (무겁고 비용이 큼)
thread {
    Thread.sleep(1000)
    println("Done")
}

// 코루틴 (경량이고 효율적)
launch {
    delay(1000)
    println("Done")
}
```

---

## 이 프로젝트에서 배울 수 있는 것

이 프로젝트는 다음과 같은 코루틴 개념을 실습할 수 있도록 설계되었습니다:

1. ✅ **suspend 함수**: 일시 중단 가능한 함수 정의와 호출
2. ✅ **async/await**: 병렬 실행과 결과 대기
3. ✅ **coroutineScope**: 구조화된 동시성
4. ✅ **delay**: 비동기 작업 시뮬레이션
5. ✅ **레이어 간 코루틴 전파**: suspend 함수가 레이어를 통과하는 방식
6. ✅ **에러 처리**: 코루틴 컨텍스트에서의 예외 처리

---

## 레이어별 코루틴 사용

이 프로젝트는 4개의 레이어로 구성되어 있으며, 각 레이어에서 코루틴을 다르게 활용합니다.

```
Controller (Routing)
    ↓ suspend 함수 호출
Application (Service Composition)
    ↓ async/await로 병렬 실행
Services (Business Logic)
    ↓ suspend 함수 체이닝
Repository (Data Access)
    ↓ delay로 비동기 시뮬레이션
```

### 1️⃣ Repository 레이어: suspend 함수와 delay

**위치**: `src/main/kotlin/com/example/repository/`

**역할**: 데이터 접근을 시뮬레이션하고 `delay`를 통해 비동기 작업을 보여줌

**코루틴 사용**:

```kotlin
// UserRepositoryImpl.kt
override suspend fun findAll(): List<User> {
    logger.debug("[Repository] Finding all users - simulating DB delay...")
    delay(150) // 🔑 데이터베이스 조회를 시뮬레이션 (스레드를 블로킹하지 않음!)
    logger.debug("[Repository] Found ${users.size} users")
    return users.values.toList()
}
```

**학습 포인트**:
- `suspend` 키워드: 이 함수는 일시 중단될 수 있음을 나타냄
- `delay()`: 코루틴을 일시 중단하지만 스레드는 블로킹하지 않음
- 다른 코루틴들은 이 시간 동안 같은 스레드에서 실행될 수 있음

**다른 예제**:
- `UserRepositoryImpl.findById()` - 100ms delay
- `OrderRepositoryImpl.findAll()` - 200ms delay
- `NotificationRepositoryImpl.create()` - 150ms delay

---

### 2️⃣ Service 레이어: suspend 함수 체이닝

**위치**: `src/main/kotlin/com/example/service/`

**역할**: 비즈니스 로직 처리 및 Repository 호출

**코루틴 사용**:

```kotlin
// UserServiceImpl.kt
override suspend fun createUser(name: String, email: String): User {
    logger.debug("[Service] Creating user: $name")
    
    // 비즈니스 로직: 이메일 검증
    if (!email.contains("@")) {
        throw InvalidRequestException("Invalid email format: $email")
    }
    
    val user = User(0, name, email)
    return userRepository.create(user) // 🔑 suspend 함수 호출
}
```

**학습 포인트**:
- Service의 모든 메서드도 `suspend` 함수
- Repository의 `suspend` 함수를 직접 호출
- 비동기 작업이지만 코드는 순차적으로 작성됨

**다른 예제**:
- `OrderServiceImpl.getStatistics()` - 여러 Repository 호출을 순차적으로 처리
- `NotificationServiceImpl.sendWelcomeEmail()` - 알림 생성 및 저장

---

### 3️⃣ Application 레이어: async/await와 병렬 처리

**위치**: `src/main/kotlin/com/example/application/`

**역할**: 여러 서비스를 조합하고 병렬 실행으로 성능 최적화

이 레이어가 **코루틴의 진정한 힘**을 보여주는 곳입니다!

#### 예제 1: 병렬 실행 (UserApplication.kt)

```kotlin
suspend fun getUserWithDetails(userId: Long): UserDetails = coroutineScope {
    logger.info("[Application] 🚀 Starting getUserWithDetails for userId: $userId")
    logger.info("[Application] 📊 Launching parallel coroutines...")
    
    // 🔑 async를 사용하여 세 가지 작업을 병렬로 시작
    val userDeferred = async {
        logger.debug("[Application] 👤 Fetching user data...")
        userService.getUserById(userId)
    }
    
    val ordersDeferred = async {
        logger.debug("[Application] 📦 Fetching orders data...")
        orderService.getOrdersByUserId(userId)
    }
    
    val notificationsDeferred = async {
        logger.debug("[Application] 📧 Fetching notifications data...")
        notificationService.getRecentByUserId(userId)
    }
    
    logger.info("[Application] ⏳ Waiting for all parallel operations...")
    
    // 🔑 await()를 호출하여 모든 결과를 기다림
    UserDetails(
        user = userDeferred.await(),
        orders = ordersDeferred.await(),
        recentNotifications = notificationsDeferred.await()
    )
}
```

**학습 포인트**:
- `coroutineScope`: 구조화된 동시성 스코프 생성
- `async`: 비동기 작업을 시작하고 `Deferred` 반환
- `await()`: `Deferred`의 결과를 기다림
- 세 작업이 **동시에** 실행되어 성능 향상

#### 예제 2: 순차 실행 (UserApplication.kt)

```kotlin
suspend fun createUserWithWelcome(name: String, email: String): UserCreationResult {
    logger.info("[Application] 🚀 Starting createUserWithWelcome for: $name")
    
    // 🔑 순차 실행: 사용자를 먼저 생성
    val user = userService.createUser(name, email)
    logger.info("[Application] ✅ User created with id: ${user.id}")
    
    // 🔑 순차 실행: 생성된 사용자 ID로 알림 발송
    val notification = notificationService.sendWelcomeEmail(user.id, email)
    logger.info("[Application] ✅ Welcome notification sent")
    
    return UserCreationResult(user, notification)
}
```

**학습 포인트**:
- 두 작업이 의존 관계에 있으므로 순차적으로 실행
- `async`를 사용하지 않고 직접 `suspend` 함수 호출
- 코드가 동기 코드처럼 읽히지만 실제로는 비동기

#### 예제 3: 대시보드 병렬 처리 (DashboardApplication.kt)

```kotlin
suspend fun getDashboardData(): DashboardData = coroutineScope {
    logger.info("[Dashboard] 🚀 Starting getDashboardData")
    val startTime = System.currentTimeMillis()
    
    // 🔑 세 가지 독립적인 작업을 병렬로 실행
    val usersDeferred = async { userService.getAllUsers() }
    val ordersDeferred = async { orderService.getAllOrders() }
    val statsDeferred = async { orderService.getStatistics() }
    
    val result = DashboardData(
        users = usersDeferred.await(),
        orders = ordersDeferred.await(),
        stats = statsDeferred.await()
    )
    
    val duration = System.currentTimeMillis() - startTime
    logger.info("[Dashboard] ✅ Completed in ${duration}ms")
    
    result
}
```

**학습 포인트**:
- 독립적인 작업들을 병렬로 실행하여 성능 최적화
- 실제 실행 시간을 측정하여 병렬 처리의 이점 확인

---

### 4️⃣ Controller 레이어: Ktor의 코루틴 통합

**위치**: `src/main/kotlin/com/example/controller/`

**역할**: HTTP 라우팅 정의 및 Application 레이어 호출

**코루틴 사용**:

```kotlin
// UserRoutes.kt
fun Route.userRoutes(userApplication: UserApplication) {
    route("/users") {
        // 🔑 Ktor의 라우트 핸들러는 자동으로 코루틴 컨텍스트에서 실행됨
        get("/{id}/details") {
            val id = call.parameters["id"]?.toLongOrNull()
                ?: return@get call.respond(HttpStatusCode.BadRequest, ...)
            
            // 🔑 Application의 suspend 함수를 직접 호출
            val userDetails = userApplication.getUserWithDetails(id)
            
            // 🔑 call.respond()도 suspend 함수
            call.respond(userDetails)
        }
    }
}
```

**학습 포인트**:
- Ktor는 모든 라우트 핸들러를 코루틴으로 실행
- `suspend` 키워드 없이도 suspend 함수 호출 가능
- `call.respond()`, `call.receive()` 등도 모두 suspend 함수

---

## 코루틴 패턴 상세 설명

### 1. suspend 함수

**정의**: 일시 중단될 수 있는 함수

```kotlin
suspend fun fetchData(): String {
    delay(1000) // 코루틴을 1초 동안 일시 중단
    return "Data"
}
```

**특징**:
- `suspend` 키워드로 표시
- 다른 suspend 함수나 코루틴 내에서만 호출 가능
- 스레드를 블로킹하지 않고 일시 중단

**프로젝트 내 사용 위치**:
- 모든 Repository 메서드
- 모든 Service 메서드
- 모든 Application 메서드

---

### 2. coroutineScope

**정의**: 구조화된 동시성을 위한 스코프

```kotlin
suspend fun doWork() = coroutineScope {
    // 이 스코프 내의 모든 코루틴이 완료될 때까지 대기
    launch { task1() }
    launch { task2() }
} // 여기서 모든 작업이 완료됨
```

**특징**:
- 스코프 내의 모든 코루틴이 완료될 때까지 대기
- 하나의 코루틴이 실패하면 모든 코루틴 취소
- 구조화된 동시성 보장

**프로젝트 내 사용 위치**:
- `UserApplication.getUserWithDetails()`
- `DashboardApplication.getDashboardData()`

---

### 3. async/await

**정의**: 비동기 작업을 시작하고 결과를 기다림

```kotlin
suspend fun parallelWork() = coroutineScope {
    val result1 = async { heavyWork1() } // 시작
    val result2 = async { heavyWork2() } // 시작
    
    // 두 작업이 병렬로 실행됨
    
    val finalResult = result1.await() + result2.await() // 결과 대기
}
```

**특징**:
- `async`: 비동기 작업을 시작하고 `Deferred<T>` 반환
- `await()`: 결과를 기다림
- 여러 작업을 병렬로 실행 가능

**프로젝트 내 사용 위치**:
- `UserApplication.getUserWithDetails()` - 사용자, 주문, 알림을 병렬 조회
- `DashboardApplication.getDashboardData()` - 대시보드 데이터를 병렬 조회

---

### 4. delay

**정의**: 코루틴을 일시 중단 (스레드 블로킹 없음)

```kotlin
suspend fun simulateWork() {
    delay(1000) // 1초 대기 (스레드는 다른 작업 수행 가능)
}
```

**Thread.sleep() vs delay()**:
```kotlin
// ❌ Thread.sleep() - 스레드를 블로킹
Thread.sleep(1000) // 이 스레드는 1초 동안 아무것도 못함

// ✅ delay() - 코루틴만 일시 중단
delay(1000) // 이 스레드는 다른 코루틴을 실행할 수 있음
```

**프로젝트 내 사용 위치**:
- 모든 Repository 구현체에서 데이터베이스 작업 시뮬레이션

---

## 실제 코드 예제

### 예제 1: Repository에서 delay 사용

**파일**: `src/main/kotlin/com/example/repository/UserRepositoryImpl.kt`

```kotlin
override suspend fun findById(id: Long): User? {
    logger.debug("[Repository] Finding user by id: $id - simulating DB delay...")
    delay(100) // 데이터베이스 조회 시뮬레이션
    val user = users[id]
    logger.debug("[Repository] User found: ${user != null}")
    return user
}
```

**실행 흐름**:
1. 로그 출력: "Finding user by id..."
2. `delay(100)` 호출 → 코루틴 일시 중단 (스레드는 해제)
3. 100ms 후 코루틴 재개
4. 사용자 조회 및 반환

---

### 예제 2: Service에서 suspend 함수 체이닝

**파일**: `src/main/kotlin/com/example/service/UserServiceImpl.kt`

```kotlin
override suspend fun getUserById(id: Long): User {
    logger.debug("[Service] Getting user by id: $id")
    return userRepository.findById(id) 
        ?: throw UserNotFoundException(id)
}
```

**실행 흐름**:
1. Service 메서드 호출
2. Repository의 suspend 함수 호출
3. Repository에서 delay 발생
4. 결과 반환 또는 예외 발생

---

### 예제 3: Application에서 병렬 실행

**파일**: `src/main/kotlin/com/example/application/UserApplication.kt`

```kotlin
suspend fun getUserWithDetails(userId: Long): UserDetails = coroutineScope {
    val userDeferred = async { userService.getUserById(userId) }
    val ordersDeferred = async { orderService.getOrdersByUserId(userId) }
    val notificationsDeferred = async { notificationService.getRecentByUserId(userId) }
    
    UserDetails(
        user = userDeferred.await(),
        orders = ordersDeferred.await(),
        recentNotifications = notificationsDeferred.await()
    )
}
```

**실행 흐름**:
1. `coroutineScope` 시작
2. 세 개의 `async` 블록이 **동시에** 시작됨
   - 사용자 조회 (100ms)
   - 주문 조회 (180ms)
   - 알림 조회 (130ms)
3. 세 작업이 병렬로 실행됨
4. `await()` 호출 시 가장 긴 작업(180ms)만큼만 대기
5. 결과 조합 및 반환

**순차 실행 시**: 100 + 180 + 130 = 410ms
**병렬 실행 시**: max(100, 180, 130) = 180ms
**성능 향상**: 약 2.3배!

---

## 성능 비교

### 순차 실행 vs 병렬 실행

#### 순차 실행 (async 없이)

```kotlin
suspend fun getDataSequential(): Result {
    val user = userService.getUser()        // 100ms
    val orders = orderService.getOrders()   // 200ms
    val stats = orderService.getStats()     // 200ms
    return Result(user, orders, stats)      // 총 500ms
}
```

#### 병렬 실행 (async 사용)

```kotlin
suspend fun getDataParallel(): Result = coroutineScope {
    val userDeferred = async { userService.getUser() }      // 100ms
    val ordersDeferred = async { orderService.getOrders() } // 200ms
    val statsDeferred = async { orderService.getStats() }   // 200ms
    
    Result(
        userDeferred.await(),
        ordersDeferred.await(),
        statsDeferred.await()
    ) // 총 200ms (가장 긴 작업)
}
```

### 실제 측정 방법

프로젝트를 실행하고 다음 엔드포인트를 호출해보세요:

```bash
# 병렬 처리 예제
curl http://localhost:8080/api/users/1/details

# 대시보드 병렬 처리
curl http://localhost:8080/api/dashboard
```

로그에서 실행 시간을 확인할 수 있습니다:
```
[Dashboard] ✅ Completed in 203ms
[Dashboard] 💡 병렬 실행으로 성능 최적화! (순차 실행 대비 약 2.71배 빠름)
```

---

## 베스트 프랙티스

### ✅ DO: 해야 할 것

1. **독립적인 작업은 병렬로 실행**
   ```kotlin
   suspend fun getData() = coroutineScope {
       val data1 = async { fetchData1() }
       val data2 = async { fetchData2() }
       combine(data1.await(), data2.await())
   }
   ```

2. **구조화된 동시성 사용**
   ```kotlin
   suspend fun doWork() = coroutineScope {
       // 이 스코프가 끝나기 전에 모든 작업 완료 보장
   }
   ```

3. **적절한 에러 처리**
   ```kotlin
   try {
       val result = async { riskyOperation() }
       result.await()
   } catch (e: Exception) {
       handleError(e)
   }
   ```

### ❌ DON'T: 하지 말아야 할 것

1. **Thread.sleep() 사용하지 않기**
   ```kotlin
   // ❌ 나쁜 예
   suspend fun bad() {
       Thread.sleep(1000) // 스레드 블로킹!
   }
   
   // ✅ 좋은 예
   suspend fun good() {
       delay(1000) // 코루틴만 일시 중단
   }
   ```

2. **GlobalScope 사용 피하기**
   ```kotlin
   // ❌ 나쁜 예
   GlobalScope.launch {
       // 생명주기 관리 어려움
   }
   
   // ✅ 좋은 예
   coroutineScope {
       launch {
           // 구조화된 동시성
       }
   }
   ```

3. **의존 관계가 있는 작업을 병렬로 실행하지 않기**
   ```kotlin
   // ❌ 나쁜 예
   coroutineScope {
       val userDeferred = async { createUser() }
       val notificationDeferred = async { 
           sendNotification(userDeferred.await().id) // 의존 관계!
       }
   }
   
   // ✅ 좋은 예
   val user = createUser()
   val notification = sendNotification(user.id)
   ```

---

## 추가 학습 자료

### 공식 문서

- [Kotlin Coroutines 공식 가이드](https://kotlinlang.org/docs/coroutines-guide.html)
- [Ktor 공식 문서](https://ktor.io/docs/)
- [kotlinx.coroutines API 문서](https://kotlinlang.org/api/kotlinx.coroutines/)

### 추천 학습 경로

1. **기초**: suspend 함수와 delay 이해
2. **중급**: async/await와 병렬 처리
3. **고급**: 구조화된 동시성과 취소
4. **실전**: Flow와 채널

### 실습 제안

1. **로그 분석**: 애플리케이션을 실행하고 로그를 통해 코루틴 실행 흐름 추적
2. **성능 측정**: 순차 실행과 병렬 실행의 시간 차이 측정
3. **코드 수정**: delay 시간을 변경하여 성능 변화 관찰
4. **새로운 엔드포인트 추가**: 학습한 패턴을 적용하여 새로운 기능 구현

---

## 🎯 학습 체크리스트

- [ ] suspend 함수의 개념 이해
- [ ] delay와 Thread.sleep의 차이 이해
- [ ] async/await를 사용한 병렬 실행 이해
- [ ] coroutineScope의 역할 이해
- [ ] 순차 실행과 병렬 실행의 차이 이해
- [ ] 레이어 간 코루틴 전파 방식 이해
- [ ] Ktor의 코루틴 통합 방식 이해
- [ ] 실제 프로젝트에서 코루틴 적용 가능

---

**축하합니다! 🎉**

이 가이드를 통해 Kotlin 코루틴의 핵심 개념을 학습하셨습니다. 
이제 실제 프로젝트에서 코루틴을 효과적으로 활용할 수 있습니다!
