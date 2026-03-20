---
name: android-clean-arch
description: "Use when creating or modifying UseCase, Repository interface, RepositoryImpl, or DataSource in an Android Clean Architecture project"
---

# Clean Architecture Guidelines

## Package Structure
```text
com.appvoyager.cloudphotos
├── domain
│   └── {feature}
│       ├── usecase/        # UseCases
│       ├── repository/     # Repository interfaces
│       ├── model/          # Domain models
│       ├── request/        # Request data classes
│       └── valueobject/    # Value objects
└── data
    └── {feature}
        ├── repository/     # RepositoryImpl
        ├── datasource/     # DataSource interfaces + DataSourceImpl
        └── util/           # Error mappers etc.
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

import com.appvoyager.cloudphotos.domain.{feature}.model.HogeResult
import com.appvoyager.cloudphotos.domain.{feature}.repository.HogeRepository
import com.appvoyager.cloudphotos.domain.{feature}.request.HogeRequest
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

import com.appvoyager.cloudphotos.domain.{feature}.model.HogeResult
import com.appvoyager.cloudphotos.domain.{feature}.request.HogeRequest

interface HogeRepository {
    suspend fun hoge(request: HogeRequest): HogeResult
}
```

## RepositoryImpl Template
```kotlin
package com.appvoyager.cloudphotos.data.{feature}.repository

import com.appvoyager.cloudphotos.data.{feature}.datasource.HogeDataSource
import com.appvoyager.cloudphotos.domain.{feature}.model.HogeResult
import com.appvoyager.cloudphotos.domain.{feature}.repository.HogeRepository
import com.appvoyager.cloudphotos.domain.{feature}.request.HogeRequest
import javax.inject.Inject

class HogeRepositoryImpl @Inject constructor(
    private val dataSource: HogeDataSource
) : HogeRepository {
    override suspend fun hoge(request: HogeRequest): HogeResult =
        dataSource.hoge(request)
}
```