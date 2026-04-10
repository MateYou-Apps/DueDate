package com.mateyou.duedate

import androidx.annotation.RawRes
import androidx.compose.animation.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

data class OnboardingData(
    val title: String,
    val description: String,
    @RawRes val svgRes: Int
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun OnboardingScreen(onComplete: () -> Unit) {
    val pages = listOf(
        OnboardingData(
            title = "Track your Credit Card Bills",
            description = "DueDate keeps track of your credit card bills. Organized in one place so that you never miss a payment again.",
            svgRes = R.raw.onboarding_track
        ),
        OnboardingData(
            title = "Smart SMS Detection",
            description = "DueDate automatically detects bank alerts and extracts your bill details for you. No manual entry required.",
            svgRes = R.raw.onboarding_sms
        ),
        OnboardingData(
            title = "Get Customized Reminders",
            description = "DueDate notifies before your bill is due. Get smart, timely notifications that help you clear your bills on time, every time.",
            svgRes = R.raw.onboarding_reminders
        ),
        OnboardingData(
            title = "Your Data is Yours",
            description = "Privacy First. All your data is processed locally and never leaves your device. No internet permission required.",
            svgRes = R.raw.onboarding_privacy
        )
    )

    val pagerState = rememberPagerState(pageCount = { pages.size })
    val scope = rememberCoroutineScope()

    val blobColor = MaterialTheme.colorScheme.primaryContainer
    val lineColor = MaterialTheme.colorScheme.primary

    Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            // 1. Pager Area
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize()
            ) { pageIndex ->
                OnboardingPage(
                    data = pages[pageIndex],
                    blobColor = blobColor,
                    lineColor = lineColor
                )
            }

            // 2. Fixed position Button Area overlaying the pager
            // This Column matches the layout of PermissionRequirementScreen exactly
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Top
            ) {
                Spacer(modifier = Modifier.weight(1.8f))
                Box(modifier = Modifier.size(280.dp)) // Illustration slot
                Spacer(modifier = Modifier.height(48.dp))
                Box(modifier = Modifier.height(160.dp)) // Text area slot
                Spacer(modifier = Modifier.height(24.dp))

                // The Button
                Box(modifier = Modifier.height(100.dp).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    androidx.compose.animation.AnimatedVisibility(
                        visible = pagerState.currentPage == pages.size - 1,
                        enter = fadeIn() + scaleIn(),
                        exit = fadeOut() + scaleOut()
                    ) {
                        Button(
                            onClick = { onComplete() },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.height(56.dp).fillMaxWidth(0.7f),
                            contentPadding = PaddingValues(horizontal = 24.dp, vertical = 0.dp)
                        ) {
                            Text("Get Started", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        }
                    }
                }
                Spacer(modifier = Modifier.weight(1f))
            }
        }

        // Bottom Controls (Slider Indicator area) - Height matches 112.dp dummy spacer
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 32.dp, vertical = 32.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Back Button
            if (pagerState.currentPage > 0) {
                IconButton(onClick = { scope.launch { pagerState.animateScrollToPage(pagerState.currentPage - 1) } }) {
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, "Back")
                }
            } else {
                Spacer(modifier = Modifier.size(48.dp))
            }

            // Page Indicators
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                repeat(pages.size) { index ->
                    Box(
                        modifier = Modifier
                            .size(if (pagerState.currentPage == index) 12.dp else 8.dp)
                            .clip(CircleShape)
                            .background(if (pagerState.currentPage == index) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant)
                    )
                }
            }

            // Next Button (Hidden on last page)
            if (pagerState.currentPage < pages.size - 1) {
                IconButton(onClick = { scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) } }) {
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, "Next")
                }
            } else {
                Spacer(modifier = Modifier.size(48.dp))
            }
        }
    }
}

@Composable
fun OnboardingPage(
    data: OnboardingData,
    blobColor: Color,
    lineColor: Color
) {
    val context = LocalContext.current
    val tintedSvg = remember(data.svgRes, blobColor, lineColor) {
        try {
            val svgString = context.resources.openRawResource(data.svgRes).bufferedReader().use { it.readText() }
            val blobHex = String.format("#%06X", (0xFFFFFF and blobColor.toArgb()))
            val lineHex = String.format("#%06X", (0xFFFFFF and lineColor.toArgb()))
            svgString.replace("#F5F5F5", blobHex).replace("#000000", lineHex)
        } catch (e: Exception) {
            ""
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {
        // Dynamic top spacer
        Spacer(modifier = Modifier.weight(1.8f))
        
        // 1. Illustration
        Box(modifier = Modifier.size(280.dp), contentAlignment = Alignment.Center) {
            if (tintedSvg.isNotEmpty()) {
                SvgIntro(
                    svgString = tintedSvg,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
        
        Spacer(modifier = Modifier.height(48.dp))

        // 2. Text area
        Column(
            modifier = Modifier.height(160.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = data.title,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onBackground
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = data.description,
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.outline,
                lineHeight = 22.sp
            )
        }

        // 3. Spacers to match button area height in overlay
        Spacer(modifier = Modifier.height(24.dp))
        Spacer(modifier = Modifier.height(100.dp))
        
        // Bottom spacer to balance the layout
        Spacer(modifier = Modifier.weight(1f))
    }
}
