# Exposure backend (Phase 8)

Sends the email verification code and performs the breach lookup once the
email is verified. Deploy to any Node host:

1. `npm i`
2. Set env:
   - `SMTP_URL=smtp://user:pass@smtp.host:587` (e.g. Gmail app password)
   - `EMAIL_FROM="Cyber Victim Assistant <you@yourdomain>"` 
   - `HIBP_API_KEY=` (from haveibeenpwned.com, needed only for the email breach scan)
3. `node server.js` (Render/Fly/Railway: start command `npm start`)

Then in the app: put the base URL in `gradle.properties` as
`EXPOSURE_BASE_URL=https://your-app.example.com` and rebuild.

Privacy: codes are hashed, RAM-only, 10-minute TTL, 5-try cap; no breach
data is cached server-side; token bound to the verified email for 30 min.
