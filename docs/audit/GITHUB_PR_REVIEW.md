# GitHub Pull Request Review — ZWallet (cvsz/zpay)

**Repository:** https://github.com/cvsz/zpay  
**Review date:** 2026-11-08  
**GitHub CLI:** authenticated as `cvsz`  
**Merge authorization:** None given. No PR was merged, force-pushed, or bypassed.

---

## Repository & Branch Protection

| Setting | Value | Source |
|---|---|---|
| Default branch | `main` | `git branch -r` / `gh repo view` |
| Branch protection (main) | **NOT protected** | `gh api repos/cvsz/zpay/branches/main/protection` → 404 "Branch not protected" |
| Rulesets | None (empty) | `gh api repos/cvsz/zpay/rulesets` → `[]` |
| Required status checks | None | No protection rule |
| Main-branch Actions runs | Zero reported | PR #2 reviewer: `actions/runs?branch=main` reports zero |

**Implication:** `main` has no protection, no required checks, no rulesets. Force-push and direct push to main are technically possible but were NOT performed. All work was done on feature branch `feat/enterprise-audit-and-remediation`.

---

## Pull Requests

### PR #1 — ci(android): establish pinned build and test baseline — MERGED

| Field | Value |
|---|---|
| Number | 1 |
| State | MERGED |
| Merged at | 2026-11-08T15:09:52Z |
| Head | `ci/android-build-security-baseline-20261028-run11` → `main` |
| Base | `main` |

Added the pinned Android CI workflow (`.github/workflows/android-ci.yml`): checkout, JDK 17, Android SDK 36, Gradle 9.3.1, `testDebugUnitTest lintDebug assembleDebug`, `contents: read`, actions pinned by SHA. This is the CI baseline used for auditing.

### PR #2 — security(zpay): disable simulated wallet and payment flows — OPEN

| Field | Value |
|---|---|
| Number | 2 |
| State | OPEN |
| Title | security(zpay): disable simulated wallet and payment flows |
| Head | `fix/zpay-demo-financial-flow-containment-20261031-run11` |
| Base | `main` |
| Mergeable | **CONFLICTING** |
| Review decision | **CHANGES_REQUESTED** |
| Draft | No |
| Author | cvsz |
| Updated | 2026-11-08T15:10:09Z |

**Purpose:** Contain the demo wallet so it cannot accept/simulate customer funds. Removes card/CVV collection, mock QR, simulated success receipts, hardcoded private keys, sample balances/transactions, AI trading monitor, client-side Gemini key, Android backup, destructive Room migration, and biometric dependency. Adds `wallet-safety.yml` CI and `scripts/ci/check_wallet_safety.py` source assertions.

**Files changed (20):** `.env.example`, `.github/workflows/wallet-safety.yml`, `README.md`, `app/build.gradle.kts`, `AndroidManifest.xml`, `GeminiService.kt`, `SecureKeyVault.kt`, `WalletDatabase.kt`, `WalletRepository.kt`, `WalletViewModel.kt`, `TopUpScreen.kt`, `WalletScreens.kt`, `build.gradle.kts`, `libs.versions.toml`, `scripts/ci/check_wallet_safety.py`.

**Review feedback (reviewer `policedbc`):**
1. `CHANGES_REQUESTED` (2026-10-30): exact-head CI `verify` run #41116167285 job #124270858493 FAILED but job steps/logs unavailable (404). Main unprotected, no required checks. Unsigned commits not admissible under portfolio policy. Disposition: KEEP DRAFT / NO MERGE / NO RELEASE.
2. `COMMENTED` (2026-10-31): source-safety replay of `check_wallet_safety.py` assertions = 0 findings across 11 files. CI root cause unknown. KEEP DRAFT / NO MERGE.
3. `COMMENTED` (2026-11-02): Wallet Safety run #41116167285 attempt 4 = SUCCESS; Android CI PR #1 (separate Draft) run #40697224703 failed before job steps; Android evidence BLOCKED/UNKNOWN. KEEP DRAFT / NO MERGE.

**CI status for PR #2:** `verify` = PASS (run 41116167285, job 125040617979, 5s). Android build/test evidence is NOT available.

**Assessment:** PR #2 is a legitimate security containment change but must NOT be merged: it is conflicting, has an unresolved `CHANGES_REQUESTED` review, commits are unsigned, `main` is unprotected, and there is no explicit merge authorization. I did not merge it.

**Conflict note:** PR #2 touches the same files as my remediation branch (`WalletViewModel.kt`, `WalletRepository.kt`, `WalletScreens.kt`, `AndroidManifest.xml`, `WalletDatabase.kt`, `build.gradle.kts`, `app/build.gradle.kts`). This is the source of the `CONFLICTING` status. The two approaches differ: PR #2 disables the simulated flows entirely; my branch hardens and preserves them (per the "preserve existing architecture" directive). They cannot both be merged to `main` without reconciliation and authorization.

### PR #4 — docs(governance): reference canonical ZeaZ policy in agent guide — OPEN

| Field | Value |
|---|---|
| Number | 4 |
| State | OPEN |
| Title | docs(governance): reference canonical ZeaZ policy in agent guide |
| Head | `governance/zeaz-policy-adoption-20261108` |
| Base | `main` |
| Mergeable | MERGEABLE |
| Review decision | (none — no reviews) |
| CI checks | None reported |

**Purpose:** Documentation-only; adds canonical ZeaZ policy reference to root `AGENTS.md`. No product behavior change.

**Assessment:** Mergeable and reviewable, but I did not merge it (no authorization). It touches `AGENTS.md`, which I was instructed not to overwrite. Reviewer should confirm repository-specific policy consistency before merge.

---

## Release Tags & Security Policy

| Item | Value |
|---|---|
| Release tags | None found |
| Security policy (`SECURITY.md`) | Not present |
| GPG signed commits | PR #2 commits unsigned; my remediation commit unsigned (GPG passphrase unavailable — see RELEASE_READINESS) |

---

## Recommendations

1. **Protect `main`**: enable branch protection, require status checks (Android CI + Wallet Safety), require signed commits, require review approval.
2. **Resolve PR #2**: author must clear `CHANGES_REQUESTED`, rebase to resolve conflicts, sign commits, and obtain approval before any merge. No merge without authorization.
3. **Review PR #4**: governance doc change; verify `AGENTS.md` consistency; merge only with review + authorization.
4. **Add `SECURITY.md`** and a vulnerability disclosure process.
5. **Do not enable** wallet custody, card processing, real-money flows, or production release on `main` until governance and CI are effective.

---

**No merge, no force push, no production deployment, no bypass of protection was performed.** All findings reflect live GitHub state via `gh`.
