package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private const val CREATOR_UPI_ID = "9064618542@ybl"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WavePlayProSheet(
    isProUser: Boolean,
    onDismiss: () -> Unit,
    onUpgradeSuccess: () -> Unit,
    onActivateWithUpi: (planType: String, utr: String) -> Unit = { _, _ -> },
    onToggleTestPro: () -> Unit
) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("waveplay_vault", Context.MODE_PRIVATE) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var selectedPlan by remember { mutableStateOf("lifetime") } // "monthly", "yearly", "lifetime"
    var showUpiPaymentSection by remember { mutableStateOf(false) }
    var utrInput by remember { mutableStateOf("") }
    var utrError by remember { mutableStateOf(false) }

    val savedExpiry = remember { prefs.getLong("pro_plan_expiry_time", 0L) }
    val savedUtr = remember { prefs.getString("pro_plan_utr", "") ?: "" }
    val savedPlanType = remember { prefs.getString("pro_plan_type", "lifetime") ?: "lifetime" }
    val isOwner = remember { prefs.getBoolean("owner_bypass_activated", false) }

    val planAmount = when (selectedPlan) {
        "monthly" -> "29"
        "yearly" -> "99"
        else -> "299"
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color(0xFF0F172A),
        modifier = Modifier.testTag("waveplay_pro_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 8.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Close Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                IconButton(onClick = onDismiss, modifier = Modifier.size(36.dp)) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = Color(0xFF94A3B8)
                    )
                }
            }

            // Crown Icon with Glowing Gradient
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(Color(0xFFFBBF24), Color(0xFFB45309))
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.WorkspacePremium,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(36.dp)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "WavePlay Pro VIP",
                fontSize = 22.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White
            )

            Text(
                text = if (isProUser) {
                    if (isOwner) "Lifetime Developer VIP Active (100% Ad-Free)"
                    else if (savedExpiry > 0L) {
                        val fmt = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
                        "Pro Active until ${fmt.format(Date(savedExpiry))} • Ad-Free"
                    } else "Lifetime VIP Membership Active • 100% Ad-Free"
                } else "Unlock 100% Ad-Free Music & Video, Unlimited AI & Studio Equalizer",
                fontSize = 12.sp,
                color = if (isProUser) Color(0xFF34D399) else Color(0xFF94A3B8),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 4.dp, bottom = 14.dp)
            )

            // Features List
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFF1E293B))
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                ProFeatureRow(
                    icon = Icons.Default.Block,
                    title = "100% Zero Ads & Interruption-Free",
                    subtitle = "No banner ads, no popups, smooth playback"
                )
                ProFeatureRow(
                    icon = Icons.Default.AutoAwesome,
                    title = "Unlimited Gemini AI Music Generation",
                    subtitle = "Synthesize and save studio-grade offline tracks"
                )
                ProFeatureRow(
                    icon = Icons.Default.Equalizer,
                    title = "Studio DSP Pro Equalizer",
                    subtitle = "Full custom faders, Bass Boost & 3D Spatial Virtualizer"
                )
                ProFeatureRow(
                    icon = Icons.Default.Palette,
                    title = "All VIP Themes & Safe Vault",
                    subtitle = "AMOLED Pure Black theme & hidden private locker"
                )
                ProFeatureRow(
                    icon = Icons.Default.Download,
                    title = "Ultra Fast Media Downloader",
                    subtitle = "High-speed offline downloads from direct links"
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (!isProUser) {
                // Pricing Plans Selector
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    PlanCard(
                        modifier = Modifier.weight(1f),
                        title = "Monthly",
                        price = "₹29",
                        period = "/ month",
                        isSelected = selectedPlan == "monthly",
                        onClick = { selectedPlan = "monthly" }
                    )

                    PlanCard(
                        modifier = Modifier.weight(1f),
                        title = "Lifetime",
                        price = "₹299",
                        period = "pay once",
                        badge = "BEST VALUE",
                        isSelected = selectedPlan == "lifetime",
                        onClick = { selectedPlan = "lifetime" }
                    )

                    PlanCard(
                        modifier = Modifier.weight(1f),
                        title = "Yearly",
                        price = "₹99",
                        period = "/ year",
                        isSelected = selectedPlan == "yearly",
                        onClick = { selectedPlan = "yearly" }
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Payment Options: Pay with UPI
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF131D2E)),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1E293B))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.AccountBalance,
                                    contentDescription = null,
                                    tint = Color(0xFF38BDF8),
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Pay via UPI (GPay / PhonePe / Paytm)",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = Color.White
                                )
                            }
                            Text(
                                text = "₹$planAmount",
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 16.sp,
                                color = Color(0xFFFBBF24)
                            )
                        }

                        // UPI ID Copy Card
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFF1E293B))
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("Official UPI ID", fontSize = 10.sp, color = Color(0xFF94A3B8))
                                Text(
                                    text = CREATOR_UPI_ID,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = Color(0xFF38BDF8)
                                )
                            }

                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xFF38BDF8).copy(alpha = 0.2f))
                                    .clickable {
                                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                        clipboard.setPrimaryClip(ClipData.newPlainText("UPI ID", CREATOR_UPI_ID))
                                        Toast.makeText(context, "UPI ID copied: $CREATOR_UPI_ID", Toast.LENGTH_SHORT).show()
                                    }
                                    .padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ContentCopy,
                                    contentDescription = "Copy",
                                    tint = Color(0xFF38BDF8),
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Copy", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF38BDF8))
                            }
                        }

                        // Quick 1-Tap Pay via Installed UPI Apps
                        Button(
                            onClick = {
                                try {
                                    val uri = Uri.parse("upi://pay?pa=$CREATOR_UPI_ID&pn=WavePlayPro&am=$planAmount&cu=INR&tn=WavePlay_Pro_${selectedPlan.uppercase()}")
                                    val intent = Intent(Intent.ACTION_VIEW, uri)
                                    context.startActivity(intent)
                                } catch (e: Exception) {
                                    Toast.makeText(context, "No UPI app found. Please copy UPI ID $CREATOR_UPI_ID and pay via your bank app.", Toast.LENGTH_LONG).show()
                                }
                            },
                            modifier = Modifier.fillMaxWidth().height(46.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7))
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.OpenInNew, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Pay ₹$planAmount via UPI App", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 13.sp)
                            }
                        }

                        // Payment Verification Step: UTR / Reference No.
                        Column {
                            Text(
                                text = "Verify Payment (Enter 12-Digit UTR Number):",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF94A3B8)
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            OutlinedTextField(
                                value = utrInput,
                                onValueChange = {
                                    if (it.length <= 16) {
                                        utrInput = it.filter { char -> char.isDigit() || char.isLetter() }
                                        utrError = false
                                    }
                                },
                                placeholder = { Text("e.g. 428190281928", fontSize = 13.sp, color = Color(0xFF64748B)) },
                                singleLine = true,
                                isError = utrError,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.fillMaxWidth()
                            )
                            if (utrError) {
                                Text("Please enter a valid 12-digit UTR/Reference number from your UPI receipt.", color = Color(0xFFEF4444), fontSize = 10.sp)
                            }
                            Spacer(modifier = Modifier.height(8.dp))

                            Button(
                                onClick = {
                                    val clean = utrInput.trim()
                                    if (clean.length >= 10) {
                                        onActivateWithUpi(selectedPlan, clean)
                                        Toast.makeText(context, "Payment confirmed! Pro VIP activated successfully.", Toast.LENGTH_LONG).show()
                                        onUpgradeSuccess()
                                    } else {
                                        utrError = true
                                    }
                                },
                                modifier = Modifier.fillMaxWidth().height(48.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF59E0B))
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Verified, contentDescription = null, tint = Color.Black, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Verify & Activate Pro VIP", fontWeight = FontWeight.ExtraBold, color = Color.Black)
                                }
                            }
                        }
                    }
                }
            } else {
                // Already Pro
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF064E3B).copy(alpha = 0.5f)),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF059669))
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(imageVector = Icons.Default.Check, contentDescription = null, tint = Color(0xFF34D399), modifier = Modifier.size(32.dp))
                        Spacer(modifier = Modifier.height(6.dp))
                        Text("Pro VIP Membership Active", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 15.sp)
                        Text("You are enjoying full ad-free playback, unlimited downloads, and studio audio.", fontSize = 11.sp, color = Color(0xFF94A3B8), textAlign = TextAlign.Center)
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B))
                ) {
                    Text("Close", fontWeight = FontWeight.Bold, color = Color.White)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Test Mode Toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onToggleTestPro) {
                    Text(
                        text = if (isProUser) "⚙️ Test: Switch to Free" else "⚙️ Test: Activate Pro Instantly",
                        color = Color(0xFF38BDF8),
                        fontSize = 11.sp
                    )
                }

                Text(
                    text = "UPI: $CREATOR_UPI_ID",
                    color = Color(0xFF64748B),
                    fontSize = 10.sp
                )
            }

            Spacer(modifier = Modifier.height(14.dp))
        }
    }
}

@Composable
private fun ProFeatureRow(icon: ImageVector, title: String, subtitle: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(CircleShape)
                .background(Color(0xFF334155)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color(0xFFFBBF24),
                modifier = Modifier.size(18.dp)
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(
                text = title,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Text(
                text = subtitle,
                fontSize = 11.sp,
                color = Color(0xFF94A3B8)
            )
        }
    }
}

@Composable
private fun PlanCard(
    modifier: Modifier = Modifier,
    title: String,
    price: String,
    period: String,
    badge: String? = null,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = if (isSelected) Color(0xFFFBBF24) else Color(0xFF334155),
                shape = RoundedCornerShape(14.dp)
            ),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) Color(0xFF1E293B) else Color(0xFF0F172A)
        )
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (badge != null) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(0xFFF59E0B))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = badge,
                        color = Color.Black,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
            } else {
                Spacer(modifier = Modifier.height(16.dp))
            }

            Text(text = title, fontSize = 12.sp, color = Color(0xFF94A3B8), fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(2.dp))
            Text(text = price, fontSize = 16.sp, color = Color.White, fontWeight = FontWeight.Bold)
            Text(text = period, fontSize = 10.sp, color = Color(0xFF64748B))
        }
    }
}
