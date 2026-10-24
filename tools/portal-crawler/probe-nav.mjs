import { chromium } from "playwright";
const b = await chromium.launch({ headless: true });
const c = await b.newContext({
  viewport: { width: 390, height: 844 },
  userAgent:
    "Mozilla/5.0 (Linux; Android 13; Pixel 7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.6367.82 Mobile Safari/537.36",
});
const p = await c.newPage();
const reqs = new Set();
p.on("request", (r) => {
  const u = r.url();
  if (/\d|report|crime|ashwini|webform|web\//i.test(u) && !/\.(png|jpg|svg|css|js|woff)/i.test(u)) reqs.add(u);
});
await p.goto("https://cybercrime.gov.in/home", { waitUntil: "domcontentloaded", timeout: 45000 });
await p.waitForTimeout(4000);
const els = await p.evaluate(() => {
  const out = [];
  document.querySelectorAll("a").forEach((a) => {
    if (/report|complaint/i.test(a.innerText)) out.push({ t: a.innerText.trim().slice(0, 30), oc: a.getAttribute("onclick"), nc: a.getAttribute("ng-click"), cap: a.getAttribute("clickcapture"), id: a.id, h: a.getAttribute("href"), cls: a.className.slice(0, 50) });
  });
  return out;
});
console.log(JSON.stringify(els, null, 1));
// click via evaluate to bypass strictness
const result = await p.evaluate(() => {
  const a = [...document.querySelectorAll("a")].find((e) => /register a complaint/i.test(e.innerText));
  if (a) {
    a.click();
    return "clicked " + (a.getAttribute("clickcapture") || a.getAttribute("onclick") || a.id || "?");
  }
  const a2 = [...document.querySelectorAll("a")].find((e) => /report a crime/i.test(e.innerText));
  if (a2) {
    a2.click();
    return "clicked2 " + (a2.getAttribute("clickcapture") || a2.getAttribute("onclick") || "?");
  }
  return "none";
});
console.log(result);
await p.waitForTimeout(6000);
console.log("now at:", p.url());
console.log((await p.evaluate(() => document.body.innerText.slice(0, 400))).replace(/\n+/g, " | "));
console.log("interesting requests:", [...reqs].slice(0, 25));
await b.close();
