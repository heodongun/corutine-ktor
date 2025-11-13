package com.example.repository

import com.example.domain.model.User

interface UserRepository {
    suspend fun findAll(): List<User>
    suspend fun findById(id: Long): User?
    suspend fun create(user: User): User
    suspend fun update(id: Long, name: String, email: String): User?
    suspend fun delete(id: Long): Boolean
}
