package com.appvoyager.cloudphotos.domain.auth.usecase

import com.appvoyager.cloudphotos.domain.auth.model.AuthError
import com.appvoyager.cloudphotos.domain.auth.model.AuthResult
import com.appvoyager.cloudphotos.domain.auth.repository.AuthRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class DeleteUserUseCaseTest {

    private val repository = mockk<AuthRepository>()
    private lateinit var useCase: DeleteUserUseCase

    @BeforeEach
    fun setUp() {
        useCase = DeleteUserUseCase(repository)
    }

    @Test
    fun `invoke returns Success when repository succeeds`() = runTest {
        // Arrange
        val expected = AuthResult.Success(Unit)
        coEvery { repository.deleteUser() } returns expected

        // Act
        // Normal: repository success is returned unchanged
        val result = useCase()

        // Assert
        assertEquals(expected, result)
    }

    @Test
    fun `invoke calls deleteUser when invoked`() = runTest {
        // Arrange
        coEvery { repository.deleteUser() } returns AuthResult.Success(Unit)

        // Act
        // Interaction: use case delegates once to repository
        useCase()

        // Assert
        coVerify(exactly = 1) { repository.deleteUser() }
    }

    @Test
    fun `invoke returns Error when repository fails`() = runTest {
        // Arrange
        val expected = AuthResult.Error(AuthError.Unknown())
        coEvery { repository.deleteUser() } returns expected

        // Act
        // Error: repository error is returned unchanged
        val result = useCase()

        // Assert
        assertEquals(expected, result)
    }
}
