package com.example.controller

import com.example.application.UserApplication
import com.example.dto.CreateUserRequest
import com.example.dto.UpdateUserRequest
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.slf4j.LoggerFactory

/**
 * User Routes: 사용자 관련 HTTP 엔드포인트
 * 
 * 코루틴 학습 포인트:
 * - Ktor의 모든 라우트 핸들러는 자동으로 코루틴 컨텍스트에서 실행됨
 * - call.receive(), call.respond() 등은 모두 suspend 함수
 * - Application 레이어의 suspend 함수를 직접 호출 가능
 */
fun Route.userRoutes(userApplication: UserApplication) {
    val logger = LoggerFactory.getLogger("UserRoutes")

    route("/users") {
        // GET /api/users - 모든 사용자 조회
        get {
            logger.info("[Controller] GET /api/users")
            val users = userApplication.getAllUsers()
            call.respond(users)
        }

        // POST /api/users - 사용자 생성 (환영 알림 자동 발송)
        post {
            logger.info("[Controller] POST /api/users")
            val request = call.receive<CreateUserRequest>()
            val result = userApplication.createUserWithWelcome(request.name, request.email)
            call.respond(HttpStatusCode.Created, result)
        }

        // GET /api/users/{id} - 특정 사용자 조회
        get("/{id}") {
            val id = call.parameters["id"]?.toLongOrNull()
                ?: return@get call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Invalid user ID"))
            
            logger.info("[Controller] GET /api/users/$id")
            val user = userApplication.getUserById(id)
            call.respond(user)
        }

        // GET /api/users/{id}/details - 사용자 상세 정보 (주문, 알림 포함)
        get("/{id}/details") {
            val id = call.parameters["id"]?.toLongOrNull()
                ?: return@get call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Invalid user ID"))
            
            logger.info("[Controller] GET /api/users/$id/details")
            val userDetails = userApplication.getUserWithDetails(id)
            call.respond(userDetails)
        }

        // PUT /api/users/{id} - 사용자 수정
        put("/{id}") {
            val id = call.parameters["id"]?.toLongOrNull()
                ?: return@put call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Invalid user ID"))
            
            logger.info("[Controller] PUT /api/users/$id")
            val request = call.receive<UpdateUserRequest>()
            
            if (request.name == null || request.email == null) {
                return@put call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Name and email are required"))
            }
            
            val user = userApplication.updateUser(id, request.name, request.email)
            call.respond(user)
        }

        // DELETE /api/users/{id} - 사용자 삭제
        delete("/{id}") {
            val id = call.parameters["id"]?.toLongOrNull()
                ?: return@delete call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Invalid user ID"))
            
            logger.info("[Controller] DELETE /api/users/$id")
            userApplication.deleteUser(id)
            call.respond(HttpStatusCode.NoContent)
        }
    }
}
