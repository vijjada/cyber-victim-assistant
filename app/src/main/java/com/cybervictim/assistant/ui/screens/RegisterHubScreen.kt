package com.cybervictim.assistant.ui.screens

import android.content.Intent
import android.net.Uri
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.cybervictim.assistant.R
import com.cybervictim.assistant.data.UserProfileStore
import com.cybervictim.assistant.portal.StepOwner
import com.cybervictim.assistant.ui.Routes
import com.cybervictim.assistant.voice.ScreenDescriptor
import com.cybervictim.assistant.voice.TrackScreenDescriptor
import kotlinx.coroutines.launch

// Official App Palette (same as LanguageSelectScreen)
private val HubDeepNavy = Color(0xFF082B5B)
private val HubRoyalBlue = Color(0xFF145DA0)
private val HubBg = Color(0xFFFAF9F6)
private val HubWhite = Color(0xFFFFFFFF)
private val HubBorder = Color(0xFFE5E7EB)
private val HubGold = Color(0xFFF4A62A)
private val HubMint = Color(0xFFDDF3E8)
private val HubDarkText = Color(0xFF263238)
private val HubSubtitle = Color(0xFF546E7A)

@Composable
fun RegisterHubScreen(nav: NavHostController) {
    TrackScreenDescriptor(
        ScreenDescriptor(
            screenId = "register_hub",
            title = "CyBridge Home",
            buttons = listOf("File Complaint", "Check Status", "Report Suspect", "Case Builder"),
            fields = emptyList(),
            owner = StepOwner.AI_AUTO,
            hintKey = "voice_hint_register_hub"
        )
    )
    val context = LocalContext.current
    val userProfile = remember { UserProfileStore.get(context) }
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val coroutineScope = rememberCoroutineScope()

    // ----- DRAWER DIALOGS (profile, complaint status, etc.) -----
    var showUserDetailsDialog by remember { mutableStateOf(false) }

    if (showUserDetailsDialog) {
        AlertDialog(
            onDismissRequest = { showUserDetailsDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Person, contentDescription = null, tint = HubDeepNavy)
                    Spacer(Modifier.width(8.dp))
                    Text("Your Profile", fontWeight = FontWeight.Bold, color = HubDarkText)
                }
            },
            text = {
                Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (userProfile.victimId.isNotBlank()) {
                        Surface(shape = RoundedCornerShape(8.dp), color = Color(0xFFEFF6FF), modifier = Modifier.fillMaxWidth()) {
                            Text("Victim ID: ${userProfile.victimId}", fontSize = 13.sp, fontWeight = FontWeight.Bold,
                                color = Color(0xFF1E3A8A), modifier = Modifier.padding(10.dp))
                        }
                    }
                    if (userProfile.fullName.isNotBlank()) Text("Name: ${userProfile.fullName}", fontSize = 13.sp)
                    if (userProfile.mobile.isNotBlank()) Text("Mobile: ${userProfile.mobile}", fontSize = 13.sp)
                    if (userProfile.state.isNotBlank()) Text("State: ${userProfile.state}", fontSize = 13.sp)
                }
            },
            confirmButton = {
                TextButton(onClick = { showUserDetailsDialog = false }) { Text("Close") }
            }
        )
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                modifier = Modifier.width(310.dp).fillMaxHeight(),
                drawerContainerColor = HubWhite,
                drawerShape = RoundedCornerShape(topEnd = 16.dp, bottomEnd = 16.dp)
            ) {
                // Drawer header with app branding
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(HubDeepNavy)
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
                                contentDescription = "Cybridge Logo",
                                modifier = Modifier.size(46.dp).clip(RoundedCornerShape(12.dp))
                            )
                            IconButton(onClick = { coroutineScope.launch { drawerState.close() } }) {
                                Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                            }
                        }
                        Spacer(Modifier.height(14.dp))
                        Text("Cybridge", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        Text("National Cybercrime Portal Assistant", fontSize = 11.sp, color = Color.White.copy(alpha = 0.8f))
                        if (userProfile.victimId.isNotBlank()) {
                            Spacer(Modifier.height(8.dp))
                            Surface(shape = RoundedCornerShape(6.dp), color = Color.White.copy(alpha = 0.15f),
                                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.3f))) {
                                Text("ID: ${userProfile.victimId}", fontSize = 10.sp, fontWeight = FontWeight.Bold,
                                    color = Color(0xFFFFE082), modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp))
                            }
                        }
                    }
                }

                Spacer(Modifier.height(8.dp))

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(bottom = 16.dp)
                ) {
                    Text("MENU", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = HubSubtitle,
                        letterSpacing = 1.sp, modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp))

                    // Menu item: View Profile
                    NavigationDrawerItem(
                        icon = { Icon(Icons.Default.Person, contentDescription = null, tint = HubDeepNavy, modifier = Modifier.size(22.dp)) },
                        label = {
                            Column {
                                Text("View Profile", fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = HubDarkText)
                                Text(userProfile.fullName.ifBlank { "Citizen details" }, fontSize = 11.sp, color = HubSubtitle,
                                    maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        },
                        selected = false,
                        onClick = { coroutineScope.launch { drawerState.close(); showUserDetailsDialog = true } },
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp),
                        shape = RoundedCornerShape(12.dp)
                    )

                    // Menu item: Register Cyber Complaint
                    NavigationDrawerItem(
                        icon = { Icon(Icons.Default.PlayArrow, contentDescription = null, tint = HubDeepNavy, modifier = Modifier.size(22.dp)) },
                        label = {
                            Column {
                                Text("Register Cyber Complaint", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = HubDeepNavy)
                                Text("File official report on cybercrime.gov.in", fontSize = 11.sp, color = HubSubtitle)
                            }
                        },
                        selected = false,
                        onClick = { coroutineScope.launch { drawerState.close() }; nav.navigate(Routes.PORTAL_WEBVIEW) },
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp),
                        shape = RoundedCornerShape(12.dp)
                    )

                    Divider(modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp))

                    Text("EVIDENCE & LEGAL", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = HubSubtitle,
                        letterSpacing = 1.sp, modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp))

                    // Menu item: Evidence Locker
                    NavigationDrawerItem(
                        icon = { Icon(Icons.Default.Info, contentDescription = null, tint = HubDeepNavy, modifier = Modifier.size(22.dp)) },
                        label = {
                            Column {
                                Text("Evidence Locker", fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = HubDarkText)
                                Text("Screenshots, transaction slips & files", fontSize = 11.sp, color = HubSubtitle)
                            }
                        },
                        selected = false,
                        onClick = { coroutineScope.launch { drawerState.close() }; nav.navigate(Routes.EVIDENCE) },
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp),
                        shape = RoundedCornerShape(12.dp)
                    )

                    // Menu item: Freeze & Legal Requests
                    NavigationDrawerItem(
                        icon = { Icon(Icons.Default.CheckCircle, contentDescription = null, tint = HubDeepNavy, modifier = Modifier.size(22.dp)) },
                        label = {
                            Column {
                                Text("Freeze & Legal Requests", fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = HubDarkText)
                                Text("Draft bank freeze, unfreeze & notices", fontSize = 11.sp, color = HubSubtitle)
                            }
                        },
                        selected = false,
                        onClick = { coroutineScope.launch { drawerState.close() }; nav.navigate(Routes.REQUESTS) },
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp),
                        shape = RoundedCornerShape(12.dp)
                    )

                    // Menu item: Case Timeline
                    NavigationDrawerItem(
                        icon = { Icon(Icons.Default.DateRange, contentDescription = null, tint = HubDeepNavy, modifier = Modifier.size(22.dp)) },
                        label = {
                            Column {
                                Text("Case Timeline", fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = HubDarkText)
                                Text("View events & complaint history", fontSize = 11.sp, color = HubSubtitle)
                            }
                        },
                        selected = false,
                        onClick = { coroutineScope.launch { drawerState.close() }; nav.navigate(Routes.TRACKER) },
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp),
                        shape = RoundedCornerShape(12.dp)
                    )

                    Divider(modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp))

                    Text("INVESTIGATION TOOLS", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = HubSubtitle,
                        letterSpacing = 1.sp, modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp))

                    // Menu item: Phone Trace
                    NavigationDrawerItem(
                        icon = { Icon(Icons.Default.Phone, contentDescription = null, tint = HubDeepNavy, modifier = Modifier.size(22.dp)) },
                        label = {
                            Column {
                                Text("PhoneTrace Audit", fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = HubDarkText)
                                Text("Analyze suspicious apps & entry points", fontSize = 11.sp, color = HubSubtitle)
                            }
                        },
                        selected = false,
                        onClick = { coroutineScope.launch { drawerState.close() }; nav.navigate(Routes.PHONE_TRACE) },
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp),
                        shape = RoundedCornerShape(12.dp)
                    )

                    // Menu item: Suspicious Email Check
                    NavigationDrawerItem(
                        icon = { Icon(Icons.Default.Email, contentDescription = null, tint = HubDeepNavy, modifier = Modifier.size(22.dp)) },
                        label = {
                            Column {
                                Text("Suspicious Email Check", fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = HubDarkText)
                                Text("Phishing analysis on-device", fontSize = 11.sp, color = HubSubtitle)
                            }
                        },
                        selected = false,
                        onClick = { coroutineScope.launch { drawerState.close() }; nav.navigate(Routes.SUSPICIOUS_EMAIL) },
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp),
                        shape = RoundedCornerShape(12.dp)
                    )

                    // Menu item: Digital Exposure Check
                    NavigationDrawerItem(
                        icon = { Icon(Icons.Default.Search, contentDescription = null, tint = HubDeepNavy, modifier = Modifier.size(22.dp)) },
                        label = {
                            Column {
                                Text("Digital Exposure Check", fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = HubDarkText)
                                Text("Breach checks & password leak status", fontSize = 11.sp, color = HubSubtitle)
                            }
                        },
                        selected = false,
                        onClick = { coroutineScope.launch { drawerState.close() }; nav.navigate(Routes.EXPOSURE) },
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp),
                        shape = RoundedCornerShape(12.dp)
                    )

                    Divider(modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp))

                    Text("GUIDANCE & SETTINGS", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = HubSubtitle,
                        letterSpacing = 1.sp, modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp))

                    // Menu item: Portal Filing Guide
                    NavigationDrawerItem(
                        icon = { Icon(Icons.Default.Info, contentDescription = null, tint = HubDeepNavy, modifier = Modifier.size(22.dp)) },
                        label = {
                            Column {
                                Text("Portal Filing Guide", fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = HubDarkText)
                                Text("Official portal instructions & steps", fontSize = 11.sp, color = HubSubtitle)
                            }
                        },
                        selected = false,
                        onClick = { coroutineScope.launch { drawerState.close() }; nav.navigate(Routes.PORTAL_GUIDE) },
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp),
                        shape = RoundedCornerShape(12.dp)
                    )

                    // Menu item: Settings & Language
                    NavigationDrawerItem(
                        icon = { Icon(Icons.Default.Settings, contentDescription = null, tint = HubDeepNavy, modifier = Modifier.size(22.dp)) },
                        label = {
                            Column {
                                Text("Settings & Language", fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = HubDarkText)
                                Text("Change language & privacy preferences", fontSize = 11.sp, color = HubSubtitle)
                            }
                        },
                        selected = false,
                        onClick = { coroutineScope.launch { drawerState.close() }; nav.navigate(Routes.SETTINGS) },
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp),
                        shape = RoundedCornerShape(12.dp)
                    )

                    Divider(modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp))

                    // Menu item: Helpline 1930
                    NavigationDrawerItem(
                        icon = { Icon(Icons.Default.Call, contentDescription = null, tint = Color(0xFF16A34A), modifier = Modifier.size(22.dp)) },
                        label = {
                            Column {
                                Text("Helpline 1930", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color(0xFF15803D))
                                Text("National Cyber Crime Emergency Helpline", fontSize = 11.sp, color = HubSubtitle)
                            }
                        },
                        selected = false,
                        onClick = {
                            coroutineScope.launch { drawerState.close() }
                            val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:1930"))
                            context.startActivity(intent)
                        },
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp),
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            }
        }
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
            ) {
                // ---- TOP BAR: 3-line hamburger menu + logo ----
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // ===== HAMBURGER MENU BUTTON (3-line bar) =====
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFFF1F5F9),
                            border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                        ) {
                            IconButton(
                                onClick = { coroutineScope.launch { drawerState.open() } },
                                modifier = Modifier.size(44.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Menu,
                                    contentDescription = "Open Menu",
                                    tint = HubDeepNavy,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }

                        Spacer(Modifier.width(12.dp))

                        Image(
                            painter = painterResource(id = R.drawable.app_logo),
                            contentDescription = "Cybridge Logo",
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(8.dp))
                        )

                        Spacer(Modifier.width(10.dp))

                        Column {
                            Text("Cybridge", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = HubDeepNavy)
                            Text("National Cybercrime Portal", fontSize = 10.sp, color = HubSubtitle)
                        }
                    }

                    // 1930 Helpline quick-dial badge
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = HubMint,
                        border = BorderStroke(1.dp, HubDeepNavy.copy(alpha = 0.2f)),
                        modifier = Modifier.clickable {
                            val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:1930"))
                            context.startActivity(intent)
                        }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Phone, contentDescription = null, tint = HubDeepNavy, modifier = Modifier.size(13.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("1930", color = HubDeepNavy, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // ---- MAIN CONTENT: centered, full-height ----
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Spacer(Modifier.height(40.dp))

                    // App Logo (Clean, no shield box)
                    Image(
                        painter = painterResource(id = R.drawable.app_logo),
                        contentDescription = "Cybridge Logo",
                        modifier = Modifier
                            .size(88.dp)
                            .clip(RoundedCornerShape(20.dp))
                    )

                    Spacer(Modifier.height(28.dp))

                    Text(
                        text = "Cyber Crime Reporting Portal",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = HubRoyalBlue,
                        letterSpacing = 1.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 32.dp)
                    )

                    Spacer(Modifier.height(10.dp))

                    Text(
                        text = "File Your Official\nCyber Complaint",
                        fontSize = 30.sp,
                        fontWeight = FontWeight.Black,
                        color = HubDeepNavy,
                        textAlign = TextAlign.Center,
                        lineHeight = 36.sp,
                        modifier = Modifier.padding(horizontal = 24.dp)
                    )

                    Spacer(Modifier.height(14.dp))

                    Text(
                        text = "AI-guided step-by-step form for\ncybercrime.gov.in — National Portal",
                        fontSize = 14.sp,
                        color = HubSubtitle,
                        textAlign = TextAlign.Center,
                        lineHeight = 20.sp,
                        modifier = Modifier.padding(horizontal = 32.dp)
                    )

                    Spacer(Modifier.height(44.dp))

                    // ===== BIG REGISTER CYBER COMPLAINT BUTTON (In C Logo Color: Deep Navy) =====
                    Button(
                        onClick = { nav.navigate(Routes.PORTAL_WEBVIEW) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 28.dp)
                            .height(68.dp),
                        shape = RoundedCornerShape(18.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = HubDeepNavy),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 6.dp, pressedElevation = 2.dp)
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "Register Cyber Complaint",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White,
                                textAlign = TextAlign.Center
                            )
                            Text(
                                text = "Tap to start filing on the official portal",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Normal,
                                color = Color.White.copy(alpha = 0.8f)
                            )
                        }
                    }

                    Spacer(Modifier.height(40.dp))
                }
            }
        }
    }
}
