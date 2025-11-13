package com.example.service

import com.example.domain.model.User

interface UserService {
    suspend fun getAllUsers(): List<User>
    suspend fun getUserById(id: Long): User
    suspend fun createUser(name: String, email: String): User
    suspend fun updateUser(id: Long, name: String, email: String): User
    suspend fun deleteUser(id: Long): Boolean
}
