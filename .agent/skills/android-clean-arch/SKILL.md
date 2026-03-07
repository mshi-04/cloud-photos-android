---
name: android-clean-arch
description: "Use when creating or modifying UseCase, Repository interface, RepositoryImpl, or DataSource in an Android Clean Architecture project"
---

# Clean Architecture Guidelines

## Package Structure
```
com.appvoyager.cloudphotos
├── domain
│   └── {feature}
│       ├── usecase/        # UseCases
│       ├── repository/     # Repository interfaces
│       ├── datasource/     # DataSource interfaces
│       ├── model/          # Domain models
│       └── request/        # Request data classes
└── data
    └── {feature}
        ├── repository/     # RepositoryImpl
        └── datasource/     # DataSourceImpl
```

## Rules
1. Domain層: Pure Kotlin のみ。Android依存 (Context等) 禁止
2. UseCase: 単一責務。`suspend operator fun invoke()` を使う
3. Repository interface: domain層に置く
4. RepositoryImpl: dataSourceに委譲するだけ。ビジネスロジックを書かない
5. 戻り値: `AuthResult<T>` のようなドメイン固有のResult型を使う
6. DI: `@Inject constructor` を使う

## UseCase Template
```kotlin
package com.appvoyager.cloudphotos.domain.{feature}.usecase

import javax.inject.Inject

class HogeUseCase @Inject constructor(
    private val repository: HogeRepository
) {
    suspend operator fun invoke(request: HogeRequest): HogeResult =
        repository.hoge(request)
}
```

## Repository Interface Template
```kotlin
package com.appvoyager.cloudphotos.domain.{feature}.repository

interface HogeRepository {
    suspend fun hoge(request: HogeRequest): HogeResult
}
```

## RepositoryImpl Template
```kotlin
package com.appvoyager.cloudphotos.data.{feature}.repository

import javax.inject.Inject

class HogeRepositoryImpl @Inject constructor(
    private val dataSource: HogeDataSource
) : HogeRepository {
    override suspend fun hoge(request: HogeRequest): HogeResult =
        dataSource.hoge(request)
}
```