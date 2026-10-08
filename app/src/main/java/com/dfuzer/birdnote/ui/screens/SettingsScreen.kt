package com.dfuzer.birdnote.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialogDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.VerticalDivider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import com.dfuzer.birdnote.R
import com.dfuzer.birdnote.domain.AppLanguage
import com.dfuzer.birdnote.domain.Clef
import com.dfuzer.birdnote.domain.NoteNaming
import com.dfuzer.birdnote.domain.scalePreview
import com.dfuzer.birdnote.ui.LayoutTuning
import com.dfuzer.birdnote.ui.components.BackButton
import com.dfuzer.birdnote.ui.components.DecoratedScreen
import com.dfuzer.birdnote.ui.components.FittedText
import com.dfuzer.birdnote.ui.components.label
import com.dfuzer.birdnote.ui.theme.DarkBlue
import com.dfuzer.birdnote.ui.theme.DisabledGrey
import com.dfuzer.birdnote.ui.theme.Neutral
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun SettingsScreen(
    noteNaming: NoteNaming,
    onNoteNamingChange: (NoteNaming) -> Unit,
    appLanguage: AppLanguage,
    onAppLanguageChange: (AppLanguage) -> Unit,
    preferredClefs: Set<Clef>,
    onPreferredClefToggle: (Clef) -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    soundEnabled: Boolean = true,
    onSoundEnabledChange: (Boolean) -> Unit = {},
) {
    val common = LayoutTuning.Common
    val tuning = LayoutTuning.Settings
    var showHelp by rememberSaveable { mutableStateOf(false) }
    var showLicenses by rememberSaveable { mutableStateOf(false) }
    if (showHelp) HelpDialog { showHelp = false }
    if (showLicenses) LicenseDialog { showLicenses = false }
    DecoratedScreen(modifier = modifier) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .heightIn(min = maxHeight)
                    .padding(
                        horizontal = common.screenHorizontalPadding,
                        vertical = common.screenVerticalPadding,
                    ),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = stringResource(R.string.settings),
                    style = MaterialTheme.typography.headlineLarge,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = common.titleBottomSpacing),
                )
                SettingsPanel(
                    noteNaming = noteNaming,
                    onNoteNamingChange = onNoteNamingChange,
                    appLanguage = appLanguage,
                    onAppLanguageChange = onAppLanguageChange,
                    preferredClefs = preferredClefs,
                    onPreferredClefToggle = onPreferredClefToggle,
                    soundEnabled = soundEnabled,
                    onSoundEnabledChange = onSoundEnabledChange,
                    onHowToPlay = { showHelp = true },
                    onLicenses = { showLicenses = true },
                    modifier = Modifier
                        .widthIn(max = tuning.panelMaxWidth)
                        .fillMaxWidth(),
                )
            }
        }
        BackButton(
            label = stringResource(R.string.back),
            onClick = onBackClick,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(common.backButtonMargin),
        )
    }
}

@Composable
private fun SettingsPanel(
    noteNaming: NoteNaming,
    onNoteNamingChange: (NoteNaming) -> Unit,
    appLanguage: AppLanguage,
    onAppLanguageChange: (AppLanguage) -> Unit,
    preferredClefs: Set<Clef>,
    onPreferredClefToggle: (Clef) -> Unit,
    soundEnabled: Boolean,
    onSoundEnabledChange: (Boolean) -> Unit,
    onHowToPlay: () -> Unit,
    onLicenses: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val tuning = LayoutTuning.Settings
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(tuning.cardCorner),
        colors = CardDefaults.cardColors(containerColor = Neutral),
        elevation = CardDefaults.cardElevation(defaultElevation = LayoutTuning.Common.buttonElevation),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min),
        ) {
            Column(
                modifier = Modifier
                    .weight(tuning.preferencesWeight)
                    .padding(horizontal = tuning.cardPadding, vertical = 6.dp),
            ) {
                ChoiceRow(
                    label = stringResource(R.string.language),
                    selected = appLanguage.label(),
                    options = AppLanguage.entries.map { it.label() },
                    onSelect = { onAppLanguageChange(AppLanguage.entries[it]) },
                )
                SettingsDivider()
                ChoiceRow(
                    label = stringResource(R.string.note_names),
                    selected = noteNaming.scalePreview(),
                    options = NoteNaming.entries.map { it.scalePreview() },
                    onSelect = { onNoteNamingChange(NoteNaming.entries[it]) },
                )
                SettingsDivider()
                ClefRow(
                    selected = preferredClefs,
                    onToggle = onPreferredClefToggle,
                )
                SettingsDivider()
                SoundRow(
                    enabled = soundEnabled,
                    onEnabledChange = onSoundEnabledChange,
                )
            }
            VerticalDivider(color = DarkBlue.copy(alpha = 0.12f))
            Column(
                modifier = Modifier
                    .weight(tuning.actionsWeight)
                    .fillMaxHeight()
                    .padding(horizontal = tuning.cardPadding),
                verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically),
            ) {
                SettingsActionButton(
                    text = stringResource(R.string.how_to_play),
                    onClick = onHowToPlay,
                )
                SettingsActionButton(
                    text = stringResource(R.string.licenses),
                    onClick = onLicenses,
                )
            }
        }
    }
}

@Composable
private fun SettingsActionButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val common = LayoutTuning.Common
    val tuning = LayoutTuning.Settings
    Button(
        onClick = onClick,
        shape = RoundedCornerShape(common.buttonCornerRadius),
        colors = ButtonDefaults.buttonColors(
            containerColor = DarkBlue,
            contentColor = Neutral,
        ),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = common.buttonElevation),
        contentPadding = PaddingValues(
            horizontal = common.buttonHorizontalPadding,
            vertical = common.buttonVerticalPadding,
        ),
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = tuning.actionMinHeight),
    ) {
        FittedText(text = text, style = MaterialTheme.typography.titleMedium)
    }
}

@Composable
private fun SettingsDivider() {
    HorizontalDivider(color = DarkBlue.copy(alpha = 0.12f))
}

@Composable
private fun ChoiceRow(
    label: String,
    selected: String,
    options: List<String>,
    onSelect: (Int) -> Unit,
) {
    var menuExpanded by rememberSaveable { mutableStateOf(false) }
    SettingRow(
        label = label,
        onClick = { menuExpanded = true },
    ) {
        BesideButtonMenu(
            expanded = menuExpanded,
            onDismissRequest = { menuExpanded = false },
            anchor = { ValueChip(text = selected) },
        ) {
            options.forEachIndexed { index, option ->
                DropdownMenuItem(
                    text = {
                        Text(
                            text = option,
                            style = MaterialTheme.typography.bodyLarge,
                        )
                    },
                    onClick = {
                        onSelect(index)
                        menuExpanded = false
                    },
                )
            }
        }
    }
}

@Composable
private fun ClefRow(
    selected: Set<Clef>,
    onToggle: (Clef) -> Unit,
) {
    var menuExpanded by rememberSaveable { mutableStateOf(false) }
    val summary = Clef.entries
        .filter { it in selected }
        .map { it.label() }
        .joinToString(", ")
    SettingRow(
        label = stringResource(R.string.clefs),
        onClick = { menuExpanded = true },
    ) {
        BesideButtonMenu(
            expanded = menuExpanded,
            onDismissRequest = { menuExpanded = false },
            anchor = { ValueChip(text = summary) },
        ) {
            Clef.entries.forEach { clef ->
                val on = clef in selected
                DropdownMenuItem(
                    text = {
                        Text(
                            text = clef.label(),
                            style = MaterialTheme.typography.bodyLarge,
                        )
                    },
                    trailingIcon = if (on) {
                        { Text(text = "✓", style = MaterialTheme.typography.bodyLarge) }
                    } else {
                        null
                    },
                    enabled = !on || selected.size > 1,
                    onClick = { onToggle(clef) },
                    modifier = Modifier.semantics { this.selected = on },
                )
            }
        }
    }
}

@Composable
private fun BesideButtonMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    anchor: @Composable () -> Unit,
    menu: @Composable ColumnScope.() -> Unit,
) {
    val density = LocalDensity.current
    Box {
        anchor()
        if (expanded) {
            Popup(
                popupPositionProvider = remember(density) { BesideButtonPositionProvider(density) },
                onDismissRequest = onDismissRequest,
                properties = PopupProperties(focusable = true),
            ) {
                Surface(
                    shape = MenuDefaults.shape,
                    color = MenuDefaults.containerColor,
                    tonalElevation = MenuDefaults.TonalElevation,
                    shadowElevation = MenuDefaults.ShadowElevation,
                ) {
                    Column(
                        modifier = Modifier
                            .padding(vertical = 8.dp)
                            .width(IntrinsicSize.Max)
                            .verticalScroll(rememberScrollState()),
                        content = menu,
                    )
                }
            }
        }
    }
}

/** Places the menu on the roomier side of the anchor so the button stays visible. */
private class BesideButtonPositionProvider(
    private val density: Density,
) : PopupPositionProvider {
    override fun calculatePosition(
        anchorBounds: IntRect,
        windowSize: IntSize,
        layoutDirection: LayoutDirection,
        popupContentSize: IntSize,
    ): IntOffset {
        val gap = with(density) { 8.dp.roundToPx() }
        val width = popupContentSize.width
        val spaceLeft = anchorBounds.left
        val spaceRight = windowSize.width - anchorBounds.right
        val leftX = anchorBounds.left - gap - width
        val rightX = anchorBounds.right + gap
        val leftFits = leftX >= 0
        val rightFits = rightX + width <= windowSize.width
        val x = when {
            rightFits && (!leftFits || spaceRight >= spaceLeft) -> rightX
            leftFits -> leftX
            spaceRight >= spaceLeft -> rightX
            else -> leftX
        }
        val maxY = (windowSize.height - popupContentSize.height).coerceAtLeast(0)
        val y = (anchorBounds.center.y - popupContentSize.height / 2).coerceIn(0, maxY)
        return IntOffset(x, y)
    }
}

@Composable
private fun SoundRow(
    enabled: Boolean,
    onEnabledChange: (Boolean) -> Unit,
) {
    val tuning = LayoutTuning.Settings
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = tuning.rowMinHeight)
            .toggleable(
                value = enabled,
                role = Role.Switch,
                onValueChange = onEnabledChange,
            )
            .padding(vertical = 4.dp),
    ) {
        Text(
            text = stringResource(R.string.sound_enabled),
            style = MaterialTheme.typography.titleMedium,
            color = DarkBlue,
            modifier = Modifier.weight(1f),
        )
        Switch(
            checked = enabled,
            onCheckedChange = null,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Neutral,
                checkedTrackColor = DarkBlue,
                checkedBorderColor = DarkBlue,
                uncheckedThumbColor = Neutral,
                uncheckedTrackColor = DisabledGrey,
                uncheckedBorderColor = DisabledGrey,
            ),
        )
    }
}

@Composable
private fun SettingRow(
    label: String,
    onClick: () -> Unit,
    value: @Composable () -> Unit,
) {
    val tuning = LayoutTuning.Settings
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = tuning.rowMinHeight)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(vertical = 4.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.titleMedium,
            color = DarkBlue,
            modifier = Modifier.weight(1f),
        )
        value()
    }
}

@Composable
private fun ValueChip(text: String) {
    val common = LayoutTuning.Common
    val tuning = LayoutTuning.Settings
    Surface(
        color = DarkBlue,
        contentColor = Neutral,
        shape = RoundedCornerShape(common.buttonCornerRadius),
        shadowElevation = common.buttonElevation,
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .heightIn(min = 36.dp)
                .widthIn(min = tuning.chipMinWidth, max = tuning.chipMaxWidth)
                .padding(horizontal = 10.dp, vertical = 4.dp),
        ) {
            FittedText(text = text, style = MaterialTheme.typography.bodyLarge)
        }
    }
}

@Composable
private fun LicenseDialog(onDismiss: () -> Unit) {
    val assets = LocalContext.current.assets
    var notice by rememberSaveable { mutableStateOf("") }
    LaunchedEffect(assets) {
        notice = withContext(Dispatchers.IO) {
            listOf(
                "ATTRIBUTIONS.txt",
                "licenses/SALAMANDER-CC-BY-3.0.txt",
                "licenses/AMARANTH-OFL.txt",
            ).joinToString("\n\n") { path ->
                assets.open(path).bufferedReader().use { it.readText() }
            }
        }
    }
    val linkedNotice = remember(notice) {
        buildAnnotatedString {
            append(notice)
            Regex("https?://[^\\s]+").findAll(notice).forEach { match ->
                addLink(LinkAnnotation.Url(match.value), match.range.first, match.range.last + 1)
            }
        }
    }
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.96f)
                .fillMaxHeight(0.94f),
            shape = AlertDialogDefaults.shape,
            color = AlertDialogDefaults.containerColor,
            tonalElevation = AlertDialogDefaults.TonalElevation,
        ) {
            Column(Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                Text(
                    stringResource(R.string.licenses),
                    style = MaterialTheme.typography.titleMedium,
                    color = AlertDialogDefaults.titleContentColor,
                )
                Text(
                    linkedNotice,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontSize = 14.3.sp,
                        lineHeight = 18.2.sp,
                    ),
                    color = AlertDialogDefaults.textContentColor,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(top = 6.dp, bottom = 4.dp),
                )
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .align(Alignment.End)
                        .heightIn(min = 32.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                ) {
                    Text(stringResource(R.string.close))
                }
            }
        }
    }
}
