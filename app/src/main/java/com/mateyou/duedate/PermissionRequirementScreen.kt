package com.mateyou.duedate

import androidx.annotation.RawRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun PermissionRequirementScreen(
    title: String,
    description: String,
    @RawRes svgRes: Int,
    buttonText: String,
    onButtonClick: () -> Unit
) {
    val context = LocalContext.current
    val blobColor = MaterialTheme.colorScheme.primaryContainer
    val lineColor = MaterialTheme.colorScheme.primary

    val tintedSvg = remember(svgRes, blobColor, lineColor) {
        try {
            val svgString = context.resources.openRawResource(svgRes).bufferedReader().use { it.readText() }
            val blobHex = String.format("#%06X", (0xFFFFFF and blobColor.toArgb()))
            val lineHex = String.format("#%06X", (0xFFFFFF and lineColor.toArgb()))
            // Permission SVGs use #F1F1F1 for circle
            svgString.replace("#F1F1F1", blobHex).replace("#000000", lineHex)
        } catch (e: Exception) {
            ""
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ) {
            // Match the top weight from OnboardingPage
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

            // 2. Text area with fixed height for consistency
            Column(
                modifier = Modifier.height(160.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onBackground
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.outline,
                    lineHeight = 22.sp
                )
            }

            // 3. Spacer between Text and Button Area
            Spacer(modifier = Modifier.height(24.dp))

            // 4. Button Area (fixed position area matching onboarding)
            Box(modifier = Modifier.height(100.dp).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Button(
                    onClick = onButtonClick,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.height(56.dp).fillMaxWidth(0.7f),
                    contentPadding = PaddingValues(horizontal = 24.dp, vertical = 0.dp)
                ) {
                    Text(buttonText, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
            }

            // Match the bottom weight from OnboardingPage
            Spacer(modifier = Modifier.weight(1f))
        }

        // Dummy Spacer to match the height of bottom controls row in OnboardingScreen
        // padding(vertical = 32.dp) + estimated content height (48.dp) = 112.dp
        Spacer(modifier = Modifier.height(112.dp))
    }
}
