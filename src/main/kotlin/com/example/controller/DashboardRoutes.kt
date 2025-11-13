package com.example.controller

import com.example.application.DashboardApplication
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.slf4j.LoggerFactory

/**
 * Dashboard Routes: 대시보드 관련 HTTP 엔드포인트
 * 
 * 코루틴 학습 포인트:
 * - 병렬 처리의 실제 성능 이점을 확인할 수 있는 엔드포인트
 * - 로그를 통해 코루틴의 실행 흐름 확인 가능
 */
fun Route.dashboardRoutes(dashboardApplication: DashboardApplication) {
    val logger = LoggerFactory.getLogger("DashboardRoutes")

    route("/dashboard") {
        // GET /api/dashboard - 대시보드 데이터 (병렬 처리 예제)
        get {
            logger.info("[Controller] GET /api/dashboard - 병렬 처리 예제 시작")
            val dashboardData = dashboardApplication.getDashboardData()
            call.respond(dashboardData)
        }

        // GET /api/dashboard/stats - 통계 데이터
        get("/stats") {
            logger.info("[Controller] GET /api/dashboard/stats")
            val stats = dashboardApplication.getStatistics()
            call.respond(stats)
        }
    }
}
