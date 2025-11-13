package com.example.plugins

import com.example.domain.exception.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.response.*
import org.slf4j.LoggerFactory

fun Application.configureStatusPages() {
    val logger = LoggerFactory.getLogger("StatusPages")
    
    install(StatusPages) {
        exception<UserNotFoundException> { call, cause ->
            logger.warn("User not found: ${cause.message}")
            call.respond(HttpStatusCode.NotFound, mapOf("error" to cause.message))
        }
        
        exception<OrderNotFoundException> { call, cause ->
            logger.warn("Order not found: ${cause.message}")
            call.respond(HttpStatusCode.NotFound, mapOf("error" to cause.message))
        }
        
        exception<InvalidRequestException> { call, cause ->
            logger.warn("Invalid request: ${cause.message}")
            call.respond(HttpStatusCode.BadRequest, mapOf("error" to cause.message))
        }
        
        exception<ServiceException> { call, cause ->
            logger.error("Service error: ${cause.message}", cause)
            call.respond(HttpStatusCode.InternalServerError, mapOf("error" to "Service error occurred"))
        }
        
        exception<Exception> { call, cause ->
            logger.error("Unexpected error: ${cause.message}", cause)
            call.respond(HttpStatusCode.InternalServerError, mapOf("error" to "Internal server error"))
        }
    }
}
