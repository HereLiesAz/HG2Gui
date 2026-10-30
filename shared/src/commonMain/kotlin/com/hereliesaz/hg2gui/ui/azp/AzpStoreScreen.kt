package com.hereliesaz.hg2gui.ui.azp

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hereliesaz.hg2gui.ui.AzphaltChip
import com.hereliesaz.hg2gui.ui.AzphaltHeading
import com.hereliesaz.hg2gui.ui.ConfirmDialog
import com.hereliesaz.hg2gui.ui.StatusKind
import com.hereliesaz.hg2gui.ui.liveRegionPolite
import com.hereliesaz.hg2gui.ui.maxContentWidth
import com.hereliesaz.hg2gui.ui.minTouchSize
import com.hereliesaz.hg2gui.ui.screenInsets
import com.hereliesaz.hg2gui.ui.statusColor
import com.hereliesaz.hg2gui.ui.headingRole
import com.hereliesaz.hg2gui.ui.menu.Azphalt
import com.hereliesaz.hg2gui.ui.menu.onPage
import com.hereliesaz.hg2gui.ui.menu.pageBrush
import com.hereliesaz.hg2gui.ui.theme.AzphaltSurface
import com.hereliesaz.hg2gui.ui.theme.LocalAzphaltType
import hg2gui.shared.generated.resources.Res
import hg2gui.shared.generated.resources.azp_awaiting_bootstrap
import hg2gui.shared.generated.resources.azp_back
import hg2gui.shared.generated.resources.azp_back_description
import hg2gui.shared.generated.resources.azp_empty_idle
import hg2gui.shared.generated.resources.azp_empty_no_results
import hg2gui.shared.generated.resources.azp_empty_searching
import hg2gui.shared.generated.resources.azp_install
import hg2gui.shared.generated.resources.azp_install_description
import hg2gui.shared.generated.resources.azp_install_failed_prefix
import hg2gui.shared.generated.resources.azp_installed
import hg2gui.shared.generated.resources.azp_installed_description
import hg2gui.shared.generated.resources.azp_installing
import hg2gui.shared.generated.resources.azp_installing_description
import hg2gui.shared.generated.resources.azp_remove
import hg2gui.shared.generated.resources.azp_remove_confirm
import hg2gui.shared.generated.resources.azp_remove_description
import hg2gui.shared.generated.resources.azp_remove_message
import hg2gui.shared.generated.resources.azp_remove_title
import hg2gui.shared.generated.resources.azp_script_command
import hg2gui.shared.generated.resources.azp_search
import hg2gui.shared.generated.resources.azp_search_busy
import hg2gui.shared.generated.resources.azp_search_description
import hg2gui.shared.generated.resources.azp_search_placeholder
import hg2gui.shared.generated.resources.azp_section_installed
import hg2gui.shared.generated.resources.azp_section_registry
import hg2gui.shared.generated.resources.azp_title
import hg2gui.shared.generated.resources.azp_trust_trusted
import hg2gui.shared.generated.resources.azp_trust_unchecked
import hg2gui.shared.generated.resources.azp_trust_unknown
import hg2gui.shared.generated.resources.azp_trust_unsigned
import hg2gui.shared.generated.resources.azp_trust_unverifiable
import hg2gui.shared.generated.resources.azp_trust_valid
import hg2gui.shared.generated.resources.azp_unverified_cancel
import hg2gui.shared.generated.resources.azp_unverified_confirm
import hg2gui.shared.generated.resources.azp_unverified_message
import hg2gui.shared.generated.resources.azp_unverified_title
import hg2gui.shared.generated.resources.azp_version_line
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

/** A search result row, or an already-installed package - shared shape for the list.
 *  [trust] mirrors androidMain's `AzpTrust` enum by name (commonMain can't reference it directly,
 *  since it's platform-specific) - blank until [installed] is true, since trust is only known
 *  once a package's signature has actually been checked. */
data class AzpListing(
    val id: String,
    val name: String,
    val author: String,
    val description: String,
    val version: String,
    val kind: String,
    val installed: Boolean,
    val trust: String = "",
    /** For an installed `kind:"script"` package - the name it's callable as on PATH once
     *  `ScriptInstaller` has resolved its dependencies. Blank if not a script, not yet installed,
     *  or the host has no Termux bootstrap to wire it onto PATH with yet. */
    val scriptCommand: String = "",
)

private val KINDS = listOf("all", "skill", "mcp", "code", "pack", "asset", "app", "script")

private const val PLACEHOLDER_ALPHA = 0.7f

/** The registry kind whose text is folded into the AI chat's system prompt - see [azpNeedsInstallConfirmation]. */
private const val KIND_SKILL = "skill"

/**
 * Installed packages to list above the search results: the ones the registry search did not
 * return (it was not run yet, failed, or the package is gone from the registry), narrowed by the
 * same [kind] chip and [query] text the user has set, so installed packages are visible and
 * removable without searching first. A package already present in [results] is not repeated.
 */
internal fun azpInstalledToShow(
    installed: List<AzpListing>,
    results: List<AzpListing>,
    query: String,
    kind: String
): List<AzpListing> {
    val resultIds = results.mapTo(HashSet()) { it.id }
    val needle = query.trim()
    return installed
        .filter { it.id !in resultIds }
        .filter { kind == "all" || it.kind == kind }
        .filter {
            needle.isEmpty() ||
                it.name.contains(needle, ignoreCase = true) ||
                it.id.contains(needle, ignoreCase = true) ||
                it.description.contains(needle, ignoreCase = true) ||
                it.author.contains(needle, ignoreCase = true)
        }
        .map { it.copy(installed = true) }
}

/**
 * Whether the store itself asks "install this?" before handing [listing] to `onInstall`. A skill
 * is prose that gets folded into the AI chat's system prompt, so a hostile one can steer the
 * commands the AI suggests - and its signature is only checked after the download, which nothing
 * below this screen can retroactively undo. Scripts already pause for their own confirmation
 * further down the install path, and the other kinds are inert downloads.
 */
internal fun azpNeedsInstallConfirmation(listing: AzpListing): Boolean = listing.kind == KIND_SKILL && !listing.installed

/** What the list area shows when it has no rows. */
internal enum class AzpEmptyState {
    /** Rows are showing, or an error banner already explains the emptiness. */
    None,

    /** A search is running and nothing has come back yet. */
    Searching,

    /** Nothing has been searched yet. */
    Idle,

    /** A search finished and matched nothing. */
    NoResults
}

internal fun azpEmptyState(hasRows: Boolean, busy: Boolean, searched: Boolean, hasError: Boolean): AzpEmptyState = when {
    hasRows -> AzpEmptyState.None
    busy -> AzpEmptyState.Searching
    hasError -> AzpEmptyState.None
    !searched -> AzpEmptyState.Idle
    else -> AzpEmptyState.NoResults
}

/** Row title: the body face at a list-row size. */
@Composable
private fun rowTitleStyle(): TextStyle =
    LocalAzphaltType.current.body.copy(fontSize = 15.sp, lineHeight = 20.sp, fontWeight = FontWeight.ExtraBold)

/** Row description and message text: the body face at caption size. */
@Composable
private fun captionStyle(): TextStyle = LocalAzphaltType.current.body.copy(fontSize = 13.sp, lineHeight = 18.sp)

/**
 * Browses the azphalt registry (spec/repository-api.md) - the same `pkg`/`apt` idiom, but for
 * `.azp` extensions: search, filter by kind, install. HG2Gui has no `.azp` code/WASM execution
 * runtime, so "install" downloads + unpacks the archive for most kinds - but two do something
 * further: `kind:"skill"` packages fold their SKILL.md into the AI chat's system prompt (see
 * AiClient/AzpLibrary), and `kind:"script"` packages resolve their declared Termux dependencies
 * and wire an executable wrapper onto PATH (see ScriptInstaller). Every other kind is downloaded
 * for the user's own use elsewhere, same as `apt download`.
 *
 * The list area is never blank: before any search it says how to start, while one runs it says
 * so, and an empty answer says "no results". [installed] lists what is already on the device so it
 * can be seen and removed without searching.
 */
@Composable
fun AzpStoreScreen(
    fullscreen: Boolean,
    results: List<AzpListing>,
    busy: Boolean,
    installingId: String?,
    onSearch: (query: String, kind: String) -> Unit,
    onInstall: (AzpListing) -> Unit,
    onBack: () -> Unit,
    /** Removes an installed package (wrapper, files, record); null hides the REMOVE capsule. */
    onUninstall: ((AzpListing) -> Unit)? = null,
    /** Last search failure, shown above the results; null when the last search succeeded. */
    error: String? = null,
    /** Why the last install failed (download, archive or signature problem); null when it did not. */
    installError: String? = null,
    /** Packages already on the device, listed even before (or without) a search. */
    installed: List<AzpListing> = emptyList(),
    /**
     * Whether the store asks for confirmation before calling [onInstall] for a listing. The
     * default asks for skills (see [azpNeedsInstallConfirmation]); pass `{ false }` if the caller
     * already confirms.
     */
    needsInstallConfirmation: (AzpListing) -> Boolean = ::azpNeedsInstallConfirmation,
) {
    var query by remember { mutableStateOf("") }
    var kind by remember { mutableStateOf("all") }
    var searched by remember { mutableStateOf(false) }
    // Removing deletes the package's files and PATH wrapper - one stray tap shouldn't do that.
    var confirmRemove by remember { mutableStateOf<AzpListing?>(null) }
    var confirmInstall by remember { mutableStateOf<AzpListing?>(null) }

    val search = { searchKind: String ->
        searched = true
        onSearch(query.trim(), searchKind)
    }
    val requestInstall: (AzpListing) -> Unit = { listing ->
        if (needsInstallConfirmation(listing)) confirmInstall = listing else onInstall(listing)
    }
    val extraInstalled = azpInstalledToShow(installed, results, query, kind)
    val emptyState = azpEmptyState(
        hasRows = results.isNotEmpty() || extraInstalled.isNotEmpty(),
        busy = busy,
        searched = searched,
        hasError = error != null
    )

    Box(
        Modifier
            .fillMaxSize()
            .background(Azphalt.currentGround.pageBrush())
            .screenInsets(fullscreen)
    ) {
        Column(Modifier.fillMaxHeight().maxContentWidth()) {
            Row(
                Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AzphaltChip(
                    label = stringResource(Res.string.azp_back),
                    onClick = onBack,
                    contentDescription = stringResource(Res.string.azp_back_description)
                )
            }

            AzphaltHeading(
                stringResource(Res.string.azp_title),
                Modifier.padding(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 4.dp)
            )

            Row(
                // Scrolls rather than clipping the last kind chips on a narrow screen.
                Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .selectableGroup()
                    .padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                KINDS.forEach { k ->
                    AzphaltChip(
                        label = k,
                        // Gated on !busy the same as the search field and SEARCH button below -
                        // without it, tapping a second kind while the first search is still in
                        // flight let two unsequenced requests race (see onSearch's own caller).
                        onClick = {
                            kind = k
                            search(k)
                        },
                        selected = k == kind,
                        enabled = !busy,
                        role = Role.RadioButton
                    )
                }
            }

            val searchLabel = stringResource(Res.string.azp_search_description)
            val fieldStyle = captionStyle().copy(color = Azphalt.Yellow)
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    Modifier
                        .weight(1f)
                        .heightIn(min = minTouchSize())
                        .clip(AzphaltSurface.capsule)
                        .background(Azphalt.Ink)
                        .padding(start = 16.dp, end = 12.dp, top = 6.dp, bottom = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    BasicTextField(
                        value = query,
                        onValueChange = { query = it },
                        modifier = Modifier.weight(1f).semantics { contentDescription = searchLabel },
                        textStyle = fieldStyle,
                        cursorBrush = SolidColor(Azphalt.Yellow),
                        singleLine = true,
                        enabled = !busy,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(onSearch = { search(kind) }),
                        decorationBox = { inner ->
                            Box(contentAlignment = Alignment.CenterStart) {
                                if (query.isEmpty()) {
                                    Text(
                                        stringResource(Res.string.azp_search_placeholder),
                                        color = Azphalt.Yellow.copy(alpha = PLACEHOLDER_ALPHA),
                                        style = fieldStyle
                                    )
                                }
                                inner()
                            }
                        }
                    )
                }
                AzphaltChip(
                    label = if (busy) "…" else stringResource(Res.string.azp_search),
                    onClick = { search(kind) },
                    enabled = !busy,
                    tint = Azphalt.hues[6],
                    contentDescription = if (busy) stringResource(Res.string.azp_search_busy) else null
                )
            }

            // Failures are announced, not just drawn: a polite live region around both banners.
            Column(Modifier.liveRegionPolite()) {
                if (error != null) {
                    StatusLine(error)
                }
                if (installError != null) {
                    StatusLine(stringResource(Res.string.azp_install_failed_prefix, installError))
                }
            }

            when (emptyState) {
                AzpEmptyState.None -> Unit
                AzpEmptyState.Searching -> EmptyNote(stringResource(Res.string.azp_empty_searching))
                AzpEmptyState.Idle -> EmptyNote(stringResource(Res.string.azp_empty_idle))
                AzpEmptyState.NoResults -> EmptyNote(stringResource(Res.string.azp_empty_no_results))
            }

            LazyColumn(
                modifier = Modifier.weight(1f).fillMaxWidth().padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(bottom = 12.dp)
            ) {
                if (extraInstalled.isNotEmpty()) {
                    item { SectionLabel(stringResource(Res.string.azp_section_installed)) }
                    items(extraInstalled) { listing ->
                        AzpRow(listing, installingId == listing.id, requestInstall, onUninstall?.let { { picked: AzpListing -> confirmRemove = picked } })
                    }
                    if (results.isNotEmpty()) item { SectionLabel(stringResource(Res.string.azp_section_registry)) }
                }
                items(results) { listing ->
                    AzpRow(listing, installingId == listing.id, requestInstall, onUninstall?.let { { picked: AzpListing -> confirmRemove = picked } })
                }
            }
        }
    }

    val removing = confirmRemove
    if (removing != null && onUninstall != null) {
        ConfirmDialog(
            title = stringResource(Res.string.azp_remove_title, removing.name).uppercase(),
            message = stringResource(Res.string.azp_remove_message),
            confirmLabel = stringResource(Res.string.azp_remove_confirm),
            onConfirm = { confirmRemove = null; onUninstall(removing) },
            onDismiss = { confirmRemove = null }
        )
    }

    val installing = confirmInstall
    if (installing != null) {
        ConfirmDialog(
            title = stringResource(Res.string.azp_unverified_title),
            message = stringResource(Res.string.azp_unverified_message, installing.name, installing.author),
            confirmLabel = stringResource(Res.string.azp_unverified_confirm),
            cancelLabel = stringResource(Res.string.azp_unverified_cancel),
            onConfirm = { confirmInstall = null; onInstall(installing) },
            onDismiss = { confirmInstall = null }
        )
    }
}

/** An error line: the colour is a status colour, but the words (the caller's message) carry the meaning. */
@Composable
private fun StatusLine(text: String) {
    Text(
        text,
        color = statusColor(StatusKind.Error),
        style = captionStyle(),
        modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)
    )
}

@Composable
private fun EmptyNote(text: String) {
    Text(
        text,
        color = statusColor(StatusKind.Muted),
        style = captionStyle(),
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp).liveRegionPolite()
    )
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text,
        color = Azphalt.currentGround.onPage,
        style = LocalAzphaltType.current.eyebrow,
        modifier = Modifier.padding(top = 4.dp).headingRole()
    )
}

@Composable
private fun AzpRow(
    listing: AzpListing,
    installing: Boolean,
    onInstall: (AzpListing) -> Unit,
    onUninstall: ((AzpListing) -> Unit)?
) {
    val onPage = Azphalt.currentGround.onPage
    val muted = statusColor(StatusKind.Muted)
    Column(
        Modifier
            .fillMaxWidth()
            .clip(AzphaltSurface.recordTile)
            .background(Azphalt.Ink.copy(alpha = .05f))
            .padding(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(listing.name, color = onPage, style = rowTitleStyle(), modifier = Modifier.weight(1f, fill = false))
            Spacer(Modifier.width(8.dp))
            Text(listing.kind.uppercase(), color = muted, style = LocalAzphaltType.current.micro)
        }
        if (listing.description.isNotBlank()) {
            Text(
                listing.description,
                color = muted, style = captionStyle(),
                modifier = Modifier.padding(top = 2.dp)
            )
        }
        Text(
            stringResource(Res.string.azp_version_line, listing.author, listing.version),
            color = muted, style = captionStyle(),
            modifier = Modifier.padding(top = 2.dp)
        )
        if (listing.installed && listing.trust.isNotBlank()) {
            Text(
                stringResource(trustLabelRes(listing.trust)),
                color = trustColor(listing.trust),
                style = LocalAzphaltType.current.micro,
                modifier = Modifier.padding(top = 3.dp)
            )
        } else if (!listing.installed) {
            // AZP-7: signature verification needs the actual downloaded manifest.json and
            // signature.json bytes - the repository's search API never returns anything
            // signature-related, so there is no real trust verdict to show before installing.
            // Rather than say nothing (which reads as "already vetted" by omission), this
            // states that plainly: trust is unknown until install, not silently assumed good.
            Text(
                stringResource(Res.string.azp_trust_unknown),
                color = muted,
                style = LocalAzphaltType.current.micro,
                modifier = Modifier.padding(top = 3.dp)
            )
        }
        if (listing.installed && listing.kind == "script") {
            Text(
                if (listing.scriptCommand.isNotBlank()) {
                    stringResource(Res.string.azp_script_command, listing.scriptCommand)
                } else {
                    stringResource(Res.string.azp_awaiting_bootstrap)
                },
                color = muted,
                style = LocalAzphaltType.current.micro,
                modifier = Modifier.padding(top = 3.dp)
            )
        }
        Row(
            Modifier.padding(top = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (listing.installed && onUninstall != null) {
                AzphaltChip(
                    label = stringResource(Res.string.azp_remove),
                    onClick = { onUninstall(listing) },
                    enabled = !installing,
                    contentDescription = stringResource(Res.string.azp_remove_description, listing.name)
                )
            }
            // A capsule is never tinted or faded by hand (style guide "03 - Transparency"):
            // AzphaltChip's own disabled treatment is the one idle look, used for "already installed".
            AzphaltChip(
                label = when {
                    listing.installed -> stringResource(Res.string.azp_installed)
                    installing -> stringResource(Res.string.azp_installing)
                    else -> stringResource(Res.string.azp_install)
                },
                onClick = { onInstall(listing) },
                enabled = !listing.installed && !installing,
                contentDescription = when {
                    listing.installed -> stringResource(Res.string.azp_installed_description, listing.name)
                    installing -> stringResource(Res.string.azp_installing_description, listing.name)
                    else -> stringResource(Res.string.azp_install_description, listing.name)
                }
            )
        }
    }
}

/** Maps an androidMain `AzpTrust` enum name to the short badge shown under an installed row. */
private fun trustLabelRes(trust: String): StringResource = when (trust) {
    "TRUSTED" -> Res.string.azp_trust_trusted
    "VALID" -> Res.string.azp_trust_valid
    "UNVERIFIABLE" -> Res.string.azp_trust_unverifiable
    "UNSIGNED" -> Res.string.azp_trust_unsigned
    "UNCHECKED" -> Res.string.azp_trust_unchecked
    else -> Res.string.azp_trust_unchecked
}

/** Single source of truth for "is this the TRUSTED tier" - [trustLabelRes] and this must never
 *  disagree about which string means trusted, so both read the same comparison. */
@Composable
private fun trustColor(trust: String): Color =
    if (trust == "TRUSTED") statusColor(StatusKind.Success) else statusColor(StatusKind.Muted)
