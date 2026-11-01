/**
 * Cybridge Marketing Website Configuration
 * Update these values when releasing a new APK build.
 */
const CYBRIDGE_CONFIG = {
  // App Identifiers
  appName: "Cybridge",
  tagline: "Cyber Victim Assistant",
  packageName: "com.cybervictim.assistant",
  
  // Download Information
  downloadUrl: "downloads/cybridge.apk",
  version: "0.1.0",
  versionCode: 1,
  fileSize: "55 MB",
  minAndroid: "Android 8.0+ (API 26+)",
  lastUpdated: "November 1, 2026",
  sha256Checksum: "dee1d290f9366e28512e2cb188fbaa18becf90b7eb1d69252bf5f6ef55d5a664",
  
  // External Official References
  liveWebsiteUrl: "https://cybridge.vijjadapremsaiofficial.workers.dev/",
  helplineNumber: "1930",
  helplineTel: "tel:1930",
  portalUrl: "https://cybercrime.gov.in",
  githubRepoUrl: "https://github.com/vijjada/cyber-victim-assistant",
  
  // Contact & Support
  contactEmail: "vijjadapremsaiofficial@gmail.com",
  leadDeveloper: "Vijjada Prem Sai",
  institution: "GD Goenka University, Gurugram"
};

// Freeze configuration to prevent runtime mutation
Object.freeze(CYBRIDGE_CONFIG);
