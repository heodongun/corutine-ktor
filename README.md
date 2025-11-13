# 🎓 Ktor Coroutine Learning Project

Kotlin 코루틴과 Ktor 프레임워크를 학습하기 위한 실습 프로젝트입니다.

## 📚 프로젝트 소개

이 프로젝트는 **레이어드 아키텍처**를 통해 코루틴이 각 레이어에서 어떻게 사용되고 조합되는지 보여줍니다.

### 핵심 학습 목표

- ✅ **suspend 함수**: 일시 중단 가능한 함수의 정의와 사용
- ✅ **async/await**: 병렬 실행을 통한 성능 최적화
- ✅ **coroutineScope**: 구조화된 동시성
- ✅ **delay**: 비동기 작업 시뮬레이션
- ✅ **레이어 간 코루틴 전파**: suspend 함수가 레이어를 통과하는 방식
- ✅ **Ktor DSL**: 타입 안전한 라우팅 정의

### 아키텍처

```
Controller (HTTP Routing)
    ↓ calls
Application (Service Composition)
    ↓ calls multiple
Services (Business Logic)
    ↓ calls
Repository (Data Access with delay simulation)
```

## 🚀 빠른 시작

### 요구사항

- JDK 17 이상
- Gradle 8.x

### 실행 방법

1. **프로젝트 클론 또는 다운로드**

2. **애플리케이션 실행**
   ```bash
   ./gradlew run
   ```

3. **브라우저에서 확인**
   ```
   http://localhost:8080
   ```


## 📡 API 엔드포인트

### User Endpoints

| Method | Endpoint | Description | 코루틴 패턴 |
|--------|----------|-------------|------------|
| GET | `/api/users` | 모든 사용자 조회 | suspend 함수 |
| POST | `/api/users` | 사용자 생성 + 환영 알림 | 순차 실행 |
| GET | `/api/users/{id}` | 특정 사용자 조회 | suspend 함수 |
| GET | `/api/users/{id}/details` | 사용자 상세 정보 | **병렬 실행** 🚀 |
| PUT | `/api/users/{id}` | 사용자 수정 | suspend 함수 |
| DELETE | `/api/users/{id}` | 사용자 삭제 | suspend 함수 |

### Order Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/orders` | 모든 주문 조회 |
| POST | `/api/orders` | 주문 생성 |
| GET | `/api/orders/{id}` | 특정 주문 조회 |
| GET | `/api/orders/user/{userId}` | 사용자별 주문 조회 |
| PUT | `/api/orders/{id}/status` | 주문 상태 변경 |

### Dashboard Endpoints

| Method | Endpoint | Description | 코루틴 패턴 |
|--------|----------|-------------|------------|
| GET | `/api/dashboard` | 대시보드 데이터 | **병렬 실행** 🚀 |
| GET | `/api/dashboard/stats` | 통계 데이터 | suspend 함수 |

## 💡 사용 예제

### 1. 사용자 생성 (순차 실행 예제)

```bash
curl -X POST http://localhost:8080/api/users \
  -H "Content-Type: application/json" \
  -d '{"name": "John Doe", "email": "john@example.com"}'
```

**실행 흐름**:
1. 사용자 생성 (200ms delay)
2. 환영 알림 발송 (150ms delay)
3. 총 소요 시간: 약 350ms

### 2. 사용자 상세 정보 조회 (병렬 실행 예제)

```bash
curl http://localhost:8080/api/users/1/details
```

**실행 흐름**:
1. 사용자 정보 조회 (100ms)
2. 주문 목록 조회 (180ms)
3. 알림 목록 조회 (130ms)
4. **병렬 실행으로 총 소요 시간: 약 180ms** (순차 실행 시 410ms)

### 3. 대시보드 데이터 조회 (병렬 실행 예제)

```bash
curl http://localhost:8080/api/dashboard
```

**실행 흐름**:
1. 모든 사용자 조회 (150ms)
2. 모든 주문 조회 (200ms)
3. 통계 계산 (200ms)
4. **병렬 실행으로 총 소요 시간: 약 200ms** (순차 실행 시 550ms)

## 📂 프로젝트 구조

```
src/main/kotlin/com/example/
├── Application.kt                      # 애플리케이션 진입점
├── domain/
│   ├── model/                         # 도메인 모델
│   │   ├── User.kt
│   │   ├── Order.kt
│   │   ├── Notification.kt
│   │   └── AppModels.kt
│   └── exception/                     # 예외 클래스
│       └── AppException.kt
├── repository/                        # Repository 레이어 (delay 시뮬레이션)
│   ├── UserRepository.kt
│   ├── UserRepositoryImpl.kt
│   ├── OrderRepository.kt
│   ├── OrderRepositoryImpl.kt
│   ├── NotificationRepository.kt
│   ├── NotificationRepositoryImpl.kt
│   └── DataInitializer.kt
├── service/                           # Service 레이어 (비즈니스 로직)
│   ├── UserService.kt
│   ├── UserServiceImpl.kt
│   ├── OrderService.kt
│   ├── OrderServiceImpl.kt
│   ├── NotificationService.kt
│   └── NotificationServiceImpl.kt
├── application/                       # Application 레이어 (서비스 조합, 병렬 처리)
│   ├── UserApplication.kt
│   └── DashboardApplication.kt
├── controller/                        # Controller 레이어 (HTTP 라우팅)
│   ├── UserRoutes.kt
│   ├── OrderRoutes.kt
│   └── DashboardRoutes.kt
├── dto/                              # Request/Response DTO
│   ├── UserDto.kt
│   └── OrderDto.kt
└── plugins/                          # Ktor 플러그인 설정
    ├── Routing.kt
    ├── Serialization.kt
    └── StatusPages.kt
```

## 🎯 학습 포인트

### 1. Repository 레이어: delay를 사용한 비동기 시뮬레이션

```kotlin
override suspend fun findAll(): List<User> {
    delay(150) // 데이터베이스 조회 시뮬레이션
    return users.values.toList()
}
```

### 2. Service 레이어: suspend 함수 체이닝

```kotlin
override suspend fun getUserById(id: Long): User {
    return userRepository.findById(id) 
        ?: throw UserNotFoundException(id)
}
```

### 3. Application 레이어: async/await를 사용한 병렬 처리

```kotlin
suspend fun getUserWithDetails(userId: Long) = coroutineScope {
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

### 4. Controller 레이어: Ktor DSL과 코루틴 통합

```kotlin
fun Route.userRoutes(userApplication: UserApplication) {
    route("/users") {
        get("/{id}/details") {
            val id = call.parameters["id"]?.toLongOrNull() ?: return@get ...
            val userDetails = userApplication.getUserWithDetails(id)
            call.respond(userDetails)
        }
    }
}
```

## 📖 상세 학습 가이드

코루틴의 상세한 사용법과 패턴은 **[COROUTINE_GUIDE.md](COROUTINE_GUIDE.md)**를 참고하세요.

이 가이드에는 다음 내용이 포함되어 있습니다:
- 코루틴 기본 개념
- 레이어별 코루틴 사용 방법
- 코루틴 패턴 상세 설명
- 실제 코드 예제와 실행 흐름
- 성능 비교 (순차 vs 병렬)
- 베스트 프랙티스
- 추가 학습 자료

## 🔍 로그를 통한 학습

애플리케이션을 실행하면 코루틴의 실행 흐름을 로그로 확인할 수 있습니다:

```
[Application] 🚀 Starting getUserWithDetails for userId: 1
[Application] 📊 Launching parallel coroutines for user, orders, and notifications
[Application] 👤 Fetching user data...
[Application] 📦 Fetching orders data...
[Application] 📧 Fetching notifications data...
[Repository] Finding user by id: 1 - simulating DB delay...
[Repository] Finding orders for user: 1 - simulating DB delay...
[Repository] Finding recent notifications for user: 1 - simulating DB delay...
[Application] ⏳ Waiting for all parallel operations to complete...
[Application] ✅ Completed getUserWithDetails successfully
```

## 🛠️ 기술 스택

- **Kotlin** 1.9.21
- **Ktor** 2.3.7
- **Kotlinx Coroutines** 1.7.3
- **Kotlinx Serialization** (JSON)
- **Logback** (로깅)
- **Gradle** (빌드 도구)

## 📝 라이선스

이 프로젝트는 학습 목적으로 만들어졌습니다.

## 🤝 기여

학습 프로젝트이므로 자유롭게 수정하고 실험해보세요!

---

**Happy Learning! 🎉**

코루틴의 강력함을 직접 경험해보세요!
