package com.cybervictim.assistant.ui.screens

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.os.Handler
import android.os.Looper
import android.view.ViewGroup
import android.webkit.*
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.navigation.NavHostController
import com.cybervictim.assistant.portal.StepOwner
import com.cybervictim.assistant.ui.Routes
import com.cybervictim.assistant.voice.ScreenDescriptor
import com.cybervictim.assistant.voice.TrackScreenDescriptor
import kotlinx.coroutines.delay

/**
 * JavaScript Interface Bridge to receive instantaneous callbacks from inside the official portal WebView.
 */
class PortalAuthBridge(private val onLoginSuccess: (String) -> Unit) {
    @JavascriptInterface
    fun onLoginSuccess(reason: String) {
        android.util.Log.i("PortalAuthBridge", "Login success detected: $reason")
        Handler(Looper.getMainLooper()).post {
            onLoginSuccess(reason)
        }
    }
}

/**
 * Official Cybercrime Portal Login WebView Screen.
 *
 * Strict Requirements:
 * 1. The user logs in to the official portal (cybercrime.gov.in/login) with OTP and Captcha.
 * 2. All "Go" buttons are completely removed.
 * 3. Immediately upon login button click / successful authentication, the user is automatically
 *    redirected to our app (Routes.PORTAL_CLONE) to register the cyber complaint.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PortalWebViewScreen(nav: NavHostController) {
    TrackScreenDescriptor(
        ScreenDescriptor(
            screenId = "portal_webview",
            title = "Official Portal Authentication",
            buttons = listOf("Submit", "Refresh", "Back"),
            fields = listOf("OTP", "Captcha"),
            owner = StepOwner.HUMAN,
            hintKey = "voice_hint_portal_webview"
        )
    )
    val ctx = LocalContext.current

    // ── State ──
    var webView by remember { mutableStateOf<WebView?>(null) }
    var pageUrl by remember { mutableStateOf("Loading…") }
    var pageTitle by remember { mutableStateOf("Cybercrime Portal — Official Login") }
    var isLoading by remember { mutableStateOf(true) }
    var canGoBack by remember { mutableStateOf(false) }

    // Redirect & Login State
    var hasRedirectedToApp by remember { mutableStateOf(false) }
    var isLoggedIn by remember { mutableStateOf(false) }

    // ── Strict Redirection to Mobile Application ──
    fun executeStrictRedirectToApp(reason: String) {
        if (hasRedirectedToApp) return
        hasRedirectedToApp = true
        isLoggedIn = true

        android.util.Log.i("PortalWebView", "Executing strict redirect to app. Reason: $reason")

        // Flush cookies so background WebView in PortalCloneScreen has the live authenticated session
        CookieManager.getInstance().flush()

        android.widget.Toast.makeText(
            ctx,
            "Login Successful! Redirecting to App to register complaint...",
            android.widget.Toast.LENGTH_SHORT
        ).show()

        nav.navigate(Routes.PORTAL_CLONE) {
            popUpTo(Routes.PORTAL_WEBVIEW) { inclusive = true }
        }
    }

    // Register JS Interface Bridge
    val authBridge = remember {
        PortalAuthBridge { reason ->
            executeStrictRedirectToApp(reason)
        }
    }

    // ── JavaScript Injection Payload for Real-Time Login Detection ──
    val authScript = """
        (function() {
            if (window._cybridgeAuthInjected) return;
            window._cybridgeAuthInjected = true;

            function checkLoginState(source) {
                var href = (window.location.href || '').toLowerCase();
                var pathname = (window.location.pathname || '').toLowerCase();

                // 1. URL Navigation away from /login (Angular client-side routing)
                var onLoginPage = href.indexOf('/login') >= 0 && href.indexOf('dashboard') === -1 && href.indexOf('crimereport') === -1;
                if (!onLoginPage && (
                    href.indexOf('dashboard') >= 0 ||
                    href.indexOf('crimereport') >= 0 ||
                    href.indexOf('update-profile') >= 0 ||
                    href.indexOf('citizen') >= 0 ||
                    href.indexOf('home') >= 0 ||
                    href.indexOf('report') >= 0 ||
                    (pathname.length > 1 && pathname.indexOf('/login') === -1)
                )) {
                    if (window.CybridgeAuth) {
                        window.CybridgeAuth.onLoginSuccess('URL change: ' + href + ' via ' + source);
                        return true;
                    }
                }

                // 2. DOM text check for authenticated indicators
                var bodyText = (document.body ? (document.body.innerText || '') : '').toLowerCase();
                if (bodyText.indexOf('logout') >= 0 ||
                    bodyText.indexOf('sign out') >= 0 ||
                    bodyText.indexOf('user id:') >= 0 ||
                    bodyText.indexOf('welcome,') >= 0 ||
                    bodyText.indexOf('incident details') >= 0 ||
                    bodyText.indexOf('complainant details') >= 0) {
                    if (window.CybridgeAuth) {
                        window.CybridgeAuth.onLoginSuccess('DOM text match via ' + source);
                        return true;
                    }
                }
                return false;
            }

            // A. Intercept Angular SPA Router history events
            var origPush = history.pushState;
            history.pushState = function() {
                origPush.apply(this, arguments);
                setTimeout(function() { checkLoginState('history.pushState'); }, 150);
                setTimeout(function() { checkLoginState('history.pushState-delay'); }, 600);
            };

            var origReplace = history.replaceState;
            history.replaceState = function() {
                origReplace.apply(this, arguments);
                setTimeout(function() { checkLoginState('history.replaceState'); }, 150);
                setTimeout(function() { checkLoginState('history.replaceState-delay'); }, 600);
            };

            window.addEventListener('popstate', function() {
                setTimeout(function() { checkLoginState('popstate'); }, 150);
            });
            window.addEventListener('hashchange', function() {
                setTimeout(function() { checkLoginState('hashchange'); }, 150);
            });

            // B. Intercept click on Login / Submit / Sign In buttons
            document.addEventListener('click', function(e) {
                var el = e.target;
                var btn = el ? el.closest('button, input[type="submit"], input[type="button"], a') : null;
                if (!btn) return;
                var txt = (btn.textContent || btn.innerText || btn.value || '').trim().toLowerCase();
                var id = (btn.id || '').toLowerCase();
                var cls = (btn.className || '').toLowerCase();

                if (txt === 'login' || txt.indexOf('login') >= 0 || txt.indexOf('sign in') >= 0 || id.indexOf('login') >= 0 || cls.indexOf('login') >= 0) {
                    // User clicked Login! Poll rapidly for authentication completion
                    var count = 0;
                    var pollTimer = setInterval(function() {
                        count++;
                        if (checkLoginState('post-login-click-attempt-' + count) || count > 30) {
                            clearInterval(pollTimer);
                        }
                    }, 250);
                }
            }, true);

            // C. Intercept fetch responses for login endpoints
            if (window.fetch) {
                var origFetch = window.fetch;
                window.fetch = function() {
                    var u = arguments[0];
                    var urlStr = (typeof u === 'string') ? u : (u && u.url ? u.url : '');
                    return origFetch.apply(this, arguments).then(function(res) {
                        try {
                            if (res.ok && urlStr && (urlStr.indexOf('login') >= 0 || urlStr.indexOf('auth') >= 0 || urlStr.indexOf('verify') >= 0 || urlStr.indexOf('otp') >= 0)) {
                                setTimeout(function() { checkLoginState('fetch-login-200'); }, 300);
                                setTimeout(function() { checkLoginState('fetch-login-200-delay'); }, 1000);
                            }
                        } catch(e) {}
                        return res;
                    });
                };
            }

            // D. Intercept XHR responses for login endpoints
            var origXHROpen = window.XMLHttpRequest.prototype.open;
            window.XMLHttpRequest.prototype.open = function(method, url) {
                this._cybridgeUrl = url;
                return origXHROpen.apply(this, arguments);
            };
            var origXHRSend = window.XMLHttpRequest.prototype.send;
            window.XMLHttpRequest.prototype.send = function() {
                this.addEventListener('load', function() {
                    try {
                        if (this.status >= 200 && this.status < 300 && this._cybridgeUrl &&
                            (this._cybridgeUrl.indexOf('login') >= 0 || this._cybridgeUrl.indexOf('auth') >= 0 || this._cybridgeUrl.indexOf('verify') >= 0 || this._cybridgeUrl.indexOf('otp') >= 0)) {
                            setTimeout(function() { checkLoginState('xhr-login-200'); }, 300);
                            setTimeout(function() { checkLoginState('xhr-login-200-delay'); }, 1000);
                        }
                    } catch(e) {}
                });
                return origXHRSend.apply(this, arguments);
            };

            // E. MutationObserver
            var observer = new MutationObserver(function() {
                checkLoginState('MutationObserver');
            });
            if (document.body) {
                observer.observe(document.body, { childList: true, subtree: true });
            }

            // F. Periodic interval check
            setInterval(function() {
                checkLoginState('heartbeat');
            }, 500);
        })();
    """.trimIndent()

    // ── Native Android Coroutine Poller (Continuous Verification Loop) ──
    LaunchedEffect(webView) {
        val wv = webView ?: return@LaunchedEffect
        while (!hasRedirectedToApp) {
            delay(500)
            wv.post {
                if (hasRedirectedToApp) return@post
                val currentUrl = wv.url?.lowercase() ?: ""

                // 1. Native URL Check: Has navigated away from /login
                val onLoginPage = currentUrl.endsWith("/login") || currentUrl.endsWith("/login/") || currentUrl.contains("/login?")
                if (!onLoginPage && currentUrl.isNotBlank() && (
                    currentUrl.contains("dashboard") ||
                    currentUrl.contains("crimereport") ||
                    currentUrl.contains("update-profile") ||
                    currentUrl.contains("citizen") ||
                    currentUrl.contains("home") ||
                    currentUrl.contains("report")
                )) {
                    executeStrictRedirectToApp("Native URL Poller: $currentUrl")
                    return@post
                }

                // 2. Continuous Script Injection & DOM Check
                wv.evaluateJavascript(authScript, null)
                wv.evaluateJavascript(
                    """
                    (function() {
                        var h = (window.location.href || '').toLowerCase();
                        var p = (window.location.pathname || '').toLowerCase();
                        var onLog = h.indexOf('/login') >= 0 && h.indexOf('dashboard') === -1 && h.indexOf('crimereport') === -1;
                        if (!onLog && (h.indexOf('dashboard') >= 0 || h.indexOf('crimereport') >= 0 || (p.length > 1 && p.indexOf('/login') === -1))) {
                            return 'AUTH_URL';
                        }
                        var b = (document.body ? (document.body.innerText || '') : '').toLowerCase();
                        if (b.indexOf('logout') >= 0 || b.indexOf('sign out') >= 0 || b.indexOf('user id:') >= 0 || b.indexOf('welcome,') >= 0) {
                            return 'AUTH_DOM';
                        }
                        return 'WAITING';
                    })();
                    """.trimIndent()
                ) { result ->
                    val clean = result?.replace("\"", "")?.trim() ?: ""
                    if ((clean == "AUTH_URL" || clean == "AUTH_DOM") && !hasRedirectedToApp) {
                        executeStrictRedirectToApp("Native DOM Result: $clean")
                    }
                }
            }
        }
    }

    // ── Back handler for WebView ──
    BackHandler(enabled = canGoBack) {
        webView?.goBack()
    }

    // ── Main UI (Strict: NO 'Go' buttons anywhere) ──
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = if (isLoggedIn) "Logged In — Redirecting to App..." else "Official Portal Login",
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (isLoggedIn) "Redirecting to complaint registration..." else "Enter OTP & Captcha to authenticate",
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = {
                        if (canGoBack) webView?.goBack() else nav.popBackStack()
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { webView?.reload() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Reload")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = if (isLoggedIn) Color(0xFFDDF3E8) else Color(0xFFFAF9F6)
                )
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // ── WebView taking full screen, zero extra buttons ──
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { context ->
                    WebView(context).apply {
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT,
                        )
                        @SuppressLint("SetJavaScriptEnabled")
                        settings.javaScriptEnabled = true
                        settings.domStorageEnabled = true
                        settings.loadWithOverviewMode = true
                        settings.useWideViewPort = true
                        settings.builtInZoomControls = true
                        settings.displayZoomControls = false
                        settings.setSupportMultipleWindows(false)
                        settings.mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
                        settings.userAgentString = settings.userAgentString.replace(
                            "wv", ""
                        ) + " CyberVictimAssistant/1.0"

                        // Cookie support
                        val wv = this
                        CookieManager.getInstance().apply {
                            setAcceptCookie(true)
                            setAcceptThirdPartyCookies(wv, true)
                        }

                        // Attach the JavaScript Authentication Bridge
                        addJavascriptInterface(authBridge, "CybridgeAuth")

                        webViewClient = object : WebViewClient() {
                            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                                isLoading = true
                                pageUrl = url ?: ""

                                // Fast URL redirect check
                                val u = (url ?: "").lowercase()
                                val onLogin = u.endsWith("/login") || u.endsWith("/login/") || u.contains("/login?")
                                if (!onLogin && (u.contains("dashboard") || u.contains("crimereport") || u.contains("update-profile") || u.contains("citizen") || u.contains("home")) && !hasRedirectedToApp) {
                                    executeStrictRedirectToApp("onPageStarted URL match: $u")
                                }
                            }

                            override fun onPageFinished(view: WebView?, url: String?) {
                                isLoading = false
                                pageUrl = url ?: ""
                                pageTitle = view?.title ?: "Cybercrime Portal"
                                canGoBack = view?.canGoBack() ?: false

                                val currentUrl = (url ?: "").lowercase()
                                val onLogin = currentUrl.endsWith("/login") || currentUrl.endsWith("/login/") || currentUrl.contains("/login?")
                                if (!onLogin && (currentUrl.contains("dashboard") ||
                                    currentUrl.contains("crimereport") ||
                                    currentUrl.contains("update-profile") ||
                                    currentUrl.contains("citizen") ||
                                    currentUrl.contains("home")) && !hasRedirectedToApp) {
                                    executeStrictRedirectToApp("onPageFinished URL match: $currentUrl")
                                    return
                                }

                                // Inject authentication monitor script into the page
                                view?.evaluateJavascript(authScript, null)
                            }

                            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                                val reqUrl = request?.url?.toString() ?: return false
                                if (reqUrl.contains("cybercrime.gov.in")) return false
                                return true
                            }
                        }

                        webChromeClient = object : WebChromeClient() {
                            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                                if (newProgress >= 100) {
                                    isLoading = false
                                    view?.evaluateJavascript(authScript, null)
                                }
                            }
                        }

                        // Load official cybercrime login page
                        loadUrl("https://cybercrime.gov.in/login")
                        webView = this
                    }
                },
            )

            // Loading bar
            if (isLoading) {
                Box(
                    Modifier.fillMaxWidth().padding(top = 1.dp),
                    contentAlignment = Alignment.TopCenter,
                ) {
                    LinearProgressIndicator(Modifier.fillMaxWidth(), color = Color(0xFF145DA0))
                }
            }
        }
    }
}
