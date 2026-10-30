<div align="center">
<img width="1200" height="475" alt="GHBanner" src="https://ai.google.dev/static/site-assets/images/share-ais-513315318.png" />
</div>

# ZPay Android — Demo Build Only

This repository currently contains a non-production UI prototype. Wallet custody, on-chain transactions, payments, card processing, automated trading, biometric authentication, and AI gateway integration are disabled.

View your app in AI Studio: https://ai.studio/apps/3f01c838-dffa-45be-8dfa-a73d848d3f17

## Run Locally

**Prerequisites:**  [Android Studio](https://developer.android.com/studio)


1. Open Android Studio
2. Select **Open** and choose the directory containing this project
3. Allow Android Studio to fix any incompatibilities as it imports the project.
4. Do not add API keys, payment credentials, wallet seeds, or signing keys to the Android client. AI and payment integrations remain disabled until a trusted server-side gateway is implemented and reviewed.
5. Do not use the debug signing configuration for a release build. Release builds use the dedicated `release` signing configuration and require `KEYSTORE_PATH`, `STORE_PASSWORD`, and `KEY_PASSWORD` from an approved secret manager. Never commit a keystore or signing password. CI intentionally builds and tests debug artifacts only.
6. Run the demo UI on an emulator or physical device. Do not send funds, enter card details, or use any displayed balance/address as a real wallet.
