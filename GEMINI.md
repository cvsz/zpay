# GEMINI.md — ZWallet Project Instructions

## Project
ZWallet (ZPay) — Enterprise multi-chain Web3 wallet for Android.

## Build & Test
- `./gradlew :app:assembleDebug --no-daemon`
- `./gradlew :app:testDebugUnitTest --no-daemon`
- `./gradlew :app:lint --no-daemon`

## Implementation Summary
- BIP-39/32/44 HD wallet derivation
- SLIP-0010 ed25519 for Solana
- EVM (EIP-55), Bitcoin P2WPKH bech32, Solana base58 addresses
- BIP-143 transaction signing with canonical low-S DER
- EVM/Bitcoin/Solana network RPC clients
- CoinGecko market price service
- Hardware-backed AES-GCM key storage (Android Keystore, StrongBox)
- PBKDF2-HMAC-SHA256 PIN authentication with biometric and lockout
- BigInteger exact financial arithmetic in minor units
- Room v3 with non-destructive migrations

## Standards
- Kotlin only; no floating-point for money
- HTTPS-only network clients; no hardcoded secrets
- Context-bound GCM AAD; hardware keystore preferred
