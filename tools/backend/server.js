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
    // FREE NO-KEY SOURCE (default): XposedOrNot breach analytics — no API key,
    // rich free tier (100 lookups/day per server IP). Falls back to DEMO on failure.
    try {
      const f = await fetch("https://api.xposedornot.com/v1/breach-analytics?email=" + encodeURIComponent(req.params.email), { signal: AbortSignal.timeout(9000) });
      const d = await f.json();
      if (f.ok && d) {
        const details = (d.ExposedBreaches && Array.isArray(d.ExposedBreaches.breaches_details)) ? d.ExposedBreaches.breaches_details : null;
        if (details === null) {
          // reachable + clean: honest "not found" instead of demo samples
          return res.status(404).json({ configured: true, mode: "xposedornot", note: "Clean: no public breaches found for this address (XposedOrNot free source)." });
        }
        const list = details.map((b) => ({
          name: b.breach || "unknown",
          domain: b.domain || "",
          date: (b.xposed_year || b.year || "") + "",
          what: String(b.details || "").slice(0, 300),
          data: (b.xposed_data || b.exposed_data || "").split(";").filter(Boolean).slice(0, 6).join(", "),
        }));
        if (list.length === 0) {
          return res.status(404).json({
            configured: true,
            mode: "xposedornot",
            note: "Clean: no public breaches found for this address (XposedOrNot free source).",
          });
        }
        return res.json({
          configured: true,
          mode: "xposedornot",
          note: "Free no-key breach source (XposedOrNot). Rate-limited by their free tier (100/day per server IP).",
          breaches: list,
        });
      }
    } catch (_) { /* fall through to DEMO */ }
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

app.post("/voice-guide", async (req, res) => {
  const { transcript, languageTag, screen, history } = req.body || {};
  if (!transcript || typeof transcript !== "string") {
    return res.status(400).json({ error: "missing transcript" });
  }

  const s = screen || {};
  const lang = (languageTag || "en-IN").toLowerCase();
  const key = process.env.GEMINI_API_KEY;

  if (key) {
    try {
      const sysPrompt = `You are CyBridge Voice Guide helping a cybercrime victim in India navigate the official complaint process.
Current Screen: ${s.title || "Report"} (ID: ${s.screenId || "unknown"}, Step: ${s.stepIndex ?? -1} - ${s.stepTitle || ""}).
Available buttons on this screen: ${(s.buttons || []).join(", ") || "None"}.
Available fields on this screen: ${(s.fields || []).join(", ") || "None"}.
Step Owner: ${s.owner || "AI_AUTO"} (HUMAN = user enters OTP/CAPTCHA/credentials manually; VICTIM_LOCK = final locked submit step; AI_AUTO = user taps recommended action).
Target Language BCP-47: ${lang}.
Instructions:
- Provide exactly ONE clear next step instruction for the user in the target language (${lang}).
- The user does all tapping. You only advise what to tap or do next.
- Keep the response short (1 to 2 sentences).
- Strictly no emojis.`;

      const contents = [];
      if (Array.isArray(history)) {
        for (const item of history.slice(-3)) {
          contents.push({
            role: item.speaker === "user" ? "user" : "model",
            parts: [{ text: String(item.text || "") }],
          });
        }
      }
      contents.push({
        role: "user",
        parts: [{ text: transcript }],
      });

      const body = {
        system_instruction: {
          parts: [{ text: sysPrompt }],
        },
        contents,
        generationConfig: {
          temperature: 0.2,
          maxOutputTokens: 300,
        },
      };

      const resp = await fetch(
        `https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=${key}`,
        {
          method: "POST",
          headers: { "Content-Type": "application/json" },
          body: JSON.stringify(body),
          signal: AbortSignal.timeout(7000),
        }
      );

      if (resp.ok) {
        const data = await resp.json();
        const candidate = data.candidates?.[0]?.content?.parts?.[0]?.text?.trim();
        if (candidate) {
          const clean = candidate
            .replace(/[\uD83C-\uDBFF][\uDC00-\uDFFF]/g, "")
            .replace(/[\u2000-\u2BFF\uFE00-\uFE0F\u25A0-\u27BF]/g, "")
            .trim();
          return res.json({ ok: true, nextStep: clean, source: "gemini" });
        }
      }
    } catch (_) {
      // Fall through to contextual rule-based response
    }
  }

  // Contextual fallback based on screen and owner
  let fallback = "";
  if (s.owner === "HUMAN") {
    fallback = lang.startsWith("hi")
      ? "कृपया स्क्रीन पर अपना OTP या कैप्चा दर्ज करें और आगे बढ़ने के लिए बटन दबाएँ।"
      : lang.startsWith("te")
      ? "దయచేసి స్క్రీన్‌పై మీ OTP లేదా క్యాప్చాను నమోదు చేసి కొనసాగించడానికి బటన్‌ను నొక్కండి."
      : "Please enter your OTP or captcha on the screen and tap the button to proceed.";
  } else if (s.owner === "VICTIM_LOCK") {
    fallback = lang.startsWith("hi")
      ? "यह अंतिम लॉक है। सभी विवरणों की जाँच करें और शिकायत दर्ज करने के लिए सबमिट दबाएँ।"
      : lang.startsWith("te")
      ? "ఇది తుది లాక్. అన్ని వివరాలను తనిఖీ చేసి ఫిర్యాదు దాఖలు చేయడానికి సమర్పించు నొక్కండి."
      : "This is the final locked step. Review all details and tap Submit to file your complaint.";
  } else if (s.buttons && s.buttons.length > 0) {
    const btn = s.buttons[0];
    fallback = lang.startsWith("hi")
      ? `आगे बढ़ने के लिए स्क्रीन पर '${btn}' बटन दबाएँ।`
      : lang.startsWith("te")
      ? `కొనసాగడానికి స్క్రీన్‌పై '${btn}' బటన్‌ను నొక్కండి.`
      : `Tap '${btn}' on the screen to proceed to the next step.`;
  } else {
    fallback = lang.startsWith("hi")
      ? "अगले चरण के लिए स्क्रीन पर दिए गए निर्देशों का पालन करें।"
      : lang.startsWith("te")
      ? "తర్వాతి దశ కోసం స్క్రీన్‌పై ఉన్న సూచనలను అనుసరించండి."
      : "Follow the on-screen options to continue.";
  }

  return res.json({ ok: true, nextStep: fallback, source: "contextual_rule" });
});

const port = process.env.PORT || 8080;
app.listen(port, () => {
  console.log("exposure backend on :" + port);
  console.log("config status (booleans only, values never printed):");
  console.log("  .env file loaded: " + envFileLoaded);
  console.log("  SMTP_URL set: " + !!process.env.SMTP_URL);
  console.log("  EMAIL_FROM set: " + !!process.env.EMAIL_FROM);
  console.log("  HIBP_API_KEY set: " + !!process.env.HIBP_API_KEY + "  -> breach scan mode: " + (process.env.HIBP_API_KEY ? "LIVE" : "DEMO DATA"));
});

// Phase 10-G: reuse the express app from Firebase Cloud Functions (no listen there).
if (require.main === module) { /* standalone mode only */ }
module.exports = app;
