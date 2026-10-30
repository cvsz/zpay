#!/usr/bin/env python3
"""Static safety gates for the non-production ZPay Android demo."""

from pathlib import Path
import re
import sys

ROOT = Path(__file__).resolve().parents[2]


def read(relative: str) -> str:
    path = ROOT / relative
    if not path.is_file():
        raise AssertionError(f"required file is missing: {relative}")
    return path.read_text(encoding="utf-8")


def function_block(source: str, start: str, end: str) -> str:
    start_index = source.find(start)
    if start_index < 0:
        raise AssertionError(f"function marker is missing: {start}")
    end_index = source.find(end, start_index + len(start))
    if end_index < 0:
        raise AssertionError(f"end marker is missing after: {start}")
    return source[start_index:end_index]


def main() -> int:
    failures: list[str] = []

    def require(condition: bool, message: str) -> None:
        if not condition:
            failures.append(message)

    manifest = read("app/src/main/AndroidManifest.xml")
    require('android:allowBackup="false"' in manifest, "Android backup must be disabled")
    require("android.permission.USE_BIOMETRIC" not in manifest, "unimplemented biometric permission must not be requested")

    topup = read("app/src/main/java/com/example/ui/screens/TopUpScreen.kt")
    require("PAYMENTS DISABLED" in topup, "top-up screen must show the payment-disabled state")
    require("Do not enter card" in topup, "top-up screen must warn against entering payment credentials")
    for marker in ("cardNumber", "cardCvv", "CardOnRampPanel", "PCI-DSS", "SIMULATE", "LOAD SUCCESSFUL"):
        require(marker not in topup, f"top-up screen still contains unsafe payment UI marker: {marker}")

    view_model = read("app/src/main/java/com/example/data/WalletViewModel.kt")
    for marker in (
        "KzHg5X7p9qWf5nUdy5SreH96h1r2v929848Y3uX1zR678X9W",
        "0x4c82c9f1a238612984a91cf2376dc828ea285f20f0119e24ca6c3821faf95d6b",
        "2A4b9Y7Z7qW5nR1pXyA6vHq3cT2b9v6xW5nUdy5SreH9",
        "repository.secureStoreKey",
        "repository.secureStoreMnemonic",
        "0x89e24ca6",
        "0xtopup_",
        "0xbridge_",
    ):
        require(marker not in view_model, f"wallet view model contains legacy test custody/transaction marker: {marker}")
    require('val btcAddress = "NOT_CONFIGURED"' in view_model, "BTC deposit address must remain unconfigured")
    require('val ethAddress = "NOT_CONFIGURED"' in view_model, "EVM deposit address must remain unconfigured")
    require('val solAddress = "NOT_CONFIGURED"' in view_model, "SOL deposit address must remain unconfigured")
    require("startPriceTickerSimulation" not in view_model, "demo price simulation must not mutate portfolio balances")
    require("repository.saveBalance" not in view_model, "view model must not mutate portfolio balances in demo mode")
    require("repository.saveTransaction" not in view_model, "view model must not fabricate transaction records in demo mode")
    require("startAITradingMonitoring" not in view_model, "automated trading monitor must remain disabled")
    require("repository.saveTradingRule" not in view_model, "demo mode must not persist active trading rules")
    require("repository.saveBalance" not in view_model, "demo view model must not mutate balances anywhere")
    require("repository.saveTransaction" not in view_model, "demo view model must not create transaction records anywhere")
    require("GeminiService." not in view_model, "demo view model must not call the disabled AI client")
    require("AI Trading Agent proposed execution strategy." not in view_model, "demo view model must not claim to generate trading strategies")
    gas_method = function_block(
        view_model,
        "    fun refreshGasEstimation(",
        "    /**",
    )
    require(".random()" not in gas_method, "gas estimates must not be fabricated with random values")
    require("_gasEthGwei.value = 0.0" in gas_method, "ETH gas estimate must remain unavailable")
    require("_gasSolFee.value = 0.0" in gas_method, "SOL gas estimate must remain unavailable")

    topup_method = function_block(
        view_model,
        "    fun performTopUp(",
        "    /**\n     * Start background price monitors",
    )
    require("repository.saveBalance" not in topup_method, "performTopUp must not mutate balances")
    require("repository.saveTransaction" not in topup_method, "performTopUp must not create a successful transaction")

    bridge_method = function_block(
        view_model,
        "    fun executeBridgeSwap(",
        "    /**\n     * Triggers the Gemini AI Sentiment analysis",
    )
    require("repository.saveBalance" not in bridge_method, "executeBridgeSwap must not mutate balances")
    require("repository.saveTransaction" not in bridge_method, "executeBridgeSwap must not create a successful transaction")

    wallet_repository = read("app/src/main/java/com/example/data/WalletRepository.kt")
    require("SecureKeyVault" not in wallet_repository, "repository must not call the retired demo key vault")
    require("UnsupportedOperationException" in wallet_repository, "wallet key storage must fail closed")

    gemini = read("app/src/main/java/com/example/data/GeminiService.kt")
    require("AI_SERVICE_DISABLED" in gemini, "AI client must remain disabled without a trusted backend")
    for marker in ("BuildConfig.GEMINI_API_KEY", "generativelanguage.googleapis.com", "?key="):
        require(marker not in gemini, f"client-side Gemini credential/endpoint remains: {marker}")

    app_gradle = read("app/build.gradle.kts")
    root_gradle = read("build.gradle.kts")
    version_catalog = read("gradle/libs.versions.toml")
    require("libs.plugins.secrets" not in app_gradle, "app must not apply the client secrets plugin")
    require("libs.plugins.secrets" not in root_gradle, "root build must not configure the client secrets plugin")
    require("secretsGradlePlugin" not in version_catalog, "secrets plugin version must be removed")
    require("plugins.secrets" not in version_catalog, "secrets plugin alias must be removed")

    database = read("app/src/main/java/com/example/data/WalletDatabase.kt")
    require("fallbackToDestructiveMigration" not in database, "Room must not silently destroy wallet data on schema mismatch")

    screens = read("app/src/main/java/com/example/ui/screens/WalletScreens.kt")
    require("DEMO BUILD ONLY" in screens, "dashboard must disclose demo-only balances and actions")
    require("DEMO MODE — NO KEY CUSTODY" in screens, "wallet UI must not claim secure enclave custody")
    require("Transfers are disabled in this demo build." in screens, "send action must be disabled")
    require("No verified deposit addresses are configured." in screens, "receive action must warn against deposits")
    require("AUTOMATED TRADING DISABLED" in screens, "trading controls must be replaced with a disabled notice")
    require("DEPLOY ALGORITHMIC RULE" not in screens, "demo UI must not offer automated rule deployment")
    require("AI FEATURES DISABLED" in screens, "AI screen must disclose disabled AI functionality")
    for marker in ("TRADING WITH AI AGENTS", "REFRESH MARKET SENTIMENT SCORE", "RUN ALGORITHMIC ANALYSIS", "server-side Gemini intelligence models"):
        require(marker not in screens, f"AI screen still presents unavailable capability: {marker}")
    require("GAS FEE ESTIMATION DISABLED" in screens, "bridge screen must disclose unavailable network fee estimates")
    bridge_button = function_block(
        screens,
        "        // BRIDGE EXECUTION",
        "        // BRIDGE STATUS NOTIFIER",
    )
    require("enabled = false" in bridge_button, "bridge execution button must remain disabled")
    require("BRIDGE EXECUTION DISABLED" in bridge_button, "bridge execution button must disclose its disabled state")
    require("viewModel.executeBridgeSwap" not in bridge_button, "demo bridge UI must not invoke execution")
    for marker in ("ERC-20 Bridge Link", "SPL Wormhole Portal", "tBTC Native Custody", "BSC TokenBridge Gateway", "Estimated Toll Cost", "INITIATE CRYPTOGRAPHIC BRIDGE"):
        require(marker not in screens, f"bridge screen still presents an unverified route or fee: {marker}")

    readme = read("README.md")
    require("DEMO BUILD ONLY" in readme or "Demo Build Only" in readme, "README must disclose non-production status")
    require("GEMINI_API_KEY" not in readme, "README must not instruct users to embed a Gemini key in the client")

    if failures:
        for failure in failures:
            print(f"FAIL: {failure}", file=sys.stderr)
        return 1

    print("PASS: wallet demo disables custody, payment, bridge, AI trading, fabricated gas estimates, and client credentials.")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
