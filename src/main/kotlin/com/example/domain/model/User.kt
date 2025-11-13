package com.example.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class User(
    val id: Long,
    val name: String,
    val email: String,
    val createdAt: Long = System.currentTimeMillis()
)
