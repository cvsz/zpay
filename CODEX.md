# CODEX.md — ZWallet Project Instructions

## Project
ZWallet (ZPay) — Production-grade multi-chain Web3 wallet for Android.

## Commands
- Build: `./gradlew :app:assembleDebug --no-daemon`
- Test: `./gradlew :app:testDebugUnitTest --no-daemon`
- Lint: `./gradlew :app:lint --no-daemon`

## Tech Stack
- Kotlin, Jetpack Compose, Room, Coroutines, Flow
- AGP 9.1.1, Gradle 9.3.1, JDK 17
- web3j 4.12.2, bouncycastle 1.78.1

## Crypto Modules
- `WalletCrypto.kt` — BIP-39/32/44, SLIP-0010, address derivation, signing
- `BitcoinTx.kt` — BIP-143 P2WPKH, low-S DER
- `SolanaTx.kt` — Legacy Solana, ed25519
- `Base58.kt`, `Bech32.kt` — Address encodings

## Network Clients
- `EvmRpcClient.kt` — Ethereum/BSC JSON-RPC
- `BitcoinApiClient.kt` — mempool.space REST
- `SolanaRpcClient.kt` — Solana JSON-RPC
- `MarketPriceService.kt` — CoinGecko prices

## Security
- Android Keystore AES-GCM (StrongBox preferred)
- Context-bound GCM AAD ciphertext
- PBKDF2-HMAC-SHA256 PIN (6 digits), biometric, lockout
- No hardcoded secrets; HTTPS-only

## Money
- BigInteger for all amounts
- Minor units (wei, satoshi, lamport, cents)
- Per-asset decimal awareness (BTC:8, ETH:18, SOL:9, BNB:18)
