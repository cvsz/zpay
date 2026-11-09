# KILO.md — ZWallet Project Instructions

## Project
ZWallet (ZPay) — Enterprise multi-chain Web3 wallet (Android).

## Commands
- Build: `./gradlew :app:assembleDebug --no-daemon`
- Test: `./gradlew :app:testDebugUnitTest --no-daemon`
- Lint: `./gradlew :app:lint --no-daemon`

## Key Files
- `AGENTS.md` — Full project context and guidelines
- `FINAL_EXECUTION_REPORT.md` — Implementation report
- `final-report.md` — Final execution report
- `implementation-checklist.md` — Task checklist

## Implementation Highlights
- BIP-39/32/44 HD wallet with SLIP-0010 ed25519
- EVM (EIP-55), Bitcoin P2WPKH bech32, Solana base58 addresses
- BIP-143 transaction signing with low-S DER
- EVM/Bitcoin/Solana RPC clients
- Hardware-backed AES-GCM key storage (StrongBox)
- PBKDF2 PIN auth with biometric and lockout
- BigInteger exact financial arithmetic
