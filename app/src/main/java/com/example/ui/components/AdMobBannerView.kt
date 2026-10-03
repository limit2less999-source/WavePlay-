package com.example.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

private data class AdCreative(
    val brand: String,
    val headline: String,
    val description: String,
    val cta: String,
    val brandColor: Color
)

private val SAMPLE_ADS = listOf(
    AdCreative(
        brand = "Spotify",
        headline = "Stream 100M+ Songs Ad-Free",
        description = "Get 3 months of Premium free. Cancel anytime.",
        cta = "Install",
        brandColor = Color(0xFF1DB954)
    ),
    AdCreative(
        brand = "Pixel Buds",
        headline = "Pro Audio with Silent Seal ANC",
        description = "Experience rich, immersive studio sound all day.",
        cta = "Shop Now",
        brandColor = Color(0xFF4285F4)
    ),
    AdCreative(
        brand = "Headspace",
        headline = "Sleep Better, Stress Less",
        description = "Guided meditation and relaxing sleep audio tracks.",
        cta = "Try Free",
        brandColor = Color(0xFFF59E0B)
    ),
    AdCreative(
        brand = "Audible",
        headline = "Thousands of Best-Selling Audiobooks",
        description = "Listen offline anytime, anywhere. 30-day trial.",
        cta = "Listen",
        brandColor = Color(0xFFEA580C)
    )
)

/**
 * Realistic Interactive AdMob Banner:
 * - Hidden automatically for Pro / Developer bypass users.
 * - Active animated rotating sponsor ads with AdMob Test Unit credentials.
 * - 1-tap "Remove Ads" pill leading directly to UPI / VIP upgrade sheet.
 */
@Composable
fun AdMobBannerView(
    modifier: Modifier = Modifier,
    isProUser: Boolean,
    onOpenPro: () -> Unit
) {
    if (isProUser) return // Pro / Developer bypass users enjoy 100% ad-free experience

    val context = LocalContext.current
    var currentAdIndex by remember { mutableIntStateOf(0) }

    LaunchedEffect(Unit) {
        while (true) {
            delay(7000)
            currentAdIndex = (currentAdIndex + 1) % SAMPLE_ADS.size
        }
    }

    val ad = SAMPLE_ADS[currentAdIndex]

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF0F172A))
            .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(12.dp))
            .testTag("admob_banner_view")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            // Top Row: Ad Badge & Remove Ads button
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color(0xFFF59E0B))
                            .padding(horizontal = 5.dp, vertical = 1.dp)
                    ) {
                        Text(
                            text = "Ad",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.Black
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Google AdMob • Test Mode",
                        fontSize = 10.sp,
                        color = Color(0xFF64748B),
                        fontWeight = FontWeight.Medium
                    )
                }

                // Remove Ads Pill Button
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color(0xFFF59E0B).copy(alpha = 0.15f))
                        .clickable(onClick = onOpenPro)
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.WorkspacePremium,
                        contentDescription = null,
                        tint = Color(0xFFF59E0B),
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Remove Ads",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFF59E0B)
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Animated Creative Content
            AnimatedContent(
                targetState = ad,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "ad_animation"
            ) { currentAd ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(currentAd.brandColor.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = currentAd.brand.take(1),
                                color = currentAd.brandColor,
                                fontWeight = FontWeight.Black,
                                fontSize = 16.sp
                            )
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = currentAd.headline,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = currentAd.description,
                                fontSize = 10.sp,
                                color = Color(0xFF94A3B8),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    // CTA Button
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(currentAd.brandColor)
                            .clickable {
                                android.widget.Toast.makeText(context, "Opening ${currentAd.brand} sponsored link...", android.widget.Toast.LENGTH_SHORT).show()
                            }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = currentAd.cta,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}
