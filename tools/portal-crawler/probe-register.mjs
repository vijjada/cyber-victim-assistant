import { chromium } from "playwright";
const b = await chromium.launch({ headless: true });
const c = await b.newContext({
  viewport: { width: 390, height: 844 },
  userAgent:
    "Mozilla/5.0 (Linux; Android 13; Pixel 7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.6367.82 Mobile Safari/537.36",
});
const p = await c.newPage();
await p.goto("https://cybercrime.gov.in/home", { waitUntil: "domcontentloaded", timeout: 45000 });
await p.waitForTimeout(4000);
const lks = await p.evaluate(() =>
  [...document.querySelectorAll("a, button")].map((a) => ({
    tag: a.tagName,
    t: (a.innerText || "").trim().slice(0, 40),
    h: a.getAttribute("href"),
    oc: (a.getAttribute("ng-click") || a.getAttribute("onclick") || a.getAttribute("clickcapture") || ""),
  })).filter((l) => /complain|report|register/i.test(l.t + l.h + l.oc)),
);
console.log(JSON.stringify(lks, null, 1));
const el = p.locator("text=Register a Complaint").first();
try {
  await el.click({ timeout: 5000 });
  await p.waitForTimeout(7000);
  console.log("AFTER CLICK url:", p.url());
  console.log(await p.evaluate(() => document.body.innerText.slice(0, 700)));
} catch (e) {
  console.log("click fail:", e.message.slice(0, 150));
}
await b.close();
