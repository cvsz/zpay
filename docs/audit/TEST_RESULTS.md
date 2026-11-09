# Test Results — ZWallet (cvsz/zpay)

**Repository:** https://github.com/cvsz/zpay  
**Branch:** `feat/enterprise-audit-and-remediation` (`9de77e2`)  
**Date:** 2026-11-08  
**Environment limitation:** Android SDK, Gradle wrapper, and `kotlinc` are NOT available locally. No build, lint, type check, unit test, or screenshot test could be executed locally. Results below are either source-level (tests written) or CI-observed (GitHub). No results are fabricated.

---

## Tests in Repository (after remediation)

### New tests (commit `9de77e2`)

| Test file | Type | Purpose | Status |
|---|---|---|---|
| `app/src/test/java/com/example/TransactionStateMachineTest.kt` | Unit (JUnit) | Valid/invalid state-machine transitions; terminal-state immutability; unknown-status rejection | Written — not executed locally |
| `app/src/test/java/com/example/WalletRepositoryTest.kt` | Unit (JUnit + coroutines) | Amount validation; idempotent create (replay returns original); settle rejects non-terminal/post-terminal transitions | Written — not executed locally |

### Pre-existing tests (retained)

| Test file | Type | Status |
|---|---|---|
| `app/src/test/java/com/example/ExampleUnitTest.kt` | Unit (JUnit) | `addition_isCorrect` — not executed locally |
| `app/src/test/java/com/example/ExampleRobolectricTest.kt` | Robolectric | Reads `R.string.app_name` — not executed locally |
| `app/src/test/java/com/example/GreetingScreenshotTest.kt` | Roborazzi screenshot | Renders theme box — not executed locally |
| `app/src/androidTest/java/com/example/ExampleInstrumentedTest.kt` | Instrumented | `useAppContext` — requires device/emulator — not executed locally |

---

## Tests Not Executed Locally (required before release)

| Check | Command | Why not executed |
|---|---|---|
| Compile / type check | `gradle compileDebugKotlin` | No `kotlinc` / Android SDK |
| Unit tests | `gradle testDebugUnitTest` | No Android SDK / Robolectric runtime |
| Lint | `gradle lintDebug` | No Android SDK |
| Debug build | `gradle assembleDebug` | No Gradle wrapper / Android SDK |
| Screenshot tests | Roborazzi (`testDebugUnitTest` includes roborazzi) | No Android SDK |
| Instrumented tests | connected Android device/emulator | No device farm access |

**Execution prerequisite:** CI (`.github/workflows/android-ci.yml`) provides JDK 17, Android SDK 36, Gradle 9.3.1 and runs `testDebugUnitTest lintDebug assembleDebug`. That workflow must be run on `feat/enterprise-audit-and-remediation` (or the change rebased to `main`) and green before release.

---

## CI / Observed Results

### GitHub Actions — Android CI baseline (PR #1, merged)

Workflow: `.github/workflows/android-ci.yml`
- `verify`: Android unit tests, lint, debug build on `ubuntu-24.04`
- Steps: checkout (pinned SHA), setup-java 17 (pinned), setup-android (pinned), setup-gradle 9.3.1 (pinned), `testDebugUnitTest lintDebug`, `assembleDebug`
- Permissions: `contents: read`
- Runs on: `pull_request` and `push` to `main`

### PR #2 CI (observed via `gh pr checks 2`)

| Check | Conclusion | Job | Link |
|---|---|---|---|
| `verify` | PASS | 125040617979 | runs/41116167285 |

Note: PR #2's `verify` runs its `wallet-safety.yml` source assertions (check_wallet_safety.py). Android build/test evidence for PR #2 is reported BLOCKED/UNKNOWN by its reviewer (run failed before job steps in the Android CI dependency).

### main branch CI

No main-branch Actions runs reported. `main` is unprotected with no required status checks.

---

## Test Gaps (to address before production)

1. **Concurrent payment requests** — no test for simultaneous top-up/bridge on the same account.
2. **Invalid webhook signatures / expired timestamps** — no webhook endpoint exists yet.
3. **Duplicate webhook events** — N/A until provider webhooks integrated.
4. **Provider timeout / partial failure** — no provider integrated.
5. **Refund retries** — no refund flow exists.
6. **Unauthorized account access** — single-user local app; no multi-tenancy.
7. **Currency/amount mismatch** — partially covered by `validatePayment` (unit-tested).
8. **Roborazzi screenshot coverage** for critical flows (unlock, top-up, keys) — only one greeting screenshot exists.

---

## Statement

No test result is claimed unless the test actually executed. Locally, no Android tests executed due to missing SDK/Gradle. The two new unit-test files are written and logically cover the P1 financial invariants; they must be executed in CI before release.
