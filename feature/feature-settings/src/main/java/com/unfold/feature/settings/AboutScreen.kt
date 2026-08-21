package com.unfold.feature.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Copyright
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.unfold.core.ui.components.CarvedIcon
import com.unfold.core.ui.components.GlassPanel
import com.unfold.core.ui.theme.LocalUnfoldTheme

@Composable
fun AboutScreen(
    onBack: () -> Unit,
    onOpenLicense: () -> Unit,
    onOpenThirdPartyNotices: () -> Unit
) {
    val theme = LocalUnfoldTheme.current
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(theme.bgVoid)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(22.dp)
    ) {
        AboutHeader(onBack)
        AboutHero()
        AboutSection("THE IDEA", Icons.Default.Lightbulb) {
            AboutBody("Unfold was created around a simple idea: your home screen shouldn't compete for your attention.\n\nInstead of filling the screen with information, Unfold focuses on giving you just enough to get where you need to go.")
            Spacer(Modifier.height(14.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                PhilosophyItem("LESS", "Less visual noise.", Modifier.weight(1f))
                PhilosophyItem("FOCUS", "Faster access to what matters.", Modifier.weight(1f))
                PhilosophyItem("CONTROL", "Your launcher, your way.", Modifier.weight(1f))
            }
        }
        AboutSection("DESIGN INSPIRATION", Icons.Default.Brush) {
            AboutCard {
                AboutCardTitle("Opus Launcher", "by Indistractable", Icons.Default.Info)
                AboutBody("The HUD-inspired visual direction of Unfold was influenced by existing launcher concepts, including Opus Launcher by Indistractable.")
                Spacer(Modifier.height(10.dp))
                AboutBody("Unfold is independently designed and developed. It is not affiliated with, endorsed by, or sponsored by Opus Launcher or Indistractable.")
            }
        }
        AboutSection("THE UNFOLD PHILOSOPHY", Icons.Default.TouchApp) {
            AboutCard {
                PhilosophyRow("01", "INFORMATION, NOT NOISE", "The home screen provides useful information without turning into another feed.", Icons.Default.Info)
                PhilosophyRow("02", "ACTION OVER DISTRACTION", "Apps and tools remain accessible without demanding attention.", Icons.Default.TouchApp)
                PhilosophyRow("03", "YOUR FLOW", "Gestures, dock, drawer, and layout are designed around how you use your phone.", Icons.Default.Brush)
                PhilosophyRow("04", "QUIET BY DEFAULT", "The interface stays visually restrained so your device feels calmer rather than busier.", Icons.Default.Security, last = true)
            }
        }
        AboutSection("DEVELOPMENT", Icons.Default.Code) {
            AboutBody("Unfold was developed through independent engineering, experimentation, and AI-assisted development.")
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                ToolCard("Google\nAntigravity", Modifier.weight(1f))
                ToolCard("OpenAI\nCodex", Modifier.weight(1f))
                ToolCard("GitHub\nCopilot", Modifier.weight(1f))
            }
            Spacer(Modifier.height(10.dp))
            AboutBody("These tools were used for development assistance, experimentation, debugging, research, and iteration. Product direction, design decisions, implementation, and final integration remain part of the Unfold project.")
        }
        AboutSection("CREDITS & ATTRIBUTION", Icons.Default.Info) {
            AboutCard {
                AboutLinkRow("Design Inspiration", "Opus Launcher by Indistractable", Icons.Default.Brush)
                AboutLinkRow("Development Assistance", "Google Antigravity, OpenAI Codex, GitHub Copilot", Icons.Default.Code)
                AboutLinkRow("Third-Party Components", "Libraries, fonts, icons and other components", Icons.Default.Description, last = true)
            }
            AboutActionRow("VIEW THIRD-PARTY NOTICES", Icons.Default.Description, onOpenThirdPartyNotices)
        }
        AboutSection("OPEN SOURCE & LICENSE", Icons.Default.Security) {
            AboutCard {
                Text("LICENSE NOT CONFIGURED", color = theme.textPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(6.dp))
                AboutBody("This repository does not currently include a LICENSE file, so Unfold's license is not claimed here.")
            }
            AboutActionRow("VIEW LICENSE", Icons.Default.Description, onOpenLicense)
        }
        AboutSection("TRADEMARKS", Icons.Default.Security) {
            AboutBody("Unfold and its associated branding are part of the Unfold project.\n\nThird-party product names and trademarks mentioned within this application belong to their respective owners. Their mention does not imply affiliation, sponsorship, or endorsement.")
        }
        AboutSection("COPYRIGHT", Icons.Default.Copyright) {
            AboutCard {
                Text("© 2026 Unfold", color = theme.textPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                AboutBody("The original Unfold source code, interface implementation, artwork, branding, and other original project materials are subject to their applicable licenses and rights.")
            }
            AboutActionRow("VIEW LICENSE", Icons.Default.Description, onOpenLicense)
        }
        AboutSection("UNFOLD ON THE WEB", Icons.Default.Language) {
            AboutCard {
                AboutBody("No public repository, documentation, issue tracker, or project website is configured in this build.")
            }
        }
        AboutCard(modifier = Modifier.padding(bottom = 20.dp)) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                Text("B U I L T  T O  D I S A P P E A R .", color = theme.accentPrimary, fontSize = 15.sp, letterSpacing = 1.5.sp, textAlign = TextAlign.Center)
                Spacer(Modifier.height(10.dp))
                AboutBody("Your launcher shouldn't be the thing you notice.\nIt should be the thing that gets out of your way.")
                Spacer(Modifier.height(10.dp))
                AboutBody("Thank you for using Unfold.\nMade with focus and intention.")
                Spacer(Modifier.height(12.dp))
                Text("U N F O L D", color = theme.accentPrimary, fontSize = 14.sp, letterSpacing = 3.sp)
            }
        }
    }
}

@Composable
private fun AboutHeader(onBack: () -> Unit) {
    val theme = LocalUnfoldTheme.current
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Back", tint = theme.textPrimary) }
        Text("ABOUT UNFOLD", color = theme.textPrimary, fontSize = 17.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp)
    }
}

@Composable
private fun AboutHero() {
    val theme = LocalUnfoldTheme.current
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp)) {
        Text("U N F O L D", color = theme.textPrimary, fontSize = 28.sp, letterSpacing = 6.sp)
        Text("Minimal. Intentional. Yours.", color = theme.accentPrimary, fontSize = 13.sp, letterSpacing = 1.2.sp, modifier = Modifier.padding(top = 8.dp))
        Text("VERSION 1.0.0", color = theme.textSecondary, fontSize = 10.sp, letterSpacing = 1.sp, modifier = Modifier.padding(top = 14.dp))
        Text("MADE FOR ANDROID", color = theme.textMuted, fontSize = 10.sp, letterSpacing = 1.sp, modifier = Modifier.padding(top = 4.dp))
    }
}

@Composable
private fun AboutSection(title: String, icon: ImageVector, content: @Composable () -> Unit) {
    val theme = LocalUnfoldTheme.current
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(9.dp)) {
            Icon(icon, title, tint = theme.accentPrimary, modifier = Modifier.size(18.dp))
            Text(title, color = theme.accentPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
        }
        content()
    }
}

@Composable
private fun AboutCard(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    val theme = LocalUnfoldTheme.current
    GlassPanel(modifier = modifier.fillMaxWidth(), cornerRadius = 16.dp) {
        Column(modifier = Modifier.padding(16.dp)) { content() }
    }
}

@Composable
private fun AboutBody(text: String) {
    val theme = LocalUnfoldTheme.current
    Text(text, color = theme.textSecondary, fontSize = 12.sp, lineHeight = 18.sp)
}

@Composable
private fun PhilosophyItem(title: String, text: String, modifier: Modifier) {
    val theme = LocalUnfoldTheme.current
    Column(modifier = modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(title, color = theme.accentPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
        Text(text, color = theme.textSecondary, fontSize = 10.sp, textAlign = TextAlign.Center, lineHeight = 15.sp, modifier = Modifier.padding(top = 4.dp))
    }
}

@Composable
private fun AboutCardTitle(title: String, subtitle: String, icon: ImageVector) {
    val theme = LocalUnfoldTheme.current
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        CarvedIcon(size = 36.dp, contentDescription = title, icon = { Icon(icon, null, tint = theme.accentPrimary, modifier = Modifier.size(18.dp)) })
        Column {
            Text(title, color = theme.textPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            Text(subtitle, color = theme.textSecondary, fontSize = 11.sp)
        }
    }
    Spacer(Modifier.height(12.dp))
}

@Composable
private fun PhilosophyRow(number: String, title: String, description: String, icon: ImageVector, last: Boolean = false) {
    val theme = LocalUnfoldTheme.current
    Column {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 10.dp)) {
            CarvedIcon(size = 34.dp, contentDescription = title, icon = { Icon(icon, null, tint = theme.accentPrimary, modifier = Modifier.size(16.dp)) })
            Text(number, color = theme.accentPrimary, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, color = theme.textPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Text(description, color = theme.textSecondary, fontSize = 10.sp, lineHeight = 14.sp)
            }
        }
        if (!last) Box(Modifier.fillMaxWidth().height(1.dp).background(theme.panelBorder.copy(alpha = 0.35f)))
    }
}

@Composable
private fun ToolCard(title: String, modifier: Modifier) {
    val theme = LocalUnfoldTheme.current
    Box(modifier = modifier.border(1.dp, theme.panelBorder.copy(alpha = 0.45f), RoundedCornerShape(12.dp)).padding(10.dp), contentAlignment = Alignment.Center) {
        Text(title, color = theme.textSecondary, fontSize = 10.sp, textAlign = TextAlign.Center, lineHeight = 14.sp)
    }
}

@Composable
private fun AboutLinkRow(title: String, subtitle: String, icon: ImageVector, last: Boolean = false) {
    val theme = LocalUnfoldTheme.current
    Column {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 10.dp)) {
            Icon(icon, title, tint = theme.accentPrimary, modifier = Modifier.size(20.dp))
            Column(modifier = Modifier.weight(1f).padding(horizontal = 12.dp)) {
                Text(title, color = theme.textPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Text(subtitle, color = theme.textSecondary, fontSize = 10.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
            Icon(Icons.Default.ArrowForward, "Details", tint = theme.textSecondary, modifier = Modifier.size(18.dp))
        }
        if (!last) Box(Modifier.fillMaxWidth().height(1.dp).background(theme.panelBorder.copy(alpha = 0.3f)))
    }
}

@Composable
private fun AboutActionRow(title: String, icon: ImageVector, onClick: () -> Unit) {
    val theme = LocalUnfoldTheme.current
    Row(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).border(1.dp, theme.panelBorder.copy(alpha = 0.45f), RoundedCornerShape(14.dp)).padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, title, tint = theme.accentPrimary, modifier = Modifier.size(20.dp))
        Text(title, color = theme.textPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f).padding(horizontal = 12.dp))
        Icon(Icons.Default.ArrowForward, "Open", tint = theme.textSecondary, modifier = Modifier.size(18.dp))
    }
}

@Composable
fun LicenseScreen(onBack: () -> Unit) {
    SimpleAboutDetailScreen("LICENSE", onBack, "No LICENSE file is currently included in this repository. Unfold's license status is therefore intentionally not represented as MIT or another open-source license.")
}

@Composable
fun ThirdPartyNoticesScreen(onBack: () -> Unit) {
    SimpleAboutDetailScreen("THIRD-PARTY NOTICES", onBack, "No bundled third-party notices document is currently configured for this build. This screen is reserved for the project's verified dependency notices.")
}

@Composable
private fun SimpleAboutDetailScreen(title: String, onBack: () -> Unit, message: String) {
    val theme = LocalUnfoldTheme.current
    Column(modifier = Modifier.fillMaxSize().background(theme.bgVoid).verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
        AboutHeader(onBack)
        AboutCard {
            Text(title, color = theme.accentPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
            Spacer(Modifier.height(10.dp))
            AboutBody(message)
        }
    }
}
