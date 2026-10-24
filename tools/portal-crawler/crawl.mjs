#!/usr/bin/env node
/**
 * Cyber Victim Assistant - Portal Knowledge Base crawler (dev tool)
 * Grounding source: https://cybercrime.gov.in (apex; www. is Akamai-blocked on datacenter IPs)
 *
 * Discovers and walks portal pages, capturing buttons, form fields, select
 * options (including violation categories), and captcha presence into a
 * versioned KB snapshot. Never solves captchas, never submits anything,
 * never logs in.
 */
import { chromium } from "playwright";
import fs from "node:fs";
import path from "node:path";

const BASE = "https://cybercrime.gov.in";
const MAX_PAGES = Number(process.argv[2] || 12);
const EXCLUDE = /logout|javascript:|mailto:|#|\.(jpg|png|svg|css|js|pdf)$/i;

const browser = await chromium.launch({ headless: true, args: ["--disable-blink-features=AutomationControlled"] });
const context = await browser.newContext({
  viewport: { width: 390, height: 844 },
  userAgent:
    "Mozilla/5.0 (Linux; Android 13; Pixel 7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.6367.82 Mobile Safari/537.36",
});
const page = await context.newPage();

const snapshot = {
  lastVerified: new Date().toISOString().slice(0, 10),
  portal: BASE,
  screens: [],
};
const visited = new Set();
const queue = [BASE + "/home"];

async function capture(url) {
  if (visited.has(url) || visited.size >= MAX_PAGES) return [];
  visited.add(url);
  try {
    const resp = await page.goto(url, { waitUntil: "domcontentloaded", timeout: 45000 });
    if (!resp || resp.status() !== 200) {
      console.log("SKIP", url, resp ? resp.status() : "no-resp");
      return [];
    }
    await page.waitForTimeout(5000);
    const screen = await page.evaluate((u) => {
      const norm = (s) => (s || "").replace(/\s+/g, " ").trim().slice(0, 90);
      const buttons = [...document.querySelectorAll("button, a[role=button], input[type=submit], a.btn, [ng-click], [clickcapture]")]
        .map((e) => ({ text: norm(e.innerText || e.value), id: e.id || null }))
        .filter((b) => b.text);
      const links = [...document.querySelectorAll("a[href]")]
        .map((a) => new URL(a.href, location.origin).href)
        .filter((h) => h.startsWith(location.origin));
      const fields = [...document.querySelectorAll("input, select, textarea")].map((el) => ({
        tag: el.tagName.toLowerCase(),
        type: el.type || null,
        name: el.name || null,
        id: el.id || null,
        required: !!el.required,
        options: el.tagName === "SELECT"
          ? [...el.options].map((o) => ({ v: o.value, t: o.text.trim() })).slice(0, 150)
          : undefined,
      }));
      return {
        url: u,
        title: document.title,
        heading: norm(document.querySelector("h1,h2,h3")?.innerText),
        buttons,
        fields,
        captcha: !!document.querySelector(".captcha,img[src*=Captcha i],[id*=captcha i]"),
        newLinks: [...new Set(links)],
      };
    }, url);
    snapshot.screens.push(screen);
    console.log("CAPTURED", url, "| buttons:", screen.buttons.length, "| fields:", screen.fields.length);
    return screen.newLinks;
  } catch (e) {
    console.error("FAIL", url, e.message.slice(0, 120));
    return [];
  }
}

while (queue.length && visited.size < MAX_PAGES) {
  const url = queue.shift();
  const links = await capture(url);
  for (const l of links) if (!visited.has(l)) queue.push(l);
}

await browser.close();
fs.writeFileSync("output/portal_map_snapshot.json", JSON.stringify(snapshot, null, 2));
console.log("Done:", visited.size, "pages ->", path.resolve("output/portal_map_snapshot.json"));
