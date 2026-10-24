import { chromium } from "playwright";
const b = await chromium.launch({ headless: true });
const c = await b.newContext({
  viewport: { width: 390, height: 844 },
  userAgent:
    "Mozilla/5.0 (Linux; Android 13; Pixel 7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.6367.82 Mobile Safari/537.36",
});
const p = await c.newPage();
let consoleMsgs = [];
p.on("console", (m) => consoleMsgs.push(m.text().slice(0, 120)));
await p.goto("https://cybercrime.gov.in/home", { waitUntil: "networkidle", timeout: 60000 });
await p.waitForTimeout(3000);
for (const label of ["Report a Crime", "Report Cyber Crime", "Report Cybercrime"]) {
  const loc = p.locator(`a:has-text("${label}")`).first();
  try {
    await loc.click({ timeout: 4000 });
    await p.waitForTimeout(5000);
    console.log("clicked:", label, "| now at:", p.url());
    const txt = await p.evaluate(() => document.body.innerText.slice(0, 400).replace(/\n+/g, " | "));
    console.log("body:", txt);
    // cleanup: go home again
    await p.goto("https://cybercrime.gov.in/home", { waitUntil: "domcontentloaded" });
    await p.waitForTimeout(2500);
  } catch (e) {
    console.log("click fail:", label, e.message.slice(0, 100));
  }
}
console.log("console msgs:", consoleMsgs.slice(0, 12));
await b.close();
