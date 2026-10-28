<div align="center">
<img width="1200" height="475" alt="GHBanner" src="https://ai.google.dev/static/site-assets/images/share-ais-513315318.png" />
</div>

# Run and deploy your AI Studio app

This contains everything you need to run your app locally.

View your app in AI Studio: https://ai.studio/apps/3f01c838-dffa-45be-8dfa-a73d848d3f17

## Run Locally

**Prerequisites:**  [Android Studio](https://developer.android.com/studio)


1. Open Android Studio
2. Select **Open** and choose the directory containing this project
3. Allow Android Studio to fix any incompatibilities as it imports the project.
4. Create a file named `.env` in the project directory and set `GEMINI_API_KEY` in that file to your Gemini API key (see `.env.example` for an example)
5. Do not use the debug signing configuration for a release build. Release builds use the dedicated `release` signing configuration and require `KEYSTORE_PATH`, `STORE_PASSWORD`, and `KEY_PASSWORD` from an approved secret manager. Never commit a keystore or signing password. CI intentionally builds and tests debug artifacts only.
6. Run the app on an emulator or physical device
