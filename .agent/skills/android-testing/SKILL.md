---
name: android-testing
description: "Use when creating, writing, or modifying unit tests, instrumentation tests, or any test-related code in an Android project"
---

# Android Testing Guidelines

## Frameworks
- Unit Tests: JUnit 5 + MockK
- Coroutines: runTest (kotlinx-coroutines-test)
- Assertions: assertEquals, assertFalse, assertNull (kotlin.test)

## Rules
1. Test対象: UseCase と ViewModel を優先的にテストする
2. MockK: `mockk<>()` で直接生成、@ExtendWithは使わない
3. Dispatcher: StandardTestDispatcher + setMain/resetMain (@BeforeEach/@AfterEach)
4. テスト名: バッククォート + 英語 (`initial state has empty email`)
5. AAA構造を守る (Arrange / Act / Assert)
6. 1テスト1アサーションを原則とする

## ViewModel Test Template
```kotlin
@OptIn(ExperimentalCoroutinesApi::class)
class HogeViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val hogeUseCase = mockk<HogeUseCase>()

    private lateinit var viewModel: HogeViewModel

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        viewModel = HogeViewModel(hogeUseCase)
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state is correct`() {
        // Arrange
        coEvery { hogeUseCase() } returns Result.success(Unit)

        // Act
        viewModel.onIntent(HogeIntent.DoSomething)

        // Assert
        assertEquals(HogeUiState.Success, viewModel.uiState.value)
    }
}
```

## UseCase Test Template
```kotlin
@OptIn(ExperimentalCoroutinesApi::class)
class HogeUseCaseTest {

    private val hogeRepository = mockk<HogeRepository>()

    private lateinit var useCase: HogeUseCase

    @BeforeEach
    fun setUp() {
        useCase = HogeUseCase(hogeRepository)
    }

    @Test
    fun `fetch success returns success result`() = runTest {
        // Arrange
        coEvery { hogeRepository.fetch() } returns Result.success(fakeData)

        // Act
        val result = useCase()

        // Assert
        assertTrue(result.isSuccess)
    }
}
```