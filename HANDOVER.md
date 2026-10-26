# Cyber Victim Assistant — Handover

Private-repo handover doc. Inspector friendly: no secrets, only variable NAMES + file locations.

## 1. What is built and working

- **Android app** (Kotlin + Jetpack Compose): multi-step cybercrime victim flow — Case building (facts/evidence with OTP-verified victim identity), Portal Guide (grounded in official `MHA Citizen Manual — Report Other Cyber Crime` ARCHIVE SNAPSHOT), Gemini scenario pick, exposure (email breach + password reuse) checks, `EN / HI / TE` localization, explainer dialogs for every step.
- **Gating rule (fixed core invariant)**: `ActionPolicy` — Submit/Preview/Final-submit buttons permanently `VICTIM_LOCK`; captcha / OTP / password / username fields `HUMAN` taps; everything else AI_AUTO fills. AI never touches final submit or OAuth-style credential entries.
- **Gemini pick contract**: `gemini-3.8-flash` primary + `gemini-flash-latest` fallback chain (which model string to substitute can vary by unix-account key model schedule) — `thinkingConfig.thinkingBudget = 0`. `PlanPickParseTest` verbatim test; accuracy suite `SCENARIO-ACCURACY 40/40 = 100%`.
- **Portal Guide**: KB from `app/src/main/assets/kb/scenario_buttons.json` + `portal_map.json` (structured per official manual snapshot; `lastVerified: 2026-10-22`).
- **Backend** `tools/backend/server.js` (Express): OTP email-verify loop (throwaway mail demo route), `/breaches/:email` — **XposedOrNot free no-key default** + optional HIBP API-key branch + explicit demo-mode last resort; per-IP rate limit; on 401 token invalidation scrubs token from encrypted prefs.
- **Exposure security**: `exposure/SecPrefs.kt` — EncryptedSharedPreferences (AES-256 GCM) for OTP-verified device tokens wiped on 401.
- **Evidence AI-refine OCR pass**: Gemini text pass extracting facts (`upi_id` / `txn_ref` / amounts …) from raw OCR text; opt-in chip in `EvidenceScreen`, default OFF.
- **Play compliance doc**: `docs/PLAY_COMPLIANCE.md` — QUERY_ALL_PACKAGES / PACKAGE_USAGE_STATS / SCHEDULE_EXACT_ALARM justifications + data-safety checklist.
- **Firebase Functions scaffold** `tools/functions/` ready (`DEPLOY.md` inside describes project ID + secrets + region `asia-south1`).
- **Tests/Violations**: all green (PlanAccuracy, PlanPickParse, PackBudget, ExposureApi flows).

## 2. What is pending

1. **Firebase deployment** — CLI-side auth token / fire-tool; DEPLOY.md inside `tools/functions/` describes the hand path (`firebase deploy --only functions`, region `asia-south1`); secret move to `functions/.env` per `DEPLOY.md`.
2. **Portal Guide polish** — in-app WebView executor is parked (IDE decision); manual re-verification cadence (user decides when to re-crawl the live portal).
3. **On-device acceptance test** — full victim flow against the deployed backend.

## 3. Versions used (source machine)

| Tool      | Version                              |
|-----------|--------------------------------------|
| Node      | v22.22.1                             |
| Java      | OpenJDK 21.0.12.1+1 (Temurin 21) — via `JAVA_HOME=$HOME/cva-tools/jdk21` on source machine |
| Gradle    | 8.9 (wrapper `gradlew`, distribution pinned in `gradle/wrapper/gradle-wrapper.properties`) |
| AGP       | com.android.application **8.5.2**    |
| Kotlin    | org.jetbrains.kotlin.plugin.compose **2.0.20** |
| Android SDK | minSdk 26 · target/compileSdk 35 — root `local.properties` has `sdk.dir=/home/arif-moin/cva-tools/android-sdk`; **on your laptop set your own `sdk.dir`** (Android Studio path or `~/Android/Sdk`) |

## 4. Ports

| Port | Purpose        | Notes                                        |
|------|----------------|----------------------------------------------|
| 8080 | dev file share | `python3 -m http.server 8080` from `~/cva-share` (sideload/preview) — LAN only |
| 8081 | API backend    | `cd tools/backend && PORT=8081 node server.js` (avoids clash w/ 8080) |
| prod | Firebase functions | HTTP(S) URL post-deploy; `EXPOSURE_BASE_URL` rebuild |

LAN cleartext permission (`10.1.35.207`) in `network_security_config.xml` is dev-only; the deployed HTTPS URL needs no entry.

## 5. How to run

### Backend
```bash
cd tools/backend
npm install          # once
PORT=8081 node server.js
```
`.env` (recreate fresh — see §11) supplies SMTP_URL / EMAIL_FROM / optional HIBP_API_KEY.

### Android
```bash
export JAVA_HOME=$HOME/cva-tools/jdk21     # adapt to your JDK 21 path
./gradlew assembleDebug                    # or assembleRelease
./gradlew testDebugUnitTest                # test suite incl. SCENARIO-ACCURACY 40/40 gate
```

## 6. Environment variables — NAMES only (never values)

| File that holds them                      | Variable names                                                        |
|-------------------------------------------|------------------------------------------------------------------------|
| `~/.gradle/gradle.properties` (user-global gradle dir; create fresh — see §11) | `GEMINI_API_KEY` · `EXPOSURE_BASE_URL` · `CVA_RELEASE_STORE_PASSWORD` · `CVA_RELEASE_KEY_PASSWORD` |
| `tools/backend/.env` (create fresh — §11) | `PORT` (runtime), `SMTP_URL`, `EMAIL_FROM`, `HIBP_API_KEY` (optional; XposedOrNot default needs no key) |
| `tools/functions/functions/.env.example`  | same-name template for the Functions deploy (committed by design)     |
| `.firebase/`                              | CLI session cache (outside repo; managed by firebase-tools)           |

- `GEMINI_API_KEY` → consumed by `app/build.gradle.kts` into BuildConfig → `GeminiClient`.
- `EXPOSURE_BASE_URL` → consumed by `app/build.gradle.kts` into BuildConfig → `ExposureApi`.
- Keystore pass names (`CVA_RELEASE_*`) → `app/build.gradle.kts` signingConfig.

## 7. APK signing keystore

- **Path (source machine)**: `/home/arif-moin/CyberVictimAssistant/cva-release.jks` (repo root, excluded by .gitignore)
- Key alias: `cva`
- Secrets: passwords only (NAMES `CVA_RELEASE_STORE_PASSWORD` / `CVA_RELEASE_KEY_PASSWORD` from `~/.gradle/gradle.properties` above).

## 8. portal_knowledge_map.md

Does **not exist** in this project (searched the entire tree) — nothing was omitted; the portal knowledge is in `app/src/main/assets/kb/*.json` instead.

## 9. Secrets posture

- `tools/backend/.env` — carries real SMTP URL + credentials → **excluded from commit; recreate per §11**.
- `~/cva-share/fb-login.html` — dev-only link file (outside repo).
- `~/.gradle/gradle.properties` — recreate per §11 (contains the four names above with values you generate fresh).
- Repo `gradle.properties` in the tree has `GEMINI_API_KEY=` **empty placeholder only** → excluded from commit anyway per policy.
- `tools/functions/functions/.env.example` — placeholder template → **committed** (it is theASON template you copy to `functions/.env` on deploy).

## 10. Deployment path next (reminder)

1. `firebase login` on your machine (interactive browser loop) — or service-account JSON route (Google service-account key path NOT committed: `client_secret*.json` / `service-account*.json` gitignored).
2. `tools/functions/DEPLOY.md` — project id + functions deploy steps.
3. Rebuild release APK with `EXPOSURE_BASE_URL=https://<function-url>`.

## 11. Secrets to recreate on the new laptop

NO pendrive required — nothing secret is transferred. Generate everything fresh, drop it into the files below.

### 11.1 SMTP sender account + app password → `tools/backend/.env`

Variables to fill: `SMTP_URL` (`smtp://USER:APP_PASSWORD@HOST:PORT`), `EMAIL_FROM`.
How: create a throwaway sender mailbox (Gmail works), enable 2-Step-Verification, then generate a
Google account **App Password** (16 chars). Compose (hour template, values only you type):

```
SMTP_URL=smtp://<sender-email>:<app-password>@smtp.gmail.com:587
EMAIL_FROM=Cyber Victim Assistant <sender-email>
```

Copy `tools/backend/.env.example` → `tools/backend/.env` and replace placeholders. For the Functions
deploy, the same values belong in `tools/functions/functions/.env` (copy from `tools/functions/.env.example`).

### 11.2 Gemini API key → `~/.gradle/gradle.properties`

Variable: `GEMINI_API_KEY`. How: Google AI Studio (aistudio.google.com) → `Get API key` → Create API key (free tier is enough).
Paste into the user-global gradle properties file (create it: `mkdir -p ~/.gradle && cp gradle.properties.example ~/.gradle/gradle.properties` from the repo root, then edit).

### 11.3 Release signing keystore → `cva-release.jks` at repo root + its passwords in `~/.gradle/gradle.properties`

`app/build.gradle.kts` already points the signingConfig at `../cva-release.jks` with alias `cva` — so your fresh keystore must keep that exact filename and alias, and its two passwords must match the gradle properties names. Run from the repo ROOT:

```bash
keytool -genkeypair -v \
  -keystore cva-release.jks \
  -alias cva \
  -keyalg RSA -keysize 2048 -validity 10000 \
  -storepass <STORE_PASSWORD> -keypass <KEY_PASSWORD> \
  -dname "CN=Cyber Victim Assistant, OU=HackCBS9, O=vijjada, L=City, S=State, C=IN"
```

Then set in `~/.gradle/gradle.properties`: `CVA_RELEASE_STORE_PASSWORD=<STORE_PASSWORD>` and `CVA_RELEASE_KEY_PASSWORD=<KEY_PASSWORD>`
(choose a strong unique pair; keep a personal backup of the jks file outside any git repo).

### 11.4 EXPOSURE_BASE_URL → `~/.gradle/gradle.properties`

Variable: `EXPOSURE_BASE_URL` (consumed by `app/build.gradle.kts` into BuildConfig → `ExposureApi`).
How:
- **Production**: after `firebase deploy --only functions` (see `tools/functions/DEPLOY.md`), set to the deployed function URL, e.g. `https://<region>-<project-id>.cloudfunctions.net/cvaExposure`.
- **Laptop-dev only**: run the backend on `PORT=8081` and set `EXPOSURE_BASE_URL=http://<your-LAN-IP>:8081`; additionally add your IP to `app/src/main/res/xml/network_security_config.xml` (it currently whitelists only `10.1.35.207`, the source machine's LAN address). HTTPS URLs need no config entry.

---

## 12. Suspicious Email Check Feature (Added for Hackathon)

### 12.1 Overview & Architecture
The "Suspicious Email Check" feature allows a victim to inspect emails for phishing indicators with strict privacy:
- **Two Import Paths**:
  - **Path A (No-inbox-access, default & always available)**: Victim shares an email via Android share intent (`ACTION_SEND`), picks an `.eml` file, or pastes raw headers/body. Works with any mail provider.
  - **Path B (Gmail window, Gmail only)**: OAuth 2.0 sign-in with PKCE using read-only scope `https://www.googleapis.com/auth/gmail.readonly`.
- **Gmail Search Window Rules**:
  - Device timezone displayed clearly with **IST** (`Asia/Kolkata`).
  - Default window: incident time ± 24 hours.
  - **7-day hard cap**: searches wider than 7 days are strictly refused.
  - Query: Gmail search `after:<epoch_sec> before:<epoch_sec>` with `includeSpamTrash=true` (phishing often lands in Spam).
  - **Step 1 (Metadata only)**: Fetches `format=metadata` for at most 50 messages: `From, Reply-To, Return-Path, To, Subject, Date, Message-ID, Authentication-Results, Received`. Zero bodies or attachments downloaded. Candidates scored locally with plain-English reason.
  - **Step 2 (Import)**: Victim ticks specific emails. Only ticked emails have raw message fetched (`format=raw`). **All unticked candidate metadata is purged from memory immediately.**
  - **Token Security**: Access token is held in-memory ONLY. Never saved to Room, SharedPreferences, or disk. Zero refresh tokens stored. "Disconnect Gmail" button calls Google's revocation endpoint and clears memory.
- **Safety Invariants**:
  - Links are rendered as **inert plain text only**; never clickable, never pre-fetched.
  - Never render HTML emails in WebView.
  - Attachments are never executed or previewed; metadata and SHA-256 hash only.
  - Raw `.eml` saved in app-private storage with cryptographic SHA-256 hash.
  - Never log email content, addresses, tokens, or subjects.
- **Case Integration**:
  - Adds `email_phishing` event to the case timeline next to other events.
  - Pre-fills National Cyber Crime Reporting Portal category: **"Online and Social Media Related Crime" -> "E-Mail Phishing"** with original `.eml` file and SHA-256 hash referenced in the complaint pack PDF.
  - Emergency checklist and "Draft Bank Fund Freeze Request" when money or credentials may be compromised.
  - Optional Gemini "Explain this email simply" with explicit victim consent each time (redacted text only, no attachments or full headers).

### 12.2 Google OAuth Setup & Testing Mode Limits
To enable live Gmail window searches:
1. Go to [Google Cloud Console](https://console.cloud.google.com/).
2. Create or select a Google Cloud project.
3. Configure the **OAuth Consent Screen**:
   - User Type: **External**.
   - App Name: `Cyber Victim Assistant` (or `Cybridge`).
   - Scopes: Add `https://www.googleapis.com/auth/gmail.readonly` (Restricted Scope).
   - Publishing Status: **Testing**.
   - **CRITICAL TESTING LIMIT**: While in Testing mode, only users added under the **Test users** list in Google Cloud Console can sign in. Add your own Gmail test accounts here before testing.
   - **Public Release Verification Notice**: Restricted scopes like `gmail.readonly` require a formal Google Cloud verification and a CASA (Cloud Application Security Assessment) Tier 2 audit before an app can be published publicly to general users.
4. Create Credentials -> **OAuth Client ID**:
   - Application Type: **Android** (or Web client for PKCE redirect).
   - Package name: `com.cybervictim.assistant`
   - SHA-1 certificate fingerprint: your debug or release keystore SHA-1.
   - Redirect URI: `com.cybervictim.assistant:/oauth2redirect`
5. Copy Client ID and add to `~/.gradle/gradle.properties`:
   ```properties
   GOOGLE_OAUTH_CLIENT_ID=<your-oauth-client-id>.apps.googleusercontent.com
   ```

### 12.3 What to Verify Manually on a Real Phone
1. **Path A (No-Inbox-Access)**:
   - Navigate: More -> **Suspicious email check**.
   - Tap **"Load Test Vector"**: verifies instant on-device analysis of synthetic bank phish, shows word-based rating ("High Suspicion"), top 3 reasons, inert link display, and SHA-256 evidence item.
   - Tap **"Pick .eml File"**: select any `.eml` file from device storage and verify analysis.
   - Test Android Share Intent: in Gmail or another mail app, share an email to Cybridge and verify the app detects it and analyzes it.
2. **Path B (Gmail Window Search)**:
   - Tap the **"Gmail Window (OAuth)"** tab.
   - Tap **"Connect Gmail"**: browser opens Google OAuth consent screen. Sign in with an account configured in Google Cloud Console's Test Users.
   - Upon redirect back to the app, verify status shows "Gmail connected in testing mode".
   - Test the 7-day hard cap: set window wider than 7 days and verify validation error.
   - Set valid window (e.g. ±24 hours) and tap **"Step 1: Find Emails in Window"**: verify candidate list displays subject, sender, and plain-English suspicion reasons.
   - Select one email and tap **"Step 2: Import Selected"**: verify only that email is imported, and unticked candidates vanish from memory.
   - Tap **"Disconnect Gmail"**: verify session is revoked and disconnected.
3. **Emergency Actions & Timeline**:
   - On an imported evidence card, tick "I clicked a link or entered a password".
   - Verify the urgent action checklist appears with the **"Draft Bank Fund Freeze Request"** button.
   - Tap "Draft Bank Fund Freeze Request" -> verify the pre-filled letter includes the incident timestamp, sender, and SHA-256 hash.
   - Go to More -> **Case Tracker** -> verify the timeline shows the `EMAIL_PHISHING` event alongside any phone trace events.

