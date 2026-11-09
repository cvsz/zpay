# Payment Integrity Review — ZWallet (cvsz/zpay)

**Repository:** https://github.com/cvsz/zpay  
**Date:** 2026-11-08  
**Scope:** Integer minor-unit arithmetic, currency validation, server-side amount validation, idempotency, state machines, webhook auth, replay protection, refund integrity, ledger consistency, atomic operations, concurrent handling.

---

## Critical Disclosure

ZWallet's payment flows (fiat top-up, bridge swap, AI auto-trade) are **client-side simulations**. There is:
- No backend, no payment provider integration, no webhook endpoint, no blockchain node.
- No real funds move. All "Success" statuses were hardcoded pre-remediation.

This review evaluates the **integrity of the simulated ledger** and the guards that would prevent financial error if real provider logic were later added. No real payments/refunds were executed during testing.

---

## Pre-Remediation Deficiencies (all remediated on `9de77e2`)

| ID | Severity | Deficiency | Evidence (pre-fix) | Fix |
|---|---|---|---|---|
| PI-P0-1 | P0 | Floating-point money (`Double`) | `AssetBalance.balance: Double`, `amount: Double`, `feeUsd: Double` | Integer minor units: `balanceMinorUnit`, `priceUsdMinorUnit`, `amountMinorUnit`, `feeUsdMinorUnit` |
| PI-P0-2 | P0 | No idempotency — duplicate top-up/bridge double-credits | `saveTransaction(tx)` unconditional insert | `createPaymentIdempotent` + unique `idempotencyKey` DB index; duplicate key returns original |
| PI-P0-3 | P0 | Client supplies final settlement ("Success") | All `CryptoTransaction(status = "Success")` hardcoded | `TransactionStateMachine`; `settleTransaction(server-side)`; terminal states immutable |
| PI-P1-1 | P1 | No amount validation | Raw `Double` accepted | `validatePayment(symbol, amountMinorUnit, fee)` — symbol whitelist, positive, capped |
| PI-P1-2 | P1 | No transaction state machine | `status: String` free-form | `TransactionStatus` enum-like object; `isValidTransition` enforces PENDING→terminal only |
| PI-P1-3 | P1 | AI trades debit without user confirmation | `startAITradingMonitoring` debits directly | `requiresConfirmation=true`; `executeConfirmedTrade` requires explicit consent |
| PI-P2-1 | P2 | Insufficient-balance not checked before debit | Bridge/debit mutated balance without check | `executeBridgeSwap` checks `fromAsset.balanceMinorUnit >= amountMinorUnit` |
| PI-P2-2 | P2 | Destructive Room migration wipes ledger | `fallbackToDestructiveMigration(true)` | Documented pre-production blocker; implement versioned migrations |

---

## Post-Remediation Controls

### 1. Integer minor-unit arithmetic
All stored amounts are `Long` minor units:
- `AssetBalance.balanceMinorUnit` (asset minor units, e.g. wei/satoshi)
- `AssetBalance.priceUsdMinorUnit` (USD cents per whole unit)
- `CryptoTransaction.amountMinorUnit`, `feeUsdMinorUnit`
- `PriceAlert.targetPriceMinorUnit` (cents)
- `AITradingRule.targetThresholdMinorUnit`, `tradeAmountMinorUnit`

Display-only helpers (`displayBalance`, `displayPriceUsd`, `displayValueUsd`, `formatMinorUnits`, `formatUsdMinorUnits`) convert at read time; never written back.

### 2. Currency validation
`SUPPORTED_SYMBOLS = {"BTC","ETH","SOL","BNB"}`. `validatePayment` rejects unknown symbols. `AssetDecimals.decimals(symbol)` maps BTC=8, ETH=18, SOL=9, BNB=8.

### 3. Idempotent payment creation
`createPaymentIdempotent(...)`:
- Requires non-blank `idempotencyKey`.
- Validates amount (>0, <= `MAX_AMOUNT_MINOR_UNIT`, fee >=0).
- Checks `dao.findTransactionByIdempotencyKey(key)`; if present, returns the existing transaction (no double credit/debit).
- Inserts with `OnConflictStrategy.ABORT`; unique DB index on `idempotencyKey` makes the check atomic.

### 4. Transaction state machine
`TransactionStateMachine.isValidTransition(from,to)`:
- Terminal states (SUCCESS, FAILED, REVERSED) are immutable — no outbound transitions.
- PENDING may transition to SUCCESS / FAILED / REVERSED only.
- `settleTransaction(transactionId, newStatus)` loads current row, validates transition, applies. Client cannot supply final settlement.

### 5. Sufficient-balance check
`executeBridgeSwap` verifies source balance before debiting; `executeConfirmedTrade` verifies sell balance.

### 6. Atomic financial state changes
Each balance update and transaction insert is a single Room `@Insert`/`@Update` suspend call. Multi-step flows (debit + credit + log) run in one coroutine but are **not** wrapped in a single Room `@Transaction`. **Recommendation: wrap bridge debit/credit/log in `@Transaction` for atomicity.**

### 7. Reconciliation
No reconciliation process exists. Ledger consistency relies on the state machine + idempotency. **Recommendation: add a periodic reconciliation job comparing in-app ledger to provider settlement (when a real provider exists).**

### 8. Refund integrity
No refund flow exists. `REVERSED` status is defined in the state machine but no refund path is wired. **Recommendation: implement refund as a reversing transaction with its own idempotency key, never by mutating the original.**

### 9. Concurrent transaction handling
- `idempotencyKey` unique index prevents duplicate inserts under concurrency (last writer wins via ABORT).
- No row-level locking or version column; concurrent updates to the same balance could lost-update. **Recommendation: add a `version` column with optimistic locking for balance updates.**

---

## Not Tested (Environment Limitation)

- Concurrent payment requests — cannot run Android instrumented tests locally.
- Provider timeout / partial failure — no provider integrated.
- Refund retries — no refund flow.
- Invalid webhook signatures / expired timestamps — no webhook endpoint.
- Invalid signatures — no signature scheme.

These must be validated in CI with Robolectric/Espresso once the Android SDK is available. Tests written: `TransactionStateMachineTest.kt`, `WalletRepositoryTest.kt`.

---

## Recommendations Before Handling Real Funds

1. Add a server-side ledger and make settlement decisions server-side (never trust client).
2. Integrate a real payment provider with webhook signature verification + timestamp validation + replay protection.
3. Wrap multi-step balance mutations in Room `@Transaction`.
4. Add optimistic-lock `version` column to balances.
5. Implement refund as reversing transaction with idempotency key.
6. Add reconciliation + tamper-resistant audit logging.
7. Replace simulated biometric with `BiometricPrompt`.
