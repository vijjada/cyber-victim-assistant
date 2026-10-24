import { chromium } from "playwright";
const b = await chromium.launch({ headless: true });
const c = await b.newContext({
  viewport: { width: 390, height: 844 },
  userAgent:
    "Mozilla/5.0 (Linux; Android 13; Pixel 7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.6367.82 Mobile Safari/537.36",
});
const p = await c.newPage();
const routes = [
  "/web/reports/preloginreport",
  "/web/reports/loginreport",
  "/web/reports/othercrimereport",
  "/Webform/Crime",
  "/ashwini3/Crime",
  "/web/login-report",
  "/reports/login-report",
  "/registercomplainant",
  "/web/register-complainant",
  "/citizen-login",
];
for (const r of routes) {
  try {
    const resp = await p.goto("https://cybercrime.gov.in" + r, { waitUntil: "domcontentloaded", timeout: 30000 });
    await p.waitForTimeout(2500);
    const len = (await p.content()).length;
    const title = await p.title();
    if (resp.status() === 200) console.log("OK  ", r, "len", len, "| title:", title, "| h1:", await p.evaluate(() => document.body.innerText.slice(0, 120).replace(/\n/g, " / ")));
    else console.log("ERR ", r, resp.status());
  } catch (e) {
    console.log("FAIL", r, e.message.slice(0, 60));
  }
}
await b.close();
