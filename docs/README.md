# Cybridge Marketing Website & Distribution Hub

Professional, clean marketing website and APK distribution portal for **Cybridge (Cyber Victim Assistant)**, package `com.cybervictim.assistant`.

---

## 📁 Directory Structure

```
website/
├── index.html          # Main landing page (single-page, smooth scroll)
├── privacy.html        # Detailed privacy policy (local-first data guarantees)
├── terms.html          # Terms of service & statutory disclaimer
├── styles.css          # Design system with Tricolor Gov palette (zero symbols)
├── config.js           # Single configuration object for APK metadata & links
├── script.js           # Vanilla JS controller (language toggle, accordions, copy)
├── robots.txt          # Search engine crawler permissions
├── sitemap.xml         # XML sitemap for SEO indexation
├── assets/
│   ├── logo/
│   │   └── app_logo.png      # Official application logo
│   ├── qr_download.png       # Direct download QR code for desktop users
│   └── screenshots/
│       ├── app_home.png      # Official app home screen
│       └── app_intake.png    # 8-step intake mobile screen
└── downloads/
    └── cybridge.apk          # Pre-packaged production debug APK (55 MB)
```

---

## ⚙️ How to Update the APK & Metadata

All editable values (download link, version number, file size, minimum Android version, release date, and cryptographic checksum) are centralized in a single file: `config.js`.

### Step 1: Replace the APK File
1. Build your new release APK in Android Studio or via Gradle:
   ```bash
   cd /home/hp/CyberVictimAssistant
   ./gradlew assembleDebug   # or assembleRelease
   ```
2. Copy the newly compiled `.apk` to the `downloads/` directory:
   ```bash
   cp app/build/outputs/apk/debug/app-debug.apk website/downloads/cybridge.apk
   ```

### Step 2: Compute the New SHA-256 Checksum
Generate the cryptographic checksum of the new APK:
```bash
sha256sum website/downloads/cybridge.apk
```

### Step 3: Update `config.js`
Open `website/config.js` and update the relevant fields:
```javascript
const CYBRIDGE_CONFIG = {
  appName: "Cybridge",
  tagline: "Cyber Victim Assistant",
  packageName: "com.cybervictim.assistant",
  
  // Update these on each release:
  downloadUrl: "downloads/cybridge.apk",
  version: "0.2.0",
  versionCode: 2,
  fileSize: "55 MB",
  minAndroid: "Android 8.0+ (API 26+)",
  lastUpdated: "November 1, 2026",
  sha256Checksum: "your_new_sha256_hash_here",
  
  helplineNumber: "1930",
  contactEmail: "vijjadapremsaiofficial@gmail.com"
};
```
Once updated, the website will automatically reflect the new version, file size, date, and checksum across the page without needing to edit any HTML!

---

## 🚀 How to Deploy

The website is built with vanilla HTML5, CSS3, and JavaScript, meaning it can be hosted for free on any static site hosting service.

### Option A: GitHub Pages (Recommended)
You can deploy directly from your GitHub repository:
1. Copy the website files into a `docs/` folder or keep it in `website/` (or on a dedicated `gh-pages` branch):
   ```bash
   # Example: serving from docs/ on main branch
   cp -r website/* docs/
   git add docs/
   git commit -m "feat: update marketing website"
   git push origin main
   ```
2. On GitHub, navigate to **Settings > Pages**.
3. Under **Branch**, select `main` and set the folder to `/docs` (or `/ (root)` if deployed from a `gh-pages` branch).
4. Click **Save**. Your site will be live at:  
   `https://<your-username>.github.io/<repo-name>/`

### Option B: Netlify
1. Log in to [Netlify](https://www.netlify.com/).
2. Click **Add new site > Deploy manually**.
3. Drag and drop the `website/` folder onto the Netlify dashboard.
4. Your website is instantly live with free HTTPS and global CDN.

### Option C: Vercel
1. Install the Vercel CLI (or deploy via GitHub integration):
   ```bash
   npm i -g vercel
   cd website
   vercel
   ```
2. Follow the prompts. Vercel will detect it as a static project and deploy it immediately.

---

## 🎨 Design Rules & Brand Enforcement
- **Tricolor Gov Palette**:
  - Primary: Deep Navy Blue (`#082B5B`)
  - Accent: Saffron (`#F4A62A`, text `#92400E`)
  - Trust: Soft Mint (`#DDF3E8`, text `#166534`)
  - Live Verified Highlight: `#00C853`
  - Canvas: Ivory White (`#FAF9F6`)
- **Zero Symbols & Emojis**: The user interface does not use decorative icons, pictograms, glyphs, or emojis. All indicators use clean typography, numbers, and structured cards.
- **Accessibility**: Meets WCAG AA contrast standards, keyboard navigable accordions, semantic HTML, and minimum 44px tap targets for mobile visitors.
