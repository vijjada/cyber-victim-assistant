#!/usr/bin/env python3
"""Fill the prescribed BUILD WITH भारत 5.0 template with the Cybridge submission deck."""

import copy
from pptx import Presentation
from pptx.util import Inches, Pt, Emu
from pptx.dml.color import RGBColor
from pptx.enum.text import PP_ALIGN, MSO_ANCHOR
from pptx.enum.shapes import MSO_SHAPE
from pptx.oxml.ns import qn

SRC = "template.pptx"
OUT = "Cybridge_BuildWithBharat5.0.pptx"

ORANGE = RGBColor(0xFF, 0x5C, 0x1F)
GREEN = RGBColor(0x15, 0x76, 0x2B)
PURPLE = RGBColor(0x87, 0x25, 0xB4)
INK = RGBColor(0x1A, 0x1A, 0x1A)
BODY = RGBColor(0x33, 0x33, 0x33)
MUTED = RGBColor(0x6B, 0x6B, 0x6B)
WHITE = RGBColor(0xFF, 0xFF, 0xFF)
TINT_O = RGBColor(0xFF, 0xF3, 0xEE)
TINT_G = RGBColor(0xEF, 0xF7, 0xF0)
TINT_P = RGBColor(0xF7, 0xF1, 0xFB)
TINT_N = RGBColor(0xF6, 0xF7, 0xF9)
LINE = RGBColor(0xE0, 0xE0, 0xE0)

HEAD = "Montserrat"
TEXT = "Calibri"

CONTENT_TOP = 3.28
CONTENT_BOT = 10.12


# ---------------------------------------------------------------- primitives
def _set_run(run, text, size, bold=False, color=BODY, font=TEXT, italic=False):
    run.text = text
    f = run.font
    f.name = font
    f.size = Pt(size)
    f.bold = bold
    f.italic = italic
    f.color.rgb = color
    # force latin+cs+ea so Google Slides / PowerPoint keep Montserrat
    rPr = run._r.get_or_add_rPr()
    for tag in ("a:latin", "a:ea", "a:cs"):
        e = rPr.find(qn(tag))
        if e is None:
            e = rPr.makeelement(qn(tag), {})
            rPr.append(e)
        e.set("typeface", font)


def textbox(slide, x, y, w, h, anchor=MSO_ANCHOR.TOP):
    box = slide.shapes.add_textbox(Inches(x), Inches(y), Inches(w), Inches(h))
    tf = box.text_frame
    tf.word_wrap = True
    tf.vertical_anchor = anchor
    tf.margin_left = tf.margin_right = tf.margin_top = tf.margin_bottom = 0
    return box, tf


def para(tf, text, size, bold=False, color=BODY, font=TEXT, space_before=0,
         space_after=0, align=PP_ALIGN.LEFT, first=False, line=1.12, runs=None):
    p = tf.paragraphs[0] if first else tf.add_paragraph()
    p.alignment = align
    p.space_before = Pt(space_before)
    p.space_after = Pt(space_after)
    p.line_spacing = line
    if runs:
        for chunk, kw in runs:
            _set_run(p.add_run(), chunk, kw.get("size", size), kw.get("bold", False),
                     kw.get("color", color), kw.get("font", font))
    else:
        _set_run(p.add_run(), text, size, bold, color, font)
    return p


def rect(slide, x, y, w, h, fill=None, line_color=None, line_w=1.0, shape=MSO_SHAPE.RECTANGLE,
         adj=None, shadow=False):
    sp = slide.shapes.add_shape(shape, Inches(x), Inches(y), Inches(w), Inches(h))
    if fill is None:
        sp.fill.background()
    else:
        sp.fill.solid()
        sp.fill.fore_color.rgb = fill
    if line_color is None:
        sp.line.fill.background()
    else:
        sp.line.color.rgb = line_color
        sp.line.width = Pt(line_w)
    if not shadow:
        sp.shadow.inherit = False
    if adj is not None:
        try:
            sp.adjustments[0] = adj
        except Exception:
            pass
    tf = sp.text_frame
    tf.word_wrap = True
    tf.margin_left = tf.margin_right = Inches(0.16)
    tf.margin_top = tf.margin_bottom = Inches(0.10)
    tf.vertical_anchor = MSO_ANCHOR.TOP
    return sp


def card(slide, x, y, w, h, fill, accent, radius=0.04):
    sp = rect(slide, x, y, w, h, fill=fill, line_color=LINE, line_w=0.75,
              shape=MSO_SHAPE.ROUNDED_RECTANGLE, adj=radius)
    if accent is not None:
        rect(slide, x, y + 0.16, 0.075, h - 0.32, fill=accent)
    return sp


def hline(slide, x, y, w, color=ORANGE, weight=3.0):
    ln = slide.shapes.add_shape(MSO_SHAPE.RECTANGLE, Inches(x), Inches(y), Inches(w),
                                Pt(weight))
    ln.fill.solid()
    ln.fill.fore_color.rgb = color
    ln.line.fill.background()
    ln.shadow.inherit = False
    return ln


def dot(slide, cx, cy, d, color=ORANGE):
    sp = slide.shapes.add_shape(MSO_SHAPE.OVAL, Inches(cx - d / 2), Inches(cy - d / 2),
                                Inches(d), Inches(d))
    sp.fill.solid()
    sp.fill.fore_color.rgb = color
    sp.line.fill.background()
    sp.shadow.inherit = False
    return sp


# ---------------------------------------------------------------- slide chrome
def strip_body(slide):
    """Remove the template's decorative orange rule group (we redraw a uniform one)."""
    for sh in list(slide.shapes):
        if sh.shape_type == 6 and 2.0 < Emu(sh.top).inches < 2.7:
            sh._element.getparent().remove(sh._element)


def set_title(slide, text, size=40):
    strip_body(slide)
    for sh in slide.shapes:
        if sh.has_text_frame and sh.shape_type == 17 and Emu(sh.top).inches < 2.5:
            tf = sh.text_frame
            tf.clear()
            tf.word_wrap = False
            para(tf, text, size, True, INK, HEAD, first=True, line=1.0)
            w = 0.63 * size / 72.0 * len(text) + 0.4
            sh.width = Inches(min(w, 13.5))
            hline(slide, 0.91 + w + 0.55, 2.40, 19.10 - (0.91 + w + 0.55))
            dot(slide, 19.10, 2.40 + 0.021, 0.19)
            return sh
    raise RuntimeError("title box not found")


def kicker(slide, text, x=0.91, y=3.02, color=MUTED):
    _, tf = textbox(slide, x, y, 18.2, 0.3)
    para(tf, text.upper(), 13, True, color, HEAD, first=True)


def feature_header(slide, text, x, y, w, color=ORANGE, size=17):
    sp = rect(slide, x, y, w, 0.42, fill=color)
    tf = sp.text_frame
    tf.vertical_anchor = MSO_ANCHOR.MIDDLE
    tf.margin_left = Inches(0.18)
    para(tf, text, size, True, WHITE, HEAD, first=True, align=PP_ALIGN.LEFT)
    return sp


def table(slide, x, y, w, cols, rows, header_fill=INK, row_h=0.52, head_h=0.46,
          fs=14.5, hs=14.5, zebra=TINT_N):
    gf = slide.shapes.add_table(len(rows) + 1, len(cols), Inches(x), Inches(y), Inches(w),
                                Inches(head_h + row_h * len(rows)))
    tbl = gf.table
    tbl.first_row = True
    for i, cw in enumerate(cols):
        tbl.columns[i].width = Inches(cw)
    tbl.rows[0].height = Inches(head_h)
    # header
    for c, txt in enumerate(rows[0]):
        cell = tbl.cell(0, c)
        cell.fill.solid()
        cell.fill.fore_color.rgb = header_fill
        cell.vertical_anchor = MSO_ANCHOR.MIDDLE
        cell.margin_left = cell.margin_right = Inches(0.12)
        cell.margin_top = cell.margin_bottom = Inches(0.04)
        tf = cell.text_frame
        tf.word_wrap = True
        para(tf, txt, hs, True, WHITE, HEAD, first=True, line=1.0)
    for r, row in enumerate(rows[1:], start=1):
        tbl.rows[r].height = Inches(row_h)
        for c, txt in enumerate(row):
            cell = tbl.cell(r, c)
            cell.fill.solid()
            cell.fill.fore_color.rgb = WHITE if r % 2 else zebra
            cell.vertical_anchor = MSO_ANCHOR.MIDDLE
            cell.margin_left = cell.margin_right = Inches(0.12)
            cell.margin_top = cell.margin_bottom = Inches(0.03)
            tf = cell.text_frame
            tf.word_wrap = True
            bold = (c == 0)
            col = INK if c == 0 else BODY
            if isinstance(txt, tuple):
                txt, bold, col = txt
            para(tf, txt, fs, bold, col, HEAD if bold else TEXT, first=True, line=1.05)
    return gf


def stat_card(slide, x, y, w, h, value, label, note, accent=ORANGE):
    card(slide, x, y, w, h, WHITE, accent)
    _, tf = textbox(slide, x + 0.32, y + 0.14, w - 0.5, h - 0.28)
    para(tf, value, 30, True, accent, HEAD, first=True, line=1.0)
    para(tf, label, 15, True, INK, HEAD, space_before=2, line=1.05)
    para(tf, note, 12.5, False, MUTED, TEXT, space_before=2, line=1.05)


def bullet_list(tf, items, size=15, gap=5, color=BODY, marker="–  ", mcolor=ORANGE, first=True):
    for i, it in enumerate(items):
        if isinstance(it, tuple):
            lead, rest = it
        else:
            lead, rest = None, it
        kw = {"size": size, "bold": True, "color": mcolor, "font": TEXT}
        base = {"size": size, "bold": False, "color": color, "font": TEXT}
        runs = [(marker, kw)]
        if lead:
            runs.append((lead, {**base, "bold": True, "color": INK}))
        runs.append((rest, base))
        para(tf, "", size, first=(first and i == 0), space_after=gap, runs=runs, line=1.10)


# ---------------------------------------------------------------- build
prs = Presentation(SRC)
S = list(prs.slides)
cover, problem, solution, flow, stack, usp, feas, refs, guide = S[0], S[1], S[2], S[3], S[4], S[5], S[6], S[7], S[8]

# ---- delete GUIDELINES slide (slide 9 of the template)
sldIdLst = prs.slides._sldIdLst
guide_id = list(sldIdLst)[8]
prs.part.drop_rel(guide_id.get(qn("r:id")))
sldIdLst.remove(guide_id)

# ---- new "EXPECTED IMPACT" slide, cloned from the USP frame
def clone_slide(source):
    layout = source.slide_layout
    dest = prs.slides.add_slide(layout)
    for sh in list(dest.shapes):
        sh._element.getparent().remove(sh._element)
    for sh in source.shapes:
        dest.shapes._spTree.append(copy.deepcopy(sh._element))
    return dest


impact = clone_slide(usp)


def move_slide(slide, new_index):
    el = list(sldIdLst)[list(prs.slides).index(slide)]
    sldIdLst.remove(el)
    sldIdLst.insert(new_index, el)


move_slide(impact, 6)  # after USP -> position 7 (1-based)
slides = list(prs.slides)

# =============================================================== 1 COVER
COVER_VALUES = {
    "TEAM NAME": "[ enter your team name ]",
    "PROBLEM STATEMENT": "Golden-hour cyber-crime reporting, in your language",
    "TEAM MEMBERS": "[ name 1  \u00b7  name 2  \u00b7  name 3 ]",
    "COLLEGE": "[ college / institute ]",
}
for sh in cover.shapes:
    if not (sh.has_text_frame and sh.shape_type == 17):
        continue
    label = sh.text_frame.text.strip().rstrip(":")
    if label not in COVER_VALUES:
        continue
    tf = sh.text_frame
    tf.clear()
    tf.word_wrap = False
    para(tf, "", 17, first=True, line=1.0, runs=[
        (label + ":  ", {"size": 17, "bold": True, "color": INK, "font": HEAD}),
        (COVER_VALUES[label], {"size": 14.5, "bold": False, "color": BODY, "font": TEXT})])

sp = rect(cover, 9.95, 7.55, 9.45, 2.72, fill=WHITE, line_color=ORANGE, line_w=1.25,
          shape=MSO_SHAPE.ROUNDED_RECTANGLE, adj=0.05)
tf = sp.text_frame
tf.margin_left = Inches(0.34)
tf.margin_top = Inches(0.22)
para(tf, "THE PROJECT", 13, True, ORANGE, HEAD, first=True, space_after=4)
para(tf, "CYBRIDGE", 40, True, INK, HEAD, line=0.95)
para(tf, "Cyber Victim Assistant", 19, True, GREEN, HEAD, space_after=8)
para(tf, "An on-device, voice-first AI co-pilot that walks a cybercrime victim from panic to a "
         "registered, legally correct FIR \u2014 inside the golden hour.", 14.5, False, BODY, TEXT,
     line=1.15, space_after=8)
para(tf, "ANDROID 15  \u00b7  KOTLIN + COMPOSE  \u00b7  GEMINI  \u00b7  12 INDIAN LANGUAGES  \u00b7  BUILT & TESTED ON REDMI 13 5G",
     11.5, True, MUTED, HEAD)

# =============================================================== 2 PROBLEM
set_title(problem, "PROBLEM STATEMENT")
kicker(problem, "Every day, thousands of victims lose the only window in which their money can still be frozen")

_, tf = textbox(problem, 0.91, 3.62, 10.5, 0.9)
para(tf, "", 16, first=True, line=1.18, runs=[
    ("Under the MHA / I4C framework, a fraud victim has a ", {"size": 16, "color": BODY, "font": TEXT}),
    ("\u201cgolden hour\u201d of roughly 60\u2013120 minutes", {"size": 16, "bold": True, "color": INK, "font": TEXT}),
    (" to reach 1930 / CFCFRMS before the beneficiary bank moves the money out. The official portal exists \u2014 "
     "but it was never built for a panicked, non-technical, first-time user.", {"size": 16, "color": BODY, "font": TEXT})])

barriers = [
    ("01", "Panic and information asymmetry",
     "Victims do not know which identifiers a complaint legally requires \u2014 12-digit UTR/RRN, beneficiary VPA, "
     "transaction timestamp, SMS header, IP log."),
    ("02", "A portal built for clerks, not victims",
     "cybercrime.gov.in nests categories and sub-categories with legal wording across hundreds of dynamic fields. "
     "Filing a UPI task-fraud under \u201cHacking\u201d delays bank lien-marking for weeks."),
    ("03", "Evidence dies before it is filed",
     "Chat histories, scam APKs, debit SMS and payment screenshots are deleted, overwritten or lost in the first hours."),
    ("04", "Nobody can draft the law",
     "Police and bank nodal officers expect Section 66C/66D IT Act, Section 318(4) BNS and Section 91 CrPC/BNSS "
     "citations. Victims cannot produce them."),
]
y = 4.72
for num, head, body in barriers:
    card(problem, 0.91, y, 10.5, 1.22, WHITE, ORANGE)
    _, tf = textbox(problem, 1.16, y + 0.16, 0.75, 0.9)
    para(tf, num, 26, True, ORANGE, HEAD, first=True, line=1.0)
    _, tf = textbox(problem, 1.95, y + 0.15, 9.2, 1.0)
    para(tf, head, 16.5, True, INK, HEAD, first=True, space_after=2, line=1.0)
    para(tf, body, 13.5, False, BODY, TEXT, line=1.10)
    y += 1.33

_, tf = textbox(problem, 11.95, 3.62, 7.45, 0.4)
para(tf, "THE SCALE OF IT", 13, True, MUTED, HEAD, first=True)
stat_card(problem, 11.95, 4.02, 7.45, 1.55, "65.9 lakh+",
          "financial-fraud complaints on the National Cyber Crime Reporting Portal",
          "2021\u20132025, I4C / MHA (Ministry of Information & Broadcasting reply, 2026)")
stat_card(problem, 11.95, 5.67, 7.45, 1.55, "\u2248 \u20b919,800 crore",
          "reported cyber-fraud losses in 2025 alone",
          "\u224821.7 lakh complaints registered \u2014 I4C data reported by the press", GREEN)
stat_card(problem, 11.95, 7.32, 7.45, 1.55, "60\u2013120 min",
          "the entire window in which a freeze can still work",
          "Dial 1930 \u00b7 CFCFRMS lien-marking before inter-bank settlement", PURPLE)

sp = rect(problem, 11.95, 8.97, 7.45, 1.15, fill=TINT_O, line_color=ORANGE, line_w=0.75,
          shape=MSO_SHAPE.ROUNDED_RECTANGLE, adj=0.08)
tf = sp.text_frame
tf.margin_left = Inches(0.22)
para(tf, "WHAT THAT COSTS", 12, True, ORANGE, HEAD, first=True, space_after=3)
para(tf, "Late or wrongly categorised complaints are the reason money that was still traceable is already gone \u2014 "
         "and why a large share of victims simply stop halfway and never file.", 13.5, False, BODY, TEXT, line=1.12)

# =============================================================== 3 SOLUTION
set_title(solution, "PROPOSED SOLUTION")
kicker(solution, "Cybridge \u2014 one app that stays with the victim from the first minute until the case is registered")

sp = rect(solution, 0.91, 3.60, 18.5, 0.92, fill=INK, shape=MSO_SHAPE.ROUNDED_RECTANGLE, adj=0.10)
tf = sp.text_frame
tf.vertical_anchor = MSO_ANCHOR.MIDDLE
tf.margin_left = Inches(0.30)
para(tf, "", 16, first=True, line=1.10, runs=[
    ("An on-device, privacy-first Android assistant ", {"size": 16.5, "bold": True, "color": WHITE, "font": HEAD}),
    ("that mines the victim's own SMS and screenshots, maps the fraud to the exact official MHA category, "
     "pre-fills the live portal under a visible \u201cLive Eye\u201d monitor, and hands back a police-grade complaint "
     "pack \u2014 while the victim keeps every irreversible decision.",
     {"size": 16, "color": RGBColor(0xE8, 0xE8, 0xE8), "font": TEXT})])

pillars = [
    ("SPEAK, DON'T TYPE", "Voice-first intake in 12 Indian languages",
     "A floating mic and \u201cSpeak out\u201d mode let the victim narrate the fraud. Context-aware guidance knows which "
     "of the 18 screens they are on.", ORANGE),
    ("EVIDENCE", "Automatic fact mining, on-device",
     "Offline ML Kit OCR + bank-SMS parsing pull UPI/VPA ids, 12-digit UTR/RRN, amounts and timestamps "
     "straight into the case file.", GREEN),
    ("ROUTING", "The exact official category",
     "A knowledge base built from the MHA Citizen Manual maps the story to the right portal buttons \u2014 "
     "40/40 scenarios correct, deterministically, with no network.", PURPLE),
    ("AUTOMATION", "Live Eye portal companion",
     "A WebView engine highlights each field as it fills it and prints a live audit receipt: action, target, "
     "masked value, status.", ORANGE),
    ("LEGAL OUTPUT", "Police-grade documents",
     "Multi-page Complaint Pack PDF (hard <8 MB budget so it never bounces) plus Section 91 CrPC bank-nodal "
     "freeze and CDR preservation letters.", GREEN),
    ("AFTERWARDS", "Follow-up tracker",
     "Statutory deadlines chased automatically: 24-hour bank nodal, 3-day police acknowledgement, "
     "7-day magistrate compliance.", PURPLE),
]
x0, y0, cw, ch, gx, gy = 0.91, 4.72, 6.03, 2.28, 0.205, 0.20
for i, (tag, head, body, accent) in enumerate(pillars):
    cx = x0 + (i % 3) * (cw + gx)
    cy = y0 + (i // 3) * (ch + gy)
    card(solution, cx, cy, cw, ch, TINT_N if i % 2 == 0 else WHITE, accent)
    _, tf = textbox(solution, cx + 0.30, cy + 0.16, cw - 0.5, ch - 0.3)
    para(tf, tag, 11.5, True, accent, HEAD, first=True, space_after=3)
    para(tf, head, 17, True, INK, HEAD, space_after=4, line=1.02)
    para(tf, body, 13.5, False, BODY, TEXT, line=1.13)

sp = rect(solution, 0.91, 9.62, 18.5, 0.62, fill=TINT_G, line_color=GREEN, line_w=0.9,
          shape=MSO_SHAPE.ROUNDED_RECTANGLE, adj=0.16)
tf = sp.text_frame
tf.vertical_anchor = MSO_ANCHOR.MIDDLE
tf.margin_left = Inches(0.26)
para(tf, "", 14, first=True, runs=[
    ("NON-NEGOTIABLE:  ", {"size": 14, "bold": True, "color": GREEN, "font": HEAD}),
    ("AI never presses Submit, Preview or Generate Token (VICTIM_LOCK). CAPTCHA, OTP and bank passwords are "
     "HUMAN_ONLY and are never read. Screenshots never leave the phone \u2014 all OCR is offline.",
     {"size": 14.5, "color": BODY, "font": TEXT})])

# =============================================================== 4 FLOW
set_title(flow, "FLOW OF SOLUTION")
kicker(flow, "One session, five phases \u2014 case facts collected once, reused by every downstream engine")

phases = [
    ("MINUTE 0", "Land & language",
     ["Splash \u2192 pick 1 of 12 languages", "TTS rate + \u201chear a sample\u201d", "1930 emergency dial, one tap"], ORANGE),
    ("PHASE 1", "Tell us what happened",
     ["Free narration by voice or text", "Guided 8-step intake fills gaps", "Narrative + case session created"], GREEN),
    ("PHASE 2", "Build the evidence",
     ["Vault: photos, SMS, .eml, chats", "Offline ML Kit OCR + regex facts", "SHA-256 hash, confidence, audit log"], PURPLE),
    ("PHASE 3", "File on the real portal",
     ["PlanBuilder \u2192 exact MHA buttons", "WebView driver pre-fills + highlights", "Victim reviews, victim submits"], ORANGE),
    ("PHASE 4", "Legal pack & follow-up",
     ["Complaint PDF <8 MB", "Sec 91 CrPC freeze / CDR letters", "Tracker: 24 h \u00b7 3 d \u00b7 7 d reminders"], GREEN),
]
x0, cw, gap = 0.91, 3.53, 0.21
for i, (tag, head, items, accent) in enumerate(phases):
    cx = x0 + i * (cw + gap)
    card(flow, cx, 3.66, cw, 2.92, WHITE, accent)
    _, tf = textbox(flow, cx + 0.28, 3.82, cw - 0.5, 0.9)
    para(tf, tag, 11.5, True, accent, HEAD, first=True, space_after=2)
    para(tf, head, 16.5, True, INK, HEAD, line=1.02)
    _, tf = textbox(flow, cx + 0.28, 4.82, cw - 0.5, 1.65)
    bullet_list(tf, items, size=12.5, gap=5, mcolor=accent)
    if i < 4:
        dot(flow, cx + cw + gap / 2, 5.12, 0.16, RGBColor(0xBF, 0xBF, 0xBF))

feature_header(flow, "ALWAYS ON, ACROSS ALL 18 SCREENS", 0.91, 6.78, 6.4, INK, 13)
card(flow, 0.91, 7.26, 9.12, 2.78, TINT_N, ORANGE)
_, tf = textbox(flow, 1.20, 7.44, 8.6, 2.45)
para(tf, "Voice Guide \u2014 STT \u2192 context \u2192 guidance \u2192 TTS", 16, True, INK, HEAD, first=True, space_after=5)
bullet_list(tf, [
    ("SttEngine: ", "Android SpeechRecognizer, tuned 1200 ms / 1000 ms silence so slow speakers are not cut off."),
    ("ScreenDescriptorRegistry: ", "tracks the active screen, so the answer matches where the victim actually is."),
    ("VoiceGuideClient: ", "answers on-device first for speed; Gemini only when reasoning is needed. Emojis and markdown stripped before TTS."),
    ("TtsManager: ", "12 Indian locales, utterance tracking, graceful fallback when a voice pack is missing."),
], size=12.5, gap=4, first=False)

card(flow, 10.29, 7.26, 9.12, 2.78, TINT_O, GREEN)
_, tf = textbox(flow, 10.58, 7.44, 8.6, 2.45)
para(tf, "Trust boundary \u2014 ActionPolicy, enforced in code", 16, True, INK, HEAD, first=True, space_after=5)
bullet_list(tf, [
    ("AI_AUTO: ", "category dropdowns, dates, narrative text, transaction fields."),
    ("HUMAN_ONLY: ", "CAPTCHA, SMS OTP, net-banking password, Aadhaar \u2014 never inspected, never filled."),
    ("VICTIM_LOCK: ", "Preview, Final Submit, Token generation. The engine halts and says \u201creview it, then tap Submit yourself\u201d."),
    ("Local-first: ", "Room DB + AES-256-GCM EncryptedSharedPreferences; raw evidence is never uploaded."),
], size=12.5, gap=4, mcolor=GREEN, first=False)

# =============================================================== 5 TECH STACK
set_title(stack, "TECH STACK")
kicker(stack, "Everything that touches a victim's data runs on the victim's device; the network is strictly opt-in")

rows = [
    ("Layer", "Technology", "What it does here", "Why this choice"),
    ("Platform", "Kotlin 2.0.20 \u00b7 Jetpack Compose Material 3 \u00b7 Coroutines & Flow",
     "18 screens, minSdk 26 / targetSdk 35 (Android 15)", "Runs on a budget Android phone \u2014 no iPhone-only assumption"),
    ("UI / L10n", "Compose theming \u00b7 L10n.kt repository \u00b7 tricolor gov palette",
     "Live switch across EN, HI, TE, PA, GU, MR, TA, BN, KN, ML, OR, AS", "Victims report best in their mother tongue"),
    ("Voice", "Android SpeechRecognizer \u00b7 TextToSpeech \u00b7 screen-context registry",
     "Hands-free \u201cSpeak out\u201d intake and per-screen guidance", "Native APIs \u2014 no per-minute speech billing, works offline"),
    ("On-device AI", "Google ML Kit Text Recognition \u00b7 regex fact extractors",
     "OCR of screenshots \u2192 UPI/VPA, 12-digit UTR/RRN, amounts, dates", "Zero image upload; subsampled decode avoids OOM crashes"),
    ("LLM", "Gemini flash (primary) + flash-latest fallback, thinkingBudget = 0",
     "Picks a scenario id and drafts the narrative only", "Model can never invent portal buttons; validated against the KB"),
    ("Automation", "Android WebView + injected DOM engine, cookie sync",
     "Live Eye pre-fill, #00C853 element highlight, audit receipts", "Skyvern-style vision agent, but bounded by ActionPolicy"),
    ("Documents", "Custom PDF writer \u00b7 RFC 822/2047 MIME parser \u00b7 SPF/DKIM/DMARC heuristics",
     "Complaint Pack <8 MB, Sec 91 CrPC notices, phishing score", "Deterministic, testable, no heavy JavaMail dependency"),
    ("Storage & security", "Room \u00b7 Android Keystore \u00b7 EncryptedSharedPreferences (AES-256-GCM) \u00b7 SHA-256",
     "CaseDb: EvidenceItem, CaseFact, append-only CaseEvent", "Tamper-evident chain of custody for court"),
    ("Backend", "Node.js Express (port 8081) \u00b7 Firebase Functions, asia-south1",
     "Email OTP verify \u00b7 /breaches/:email (XposedOrNot, optional HIBP) \u00b7 rate limit", "Only opt-in lookups; demo mode is labelled in the API itself"),
    ("Quality", "Gradle 8.9 \u00b7 AGP 8.5.2 \u00b7 JDK 21 \u00b7 7 JUnit suites",
     "SCENARIO-ACCURACY 40/40, parser, budget and scoring gates", "Every legal claim in the app has a test behind it"),
]
table(stack, 0.91, 3.66, 18.5, [2.55, 5.15, 5.45, 5.35], rows, row_h=0.545, head_h=0.44, fs=12.5, hs=13.5)

_, tf = textbox(stack, 0.91, 9.52, 18.5, 0.4)
para(tf, "Implementation approach: deterministic engine first, LLM second, human always last \u2014 the app still "
         "produces a correct filing plan with no network and no model available.", 13.5, True, GREEN, HEAD, first=True)

# =============================================================== 6 USP
set_title(usp, "UNIQUE SELLING PROPOSITION")
kicker(usp, "Why this cannot be reproduced by bolting a chatbot onto the existing portal")

usps = [
    ("Visible automation, not blind automation",
     "Every AI action is shown as it happens \u2014 the target field lights up green and a live receipt prints "
     "[FILL] [#account_no] [********4210] [VERIFIED]. Browser agents do this for merchants; nobody has done it "
     "for a citizen filing a police complaint.", ORANGE),
    ("A hard, auditable human-in-the-loop lock",
     "ActionPolicy is an enum compiled into the engine, not a prompt. Submit / Preview / Token are permanently "
     "VICTIM_LOCK, CAPTCHA-OTP-password are HUMAN_ONLY. No jailbreak of the model can submit a complaint.", GREEN),
    ("Deterministic-first, so accuracy never collapses",
     "The scenario picker is an on-device knowledge base extracted from the official MHA manual (last verified "
     "2026-10-22). 40/40 real Indian fraud scenarios route correctly with the network off; Gemini is an "
     "augmentation, and the fallback is the same deterministic engine.", PURPLE),
    ("Vernacular and voice are the default path, not a setting",
     "12 Indian languages with context-aware spoken guidance and hands-free narration \u2014 designed for a victim "
     "who is shaking, low-literacy, or using a shared family phone.", ORANGE),
    ("It finishes the job the portal starts",
     "Evidence hashing, statutory drafting (66C/66D IT Act, 318(4) BNS, 91 CrPC/BNSS), an 8 MB-safe complaint "
     "pack, and 24 h / 3 d / 7 d follow-up reminders \u2014 the part every other tool stops before.", GREEN),
    ("Privacy that survives scrutiny",
     "Local-first Room store, AES-256-GCM Keystore tokens, OCR on-device, zero mock data, and an append-only "
     "CaseEvent log that a nodal officer or court can actually audit.", PURPLE),
]
x0, y0, cw, ch, gx, gy = 0.91, 3.66, 9.12, 1.80, 0.26, 0.20
for i, (head, body, accent) in enumerate(usps):
    cx = x0 + (i % 2) * (cw + gx)
    cy = y0 + (i // 2) * (ch + gy)
    card(usp, cx, cy, cw, ch, WHITE if i % 2 else TINT_N, accent)
    _, tf = textbox(usp, cx + 0.32, cy + 0.17, cw - 0.55, ch - 0.3)
    para(tf, head, 16.5, True, INK, HEAD, first=True, space_after=4, line=1.02)
    para(tf, body, 13.5, False, BODY, TEXT, line=1.16)

sp = rect(usp, 0.91, 9.60, 18.5, 0.62, fill=INK, shape=MSO_SHAPE.ROUNDED_RECTANGLE, adj=0.16)
tf = sp.text_frame
tf.vertical_anchor = MSO_ANCHOR.MIDDLE
tf.margin_left = Inches(0.26)
para(tf, "", 14.5, first=True, runs=[
    ("THE PITCH IN ONE LINE:   ", {"size": 13.5, "bold": True, "color": ORANGE, "font": HEAD}),
    ("The only assistant that takes an Indian cybercrime victim from a panicked voice note to a legally complete, "
     "correctly categorised FIR inside the golden hour \u2014 without ever touching a password or pressing Submit.",
     {"size": 15, "color": WHITE, "font": TEXT})])

# =============================================================== 7 IMPACT
set_title(impact, "EXPECTED IMPACT")
kicker(impact, "Measured against the only thing that matters in financial cyber fraud: minutes to a correct filing")

kpis = [
    ("Time to file", "2\u20136 hours, or never", "under 30 minutes", ORANGE),
    ("Required identifiers", "victim must already know them", "auto-mined from SMS + screenshots", GREEN),
    ("Category accuracy", "wrong sub-category \u2192 lien delay", "40/40 on the scenario suite", PURPLE),
    ("Complaint quality", "handwritten, incomplete", "statutory citations + evidence pack", ORANGE),
]
x0, cw, gap = 0.91, 4.51, 0.155
for i, (head, before, after, accent) in enumerate(kpis):
    cx = x0 + i * (cw + gap)
    card(impact, cx, 3.66, cw, 1.92, WHITE, accent)
    _, tf = textbox(impact, cx + 0.28, 3.82, cw - 0.5, 1.65)
    para(tf, head.upper(), 11.5, True, accent, HEAD, first=True, space_after=4)
    para(tf, "TODAY", 11, True, MUTED, HEAD, space_after=1)
    para(tf, before, 13.5, False, MUTED, TEXT, line=1.08, space_after=6)
    para(tf, "WITH CYBRIDGE", 11, True, INK, HEAD, space_after=1)
    para(tf, after, 15.5, True, INK, HEAD, line=1.05)

card(impact, 0.91, 5.82, 9.12, 2.35, TINT_O, ORANGE)
_, tf = textbox(impact, 1.22, 6.00, 8.6, 2.0)
para(tf, "FOR THE VICTIM", 13, True, ORANGE, HEAD, first=True, space_after=6)
bullet_list(tf, [
    "Files inside the freeze window instead of after the money has moved.",
    "No re-typing: one narration becomes the complaint, the PDF and the bank notice.",
    "Understands every step in their own language, spoken aloud.",
    "Keeps control \u2014 the app cannot submit anything without them.",
    "Doesn't need a lawyer, an agent, or a favour to get a valid complaint written.",
], size=13.5, gap=5, mcolor=ORANGE, first=False)

card(impact, 10.29, 5.82, 9.12, 2.35, TINT_G, GREEN)
_, tf = textbox(impact, 10.60, 6.00, 8.6, 2.0)
para(tf, "FOR THE SYSTEM", 13, True, GREEN, HEAD, first=True, space_after=6)
bullet_list(tf, [
    "Correctly categorised cases reach the right nodal officer on day one.",
    "Complete UTR/VPA/timestamp data makes 1930 lien-marking actionable.",
    "Fewer abandoned and duplicate complaints in the cyber cell queue.",
    "Standard, machine-readable evidence reduces police drafting time.",
    "A reusable pattern for citizen-services automation across every MHA portal.",
], size=13.5, gap=5, mcolor=GREEN, first=False)

_, tf = textbox(impact, 0.91, 8.42, 18.5, 0.5)
para(tf, "If even a small share of the 65 lakh+ complaints registered since 2021 were filed complete and inside the "
         "golden hour, the amount still sitting in beneficiary accounts when the freeze lands \u2014 and therefore "
         "recoverable \u2014 rises materially.", 14.5, True, INK, HEAD, first=True, line=1.15)

chips = ["DIGITAL INDIA", "MHA / I4C 1930 CFCFRMS", "ON-DEVICE AI \u00b7 MAKE IN INDIA", "SDG 16 \u00b7 ACCESS TO JUSTICE",
         "SDG 9 \u00b7 INNOVATION", "SDG 10 \u00b7 REDUCED INEQUALITY"]
cx = 0.91
for i, c in enumerate(chips):
    w = 0.38 + 0.105 * len(c)
    sp = rect(impact, cx, 9.28, w, 0.5, fill=WHITE, line_color=(ORANGE, GREEN, PURPLE)[i % 3], line_w=1.0,
              shape=MSO_SHAPE.ROUNDED_RECTANGLE, adj=0.4)
    tf = sp.text_frame
    tf.word_wrap = False
    tf.vertical_anchor = MSO_ANCHOR.MIDDLE
    tf.margin_left = tf.margin_right = Inches(0.05)
    para(tf, c, 11.5, True, (ORANGE, GREEN, PURPLE)[i % 3], HEAD, first=True, align=PP_ALIGN.CENTER)
    cx += w + 0.18

# =============================================================== 8 FEASIBILITY
set_title(feas, "FEASIBILITY & COMPETITORS")
kicker(feas, "Not a prototype \u2014 it is installed, running and tested on a physical Android 15 device today")

feature_header(feas, "WHAT IS ALREADY BUILT", 0.91, 3.62, 5.4, GREEN, 13)
card(feas, 0.91, 4.10, 9.12, 2.78, TINT_G, GREEN)
_, tf = textbox(feas, 1.22, 4.28, 8.6, 2.45)
bullet_list(tf, [
    ("61 Kotlin files, 18 screens, ", "intake \u2192 evidence \u2192 portal \u2192 legal \u2192 follow-up working end to end."),
    ("Verified on Redmi 13 5G (Android 15): ", "language switching, offline OCR, voice guide and live portal DOM highlighting all run on hardware."),
    ("7 JUnit suites green: ", "40/40 scenario routing, MIME parsing, statutory citations, PDF size budget, risk scoring."),
    ("35+ audited defects fixed ", "and the Express backend, Firebase scaffold and Play-policy justifications are in the repo."),
], size=12.5, gap=6, mcolor=GREEN)

feature_header(feas, "RISKS AND HOW THEY ARE HANDLED", 10.29, 3.62, 6.0, PURPLE, 13)
card(feas, 10.29, 4.10, 9.12, 2.78, TINT_P, PURPLE)
_, tf = textbox(feas, 10.60, 4.28, 8.6, 2.45)
bullet_list(tf, [
    ("Portal changes without notice \u2192 ", "the knowledge base carries a lastVerified date, shows it, and a \u201cmy screen looks different\u201d path falls back to manual steps."),
    ("Legal exposure from automation \u2192 ", "VICTIM_LOCK means no automated submission and no CAPTCHA circumvention."),
    ("Model error \u2192 ", "the LLM only selects a validated scenario id; malformed output is rejected and the deterministic picker answers instead."),
    ("Privacy objection \u2192 ", "on-device OCR, AES-256-GCM Keystore tokens, and DEMO DATA labelled inside the API response itself."),
], size=12.5, gap=6, mcolor=PURPLE)

feature_header(feas, "COMPETITOR LANDSCAPE", 0.91, 7.05, 5.2, INK, 13)
rows = [
    ("Who", "What they do", "Where the victim still gets stuck"),
    ("1930 helpline & cybercrime.gov.in",
     "Official reporting and lien-marking", "Manual, English-heavy, no case building, no evidence mining, no drafting help"),
    ("Bank fraud apps / nodal desks",
     "Card and account controls after the fact", "Bank-side only; cannot produce the police complaint or the Sec 91 notice"),
    ("Anti-phishing & device-security apps",
     "Scan the phone, flag malicious APKs", "Device hygiene only; no reporting workflow, no legal output"),
    ("Generic AI legal chatbots",
     "Draft a complaint from a prompt", "Cloud upload of sensitive evidence, invented law, no portal execution, English-first"),
    ("Vision browser agents (Skyvern class)",
     "Automate any web flow", "Built for merchants; no human-lock policy, no vernacular voice, not victim-grade"),
]
table(feas, 0.91, 7.52, 18.5, [4.35, 5.35, 8.80], rows, row_h=0.46, head_h=0.40, fs=12.5, hs=13)

# =============================================================== 9 REFERENCES
set_title(refs, "RESEARCH & REFERENCE")
kicker(refs, "Grounded in official procedure, not in assumptions")

feature_header(refs, "PRIMARY SOURCES & SPECIFICATIONS", 0.91, 3.62, 6.0, ORANGE, 13)
card(refs, 0.91, 4.10, 9.12, 4.55, WHITE, ORANGE)
_, tf = textbox(refs, 1.22, 4.30, 8.6, 4.1)
bullet_list(tf, [
    "Ministry of Home Affairs / I4C \u2014 Indian Cyber Crime Coordination Centre, 1930 helpline and CFCFRMS lien-marking procedure.",
    "cybercrime.gov.in \u2014 MHA Citizen Manual, \u201cReport Other Cyber Crime\u201d (archived snapshot; category and sub-category tree reproduced in app/assets/kb, last verified 2026-10-22).",
    "Press Information Bureau, Govt. of India \u2014 national cyber crime data, financial-fraud complaints on NCRP 2021\u20132025.",
    "Statutes cited by the drafter: Section 66C & 66D Information Technology Act; Section 318(4) Bharatiya Nyaya Sanhita; Section 91 CrPC / BNSS document preservation.",
    "Google ML Kit \u2014 on-device Text Recognition v2. Gemini API \u2014 model routing and JSON schema contract.",
    "RFC 822 / RFC 2047 (MIME), RFC 7636 (OAuth PKCE), RFC 7208 SPF, RFC 6376 DKIM, RFC 7489 DMARC.",
    "Skyvern \u2014 published approach to vision-driven browser automation, the reference model for the Live Eye monitor.",
    "OWASP MASVS + Android Keystore / EncryptedSharedPreferences guidance.",
    "XposedOrNot (free tier) and Have I Been Pwned \u2014 credential exposure lookups.",
], size=12.5, gap=6, mcolor=ORANGE)

feature_header(refs, "CODE, BUILD & DEMO", 10.29, 3.62, 4.6, GREEN, 13)
card(refs, 10.29, 4.10, 9.12, 2.35, TINT_G, GREEN)
_, tf = textbox(refs, 10.60, 4.30, 8.6, 2.0)
para(tf, "GITHUB REPOSITORY", 12, True, GREEN, HEAD, first=True, space_after=2)
para(tf, "[ paste your repo URL ]", 15, True, INK, TEXT, space_after=8)
para(tf, "LIVE DEMO / VIDEO WALKTHROUGH", 12, True, GREEN, HEAD, space_after=2)
para(tf, "[ paste your demo video URL ]", 15, True, INK, TEXT, space_after=8)
para(tf, "APK FOR JUDGE DEVICES", 12, True, GREEN, HEAD, space_after=2)
para(tf, "[ sideload link / QR \u2014 app-debug.apk, Android 8.0+ ]", 13, False, BODY, TEXT)

card(refs, 10.29, 6.62, 9.12, 2.03, TINT_N, PURPLE)
_, tf = textbox(refs, 10.60, 6.80, 8.6, 1.7)
para(tf, "HOW TO RUN IT", 12, True, PURPLE, HEAD, first=True, space_after=5)
para(tf, "./gradlew assembleDebug\nadb install -r -d -t app/build/outputs/apk/debug/app-debug.apk\n"
         "adb shell am start -n com.cybervictim.assistant/.MainActivity\ncd tools/backend && PORT=8081 node server.js",
     12, False, BODY, "Consolas", line=1.25)

_, tf = textbox(refs, 0.91, 8.86, 18.5, 1.2)
para(tf, "Every product figure in this deck is measured on the current build; every public figure is attributed to a "
         "source above. The app ships with zero mock data \u2014 no invented citizen names, phone numbers or bank "
         "accounts; the placeholder fields in this presentation are the team's own details, to be filled before upload.",
     13, False, MUTED, TEXT, first=True, line=1.18)

prs.save(OUT)
print("wrote", OUT, "slides:", len(list(prs.slides)))
