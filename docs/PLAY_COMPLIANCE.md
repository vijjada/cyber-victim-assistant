# Play Store compliance notes (Phase 9-D)

Everything here is user-visible and user-consented; document before any store upload.

## Sensitive permissions
- `QUERY_ALL_PACKAGES` — PhoneTrace scans the victim's OWN installed apps to find remote-control
  / stalkerware families (only at the victim's explicit request, results never leave the device).
  No ads, no cross-app activity, no other-app tracking. Fallback if restricted: requested
  software-based install-list via user-typed manifest (feature degraded but no permission).
- `PACKAGE_USAGE_STATS` — powers the "apps opened around the fraud time" timeline; special-access
  setting the user grants manually; never sent anywhere.
- `SCHEDULE_EXACT_ALARM` — 10-day exposure re-check reminder, scheduled only after a successful
  user-authorized scan; user-visible notification, no wake-lock abuse.

## Data safety summary (all true)
- Case facts/evidence: on-device Room/SQLite only; never uploaded.
- OCR: on-device ML Kit; screenshots never leave the phone (the AI-refine beta sends OCR *text*
  only when the user expressly toggles it on).
- Exposure check: user's email + 6-digit OTP code transmitted to the operator's own backend
  (Firebase/Cloud Run URL); password check transmits only a 5-char SHA-1 hash prefix (k-anonymity);
  session token stored in Keystore-EncryptedSharedPreferences (never plain prefs).
- Legal drafts: generated on-device; sharing is via the standard Android chooser (user-initiated).
- Emergency dialer: ACTION_DIAL only (dials, no contact read).

## Not asked for (and must never be added silently)
READ_SMS / contacts / location / camera / microphone without an explicit user step in-app.
