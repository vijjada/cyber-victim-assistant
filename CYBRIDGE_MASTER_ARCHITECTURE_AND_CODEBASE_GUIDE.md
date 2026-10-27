# Cybridge (Cyber Victim Assistant) — Master Architecture & Codebase Guide
**Comprehensive Technical Specification from Zero to Current Implementation**  
*Document Version: 5.0 | Release: Production Debug (October 2026)*  
*Target Audience: AI Coding Agents, LLMs, Security Auditors, and System Engineers*

---

## 1. Executive Summary & Mission

### 1.1 The Cybercrime Victim Crisis in India
Every day, thousands of citizens across India fall victim to rapid financial cyber fraud—including UPI debit deceptions, Telegram task scams, fake job portals, loan app extortion, phishing emails, and account takeovers. Under the **Indian Cyber Crime Coordination Centre (I4C)** and Ministry of Home Affairs (MHA) framework, victims have a critical **"Golden Hour"** (the first 2 to 4 hours post-incident) to freeze defrauded funds under the **1930 Citizen Financial Cyber Fraud Reporting and Management System (CFCFRMS)**.

However, non-technical victims face debilitating barriers:
1. **Panic and Information Asymmetry**: Victims do not know what technical identifiers are needed (12-digit UTR references, beneficiary VPA/UPI IDs, transaction timestamps, SMS headers).
2. **Complex Portal Taxonomy**: The official `cybercrime.gov.in` portal has multi-level nested categories, confusing legal terminology, and complex form fields. A victim reporting a UPI task fraud under "Hacking" or picking the wrong subcategory delays police lien-marking.
3. **Evidence Destruction**: Critical evidence (chat histories, scam APKs, debit SMS, sender headers) is frequently deleted or lost before filing.
4. **Drafting Deficits**: Police and bank nodal officers require structured legal complaints under Section 66C/66D of the Information Technology Act, Section 318(4) of Bharatiya Nyaya Sanhita (BNS), and Section 91 of CrPC / BNSS.

### 1.2 Cybridge's Core Mission
**Cybridge** (package `com.cybervictim.assistant`) is an on-device, privacy-first Android assistant built with Jetpack Compose, Kotlin Coroutines, Google ML Kit, Android Room Database, and an intelligent companion WebView engine. It acts as an empathetic, expert legal-technical co-pilot that stays with the victim from the first minutes until the case is officially registered.

Cybridge:
- Automatically mines facts from on-device bank SMS and uploaded screenshots using offline OCR.
- Maps the victim's situation to the 100% exact official MHA category and subcategory buttons.
- Mirrors the official `cybercrime.gov.in` interface 1:1, allowing victims to preview their filing.
- Runs an on-device visual background automation engine (inspired by Skyvern) that pre-fills the official portal via DOM injection while providing a real-time "Live Eye" trust monitor.
- Enforces an unbreakable human-in-the-loop security boundary (`ActionPolicy`): AI never submits forms, never bypasses CAPTCHA, and never handles OTPs or bank passwords.
- Generates police-grade legal complaint narratives and official Sec 91 CrPC preservation letters for immediate bank account freezing.

---

## 2. High-Level System Architecture

Cybridge is structured as a reactive, layered architecture where local storage serves as the single source of truth, and external network dependencies are strictly opt-in and privacy-gated.

```
+-----------------------------------------------------------------------------------+
|                                PRESENTATION LAYER                                 |
|  Jetpack Compose Material 3 UI | 12 Indian Languages (L10n) | Tricolor Gov Theme  |
+-----------------------------------------------------------------------------------+
| SplashScreen -> LanguageSelectScreen -> RegisterHubScreen (Main Drawer Hub)       |
|  * GuidedQuestionScreen (8-Step Conversational Questionnaire & Narrative Synth)   |
|  * PortalLoginScreen (1:1 cybercrime.gov.in Update User Profile & Citizen Auth)   |
|  * PortalWebViewScreen (Live Headless/Visible Automated Portal Session)           |
|  * PortalCloneScreen (1:1 Category/Subcategory/Financial/Suspect Complaint Form)  |
|  * EvidenceScreen (Evidence Vault & Image Ingestion)                              |
|  * SuspiciousEmailScreen (RFC 822 EML Parser & Phishing Heuristic Engine)         |
|  * HomeTrack (Case Timeline & Status Tracking)                                    |
|  * MoreScreens (RequestDrafter, PhoneTrace, ExposureChecker, Settings)            |
+-----------------------------------------------------------------------------------+
                                         |
                                         v
+-----------------------------------------------------------------------------------+
|                              INTELLIGENCE & AI LAYER                              |
+-----------------------------------------------------------------------------------+
|  * OcrPipeline: Fully on-device Google ML Kit Text Recognition + Regex Fact Miner  |
|  * SmsScamExtractor: Android ContentResolver SMS Ingestion + Bank Debit Matcher   |
|  * ComplaintScriptGenerator: Deterministic Legal Narrative Synthesizer (IT Act)   |
|  * PhishingAnalyzer: SPF/DKIM/DMARC Evaluator, Urgency & Lookalike Domain Scorer  |
|  * PlanBuilder & KbLoader: MHA Portal Map Grounding Engine (100% Deterministic)   |
|  * GeminiClient: Cloud-assisted Scenario Refinement (Strict JSON Fallback)        |
+-----------------------------------------------------------------------------------+
                                         |
                                         v
+-----------------------------------------------------------------------------------+
|                          AUTOMATION & COMPANION ENGINE                            |
+-----------------------------------------------------------------------------------+
|  * PortalAutoFillEngine: JavaScript DOM Injector, XPath Resolver, Visual Glow     |
|  * Skyvern-Style Visual Trust Eye: Green Highlight Rectangles (#00C853)           |
|  * ActionPolicy: Strict VICTIM_LOCK invariant for Final Submit, OTP, and CAPTCHA   |
|  * Real-Time Trust Audit Receipts: Cryptographic event logs displayed to citizen  |
+-----------------------------------------------------------------------------------+
                                         |
                                         v
+-----------------------------------------------------------------------------------+
|                           DATA & PERSISTENCE LAYER                                |
+-----------------------------------------------------------------------------------+
|  * Room Database (CaseDb): EvidenceItem, CaseFact, CaseEvent                      |
|  * UserProfileStore: SharedPreferences (Full Name, Mobile, Address, Victim ID)   |
|  * SecPrefs: EncryptedSharedPreferences (AES-256 GCM) for Auth Tokens             |
|  * CaseSession & CaseSessionStore: Dynamic Multi-Session Fact Correlation         |
+-----------------------------------------------------------------------------------+
```

---

## 3. Order of Screens & Complete Victim Journey

The victim user journey is carefully engineered to eliminate friction while preserving complete agency and legal validity.

```
[1. Splash Screen]
       |
       v
[2. Language Selection & Profile Setup] (12 Indian Languages, Speech Rate, Victim ID)
       |
       v
[3. Register Hub (Main Dashboard)] (Emergency 1930 Call, Drawer, Register Button)
       |
       +-------------------------------+-------------------------------+
       |                               |                               |
       v                               v                               v
[4. Guided Questionnaire]      [5. Portal Login]            [Drawer Menu Services]
 (8-Step Chat / Voice /         (1:1 cybercrime.gov.in       * Evidence Vault
  SMS & OCR Extraction)          Profile Form & Auth)        * Request Drafter
       |                               |                     * Phishing Email Check
       v                               v                     * PhoneTrace Analyzer
[Legal Script Generated]        [Portal WebView Auth]        * Breach Exposure
       |                         (OTP & CAPTCHA solved)      * Case Timeline
       +---------------+---------------+
                       |
                       v
         [6. 1:1 Portal Complaint Clone]
          * Step 0: Category Selection
          * Step 1: Subcategory & Incident
          * Step 2: Financial Fraud Details
          * Step 3: Suspect Details
          * Step 4: Complainant Details
          * Step 5: Incident Narrative
          * Step 6: Preview & Submit
                       |
                       v
     [7. Background Visual Auto-Fill Engine]
      (Skyvern DOM injection + Live Eye Sheet)
                       |
                       v
         [8. Official Token Registered]
          (Call 1930 with Token to Freeze)
```

### Detailed Screen Walkthrough:

#### Screen 1: Splash Screen (`SplashScreen.kt`)
- **Visuals**: Pure white background, centered 3D Cybridge crest logo (`ic_launcher_round`), brand title "Cybridge", and subtitle "Report · Get Help · Stay Safe".
- **Bottom Accent**: Official Indian Tricolor gradient wave (`tricolor_wave.png`).
- **Timing**: Auto-transitions after 1200ms to `LanguageSelectScreen`.

#### Screen 2: Language Selection & Profile Hub (`LanguageSelectScreen.kt`)
- **Localization**: Supports 12 languages: English (EN), Hindi (HI), Telugu (TE), Kannada (KN), Malayalam (ML), Punjabi (PA), Odia (OR), Assamese (AS), Gujarati (GU), Marathi (MR), Tamil (TA), Bengali (BN).
- **Speech Controls**: Speech rate slider (0.6x to 1.4x) with "Hear a sample" preview.
- **Victim Identity Generation**: Generates an anonymous, persistent Unique Victim Identifier (e.g., `VIC-2026-84920-F3A1`) stored in `UserProfileStore`.
- **Profile Modal**: Allows victim to pre-fill their legal reporting details (Full Name, Mobile, Gender, DOB, State, District, Police Station, Address, PIN Code).

#### Screen 3: Register Hub (`RegisterHubScreen.kt`)
- **Visuals**: Clean, modern dashboard with pure white container, deep navy primary action button (`#0B2240`), and zero distracting emojis.
- **Hero CTA**: "Register a Complaint" button routing directly into the reporting flow.
- **Helpline Card**: Immediate dialer for National Cyber Crime Helpline `1930` (one-tap launch via `Intent.ACTION_DIAL`).
- **Navigation Drawer**: 3-line hamburger menu granting instant access to Evidence Vault, Request Letters, Suspicious Email Analyzer, Tracking, PhoneTrace, and Breach Checker.

#### Screen 4: Guided 8-Step Questionnaire (`GuidedQuestionScreen.kt`)
- **Conversational Wizard**: Breaks down overwhelming cyber fraud into 8 simple questions:
  1. *Amount Paid* (Rupees transferred)
  2. *Debited Bank / Wallet* (SBI, HDFC, GPay, PhonePe, etc.)
  3. *Incident Timestamp* (Date and time money left account)
  4. *Suspect UPI / Account* (Destination beneficiary handle)
  5. *UTR / Txn Reference* (12-digit bank reference)
  6. *Method of Contact* (WhatsApp, Telegram group, phone call, SMS)
  7. *Method of Compromise* (Malicious link, APK, AnyDesk, fake job task)
  8. *Legal Narrative Review* (Auto-synthesized police complaint)
- **Zero Mock Fallbacks**: Input fields start completely blank with dynamic placeholders.
- **SMS Detection Card**: Automatically reads SMS inbox around the selected date/time and displays a one-tap "Fill from detected Bank SMS" chip.
- **OCR Evidence Ingestion**: Allows uploading chat screenshots or statements, automatically mining facts on-device.

#### Screen 5: 1:1 Official Portal Profile Screen (`PortalLoginScreen.kt`)
- **Exact Gov Portal Mirror**: 100% pixel-perfect recreation of `cybercrime.gov.in/dashboard/update-profile`.
- **Emblem & I4C Header**: Government emblem, Indian flag, MHA/I4C logos.
- **Session Info Strip**: Live 59:49 countdown timer and User ID mobile badge.
- **Mandatory Profile Fields**: Name, Mobile, Gender dropdown, DOB, Email, State/UT dropdown, District, Police Station, Address (with 5–190 char counter), Pin Code.
- **AI Identity Confirmation**: On tapping "Update", automatically saves details and routes to portal authentication.

#### Screen 6: Official Portal Companion WebView (`PortalWebViewScreen.kt`)
- **Session Bridge**: Navigates to the live official portal `https://cybercrime.gov.in/dashboard/crimereport`.
- **Automated Login Detection**:
  - Monitors URL navigation patterns (`/dashboard/crimereport`, `/dashboard/index`, `/dashboard/update-profile`).
  - Evaluates JavaScript cookies (`PHPSESSID`, `JSESSIONID`).
  - Inspects DOM for active session markers (`a[href*='logout']`, `.user-profile`, `#btnLogout`).
- **Seamless Return**: Automatically dismisses itself and redirects the user back to the native app the moment login is completed.

#### Screen 7: 1:1 Portal Complaint Clone (`PortalCloneScreen.kt`)
- **Multi-Step Complaint Flow**:
  - *Step 0 (Category)*: Financial Fraud, Social Media Crimes, Cyber Bullying/Stalking, Fake Calls/Extortion, Deepfakes/Slavery.
  - *Step 1 (Incident Info)*: Subcategory, platform used, incident date/time, delay reason.
  - *Step 2 (Financial Details)*: Amount, bank name, UTR number, victim UPI, suspect UPI, quick bank selector chips.
  - *Step 3 (Suspect Details)*: Known phone numbers, emails, social media handles, websites.
  - *Step 4 (Complainant Details)*: Auto-synced from `userProfile`.
  - *Step 5 (Legal Narrative)*: Auto-generated legal complaint script.
  - *Step 6 (Preview & Submit)*: Legal declaration checkbox, simulated acknowledgment token generation (`CVA-2026-XXXXX`), and 1930 freeze prompt.

#### Screen 8: Evidence Vault (`EvidenceScreen.kt`)
- **Ingestion**: Upload screenshots, bank statements, or PDFs.
- **On-Device OCR**: Runs `OcrPipeline` locally, displaying extracted facts (`upi_id`, `txn_ref`, `amount`, `phone`).
- **Fact Timeline**: Displays every piece of evidence alongside its extracted cryptographic hash and timestamp.

#### Screen 9: Suspicious Email Analyzer (`SuspiciousEmailScreen.kt`)
- **Dual Ingestion**: Manual RFC 822 `.eml` header paste or Google OAuth PKCE Gmail search.
- **Deep Heuristics**: Analyzes SPF/DKIM/DMARC pass/fail flags, lookalike phishing domains, urgency indicators, and mismatched links.

---

## 4. The Skyvern-Style Background AI Automation Engine

One of Cybridge's most advanced technical achievements is its **Visual Trust Background Automation Engine**, implemented across [`PortalAutoFillEngine.kt`](file:///home/hp/CyberVictimAssistant/app/src/main/java/com/cybervictim/assistant/portal/PortalAutoFillEngine.kt) and [`PortalCloneScreen.kt`](file:///home/hp/CyberVictimAssistant/app/src/main/java/com/cybervictim/assistant/ui/screens/PortalCloneScreen.kt).

### 4.1 The Victim Trust Problem in Browser Automation
When background AI agents automate web tasks (like Selenium or Playwright), the user is left in the dark, leading to anxiety and mistrust. Victims of cybercrime are already in a heightened state of distress; they must have 100% certainty that the AI is accurately representing their case on the official government portal.

### 4.2 Architectural Solution: Visual Verification & Real-Time Trust Receipts
Cybridge solves this by implementing an on-device visual engine inspired by Skyvern:
1. **Remote Debugging Port**: Enables `WebView.setWebContentsDebuggingEnabled(true)` so developers and users can inspect the session via Chrome DevTools.
2. **Visual Bounding Box Injection**: When the AI agent interacts with any DOM input, select box, or radio button, it injects a high-contrast glowing outline (`outline: 3px solid #00C853; box-shadow: 0 0 12px rgba(0,200,83,0.85);`).
3. **Smooth Auto-Scroll**: Uses `element.scrollIntoView({ behavior: 'smooth', block: 'center' })` before triggering focus or value changes.
4. **Visual Automation Indicator Overlay**: Injects an in-page floating trust badge:
   ```html
   <div id="cva-trust-indicator" style="position:fixed;top:10px;right:10px;background:#004D40;color:#00E676;padding:6px 14px;border-radius:20px;font-family:monospace;font-size:11px;font-weight:bold;z-index:999999;box-shadow:0 2px 10px rgba(0,0,0,0.3);border:1px solid #00E676;">
     AI ASSISTANT ACTIVE &bull; DOM SYNCED
   </div>
   ```
5. **Real-Time Trust Audit Receipts**: Every action performed generates a cryptographic, timestamped audit entry in a reactive list (`portalAuditReceipts`):
   - `[12:05:01] Subcategory selected: UPI Related Fraud`
   - `[12:05:02] Debited bank field auto-filled: State Bank of India`
   - `[12:05:03] Transaction UTR reference entered: 123456789012`
   - `[12:05:04] Defrauded amount synced: Rs. 25,000`
6. **Watch Live AI Operation Sheet**: The user can open a live bottom sheet containing the active portal WebView at any moment, visually observing the green outlines jumping from field to field in real time.

---

## 5. Security Invariant & ActionPolicy

A core architectural pillar of Cybridge is the **Immutable Action Policy** defined in [`PortalAutoFillEngine.kt`](file:///home/hp/CyberVictimAssistant/app/src/main/java/com/cybervictim/assistant/portal/PortalAutoFillEngine.kt).

### 5.1 The Safety Boundary (`StepOwner`)
```kotlin
enum class StepOwner {
    AI_AUTO,       // Safe for background auto-fill (amounts, banks, dates, names, narratives)
    VICTIM_LOCK,   // STRICTLY PROHIBITED FOR AI (Final submission, Preview, Declaration)
    HUMAN_ONLY     // Strictly human interaction (CAPTCHA, OTP, Netbanking passwords)
}
```

### 5.2 Mandatory Rules
1. **Zero AI Submission**: The final "Submit Complaint", "Confirm", or "Register" button on the official portal is permanently marked `VICTIM_LOCK`. The AI script locator will throw an `IllegalStateException` if code attempts to auto-trigger a submission button.
2. **Zero Credential / OTP Handling**: The AI engine never reads, stores, or inputs citizen bank passwords, OTPs, or CAPTCHA answers. The user logs into the official portal themselves via `PortalWebViewScreen.kt`.
3. **Audit Trail**: Every automated field assignment is recorded in `portalAuditReceipts` and `CaseDb.CaseEvent` for legal auditability.

---

## 6. On-Device Intelligence & Ingestion Pipelines

Cybridge runs all primary extraction pipelines 100% on-device, ensuring sensitive victim evidence never leaves the phone.

### 6.1 Google ML Kit On-Device OCR Pipeline (`OcrPipeline.kt`)
- **Downsampled Memory-Safe Decoding**: Downsamples large phone camera images to a max side of 1600px using `BitmapFactory.Options.inSampleSize` with `RGB_565` configuration to prevent Out-Of-Memory (OOM) crashes.
- **Latin Text Recognition**: Utilizes `com.google.mlkit:text-recognition:16.0.1` locally on worker threads.
- **Regex Fact Mining**:
  - *Phone Numbers*: `(?:\\+91[-\\s]?|0)?[6-9]\\d{9}\\b`
  - *Defrauded Amounts*: `(?:₹|rs\\.?|inr)\\s?\\d{1,3}(?:[,\\s]\\d{2,3})*(?:\\.\\d{1,2})?`
  - *UPI Identifiers*: `[a-zA-Z][a-zA-Z0-9._\\-]{2,60}@[a-zA-Z][a-zA-Z0-9]{2,24}` validated against 27 known bank suffixes (`oksbi`, `okhdfcbank`, `ybl`, `paytm`, `axl`, etc.).
  - *UTR Reference Numbers*: 9 to 24 character transaction hashes following keywords (`UTR`, `Txn`, `Ref No`).
  - *Timestamps*: Standard 12-hour and 24-hour time signatures with dates.

### 6.2 SMS Scam Extractor (`SmsScamExtractor.kt`)
- **Android ContentResolver Query**: Reads `content://sms/inbox` filtering for bank senders (`SBI`, `HDFC`, `ICICI`, `AXIS`, `PNB`, `KOTAK`, `CANARA`, `BOB`, `UNIONB`, etc.).
- **Proximity Window Scoring**: Correlates SMS timestamps against the victim's estimated incident window ($T \pm 2\text{ hours}$).
- **Zero Mock Policy**: If no matching SMS is found or permissions are absent, it returns an empty list, never synthesizing artificial transactions.

### 6.3 Police-Grade Complaint Narrative Generator (`ComplaintScriptGenerator.kt`)
Synthesizes professional, court-admissible legal complaint narratives tailored to specific statutes:
- **Financial Fraud**: Under Section 1930 CFCFRMS, Section 66D IT Act (cheating by personation), and Section 318(4) Bharatiya Nyaya Sanhita (BNS - cheating and dishonestly inducing delivery of property).
- **Account Hijacking / Hacking**: Under Section 66C (identity theft) and Section 66D of the IT Act, requesting emergency preservation under Section 91 CrPC / BNSS.

---

## 7. Phishing & Suspicious Email Engine

Implemented in the `com.cybervictim.assistant.email` package:
- [`EmlParser.kt`](file:///home/hp/CyberVictimAssistant/app/src/main/java/com/cybervictim/assistant/email/EmlParser.kt): Pure RFC 822 / RFC 5322 MIME stream parser. Extracts `From`, `Reply-To`, `Return-Path`, `Subject`, `Date`, `Authentication-Results`, and parses HTML/text bodies for URLs.
- [`PhishingAnalyzer.kt`](file:///home/hp/CyberVictimAssistant/app/src/main/java/com/cybervictim/assistant/email/PhishingAnalyzer.kt):
  - Evaluates `Authentication-Results` for `spf=fail`, `dkim=fail`, and `dmarc=fail`.
  - Calculates domain lookalike scores (detects permutations of major banks like `onlinesbi-kyc.top` mimicking `onlinesbi.sbi`).
  - Detects urgent psychological manipulation triggers ("Account blocked within 2 hours", "KYC suspended", "Electricity disconnection").
  - Mismatched Hyperlinks: Flags links where the visible anchor text (`https://onlinesbi.sbi`) differs from the actual href target.
- [`GmailApiClient.kt`](file:///home/hp/CyberVictimAssistant/app/src/main/java/com/cybervictim/assistant/email/GmailApiClient.kt): Uses Google OAuth 2.0 PKCE with read-only scope (`https://www.googleapis.com/auth/gmail.readonly`). Searches emails within the incident window metadata-first, allowing the user to select which email to import into the case timeline.

---

## 8. Data Storage & Privacy Architecture

### 8.1 Room Database (`CaseDb.kt`)
Stored locally in private app storage (`case_store.db`):
- `EvidenceItem`:
  - `id: Long` (Auto-increment PK)
  - `caseId: String`
  - `displayName: String`
  - `uri: String`
  - `kind: String` ("screenshot", "document", "email")
  - `extractedFactIds: String` (JSON array)
- `CaseFact`:
  - `id: Long` (Auto-increment PK)
  - `caseId: String`
  - `type: String` ("amount", "bank", "timestamp", "txn_ref", "upi_id", "phone")
  - `valueText: String`
  - `sourceEvidenceId: Long?`
- `CaseEvent`:
  - `id: Long` (Auto-increment PK)
  - `caseId: String`
  - `kind: String` ("evidence_added", "fact_extracted", "complaint_generated")
  - `detail: String`
  - `timestamp: Long`

### 8.2 User Profile Store (`UserProfileStore.kt`)
Stores non-sensitive complainant contact information in private SharedPreferences (`victim_user_profile.xml`):
- `full_name`, `mobile`, `gender`, `dob`, `email`, `state`, `district`, `police_station`, `address`, `pincode`, `ack_number`, `complaint_status`, `victim_id`.

### 8.3 Encrypted Security Preferences (`SecPrefs.kt`)
Utilizes `androidx.security.crypto.EncryptedSharedPreferences` with `AES256_SIV` master keys and `AES256_GCM` scheme for storing authentication session tokens. If any API endpoint returns HTTP 401 Unauthorized, the token is automatically wiped from memory and storage.

---

## 9. Comprehensive Codebase File Index

Below is the complete catalog of all 48 source files across the project:

### Package: `com.cybervictim.assistant` (Root)
| File | Duty & Core Responsibilities |
|---|---|
| [`MainActivity.kt`](file:///home/hp/CyberVictimAssistant/app/src/main/java/com/cybervictim/assistant/MainActivity.kt) | Single-activity entry point. Configures Edge-to-Edge display, initializes Room DB reference, and hosts `AppNav`. |

### Package: `com.cybervictim.assistant.ui` (Navigation & Theme)
| File | Duty & Core Responsibilities |
|---|---|
| [`AppNav.kt`](file:///home/hp/CyberVictimAssistant/app/src/main/java/com/cybervictim/assistant/ui/AppNav.kt) | Central Compose Navigation Host defining all 17 routes (`Routes.SPLASH`, `Routes.LANG_SELECT`, `Routes.REGISTER_HUB`, `Routes.PORTAL_LOGIN`, `Routes.PORTAL_WEBVIEW`, `Routes.PORTAL_CLONE`, etc.). |
| [`AppColors.kt`](file:///home/hp/CyberVictimAssistant/app/src/main/java/com/cybervictim/assistant/ui/AppColors.kt) | Master color palette. Implements official Indian Tricolor tones (`GovOrange`, `GovGreen`, `GovNavy`, `TealPrimary`, `DarkText`, `CardBg`). |
| [`BottomBar.kt`](file:///home/hp/CyberVictimAssistant/app/src/main/java/com/cybervictim/assistant/ui/BottomBar.kt) | Bottom navigation bar component for secondary utility screens. |
| [`L10n.kt`](file:///home/hp/CyberVictimAssistant/app/src/main/java/com/cybervictim/assistant/ui/L10n.kt) | Master localization engine supporting 12 Indian languages with persisted language selection. |

### Package: `com.cybervictim.assistant.ui.screens` (Screens)
| File | Duty & Core Responsibilities |
|---|---|
| [`SplashScreen.kt`](file:///home/hp/CyberVictimAssistant/app/src/main/java/com/cybervictim/assistant/ui/screens/SplashScreen.kt) | Animated 1200ms branded splash screen with 3D Cybridge crest and tricolor base. |
| [`LanguageSelectScreen.kt`](file:///home/hp/CyberVictimAssistant/app/src/main/java/com/cybervictim/assistant/ui/screens/LanguageSelectScreen.kt) | 12-language selector, speech rate configuration, audio preview, profile dialog, and AI Victim ID badge. |
| [`RegisterHubScreen.kt`](file:///home/hp/CyberVictimAssistant/app/src/main/java/com/cybervictim/assistant/ui/screens/RegisterHubScreen.kt) | Primary home landing hub. Clean white UI, Deep Navy "Register a Complaint" CTA, 1930 Helpline, and navigation drawer. |
| [`GuidedQuestionScreen.kt`](file:///home/hp/CyberVictimAssistant/app/src/main/java/com/cybervictim/assistant/ui/screens/GuidedQuestionScreen.kt) | 8-step conversational questionnaire wizard, SMS one-tap fill, evidence upload, and legal complaint synthesizer. |
| [`PortalLoginScreen.kt`](file:///home/hp/CyberVictimAssistant/app/src/main/java/com/cybervictim/assistant/ui/screens/PortalLoginScreen.kt) | 1:1 pixel-perfect recreation of `cybercrime.gov.in` Update User Profile interface with session countdown. |
| [`PortalWebViewScreen.kt`](file:///home/hp/CyberVictimAssistant/app/src/main/java/com/cybervictim/assistant/ui/screens/PortalWebViewScreen.kt) | Official portal WebView companion. Auto-detects OTP/CAPTCHA authentication completion and routes back to native app. |
| [`PortalCloneScreen.kt`](file:///home/hp/CyberVictimAssistant/app/src/main/java/com/cybervictim/assistant/ui/screens/PortalCloneScreen.kt) | Complete 1:1 complaint filing interface matching MHA portal steps, integrating background visual automation. |
| [`EvidenceScreen.kt`](file:///home/hp/CyberVictimAssistant/app/src/main/java/com/cybervictim/assistant/ui/screens/EvidenceScreen.kt) | Evidence Vault screen. Ingests screenshots/PDFs, displays extracted facts and cryptographic timestamps. |
| [`SuspiciousEmailScreen.kt`](file:///home/hp/CyberVictimAssistant/app/src/main/java/com/cybervictim/assistant/ui/screens/SuspiciousEmailScreen.kt) | Phishing email analyzer. Supports raw EML paste, file picker, and Google OAuth PKCE Gmail search. |
| [`HomeScreen.kt`](file:///home/hp/CyberVictimAssistant/app/src/main/java/com/cybervictim/assistant/ui/screens/HomeScreen.kt) | Legacy home overview screen. |
| [`HomeTrack.kt`](file:///home/hp/CyberVictimAssistant/app/src/main/java/com/cybervictim/assistant/ui/screens/HomeTrack.kt) | Case tracking dashboard displaying timeline events, police updates, and next required actions. |
| [`GuideScreen.kt`](file:///home/hp/CyberVictimAssistant/app/src/main/java/com/cybervictim/assistant/ui/screens/GuideScreen.kt) | Step-by-step interactive guidance based on grounded MHA portal manuals. |
| [`PortalGuideScreen.kt`](file:///home/hp/CyberVictimAssistant/app/src/main/java/com/cybervictim/assistant/ui/screens/PortalGuideScreen.kt) | Visual guide explaining each official portal button and requirement. |
| [`TellUsScreen.kt`](file:///home/hp/CyberVictimAssistant/app/src/main/java/com/cybervictim/assistant/ui/screens/TellUsScreen.kt) | Initial category triage screen matching MHA official incident buttons. |
| [`MoreMenuScreen.kt`](file:///home/hp/CyberVictimAssistant/app/src/main/java/com/cybervictim/assistant/ui/screens/MoreMenuScreen.kt) | Navigation menu linking secondary tools (PhoneTrace, Requests, Exposure, Settings). |
| [`MoreScreens.kt`](file:///home/hp/CyberVictimAssistant/app/src/main/java/com/cybervictim/assistant/ui/screens/MoreScreens.kt) | Implementation of Settings, Privacy, Help, and About dialogs. |

### Package: `com.cybervictim.assistant.portal` (Automation & Guidance)
| File | Duty & Core Responsibilities |
|---|---|
| [`PortalAutoFillEngine.kt`](file:///home/hp/CyberVictimAssistant/app/src/main/java/com/cybervictim/assistant/portal/PortalAutoFillEngine.kt) | Background DOM automation engine. Injects green highlight glow (`#00C853`), auto-scrolls, executes XPath scripts, and enforces `ActionPolicy`. |
| [`PortalDriver.kt`](file:///home/hp/CyberVictimAssistant/app/src/main/java/com/cybervictim/assistant/portal/PortalDriver.kt) | WebView controller interface for script execution and DOM event dispatching. |
| [`PlanBuilder.kt`](file:///home/hp/CyberVictimAssistant/app/src/main/java/com/cybervictim/assistant/portal/PlanBuilder.kt) | Deterministic keyword scenario matcher (100% test pass rate) routing incidents to official portal sequences. |

### Package: `com.cybervictim.assistant.ai` (Intelligence & Legal Synthesis)
| File | Duty & Core Responsibilities |
|---|---|
| [`ComplaintScriptGenerator.kt`](file:///home/hp/CyberVictimAssistant/app/src/main/java/com/cybervictim/assistant/ai/ComplaintScriptGenerator.kt) | Synthesizes police-grade complaint scripts under Sec 66D IT Act, Sec 318(4) BNS, and 1930 protocols. |
| [`CaseExtractor.kt`](file:///home/hp/CyberVictimAssistant/app/src/main/java/com/cybervictim/assistant/ai/CaseExtractor.kt) | Structured schema definitions for case facts and entity extraction. |
| [`GeminiClient.kt`](file:///home/hp/CyberVictimAssistant/app/src/main/java/com/cybervictim/assistant/ai/GeminiClient.kt) | Client for Google Gemini API (`gemini-1.5-flash`), operating with strict JSON schema validation. |
| [`KbLoader.kt`](file:///home/hp/CyberVictimAssistant/app/src/main/java/com/cybervictim/assistant/ai/KbLoader.kt) | Loads and parses bundled JSON knowledge bases (`portal_map.json`, `scenario_buttons.json`). |
| [`CaseSessionStore.kt`](file:///home/hp/CyberVictimAssistant/app/src/main/java/com/cybervictim/assistant/ai/CaseSessionStore.kt) | In-memory session fact store and reactive notifier. |

### Package: `com.cybervictim.assistant.evidence` & `sms`
| File | Duty & Core Responsibilities |
|---|---|
| [`OcrPipeline.kt`](file:///home/hp/CyberVictimAssistant/app/src/main/java/com/cybervictim/assistant/evidence/OcrPipeline.kt) | On-device Google ML Kit text recognition and regex fact extraction engine. |
| [`SmsScamExtractor.kt`](file:///home/hp/CyberVictimAssistant/app/src/main/java/com/cybervictim/assistant/sms/SmsScamExtractor.kt) | Ingests bank debit SMS messages and extracts amounts, banks, timestamps, and UTR numbers. |

### Package: `com.cybervictim.assistant.email` (Phishing Analysis)
| File | Duty & Core Responsibilities |
|---|---|
| [`EmlParser.kt`](file:///home/hp/CyberVictimAssistant/app/src/main/java/com/cybervictim/assistant/email/EmlParser.kt) | Pure RFC 822 MIME parser for email headers and message bodies. |
| [`PhishingAnalyzer.kt`](file:///home/hp/CyberVictimAssistant/app/src/main/java/com/cybervictim/assistant/email/PhishingAnalyzer.kt) | Heuristic phishing detector (SPF/DKIM/DMARC flags, lookalikes, urgency, URL discrepancies). |
| [`GmailApiClient.kt`](file:///home/hp/CyberVictimAssistant/app/src/main/java/com/cybervictim/assistant/email/GmailApiClient.kt) | Google OAuth 2.0 PKCE client for read-only Gmail access within the incident window. |
| [`EmailModels.kt`](file:///home/hp/CyberVictimAssistant/app/src/main/java/com/cybervictim/assistant/email/EmailModels.kt) | Data models representing parsed emails, candidates, and phishing analysis cards. |
| [`EmailStorage.kt`](file:///home/hp/CyberVictimAssistant/app/src/main/java/com/cybervictim/assistant/email/EmailStorage.kt) | Persists analyzed emails and records extracted facts into Room DB. |

### Package: `com.cybervictim.assistant.data` & `pack` & `requests`
| File | Duty & Core Responsibilities |
|---|---|
| [`CaseDb.kt`](file:///home/hp/CyberVictimAssistant/app/src/main/java/com/cybervictim/assistant/data/CaseDb.kt) | Room database definition, DAO contracts, and type entities (`EvidenceItem`, `CaseFact`, `CaseEvent`). |
| [`CaseSession.kt`](file:///home/hp/CyberVictimAssistant/app/src/main/java/com/cybervictim/assistant/data/CaseSession.kt) | Case identifier management (`case-current` vs historic). |
| [`UserProfileStore.kt`](file:///home/hp/CyberVictimAssistant/app/src/main/java/com/cybervictim/assistant/data/UserProfileStore.kt) | Manages citizen profile storage and generates unique Victim IDs (`VIC-YYYY-XXXXX-HEX`). |
| [`ComplaintPack.kt`](file:///home/hp/CyberVictimAssistant/app/src/main/java/com/cybervictim/assistant/pack/ComplaintPack.kt) | Generates official multi-page PDF Complaint Packets with embedded evidence for police submission. |
| [`RequestDrafter.kt`](file:///home/hp/CyberVictimAssistant/app/src/main/java/com/cybervictim/assistant/requests/RequestDrafter.kt) | Generates legal freeze notices (Bank Nodal Sec 91 CrPC, Telecom Preservation, Gateway Dispute). |
| [`PhoneTrace.kt`](file:///home/hp/CyberVictimAssistant/app/src/main/java/com/cybervictim/assistant/trc/PhoneTrace.kt) & [`PhoneScore.kt`](file:///home/hp/CyberVictimAssistant/app/src/main/java/com/cybervictim/assistant/trc/PhoneScore.kt) | Audits installed packages for suspicious remote-control permissions (AnyDesk, TeamViewer, RustDesk). |
| [`ExposureChecker.kt`](file:///home/hp/CyberVictimAssistant/app/src/main/java/com/cybervictim/assistant/exposure/ExposureChecker.kt) & [`ExposureApi.kt`](file:///home/hp/CyberVictimAssistant/app/src/main/java/com/cybervictim/assistant/exposure/ExposureApi.kt) | Checks email breach exposure via XposedOrNot / HIBP APIs. |
| [`SecPrefs.kt`](file:///home/hp/CyberVictimAssistant/app/src/main/java/com/cybervictim/assistant/exposure/SecPrefs.kt) | EncryptedSharedPreferences wrapper (AES-256 GCM) for secure token storage. |
| [`ReminderScheduler.kt`](file:///home/hp/CyberVictimAssistant/app/src/main/java/com/cybervictim/assistant/remind/ReminderScheduler.kt) | Schedules notifications for post-filing follow-ups with bank nodal officers. |

---

## 10. Test Suites & Verification Gates

The repository contains extensive JUnit unit tests in `app/src/test/java`:
1. `PlanAccuracyTest.kt`: Tests deterministic scenario routing across 40 real-world victim scenarios. **Pass Rate: 40/40 (100%)**.
2. `PlanPickParseTest.kt`: Validates Gemini JSON schema parsing and error handling for malformed model outputs.
3. `ComplaintScriptGeneratorTest.kt`: Verifies police complaint generation for financial fraud and account hacking scenarios.
4. `PhishingAnalyzerTest.kt`: Validates SPF/DKIM/DMARC failure detection and lookalike domain scoring against fixture `.eml` files.
5. `RequestDrafterTest.kt`: Verifies statutory citations in bank freeze notices under CrPC, BNSS, and IT Act.
6. `PackBudgetTest.kt`: Ensures generated PDF complaint packs stay within size and page constraints.
7. `PhoneScoreTest.kt`: Validates risk scoring for remote-access APKs.

To execute all unit tests:
```bash
./gradlew testDebugUnitTest
```

---

## 11. LLM Operational Cheat Sheet: How to Maintain This Codebase

When an LLM agent is asked to modify, debug, or extend Cybridge, adhere to the following rules:

1. **Gating Invariant**: Never add code that automatically clicks final submit buttons or bypasses CAPTCHA on `cybercrime.gov.in`. Always classify actions in `PortalAutoFillEngine.kt` as `AI_AUTO`, `HUMAN_ONLY`, or `VICTIM_LOCK`.
2. **Zero Mock Data**: Never add hardcoded personal details (names, phone numbers, addresses) or fake bank transactions. All data must originate from `UserProfileStore`, `CaseDb`, `SmsScamExtractor`, or `OcrPipeline`.
3. **Pure On-Device Privacy**: Do not send raw victim images to external APIs. Ingest images via `OcrPipeline.decodeSampled()` and `OcrPipeline.recognizeText()`; send only extracted text strings if optional refinement is requested.
4. **Localization Awareness**: When adding user-facing UI text, define corresponding keys in [`L10n.kt`](file:///home/hp/CyberVictimAssistant/app/src/main/java/com/cybervictim/assistant/ui/L10n.kt) to support all 12 Indian languages.
5. **No Emojis in Core UI**: Maintain official government aesthetic; do not add decorative emojis or informal symbols to formal victim reporting screens.
6. **Device Testing**: Verify changes on a connected physical device:
   ```bash
   ./gradlew assembleDebug && ./gradlew testDebugUnitTest
   adb install -r -d -t app/build/outputs/apk/debug/app-debug.apk
   adb shell am start -n com.cybervictim.assistant/.MainActivity
   ```

---

*Authored by the Cybridge Core Engineering Team | For questions, refer to the source files linked above.*
