# Fixture

テスト用のデータ生成は、モジュールごとの `testutil` package に top-level 関数として置きます。
class にも object にもしません。現在あるファイルは 3 つです。

- `feature/auth/domain/src/test/kotlin/com/appvoyager/cloudphotos/domain/auth/testutil/AuthDomainFixtures.kt`
- `feature/auth/data/src/test/kotlin/com/appvoyager/cloudphotos/data/auth/testutil/AuthDataFixtures.kt`
- `feature/media/data/src/test/kotlin/com/appvoyager/cloudphotos/data/media/testutil/UploadDataFixtures.kt`

ファイル名は `対象領域 + Fixtures.kt` です。

## domain 層の命名: suffix なし、引数は value object

`AuthDomainFixtures.kt` では、value object 単体を作る関数に `valid` prefix を付けます
（`validEmail()`、`validPassword()`、`validCode()`）。
request と model には prefix も suffix も付けません（`signInRequest()`、`signUpRequest()`、
`confirmSignUpRequest()`、`authUser()`）。

組み立て系の関数は引数を value object 型で受け、default 値として value object の fixture 関数を呼びます。
`signInRequest()` は `email: Email = validEmail()` と `password: Password = validPassword()` を取ります。

state のバリエーションは引数ではなく関数名で分けます。`signedInSession()` と `guestSession()` が別関数になっており、
`AuthState` を引数で渡す形にはしていません。

## data 層の命名: `Fixture` suffix、引数は生の String

`AuthDataFixtures.kt` と `UploadDataFixtures.kt` では、関数名が `signUpRequestFixture()`、`signInRequestFixture()`、
`confirmSignUpRequestFixture()`、`authUserFixture()`、`authSessionFixture()`、`uploadMediaRequestFixture()`、
`cloudStoragePathFixture()` のように `Fixture` suffix を持ちます。

引数は value object ではなく生の `String` で受け、関数の中で `Email.of(email)` のように value object へ変換します。
data 層のテストは provider 由来の生の値を扱うことが多いため、呼び出し側で `.of()` を書かずに済む形にしてあります。

default 値も層で分けてあります。domain 側の email は `user@example.com`、data 側は `fixture@example.com` です。

同じ型の fixture を domain 側と data 側の両方に置いているのは、layer をまたぐテスト依存を作らないためです。
data のテストから domain の `testutil` を import しません。

## テストクラス内に置くもの

1 つのテストクラスでしか使わないデータは `testutil` に出さず、そのクラスの private 関数として file 末尾に置きます。
Worker テストの `createUploadRecord()` と `createPendingRecord()`、Repository テストの `createUploadRecord()`、
DAO テストの `createRecord()` がこれにあたります。

複数の同種テストが同じ形を必要とした時点で `testutil` へ移します。1 箇所でしか使わないものは移しません。

Mapper テストのように全テストが同じ入力を使う場合は、関数ではなく private val として class に持たせます。
複数テストで共有する list を持つ場合は companion object の val に置きます。

## 値の決め方

timestamp は固定値を使い、現在時刻を呼びません。`1700000000000L`（Mapper テストでは桁区切りの
`1_700_000_000_000L`）が使われています。
mediaId は `media-1` や `external_primary_123`、cloudStoragePath は `private/identity123/uuid.jpg` のように、
production で実際に現れる形を模した文字列にします。

`src/androidTest` には `testutil` package がありません。InstrumentedTest 側の生成関数はテストクラス内に置きます。
