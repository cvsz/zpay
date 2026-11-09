# CLAUDE.md — ZWallet Project Instructions

## Project
ZWallet (ZPay) — Enterprise-grade multi-chain Web3 wallet for Android.

## Build & Test
- Build: `./gradlew :app:assembleDebug --no-daemon`
- Test: `./gradlew :app:testDebugUnitTest --no-daemon`
- Lint: `./gradlew :app:lint --no-daemon`
- Clean build: `./gradlew clean :app:assembleDebug --no-daemon`

## Tech Stack
- Kotlin, Jetpack Compose, Room, Coroutines, Flow
- AGP 9.1.1, Gradle 9.3.1, JDK 17, Kotlin 2.2.10
- minSdk 24, targetSdk 36, compileSdk 36.1
- web3j 4.12.2, bouncycastle bcprov-jdk18on 1.78.1

## Key Directories
- `app/src/main/java/com/example/crypto/` — BIP-39/32/44, addresses, signing
- `app/src/main/java/com/example/network/` — EVM/BTC/SOL RPC clients, market data
- `app/src/main/java/com/example/data/` — Room DB, repository, vault, auth
- `app/src/main/java/com/example/ui/screens/` — Compose UI screens
- `app/src/test/java/com/example/` — Unit tests

## Security Rules
- Never hardcode secrets; use .env via Secrets Gradle Plugin
- BigInteger for all financial amounts (no floating-point)
- All amounts in minor units
- HTTPS-only network clients
- Context-bound ciphertext (chain\|address as GCM AAD)
- Hardware-backed AES-GCM key storage (StrongBox preferred)

## Response Guidelines
- Concise, technical, direct
- File references: `file_path:line_number`
- Short answers preferred
- Do not commit unless explicitly requested
