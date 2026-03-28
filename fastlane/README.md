fastlane documentation
----

# Installation

Make sure you have the latest version of the Xcode command line tools installed:

```sh
xcode-select --install
```

For _fastlane_ installation instructions, see [Installing _fastlane_](https://docs.fastlane.tools/#installing-fastlane)

# Available Actions

## Android

### android lint

```sh
[bundle exec] fastlane android lint
```

Run ktlint and detekt (used in CI)

### android test

```sh
[bundle exec] fastlane android test
```

Run Unit Tests (used in CI)

### android build_dev

```sh
[bundle exec] fastlane android build_dev
```

Build the DEV environment APK (used in CD)

### android build_prod

```sh
[bundle exec] fastlane android build_prod
```

Build the PROD environment APK and AAB (used in CD)

----

This README.md is auto-generated and will be re-generated every time [_fastlane_](https://fastlane.tools) is run.

More information about _fastlane_ can be found on [fastlane.tools](https://fastlane.tools).

The documentation of _fastlane_ can be found on [docs.fastlane.tools](https://docs.fastlane.tools).
