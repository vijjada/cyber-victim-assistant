# Exposure backend — Firebase Cloud Functions (replaces Render, per decision 2026-10-22)

Same `tools/backend/server.js` app, wrapped in `functions/srv.js` (imported as a module —
no `.listen` in cloud mode). Region `asia-south1` (closest to IN users).

## One-time setup (your Google account)
1. Install CLI once: `npm i -g firebase-tools`
2. `firebase login`
3. `cd tools/functions`

## Deploy
```
# secrets — run each, paste when prompted:
firebase functions:secrets:set SMTP_URL
firebase functions:secrets:set EMAIL_FROM
firebase functions:secrets:set HIBP_API_KEY   # optional; omit = DEMO mode endpoints stay truthful

firebase deploy --only functions
```
Cloud Console prints the URL: `https://cvaexposure-<hash>-asia-south1.<region>.run.app`-style.

## Point the app at it
Add to `~/.gradle/gradle.properties` and rebuild:
```
EXPOSURE_BASE_URL=https://<your-function-url>
```
HTTPS note: no extra transport-security changes needed — cleartext block only ever allowed `10.1.35.207`.

## Verify after deploy
```
curl https://<url>/health            # {"ok":true,...}
curl -X POST -H 'Content-Type: application/json' -d '{"email":"<throwaway@x>"}' https://<url>/verify-email/start
# code arrives by SMTP; then /verify-email/check -> token; then GET /breaches/<email> with Bearer
```
