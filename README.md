# IRIS AI Voice Assistant — Android Only

Version 1.0.0 public testing build. Premium/login are intentionally removed.

## Included
- Official supplied ROYAL ANKIT AHIRAN logo used unchanged as the app brand asset.
- Real Android SpeechRecognizer voice input with runtime microphone permission.
- Android TextToSpeech voice output.
- Built-in command router for time/date, settings, web search, YouTube, alarms, timers, and launching installed apps.
- Local command history and lightweight conversation memory using SharedPreferences.
- Hindi/English-friendly command matching; TTS language follows the device locale where available.
- Optional "Hey IRIS" phrase detection during an active listening session.
- Optional floating IRIS overlay, enabled only after the user grants Android overlay permission.
- Optional Gemini/cloud integration through a secure HTTPS backend endpoint; no Gemini secret is hard-coded in the APK.
- Color-coded action buttons and futuristic IRIS HUD styling.

## Important API architecture
Android app -> HTTPS backend -> Google Gemini API -> backend -> Android app.

Set the backend endpoint in the app's API settings. Do not put a Gemini API key directly in this APK.

## Build
Open this folder in Android Studio and sync Gradle. Then build a debug APK.

The supplied environment did not contain an Android SDK/Gradle cache, so this source package was statically checked here but an APK was not claimed as compiled in this environment.
