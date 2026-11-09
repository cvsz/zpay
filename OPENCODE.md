# OPENCODE.md — ZWallet Project Instructions

## Overview
ZWallet (ZPay) — Production-ready multi-chain Web3 wallet for Android.

## Quick Commands
- Build: `./gradlew :app:assembleDebug --no-daemon`
- Test: `./gradlew :app:testDebugUnitTest --no-daemon`
- Lint: `./gradlew :app:lint --no-daemon`

## Project Structure
- `app/src/main/java/com/example/crypto/` — WalletCrypto, Base58, Bech32, BitcoinTx, SolanaTx
- `app/src/main/java/com/example/network/` — EvmRpcClient, BitcoinApiClient, SolanaRpcClient, MarketPriceService
- `app/src/main/java/com/example/data/` — WalletRepository, WalletDao, WalletDatabase, SecureKeyVault, VaultAuthManager, WalletViewModel
- `app/src/test/java/com/example/` — WalletCryptoTest, BitcoinTxTest, VaultAuthManagerTest, WalletRepositoryTest

## Standards
- Kotlin only; BigInteger for money; minor units for amounts
- HTTPS-only; no hardcoded secrets
- Context-bound GCM AAD; hardware-backed keystore
- PBKDF2-HMAC-SHA256 PIN; BiometricPrompt; escalating lockout
