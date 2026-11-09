# Release Readiness — ZWallet (cvsz/zpay)

**Repository:** https://github.com/cvsz/zpay  
**Branch:** `feat/enterprise-audit-and-remediation` (`9de77e2`)  
**Date:** 2026-11-08  
**Status:** NOT READY FOR RELEASE — blockers documented below  
**Release performed:** None. No tag, no force push, no production deployment.

---

## GPG Signing Status (honest)

| Item | Value |
|---|---|
| GPG available | Yes (`/usr/bin/gpg`) |
| Secret keys present | Yes: `8BC7A85F1FB5322782921AEC14E1E0BC08F632B0` (ed25519, [ultimate], expires 2027-10-05) and `A62FF2713F8DC36589AB1EA9C3CDE65F606F7BF0` (nistp256, [ultimate], expires 2028-10-06) |
| Git user | `cvsz <seaza@msn.com>` |
| `commit.gpgsign` | `true` |
| `gpg.program` | `/home/cvsz/.local/bin/gpg-loopback.sh` (loopback, reads `GPG_PASSPHRASE` env var) |
| Commit `9de77e2` signed? | **NO** — unsigned |
| Reason | `gpg: NEED_PASSPHRASE ... in batchmode - can't get input`. The `GPG_PASSPHRASE` required by the loopback wrapper is not available in this environment. |
| Private key handled? | Never generated, exposed, printed, or committed. Passphrase never requested from user. |

**Blocker:** GPG signing credentials (passphrase) are unavailable. Per the directive, no verified signature was invented and no private key material was handled. The remediation commit is **unsigned**. Before release, commits/tags must be GPG-signed with an available, authorized signing key, and signatures independently verified.

## Release Blockers

| # | Blocker | Severity | Required action |
|---|----|---|---|
| B-1 | Android build/tests not executable locally (no SDK/Gradle/kotlinc) | P0 | Run `gradle testDebugUnitTest lintDebug assembleDebug` in CI; must be green |
| B-2 | GPG signing unavailable (passphrase not in environment) | P1 | Obtain authorized signing key; sign commits/tags; verify independently |
| B-3 | `main` unprotected; no required status checks; no rulesets | P1 | Enable branch protection + required checks + signed-commit requirement |
| B-4 | Room destructive migration (`fallbackToDestructiveMigration=true`) | P1 | Implement versioned Room migrations; set `fallbackToDestructiveMigration(false)` |
| B-5 | Biometric authentication simulated only | P1 | Integrate `androidx.biometric` `BiometricPrompt` |
| B-6 | Private keys revealed in plaintext in UI | P1 | Mask / gate key display behind real biometric |
| B-7 | No server-side ledger / real payment provider | P0 | Do not release as a real-money wallet; flows are client-side simulated |
| B-8 | Open PR #2 is `CONFLICTING` + `CHANGES_REQUESTED` + unsigned | P2 | Author must clear review, rebase, sign, and obtain merge authorization |
| B-9 | Test execution evidence missing (unit/lint/build) | P1 | CI must run and produce executed-test evidence |

## Release Checklist (all must be satisfied before any release)

- [ ] Android CI green on exact final commit (`testDebugUnitTest`, `lintDebug`, `assembleDebug`)
- [ ] Unit tests for financial invariants executed and passing
- [ ] Room migrations implemented and tested (no destructive migration)
- [ ] `main` branch protected with required status checks
- [ ] GPG signing key available; commits/tags signed and independently verified
- [ ] Open PR #2 either merged (with authorization, after review cleared) or closed
- [ ] No secrets in commit history (hardcoded keys removed; verify with secret scan)
- [ ] Release notes describe demo-only status if any pre-release is cut
- [ ] No production deployment without explicit authorization

## What Was NOT Done (by directive)

- No release tag created
- No GPG signature invented
- No force push
- No bypass of branch protection
- No unapproved merge
- No production deployment
- No irreversible operation
- No real payment or refund executed

## Next Actions Requiring Human Approval

1. Authorize GPG signing / provide signing-key access so commits can be signed.
2. Authorize merging PR #4 (governance) after review, OR close it.
3. Authorize handling of PR #2 (close or have author rebase + clear review + sign).
4. Approve enabling `main` branch protection and required CI checks.
5. Approve any production release / real-money provider integration.
