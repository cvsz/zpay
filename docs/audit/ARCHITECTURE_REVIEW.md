# Architecture Review — ZWallet (cvsz/zpay)

**Repository:** https://github.com/cvsz/zpay  
**Date:** 2026-11-08  
**Scope:** SOLID, Clean Architecture, domain boundaries, dependency inversion, coupling/cohesion, API consistency, error handling, state management.

---

## System Overview

ZWallet is a **single-module Android native app** (package `com.example`) built with Kotlin, Jetpack Compose, and Room. A minimal React/recharts web-dashboard component (`web-dashboard/components/PortfolioVisualizer.tsx`) exists but is disconnected from the Android app (no shared API, no backend). The app is client-side simulated: balances, fiat top-ups, bridge swaps, and AI trades are computed and persisted locally; there is no backend, no blockchain node, and no real payment provider.

## Layering & Domain Boundaries

| Layer | Components | Responsibility |
|---|---|---|
| Entry | `MainActivity.kt` | Activity lifecycle, Compose content root, deep-link handling |
| UI | `ui/screens/*.kt`, `ui/theme/*.kt` | Compose screens, navigation, theming |
| ViewModel | `data/WalletViewModel.kt` (`AndroidViewModel`) | UI state, business orchestration, coroutine launches |
| Repository | `data/WalletRepository.kt` | Domain logic, validation, idempotency, state-machine settlement |
| Data access | `data/WalletDao.kt` (Room `@Dao`) | SQL queries/inserts |
| Entities | `data/WalletEntities.kt` (Room `@Entity`) | Persisted schema |
| DB | `data/WalletDatabase.kt` (RoomDatabase) | Singleton Room instance |
| Crypto | `data/SecureKeyVault.kt` | Android Keystore AES-GCM |
| AI client | `data/GeminiService.kt` | OkHttp calls to Gemini API |

This is a **Repository + ViewModel** pattern over Room, not a full Clean Architecture (no separate domain/use-case layer, no presenter/interactor separation). For the app's scale this is acceptable, but financial logic is mixed into the ViewModel in places (e.g. bridge swap arithmetic).

## Dependency Inversion

- ViewModel depends on Repository interface-ish (concrete `WalletRepository`).
- Repository depends on `WalletDao` (Room interface).
- Direction: UI → ViewModel → Repository → DAO → Room. Reasonable inversion for data access.
- `WalletViewModel` instantiates `WalletDatabase.getDatabase(application)` directly (service-locator-ish singleton) rather than receiving the DB via constructor injection. This impedes unit testing without Robolectric. **Recommendation: constructor-inject `WalletDao` into `WalletRepository` and ViewModel.**

## Coupling & Cohesion

- High cohesion within `data/` package (entities, DAO, repository, vault).
- `WalletViewModel` is a "god ViewModel" (~600 lines) handling navigation, biometrics, gas, bridge, sentiment, AI trading, alerts, top-up, notifications, price ticker. **High coupling; recommend splitting into per-domain ViewModels or UseCases.**
- UI screens reference `com.example.data.*` wildcard import, coupling UI to data model internals.

## State Management

- UI navigation via `MutableStateFlow<String>` (screen names as strings — error-prone; recommend sealed class/navigation route type).
- Financial balances observed via Room `Flow` → `stateIn(viewModelScope, WhileSubscribed(5000))`.
- Background loops (`startPriceAlertMonitoring`, `startAITradingMonitoring`, `startPriceTickerSimulation`) run indefinitely in `viewModelScope` with fixed delays; no cancellation safety beyond ViewModel clearing. Price ticker writes to DB every 10s unconditionally.

## Error Handling

- `SecureKeyVault` and original `WalletRepository` swallowed exceptions with `e.printStackTrace()`.
- Post-fix: `WalletRepository` throws `WalletRepositoryException` for storage failures; `validatePayment` uses `require()`; state machine rejects invalid transitions.
- UI uses `Toast` for errors (not observable/testable).

## API Consistency

- No network API layer beyond GeminiService (direct OkHttp, no Retrofit interface despite Retrofit dependency).
- No DTOs, no mappers, no error model. GeminiService returns plain `String` with magic error strings (`"ERROR_API_FAIL"`, `"API_KEY_MISSING"`).

## SOLID Assessment

| Principle | Assessment |
|---|---|
| SRP | Weak — ViewModel handles many domains; SecureKeyVault does keystore + mnemonic generation |
| OCP | Weak — adding chains requires editing `when` branches in ViewModel |
| LSP | N/A (no inheritance hierarchies) |
| ISP | N/A |
| DIP | Partial — DAO interface inverted, but DB instantiated directly in ViewModel |

## Findings

| ID | Severity | Finding | Evidence | Recommendation |
|---|---|---|---|---|
| A-1 | P2 | God ViewModel; no use-case layer | `WalletViewModel.kt` ~600 lines | Split into domain ViewModels/UseCases |
| A-2 | P3 | DB instantiated directly (service locator) | `WalletViewModel.kt:12` | Constructor-inject `WalletDao` |
| A-3 | P3 | String-based screen navigation | `currentScreen: StateFlow<String>` | Sealed route type |
| A-4 | P3 | No API layer/DTOs for Gemini | `GeminiService.kt` returns `String` | Retrofit interface + DTOs + error model |
| A-5 | P2 | Background loops write DB every 10s unconditionally | `startPriceTickerSimulation` | Add debounce / only-write-on-change |
| A-6 | P3 | Wildcard data imports in UI | `import com.example.data.*` | Explicit imports |

## Conclusion

Architecture is coherent and appropriate for a demo client app, but not enterprise Clean Architecture. Financial logic should be extracted from the ViewModel into a transaction service with constructor-injected dependencies before handling real funds.
