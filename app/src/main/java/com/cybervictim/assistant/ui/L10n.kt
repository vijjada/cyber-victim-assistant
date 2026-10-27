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
        PA("pa", "ਪੰਜਾਬੀ"),
        GU("gu", "ગુજરાતી"),
        MR("mr", "मराठी"),
        TA("ta", "தமிழ்"),
        BN("bn", "বাংলা"),
        KN("kn", "ಕನ್ನಡ"),
        ML("ml", "മലയാളം"),
        OR("or", "ଓଡ଼ିଆ"),
        AS("as", "অসমীয়া");

        fun toBcp47(): String = when (this) {
            EN -> "en-IN"
            HI -> "hi-IN"
            TE -> "te-IN"
            PA -> "pa-IN"
            GU -> "gu-IN"
            MR -> "mr-IN"
            TA -> "ta-IN"
            BN -> "bn-IN"
            KN -> "kn-IN"
            ML -> "ml-IN"
            OR -> "or-IN"
            AS -> "as-IN"
        }

        fun toLocale(): java.util.Locale = java.util.Locale.forLanguageTag(toBcp47())
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
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(KEY_LANG, to.code).commit()
        tick.value += 1
    }

    fun t(key: String): String {
        tick.value // dependency read for recomposition
        val m = i18n[key] ?: return key
        return m[lang] ?: m[Lang.EN] ?: key
    }

    private fun tr(
        en: String,
        hi: String,
        te: String,
        pa: String? = null,
        gu: String? = null,
        mr: String? = null,
        ta: String? = null,
        bn: String? = null,
        kn: String? = null,
        ml: String? = null,
        or: String? = null,
        asLang: String? = null
    ): Map<Lang, String> = buildMap {
        put(Lang.EN, en)
        put(Lang.HI, hi)
        put(Lang.TE, te)
        pa?.let { put(Lang.PA, it) }
        gu?.let { put(Lang.GU, it) }
        mr?.let { put(Lang.MR, it) }
        ta?.let { put(Lang.TA, it) }
        bn?.let { put(Lang.BN, it) }
        kn?.let { put(Lang.KN, it) }
        ml?.let { put(Lang.ML, it) }
        or?.let { put(Lang.OR, it) }
        asLang?.let { put(Lang.AS, it) }
    }

    private val i18n = mapOf(
        // Bottom bar tabs
        "tab_home" to tr("Home", "होम", "హోమ్", pa = "ਹੋਮ", gu = "હોમ", mr = "होम", ta = "முகப்பு", bn = "হোম"),
        "tab_guide" to tr("Guide", "गाइड", "గైడ్", pa = "ਗਾਈਡ", gu = "માર્ગદર્શિકા", mr = "मार्गदर्शक", ta = "வழிகாட்டி", bn = "গাইড"),
        "tab_evidence" to tr("Evidence", "साक्ष्य", "సాక్ష్యం", pa = "ਸਬੂਤ", gu = "પુરાવા", mr = "पुरावा", ta = "சான்று", bn = "প্রমাণ"),
        "tab_requests" to tr("Requests", "निवेदन", "విజ్ఞప్తులు", pa = "ਬੇਨਤੀਆਂ", gu = "વિનંતીઓ", mr = "विनंत्या", ta = "கோரிக்கைகள்", bn = "অনুরোধ"),
        "tab_more" to tr("More", "और", "ఇంకా", pa = "ਹੋਰ", gu = "વધુ", mr = "अधिक", ta = "மேலும்", bn = "আরও"),

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
        "pg_start" to tr("Speak out", "बोलें", "మాట్లాడండి", pa = "ਬੋਲੋ", gu = "બોલો", mr = "बोला", ta = "பேசுங்கள்", bn = "বলুন", kn = "ಮಾತನಾಡಿ", ml = "സംസാരിക്കൂ", or = "କୁହନ୍ତୁ", asLang = "কওক"),
        "pg_hatch" to tr("My screen looks different", "मेरी स्क्रीन अलग दिख रही है", "నా స్క్రీన్ వేరుగా కనిపిస్తోంది"),
        "pg_hatch_body" to tr("Two honest possibilities: (1) the live portal changed — every flow still follows the same order: File a complaint → Report Other Cyber Crime → Category → Sub-category → Incident details → Complainant → Suspect → Save & Preview → Preview → Submit; follow the numbered steps even if the boxes moved, and note what looks different in your tracker. (2) You opened a different portal page — return to https://cybercrime.gov.in and start again from Step 1. If money was stolen, call 1930 within the golden hour while filing. Manual steps were verified on 2026-10-22 against the official MHA citizen manual (v10).", "दो सच्ची संभावनाएँ: (1) लाइव पोर्टल बदल गया है — हर फ़्लो का क्रम वही रहेगा: File a complaint → Report Other Cyber Crime → Category → Sub-category → Incident details → Complainant → Suspect → Save & Preview → Preview → Submit; बॉक्स अलग दिखें तो भी क्रमांकित चरणों का पालन करें, और ट्रैकर में अंतर नोट करें। (2) आपने कोई दूसरा पोर्टल पेज खोला है — https://cybercrime.gov.in पर वापस जाएँ और चरण 1 से दोबारा शुरू करें। पैसे चोरी हुए हों तो फ़ाइल करते समय गोल्डन आवर में 1930 पर कॉल करें। मैनुअल चरण 2026-10-22 को आधिकारिक MHA सिटिज़न मैनुअल (v10) के विरुद्ध सत्यापित हैं।", "రెండు నిజమైన సాధ్యతలు: (1) లైవ్ పోర్టల్ మారింది — ప్రతి ఫ్లో ఒకే క్రమం పాటిస్తుంది: File a complaint → Report Other Cyber Crime → Category → Sub-category → Incident details → Complainant → Suspect → Save & Preview → Preview → Submit; బాక్సులు తరలినా నంబర్ దశలను అనుసరించండి, తేడాలను ట్రాకర్‌లో నమోదు చేయండి. (2) మీరు మరో పోర్టల్ పేజీ తెరిచారు — https://cybercrime.gov.in కి తిరిగి వెళ్లి దశ 1 నుండి మళ్లీ ప్రారంభించండి. డబ్బు దొంగిలితే, ఫైల్ చేస్తూనే గోల్డెన్ ఆవర్‌లో 1930 కి కాల్ చేయండి. మాన్యువల్ దశలు 2026-10-22 న అధికారిక MHA సిటిజన్ మాన్యువల్ (v10) తో ధృవీకరించబడ్డాయి."),
        "pg_empty" to tr("Tell the AI Case Guide what happened first — then come back here to run the plan.", "पहले AI केस गाइड को बताइए क्या हुआ — फिर योजना चलाने यहाँ वापस आएँ।", "ముందు ఏమి జరిగిందో AI కేసు గైడ్‌కి చెప్పండి — తర్వాత ప్లాన్ నడపడానికి ఇక్కడికి తిరిగి రండి."),
        "pg_owner_ai" to tr("AI", "AI", "AI"),
        "pg_owner_you" to tr("YOU (credentials/captcha/OTP)", "आप (क्रेडेंशियल/कैप्चा/OTP)", "మీరు (క్రెడెన్షియల్స్/క్యాప్చా/OTP)"),
        "pg_owner_tap" to tr("YOUR TAP", "आपका टैप", "మీ ట్యాప్"),

        // Voice Guide Core Keys (all 12 languages)
        "voice_guide_btn" to tr("Voice Guide", "वॉइस गाइड", "వాయిస్ గైడ్", pa = "ਵੌਇਸ ਗਾਈਡ", gu = "વૉઇસ ગાઇડ", mr = "व्हॉइस मार्गदर्शक", ta = "குரல் வழிகாட்டி", bn = "ভয়েস গাইড", kn = "ಧ್ವನಿ ಮಾರ್ಗದರ್ಶಿ", ml = "വോയ്സ് ഗൈഡ്", or = "ଭଏସ୍ ଗାଇଡ୍", asLang = "ভইচ গাইড"),
        "voice_guide_title" to tr("CyBridge Voice Guide", "साइब्रिज वॉइस गाइड", "సైబ్రిడ్జ్ వాయిస్ గైడ్", pa = "ਸਾਈਬ੍ਰਿਜ ਵੌਇਸ ਗਾਈਡ", gu = "સાયબ્રિજ વૉઇસ ગાઇડ", mr = "सायब्रिज व्हॉइस मार्गदर्शक", ta = "சைபிரிட்ஜ் குரல் வழிகாட்டி", bn = "সাইব্রিজ ভয়েস গাইড", kn = "ಸೈಬ್ರಿಡ್ಜ್ ಧ್ವನಿ ಮಾರ್ಗದರ್ಶಿ", ml = "സൈബ്രിഡ്ജ് വോയ്സ് ഗൈഡ്", or = "ସାଇବ୍ରିଜ୍ ଭଏସ୍ ଗାଇଡ୍", asLang = "চাইব্ৰিজ ভইচ গাইড"),
        "voice_guide_sub" to tr(
            "Speak in your language. CyBridge will guide your next tap.",
            "अपनी भाषा में बोलें। साइब्रिज आपके अगले टैप का मार्गदर्शन करेगा।",
            "మీ భాషలో మాట్లాడండి. సైబ్రిడ్జ్ మీ తర్వాతి ట్యాప్‌ను మార్గదర్శనం చేస్తుంది.",
            pa = "ਆਪਣੀ ਭਾਸ਼ਾ ਵਿੱਚ ਬੋਲੋ। ਸਾਈਬ੍ਰਿਜ ਤੁਹਾਡੇ ਅਗਲੇ ਟੈਪ ਦੀ ਅਗਵਾਈ ਕਰੇਗਾ।",
            gu = "તમારી ભાષામાં બોલો. સાયબ્રિજ તમારા આગલા ટૅપનું માર્ગદર્શન કરશે.",
            mr = "आपल्या भाषेत बोला. सायब्रिज आपल्या पुढील टॅपचे मार्गदर्शन करेल.",
            ta = "உங்கள் மொழியில் பேசுங்கள். சைபிரிட்ஜ் உங்கள் அடுத்த தட்டலை வழிநடத்தும்.",
            bn = "আপনার ভাষায় কথা বলুন। সাইব্রিজ আপনার পরবর্তী ট্যাপ নির্দেশ করবে।",
            kn = "ನಿಮ್ಮ ಭಾಷೆಯಲ್ಲಿ ಮಾತನಾಡಿ. ಸೈಬ್ರಿಡ್ಜ್ ನಿಮ್ಮ ಮುಂದಿನ ಟ್ಯಾಪ್ ಮಾರ್ಗದರ್ಶನ ನೀಡುತ್ತದೆ.",
            ml = "നിങ്ങളുടെ ഭാഷയിൽ സംസാരിക്കുക. സൈബ്രിഡ്ജ് നിങ്ങളുടെ അടുത്ത ടാപ്പ് നയിക്കും.",
            or = "ଆପଣଙ୍କ ଭାଷାରେ କୁହନ୍ତୁ। ସାଇବ୍ରିଜ୍ ଆପଣଙ୍କ ପରବର୍ତ୍ତୀ ଟ୍ୟାପ୍ ମାର୍ଗଦର୍ଶନ କରିବ।",
            asLang = "আপোনাৰ ভাষাত কওক। চাইব্ৰিজে আপোনাৰ পৰৱৰ্তী টেপৰ নিৰ্দেশনা দিব।"
        ),
        "voice_listening" to tr("Listening...", "सुन रहे हैं...", "వింటున్నాము...", pa = "ਸੁਣ ਰਿਹਾ ਹੈ...", gu = "સાંભળી રહ્યા છીએ...", mr = "ऐकत आहे...", ta = "கேட்கிறது...", bn = "শুনছি...", kn = "ಕೇಳಿಸಿಕೊಳ್ಳುತ್ತಿದ್ದೇವೆ...", ml = "കേൾക്കുന്നു...", or = "ଶୁଣୁଛୁ...", asLang = "শুনি আছোঁ..."),
        "voice_tap_to_speak" to tr("Tap microphone to speak", "बोलने के लिए माइक दबाएँ", "మాట్లాడటానికి మైక్ నొక్కండి", pa = "ਬੋਲਣ ਲਈ ਮਾਈਕ ਦਬਾਓ", gu = "બોલવા માટે માઇક દબાવો", mr = "बोलण्यासाठी माइक दाबा", ta = "பேச மைக்கை அழுத்தவும்", bn = "বলতে মাইক চাপুন", kn = "ಮಾತನಾಡಲು ಮೈಕ್ ಒತ್ತಿ", ml = "സംസാരിക്കാൻ മൈക്ക് അമർത്തുക", or = "କହିବା ପାଇଁ ମାଇକ୍ ଦବାନ୍ତୁ", asLang = "কবলৈ মাইক টিপক"),
        "voice_speak_now" to tr(
            "Speak now in your chosen language...",
            "अब अपनी चुनी हुई भाषा में बोलें...",
            "ఇప్పుడు మీ ఎంచుకున్న భాషలో మాట్లాడండి...",
            pa = "ਹੁਣ ਆਪਣੀ ਚੁਣੀ ਹੋਈ ਭਾਸ਼ਾ ਵਿੱਚ ਬੋਲੋ...",
            gu = "હવે તમારી પસંદ કરેલી ભાષામાં બોલો...",
            mr = "आता आपल्या निवडलेल्या भाषेत बोला...",
            ta = "இப்போது உங்கள் தேர்வு செய்த மொழியில் பேசுங்கள்...",
            bn = "এখন আপনার নির্বাচিত ভাষায় বলুন...",
            kn = "ಈಗ ನೀವು ಆಯ್ಕೆ ಮಾಡಿದ ಭಾಷೆಯಲ್ಲಿ ಮಾತನಾಡಿ...",
            ml = "ഇപ്പോൾ നിങ്ങൾ തിരഞ്ഞെടുത്ത ഭാഷയിൽ സംസാരിക്കുക...",
            or = "ବର୍ତ୍ତମାନ ଆପଣଙ୍କ ମନୋନୀତ ଭାଷାରେ କୁହନ୍ତୁ...",
            asLang = "এতিয়া আপোনাৰ নিৰ্বাচিত ভাষাত কওক..."
        ),
        "voice_thinking" to tr("Finding your next step...", "अगला चरण खोज रहे हैं...", "తర్వాతి దశను వెతుకుతున్నాము...", pa = "ਅਗਲਾ ਕਦਮ ਲੱਭ ਰਿਹਾ ਹੈ...", gu = "આગળનું પગલું શોધી રહ્યા છીએ...", mr = "पुढील पाऊल शोधत आहे...", ta = "அடுத்த படியைக் கண்டறிகிறது...", bn = "পরবর্তী পদক্ষেপ খুঁজছি...", kn = "ಮುಂದಿನ ಹಂತವನ್ನು ಹುಡುಕಲಾಗುತ್ತಿದೆ...", ml = "അടുത്ത ഘട്ടം കണ്ടെത്തുന്നു...", or = "ପରବର୍ତ୍ତୀ ପଦକ୍ଷେପ ଖୋଜୁଛୁ...", asLang = "পৰৱৰ্তী পদক্ষেপ বিচাৰি আছোঁ..."),
        "voice_speaking" to tr("Speaking next step...", "अगला चरण बोल रहे हैं...", "తర్వాతి దశను చెబుతున్నాము...", pa = "ਅਗਲਾ ਕਦਮ ਬੋਲ ਰਿਹਾ ਹੈ...", gu = "આગળનું પગલું બોલી રહ્યા છીએ...", mr = "पुढील पाऊल सांगत आहे...", ta = "அடுத்த படியைக் கூறுகிறது...", bn = "পরবর্তী পদক্ষেপ বলছি...", kn = "ಮುಂದಿನ ಹಂತವನ್ನು ಹೇಳಲಾಗುತ್ತಿದೆ...", ml = "അടുത്ത ഘട്ടം പറയുന്നു...", or = "ପରବର୍ତ୍ତୀ ପଦକ୍ଷେପ କହୁଛୁ...", asLang = "পৰৱৰ্তী পদক্ষেপ কৈ আছোঁ..."),
        "voice_stop_speaking" to tr("Stop voice", "आवाज़ रोकें", "వాయిస్ ఆపండి", pa = "ਆਵਾਜ਼ ਰੋਕੋ", gu = "અવાજ રોકો", mr = "आवाज थांबवा", ta = "குரலை நிறுத்து", bn = "কণ্ঠ থামান", kn = "ಧ್ವನಿಯನ್ನು ನಿಲ್ಲಿಸಿ", ml = "ശബ്ദം നിർത്തുക", or = "ସ୍ୱର ବନ୍ଦ କରନ୍ତୁ", asLang = "ভইচ বন্ধ কৰক"),
        "voice_type_instead" to tr("Type your question here", "अपना प्रश्न यहाँ लिखें", "మీ ప్రశ్నను ఇక్కడ రాయండి", pa = "ਆਪਣਾ ਸਵਾਲ ਇੱਥੇ ਲਿਖੋ", gu = "તમારો પ્રશ્ન અહીં લખો", mr = "आपला प्रश्न येथे लिहा", ta = "உங்கள் கேள்வியை இங்கே தட்டச்சு செய்க", bn = "আপনার প্রশ্ন এখানে লিখুন", kn = "ನಿಮ್ಮ ಪ್ರಶ್ನೆಯನ್ನು ಇಲ್ಲಿ ಬರೆಯಿರಿ", ml = "നിങ്ങളുടെ ചോദ്യം ഇവിടെ ടൈപ്പ് ചെയ്യുക", or = "ଆପଣଙ୍କ ପ୍ରଶ୍ନ ଏଠାରେ ଲେଖନ୍ତୁ", asLang = "আপোনাৰ প্ৰশ্ন ইয়াত লিখক"),
        "voice_send" to tr("Send", "भेजें", "పంపండి", pa = "ਭੇਜੋ", gu = "મોકલો", mr = "पाठवा", ta = "அனுப்பு", bn = "পাঠান", kn = "ಕಳುಹಿಸಿ", ml = "അയക്കുക", or = "ପଠାନ୍ତୁ", asLang = "পঠিয়াওক"),
        "voice_system_dialog" to tr("System Voice Input", "सिस्टम वॉइस इनपुट", "సిస్టమ్ వాయిస్ ఇన్‌పుట్", pa = "ਸਿਸਟਮ ਵੌਇਸ ਇਨਪੁਟ", gu = "સિસ્ટમ વૉઇસ ઇનપુટ", mr = "सिस्टम व्हॉइस इनपुट", ta = "சிஸ்டம் குரல் உள்ளீடு", bn = "সিস্টেম ভয়েস ইনপুট", kn = "ಸಿಸ್ಟಮ್ ಧ್ವನಿ ಇನ್‌ಪುಟ್", ml = "സിസ്റ്റം വോയ്സ് ഇൻപുട്ട്", or = "ସିଷ୍ଟମ୍ ଭଏସ୍ ଇନପୁଟ୍", asLang = "ছিষ্টেম ভইচ ইনপুট"),
        "voice_retry" to tr("Retry", "पुनः प्रयास करें", "మళ్లీ ప్రయత్నించండి", pa = "ਦੁਬਾਰਾ ਕੋਸ਼ਿਸ਼ ਕਰੋ", gu = "ફરી પ્રયાસ કરો", mr = "पुन्हा प्रयत्न करा", ta = "மீண்டும் முயற்சிக்கவும்", bn = "আবার চেষ্টা করুন", kn = "ಮತ್ತೆ ಪ್ರಯತ್ನಿಸಿ", ml = "വീണ്ടും ശ്രമിക്കുക", or = "ପୁନର୍ବାର ଚେଷ୍ଟା କରନ୍ତୁ", asLang = "পুনৰ চেষ্টা কৰক"),
        "voice_error_unavailable" to tr(
            "Speech recognition is not available on this device. You can type below instead.",
            "इस डिवाइस पर वाक् पहचान उपलब्ध नहीं है। आप नीचे लिखकर पूछ सकते हैं।",
            "ఈ పరికరంలో వాయిస్ గుర్తింపు అందుబాటులో లేదు. మీరు క్రింద టైప్ చేయవచ్చు.",
            pa = "ਇਸ ਡਿਵਾਈਸ 'ਤੇ ਬੋਲ ਪਛਾਣ ਉਪਲਬਧ ਨਹੀਂ ਹੈ। ਤੁਸੀਂ ਹੇਠਾਂ ਟਾਈਪ ਕਰ ਸਕਦੇ ਹੋ।",
            gu = "આ ઉપકરણ પર અવાજ ઓળખ ઉપલબ્ધ નથી. તમે નીચે ટાઇપ કરી શકો છો.",
            mr = "या डिव्हाइसवर आवाज ओळख उपलब्ध नाही. आपण खाली टाइप करू शकता.",
            ta = "இந்த சாதனத்தில் குரல் அறிதல் கிடைக்கவில்லை. நீங்கள் கீழே தட்டச்சு செய்யலாம்.",
            bn = "এই ডিভাইসে ভয়েস সনাক্তকরণ উপলব্ধ নেই। আপনি নীচে টাইপ করতে পারেন।",
            kn = "ಈ ಸಾಧನದಲ್ಲಿ ಧ್ವನಿ ಗುರುತಿಸುವಿಕೆ ಲಭ್ಯವಿಲ್ಲ. ನೀವು ಕೆಳಗೆ ಟೈಪ್ ಮಾಡಬಹುದು.",
            ml = "ഈ ഉപകരണത്തിൽ സംഭാഷണ തിരിച്ചറിയൽ ലഭ്യമല്ല. നിങ്ങൾക്ക് താഴെ ടൈപ്പ് ചെയ്യാം.",
            or = "ଏହି ଉପକରଣରେ ଭଏସ୍ ଚିହ୍ନଟ ଉପଲବ୍ଧ ନାହିଁ। ଆପଣ ତଳେ ଟାଇପ୍ କରିପାରିବେ।",
            asLang = "এই ডিভাইচত ভইচ স্বীকৃতি উপলব্ধ নাই। আপুনি তলত টাইপ কৰিব পাৰে।"
        ),
        "voice_permission_required" to tr(
            "Microphone permission is required to listen. Tap to grant or type below.",
            "सुनने के लिए माइक्रोफ़ोन अनुमति आवश्यक है। अनुमति देने के लिए टैप करें या नीचे लिखें।",
            "వినడానికి మైక్రోఫోన్ అనుమతి అవసరం. అనుమతి ఇవ్వడానికి నొక్కండి లేదా క్రింద టైప్ చేయండి.",
            pa = "ਸੁਣਨ ਲਈ ਮਾਈਕ੍ਰੋਫੋਨ ਦੀ ਇਜਾਜ਼ਤ ਲੋੜੀਂਦੀ ਹੈ। ਇਜਾਜ਼ਤ ਦੇਣ ਲਈ ਟੈਪ ਕਰੋ ਜਾਂ ਹੇਠਾਂ ਲਿਖੋ।",
            gu = "સાંભળવા માટે માઇક્રોફોનની મંજૂરી જરૂરી છે. પરવાનગી આપવા માટે ટૅપ કરો અથવા નીચે લખો.",
            mr = "ऐकण्यासाठी मायक्रोफोन परवानगी आवश्यक आहे. परवानगी देण्यासाठी टॅप करा किंवा खाली लिहा.",
            ta = "கேட்க மைக்ரோஃபோன் அனுமதி தேவை. வழங்க தட்டவும் அல்லது கீழே தட்டச்சு செய்யவும்.",
            bn = "শোনার জন্য মাইক্রোফোন অনুমতি প্রয়োজন। অনুমতি দিতে ট্যাপ করুন বা নীচে লিখুন।",
            kn = "ಕೇಳಿಸಿಕೊಳ್ಳಲು ಮೈಕ್ರೋಫೋನ್ ಅನುಮತಿ ಅಗತ್ಯವಿದೆ. ಅನುಮತಿ ನೀಡಲು ಟ್ಯಾಪ್ ಮಾಡಿ ಅಥವಾ ಕೆಳಗೆ ಬರೆಯಿರಿ.",
            ml = "കേൾക്കാൻ മൈക്രോഫോൺ അനുമതി ആവശ്യമാണ്. നൽകാൻ ടാപ്പ് ചെയ്യുക അല്ലെങ്കിൽ താഴെ ടൈപ്പ് ചെയ്യുക.",
            or = "ଶୁଣିବା ପାଇଁ ମାଇକ୍ରୋଫୋନ୍ ଅନୁମତି ଆବଶ୍ୟକ। ଅନୁମତି ଦେବାକୁ ଟ୍ୟାପ୍ କରନ୍ତୁ କିମ୍ବା ତଳେ ଲେଖନ୍ତୁ।",
            asLang = "শুনিবলৈ মাইক্ৰ'ফোনৰ অনুমতিৰ প্ৰয়োজন। অনুমতি দিবলৈ টেপ কৰক বা তলত লিখক।"
        ),
        "voice_step_owner_human" to tr(
            "Manual step: You must enter OTP, CAPTCHA, or credentials yourself.",
            "मैनुअल चरण: OTP, कैप्चा या लॉगिन क्रेडेंशियल आपको स्वयं दर्ज करना होगा।",
            "మాన్యువల్ దశ: OTP, క్యాప్చా లేదా లాగిన్ వివరాలను మీరే నమోదు చేయాలి.",
            pa = "ਮੈਨੂਅਲ ਕਦਮ: ਤੁਹਾਨੂੰ OTP, ਕੈਪਚਾ ਜਾਂ ਲੌਗਇਨ ਵੇਰਵੇ ਖੁਦ ਦਰਜ ਕਰਨੇ ਪੈਣਗੇ।",
            gu = "મેન્યુઅલ પગલું: તમારે OTP, કેપ્ચા અથવા લૉગિન વિગતો જાતે દાખલ કરવી પડશે.",
            mr = "मॅन्युअल पाऊल: आपल्याला OTP, कॅप्चा किंवा लॉगिन तपशील स्वतः प्रविष्ट करावे लागतील.",
            ta = "கைமுறை படி: OTP, கேப்ட்சா அல்லது நற்சான்றிதழ்களை நீங்களே உள்ளிட வேண்டும்.",
            bn = "ম্যানুয়াল পদক্ষেপ: OTP, ক্যাপচা বা লগইন তথ্য আপনাকে নিজে লিখতে হবে।",
            kn = "ಹಸ್ತಚಾಲಿತ ಹಂತ: OTP, ಕ್ಯಾಪ್ಚಾ ಅಥವಾ ಲಾಗಿನ್ ವಿವರಗಳನ್ನು ನೀವೇ ನಮೂದಿಸಬೇಕು.",
            ml = "മാനുവൽ ഘട്ടം: OTP, ക്യാപ്‌ച അല്ലെങ്കിൽ ലോഗിൻ വിവരങ്ങൾ നിങ്ങൾ തന്നെ നൽകണം.",
            or = "ମାନୁଆଲ୍ ପଦକ୍ଷେପ: ଆପଣଙ୍କୁ ନିଜେ OTP, କ୍ୟାପଚା କିମ୍ବା ଲଗଇନ୍ ବିବରଣୀ ପ୍ରବେଶ କରିବାକୁ ପଡିବ।",
            asLang = "মেনুৱেল পদক্ষেপ: আপুনি নিজে OTP, কেপচা বা লগইন তথ্য দিব লাগিব।"
        ),
        "voice_step_owner_lock" to tr(
            "Final confirmation lock: Review all details and tap Submit yourself.",
            "अंतिम पुष्टि लॉक: सभी विवरणों की समीक्षा करें और स्वयं सबमिट दबाएँ।",
            "తుది నిర్ధారణ లాక్: అన్ని వివరాలను సమీక్షించి మీరే సమర్పించు నొక్కండి.",
            pa = "ਅੰਤਿਮ ਪੁਸ਼ਟੀ ਲਾਕ: ਸਾਰੇ ਵੇਰਵਿਆਂ ਦੀ ਸਮੀਖਿਆ ਕਰੋ ਅਤੇ ਖੁਦ ਸਬਮਿਟ ਦਬਾਓ।",
            gu = "અંતિમ પુષ્ટિ લૉક: બધી વિગતો તપાસો અને જાતે સબમિટ દબાવો.",
            mr = "अंतिम पुष्टीकरण लॉक: सर्व तपशील तपासा आणि स्वतः सबमिट दाबा.",
            ta = "இறுதி உறுதிப்படுத்தல் பூட்டு: அனைத்து விவரங்களையும் சரிபார்த்து நீங்களே சமர்ப்பிக்கவும்.",
            bn = "চূড়ান্ত নিশ্চিতকরণ লক: সমস্ত বিবরণ পর্যালোচনা করুন এবং নিজে জমা দিন চাপুন।",
            kn = "ಅಂತಿಮ ದೃಢೀಕರಣ ಲಾಕ್: ಎಲ್ಲಾ ವಿವರಗಳನ್ನು ಪರಿಶೀಲಿಸಿ ಮತ್ತು ನೀವೇ ಸಲ್ಲಿಸಿ ಒತ್ತಿರಿ.",
            ml = "അന്തിമ സ്ഥിരീകരണ ലോക്ക്: എല്ലാ വിവരങ്ങളും പരിശോധിച്ച് നിങ്ങൾ തന്നെ സമർപ്പിക്കുക അമർത്തുക.",
            or = "ଚୂଡ଼ାନ୍ତ ନିଶ୍ଚିତକରଣ ଲକ୍: ସମସ୍ତ ବିବରଣୀ ସମୀକ୍ଷା କରନ୍ତୁ ଏବଂ ନିଜେ ଦାଖଲ କରନ୍ତୁ ଦବାନ୍ତୁ।",
            asLang = "চূড়ান্ত নিশ্চিতকৰণ লক: সকলো তথ্য পৰীক্ষা কৰক আৰু নিজে দাখিল কৰক টিপক।"
        ),

        // Screen Hint Keys
        "voice_hint_register_hub" to tr(
            "You are on the Home screen. Tap File Complaint to start filing or Case Builder to add evidence.",
            "आप होम स्क्रीन पर हैं। शिकायत दर्ज करने के लिए 'File Complaint' या सबूत जोड़ने के लिए 'Case Builder' दबाएँ।",
            "మీరు హోమ్ స్క్రీన్‌లో ఉన్నారు. ఫిర్యాదు చేయడానికి 'File Complaint' లేదా సాక్ష్యం జోడించడానికి 'Case Builder' నొక్కండి.",
            pa = "ਤੁਸੀਂ ਹੋਮ ਸਕ੍ਰੀਨ 'ਤੇ ਹੋ। ਸ਼ਿਕਾਇਤ ਦਰਜ ਕਰਨ ਲਈ 'File Complaint' ਜਾਂ ਸਬੂਤ ਜੋੜਨ ਲਈ 'Case Builder' ਦਬਾਓ।",
            gu = "તમે હોમ સ્ક્રીન પર છો. ફરિયાદ કરવા માટે 'File Complaint' અથવા પુરાવા ઉમેરવા 'Case Builder' દબાવો.",
            mr = "आपण होम स्क्रीनवर आहात. तक्रार दाखल करण्यासाठी 'File Complaint' किंवा पुरावे जोडण्यासाठी 'Case Builder' दाबा.",
            ta = "நீங்கள் முகப்புத் திரையில் உள்ளீர்கள். புகாரளிக்க 'File Complaint' அல்லது சான்றுகளைச் சேர்க்க 'Case Builder' தட்டவும்.",
            bn = "আপনি হোম স্ক্রিনে আছেন। অভিযোগ জানাতে 'File Complaint' বা প্রমাণ যোগ করতে 'Case Builder' চাপুন।",
            kn = "ನೀವು ಮುಖಪುಟದಲ್ಲಿದ್ದೀರಿ. ದೂರು ಸಲ್ಲಿಸಲು 'File Complaint' ಅಥವಾ ಸಾಕ್ಷಿ ಸೇರಿಸಲು 'Case Builder' ಒತ್ತಿರಿ.",
            ml = "നിങ്ങൾ ഹോം സ്ക്രീനിലാണ്. പരാതി നൽകാൻ 'File Complaint' അല്ലെങ്കിൽ തെളിവ് ചേർക്കാൻ 'Case Builder' അമർത്തുക.",
            or = "ଆପଣ ହୋମ୍ ସ୍କ୍ରିନରେ ଅଛନ୍ତି। ଅଭିଯୋଗ ଦାଖଲ ପାଇଁ 'File Complaint' କିମ୍ବା ପ୍ରମାଣ ଯୋଡ଼ିବାକୁ 'Case Builder' ଦବାନ୍ତୁ।",
            asLang = "আপোনাৰ হোম স্ক্ৰীনত আছে। অভিযোগ কৰিবলৈ 'File Complaint' বা প্ৰমাণ যোগ কৰিবলৈ 'Case Builder' টিপক।"
        ),
        "voice_hint_portal_login" to tr(
            "Enter your mobile number and solve the captcha. Tap Send OTP to receive your SMS code.",
            "अपना मोबाइल नंबर दर्ज करें और कैप्चा भरें। SMS कोड पाने के लिए Send OTP दबाएँ।",
            "మీ మొబైల్ నంబర్ నమోదు చేసి క్యాప్చా పూరించండి. SMS కోడ్ కోసం Send OTP నొక్కండి.",
            pa = "ਆਪਣਾ ਮੋਬਾਈਲ ਨੰਬਰ ਦਰਜ ਕਰੋ ਅਤੇ ਕੈਪਚਾ ਭਰੋ। SMS ਕੋਡ ਪ੍ਰਾਪਤ ਕਰਨ ਲਈ Send OTP ਦਬਾਓ।",
            gu = "તમારો મોબાઇલ નંબર દાખલ કરો અને કેપ્ચા ભરો. SMS કોડ મેળવવા Send OTP દબાવો.",
            mr = "आपला मोबाईल नंबर प्रविष्ट करा आणि कॅप्चा भरा. SMS कोड मिळवण्यासाठी Send OTP दाबा.",
            ta = "உங்கள் மொபைல் எண்ணை உள்ளிட்டு கேப்ட்சாவை நிரப்பவும். SMS பெற Send OTP தட்டவும்.",
            bn = "আপনার মোবাইল নম্বর লিখুন এবং ক্যাপচা পূরণ করুন। SMS কোড পেতে Send OTP চাপুন।",
            kn = "ನಿಮ್ಮ ಮೊಬೈಲ್ ಸಂಖ್ಯೆ ನಮೂದಿಸಿ ಮತ್ತು ಕ್ಯಾಪ್ಚಾ ಭರ್ತಿ ಮಾಡಿ. SMS ಕೋಡ್ ಪಡೆಯಲು Send OTP ಒತ್ತಿರಿ.",
            ml = "നിങ്ങളുടെ മൊബൈൽ നമ്പർ നൽകി ക്യാപ്‌ച പൂരിപ്പിക്കുക. SMS കോഡിനായി Send OTP അമർത്തുക.",
            or = "ଆପଣଙ୍କ ମୋବାଇଲ୍ ନମ୍ବର ପ୍ରବେଶ କରନ୍ତୁ ଏବଂ କ୍ୟାପଚା ପୂରଣ କରନ୍ତୁ। SMS କୋଡ୍ ପାଇଁ Send OTP ଦବାନ୍ତୁ।",
            asLang = "আপোনাৰ ম'বাইল নম্বৰ দিয়ক আৰু কেপচা পূৰণ কৰক। SMS ক'ড পাবলৈ Send OTP টিপক।"
        ),
        "voice_hint_portal_webview" to tr(
            "Enter the OTP received on your mobile phone and tap Submit to verify.",
            "अपने मोबाइल पर आया OTP दर्ज करें और पुष्टि करने के लिए Submit दबाएँ।",
            "మీ మొబైల్‌కు వచ్చిన OTP నమోదు చేసి ధృవీకరించడానికి Submit నొక్కండి.",
            pa = "ਆਪਣੇ ਮੋਬਾਈਲ 'ਤੇ ਆਇਆ OTP ਦਰਜ ਕਰੋ ਅਤੇ ਪੁਸ਼ਟੀ ਲਈ Submit ਦਬਾਓ।",
            gu = "તમારા મોબાઈલ પર આવેલ OTP દાખલ કરો અને ચકાસણી માટે Submit દબાવો.",
            mr = "आपल्या मोबाईलवर आलेला OTP प्रविष्ट करा आणि पडताळणीसाठी Submit दाबा.",
            ta = "உங்கள் மொபைலுக்கு வந்த OTP-ஐ உள்ளிட்டு சரிபார்க்க Submit தட்டவும்.",
            bn = "আপনার মোবাইলে আসা OTP লিখুন এবং যাচাই করতে Submit চাপুন।",
            kn = "ನಿಮ್ಮ ಮೊಬೈಲ್‌ಗೆ ಬಂದ OTP ನಮೂದಿಸಿ ಮತ್ತು ಪರಿಶೀಲಿಸಲು Submit ಒತ್ತಿರಿ.",
            ml = "നിങ്ങളുടെ മൊബൈലിൽ വന്ന OTP നൽകി സ്ഥിരീകരിക്കാൻ Submit അമർത്തുക.",
            or = "ଆପଣଙ୍କ ମୋବାଇଲକୁ ଆସିଥିବା OTP ପ୍ରବେଶ କରନ୍ତୁ ଏବଂ ଯାଞ୍ଚ କରିବାକୁ Submit ଦବାନ୍ତୁ।",
            asLang = "আপোনাৰ ম'বাইলত অহা OTP দিয়ক আৰু সত্যাপনৰ বাবে Submit টিপক।"
        ),
        "voice_hint_portal_clone_0" to tr(
            "Select your complaint category and sub-category from the options, then tap Save & Next.",
            "विकल्पों में से अपनी शिकायत की श्रेणी और उप-श्रेणी चुनें, फिर Save & Next दबाएँ।",
            "ఎంపికల నుండి మీ ఫిర్యాదు వర్గం మరియు ఉప-వర్గాన్ని ఎంచుకోండి, తర్వాత Save & Next నొక్కండి.",
            pa = "ਚੋਣਾਂ ਵਿੱਚੋਂ ਆਪਣੀ ਸ਼ਿਕਾਇਤ ਦੀ ਸ਼੍ਰੇਣੀ ਅਤੇ ਉਪ-ਸ਼੍ਰੇਣੀ ਚੁਣੋ, ਫਿਰ Save & Next ਦਬਾਓ।",
            gu = "વિકલ્પોમાંથી તમારી ફરિયાદની કેટેગરી અને સબ-કેટેગરી પસંદ કરો, પછી Save & Next દબાવો.",
            mr = "पर्यायांतून आपल्या तक्रारीची श्रेणी आणि उप-श्रेणी निवडा, नंतर Save & Next दाबा.",
            ta = "விருப்பங்களிலிருந்து உங்கள் புகார் வகை மற்றும் துணை வகையைத் தேர்ந்தெடுத்து Save & Next தட்டவும்.",
            bn = "বিকল্পগুলি থেকে আপনার অভিযোগের বিভাগ এবং উপ-বিভাগ নির্বাচন করুন, তারপর Save & Next চাপুন।",
            kn = "ಆಯ್ಕೆಗಳಿಂದ ನಿಮ್ಮ ದೂರಿನ ವರ್ಗ ಮತ್ತು ಉಪ-ವರ್ಗವನ್ನು ಆಯ್ಕೆಮಾಡಿ, ನಂತರ Save & Next ಒತ್ತಿರಿ.",
            ml = "ഓപ്ഷനുകളിൽ നിന്ന് നിങ്ങളുടെ പരാതി വിഭാഗവും ഉപവിഭാഗവും തിരഞ്ഞെടുക്കുക, തുടർന്ന് Save & Next അമർത്തുക.",
            or = "ବିକଳ୍ପଗୁଡ଼ିକରୁ ଆପଣଙ୍କ ଅଭିଯୋଗ ବର୍ଗ ଏବଂ ଉପ-ବର୍ଗ ବାଛନ୍ତୁ, ତା'ପରେ Save & Next ଦବାନ୍ତୁ।",
            asLang = "বিকল্পসমূহৰ পৰা আপোনাৰ অভিযোগৰ শ্ৰেণী আৰু উপ-শ্ৰেণী বাছক, তাৰ পিছত Save & Next টিপক।"
        ),
        "voice_hint_portal_clone_1" to tr(
            "Enter the date, time, and incident details where the fraud occurred, then tap Save & Next.",
            "धोखाधड़ी की तारीख, समय और घटना का विवरण दर्ज करें, फिर Save & Next दबाएँ।",
            "మోసం జరిగిన తేదీ, సమయం మరియు ఘటన వివరాలను నమోదు చేయండి, తర్వాత Save & Next నొక్కండి.",
            pa = "ਧੋਖਾਧੜੀ ਦੀ ਮਿਤੀ, ਸਮਾਂ ਅਤੇ ਘਟਨਾ ਦੇ ਵੇਰਵੇ ਦਰਜ ਕਰੋ, ਫਿਰ Save & Next ਦਬਾਓ।",
            gu = "છેતરપિંડીની તારીખ, સમય અને ઘટનાની વિગતો દાખલ કરો, પછી Save & Next દબાવો.",
            mr = "फसवणुकीची तारीख, वेळ आणि घटनेचा तपशील प्रविष्ट करा, नंतर Save & Next दाबा.",
            ta = "மோசடி நடந்த தேதி, நேரம் மற்றும் சம்பவ விவரங்களை உள்ளிட்டு Save & Next தட்டவும்.",
            bn = "প্রতারণার তারিখ, সময় এবং ঘটনার বিবরণ লিখুন, তারপর Save & Next চাপুন।",
            kn = "ವಂಚನೆ ನಡೆದ ದಿನಾಂಕ, ಸಮಯ ಮತ್ತು ಘಟನೆಯ ವಿವರಗಳನ್ನು ನಮೂದಿಸಿ, ನಂತರ Save & Next ಒತ್ತಿರಿ.",
            ml = "തട്ടിപ്പ് നടന്ന തീയതി, സമയം, സംഭവ വിവരങ്ങൾ എന്നിവ നൽകി Save & Next അമർത്തുക.",
            or = "ଠକେଇ ହୋଇଥିବା ତାରିଖ, ସମୟ ଏବଂ ଘଟଣା ବିବରଣୀ ପ୍ରବେଶ କରନ୍ତୁ, ତା'ପରେ Save & Next ଦବାନ୍ତୁ।",
            asLang = "প্ৰতাৰণা হোৱা তাৰিখ, সময় আৰু ঘটনাৰ বিৱৰণ দিয়ক, তাৰ পিছত Save & Next টিপক।"
        ),
        "voice_hint_portal_clone_2" to tr(
            "Enter suspect details like UPI ID, mobile number, or website if known, then tap Save & Next.",
            "संदिग्ध के विवरण जैसे UPI ID, मोबाइल नंबर या वेबसाइट भरें, फिर Save & Next दबाएँ।",
            "తెలిస్తే అనుమానితుడి UPI ID, మొబైల్ లేదా వెబ్‌సైట్ నమోదు చేయండి, తర్వాత Save & Next నొక్కండి.",
            pa = "ਜੇ ਪਤਾ ਹੋਵੇ ਤਾਂ ਸ਼ੱਕੀ ਦੇ UPI ID, ਮੋਬਾਈਲ ਜਾਂ ਵੈੱਬਸਾਈਟ ਦਰਜ ਕਰੋ, ਫਿਰ Save & Next ਦਬਾਓ।",
            gu = "જો ખબર હોય તો શંકાસ્પદની UPI ID, મોબાઇલ અથવા વેબસાઇટ દાખલ કરો, પછી Save & Next દબાવો.",
            mr = "माहित असल्यास संशयिताची UPI ID, मोबाईल किंवा वेबसाइट प्रविष्ट करा, नंतर Save & Next दाबा.",
            ta = "தெரிந்தால் சந்தேக நபரின் UPI ID, மொபைல் அல்லது வலைத்தளத்தை உள்ளிட்டு Save & Next தட்டவும்.",
            bn = "জানা থাকলে সন্দেহভাজনের UPI ID, মোবাইল বা ওয়েবসাইট লিখুন, তারপর Save & Next চাপুন।",
            kn = "ತಿಳಿದಿದ್ದರೆ ಶಂಕಿತರ UPI ID, ಮೊಬೈಲ್ ಅಥವಾ ವೆಬ್‌ಸೈಟ್ ನಮೂದಿಸಿ, ನಂತರ Save & Next ಒತ್ತಿರಿ.",
            ml = "അറിയാമെങ്കിൽ സംശയിക്കപ്പെടുന്ന ആളുടെ UPI ID, മൊബൈൽ അല്ലെങ്കിൽ വെബ്സൈറ്റ് നൽകി Save & Next അമർത്തുക.",
            or = "ଜଣାଥିଲେ ସନ୍ଦିଗ୍ଧଙ୍କ UPI ID, ମୋବାଇଲ୍ କିମ୍ବା ୱେବସାଇଟ୍ ପ୍ରବେଶ କରନ୍ତୁ, ତା'ପରେ Save & Next ଦବାନ୍ତୁ।",
            asLang = "জনা থাকিলে সন্দেহযুক্ত ব্যক্তিৰ UPI ID, ম'বাইল বা ৱেবছাইট দিয়ক, তাৰ পিছত Save & Next টিপক।"
        ),
        "voice_hint_portal_clone_3" to tr(
            "Verify your complainant details, name, and address, then tap Save & Next.",
            "शिकायतकर्ता के विवरण, नाम और पते की जाँच करें, फिर Save & Next दबाएँ।",
            "ఫిర్యాదుదారు వివరాలు, పేరు మరియు చిరునామాను సరిచూసి Save & Next నొక్కండి.",
            pa = "ਸ਼ਿਕਾਇਤਕਰਤਾ ਦੇ ਵੇਰਵੇ, ਨਾਮ ਅਤੇ ਪਤੇ ਦੀ ਪੁਸ਼ਟੀ ਕਰੋ, ਫਿਰ Save & Next ਦਬਾਓ।",
            gu = "ફરિયાદીની વિગતો, નામ અને સરનામું ચકાસો, પછી Save & Next દબાવો.",
            mr = "तक्रारदाराचे तपशील, नाव आणि पत्ता तपासा, नंतर Save & Next दाबा.",
            ta = "புகார்தாரர் விவரங்கள், பெயர் மற்றும் முகவரியைச் சரிபார்த்து Save & Next தட்டவும்.",
            bn = "অভিযোগকারীর বিবরণ, নাম এবং ঠিকানা যাচাই করুন, তারপর Save & Next চাপুন।",
            kn = "ದೂರುದಾರರ ವಿವರಗಳು, ಹೆಸರು ಮತ್ತು ವಿಳಾಸವನ್ನು ಪರಿಶೀಲಿಸಿ Save & Next ಒತ್ತಿರಿ.",
            ml = "പരാതിക്കാരന്റെ വിവരങ്ങൾ, പേര്, വിലാസം എന്നിവ പരിശോധിച്ച് Save & Next അമർത്തുക.",
            or = "ଅଭିଯୋଗକାରୀଙ୍କ ବିବରଣୀ, ନାମ ଏବଂ ଠିକଣା ଯାଞ୍ଚ କରନ୍ତୁ, ତା'ପରେ Save & Next ଦବାନ୍ତୁ।",
            asLang = "অভিযোগকাৰীৰ তথ্য, নাম আৰু ঠিকনা পৰীক্ষা কৰক, তাৰ পিছত Save & Next টিপক।"
        ),
        "voice_hint_portal_clone_4" to tr(
            "Upload supporting evidence documents such as bank statements or screenshots, then tap Save & Next.",
            "बैंक स्टेटमेंट या स्क्रीनशॉट जैसे साक्ष्य दस्तावेज़ अपलोड करें, फिर Save & Next दबाएँ।",
            "బ్యాంక్ స్టేట్‌మెంట్‌లు లేదా స్క్రీన్‌షాట్‌ల వంటి సాక్ష్య పత్రాలను అప్‌లోడ్ చేసి Save & Next నొక్కండి.",
            pa = "ਬੈਂਕ ਸਟੇਟਮੈਂਟ ਜਾਂ ਸਕ੍ਰੀਨਸ਼ਾਟ ਵਰਗੇ ਸਬੂਤ ਦਸਤਾਵੇਜ਼ ਅੱਪਲੋਡ ਕਰੋ, ਫਿਰ Save & Next ਦਬਾਓ।",
            gu = "બેંક સ્ટેટમેન્ટ અથવા સ્ક્રીનશૉટ્સ જેવા પુરાવા દસ્તાવેજો અપલોડ કરો, પછી Save & Next દબાવો.",
            mr = "बँक स्टेटमेंट किंवा स्क्रीनशॉट यासारखे पुरावे दस्तऐवज अपलोड करा, नंतर Save & Next दाबा.",
            ta = "வங்கி அறிக்கை அல்லது ஸ்கிரீன்ஷாட் போன்ற ஆதார ஆவணங்களைப் பதிவேற்றி Save & Next தட்டவும்.",
            bn = "ব্যাংক স্টেটমেন্ট বা স্ক্রিনশটের মতো প্রমাণের নথি আপলোড করুন, তারপর Save & Next চাপুন।",
            kn = "ಬ್ಯಾಂಕ್ ಸ್ಟೇಟ್‌ಮೆಂಟ್ ಅಥವಾ ಸ್ಕ್ರೀನ್‌ಶಾಟ್‌ಗಳಂತಹ ಪುರಾವೆ ದಾಖಲೆಗಳನ್ನು ಅಪ್‌ಲೋಡ್ ಮಾಡಿ Save & Next ಒತ್ತಿರಿ.",
            ml = "ബാങ്ക് സ്റ്റേറ്റ്മെന്റുകൾ അല്ലെങ്കിൽ സ്ക്രീൻഷോട്ടുകൾ പോലുള്ള തെളിവ് രേഖകൾ അപ്‌ലോഡ് ചെയ്ത് Save & Next അമർത്തുക.",
            or = "ବ୍ୟାଙ୍କ ଷ୍ଟେଟମେଣ୍ଟ୍ କିମ୍ବା ସ୍କ୍ରିନସଟ୍ ଭଳି ପ୍ରମାଣ ଦଲିଲ ଅପଲୋଡ୍ କରନ୍ତୁ, ତା'ପରେ Save & Next ଦବାନ୍ତୁ।",
            asLang = "বেংক ষ্টেটমেণ্ট বা স্ক্ৰীণশ্বটৰ দৰে প্ৰমাণ নথি আপলোড কৰক, তাৰ পিছত Save & Next টিপক।"
        ),
        "voice_hint_portal_clone_5" to tr(
            "Carefully preview all entered complaint details before final submission.",
            "अंतिम रूप से भेजने से पहले दर्ज किए गए सभी विवरणों का पूर्वावलोकन ध्यान से देखें।",
            "తుది సమర్పణకు ముందు నమోదు చేసిన అన్ని వివరాలను జాగ్రత్తగా ప్రివ్యూ చేయండి.",
            pa = "ਅੰਤਿਮ ਸਬਮਿਟ ਕਰਨ ਤੋਂ ਪਹਿਲਾਂ ਦਰਜ ਕੀਤੇ ਸਾਰੇ ਵੇਰਵਿਆਂ ਦੀ ਧਿਆਨ ਨਾਲ ਸਮੀਖਿਆ ਕਰੋ।",
            gu = "અંતિમ સબમિશન પહેલાં દાખલ કરેલી બધી વિગતોનું ધ્યાનપૂર્વક પૂર્વાવલોકન કરો.",
            mr = "अंतिम सबमिशनपूर्वी प्रविष्ट केलेल्या सर्व तपशिलांचे काळजीपूर्वक पूर्वावलोकन करा.",
            ta = "இறுதி சமர்ப்பிப்புக்கு முன் உள்ளிட்ட அனைத்து விவரங்களையும் கவனமாக முன்னோட்டம் பாருங்கள்.",
            bn = "চূড়ান্ত জমা দেওয়ার আগে প্রবেশ করা সমস্ত বিবরণ সাবধানে প্রাকদর্শন করুন।",
            kn = "ಅಂತಿಮ ಸಲ್ಲಿಕೆಗೆ ಮೊದಲು ನಮೂದಿಸಿದ ಎಲ್ಲಾ ವಿವರಗಳನ್ನು ಎಚ್ಚರಿಕೆಯಿಂದ ಮುನ್ನೋಟ ನೋಡಿ.",
            ml = "അന്തിമ സമർപ്പണത്തിന് മുമ്പ് നൽകിയ എല്ലാ വിവരങ്ങളും ശ്രദ്ധാപൂർവ്വം പ്രിവ്യൂ ചെയ്യുക.",
            or = "ଚୂଡ଼ାନ୍ତ ଦାଖଲ ପୂର୍ବରୁ ପ୍ରବେଶ କରାଯାଇଥିବା ସମସ୍ତ ବିବରଣୀ ଧ୍ୟାନର ସହିତ ପୂର୍ବାବଲୋକନ କରନ୍ତୁ।",
            asLang = "চূড়ান্ত দাখিলৰ আগতে দিয়া সকলো তথ্য সাৱধানে প্ৰিভিউ কৰক।"
        ),
        "voice_hint_portal_clone_6" to tr(
            "This is the final lock. Review the declaration and tap Submit to officially file.",
            "यह अंतिम लॉक है। घोषणा की जाँच करें और औपचारिक रूप से दर्ज करने के लिए Submit दबाएँ।",
            "ఇది తుది లాక్. ప్రకటనను పరిశీలించి అధికారికంగా దాఖలు చేయడానికి Submit నొక్కండి.",
            pa = "ਇਹ ਅੰਤਿਮ ਲਾਕ ਹੈ। ਘੋਸ਼ਣਾ ਦੀ ਸਮੀਖਿਆ ਕਰੋ ਅਤੇ ਅਧਿਕਾਰਤ ਤੌਰ 'ਤੇ ਦਾਇਰ ਕਰਨ ਲਈ Submit ਦਬਾਓ।",
            gu = "આ અંતિમ લૉક છે. ઘોષણા ચકાસો અને સત્તાવાર રીતે ફાઇલ કરવા માટે Submit દબાવો.",
            mr = "हा अंतिम लॉक आहे. घोषणा तपासा आणि अधिकृतपणे दाखल करण्यासाठी Submit दाबा.",
            ta = "இது இறுதி பூட்டு. பிரகடனத்தை மதிப்பாய்வு செய்து அதிகாரப்பூர்வமாகத் தாக்கல் செய்ய Submit தட்டவும்.",
            bn = "এটি চূড়ান্ত লক। ঘোষণা পর্যালোচনা করুন এবং আনুষ্ঠানিকভাবে দায়ের করতে Submit চাপুন।",
            kn = "ಇದು ಅಂತಿಮ ಲಾಕ್. ಘೋಷಣೆಯನ್ನು ಪರಿಶೀಲಿಸಿ ಮತ್ತು ಅಧಿಕೃತವಾಗಿ ಸಲ್ಲಿಸಲು Submit ಒತ್ತಿರಿ.",
            ml = "ഇതാണ് അന്തിമ ലോക്ക്. പ്രഖ്യാപനം പരിശോധിച്ച് ഔദ്യോഗികമായി ഫയൽ ചെയ്യാൻ Submit അമർത്തുക.",
            or = "ଏହା ଚୂଡ଼ାନ୍ତ ଲକ୍। ଘୋଷଣା ସମୀକ୍ଷା କରନ୍ତୁ ଏବଂ ଆନୁଷ୍ଠାନିକ ଭାବରେ ଦାଖଲ କରିବାକୁ Submit ଦବାନ୍ତୁ।",
            asLang = "এইটো চূড়ান্ত লক। ঘোষণা পৰীক্ষা কৰক আৰু আনুষ্ঠানিকভাৱে দাখিল কৰিবলৈ Submit টিপক।"
        ),
        "voice_hint_evidence" to tr(
            "Tap Add Evidence File to attach bank statements, transaction screenshots, or SMS records.",
            "बैंक स्टेटमेंट, लेन-देन के स्क्रीनशॉट या SMS जोड़ने के लिए 'सबूत फ़ाइल जोड़ें' दबाएँ।",
            "బ్యాంక్ స్టేట్‌మెంట్‌లు, లావాదేవీ స్క్రీన్‌షాట్‌లు లేదా SMS జోడించడానికి 'సాక్ష్య ఫైల్ జోడించు' నొక్కండి.",
            pa = "ਬੈਂਕ ਸਟੇਟਮੈਂਟ, ਲੈਣ-ਦੇਣ ਸਕ੍ਰੀਨਸ਼ਾਟ ਜਾਂ SMS ਜੋੜਨ ਲਈ 'ਸਬੂਤ ਫ਼ਾਈਲ ਜੋੜੋ' ਦਬਾਓ।",
            gu = "બેંક સ્ટેટમેન્ટ, વ્યવહારના સ્ક્રીનશૉટ્સ અથવા SMS જોડવા માટે 'પુરાવા ફાઇલ ઉમેરો' દબાવો.",
            mr = "बँक स्टेटमेंट, व्यवहाराचे स्क्रीनशॉट किंवा SMS जोडण्यासाठी 'पुरावा फाइल जोडा' दाबा.",
            ta = "வங்கி அறிக்கை, பரிவர்த்தனை ஸ்கிரீன்ஷாட் அல்லது SMS சேர்க்க 'சான்று கோப்பைச் சேர்' தட்டவும்.",
            bn = "ব্যাংক স্টেটমেন্ট, লেনদেনের স্ক্রিনশট বা SMS যুক্ত করতে 'প্রমাণ ফাইল যোগ করুন' চাপুন।",
            kn = "ಬ್ಯಾಂಕ್ ಸ್ಟೇಟ್‌ಮೆಂಟ್, ವಹಿವಾಟು ಸ್ಕ್ರೀನ್‌ಶಾಟ್‌ಗಳು ಅಥವಾ SMS ಸೇರಿಸಲು 'ಸಾಕ್ಷಿ ಫೈಲ್ ಸೇರಿಸಿ' ಒತ್ತಿರಿ.",
            ml = "ബാങ്ക് സ്റ്റേറ്റ്‌മെന്റുകൾ, ഇടപാട് സ്ക്രീൻഷോട്ടുകൾ അല്ലെങ്കിൽ SMS ചേർക്കാൻ 'തെളിവ് ഫയൽ ചേർക്കുക' അമർത്തുക.",
            or = "ବ୍ୟାଙ୍କ ଷ୍ଟେଟମେଣ୍ଟ୍, କାରବାର ସ୍କ୍ରିନସଟ୍ କିମ୍ବା SMS ଯୋଡ଼ିବାକୁ 'ପ୍ରମାଣ ଫାଇଲ୍ ଯୋଡ଼ନ୍ତୁ' ଦବାନ୍ତୁ।",
            asLang = "বেংক ষ্টেটমেণ্ট, লেনদেনৰ স্ক্ৰীণশ্বট বা SMS যোগ কৰিবলৈ 'প্ৰমাণ ফাইল যোগ কৰক' টিপক।"
        ),
        "voice_hint_guided_q" to tr(
            "Answer this question with your details, or tap Listen to hear it read aloud.",
            "अपने विवरण के साथ इस प्रश्न का उत्तर दें, या इसे सुनने के लिए 'Listen' दबाएँ।",
            "మీ వివరాలతో ఈ ప్రశ్నకు సమాధానం ఇవ్వండి, లేదా వినడానికి 'Listen' నొక్కండి.",
            pa = "ਆਪਣੇ ਵੇਰਵਿਆਂ ਨਾਲ ਇਸ ਸਵਾਲ ਦਾ ਜਵਾਬ ਦਿਓ, ਜਾਂ ਸੁਣਨ ਲਈ 'Listen' ਦਬਾਓ।",
            gu = "તમારી વિગતો સાથે આ પ્રશ્નનો જવાબ આપો, અથવા સાંભળવા માટે 'Listen' દબાવો.",
            mr = "आपल्या तपशिलांसह या प्रश्नाचे उत्तर द्या, किंवा ऐकण्यासाठी 'Listen' दाबा.",
            ta = "உங்கள் விவரங்களுடன் இந்தக் கேள்விக்கு பதிலளிக்கவும், அல்லது கேட்க 'Listen' தட்டவும்.",
            bn = "আপনার বিশদ সহ এই প্রশ্নের উত্তর দিন, অথবা শুনতে 'Listen' চাপুন।",
            kn = "ನಿಮ್ಮ ವಿವರಗಳೊಂದಿಗೆ ಈ ಪ್ರಶ್ನೆಗೆ ಉತ್ತರಿಸಿ, ಅಥವಾ ಕೇಳಿಸಿಕೊಳ್ಳಲು 'Listen' ಒತ್ತಿರಿ.",
            ml = "നിങ്ങളുടെ വിവരങ്ങൾ നൽകി ഈ ചോദ്യത്തിന് ഉത്തരം നൽകുക, അല്ലെങ്കിൽ കേൾക്കാൻ 'Listen' അമർത്തുക.",
            or = "ଆପଣଙ୍କ ବିବରଣୀ ସହିତ ଏହି ପ୍ରଶ୍ନର ଉତ୍ତର ଦିଅନ୍ତୁ, କିମ୍ବା ଶୁଣିବା ପାଇଁ 'Listen' ଦବାନ୍ତୁ।",
            asLang = "আপোনাৰ বিৱৰণৰ সৈতে এই প্ৰশ্নৰ উত্তৰ দিয়ক, বা শুনিবলৈ 'Listen' টিপক।"
        )
    )
}
