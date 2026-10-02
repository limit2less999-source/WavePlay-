package com.example.ui.components

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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WavePlayProSheet(
    isProUser: Boolean,
    onDismiss: () -> Unit,
    onUpgradeSuccess: () -> Unit,
    onToggleTestPro: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var selectedPlan by remember { mutableStateOf("lifetime") } // "monthly", "yearly", "lifetime"

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
                    .size(68.dp)
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
                    modifier = Modifier.size(38.dp)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "WavePlay Pro",
                fontSize = 24.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White
            )

            Text(
                text = if (isProUser) "You are a VIP Pro Member! All perks unlocked." else "Unlock the ultimate offline music & video experience",
                fontSize = 13.sp,
                color = if (isProUser) Color(0xFF34D399) else Color(0xFF94A3B8),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
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
                    title = "100% Ad-Free Experience",
                    subtitle = "No banner ads, no popups, zero interruptions"
                )
                ProFeatureRow(
                    icon = Icons.Default.AutoAwesome,
                    title = "Unlimited Gemini AI Music",
                    subtitle = "Generate and save unlimited offline AI music"
                )
                ProFeatureRow(
                    icon = Icons.Default.Equalizer,
                    title = "Studio DSP Pro Equalizer",
                    subtitle = "Full custom faders, Bass Boost & 3D Spatial Virtualizer"
                )
                ProFeatureRow(
                    icon = Icons.Default.Palette,
                    title = "All VIP Themes Unlocked",
                    subtitle = "Access AMOLED Pure Black, Cyber Neon & Sunset themes"
                )
                ProFeatureRow(
                    icon = Icons.Default.Download,
                    title = "Ultra Fast Downloader",
                    subtitle = "Unlimited offline downloads from direct media links"
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Pricing Plans Selector
            if (!isProUser) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Monthly Plan
                    PlanCard(
                        modifier = Modifier.weight(1f),
                        title = "Monthly",
                        price = "₹29",
                        period = "/ month",
                        isSelected = selectedPlan == "monthly",
                        onClick = { selectedPlan = "monthly" }
                    )

                    // Lifetime Plan (Best Value)
                    PlanCard(
                        modifier = Modifier.weight(1f),
                        title = "Lifetime",
                        price = "₹299",
                        period = "pay once",
                        badge = "BEST VALUE",
                        isSelected = selectedPlan == "lifetime",
                        onClick = { selectedPlan = "lifetime" }
                    )

                    // Yearly Plan
                    PlanCard(
                        modifier = Modifier.weight(1f),
                        title = "Yearly",
                        price = "₹99",
                        period = "/ year",
                        isSelected = selectedPlan == "yearly",
                        onClick = { selectedPlan = "yearly" }
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Primary Upgrade Button
                Button(
                    onClick = {
                        onUpgradeSuccess()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("upgrade_pro_btn"),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFF59E0B)
                    )
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.WorkspacePremium,
                            contentDescription = null,
                            tint = Color.Black,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Upgrade to WavePlay Pro",
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.Black,
                            fontSize = 15.sp
                        )
                    }
                }
            } else {
                // Already Pro
                Button(
                    onClick = onDismiss,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981))
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.Check, contentDescription = null, tint = Color.White)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Pro Membership Active", fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Test Mode Toggle (Simulate In-App Purchases for testing without Google Play Merchant account)
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
                    text = "Google Play Billing Ready",
                    color = Color(0xFF64748B),
                    fontSize = 11.sp
                )
            }

            Spacer(modifier = Modifier.height(12.dp))
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
