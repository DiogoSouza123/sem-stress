package com.semstress.mobile.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.semstress.mobile.R
import com.semstress.mobile.ui.components.CoffeeIconButton
import com.semstress.mobile.ui.components.CoffeePanel
import com.semstress.mobile.ui.theme.CoffeeTheme

/** UX-10: state read by [SettingsScreen], kept apart from [SettingsScreenActions] to stay under the parameter limit. */
data class SettingsScreenState(
    val musicMuted: Boolean,
    val sfxMuted: Boolean,
    val symbolModeEnabled: Boolean
)

/** UX-10: callbacks for [SettingsScreen]. */
data class SettingsScreenActions(
    val onToggleMusic: () -> Unit,
    val onToggleSfx: () -> Unit,
    val onToggleSymbolMode: () -> Unit,
    val onBack: () -> Unit
)

/**
 * UX-10: a dedicated settings screen replacing the ad hoc toggle buttons scattered across the
 * menu/game headers - centralizes music/sfx/symbol-mode plus the license credits and privacy
 * placeholder every store listing requires. Onboarding, language selection and "restaurar
 * progresso" (mentioned in ui-ux.md §7) are deferred to future items.
 */
@Composable
fun SettingsScreen(state: SettingsScreenState, actions: SettingsScreenActions) {
    val colors = CoffeeTheme.colors
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(listOf(colors.panelBackground, colors.surfaceBoard.copy(alpha = 0.75f)))
            )
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            CoffeeIconButton(
                icon = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = stringResource(R.string.back_to_menu),
                onClick = actions.onBack
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = stringResource(R.string.settings_title),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.ExtraBold,
                color = colors.hudText
            )
        }

        Spacer(modifier = Modifier.height(16.dp))
        SoundSection(state, actions)

        Spacer(modifier = Modifier.height(12.dp))
        CreditsSection()

        Spacer(modifier = Modifier.height(12.dp))
        PrivacySection()
    }
}

@Composable
private fun SoundSection(state: SettingsScreenState, actions: SettingsScreenActions) {
    CoffeePanel {
        SectionTitle(stringResource(R.string.settings_sound_section))
        SettingToggleRow(
            label = stringResource(if (state.musicMuted) R.string.toggle_music_off else R.string.toggle_music_on),
            checked = !state.musicMuted,
            onCheckedChange = { actions.onToggleMusic() }
        )
        SettingToggleRow(
            label = stringResource(if (state.sfxMuted) R.string.toggle_sfx_off else R.string.toggle_sfx_on),
            checked = !state.sfxMuted,
            onCheckedChange = { actions.onToggleSfx() }
        )
        SectionTitle(stringResource(R.string.settings_accessibility_section))
        SettingToggleRow(
            label = stringResource(
                if (state.symbolModeEnabled) R.string.toggle_symbol_mode_on else R.string.toggle_symbol_mode_off
            ),
            checked = state.symbolModeEnabled,
            onCheckedChange = { actions.onToggleSymbolMode() }
        )
    }
}

@Composable
private fun CreditsSection() {
    CoffeePanel {
        SectionTitle(stringResource(R.string.settings_credits_section))
        Text(
            text = stringResource(R.string.settings_credits_music),
            style = MaterialTheme.typography.bodyMedium,
            color = CoffeeTheme.colors.hudTextMuted
        )
    }
}

@Composable
private fun PrivacySection() {
    CoffeePanel {
        SectionTitle(stringResource(R.string.settings_privacy_section))
        Text(
            text = stringResource(R.string.settings_privacy_body),
            style = MaterialTheme.typography.bodyMedium,
            color = CoffeeTheme.colors.hudTextMuted
        )
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = CoffeeTheme.colors.hudText,
        modifier = Modifier.padding(bottom = 8.dp)
    )
}

@Composable
private fun SettingToggleRow(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, style = MaterialTheme.typography.bodyLarge, color = CoffeeTheme.colors.hudText)
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
