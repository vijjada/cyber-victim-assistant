/**
 * Cyber Victim Assistant — Exposure backend (Phase 8 email-verification +
 * breach lookup). Deploy anywhere Node runs (Render/Fly/Glitch):
 *   npm i && EMAIL_FROM=you@x.com SMTP_URL=smtp://user:pass@host:587 node server.js
 * Security posture:
 *   - Verification codes are 6 digits, hashed (sha256), 10-min TTL, 5 tries.
 *   - /breaches requires a verified session token; without HIBP_API_KEY it
 *     returns an explicit "not_configured" response, never fake data.
 *   - No PII stored beyond the email TTL/verification window; codes in RAM
 *     (swap to Redis for production).
 */
const express = require("express");
const { createHash, randomInt } = require("crypto");
const nodemailer = require("nodemailer");
const fs = require("fs");
const path = require("path");

/* Loads tools/backend/.env at startup. Values are never logged or echoed. */
let envFileLoaded = false;
try {
  const envPath = path.join(__dirname, ".env");
  for (const line of fs.readFileSync(envPath, "utf8").split(/\r?\n/)) {
    const m = line.match(/^\s*([A-Za-z_][A-Za-z0-9_]*)\s*=\s*(.*?)\s*$/);
    if (m && process.env[m[1]] === undefined) {
      process.env[m[1]] = m[2].replace(/^["'](.*)["']$/, "$1");
      envFileLoaded = true;
    }
  }
} catch (_) { /* no .env yet — plain env vars still work */ }

const app = express();
app.use(express.json());

const EMAIL_RE = /^[^@\s]+@[^@\s]+\.[^@\s]{2,}$/;
const codes = new Map(); // email -> {hash, exp, tries}
const verified = new Map(); // token -> {email, exp}

const sha = (s) => createHash("sha256").update(s).digest("hex");
const ttlOk = (e) => e && e.exp > Date.now();

function smtp() {
  if (!process.env.SMTP_URL) return null;
  try { return nodemailer.createTransport(process.env.SMTP_URL); }
  catch (e) { console.error("SMTP init failed:", e.message); return null; }
}
const mailer = smtp();

app.post("/verify-email/start", (req, res) => {
  const { email } = req.body || {};
  if (!EMAIL_RE.test(email || "")) return res.status(400).json({ error: "invalid email" });
  const code = String(randomInt(0, 1000000)).padStart(6, "0");
  codes.set(email.toLowerCase(), { hash: sha(code), exp: Date.now() + 10 * 60_000, tries: 0 });
  if (!mailer) {
    // DEV-ONLY fallback: no SMTP configured, so the code goes to the server
    // console instead of an inbox. Never use this mode in production.
    console.log("DEV-ONLY verification code for " + email + ": " + code + " (valid 10 min)");
    return res.json({ ok: true, delivery: "server-console", note: "DEV MODE: SMTP not configured; code printed to server console only." });
  }
  mailer.sendMail({
    from: process.env.EMAIL_FROM,
    to: email,
    subject: "Cyber Victim Assistant — verification code",
    text: "Your verification code is " + code + ". It expires in 10 minutes.",
  }).then(() => res.json({ ok: true }))
    .catch((e) => { console.error(e.message); res.status(502).json({ error: "email send failed" }); });
});

app.post("/verify-email/check", (req, res) => {
  const { email, code } = req.body || {};
  const e = codes.get((email || "").toLowerCase());
  if (!e || !ttlOk(e)) return res.status(400).json({ error: "code expired or unknown email" });
  if (e.tries >= 5) { codes.delete(email.toLowerCase()); return res.status(429).json({ error: "too many tries" }); }
  e.tries += 1;
  if (e.hash !== sha(String(code))) return res.status(400).json({ error: "wrong code" });
  codes.delete(email.toLowerCase());
  const token = sha(email.toLowerCase() + Date.now() + Math.random()).slice(0, 40);
  verified.set(token, { email: email.toLowerCase(), exp: Date.now() + 30 * 60_000 });
  res.json({ ok: true, token });
});

app.get("/breaches/:email", async (req, res) => {
  const token = req.get("authorization")?.replace(/^Bearer\s+/i, "");
  const v = token && verified.get(token);
  if (!v || !ttlOk(v)) return res.status(401).json({ error: "verify your email first" });
  if (v.email !== req.params.email.toLowerCase()) return res.status(403).json({ error: "token/email mismatch" });
  const key = process.env.HIBP_API_KEY;
  if (!key) {
    // DEMO DATA MODE — explicitly labelled; never presented as a real lookup.
    return res.json({
      mode: "demo",
      demo: true,
      configured: false,
      note: "DEMO DATA: no HIBP_API_KEY on this server. Entries below are fixed samples so the app flow can be tested — they are NOT a real breach lookup of this address.",
      breaches: [
        {
          name: "DemoShop2024 (SAMPLE)",
          domain: "example-demo-1.com",
          date: "2024-03-01",
          what: "SAMPLE DEMO DATA: email + hashed password + phone exposed in a fake shop breach. Testing entry only.",
        },
        {
          name: "ForumLeak2021 (SAMPLE)",
          domain: "example-demo-2.org",
          date: "2021-11-15",
          what: "SAMPLE DEMO DATA: username + email + IP exposed in a fake forum leak. Testing entry only.",
        },
      ],
    });
  }
  try {
    const r = await fetch("https://haveibeenpwned.com/api/v3/breachedaccount/" +
      encodeURIComponent(req.params.email) + "?truncateResponse=false",
      { headers: { "HIBP-API-KEY": key, "User-Agent": "CyberVictimAssistant" } });
    if (r.status === 404) return res.json({ configured: true, breaches: [] });
    if (!r.ok) return res.status(502).json({ error: "HIBP " + r.status });
    const data = await r.json();
    res.json({
      configured: true,
      mode: "live",
      breaches: data.map((b) => ({
        name: b.Name, domain: b.Domain, date: b.BreachDate,
        what: b.Description?.replace(/<[^>]+>/g, "").slice(0, 200),
      })),
    });
  } catch (e) { res.status(502).json({ error: "network " + e.message }); }
});

const port = process.env.PORT || 8080;
if (CVA_STANDALONE) app.listen(port, () => {
  console.log("exposure backend on :" + port);
  console.log("config status (booleans only, values never printed):");
  console.log("  .env file loaded: " + envFileLoaded);
  console.log("  SMTP_URL set: " + !!process.env.SMTP_URL);
  console.log("  EMAIL_FROM set: " + !!process.env.EMAIL_FROM);
  console.log("  HIBP_API_KEY set: " + !!process.env.HIBP_API_KEY + "  -> breach scan mode: " + (process.env.HIBP_API_KEY ? "LIVE" : "DEMO DATA"));
});

// Phase 10-G: reuse the express app from Firebase Cloud Functions (no listen there).
if (require.main === module) { /* standalone mode only */ }
export default app;

export default app;
