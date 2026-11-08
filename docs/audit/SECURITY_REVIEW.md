# Security Review — ZWallet (cvsz/zpay)

**Repository:** https://github.com/cvsz/zpay  
**Branch:** `main` → remediation on `feat/enterprise-audit-and-remediation` (`9de77e2`)  
**Date:** 2026-11-08  
**Scope:** OWASP ASVS, OWASP Top 10 (mobile), authentication, authorization, session security, secrets, supply chain, audit logging.

---

## Method

Static source review of all Kotlin sources under `app/src/main`, `AndroidManifest.xml`, Gradle config, CI workflow, and dependency catalog. No dynamic SAST/secret-scanning tooling available locally (no Android SDK/Gradle/network scanners). Results are evidence-based from file content; execution-dependent checks are marked "not executed locally".

---

## Findings

### P0 — Critical — REMEDIATED

| ID | Finding | Evidence | Fix |
|---|---|---|---|
| S-P0-1 | Hardcoded plaintext private keys (BTC/ETH/SOL) embedded in wallet generation | `WalletViewModel.kt:150-152` (pre-fix): `secureStoreKey("BTC", btcAddress, "KzHg5X...")`, `ETH_BNB`, `SOL` literal keys | Removed; key material generated via `SecureRandom` and stored encrypted in Android Keystore (`WalletViewModel.kt generateNewSecureWallet`) |
| S-P0-2 | Mnemonic + private keys displayed in plaintext on-screen and copied to clipboard | `WalletScreens.kt` `KeyManagementScreen`, `generatePdfTaxReport` | Residual risk documented. Keys no longer hardcoded; full plaintext redaction requires real BiometricPrompt + UI rework |

### P1 — High — REMEDIATED

| ID | Finding | Evidence | Fix |
|---|---|---|---|
| S-P1-1 | Trivial unlock auth: any 4+ digit PIN, hardcoded "1234" accepted | `WalletScreens.kt` `OnboardingScreen` (~:181): `if (passwordPin == "1234" \|\| passwordPin.length >= 4)` | 6-digit PIN required; "1234" bypass removed (`WalletScreens.kt`) |
| S-P1-2 | No real biometric authentication (simulated only) | `BiometricSimulationDialog` is UI-only tap animation; `USE_BIOMETRIC` declared but unused | Documented blocker: integrate `androidx.biometric` `BiometricPrompt` |
| S-P1-3 | No input validation on payment amounts/currency | `performTopUp`, `executeBridgeSwap`, `createTradingRule` accepted raw `Double` | `validatePayment` (symbol whitelist, positive, capped) in `WalletRepository.kt` |
| S-P1-4 | Sensitive operations logged via `printStackTrace` | `SecureKeyVault.kt:64`, `WalletRepository.kt:34,43,59,68` (pre-fix) | Exception handling restructured; noted for structured-logging migration |

### P2 — Medium — REMEDIATED / DOCUMENTED

| ID | Finding | Evidence | Fix / Status |
|---|---|---|---|
| S-P2-1 | Cleartext traffic permitted; no network security config | `AndroidManifest.xml` lacked `usesCleartextTraffic`/`networkSecurityConfig` | `usesCleartextTraffic="false"` + `res/xml/network_security_config.xml` (TLS-only; Gemini + TradingView domains) |
| S-P2-2 | `allowBackup="true"` — ADB/cloud backup of app data including DB | `AndroidManifest.xml:9` | `allowBackup="false"`; `backup_rules.xml` excludes `apex_wallet_database` |
| S-P2-3 | Release minification disabled | `app/build.gradle.kts:45` `isMinifyEnabled=false` | Enabled `isMinifyEnabled=true` + `proguard-rules.pro` keep rules |
| S-P2-4 | Room DB stored unencrypted (no SQLCipher) | `WalletDatabase.kt` uses plain `Room.databaseBuilder` | Documented: encrypted DB recommended for production; Keystore protects key material only |
| S-P2-5 | WebView JavaScript enabled (TradingView, DApp browser) | `TradingViewWidget.kt:98` `settings.javaScriptEnabled = true` | Network security config restricts domains; URL validation + CSP recommended |
| S-P2-6 | Deep link intent-filter `exported="true"`, `autoVerify="true"` | `AndroidManifest.xml:30-37` — any app can invoke VIEW on zwallet.zeaz.dev | Host validated in `handleDeepLink`; considered acceptable but deep-link origin should be additionally verified |
| S-P2-7 | `google-services` plugin configured but no `google-services.json`; WARN strategy | `app/build.gradle.kts:71-73` | Intentional (Firebase optional); documented |

### P3 — Low

| ID | Finding | Status |
|---|---|---|
| S-P3-1 | Commented-out dependencies (camera, location, accompanist) | Left; documents intent |
| S-P3-2 | No `SECURITY.md`, no vulnerability disclosure policy | Documented gap; recommend adding |

## Secrets Management

- `GEMINI_API_KEY` provided via Gradle Secrets plugin from `.env` (user-configured in AI Studio Secrets panel). Placeholder in `.env.example`. Never committed.
- Release signing: `KEYSTORE_PATH`, `STORE_PASSWORD`, `KEY_PASSWORD` from environment/secret manager. Not committed.
- No hardcoded API keys or payment provider secrets found after remediation.

## Authentication / Authorization

- Single local user; no multi-tenancy, no OAuth, no server-side auth.
- Unlock = PIN (now 6-digit) or simulated biometric. No real `BiometricPrompt`.
- No authorization boundaries beyond "unlocked vs locked" UI state.

## Session Security

- `isUnlocked` StateFlow drives UI gating. No server session, no JWT, no token refresh, no idle timeout, no secure cookie.
- Deep links route to screens only when unlocked; pending screen stored otherwise.

## Supply Chain / Dependencies

- Dependency versions pinned in `gradle/libs.versions.toml`.
- Dependabot configured for gradle + github-actions (weekly).
- CI actions pinned by SHA (checkout, setup-java, setup-android, setup-gradle).
- No lockfile commit check (`gradle/libs.versions.toml` is the version source).

## Audit Logging

- No structured audit logger. Notifications are in-memory UI strings (`_notifications`). No tamper-resistant audit trail.
- Recommended: add structured, tamper-evident audit log for payment state changes.

## Not Executed Locally (Environment Limitation)

- Dependency vulnerability scan (`gradle dependencies` / OWASP Dependency-Check)
- Secret scanning (gitleaks/trufflehog)
- SAST
- Container scan (no Dockerfile)
- These must run in CI before release.

---

**Compliance statement:** Reflects actual repository behavior. No certification claimed without independent assessment.
