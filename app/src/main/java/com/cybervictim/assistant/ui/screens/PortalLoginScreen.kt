package com.cybervictim.assistant.ui.screens

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.view.ViewGroup
import android.webkit.*
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.navigation.NavHostController
import com.cybervictim.assistant.data.UserProfile
import com.cybervictim.assistant.data.UserProfileStore
import com.cybervictim.assistant.ui.Routes
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// Official App Palette
// | Colour          | HEX code  | Usage                                     |
// | Deep Navy Blue  | #082B5B   | Primary buttons, headers, navigation       |
// | Royal Blue      | #145DA0   | Active tabs, links, progress indicators   |
// | Ivory White     | #FAF9F6   | Main background                           |
// | White           | #FFFFFF   | Forms, cards, input fields                |
// | Ash Grey        | #E5E7EB   | Borders, dividers, inactive elements      |
// | Saffron Gold    | #F4A62A   | Important actions and highlights          |
// | Soft Mint       | #DDF3E8   | Success messages and confirmation cards   |
// | Charcoal        | #263238   | Main text                                 |
private val DeepNavyBlue = Color(0xFF082B5B) // Primary buttons, headers, navigation
private val RoyalBlue = Color(0xFF145DA0)    // Active tabs, links, progress indicators
private val IvoryWhite = Color(0xFFFAF9F6)   // Main background
private val CardWhite = Color(0xFFFFFFFF)    // Forms, cards, input fields
private val AshGrey = Color(0xFFE5E7EB)      // Borders, dividers, inactive elements
private val SaffronGold = Color(0xFFF4A62A)  // Important actions and highlights
private val SoftMint = Color(0xFFDDF3E8)     // Success messages and confirmation cards
private val Charcoal = Color(0xFF263238)     // Main text
private val SubtitleCharcoal = Color(0xFF546E7A) // Supporting text

private val PortalHeaderBg = DeepNavyBlue
private val PortalCardHeaderBg = SoftMint
private val PortalCardHeaderBorder = AshGrey
private val PortalCardHeaderTitle = DeepNavyBlue
private val BgColor = IvoryWhite
private val DarkText = Charcoal
private val SubtitleGray = SubtitleCharcoal
private val BorderColor = AshGrey
private val GovOrange = SaffronGold
private val GovGreen = DeepNavyBlue
private val RedAsterisk = Color(0xFFDC2626)
private val PinkNoticeBg = Color(0xFFFEE2E2)
private val PinkNoticeBorder = Color(0xFFFECACA)
private val PinkNoticeText = Color(0xFF991B1B)

private val INDIAN_STATES = listOf(
    "ANDHRA PRADESH", "ARUNACHAL PRADESH", "ASSAM", "BIHAR", "CHHATTISGARH",
    "DELHI", "GOA", "GUJARAT", "HARYANA", "HIMACHAL PRADESH",
    "JHARKHAND", "KARNATAKA", "KERALA", "MADHYA PRADESH", "MAHARASHTRA",
    "MANIPUR", "MEGHALAYA", "MIZORAM", "NAGALAND", "ODISHA",
    "PUNJAB", "RAJASTHAN", "SIKKIM", "TAMIL NADU", "TELANGANA",
    "TRIPURA", "UTTAR PRADESH", "UTTARAKHAND", "WEST BENGAL",
    "ANDAMAN AND NICOBAR ISLANDS", "CHANDIGARH", "LADAKH", "JAMMU AND KASHMIR",
    "PUDUCHERRY"
)

private val GENDERS = listOf("Male", "Female", "Transgender")

/**
 * Slide 2: 100% Exact One-to-One Clone of Cybercrime Portal Update User Profile
 * Official URL: https://cybercrime.gov.in/dashboard/update-profile
 *
 * Requirements:
 * 1. Citizen login is completely removed from this screen.
 * 2. Directly places the Cyber Portal Update User Profile interface with exact 100% matching buttons
 *    matching the user's official portal screenshot:
 *    - Top header: National Emblem, I4C Logo, "National Cybercrime Reporting Portal"
 *    - Session timer ("59:49") and User ID ("7416002834")
 *    - Blue banner: "Update User Profile"
 *    - Exact input fields with red asterisks:
 *      * Name*
 *      * Mobile No *
 *      * Gender *
 *      * DOB *
 *      * Email Id *
 *      * State *
 *      * District *
 *      * Police Station
 *      * Address *Minimum length: 5 characters, Maximum length: 190 characters
 *      * Pin code *
 *    - Exact pink warning box
 *    - Exact green [Update] button on the right
 * 3. AI Operation: On successful update/login from the user, the AI agent automatically
 *    generates a Unique Victim ID (e.g. VIC-2026-XXXXX) with all user details mentioned
 *    in the cyber portal for app user identity (done by AI only, not user), and stores it
 *    into the user profile in the main menu.
 * 4. Advances to PortalCloneScreen (1:1 complaint report buttons).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PortalLoginScreen(nav: NavHostController) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val userProfile = remember { UserProfileStore.get(context) }

    // Session Timer State (Counting down from 59:49)
    var sessionSecondsRemaining by remember { mutableIntStateOf(59 * 60 + 49) }
    LaunchedEffect(Unit) {
        while (sessionSecondsRemaining > 0) {
            delay(1000)
            sessionSecondsRemaining--
        }
    }
    val sessionTimerFormatted = remember(sessionSecondsRemaining) {
        val m = sessionSecondsRemaining / 60
        val s = sessionSecondsRemaining % 60
        "%02d:%02d".format(m, s)
    }

    // ── Form State (Exact fields from cybercrime.gov.in/dashboard/update-profile) ──
    var name by remember {
        mutableStateOf(userProfile.fullName.ifBlank { "Vijjada Prem sai" })
    }
    var mobileNo by remember {
        mutableStateOf(userProfile.mobile.ifBlank { "7416002834" })
    }
    var selectedGender by remember {
        mutableStateOf(userProfile.gender.ifBlank { "Male" })
    }
    var genderDropdownExpanded by remember { mutableStateOf(false) }

    var dob by remember {
        mutableStateOf(userProfile.dob.ifBlank { "10/08/2005" })
    }
    var emailId by remember {
        mutableStateOf(userProfile.email.ifBlank { "vijjadapremsaiofficial@gmail.com" })
    }
    var selectedState by remember {
        mutableStateOf(userProfile.state.ifBlank { "ANDHRA PRADESH" })
    }
    var stateDropdownExpanded by remember { mutableStateOf(false) }

    var district by remember {
        mutableStateOf(userProfile.district.ifBlank { "NTR Commissionerate" })
    }
    var policeStation by remember {
        mutableStateOf(userProfile.policeStation.ifBlank { "AJITH SINGH NAGAR" })
    }
    var address by remember {
        mutableStateOf(userProfile.address.ifBlank { "One town vijayawada" })
    }
    var pinCode by remember {
        mutableStateOf(userProfile.pincode.ifBlank { "520001" })
    }

    var isUpdating by remember { mutableStateOf(false) }
    var showAiIdentitySuccessDialog by remember { mutableStateOf(false) }
    var generatedAiVictimId by remember { mutableStateOf(userProfile.victimId) }

    // Tab state: 0 = Portal Clone Update Profile, 1 = Live Embedded Webview
    var selectedTab by remember { mutableIntStateOf(0) }
    var portalWebView by remember { mutableStateOf<WebView?>(null) }
    var isWebLoading by remember { mutableStateOf(true) }

    // Back button handling
    BackHandler {
        if (selectedTab == 1) {
            selectedTab = 0
        } else {
            nav.popBackStack()
        }
    }

    // ── AI Operation: Automatically Generate Unique Victim App Identity ──
    fun executeAiIdentityGenerationAndSave(method: String) {
        val aiId = if (userProfile.victimId.isNotBlank()) {
            userProfile.victimId
        } else {
            UserProfileStore.generateAiVictimId()
        }
        generatedAiVictimId = aiId

        // Save all details mentioned in the cyber portal along with AI-generated identity
        val updated = userProfile.copy(
            victimId = aiId,
            fullName = name.trim(),
            mobile = mobileNo.trim(),
            gender = selectedGender.trim(),
            dob = dob.trim(),
            email = emailId.trim(),
            state = selectedState.trim(),
            district = district.trim(),
            policeStation = policeStation.trim(),
            address = address.trim(),
            pincode = pinCode.trim()
        )
        UserProfileStore.save(context, updated)

        // Sync with background webview if loaded
        portalWebView?.evaluateJavascript(
            """
            (function() {
                var n = document.querySelector('input[name*="name" i]'); if(n) { n.value = '${name.trim()}'; n.dispatchEvent(new Event('input', {bubbles: true})); }
                var m = document.querySelector('input[name*="mobile" i]'); if(m) { m.value = '${mobileNo.trim()}'; m.dispatchEvent(new Event('input', {bubbles: true})); }
                var e = document.querySelector('input[name*="email" i]'); if(e) { e.value = '${emailId.trim()}'; e.dispatchEvent(new Event('input', {bubbles: true})); }
            })();
            """.trimIndent(),
            null
        )

        showAiIdentitySuccessDialog = true
    }

    // AI Generated Identity Confirmation Dialog
    if (showAiIdentitySuccessDialog) {
        AlertDialog(
            onDismissRequest = {
                showAiIdentitySuccessDialog = false
                // After Update → redirect to official portal WebView for login
                nav.navigate(Routes.PORTAL_WEBVIEW) {
                    popUpTo(Routes.PORTAL_LOGIN) { inclusive = true }
                }
            },
            title = {
                Column {
                    Text(
                        "App User Identity Generated",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = DarkText
                    )
                    Text(
                        "Generated automatically",
                        fontSize = 11.sp,
                        color = SubtitleGray
                    )
                }
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Highlighted ID Card
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFFEFF6FF),
                        border = BorderStroke(1.dp, Color(0xFFBFDBFE)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                "UNIQUE VICTIM IDENTIFIER",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1D4ED8),
                                letterSpacing = 1.sp
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = generatedAiVictimId,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.ExtraBold,
                                fontFamily = FontFamily.Monospace,
                                color = Color(0xFF1E3A8A)
                            )
                        }
                    }

                    Text(
                        "All details saved! Next step: Login to the official Cybercrime Portal to authenticate your identity, then proceed to report your cyber crime.",
                        fontSize = 12.sp,
                        color = SubtitleGray,
                        lineHeight = 16.sp
                    )

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFFF9FAFB),
                        border = BorderStroke(1.dp, BorderColor),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text("• Complainant: $name", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                            Text("• Mobile: +91 $mobileNo", fontSize = 12.sp)
                            Text("• State / District: $selectedState / $district", fontSize = 12.sp)
                            Text("• Police Station: $policeStation ($pinCode)", fontSize = 12.sp)
                        }
                    }

                    // Info notice about next step
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFFFEF3C7),
                        border = BorderStroke(1.dp, Color(0xFFFDE68A)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(modifier = Modifier.padding(12.dp)) {
                            Text(
                                "You will now be redirected to the official cybercrime.gov.in portal to login. After successful login, proceed to report your cyber crime.",
                                fontSize = 11.sp,
                                lineHeight = 15.sp,
                                color = Color(0xFF92400E)
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showAiIdentitySuccessDialog = false
                        Toast.makeText(
                            context,
                            "Victim ID: $generatedAiVictimId active! Opening Official Portal for Login...",
                            Toast.LENGTH_SHORT
                        ).show()
                        // Navigate to official portal WebView for login
                        nav.navigate(Routes.PORTAL_WEBVIEW) {
                            popUpTo(Routes.PORTAL_LOGIN) { inclusive = true }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = GovGreen)
                ) {
                    Text("Login to Official Portal →", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAiIdentitySuccessDialog = false }) {
                    Text("Stay on this page")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            Surface(
                color = Color.White,
                shadowElevation = 2.dp,
                border = BorderStroke(0.5.dp, BorderColor)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                ) {
                    // Top Official Header Bar
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = { nav.popBackStack() },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = DarkText
                            )
                        }
                        Spacer(Modifier.width(4.dp))
                        // Hamburger menu icon matching screenshot
                        Icon(
                            Icons.Default.Menu,
                            contentDescription = null,
                            tint = DarkText,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        // Emblem & Title
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                // Mini Flag
                                Box(
                                    modifier = Modifier
                                        .size(16.dp, 10.dp)
                                        .clip(RoundedCornerShape(2.dp))
                                        .background(GovOrange)
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    "National Cybercrime Reporting Portal",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = DarkText,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Text(
                                "राष्ट्रीय साइबर अपराध रिपोर्टिंग पोर्टल • I4C, MHA",
                                fontSize = 10.sp,
                                color = SubtitleGray,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    // Session Info Strip (Exact layout from screenshot: User ID & Session Countdown)
                    Surface(
                        color = Color(0xFF0F3D34),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    "User ID: ",
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    mobileNo,
                                    color = Color(0xFFFF9933), // Orange text as in portal
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    "Session Expires In: ",
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    sessionTimerFormatted,
                                    color = Color(0xFFFDE047), // Yellow countdown as in portal
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    // View Switcher: Portal Clone vs Live Web (Royal Blue active tabs)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(IvoryWhite)
                            .padding(4.dp)
                    ) {
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clickable { selectedTab = 0 },
                            shape = RoundedCornerShape(6.dp),
                            color = if (selectedTab == 0) CardWhite else Color.Transparent,
                            border = if (selectedTab == 0) BorderStroke(1.5.dp, RoyalBlue) else null
                        ) {
                            Text(
                                text = "Update Profile (Clone)",
                                modifier = Modifier.padding(vertical = 6.dp),
                                textAlign = TextAlign.Center,
                                fontSize = 11.sp,
                                fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Medium,
                                color = if (selectedTab == 0) RoyalBlue else SubtitleGray
                            )
                        }

                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clickable { selectedTab = 1 },
                            shape = RoundedCornerShape(6.dp),
                            color = if (selectedTab == 1) CardWhite else Color.Transparent,
                            border = if (selectedTab == 1) BorderStroke(1.5.dp, RoyalBlue) else null
                        ) {
                            Text(
                                text = "Official Webview (Live)",
                                modifier = Modifier.padding(vertical = 6.dp),
                                textAlign = TextAlign.Center,
                                fontSize = 11.sp,
                                fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Medium,
                                color = if (selectedTab == 1) RoyalBlue else SubtitleGray
                            )
                        }
                    }
                }
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(BgColor)
        ) {
            when (selectedTab) {
                0 -> {
                    // ── TAB 0: 100% EXACT REPLICA OF https://cybercrime.gov.in/dashboard/update-profile ──
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        // Main Update User Profile Card
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color.White,
                            border = BorderStroke(1.dp, BorderColor),
                            shadowElevation = 2.dp,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                // 1. Header Banner matching screenshot: Light blue with dark blue text
                                Surface(
                                    color = PortalCardHeaderBg,
                                    border = BorderStroke(1.dp, PortalCardHeaderBorder),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = "Update User Profile",
                                        color = PortalCardHeaderTitle,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                                    )
                                }

                                // 2. Form Fields Container
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(14.dp)
                                ) {
                                    // Row 1: Name* & Mobile No *
                                    Column {
                                        Text(
                                            text = buildAnnotatedString {
                                                append("Name")
                                                withStyle(SpanStyle(color = RedAsterisk, fontWeight = FontWeight.Bold)) {
                                                    append("*")
                                                }
                                            },
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = DarkText
                                        )
                                        Spacer(Modifier.height(4.dp))
                                        OutlinedTextField(
                                            value = name,
                                            onValueChange = { name = it },
                                            modifier = Modifier.fillMaxWidth(),
                                            singleLine = true,
                                            shape = RoundedCornerShape(4.dp)
                                        )
                                    }

                                    Column {
                                        Text(
                                            text = buildAnnotatedString {
                                                append("Mobile No ")
                                                withStyle(SpanStyle(color = RedAsterisk, fontWeight = FontWeight.Bold)) {
                                                    append("*")
                                                }
                                            },
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = DarkText
                                        )
                                        Spacer(Modifier.height(4.dp))
                                        OutlinedTextField(
                                            value = mobileNo,
                                            onValueChange = {
                                                if (it.length <= 10 && it.all { ch -> ch.isDigit() }) {
                                                    mobileNo = it
                                                }
                                            },
                                            modifier = Modifier.fillMaxWidth(),
                                            singleLine = true,
                                            shape = RoundedCornerShape(4.dp),
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                            colors = OutlinedTextFieldDefaults.colors(
                                                unfocusedContainerColor = Color(0xFFF3F4F6) // Gray container as authenticated in portal
                                            )
                                        )
                                    }

                                    // Row 2: Gender * & DOB *
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        // Gender *
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = buildAnnotatedString {
                                                    append("Gender ")
                                                    withStyle(SpanStyle(color = RedAsterisk, fontWeight = FontWeight.Bold)) {
                                                        append("*")
                                                    }
                                                },
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = DarkText
                                            )
                                            Spacer(Modifier.height(4.dp))
                                            Box {
                                                OutlinedCard(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .clickable { genderDropdownExpanded = true },
                                                    shape = RoundedCornerShape(4.dp),
                                                    border = BorderStroke(1.dp, BorderColor)
                                                ) {
                                                    Row(
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .padding(horizontal = 12.dp, vertical = 14.dp),
                                                        horizontalArrangement = Arrangement.SpaceBetween,
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Text(selectedGender, fontSize = 13.sp, color = DarkText)
                                                        Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = SubtitleGray)
                                                    }
                                                }

                                                DropdownMenu(
                                                    expanded = genderDropdownExpanded,
                                                    onDismissRequest = { genderDropdownExpanded = false }
                                                ) {
                                                    GENDERS.forEach { g ->
                                                        DropdownMenuItem(
                                                            text = { Text(g, fontSize = 13.sp) },
                                                            onClick = {
                                                                selectedGender = g
                                                                genderDropdownExpanded = false
                                                            }
                                                        )
                                                    }
                                                }
                                            }
                                        }

                                        // DOB *
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = buildAnnotatedString {
                                                    append("DOB ")
                                                    withStyle(SpanStyle(color = RedAsterisk, fontWeight = FontWeight.Bold)) {
                                                        append("*")
                                                    }
                                                },
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = DarkText
                                            )
                                            Spacer(Modifier.height(4.dp))
                                            OutlinedTextField(
                                                value = dob,
                                                onValueChange = { dob = it },
                                                modifier = Modifier.fillMaxWidth(),
                                                placeholder = { Text("DD/MM/YYYY") },
                                                trailingIcon = {
                                                    Icon(Icons.Default.DateRange, contentDescription = "Calendar", tint = SubtitleGray)
                                                },
                                                singleLine = true,
                                                shape = RoundedCornerShape(4.dp)
                                            )
                                        }
                                    }

                                    // Row 3: Email Id *
                                    Column {
                                        Text(
                                            text = buildAnnotatedString {
                                                append("Email Id ")
                                                withStyle(SpanStyle(color = RedAsterisk, fontWeight = FontWeight.Bold)) {
                                                    append("*")
                                                }
                                            },
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = DarkText
                                        )
                                        Spacer(Modifier.height(4.dp))
                                        OutlinedTextField(
                                            value = emailId,
                                            onValueChange = { emailId = it },
                                            modifier = Modifier.fillMaxWidth(),
                                            singleLine = true,
                                            shape = RoundedCornerShape(4.dp),
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
                                        )
                                    }

                                    // Row 4: State * Dropdown
                                    Column {
                                        Text(
                                            text = buildAnnotatedString {
                                                append("State ")
                                                withStyle(SpanStyle(color = RedAsterisk, fontWeight = FontWeight.Bold)) {
                                                    append("*")
                                                }
                                            },
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = DarkText
                                        )
                                        Spacer(Modifier.height(4.dp))
                                        Box {
                                            OutlinedCard(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clickable { stateDropdownExpanded = true },
                                                shape = RoundedCornerShape(4.dp),
                                                border = BorderStroke(1.dp, BorderColor)
                                            ) {
                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(horizontal = 12.dp, vertical = 14.dp),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Text(selectedState, fontSize = 13.sp, color = DarkText)
                                                    Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = SubtitleGray)
                                                }
                                            }

                                            DropdownMenu(
                                                expanded = stateDropdownExpanded,
                                                onDismissRequest = { stateDropdownExpanded = false },
                                                modifier = Modifier.heightIn(max = 280.dp)
                                            ) {
                                                INDIAN_STATES.forEach { st ->
                                                    DropdownMenuItem(
                                                        text = { Text(st, fontSize = 13.sp) },
                                                        onClick = {
                                                            selectedState = st
                                                            stateDropdownExpanded = false
                                                        }
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    // Row 5: District * & Police Station
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        // District *
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = buildAnnotatedString {
                                                    append("District ")
                                                    withStyle(SpanStyle(color = RedAsterisk, fontWeight = FontWeight.Bold)) {
                                                        append("*")
                                                    }
                                                },
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = DarkText
                                            )
                                            Spacer(Modifier.height(4.dp))
                                            OutlinedTextField(
                                                value = district,
                                                onValueChange = { district = it },
                                                modifier = Modifier.fillMaxWidth(),
                                                singleLine = true,
                                                shape = RoundedCornerShape(4.dp)
                                            )
                                        }

                                        // Police Station (no asterisk as in portal)
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = "Police Station",
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = DarkText
                                            )
                                            Spacer(Modifier.height(4.dp))
                                            OutlinedTextField(
                                                value = policeStation,
                                                onValueChange = { policeStation = it },
                                                modifier = Modifier.fillMaxWidth(),
                                                singleLine = true,
                                                shape = RoundedCornerShape(4.dp)
                                            )
                                        }
                                    }

                                    // Row 6: Address *Minimum length: 5 characters, Maximum length: 190 characters
                                    Column {
                                        Text(
                                            text = buildAnnotatedString {
                                                append("Address ")
                                                withStyle(SpanStyle(color = RedAsterisk, fontWeight = FontWeight.Bold)) {
                                                    append("*")
                                                }
                                                append("Minimum length: 5 characters, Maximum length: 190 characters")
                                            },
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = DarkText
                                        )
                                        Spacer(Modifier.height(4.dp))
                                        OutlinedTextField(
                                            value = address,
                                            onValueChange = {
                                                if (it.length <= 190) address = it
                                            },
                                            modifier = Modifier.fillMaxWidth(),
                                            singleLine = true,
                                            shape = RoundedCornerShape(4.dp)
                                        )
                                    }

                                    // Row 7: Pin code *
                                    Column {
                                        Text(
                                            text = buildAnnotatedString {
                                                append("Pin code ")
                                                withStyle(SpanStyle(color = RedAsterisk, fontWeight = FontWeight.Bold)) {
                                                    append("*")
                                                }
                                            },
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = DarkText
                                        )
                                        Spacer(Modifier.height(4.dp))
                                        OutlinedTextField(
                                            value = pinCode,
                                            onValueChange = {
                                                if (it.length <= 6 && it.all { ch -> ch.isDigit() }) {
                                                    pinCode = it
                                                }
                                            },
                                            modifier = Modifier.fillMaxWidth(),
                                            singleLine = true,
                                            shape = RoundedCornerShape(4.dp),
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                                        )
                                    }

                                    // 3. Exact Pink Notice Box from Screenshot
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = PinkNoticeBg,
                                        border = BorderStroke(1.dp, PinkNoticeBorder),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            text = "(AlphaNumeric and Symbols like @, ., (), /, _, -, :, ; are allowed. Do not use any special characters.)",
                                            fontSize = 12.sp,
                                            color = PinkNoticeText,
                                            lineHeight = 16.sp,
                                            modifier = Modifier.padding(12.dp)
                                        )
                                    }

                                    Spacer(Modifier.height(8.dp))

                                    // 4. Exact Official [Update] Button from screenshot (Right-aligned green button)
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.End
                                    ) {
                                        Button(
                                            onClick = {
                                                if (name.trim().isBlank()) {
                                                    Toast.makeText(context, "Please enter your Name", Toast.LENGTH_SHORT).show()
                                                    return@Button
                                                }
                                                if (mobileNo.trim().length != 10) {
                                                    Toast.makeText(context, "Please enter 10-digit Mobile Number", Toast.LENGTH_SHORT).show()
                                                    return@Button
                                                }
                                                if (address.trim().length < 5) {
                                                    Toast.makeText(context, "Address must be minimum 5 characters", Toast.LENGTH_SHORT).show()
                                                    return@Button
                                                }

                                                isUpdating = true
                                                coroutineScope.launch {
                                                    delay(400)
                                                    isUpdating = false
                                                    executeAiIdentityGenerationAndSave("Update Button")
                                                }
                                            },
                                            shape = RoundedCornerShape(4.dp),
                                            colors = ButtonDefaults.buttonColors(containerColor = GovGreen),
                                            modifier = Modifier.height(42.dp)
                                        ) {
                                            if (isUpdating) {
                                                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(18.dp))
                                            } else {
                                                Text(
                                                    text = "Update",
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 14.sp,
                                                    color = Color.White
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // Primary Action: Update & Login to Official Portal
                        Button(
                            onClick = {
                                executeAiIdentityGenerationAndSave("Proceed Action")
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = PortalHeaderBg)
                        ) {
                            Text(
                                text = "Update & Login to Official Portal →",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = Color.White
                            )
                        }

                        // Flow Notice Box
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFFEFF6FF),
                            border = BorderStroke(1.dp, Color(0xFFBFDBFE)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp)
                            ) {
                                Text(
                                    "AI Identity + Official Portal Login",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = Color(0xFF1E40AF)
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    "Step 1: AI generates your Unique Victim ID (VIC-2026-XXXXX)\nStep 2: Login to the official cybercrime.gov.in portal\nStep 3: Proceed to Report Cyber Crime",
                                    fontSize = 11.sp,
                                    lineHeight = 15.sp,
                                    color = Color(0xFF1E3A8A)
                                )
                            }
                        }
                    }
                }

                1 -> {
                    // ── TAB 1: EMBEDDED OFFICIAL LIVE PORTAL WEBVIEW ──
                    Box(modifier = Modifier.fillMaxSize()) {
                        AndroidView(
                            modifier = Modifier.fillMaxSize(),
                            factory = { ctx ->
                                WebView(ctx).apply {
                                    layoutParams = ViewGroup.LayoutParams(
                                        ViewGroup.LayoutParams.MATCH_PARENT,
                                        ViewGroup.LayoutParams.MATCH_PARENT
                                    )
                                    settings.apply {
                                        javaScriptEnabled = true
                                        domStorageEnabled = true
                                        useWideViewPort = true
                                        loadWithOverviewMode = true
                                        userAgentString = "Mozilla/5.0 (Linux; Android 13; Pixel 7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.6367.82 Mobile Safari/537.36"
                                    }
                                    webViewClient = object : WebViewClient() {
                                        override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                                            isWebLoading = true
                                        }

                                        override fun onPageFinished(view: WebView?, url: String?) {
                                            isWebLoading = false
                                        }
                                    }
                                    loadUrl("https://cybercrime.gov.in/login")
                                    portalWebView = this
                                }
                            }
                        )

                        if (isWebLoading) {
                            LinearProgressIndicator(
                                modifier = Modifier.fillMaxWidth().align(Alignment.TopCenter),
                                color = GovGreen
                            )
                        }

                        // Floating action bar
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .align(Alignment.BottomCenter)
                                .navigationBarsPadding()
                                .padding(12.dp),
                            shape = RoundedCornerShape(12.dp),
                            color = Color.White,
                            shadowElevation = 8.dp,
                            border = BorderStroke(1.dp, BorderColor)
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Viewing Live Web Portal", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    Text("Tap to generate Victim ID and report", fontSize = 11.sp, color = SubtitleGray)
                                }
                                Button(
                                    onClick = { executeAiIdentityGenerationAndSave("Live Webview Confirmed") },
                                    colors = ButtonDefaults.buttonColors(containerColor = GovGreen),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text("Continue →", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
