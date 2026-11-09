# AGY.md — ZWallet Project Instructions

## Project
ZWallet (ZPay) — Production-ready multi-chain Web3 wallet for Android.

## Commands
- Build: `./gradlew :app:assembleDebug --no-daemon`
- Test: `./gradlew :app:testDebugUnitTest --no-daemon`
- Lint: `./gradlew :app:lint --no-daemon`

## Core Modules
- Crypto: BIP-39/32/44, SLIP-0010, EVM/BTC/SOL addresses, signing
- Network: EVM/BTC/SOL RPC clients, CoinGecko market data
- Data: Room DB, repository, vault, auth, ViewModel
- UI: Jetpack Compose screens

## Security
- Android Keystore AES-GCM (StrongBox)
- Context-bound GCM AAD
- PBKDF2-HMAC-SHA256 PIN (6 digits), biometric, lockout
- No hardcoded secrets; HTTPS-only

## Money Handling
- BigInteger for all financial amounts
- Minor units (wei, satoshi, lamport, cents)
- Per-asset decimal scale
