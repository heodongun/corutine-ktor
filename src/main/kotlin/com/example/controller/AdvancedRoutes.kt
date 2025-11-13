package com.example.controller

import com.example.application.AdvancedDashboard
import com.example.domain.model.Order
import com.example.domain.model.OrderStatus
import com.example.infrastructure.EventBus
import com.example.service.AdvancedOrderService
import com.example.service.AdvancedUserService
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList

/**
 * 고급 코루틴 패턴 시연용 라우트
 *
 * 학습 포인트:
 * - 실제 API 엔드포인트에서 고급 패턴 적용
 * - Flow를 HTTP 응답으로 변환
 * - 비동기 작업의 REST API 설계
 */
fun Route.advancedRoutes(
    advancedUserService: AdvancedUserService,
    advancedOrderService: AdvancedOrderService,
    advancedDashboard: AdvancedDashboard
) {
    route("/api/v2") {
        /**
         * 사용자 조회 with Retry
         * GET /api/v2/users/{id}/retry
         */
        get("/users/{id}/retry") {
            val id = call.parameters["id"]?.toLongOrNull()
                ?: return@get call.respond(HttpStatusCode.BadRequest, "Invalid user ID")

            try {
                val user = advancedUserService.getUserWithRetry(id)
                call.respond(HttpStatusCode.OK, user)
            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError, mapOf("error" to e.message))
            }
        }

        /**
         * 캐싱된 사용자 조회
         * GET /api/v2/users/{id}/cached
         */
        get("/users/{id}/cached") {
            val id = call.parameters["id"]?.toLongOrNull()
                ?: return@get call.respond(HttpStatusCode.BadRequest, "Invalid user ID")

            try {
                val user = advancedUserService.getUserCached(id)
                call.respond(
                    HttpStatusCode.OK,
                    mapOf(
                        "user" to user,
                        "cacheSize" to advancedUserService.getCacheSize()
                    )
                )
            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError, mapOf("error" to e.message))
            }
        }

        /**
         * 배치 사용자 조회
         * GET /api/v2/users/batch?ids=1,2,3
         */
        get("/users/batch") {
            val idsParam = call.request.queryParameters["ids"]
                ?: return@get call.respond(HttpStatusCode.BadRequest, "Missing ids parameter")

            val ids = idsParam.split(",").mapNotNull { it.toLongOrNull() }

            if (ids.isEmpty()) {
                return@get call.respond(HttpStatusCode.BadRequest, "Invalid ids format")
            }

            val results = advancedUserService.getUsersBatch(ids)

            val response = mapOf(
                "total" to results.size,
                "successful" to results.count { it.isSuccess },
                "failed" to results.count { it.isFailure },
                "users" to results.mapNotNull { it.getOrNull() }
            )

            call.respond(HttpStatusCode.OK, response)
        }

        /**
         * 사용자 스트림 (첫 N개)
         * GET /api/v2/users/stream?limit=10
         */
        get("/users/stream") {
            val limit = call.request.queryParameters["limit"]?.toIntOrNull() ?: 10

            try {
                val users = advancedUserService.streamAllUsers()
                    .take(limit)
                    .toList()

                call.respond(
                    HttpStatusCode.OK,
                    mapOf(
                        "count" to users.size,
                        "users" to users
                    )
                )
            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError, mapOf("error" to e.message))
            }
        }

        /**
         * 주문 분석 스트림
         * GET /api/v2/orders/analysis?limit=20
         */
        get("/orders/analysis") {
            val limit = call.request.queryParameters["limit"]?.toIntOrNull() ?: 20

            try {
                val analyses = advancedOrderService.streamOrdersWithAnalysis()
                    .take(limit)
                    .toList()

                call.respond(
                    HttpStatusCode.OK,
                    mapOf(
                        "count" to analyses.size,
                        "analyses" to analyses
                    )
                )
            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError, mapOf("error" to e.message))
            }
        }

        /**
         * 주문 제출 (비동기 처리)
         * POST /api/v2/orders/submit
         */
        post("/orders/submit") {
            try {
                // 시뮬레이션: 실제로는 request body에서 파싱
                val order = Order(
                    id = 0,
                    userId = 1L,
                    productName = "Test Product",
                    amount = 1000.0,
                    status = OrderStatus.PENDING
                )

                advancedOrderService.submitOrder(order)

                call.respond(
                    HttpStatusCode.Accepted,
                    mapOf(
                        "message" to "Order submitted for processing",
                        "orderId" to order.id
                    )
                )
            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError, mapOf("error" to e.message))
            }
        }

        /**
         * 복잡한 대시보드 데이터
         * GET /api/v2/dashboard/complex
         */
        get("/dashboard/complex") {
            try {
                val data = advancedDashboard.getComplexDashboardData()

                call.respond(
                    HttpStatusCode.OK,
                    mapOf(
                        "users" to data.users.size,
                        "orders" to data.orderAnalyses.size,
                        "statistics" to data.statistics,
                        "metrics" to data.metrics,
                        "healthCheck" to data.healthCheck,
                        "fetchDuration" to "${data.fetchDuration}ms"
                    )
                )
            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError, mapOf("error" to e.message))
            }
        }

        /**
         * 시스템 메트릭스 조회
         * GET /api/v2/system/metrics
         */
        get("/system/metrics") {
            val metrics = EventBus.getCurrentMetrics()
            call.respond(HttpStatusCode.OK, metrics)
        }

        /**
         * 주문 처리 상태 조회
         * GET /api/v2/system/order-state
         */
        get("/system/order-state") {
            val state = EventBus.getCurrentOrderState()
            call.respond(HttpStatusCode.OK, mapOf("state" to state))
        }

        /**
         * 캐시 초기화
         * DELETE /api/v2/cache
         */
        delete("/cache") {
            advancedUserService.clearCache()
            call.respond(HttpStatusCode.OK, mapOf("message" to "Cache cleared"))
        }

        /**
         * 타임아웃 테스트
         * GET /api/v2/users/{id}/timeout?seconds=5
         */
        get("/users/{id}/timeout") {
            val id = call.parameters["id"]?.toLongOrNull()
                ?: return@get call.respond(HttpStatusCode.BadRequest, "Invalid user ID")

            val timeoutSeconds = call.request.queryParameters["seconds"]?.toLongOrNull() ?: 5

            try {
                val user = advancedUserService.getUserWithTimeout(id, timeoutSeconds)
                call.respond(HttpStatusCode.OK, user)
            } catch (e: kotlinx.coroutines.TimeoutCancellationException) {
                call.respond(HttpStatusCode.RequestTimeout, mapOf("error" to "Request timed out"))
            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError, mapOf("error" to e.message))
            }
        }
    }
}
