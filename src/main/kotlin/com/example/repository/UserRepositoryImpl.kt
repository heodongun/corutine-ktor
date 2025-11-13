package com.example.repository

import com.example.domain.model.User
import kotlinx.coroutines.delay
import org.slf4j.LoggerFactory
import java.util.concurrent.atomic.AtomicLong

class UserRepositoryImpl : UserRepository {
    private val logger = LoggerFactory.getLogger(UserRepositoryImpl::class.java)
    private val users = mutableMapOf<Long, User>()
    private val idCounter = AtomicLong(0)

    override suspend fun findAll(): List<User> {
        logger.debug("[Repository] Finding all users - simulating DB delay...")
        delay(150) // 데이터베이스 조회 시뮬레이션
        logger.debug("[Repository] Found ${users.size} users")
        return users.values.toList()
    }

    override suspend fun findById(id: Long): User? {
        logger.debug("[Repository] Finding user by id: $id - simulating DB delay...")
        delay(100) // 데이터베이스 조회 시뮬레이션
        val user = users[id]
        logger.debug("[Repository] User found: ${user != null}")
        return user
    }

    override suspend fun create(user: User): User {
        logger.debug("[Repository] Creating user: ${user.name} - simulating DB delay...")
        delay(200) // 데이터베이스 삽입 시뮬레이션
        val newUser = user.copy(id = idCounter.incrementAndGet())
        users[newUser.id] = newUser
        logger.debug("[Repository] User created with id: ${newUser.id}")
        return newUser
    }

    override suspend fun update(id: Long, name: String, email: String): User? {
        logger.debug("[Repository] Updating user id: $id - simulating DB delay...")
        delay(180) // 데이터베이스 업데이트 시뮬레이션
        val existingUser = users[id] ?: return null
        val updatedUser = existingUser.copy(name = name, email = email)
        users[id] = updatedUser
        logger.debug("[Repository] User updated: $id")
        return updatedUser
    }

    override suspend fun delete(id: Long): Boolean {
        logger.debug("[Repository] Deleting user id: $id - simulating DB delay...")
        delay(120) // 데이터베이스 삭제 시뮬레이션
        val removed = users.remove(id) != null
        logger.debug("[Repository] User deleted: $removed")
        return removed
    }
}
