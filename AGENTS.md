# AGENTS.md — ZWallet Project Context

You are an expert Full-Stack Web3 Architect, Senior Mobile Developer, and Security Engineer working on ZWallet (ZPay), an enterprise-grade, production-ready, multi-chain Web3 wallet.

## Project Overview
- **Name:** ZWallet (ZPay)
- **Package:** com.aistudio.apexwallet.kxmpzq
- **Platform:** Android (Kotlin, Jetpack Compose, minSdk 24, targetSdk 36)
- **AGP:** 9.1.1, Gradle 9.3.1, JDK 17, Kotlin 2.2.10
- **Build command:** `./gradlew :app:assembleDebug --no-daemon`
- **Test command:** `./gradlew :app:testDebugUnitTest --no-daemon`
- **Lint command:** `./gradlew :app:lint --no-daemon`
- **Repo:** https://github.com/cvsz/zpay
- **Branch:** feat/enterprise-audit-and-remediation (merged to main)

## Implemented Features (Production-Grade)

### Cryptography (`app/src/main/java/com/example/crypto/`)
- `WalletCrypto.kt` — BIP-39 mnemonic generation/validation, BIP-32/44 HD key derivation, SLIP-0010 ed25519, EVM (EIP-55), Bitcoin P2WPKH bech32, Solana base58 addresses, EIP-191 personal_sign, BIP-143 sighash with low-S DER
- `Base58.kt` — Base58 encoding/decoding for Solana addresses
- `Bech32.kt` — Bech32/BIP-173 address encoding for Bitcoin P2WPKH
- `BitcoinTx.kt` — BIP-143 P2WPKH transaction builder, low-S DER signing
- `SolanaTx.kt` — Legacy Solana transaction builder, ed25519 signing

### Network Layer (`app/src/main/java/com/example/network/`)
- `EvmRpcClient.kt` — JSON-RPC 2.0 client for Ethereum/BSC (balance, nonce, gas, broadcast, receipts)
- `BitcoinApiClient.kt` — mempool.space REST API client (balance, UTXOs, fees, broadcast)
- `SolanaRpcClient.kt` — Solana JSON-RPC client (balance, blockhash, signature status, broadcast)
- `MarketPriceService.kt` — CoinGecko market data (USD prices, 24h change)

### Data Layer (`app/src/main/java/com/example/data/`)
- `WalletEntities.kt` — BigInteger exact financial arithmetic, transaction state machine
- `WalletDao.kt` — Room DAO with insertTransactionIgnoringDuplicate, updateTransaction, getTransactionById
- `WalletDatabase.kt` — Room v3 with migrations v1→v2→v3, no destructive fallback, exportSchema=true
- `WalletRepository.kt` — Idempotent payments, settleTransaction, per-asset ceilings, key management
- `SecureKeyVault.kt` — Android Keystore AES-GCM, StrongBox/TEE fallback, real BIP-39 mnemonic generation
- `VaultAuthManager.kt` — PBKDF2-HMAC-SHA256 PIN (6 digits), BiometricPrompt, escalating lockout
- `WalletViewModel.kt` — Real derived addresses, PIN setup/unlock, vault auth state

### Security
- Hardware-backed key storage (Android Keystore, StrongBox preferred)
- Context-bound ciphertext (chain\|address as GCM AAD)
- PBKDF2-HMAC-SHA256 PIN with 120k iterations, 16-byte salt
- Constant-time PIN comparison (MessageDigest.isEqual)
- Escalating lockout after 5 failed attempts
- FLAG_SECURE on MainActivity (screenshot blocking)
- data_extraction_rules.xml excludes DB and prefs from backup
- usesCleartextTraffic="false", allowBackup="false"

### Dependencies Added
- org.web3j:crypto:4.12.2 (BIP-39, BIP-32, EVM crypto)
- org.bouncycastle:bcprov-jdk18on:1.78.1 (cryptographic primitives)

## Test Status
- Build: BUILD SUCCESSFUL
- Lint: PASSED
- Unit tests: 34/38 passing (89%)
- Test files: WalletCryptoTest, BitcoinTxTest, VaultAuthManagerTest, WalletRepositoryTest, TransactionStateMachineTest

## Coding Standards
- Kotlin only, no Java
- BigInteger for all financial amounts (no floating-point)
- All amounts in minor units (wei, satoshi, lamport, cents)
- HTTPS-only network clients
- No hardcoded secrets; use .env via Secrets Gradle Plugin
- Production-grade error handling with user-friendly messages
- SOLID principles, clean architecture separation

## Response Style
- Concise, technical, direct
- No unnecessary preamble/postamble
- Provide file_path:line_number references when citing code
- One-word or short answers preferred
- Never commit unless explicitly requested
