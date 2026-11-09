# ZWallet Implementation Checklist

## Status Legend
- [x] Completed
- [ ] Pending / Not Started

## Core Crypto Implementation
- [x] crypto/Base58.kt - Base58 encoding/decoding for Solana addresses
- [x] crypto/Bech32.kt - Bech32/BIP-173 address encoding for Bitcoin P2WPKH
- [x] crypto/WalletCrypto.kt - BIP-39/32/44 HD wallet, SLIP-0010 ed25519, address derivation, signing
- [x] crypto/BitcoinTx.kt - BIP-143 P2WPKH transaction builder and signer (low-S DER)
- [x] crypto/SolanaTx.kt - Legacy Solana transaction builder and ed25519 signer

## Network Layer
- [x] network/EvmRpcClient.kt - EVM JSON-RPC client (Ethereum, BSC)
- [x] network/BitcoinApiClient.kt - mempool.space REST API client
- [x] network/SolanaRpcClient.kt - Solana JSON-RPC client
- [x] network/MarketPriceService.kt - CoinGecko market data service

## Dependencies
- [x] Added web3j 4.12.2 (BIP-39, BIP-32, EVM signing)
- [x] Added bouncycastle bcprov-jdk18on 1.78.1 (crypto primitives)

## Build & Compilation
- [x] Fixed WalletViewModel.kt compilation errors (type mismatches, missing imports)
- [x] Fixed WalletScreens.kt compilation errors (parameter names, type conversions)
- [x] `:app:compileDebugKotlin` - BUILD SUCCESSFUL
- [x] `:app:assembleDebug` - BUILD SUCCESSFUL
- [ ] Run `:app:lint` 
- [ ] Run `:app:testDebugUnitTest`

## Security & Authentication
- [ ] Update SecureKeyVault to use real BIP-39 mnemonic generation
- [ ] Wire real derived keys into SecureKeyVault + WalletViewModel (replace random hex)
- [ ] Implement PIN setup/verify UI flow
- [ ] BiometricPrompt integration
- [ ] Real balances/tx history from RPC clients

## UI Implementation
- [ ] DApp browser WebView + window.ethereum EIP-1193 provider
- [ ] Gemini/AI backend wiring + simulation labels for bridge/top-up
- [ ] PIN setup/verify screen

## Testing
- [ ] Unit tests: BIP-39/32 vectors
- [ ] Unit tests: address derivation
- [ ] Unit tests: tx signing (EVM/BTC/SOL)
- [ ] Unit tests: authentication
- [ ] Unit tests: idempotency
- [ ] Integration tests

## Documentation
- [ ] Create implementation-checklist.md
- [ ] Update README.md
- [ ] Update AGENTS.md
- [ ] Create/update docs/
- [ ] Create final-report.md

## Git & Release
- [ ] GPG-signed commit (auth as cvsz)
- [ ] Push to GitHub
- [ ] Open PR
- [ ] Switch gh auth to policedbc for audit/review/approve
- [ ] Switch gh auth back to cvsz for merge
