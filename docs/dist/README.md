# Cyber Victim Assistant — Project Root

Structure:

- `docs/` — project summary v2 (markdown source + generated PDF)
- `app/` — Android (Kotlin + Jetpack Compose) APK source
- `tools/portal-crawler/` — dev-side crawler that builds the portal knowledge base from cybercrime.gov.in
- Portal KB assets live in `app/src/main/assets/kb/` (grounding data the AI reads, not model weights)

Build order follows the approved phase plan. PDF v2 is generated via headless Chrome.

Commands:
- PDF: `bash tools/make_pdf.sh`
- Crawler: `cd tools/portal-crawler && npm i && node crawl.mjs`
- Android: import `app/` in Android Studio (AGP 8.x) or `./gradlew :app:assembleDebug` once wrapper is generated.
