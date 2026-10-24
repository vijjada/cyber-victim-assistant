import { chromium } from "playwright";
const b = await chromium.launch({ headless: true });
const c = await b.newContext({
  viewport: { width: 390, height: 844 },
  userAgent:
    "Mozilla/5.0 (Linux; Android 13; Pixel 7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.6367.82 Mobile Safari/537.36",
});
const p = await c.newPage();
const navErrs = [];
p.on("pageerror", (e) => navErrs.push(String(e).slice(0, 200)));
p.on("requestfailed", (r) => navErrs.push("REQFAIL " + r.url().slice(0, 120) + " " + (r.failure() || {}).errorText));
await p.goto("https://cybercrime.gov.in/home", { waitUntil: "networkidle", timeout: 60000 });
await p.waitForTimeout(3000);
// find the Report-a-Crime anchors' bound JS
const bound = await p.evaluate(() =>
  [...document.querySelectorAll("a,button,div,span")]
    .filter((e) => /report/i.test(e.getAttribute("id") || "") || /report/i.test(e.getAttribute("class") || "") || /complain/i.test(e.getAttribute("id") || ""))
    .slice(0, 20)
    .map((e) => ({
      tag: e.tagName,
      id: e.id,
      cls: String(e.className).slice(0, 60),
      oc: e.getAttribute("onclick"),
      nc: e.getAttribute("ng-click"),
      cap: e.getAttribute("clickcapture"),
      dat: Object.keys(e.dataset || {}).join(","),
      txt: e.innerText.trim().slice(0, 30),
    })),
);
console.log(JSON.stringify(bound, null, 1));
// try clicking by internal attribute we found, e.g. [clickcapture]
const target = await p.$("ul.home_menu a, .menu_report, #ReportCrime");
console.log("direct target:", !!target);
console.log("pageerrors/fails:", navErrs.slice(0, 10));
await b.close();
