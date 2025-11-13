package com.example.service

import com.example.domain.exception.InvalidRequestException
import com.example.domain.exception.UserNotFoundException
import com.example.domain.model.User
import com.example.repository.UserRepository
import org.slf4j.LoggerFactory

class UserServiceImpl(
    private val userRepository: UserRepository
) : UserService {
    private val logger = LoggerFactory.getLogger(UserServiceImpl::class.java)

    override suspend fun getAllUsers(): List<User> {
        logger.debug("[Service] Getting all users")
        return userRepository.findAll()
    }

    override suspend fun getUserById(id: Long): User {
        logger.debug("[Service] Getting user by id: $id")
        return userRepository.findById(id) 
            ?: throw UserNotFoundException(id)
    }

    override suspend fun createUser(name: String, email: String): User {
        logger.debug("[Service] Creating user: $name")
        
        // 비즈니스 로직: 이메일 검증
        if (!email.contains("@")) {
            throw InvalidRequestException("Invalid email format: $email")
        }
        
        val user = User(0, name, email)
        return userRepository.create(user)
    }

    override suspend fun updateUser(id: Long, name: String, email: String): User {
        logger.debug("[Service] Updating user: $id")
        
        // 비즈니스 로직: 이메일 검증
        if (!email.contains("@")) {
            throw InvalidRequestException("Invalid email format: $email")
        }
        
        return userRepository.update(id, name, email)
            ?: throw UserNotFoundException(id)
    }

    override suspend fun deleteUser(id: Long): Boolean {
        logger.debug("[Service] Deleting user: $id")
        val deleted = userRepository.delete(id)
        if (!deleted) {
            throw UserNotFoundException(id)
        }
        return true
    }
}
