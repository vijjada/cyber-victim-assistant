package com.cybervictim.assistant.ui

import android.content.Context
import androidx.compose.runtime.mutableStateOf

/**
 * In-app language switcher (Phase 7): English / हिंदी / తెలుగు.
 * Compose-visible state so every screen using t() re-translates live.
 * Falls back to English for any missing key.
 */
object L10n {

    enum class Lang(val code: String, val label: String) {
        EN("en", "English"),
        HI("hi", "हिंदी"),
        TE("te", "తెలుగు"),
    }

    private const val PREFS = "cva_settings"
    private const val KEY_LANG = "language"

    var lang: Lang = Lang.EN
        private set

    // Read by t(); bumping it re-composes every translated call site.
    private val tick = mutableStateOf(0)

    fun init(ctx: Context) {
        val saved = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY_LANG, "en") ?: "en"
        lang = Lang.entries.firstOrNull { it.code == saved } ?: Lang.EN
    }

    fun setLang(ctx: Context, to: Lang) {
        lang = to
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(KEY_LANG, to.code).apply()
        tick.value += 1
    }

    fun t(key: String): String {
        tick.value // dependency read for recomposition
        val m = i18n[key] ?: return key
        return m[lang] ?: m[Lang.EN] ?: key
    }

    private fun tr(en: String, hi: String, te: String): Map<Lang, String> =
        mapOf(Lang.EN to en, Lang.HI to hi, Lang.TE to te)

    private val i18n = mapOf(
        // Bottom bar tabs
        "tab_home" to tr("Home", "होम", "హోమ్"),
        "tab_guide" to tr("Guide", "गाइड", "గైడ్"),
        "tab_evidence" to tr("Evidence", "साक्ष्य", "సాక్ష్యం"),
        "tab_requests" to tr("Requests", "निवेदन", "విజ్ఞప్తులు"),
        "tab_more" to tr("More", "और", "ఇంకా"),

        // More menu
        "more_title" to tr("More", "और", "ఇంకా"),
        "more_subtitle" to tr("Everything beyond the four main tabs", "चार मुख्य टैब के अलावा बाकी सब", "నాలుగు ప్రధాన ట్యాబ్‌ల వెలుపల మిగిలినవన్నీ"),
        "more_portal" to tr("Portal Guide", "पोर्टल गाइड", "పోర్టల్ గైడ్"),
        "more_portal_sub" to tr(
            "Step-by-step AI plan for cybercrime.gov.in — AI fills, YOU submit",
            "cybercrime.gov.in हेतु चरण-दर-चरण AI योजना — भरना AI करेगा, भेजना आप करेंगे",
            "cybercrime.gov.in కోసం దశలవారీ AI ప్రణాళిక — పూరించడం AI చేస్తుంది, పంపడం మీరు చేస్తారు",
        ),
        "more_tracker" to tr("Case Tracker and Reminders", "केस ट्रैकर और रिमाइंडर", "కేస్ ట్రాకర్ మరియు రిమైండర్‌లు"),
        "more_tracker_sub" to tr(
            "Your case event timeline and follow-up reminders",
            "आपके केस की घटनाओं की समयरेखा और फॉलो-अप अनुस्मारक",
            "మీ కేసు ఘటనల కాలరేఖ మరియు తదుపరి రిమైండర్‌లు",
        ),
        "more_phonetrace" to tr("PhoneTrace", "फ़ोनट्रेस", "ఫోన్‌ట్రేస్"),
        "more_phonetrace_sub" to tr(
            "Scan the phone for remote-control apps and stalkerware",
            "रिमोट-कंट्रोल ऐप्स और स्टॉकरवेयर के लिए फ़ोन जाँच",
            "రిమోట్-కంట్రోల్ యాప్‌లు మరియు స్టాకర్‌వేర్ కోసం ఫోన్ తనిఖీ",
        ),
        "more_exposure" to tr("Exposure Warning", "एक्सपोज़र चेतावनी", "ఎక్స్‌పోజర్ హెచ్చరిక"),
        "more_exposure_sub" to tr(
            "Email verification + breach and password exposure check",
            "ईमेल सत्यापन + डेटा लीक और पासवर्ड जोखिम जाँच",
            "ఇమెయిల్ ధృవీకరణ + డేటా లీక్ మరియు పాస్‌వర్డ్ ప్రమాద తనిఖీ",
        ),
        "more_settings" to tr("Settings and Privacy", "सेटिंग्स और गोपनीयता", "సెట్టింగ్‌లు మరియు గోప్యత"),
        "more_settings_sub" to tr(
            "Case data controls, notifications, app information",
            "केस डेटा नियंत्रण, सूचनाएँ, ऐप जानकारी",
            "కేసు డేటా నియంత్రణలు, నోటిఫికేషన్‌లు, యాప్ సమాచారం",
        ),
        "coming_soon" to tr("Coming soon", "जल्द आ रहा है", "త్వరలో వస్తుంది"),

        // Emergency dialer cards
        "dial_1930" to tr(
            "Call 1930 — National Cyber Crime Helpline",
            "कॉल करें 1930 — राष्ट्रीय साइबर अपराध हेल्पलाइन",
            "కాల్ చేయండి 1930 — జాతీయ సైబర్ క్రైమ్ హెల్ప్‌లైన్",
        ),
        "dial_1930_sub" to tr(
            "Report money fraud within the golden hour",
            "गोल्डन आवर के भीतर पैसों की धोखाधड़ी की रिपोर्ट करें",
            "గోల్డెన్ అవర్‌లో డబ్బుల మోసాన్ని నివేదించండి",
        ),
        "dial_1927" to tr(
            "Call 1927 — Anti-Human Trafficking Helpline",
            "कॉल करें 1927 — मानव तस्करी विरोधी हेल्पलाइन",
            "కాల్ చేయండి 1927 — మానవ అక్రమ రవాణా వ్యతిరేక హెల్ప్‌లైన్",
        ),
        "dial_1927_sub" to tr(
            "Help for trafficking and extortion situations",
            "तस्करी और ब्लैकमेलिंग स्थितियों में सहायता",
            "అక్రమ రవాణా మరియు బ్లాక్‌మెయిల్ పరిస్థితుల్లో సహాయం",
        ),

        // Settings — language picker
        "set_language" to tr("Language", "भाषा", "భాష"),
        "set_language_hint" to tr(
            "Switches instantly. Your data stays on this phone in every language.",
            "तुरंत बदलता है। हर भाषा में आपका डेटा इसी फ़ोन पर रहता है।",
            "వెంటనే మారుస్తుంది. ఏ భాషలోనైనా మీ డేటా ఈ ఫోన్‌లోనే ఉంటుంది.",
        ),

        // ---------- Phase 9-A full sweep ----------

        // Home
        "home_help" to tr("I need help now", "मुझे अभी मदद चाहिए", "నాకు ఇప్పుడు సహాయం కావాలి"),
        "home_open_case" to tr("Open case", "केस की जानकारी", "కేసు సమాచారం"),
        "home_case_info" to tr("%1\$s • %2\$d steps logged", "%1\$s • %2\$d चरण दर्ज हैं", "%1\$s • %2\$d దశలు నమోదు చేయబడ్డాయి"),
        "home_no_case" to tr("No case yet — tap above to start.", "अभी कोई केस नहीं — शुरू करने के लिए ऊपर टैप करें।", "ఇంకా కేసు లేదు — ప్రారంభించడానికి పైన ట్యాప్ చేయండి."),
        "home_next" to tr("Next step", "अगला चरण", "తర్వాతి దశ"),
        "home_start0" to tr("Describe what happened to the AI Case Guide.", "AI केस गाइड को बताइए कि क्या हुआ।", "ఏమి జరిగిందో AI కేసు గైడ్‌కి చెప్పండి."),
        "home_add_evidence" to tr("Add evidence in the Case Builder before anything is deleted.", "कुछ भी हटाए जाने से पहले केस बिल्डर में सबूत जोड़ें।", "ఏదైనా తొలగించబడటానికి ముందు కేసు బిల్డర్‌లో సాక్ష్యాన్ని జోడించండి."),
        "home_analyze" to tr("Run “Analyze evidence with AI” to build your facts.", "तथ्यों को बनाने के लिए “AI से सबूत जाँचें” चलाएँ।", "మీ వాస్తవాలను నిర్మించడానికి “AI తో సాక్ష్యాన్ని విశ్లేషించండి” నడపండి."),
        "home_go_portal" to tr("Go to the Portal Guide — the AI can now file with your facts.", "पोर्टल गाइड पर जाएँ — AI अब आपके तथ्यों से फ़ाइल कर सकता है।", "పోర్టల్ గైడ్‌కి వెళ్లండి — AI ఇప్పుడు మీ వాస్తవాలతో దాఖలు చేయగలదు."),
        "home_latest" to tr("Latest", "नवीनतम", "తాజాది"),

        // Tracker
        "trk_title" to tr("Case Tracker", "केस ट्रैकर", "కేసు ట్రాకర్"),
        "trk_empty" to tr("No case events yet. Start from the AI Case Guide.", "अभी कोई केस घटना नहीं। AI केस गाइड से शुरू करें।", "ఇంకా కేసు ఘటనలు లేవు. AI కేసు గైడ్ నుండి ప్రారంభించండి."),

        // Guide chat labels (Gemini replies themselves stay English here)
        "gd_welcome" to tr("I'm your cybercrime help guide. Tell me what happened in your own words.", "मैं आपका साइबर अपराध सहायता गाइड हूँ। अपने शब्दों में बताइए क्या हुआ।", "నేను మీ సైబర్ క్రైమ్ సహాయ గైడ్‌ని. ఏమి జరిగిందో మీ సొంత మాటల్లో చెప్పండి."),
        "gd_hint" to tr("Describe what happened. Example: I paid Rs.50000 by UPI and then they blocked me.", "बताइए क्या हुआ। उदाहरण: मैंने UPI से 50000 रु. दिए और फिर वे मुझे ब्लॉक करने लगे।", "ఏమి జరిగిందో వివరించండి. ఉదాహరణ: నేను UPI ద్వారా 50000 రూ. చెల్లించాను, ఆపై వారు నన్ను బ్లాక్ చేశారు."),
        "gd_send" to tr("Send", "भेजें", "పంపండి"),
        "gd_err" to tr("Connection problem: %1\$s. Your case data stays on the phone; try again.", "कनेक्शन समस्या: %1\$s. आपका डेटा फ़ोन पर ही रहता है; फिर कोशिश करें।", "కనెక్షన్ సమస్య: %1\$s. మీ డేటా ఫోన్‌లోనే ఉంటుంది; మళ్లీ ప్రయత్నించండి."),

        // Evidence / Case Builder
        "ev_title" to tr("Case Builder", "केस बिल्डर", "కేసు బిల్డర్"),
        "ev_sub" to tr("Upload screenshots, SMS and bank statements before anything is deleted. Everything stays on this phone.", "कुछ हटाए जाने से पहले स्क्रीनशॉट, SMS और बैंक विवरण अपलोड करें। सब कुछ इसी फ़ोन पर रहता है।", "ఏదైనా తొలగించే ముందు స్క్రీన్‌షాట్‌లు, SMS మరియు బ్యాంక్ స్టేట్‌మెంట్‌లను అప్‌లోడ్ చేయండి. అంతా ఈ ఫోన్‌లోనే ఉంటుంది."),
        "ev_add" to tr("Add evidence file", "सबूत फ़ाइल जोड़ें", "సాక్ష్య ఫైల్ జోడించు"),
        "ev_ai_refine" to tr("AI refinement of OCR text", "OCR टेक्स्ट का AI परिष्करण", "OCR టెక్స్ట్ యొక్క AI పరిష్కరణ"),
        "ev_analyze" to tr("Analyze evidence with AI", "AI से सबूत जाँचें", "AI తో సాక్ష్యాన్ని విశ్లేషించండి"),
        "ev_facts" to tr("Extracted facts (%1\$d)", "निकाले गए तथ्य (%1\$d)", "వెలికితీసిన వాస్తవాలు (%1\$d)"),
        "ev_facts_empty" to tr("No facts yet. Every fact will be linked back to its source file.", "अभी कोई तथ्य नहीं। हर तथ्य अपनी मूल फ़ाइल से जुड़ेगा।", "ఇంకా వాస్తవాలు లేవు. ప్రతి వాస్తవం దాని మూల ఫైల్‌కు అనుసంధానమవుతుంది."),
        "ev_notrail" to tr("No trail facts yet — analyze your evidence first.", "अभी कोई ट्रेल तथ्य नहीं — पहले सबूत जाँचें।", "ఇంకా మార్గ వాస్తवాలు లేవు — ముందుగా సాక్ష్యాన్ని విశ్లేషించండి."),
        "ev_money" to tr("Money trail (from your facts)", "पैसों का चलन (आपके तथ्यों से)", "డబ్బు మార్గం (మీ వాస్తవాల నుండి)"),
        "ev_pack" to tr("Generate complaint pack (PDF)", "शिकायत पैक बनाएँ (PDF)", "ఫిర్యాదు ప్యాక్ సృష్టించండి (PDF)"),
        "ev_shared" to tr("Saved + ready to share: %1\$s", "सहेजा गया + साझा करने के लिए तैयार: %1\$s", "సేవ్ చేయబడింది + షేర్‌కి సిద్ధం: %1\$s"),
        "ev_to_guide" to tr("Go to Portal Guide →", "पोर्टल गाइड पर जाएँ →", "పోర్టల్ గైడ్‌కి వెళ్లండి →"),
        "ev_share_chooser" to tr("Share complaint pack", "शिकायत पैक साझा करें", "ఫిర్యాదు ప్యాక్ షేర్ చేయండి"),
        "ev_no_evidence" to tr("No evidence yet.", "अभी कोई सबूत नहीं।", "ఇంకా సాక్ష్యం లేదు."),

        // Requests
        "rq_title" to tr("Bank and Cyber Cell Requests", "बैंक और साइबर सेल अनुरोध", "బ్యాంక్ మరియు సైబర్ సెల్ విజ్ఞప్తులు"),
        "rq_sub" to tr("Ready drafts built from your case data:", "आपके केस डेटा से बने तैयार ड्राफ़्ट:", "మీ కేసు డేటా నుండి తయారైన డ్రాఫ్ట్‌లు:"),
        "rq_freeze" to tr("Freeze request to bank", "बैंक को फ्रीज़ अनुरोध", "బ్యాంక్‌కి ఫ్రీజ్ విజ్ఞప్తి"),
        "rq_unfreeze" to tr("Unfreeze request", "अनफ्रीज़ अनुरोध", "అన్‌ఫ్రీజ్ విజ్ఞప్తి"),
        "rq_followup" to tr("Follow-up message", "फॉलो-अप संदेश", "ఫాలో-అప్ సందేశం"),
        "rq_docs" to tr("Documents the cyber cell may ask", "साइबर सेल ये दस्तावेज़ माँग सकता है", "సైబర్ సెల్ అడగవచ్చు డాక్యుమెంట్లు"),
        "rq_ready" to tr("Ready draft", "तैयार ड्राफ़्ट", "సిద్ధ డ్రాఫ్ట్"),
        "rq_send" to tr("Send / Copy", "भेजें / कॉपी", "పంపండి / కాపీ"),
        "rq_share" to tr("Send draft", "ड्राफ़्ट भेजें", "డ్రాఫ్ట్ పంపండి"),

        // PhoneTrace
        "pt_title" to tr("PhoneTrace", "फ़ोनट्रेस", "ఫోన్‌ట్రేస్"),
        "pt_sub" to tr("How did the attacker likely get in? Reads normal Android APIs only. “Suspicious”, never “proven”.", "हमलावर संभवतः कैसे घुसा? केवल सामान्य Android APIs पढ़ता है। “संदिग्ध”, कभी “सिद्ध” नहीं।", "దాడి చేసినవారు ఎలా ప్రవేశించి ఉండవచ్చు? సాధారణ Android APIs మాత్రమే చదువుతుంది. “అనుమానాస్పదం”, ఎప్పుడూ “రుజువు” కాదు."),
        "pt_p1" to tr("yesterday", "कल", "నిన్న"),
        "pt_p2" to tr("2 days ago", "2 दिन पहले", "2 రోజుల క్రితం"),
        "pt_p3" to tr("5 days ago", "5 दिन पहले", "5 రోజుల క్రితం"),
        "pt_p4" to tr("10 days ago", "10 दिन पहले", "10 రోజుల క్రితం"),
        "pt_p5" to tr("unknown (14 days)", "अज्ञात (14 दिन)", "తెలియదు (14 రోజులు)"),
        "pt_scan" to tr("Scan apps", "ऐप्स जाँचें", "యాప్‌లు స్కాన్ చేయండి"),
        "pt_granted" to tr("Usage access: granted", "उपयोग पहुँच: प्रदान की गई", "వినియోగ ప్రవేశం: మంజూరు"),
        "pt_grant" to tr("Grant usage access", "उपयोग पहुँच दें", "వినియోగ ప్రవేశం మంజూరు చేయండి"),
        "pt_none" to tr("No suspicious apps flagged yet.", "अभी कोई संदिग्ध ऐप चिह्नित नहीं।", "ఇంకా అనుమానాస్పద యాప్‌లు గుర్తించలేదు."),
        "pt_susp" to tr("%1\$s — suspicion %2\$d/100", "%1\$s — संदेह %2\$d/100", "%1\$s — అనుమానం %2\$d/100"),
        "pt_opened" to tr("Opened around the fraud time:", "धोखे के समय के आसपास खोले गए:", "మోసం జరిగిన సమయంలో తెరవబడినవి:"),
        "pt_opened_n" to tr("• %1\$s (opened %2\$dx)", "• %1\$s (%2\$d बार खोला)", "• %1\$s (%2\$d సార్లు తెరిచారు)"),
        "pt_baseline" to tr("Save weekly snapshot", "साप्ताहिक स्नैपशॉट सहेजें", "వారపు స్నాప్‌షాట్ సేవ్ చేయండి"),
        "pt_compare" to tr("Compare now", "अभी तुलना करें", "ఇప్పుడే పోల్చండి"),
        "pt_keep" to tr("Save evidence before deleting any app.", "कोई भी ऐप हटाने से पहले सबूत सहेजें।", "ఏదైనా యాప్ తొలగించే ముందు సాక్ష్యాన్ని సేవ్ చేయండి."),

        // Exposure
        "exp_title" to tr("10-Day Exposure Warning", "10-दिन एक्सपोज़र चेतावनी", "10-రోజుల ఎక్స్‌పోజర్ హెచ్చరిక"),
        "exp_sub" to tr("First verify it is your email — a code is sent so nobody can scan someone else's email.", "पहले यह आपका ईमेल जाँचें — कोड भेजा जाता है ताकि कोई दूसरे का ईमेल न जाँच सके।", "ముందుగా ఇది మీ ఇమెయిల్ అని ధృవీకరించండి — ఎవరూ ఇతరుల ఇమెయిల్‌ను స్కాన్ చేయకూడదు కాబట్టి కోడ్ పంపుతారు."),
        "exp_pending" to tr("Email-breach scan pending: backend not linked.", "ईमेल-डेटा लीक जाँच लंबित: बैकएंड जुड़ा नहीं है।", "ఇమెయిల్-లీక్ స్కాన్ పెండింగ్: బ్యాకెండ్ అనుసంధానం కాలేదు."),
        "exp_pending_sub" to tr("The password check below already works fully on-device.", "नीचे की पासवर्ड जाँच पूरी तरह डिवाइस पर चलती है।", "క్రింది పాస్‌వర్డ్ తనిఖీ పూర్తిగా డివైస్‌లోనే పనిచేస్తుంది."),
        "exp_email" to tr("Email", "ईमेल", "ఇమెయిల్"),
        "exp_send" to tr("Send code", "कोड भेजें", "కోడ్ పంపండి"),
        "exp_sending" to tr("sending code...", "कोड भेजा जा रहा है...", "కోడ్ పంపబడుతోంది..."),
        "exp_code" to tr("6-digit code", "6-अंकीय कोड", "6-అంకెల కోడ్"),
        "exp_verify" to tr("Verify", "जाँचें", "ధృవీకరించండి"),
        "exp_verified" to tr("Email verified", "ईमेल सत्यापित", "ఇమెయిల్ ధృవీకరించబడింది"),
        "exp_scanning" to tr("scanning...", "जाँच हो रही है...", "స్కాన్ జరుగుతోంది..."),
        "exp_scan" to tr("Scan breaches", "डेटा लीक जाँचें", "లీక్‌లను స్కాన్ చేయండి"),
        "exp_found" to tr("Your email appears in places that raise your risk:", "आपका ईमेल खतरा बढ़ाने वाली जगहों पर दिखता है:", "మీ ఇమెయిల్ ప్రమాదాన్ని పెంచే చోట్ల కనిపిస్తుంది:"),
        "exp_fix" to tr("First: secure this email (new password, 2FA/passkey). Then change reused passwords. Then delete unused accounts.", "पहले: इस ईमेल को सुरक्षित करें (नया पासवर्ड, 2FA/passkey)। फिर दोबारा इस्तेमाल हुए पासवर्ड बदलें। फिर अप्रयुक्त खाते हटाएँ।", "ముందు: ఈ ఇమెయిల్‌ను సురక్షితం చేయండి (కొత్త పాస్‌వర్డ్, 2FA/passkey). ఆపై మళ్లీ వాడిన పాస్‌వర్డ్‌లను మార్చండి. ఆపై వాడకంలో లేని ఖాతాలను తొలగించండి."),
        "exp_notfound" to tr("No public breaches returned yet. Re-check in 10 days.", "अभी कोई सार्वजनिक डेटा लीक नहीं मिला। 10 दिन में दोबारा जाँचें।", "ఇంకా బహిరంగ లీక్‌లు రాలేదు. 10 రోజుల్లో మళ్లీ తనిఖీ చేయండి."),
        "exp_pw_title" to tr("Password exposure check (fully on-device, privacy-safe):", "पासवर्ड जोखिम जाँच (पूरी तरह डिवाइस पर, गोपनीयता-सुरक्षित):", "పాస్‌వర్డ్ ప్రమాద తనిఖీ (పూర్తిగా డివైస్‌లో, గోప్యత-సురక్షితం):"),
        "exp_pw_label" to tr("Check a password (never stored, never sent)", "पासवर्ड जाँचें (कभी सहेजा नहीं जाता, भेजा नहीं जाता)", "పాస్‌వర్డ్ తనిఖీ (ఎప్పుడూ సేవ్ కాదు, పంపబడదు)"),
        "exp_pw_check" to tr("Check exposure (only 5 hash chars leave the phone)", "जोखिम जाँचें (केवल 5 हैश अक्षर बाहर जाते हैं)", "ప్రమాద తనిఖీ (ఫోన్ నుండి 5 హ్యాష్ అక్షరాలు మాత్రమే వెళ్తాయి)"),
        "exp_hit" to tr("This password appears in %1\$d known exposures — change it everywhere it is reused.", "यह पासवर्ड %1\$d ज्ञात डेटा लीक में है — जहाँ भी दोबारा इस्तेमाल है वहाँ बदलें।", "ఈ పాస్‌వర్డ్ %1\$d తెలిసిన లీక్‌లలో కనిపిస్తుంది — మళ్లీ వాడిన చోటన్నింటిలో మార్చండి."),
        "exp_safe" to tr("Not found in public exposure data. Still keep it unique per site.", "सार्वजनिक डेटा लीक में नहीं मिला। फिर भी हर साइट के लिए अलग रखें।", "బహిరంగ లీక్ డేటాలో కనిపించలేదు. అయినా ప్రతి సైట్‌కి వేరుగా ఉంచండి."),
        "exp_fail" to tr("Check failed: %1\$s", "जाँच विफल: %1\$s", "తనిఖీ విఫలమైంది: %1\$s"),

        // Settings
        "set_title" to tr("Settings and Privacy", "सेटिंग्स और गोपनीयता", "సెట్టింగ్‌లు మరియు గోప్యత"),
        "set_privacy" to tr("• Permissions, one at a time, with plain explanation\n• All case data stays on this phone\n• No silent background collection", "• अनुमतियाँ, एक बार में एक, सरल समझ के साथ\n• सारा केस डेटा इसी फ़ोन पर रहता है\n• कोई छिपा बैकग्राउंड संग्रह नहीं", "• అనుమతులు ఒక్కొక్కటి, సరళ వివరణతో\n• మొత్తం కేసు డేటా ఈ ఫోన్‌లోనే ఉంటుంది\n• రహస్య బ్యాక్‌గ్రౌండ్ సేకరణ లేదు"),

        // Portal Guide
        "pg_title" to tr("Portal Guide", "पोर्टल गाइड", "పోర్టల్ గైడ్"),
        "pg_sub" to tr("Knowledge from cybercrime.gov.in • last verified: %1\$s", "cybercrime.gov.in का ज्ञान • अंतिम सत्यापन: %1\$s", "cybercrime.gov.in నుండి జ్ఞానం • చివరిగా ధృవీకరించింది: %1\$s"),
        "pg_lock" to tr("Preview + Submit are always YOUR taps — the app cannot press them.", "Preview + Submit हमेशा आपके टैप हैं — ऐप उन्हें दबा नहीं सकता।", "Preview + Submit ఎప్పుడూ మీ ట్యాప్‌లే — యాప్ వాటిని నొక్కలేదు."),
        "pg_ev" to tr("Evidence uploads: ≤5 MB each • never delete the originals — police need them too.", "सबूत अपलोड: प्रत्येक ≤5 MB • मूल फ़ाइलें कभी न हटाएँ — पुलिस को भी चाहिए।", "సాక్ష్య అప్‌లోడ్‌లు: ఒక్కొక్కటి ≤5 MB • అసలు ఫైళ్లను ఎప్పుడూ తొలగించవద్దు — పోలీసులకు కూడా అవసరం."),
        "pg_start" to tr("Start AI filling", "AI भरना शुरू करें", "AI పూరింపు ప్రారంభించు"),
        "pg_hatch" to tr("My screen looks different", "मेरी स्क्रीन अलग दिख रही है", "నా స్క్రీన్ వేరుగా కనిపిస్తోంది"),
        "pg_hatch_body" to tr("Two honest possibilities: (1) the live portal changed — every flow still follows the same order: File a complaint → Report Other Cyber Crime → Category → Sub-category → Incident details → Complainant → Suspect → Save & Preview → Preview → Submit; follow the numbered steps even if the boxes moved, and note what looks different in your tracker. (2) You opened a different portal page — return to https://cybercrime.gov.in and start again from Step 1. If money was stolen, call 1930 within the golden hour while filing. Manual steps were verified on 2026-10-22 against the official MHA citizen manual (v10).", "दो सच्ची संभावनाएँ: (1) लाइव पोर्टल बदल गया है — हर फ़्लो का क्रम वही रहेगा: File a complaint → Report Other Cyber Crime → Category → Sub-category → Incident details → Complainant → Suspect → Save & Preview → Preview → Submit; बॉक्स अलग दिखें तो भी क्रमांकित चरणों का पालन करें, और ट्रैकर में अंतर नोट करें। (2) आपने कोई दूसरा पोर्टल पेज खोला है — https://cybercrime.gov.in पर वापस जाएँ और चरण 1 से दोबारा शुरू करें। पैसे चोरी हुए हों तो फ़ाइल करते समय गोल्डन आवर में 1930 पर कॉल करें। मैनुअल चरण 2026-10-22 को आधिकारिक MHA सिटिज़न मैनुअल (v10) के विरुद्ध सत्यापित हैं।", "రెండు నిజమైన సాధ్యతలు: (1) లైవ్ పోర్టల్ మారింది — ప్రతి ఫ్లో ఒకే క్రమం పాటిస్తుంది: File a complaint → Report Other Cyber Crime → Category → Sub-category → Incident details → Complainant → Suspect → Save & Preview → Preview → Submit; బాక్సులు తరలినా నంబర్ దశలను అనుసరించండి, తేడాలను ట్రాకర్‌లో నమోదు చేయండి. (2) మీరు మరో పోర్టల్ పేజీ తెరిచారు — https://cybercrime.gov.in కి తిరిగి వెళ్లి దశ 1 నుండి మళ్లీ ప్రారంభించండి. డబ్బు దొంగిలితే, ఫైల్ చేస్తూనే గోల్డెన్ ఆవర్‌లో 1930 కి కాల్ చేయండి. మాన్యువల్ దశలు 2026-10-22 న అధికారిక MHA సిటిజన్ మాన్యువల్ (v10) తో ధృవీకరించబడ్డాయి."),
        "pg_empty" to tr("Tell the AI Case Guide what happened first — then come back here to run the plan.", "पहले AI केस गाइड को बताइए क्या हुआ — फिर योजना चलाने यहाँ वापस आएँ।", "ముందు ఏమి జరిగిందో AI కేసు గైడ్‌కి చెప్పండి — తర్వాత ప్లాన్ నడపడానికి ఇక్కడికి తిరిగి రండి."),
        "pg_owner_ai" to tr("AI", "AI", "AI"),
        "pg_owner_you" to tr("YOU (credentials/captcha/OTP)", "आप (क्रेडेंशियल/कैप्चा/OTP)", "మీరు (క్రెడెన్షియల్స్/క్యాప్చా/OTP)"),
        "pg_owner_tap" to tr("YOUR TAP (locked)", "आपका टैप (लॉक)", "మీ ట్యాప్ (లాక్)"),
    )
}
