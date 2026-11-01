/**
 * Cybridge Marketing Website - Main Controller
 * Handles configuration binding, language toggling, accordions, and clipboard copying.
 * Strictly adheres to clean typography without decorative symbols or glyphs.
 */

document.addEventListener("DOMContentLoaded", () => {
  // 1. Bind Dynamic Configuration to DOM
  bindConfigData();

  // 2. Initialize Language Toggle
  initLanguageSwitcher();

  // 3. Initialize Accordions (FAQ and Installation Guide)
  initAccordions();

  // 4. Initialize SHA-256 Copy Action
  initCopyChecksum();

  // 5. Initialize Mobile Navigation Menu
  initMobileNav();
});

function bindConfigData() {
  if (typeof CYBRIDGE_CONFIG === "undefined") return;

  const bind = (id, value) => {
    const el = document.getElementById(id);
    if (el) el.textContent = value;
  };

  const bindAttr = (id, attr, value) => {
    const el = document.getElementById(id);
    if (el) el.setAttribute(attr, value);
  };

  bind("cfg-version", CYBRIDGE_CONFIG.version);
  bind("cfg-size", CYBRIDGE_CONFIG.fileSize);
  bind("cfg-min-android", CYBRIDGE_CONFIG.minAndroid);
  bind("cfg-updated", CYBRIDGE_CONFIG.lastUpdated);
  bind("cfg-checksum", CYBRIDGE_CONFIG.sha256Checksum);
  bind("cfg-package", CYBRIDGE_CONFIG.packageName);

  bindAttr("cfg-download-btn", "href", CYBRIDGE_CONFIG.downloadUrl);
  bindAttr("cfg-hero-download-btn", "href", CYBRIDGE_CONFIG.downloadUrl);
  bindAttr("cfg-nav-download-btn", "href", CYBRIDGE_CONFIG.downloadUrl);
}

function initCopyChecksum() {
  const copyBtn = document.getElementById("copy-checksum-btn");
  const checksumText = document.getElementById("cfg-checksum");

  if (!copyBtn || !checksumText) return;

  copyBtn.addEventListener("click", () => {
    const textToCopy = checksumText.textContent.trim();
    if (!textToCopy) return;

    navigator.clipboard.writeText(textToCopy).then(() => {
      const originalText = copyBtn.textContent;
      copyBtn.textContent = "Copied to clipboard";
      copyBtn.classList.add("btn-copied");

      setTimeout(() => {
        copyBtn.textContent = originalText;
        copyBtn.classList.remove("btn-copied");
      }, 2500);
    }).catch(() => {
      // Fallback
      const input = document.createElement("input");
      input.value = textToCopy;
      document.body.appendChild(input);
      input.select();
      document.execCommand("copy");
      document.body.removeChild(input);

      copyBtn.textContent = "Copied to clipboard";
      setTimeout(() => {
        copyBtn.textContent = "Copy SHA-256";
      }, 2500);
    });
  });
}

function initAccordions() {
  const accordionButtons = document.querySelectorAll(".accordion-header");

  accordionButtons.forEach(button => {
    button.addEventListener("click", () => {
      const isExpanded = button.getAttribute("aria-expanded") === "true";
      const targetId = button.getAttribute("aria-controls");
      const targetPanel = document.getElementById(targetId);
      const stateLabel = button.querySelector(".accordion-indicator");

      if (!targetPanel) return;

      if (isExpanded) {
        button.setAttribute("aria-expanded", "false");
        targetPanel.hidden = true;
        targetPanel.classList.remove("open");
        if (stateLabel) stateLabel.textContent = "Show Details";
      } else {
        button.setAttribute("aria-expanded", "true");
        targetPanel.hidden = false;
        targetPanel.classList.add("open");
        if (stateLabel) stateLabel.textContent = "Hide Details";
      }
    });
  });
}

function initMobileNav() {
  const menuToggle = document.getElementById("mobile-menu-toggle");
  const navLinks = document.getElementById("nav-links");

  if (!menuToggle || !navLinks) return;

  menuToggle.addEventListener("click", () => {
    const isOpen = navLinks.classList.contains("nav-open");
    if (isOpen) {
      navLinks.classList.remove("nav-open");
      menuToggle.textContent = "Menu";
      menuToggle.setAttribute("aria-expanded", "false");
    } else {
      navLinks.classList.add("nav-open");
      menuToggle.textContent = "Close";
      menuToggle.setAttribute("aria-expanded", "true");
    }
  });

  // Close nav when an anchor link is clicked
  navLinks.querySelectorAll("a").forEach(link => {
    link.addEventListener("click", () => {
      navLinks.classList.remove("nav-open");
      menuToggle.textContent = "Menu";
      menuToggle.setAttribute("aria-expanded", "false");
    });
  });
}

// --------------------------------------------------------------------------
// Multi-Language Translations (English and Hindi)
// --------------------------------------------------------------------------
const TRANSLATIONS = {
  en: {
    navFeatures: "Features",
    navHow: "How It Works",
    navSafety: "Safety Boundary",
    navPrivacy: "Privacy",
    navLanguages: "Languages",
    navFaq: "FAQ",
    navDownload: "Download App",
    
    emergencyTitle: "Lost money to cyber fraud right now? Call 1930 immediately.",
    emergencyBtn: "Call 1930 Helpline",
    
    heroTitle: "Cyber fraud happened? Act in the Golden Hour.",
    heroSubtitle: "Cybridge is an on-device, privacy-first Android assistant that helps Indian cybercrime victims prepare verified evidence and file accurate reports on cybercrime.gov.in.",
    heroDownload: "Download for Android",
    heroHow: "See How It Works",
    heroTrust: "Works offline | Data stays on your phone | 12 Indian languages | Completely free",
    
    problemBadge: "The Problem",
    problemHeading: "Why cyber fraud reporting fails in the first critical hours",
    
    howBadge: "How It Works",
    howHeading: "Four guided steps from incident to formal submission",
    
    featuresBadge: "Capabilities",
    featuresHeading: "Purpose-built tools for cyber financial fraud",
    
    safetyBadge: "Safety Boundary",
    safetyHeading: "You stay in complete control of your filing",
    safetySub: "Cybridge enforces an immutable architectural boundary: the AI assists with preparation, but never submits for you.",
    
    privacyBadge: "Privacy Guarantee",
    privacyHeading: "Your evidence and personal details never leave your device",
    
    languagesBadge: "12 Indian Languages",
    languagesHeading: "Empowering every citizen in their native tongue",
    
    downloadBadge: "Direct Installation",
    downloadHeading: "Download Cybridge for Android",
    
    faqBadge: "Questions and Answers",
    faqHeading: "Frequently Asked Questions"
  },
  hi: {
    navFeatures: "विशेषताएं",
    navHow: "काम कैसे करता है",
    navSafety: "सुरक्षा सीमा",
    navPrivacy: "गोपनीयता",
    navLanguages: "भाषाएं",
    navFaq: "सामान्य प्रश्न",
    navDownload: "ऐप डाउनलोड करें",
    
    emergencyTitle: "क्या आपके साथ अभी साइबर धोखाधड़ी हुई है? तुरंत 1930 पर कॉल करें।",
    emergencyBtn: "1930 हेल्पलाइन पर कॉल करें",
    
    heroTitle: "साइबर धोखाधड़ी हुई? गोल्डन ऑवर में तुरंत कदम उठाएं।",
    heroSubtitle: "साइब्रिज एक ऑन-डिवाइस, गोपनीयता-प्रथम एंड्रॉइड सहायक है जो भारतीय साइबर अपराध पीड़ितों को साक्ष्य तैयार करने और cybercrime.gov.in पर सटीक रिपोर्ट दर्ज करने में मदद करता है।",
    heroDownload: "एंड्रॉइड के लिए डाउनलोड करें",
    heroHow: "देखें यह कैसे काम करता है",
    heroTrust: "ऑफ़लाइन काम करता है | आपका डेटा फोन पर ही रहता है | 12 भारतीय भाषाएं | पूरी तरह निःशुल्क",
    
    problemBadge: "समस्या",
    problemHeading: "शुरुआती महत्वपूर्ण घंटों में साइबर रिपोर्टिंग क्यों विफल हो जाती है",
    
    howBadge: "काम कैसे करता है",
    howHeading: "घटना से लेकर औपचारिक शिकायत तक चार सरल चरण",
    
    featuresBadge: "मुख्य विशेषताएं",
    featuresHeading: "साइबर वित्तीय धोखाधड़ी के लिए विशेष रूप से निर्मित उपकरण",
    
    safetyBadge: "सुरक्षा सीमा",
    safetyHeading: "आप अपनी शिकायत पर पूरा नियंत्रण रखते हैं",
    safetySub: "साइब्रिज एक सख्त सुरक्षा नियम का पालन करता है: एआई केवल जानकारी भरने में मदद करता है, आपके बदले कभी सबमिट नहीं करता।",
    
    privacyBadge: "गोपनीयता की गारंटी",
    privacyHeading: "आपके साक्ष्य और व्यक्तिगत विवरण कभी फोन से बाहर नहीं जाते",
    
    languagesBadge: "12 भारतीय भाषाएं",
    languagesHeading: "हर नागरिक को अपनी मातृभाषा में सहायता",
    
    downloadBadge: "सीधा इंस्टॉलेशन",
    downloadHeading: "एंड्रॉइड के लिए साइब्रिज डाउनलोड करें",
    
    faqBadge: "प्रश्नोत्तरी",
    faqHeading: "अक्सर पूछे जाने वाले प्रश्न"
  }
};

function initLanguageSwitcher() {
  const langToggleBtn = document.getElementById("lang-toggle-btn");
  if (!langToggleBtn) return;

  let currentLang = "en";

  langToggleBtn.addEventListener("click", () => {
    currentLang = currentLang === "en" ? "hi" : "en";
    applyTranslations(currentLang);
    langToggleBtn.textContent = currentLang === "en" ? "हिन्दी" : "English";
    document.documentElement.lang = currentLang;
  });
}

function applyTranslations(lang) {
  const t = TRANSLATIONS[lang];
  if (!t) return;

  document.querySelectorAll("[data-i18n]").forEach(el => {
    const key = el.getAttribute("data-i18n");
    if (t[key]) {
      el.textContent = t[key];
    }
  });
}
