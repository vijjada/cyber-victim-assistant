package com.cybervictim.assistant.ui.screens

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.cybervictim.assistant.R
import com.cybervictim.assistant.data.UserProfile
import com.cybervictim.assistant.data.UserProfileStore
import com.cybervictim.assistant.trc.AppRisk
import com.cybervictim.assistant.trc.PhoneTrace
import com.cybervictim.assistant.ui.L10n
import com.cybervictim.assistant.ui.Routes
import kotlinx.coroutines.launch

// Official App Palette
private val DeepNavyBlue = Color(0xFF082B5B) // Deep Navy Blue: Primary buttons, headers, navigation
private val RoyalBlue = Color(0xFF145DA0)    // Royal Blue: Active tabs, links, progress indicators
private val BgColor = Color(0xFFFAF9F6)      // Ivory White: Main background
private val White = Color(0xFFFFFFFF)        // White: Forms, cards, input fields
private val BorderUnselected = Color(0xFFE5E7EB) // Ash Grey: Borders, dividers, inactive elements
private val SaffronGold = Color(0xFFF4A62A)  // Saffron Gold: Important actions and highlights
private val SoftMint = Color(0xFFDDF3E8)     // Soft Mint: Success messages and confirmation cards
private val DarkText = Color(0xFF263238)     // Charcoal: Main text
private val SubtitleGray = Color(0xFF546E7A) // Supporting text
private val TealPrimary = DeepNavyBlue       // Primary theme color
private val TealHalo = SoftMint              // Highlight / Success container

@Composable
fun LanguageSelectScreen(nav: NavHostController) {
    val context = LocalContext.current
    var selectedLang by remember { mutableStateOf(L10n.lang) }
    var speedProgress by remember { mutableFloatStateOf(0.35f) }

    // State for the menu dialogs
    var showUserDetailsDialog by remember { mutableStateOf(false) }
    var showComplaintStatusDialog by remember { mutableStateOf(false) }
    var showPhoneTraceDialog by remember { mutableStateOf(false) }
    var showMoreDetailsDialog by remember { mutableStateOf(false) }

    var userProfile by remember { mutableStateOf(UserProfileStore.get(context)) }

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        userProfile = UserProfileStore.get(context)
    }

    // Languages in order: English, Hindi, Telugu, Punjabi, Gujarati, Marathi, Tamil, Bangla
    val languages = remember {
        listOf(
            L10n.Lang.EN,
            L10n.Lang.HI,
            L10n.Lang.TE,
            L10n.Lang.PA,
            L10n.Lang.GU,
            L10n.Lang.MR,
            L10n.Lang.TA,
            L10n.Lang.BN,
        )
    }

    // --- 1. UPDATE THE PROFILE DIALOG ---
    if (showUserDetailsDialog) {
        val currentProfile = UserProfileStore.get(context)
        var name by remember { mutableStateOf(currentProfile.fullName) }
        var phone by remember { mutableStateOf(currentProfile.mobile) }
        var gender by remember { mutableStateOf(currentProfile.gender) }
        var dob by remember { mutableStateOf(currentProfile.dob) }
        var email by remember { mutableStateOf(currentProfile.email) }
        var state by remember { mutableStateOf(currentProfile.state) }
        var district by remember { mutableStateOf(currentProfile.district) }
        var policeStation by remember { mutableStateOf(currentProfile.policeStation) }
        var address by remember { mutableStateOf(currentProfile.address) }
        var pinCode by remember { mutableStateOf(currentProfile.pincode) }
        val victimId by remember { mutableStateOf(currentProfile.victimId) }

        AlertDialog(
            onDismissRequest = { showUserDetailsDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Person, contentDescription = null, tint = TealPrimary)
                    Spacer(Modifier.width(8.dp))
                    Text("Update the Profile", fontWeight = FontWeight.Bold, color = DarkText)
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        "Update your profile details. These will be automatically filled into the Cyber Crime Portal complaint.",
                        fontSize = 13.sp,
                        color = SubtitleGray
                    )

                    // Unique Victim ID Badge
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFFEFF6FF),
                        border = BorderStroke(1.dp, Color(0xFFBFDBFE)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.AccountBox,
                                contentDescription = null,
                                tint = Color(0xFF1D4ED8),
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        "Victim Identification ID",
                                        fontSize = 11.sp,
                                        color = Color(0xFF1E40AF),
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Spacer(Modifier.width(6.dp))
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = Color(0xFF2563EB)
                                    ) {
                                        Text(
                                            "AI Generated",
                                            fontSize = 8.sp,
                                            color = Color.White,
                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                                Text(
                                    text = if (victimId.isNotBlank()) victimId else if (userProfile.victimId.isNotBlank()) userProfile.victimId else "Generated on Portal Login",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF1E3A8A)
                                )
                            }
                        }
                    }

                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Full Name *") },
                        placeholder = { Text("e.g. Rahul Sharma") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = phone,
                            onValueChange = { phone = it },
                            label = { Text("Mobile Number *") },
                            placeholder = { Text("e.g. 9876543210") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = gender,
                            onValueChange = { gender = it },
                            label = { Text("Gender") },
                            placeholder = { Text("Male / Female") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = dob,
                            onValueChange = { dob = it },
                            label = { Text("DOB") },
                            placeholder = { Text("DD/MM/YYYY") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = email,
                            onValueChange = { email = it },
                            label = { Text("Email ID *") },
                            placeholder = { Text("e.g. rahul@gmail.com") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = state,
                            onValueChange = { state = it },
                            label = { Text("State / UT") },
                            placeholder = { Text("e.g. Telangana") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = district,
                            onValueChange = { district = it },
                            label = { Text("District") },
                            placeholder = { Text("e.g. Hyderabad") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = policeStation,
                            onValueChange = { policeStation = it },
                            label = { Text("Police Station") },
                            placeholder = { Text("e.g. Central PS") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = pinCode,
                            onValueChange = { pinCode = it },
                            label = { Text("Pin code") },
                            placeholder = { Text("500001") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                    }

                    OutlinedTextField(
                        value = address,
                        onValueChange = { address = it },
                        label = { Text("Address / City") },
                        placeholder = { Text("e.g. Flat 101, Green Hills") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val updated = userProfile.copy(
                            fullName = name.trim(),
                            mobile = phone.trim(),
                            gender = gender.trim(),
                            dob = dob.trim(),
                            email = email.trim(),
                            state = state.trim(),
                            district = district.trim(),
                            policeStation = policeStation.trim(),
                            address = address.trim(),
                            pincode = pinCode.trim()
                        )
                        UserProfileStore.save(context, updated)
                        userProfile = UserProfileStore.get(context)
                        showUserDetailsDialog = false
                        Toast.makeText(context, "Profile updated successfully!\nAI Victim ID: ${userProfile.victimId}", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = TealPrimary)
                ) {
                    Text("Update Profile")
                }
            },
            dismissButton = {
                TextButton(onClick = { showUserDetailsDialog = false }) {
                    Text("Close")
                }
            }
        )
    }

    // --- 2. COMPLAINT STATUS DIALOG ---
    if (showComplaintStatusDialog) {
        var ackNum by remember { mutableStateOf(userProfile.ackNumber) }

        AlertDialog(
            onDismissRequest = { showComplaintStatusDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Info, contentDescription = null, tint = TealPrimary)
                    Spacer(Modifier.width(8.dp))
                    Text("2. Complaint Status", fontWeight = FontWeight.Bold, color = DarkText)
                }
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = TealHalo,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(Modifier.padding(12.dp)) {
                            Text(
                                "CURRENT STATUS",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = TealPrimary
                            )
                            Text(
                                if (userProfile.ackNumber.isNotBlank())
                                    "Complaint Registered: ${userProfile.ackNumber}\nStatus: Under Investigation / Funds Lien Marked"
                                else
                                    "No complaint filed yet. Use Guided Flow to prepare and auto-fill your complaint.",
                                fontSize = 13.sp,
                                color = DarkText
                            )
                        }
                    }

                    OutlinedTextField(
                        value = ackNum,
                        onValueChange = { ackNum = it },
                        label = { Text("Acknowledgement / Token No.") },
                        placeholder = { Text("e.g. 20261024001930") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Button(
                        onClick = {
                            val dial = Intent(Intent.ACTION_DIAL, Uri.parse("tel:1930"))
                            context.startActivity(dial)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F766E)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Call, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Call 1930 to Track Bank Freeze")
                    }

                    OutlinedButton(
                        onClick = {
                            val openPortal = Intent(
                                Intent.ACTION_VIEW,
                                Uri.parse("https://cybercrime.gov.in/Webform/CheckStatus.aspx")
                            )
                            context.startActivity(openPortal)
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Track on cybercrime.gov.in")
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val updated = userProfile.copy(ackNumber = ackNum.trim())
                        UserProfileStore.save(context, updated)
                        userProfile = updated
                        showComplaintStatusDialog = false
                        Toast.makeText(context, "Complaint reference saved!", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = TealPrimary)
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showComplaintStatusDialog = false }) {
                    Text("Close")
                }
            }
        )
    }

    // --- 3. PHONETRACE DIALOG ---
    if (showPhoneTraceDialog) {
        var scanning by remember { mutableStateOf(false) }
        var detectedRisks by remember { mutableStateOf<List<AppRisk>?>(null) }
        val ptScroll = rememberScrollState()

        AlertDialog(
            onDismissRequest = { showPhoneTraceDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Phone, contentDescription = null, tint = TealPrimary)
                    Spacer(Modifier.width(8.dp))
                    Text("3. PhoneTrace", fontWeight = FontWeight.Bold, color = DarkText)
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(ptScroll),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        "How did the attacker likely get in? PhoneTrace scans installed apps on this phone for remote control tools (AnyDesk, TeamViewer), screen overlays, SMS-reading permissions, and apps installed around the fraud date.",
                        fontSize = 13.sp,
                        color = SubtitleGray,
                        lineHeight = 18.sp
                    )

                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        color = TealHalo,
                        border = BorderStroke(1.dp, TealPrimary.copy(alpha = 0.3f))
                    ) {
                        Column(Modifier.padding(10.dp)) {
                            Text(
                                "Forensic Safety Rule",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = TealPrimary
                            )
                            Spacer(Modifier.height(2.dp))
                            Text(
                                "Uses normal Android read-only APIs. Outputs 'suspicious' or 'likely entry point', never 'proven culprit'. Save evidence before deleting any app.",
                                fontSize = 11.sp,
                                color = DarkText
                            )
                        }
                    }

                    Button(
                        onClick = {
                            scanning = true
                            val pivot = System.currentTimeMillis() - (7L * 86400_000L)
                            detectedRisks = PhoneTrace.scan(context, pivot)
                            scanning = false
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = TealPrimary)
                    ) {
                        Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(if (scanning) "Scanning Installed Apps..." else "Run Live PhoneTrace Scan")
                    }

                    detectedRisks?.let { risks ->
                        if (risks.isEmpty()) {
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(8.dp),
                                color = Color(0xFFF3F4F6)
                            ) {
                                Text(
                                    "No high-risk remote control or overlay apps detected on this device.",
                                    modifier = Modifier.padding(10.dp),
                                    fontSize = 12.sp,
                                    color = DarkText
                                )
                            }
                        } else {
                            Text(
                                "Detected Potential Entry Points (${risks.size}):",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = Color(0xFFB91C1C)
                            )
                            risks.take(5).forEachIndexed { idx, risk ->
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = Color.White),
                                    border = BorderStroke(
                                        1.dp,
                                        if (idx == 0 && risk.score >= 50) Color(0xFFFCA5A5) else BorderUnselected
                                    )
                                ) {
                                    Column(Modifier.padding(8.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                risk.label.ifBlank { risk.pkg },
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp,
                                                color = DarkText,
                                                modifier = Modifier.weight(1f)
                                            )
                                            Surface(
                                                shape = RoundedCornerShape(6.dp),
                                                color = if (risk.score >= 50) Color(0xFFFEE2E2) else Color(0xFFFEF3C7)
                                            ) {
                                                Text(
                                                    "Risk ${risk.score}/100",
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (risk.score >= 50) Color(0xFFB91C1C) else Color(0xFFB45309)
                                                )
                                            }
                                        }
                                        Spacer(Modifier.height(3.dp))
                                        Text(
                                            "Flags: " + risk.riskyAccess.joinToString(", "),
                                            fontSize = 11.sp,
                                            color = SubtitleGray
                                        )
                                        Text(
                                            risk.pkg,
                                            fontSize = 9.sp,
                                            color = SubtitleGray
                                        )
                                    }
                                }
                            }
                        }
                    }

                    OutlinedButton(
                        onClick = {
                            showPhoneTraceDialog = false
                            nav.navigate(Routes.PHONE_TRACE)
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Open Full PhoneTrace Screen & Timeline ▶")
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showPhoneTraceDialog = false }) {
                    Text("Close")
                }
            }
        )
    }

    // --- 4. MORE DETAILS DIALOG ---
    if (showMoreDetailsDialog) {
        AlertDialog(
            onDismissRequest = { showMoreDetailsDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Menu, contentDescription = null, tint = TealPrimary)
                    Spacer(Modifier.width(8.dp))
                    Text("4. More Details", fontWeight = FontWeight.Bold, color = DarkText)
                }
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                showMoreDetailsDialog = false
                                nav.navigate(Routes.PHONE_TRACE)
                            },
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, BorderUnselected),
                        color = Color.White
                    ) {
                        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column {
                                Text("PhoneTrace — Entry Point Audit", fontWeight = FontWeight.Medium, fontSize = 14.sp)
                                Text("Analyze suspicious apps & entry points", fontSize = 12.sp, color = SubtitleGray)
                            }
                        }
                    }

                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                showMoreDetailsDialog = false
                                nav.navigate(Routes.PORTAL_LOGIN)
                            },
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, BorderUnselected),
                        color = Color.White
                    ) {
                        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.AccountCircle, contentDescription = null, tint = TealPrimary, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(10.dp))
                            Column {
                                Text("Portal Login (Citizen)", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = TealPrimary)
                                Text("Citizen Login with OTP & Captcha", fontSize = 12.sp, color = SubtitleGray)
                            }
                        }
                    }

                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                showMoreDetailsDialog = false
                                nav.navigate(Routes.PORTAL_CLONE)
                            },
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, BorderUnselected),
                        color = Color.White
                    ) {
                        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column {
                                Text("Portal Clone (Exact Buttons)", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = TealPrimary)
                                Text("1:1 replica of cybercrime.gov.in with auto-fill", fontSize = 12.sp, color = SubtitleGray)
                            }
                        }
                    }

                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                showMoreDetailsDialog = false
                                nav.navigate(Routes.PORTAL_GUIDE)
                            },
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, BorderUnselected),
                        color = Color.White
                    ) {
                        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column {
                                Text("Portal Auto-Fill Guide", fontWeight = FontWeight.Medium, fontSize = 14.sp)
                                Text("Automated cybercrime.gov.in filing", fontSize = 12.sp, color = SubtitleGray)
                            }
                        }
                    }

                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                showMoreDetailsDialog = false
                                nav.navigate(Routes.EVIDENCE)
                            },
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, BorderUnselected),
                        color = Color.White
                    ) {
                        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column {
                                Text("Evidence Locker", fontWeight = FontWeight.Medium, fontSize = 14.sp)
                                Text("SMS, receipts, chats, screenshots", fontSize = 12.sp, color = SubtitleGray)
                            }
                        }
                    }

                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                showMoreDetailsDialog = false
                                nav.navigate(Routes.TRACKER)
                            },
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, BorderUnselected),
                        color = Color.White
                    ) {
                        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column {
                                Text("Case Timeline & Events", fontWeight = FontWeight.Medium, fontSize = 14.sp)
                                Text("Real-time on-device log", fontSize = 12.sp, color = SubtitleGray)
                            }
                        }
                    }

                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                showMoreDetailsDialog = false
                                nav.navigate(Routes.SETTINGS)
                            },
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, BorderUnselected),
                        color = Color.White
                    ) {
                        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column {
                                Text("App Settings & Privacy", fontWeight = FontWeight.Medium, fontSize = 14.sp)
                                Text("On-device data controls", fontSize = 12.sp, color = SubtitleGray)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showMoreDetailsDialog = false }) {
                    Text("Close")
                }
            }
        )
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                modifier = Modifier
                    .width(320.dp)
                    .fillMaxHeight(),
                drawerContainerColor = Color.White,
                drawerShape = RoundedCornerShape(topEnd = 16.dp, bottomEnd = 16.dp)
            ) {
                // Drawer Header
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(TealPrimary)
                        .statusBarsPadding()
                        .padding(20.dp)
                ) {
                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.app_logo),
                                contentDescription = "Cyber Assistant Logo",
                                modifier = Modifier
                                    .size(46.dp)
                                    .clip(RoundedCornerShape(12.dp))
                            )

                            IconButton(
                                onClick = { coroutineScope.launch { drawerState.close() } }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Close Menu",
                                    tint = Color.White
                                )
                            }
                        }

                        Spacer(Modifier.height(14.dp))

                        Text(
                            text = "Cybridge",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "National Cybercrime Portal Helper",
                            fontSize = 11.sp,
                            color = Color.White.copy(alpha = 0.8f)
                        )

                        if (userProfile.victimId.isNotBlank()) {
                            Spacer(Modifier.height(10.dp))
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = Color.White.copy(alpha = 0.15f),
                                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.3f))
                            ) {
                                Text(
                                    text = "ID: ${userProfile.victimId}",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFFFE082),
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(8.dp))

                Text(
                    text = "MENU OPTIONS",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = SubtitleGray,
                    letterSpacing = 1.sp,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                )

                // 1. Update Profile
                NavigationDrawerItem(
                    icon = {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            tint = TealPrimary,
                            modifier = Modifier.size(22.dp)
                        )
                    },
                    label = {
                        Column {
                            Text(
                                text = "Update Profile",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp,
                                color = DarkText
                            )
                            Text(
                                text = if (userProfile.victimId.isNotBlank()) userProfile.victimId else if (userProfile.fullName.isNotBlank()) userProfile.fullName else "Citizen details & Victim ID",
                                fontSize = 11.sp,
                                color = SubtitleGray,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    },
                    selected = false,
                    onClick = {
                        coroutineScope.launch {
                            drawerState.close()
                            showUserDetailsDialog = true
                        }
                    },
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp),
                    shape = RoundedCornerShape(12.dp)
                )

                // 2. Status
                NavigationDrawerItem(
                    icon = {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = TealPrimary,
                            modifier = Modifier.size(22.dp)
                        )
                    },
                    label = {
                        Column {
                            Text(
                                text = "Status",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp,
                                color = DarkText
                            )
                            Text(
                                text = if (userProfile.ackNumber.isNotBlank()) "Ack: ${userProfile.ackNumber}" else "Track complaint & 1930 ref",
                                fontSize = 11.sp,
                                color = SubtitleGray,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    },
                    selected = false,
                    onClick = {
                        coroutineScope.launch {
                            drawerState.close()
                            showComplaintStatusDialog = true
                        }
                    },
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp),
                    shape = RoundedCornerShape(12.dp)
                )

                // 3. Phone Trace
                NavigationDrawerItem(
                    icon = {
                        Icon(
                            imageVector = Icons.Default.Phone,
                            contentDescription = null,
                            tint = Color(0xFFB45309),
                            modifier = Modifier.size(22.dp)
                        )
                    },
                    label = {
                        Column {
                            Text(
                                text = "Phone Trace",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp,
                                color = DarkText
                            )
                            Text(
                                text = "Entry Point Audit & Risk Scan",
                                fontSize = 11.sp,
                                color = SubtitleGray
                            )
                        }
                    },
                    selected = false,
                    onClick = {
                        coroutineScope.launch {
                            drawerState.close()
                            showPhoneTraceDialog = true
                        }
                    },
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp),
                    shape = RoundedCornerShape(12.dp)
                )

                // 4. More Options
                NavigationDrawerItem(
                    icon = {
                        Icon(
                            imageVector = Icons.Default.Menu,
                            contentDescription = null,
                            tint = DarkText,
                            modifier = Modifier.size(22.dp)
                        )
                    },
                    label = {
                        Column {
                            Text(
                                text = "More Options",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp,
                                color = DarkText
                            )
                            Text(
                                text = "Locker, Evidences & Settings",
                                fontSize = 11.sp,
                                color = SubtitleGray
                            )
                        }
                    },
                    selected = false,
                    onClick = {
                        coroutineScope.launch {
                            drawerState.close()
                            showMoreDetailsDialog = true
                        }
                    },
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp),
                    shape = RoundedCornerShape(12.dp)
                )

                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                    color = BorderUnselected
                )

                // 5. Emergency Helpline 1930
                NavigationDrawerItem(
                    icon = {
                        Icon(
                            imageVector = Icons.Default.Phone,
                            contentDescription = null,
                            tint = Color(0xFF1D4ED8),
                            modifier = Modifier.size(22.dp)
                        )
                    },
                    label = {
                        Column {
                            Text(
                                text = "Emergency Helpline 1930",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = Color(0xFF1D4ED8)
                            )
                            Text(
                                text = "Direct Call • Financial Fraud",
                                fontSize = 11.sp,
                                color = SubtitleGray
                            )
                        }
                    },
                    selected = false,
                    onClick = {
                        coroutineScope.launch {
                            drawerState.close()
                            val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:1930"))
                            context.startActivity(intent)
                        }
                    },
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp),
                    shape = RoundedCornerShape(12.dp)
                )
            }
        }
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(BgColor)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
            ) {
                // ========================================================
                // TOP 3-LINE MENU BAR (HAMBURGER MENU)
                // ========================================================
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.app_logo),
                            contentDescription = "Cyber Assistant Logo",
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(10.dp))
                        )

                        Spacer(Modifier.width(10.dp))

                        Column {
                            Text(
                                text = "Cyber Crime Portal",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = DarkText
                            )
                            Text(
                                text = "Citizen Assistant • MHA I4C",
                                fontSize = 11.sp,
                                color = SubtitleGray
                            )
                        }
                    }

                    // Helpline 1930 Quick Badge
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = SoftMint,
                        border = BorderStroke(1.dp, DeepNavyBlue.copy(alpha = 0.2f)),
                        modifier = Modifier.clickable {
                            val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:1930"))
                            context.startActivity(intent)
                        }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Phone,
                                contentDescription = null,
                                tint = DeepNavyBlue,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                "1930",
                                color = DeepNavyBlue,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // ========================================================
                // SCROLLABLE CONTENT (Language selection, speed, sample)
                // ========================================================
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp)
                ) {
                    Spacer(modifier = Modifier.height(6.dp))

                    // "WELCOME"
                    Text(
                        text = "WELCOME",
                color = TealPrimary,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.4.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            // "Choose your language"
            Text(
                text = "Choose your language",
                color = DarkText,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                lineHeight = 34.sp
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Subtitle
            Text(
                text = "We'll speak and listen in this language.",
                color = SubtitleGray,
                fontSize = 15.sp,
                lineHeight = 20.sp
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Language cards
            languages.forEach { langItem ->
                val isSelected = langItem == selectedLang

                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(58.dp)
                        .clickable {
                            selectedLang = langItem
                            L10n.setLang(context, langItem)
                        },
                    shape = RoundedCornerShape(14.dp),
                    color = if (isSelected) SoftMint.copy(alpha = 0.45f) else White,
                    border = BorderStroke(
                        width = if (isSelected) 2.dp else 1.dp,
                        color = if (isSelected) RoyalBlue else BorderUnselected
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 20.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = langItem.label,
                            fontSize = 18.sp,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                            color = DarkText
                        )

                        if (isSelected) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Selected",
                                tint = RoyalBlue,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Speaking speed indicator card
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = White,
                border = BorderStroke(1.dp, BorderUnselected)
            ) {
                Column(
                    modifier = Modifier.padding(18.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Speaking speed",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = DarkText
                        )
                        Text(
                            text = "Slow",
                            fontSize = 13.sp,
                            color = SubtitleGray
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    LinearProgressIndicator(
                        progress = { speedProgress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp),
                        color = RoyalBlue,
                        trackColor = BorderUnselected,
                        strokeCap = StrokeCap.Round
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // "Hear a sample" outlined button
            OutlinedButton(
                onClick = {
                    Toast.makeText(
                        context,
                        "Playing sample in ${selectedLang.label} (Voice feature coming soon)",
                        Toast.LENGTH_SHORT
                    ).show()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, BorderUnselected),
                colors = ButtonDefaults.outlinedButtonColors(containerColor = Color.White)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = null,
                        tint = DarkText,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Hear a sample",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                        color = DarkText
                    )
                }
            }

                    Spacer(modifier = Modifier.height(16.dp))
                }

                // ========================================================
                // FIXED BOTTOM BAR: ALWAYS VISIBLE WITHOUT SCROLLING DOWN
                // ========================================================
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding(),
                    color = White,
                    shadowElevation = 8.dp,
                    border = BorderStroke(1.dp, BorderUnselected)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 12.dp)
                    ) {
                        Button(
                            onClick = {
                                L10n.setLang(context, selectedLang)
                                nav.navigate(Routes.REGISTER_HUB)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = TealPrimary)
                        ) {
                            Text(
                                text = "Continue",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White
                            )
                        }
                    }
                }
            }
        }
    }
}
