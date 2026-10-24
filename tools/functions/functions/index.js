
// Phase 10-G: Firebase Cloud Functions wrapper — SAME app object as tools/backend/server.js.
// Environment (preferred: `firebase functions:secrets:set SMTP_URL` / EMAIL_FROM / HIBP_API_KEY,
// or a functions/.env file side-by-side with this file).
import { onRequest } from "firebase-functions/v2/https";
import createApp from "./srv.js";

export const cvaExposure = onRequest(
  { region: "asia-south1", maxInstances: 3 },
  createApp({ standalone: false, port: 0 })
);
