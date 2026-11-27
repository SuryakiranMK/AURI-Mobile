/*
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.android.onboarding

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.metrolist.android.R
import com.metrolist.shared.generated.resources.Res
import com.metrolist.shared.generated.resources.onboarding_brand_tagline
import com.metrolist.shared.generated.resources.onboarding_brand_title
import com.metrolist.shared.generated.resources.onboarding_continue_guest
import com.metrolist.shared.generated.resources.onboarding_feature_lossless
import com.metrolist.shared.generated.resources.onboarding_feature_no_ads
import com.metrolist.shared.generated.resources.onboarding_feature_oled
import com.metrolist.shared.generated.resources.onboarding_restore_backup
import com.metrolist.shared.generated.resources.onboarding_signin_ytm
import com.metrolist.shared.generated.resources.onboarding_title_composite
import com.metrolist.shared.ui.icons.MetrolistIcons
import com.metrolist.shared.ui.theme.CosmicCyberCyan
import com.metrolist.shared.ui.theme.CosmicDarkObsidian
import com.metrolist.shared.ui.theme.CosmicElectricViolet
import com.metrolist.shared.ui.theme.CosmicFrostedBorder
import com.metrolist.shared.ui.theme.CosmicOnObsidianVariant
import com.metrolist.shared.ui.theme.CosmicPureBlack
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun OnboardingScreen(
    migration: LegacyInstallMigration? = null,
    onComplete: (sendCrashReports: Boolean, destination: OnboardingDestination) -> Unit,
    hasLegacyInstallationOverride: Boolean? = null,
) {
    val singlePageTitle = stringResource(Res.string.onboarding_title_composite)

    Surface(
        modifier =
            Modifier
                .fillMaxSize()
                .semantics { paneTitle = singlePageTitle },
        color = CosmicPureBlack,
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Cosmic celestial ambient radial glow at top
            Box(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(420.dp)
                        .background(
                            Brush.radialGradient(
                                colors =
                                    listOf(
                                        CosmicElectricViolet.copy(alpha = 0.22f),
                                        CosmicDarkObsidian.copy(alpha = 0.10f),
                                        Color.Transparent,
                                    ),
                                radius = 700f,
                            ),
                        ),
            )

            BoxWithConstraints(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .statusBarsPadding()
                        .navigationBarsPadding(),
            ) {
                val stageWidth = minOf(maxWidth, 500.dp)

                Column(
                    modifier =
                        Modifier
                            .width(stageWidth)
                            .fillMaxSize()
                            .align(Alignment.Center)
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 28.dp, vertical = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.SpaceBetween,
                ) {
                    Spacer(modifier = Modifier.weight(0.6f))

                    // Brand Hero: 3D Arch emblem & Glow
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.padding(bottom = 24.dp),
                    ) {
                        Box(
                            modifier =
                                Modifier
                                    .size(130.dp)
                                    .clip(CircleShape)
                                    .background(
                                        Brush.radialGradient(
                                            colors =
                                                listOf(
                                                    CosmicElectricViolet.copy(alpha = 0.35f),
                                                    Color.Transparent,
                                                ),
                                        ),
                                    ),
                        )
                        Image(
                            painter = painterResource(R.drawable.ic_auri_brand),
                            contentDescription = null,
                            modifier = Modifier.size(108.dp),
                        )
                    }

                    // Title: AURI
                    Text(
                        text = stringResource(Res.string.onboarding_brand_title),
                        style = MaterialTheme.typography.displayMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White,
                        letterSpacing = 3.sp,
                        modifier = Modifier.semantics { heading() },
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Tagline: Your music, uncompromised
                    Text(
                        text = stringResource(Res.string.onboarding_brand_tagline),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium,
                        color = CosmicCyberCyan,
                        letterSpacing = 0.8.sp,
                        textAlign = TextAlign.Center,
                    )

                    Spacer(modifier = Modifier.height(28.dp))

                    // Feature highlights row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally),
                    ) {
                        FeaturePill(text = stringResource(Res.string.onboarding_feature_lossless))
                        FeaturePill(text = stringResource(Res.string.onboarding_feature_oled))
                        FeaturePill(text = stringResource(Res.string.onboarding_feature_no_ads))
                    }

                    Spacer(modifier = Modifier.weight(1f))

                    // Action Buttons Area
                    Column(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .widthIn(max = 440.dp)
                                .padding(top = 24.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        // Prominent "Sign in with YouTube Music" button
                        Button(
                            onClick = { onComplete(false, OnboardingDestination.LOGIN) },
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .height(58.dp),
                            shape = RoundedCornerShape(29.dp),
                            colors =
                                ButtonDefaults.buttonColors(
                                    containerColor = CosmicElectricViolet,
                                    contentColor = Color.White,
                                ),
                            elevation = ButtonDefaults.buttonElevation(defaultElevation = 6.dp),
                        ) {
                            Icon(
                                imageVector = MetrolistIcons.AutoMirrored.Filled.Login,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp),
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = stringResource(Res.string.onboarding_signin_ytm),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }

                        // Prominent "Continue as Guest" button
                        OutlinedButton(
                            onClick = { onComplete(false, OnboardingDestination.HOME) },
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .height(58.dp),
                            shape = RoundedCornerShape(29.dp),
                            border = BorderStroke(1.2.dp, CosmicFrostedBorder),
                            colors =
                                ButtonDefaults.outlinedButtonColors(
                                    containerColor = CosmicDarkObsidian,
                                    contentColor = Color(0xFFF1F5F9),
                                ),
                        ) {
                            Icon(
                                imageVector = MetrolistIcons.Outlined.MusicNote,
                                contentDescription = null,
                                tint = CosmicCyberCyan,
                                modifier = Modifier.size(20.dp),
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = stringResource(Res.string.onboarding_continue_guest),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium,
                            )
                        }

                        // Subtle "Restore from backup" link
                        TextButton(
                            onClick = { onComplete(false, OnboardingDestination.BACKUP_RESTORE) },
                            modifier = Modifier.padding(top = 4.dp),
                        ) {
                            Text(
                                text = stringResource(Res.string.onboarding_restore_backup),
                                color = CosmicOnObsidianVariant,
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                }
            }
        }
    }
}

@Composable
private fun FeaturePill(
    text: String,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier =
            modifier
                .border(
                    BorderStroke(1.dp, CosmicFrostedBorder),
                    shape = RoundedCornerShape(16.dp),
                ).background(CosmicDarkObsidian.copy(alpha = 0.8f), shape = RoundedCornerShape(16.dp))
                .padding(horizontal = 12.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Medium,
            color = Color(0xFFE2E8F0),
            letterSpacing = 0.4.sp,
        )
    }
}
