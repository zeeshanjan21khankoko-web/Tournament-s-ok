package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.Tournament
import com.example.data.model.TournamentRegistration
import com.example.data.model.UserProfile
import com.example.ui.components.CyberCard
import com.example.ui.components.EmptyArenaState
import com.example.ui.components.PlayerAvatar
import com.example.ui.components.SlotProgressBar
import com.example.ui.components.StatusBadge
import com.example.ui.components.TournamentBannerImage
import com.example.ui.theme.AmberGold
import com.example.ui.theme.ElectricPurple
import com.example.ui.theme.GlassBorder
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonEmerald
import com.example.ui.theme.ObsidianCard
import com.example.ui.theme.ObsidianElevated
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.VoidBlack
import com.example.util.FormatUtils

@Composable
fun TournamentsScreen(
    tournaments: List<Tournament>,
    onSelectTournament: (Tournament) -> Unit
) {
    var query by remember { mutableStateOf("") }
    var filter by remember { mutableStateOf("ALL") }

    val filtered = remember(tournaments, query, filter) {
        tournaments.filter { t ->
            val matchesFilter = when (filter) {
                "LIVE" -> t.status == "LIVE"
                "UPCOMING" -> t.status == "UPCOMING"
                "COMPLETED" -> t.status == "COMPLETED"
                "FREE" -> t.entryFee == 0L
                else -> true
            }
            val q = query.trim()
            val matchesQuery = q.isBlank() ||
                t.title.contains(q, ignoreCase = true) ||
                t.gameName.contains(q, ignoreCase = true)
            matchesFilter && matchesQuery
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text("TOURNAMENTS", style = MaterialTheme.typography.headlineLarge, color = TextPrimary)
            Spacer(Modifier.height(10.dp))
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                placeholder = { Text("Search game or tournament name...") },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null, tint = NeonCyan) },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("tournament_search_input"),
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = NeonCyan,
                    unfocusedBorderColor = GlassBorder,
                    focusedContainerColor = ObsidianCard,
                    unfocusedContainerColor = ObsidianCard,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary
                )
            )
            Spacer(Modifier.height(10.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                val chips = listOf("ALL" to "All", "LIVE" to "Live", "UPCOMING" to "Upcoming", "COMPLETED" to "Completed", "FREE" to "Free Entry")
                items(chips) { (key, label) ->
                    FilterChip(
                        selected = filter == key,
                        onClick = { filter = key },
                        label = { Text(label) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = NeonCyan.copy(alpha = 0.2f),
                            selectedLabelColor = NeonCyan,
                            containerColor = ObsidianCard,
                            labelColor = TextSecondary
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = filter == key,
                            borderColor = GlassBorder,
                            selectedBorderColor = NeonCyan
                        )
                    )
                }
            }
        }

        if (filtered.isEmpty()) {
            item {
                EmptyArenaState(
                    title = "No Matching Tournaments",
                    subtitle = "Try clearing your search or switching tournament status filters."
                )
            }
        } else {
            items(filtered, key = { it.id }) { tournament ->
                TournamentListCard(
                    tournament = tournament,
                    onClick = { onSelectTournament(tournament) }
                )
            }
        }
        item { Spacer(Modifier.height(12.dp)) }
    }
}

@Composable
private fun TournamentListCard(
    tournament: Tournament,
    onClick: () -> Unit
) {
    CyberCard(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("tournament_card_${tournament.id}"),
        onClick = onClick
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(132.dp)
                .clip(RoundedCornerShape(12.dp))
        ) {
            TournamentBannerImage(
                bannerKey = tournament.bannerKey,
                contentDescription = tournament.title,
                modifier = Modifier.fillMaxSize()
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Brush.verticalGradient(listOf(Color.Transparent, VoidBlack.copy(alpha = 0.88f))))
            )
            StatusBadge(
                status = tournament.status,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(10.dp)
            )
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = VoidBlack.copy(alpha = 0.78f),
                border = BorderStroke(1.dp, NeonCyan.copy(alpha = 0.5f)),
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(10.dp)
            ) {
                Text(
                    text = tournament.gameName,
                    style = MaterialTheme.typography.labelMedium,
                    color = NeonCyan,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(10.dp)
            ) {
                Text(
                    text = tournament.title,
                    style = MaterialTheme.typography.headlineMedium,
                    color = TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Schedule, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(tournament.matchTimeLabel, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("ENTRY FEE", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                Text(
                    text = if (tournament.entryFee == 0L) "FREE" else FormatUtils.formatPkr(tournament.entryFee),
                    style = MaterialTheme.typography.titleMedium,
                    color = NeonCyan
                )
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("PRIZE POOL", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                Text(
                    text = FormatUtils.formatPkr(tournament.prizePool),
                    style = MaterialTheme.typography.titleMedium,
                    color = NeonEmerald
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text("SLOTS", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                Text(
                    text = "${tournament.filledSlots}/${tournament.maxSlots}",
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary
                )
            }
        }

        Spacer(Modifier.height(10.dp))
        SlotProgressBar(filled = tournament.filledSlots, max = tournament.maxSlots)

        if (!tournament.winnerUsername.isNullOrBlank()) {
            Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.EmojiEvents, contentDescription = null, tint = AmberGold, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text(
                    text = "Winner: ${tournament.winnerUsername} (${tournament.winnerPlayerId.orEmpty()})",
                    style = MaterialTheme.typography.labelMedium,
                    color = AmberGold
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TournamentDetailSheet(
    tournament: Tournament,
    registrations: List<TournamentRegistration>,
    profile: UserProfile,
    isAdmin: Boolean,
    isActionLoading: Boolean,
    onDismiss: () -> Unit,
    onJoin: () -> Unit,
    onGoToDeposit: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val isRegistered = registrations.any { it.userId == profile.userId }
    var showConfirmJoin by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = ObsidianSurface,
        contentColor = TextPrimary
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                StatusBadge(status = tournament.status)
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Filled.Close, contentDescription = "Close details", tint = TextSecondary)
                }
            }

            Spacer(Modifier.height(6.dp))
            Text(tournament.title, style = MaterialTheme.typography.headlineLarge, color = TextPrimary)
            Text("${tournament.gameName} • ${tournament.matchTimeLabel}", style = MaterialTheme.typography.bodyLarge, color = NeonCyan)

            Spacer(Modifier.height(14.dp))

            CyberCard(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("ENTRY FEE", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                        Text(
                            if (tournament.entryFee == 0L) "FREE" else FormatUtils.formatPkr(tournament.entryFee),
                            style = MaterialTheme.typography.titleLarge,
                            color = NeonCyan
                        )
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("PRIZE POOL", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                        Text(FormatUtils.formatPkr(tournament.prizePool), style = MaterialTheme.typography.titleLarge, color = NeonEmerald)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("AVAILABLE", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                        Text("${tournament.availableSlots} / ${tournament.maxSlots}", style = MaterialTheme.typography.titleLarge, color = TextPrimary)
                    }
                }
                Spacer(Modifier.height(12.dp))
                SlotProgressBar(filled = tournament.filledSlots, max = tournament.maxSlots)
            }

            if (!tournament.winnerUsername.isNullOrBlank()) {
                Spacer(Modifier.height(12.dp))
                CyberCard(
                    modifier = Modifier.fillMaxWidth(),
                    accentBrush = Brush.linearGradient(listOf(AmberGold, ElectricPurple))
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.EmojiEvents, contentDescription = null, tint = AmberGold, modifier = Modifier.size(28.dp))
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text("TOURNAMENT CHAMPION", style = MaterialTheme.typography.labelSmall, color = AmberGold)
                            Text("${tournament.winnerUsername} (${tournament.winnerPlayerId.orEmpty()})", style = MaterialTheme.typography.titleLarge, color = TextPrimary)
                        }
                    }
                }
            }

            if (isRegistered || isAdmin) {
                Spacer(Modifier.height(12.dp))
                CyberCard(
                    modifier = Modifier.fillMaxWidth(),
                    accentBrush = Brush.linearGradient(listOf(NeonEmerald, NeonCyan))
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.VpnKey, contentDescription = null, tint = NeonEmerald, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("MATCH ROOM CREDENTIALS", style = MaterialTheme.typography.labelMedium, color = NeonEmerald)
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = tournament.roomId?.takeIf { it.isNotBlank() }
                            ?: "Room ID & Password will be published by admin 15 minutes before match time.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = TextPrimary
                    )
                }
            }

            Spacer(Modifier.height(14.dp))
            Text("RULES & FORMAT", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
            Spacer(Modifier.height(4.dp))
            Text(
                text = tournament.rulesSummary.ifBlank { "Standard fair-play esports rules apply. No emulators or third-party tools allowed." },
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )

            Spacer(Modifier.height(16.dp))

            // Join CTA
            when {
                isRegistered -> {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        color = NeonEmerald.copy(alpha = 0.16f),
                        border = BorderStroke(1.dp, NeonEmerald)
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = NeonEmerald)
                            Spacer(Modifier.width(8.dp))
                            Text("You are registered for this tournament", style = MaterialTheme.typography.labelLarge, color = NeonEmerald)
                        }
                    }
                }
                tournament.status == "COMPLETED" -> {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        color = ObsidianElevated
                    ) {
                        Text(
                            "Tournament Completed",
                            modifier = Modifier.padding(14.dp),
                            style = MaterialTheme.typography.labelLarge,
                            color = TextSecondary
                        )
                    }
                }
                tournament.availableSlots <= 0L -> {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        color = ObsidianElevated
                    ) {
                        Text(
                            "All Slots Filled",
                            modifier = Modifier.padding(14.dp),
                            style = MaterialTheme.typography.labelLarge,
                            color = AmberGold
                        )
                    }
                }
                profile.walletBalance < tournament.entryFee -> {
                    Column {
                        Text(
                            "Insufficient wallet balance (${FormatUtils.formatPkr(profile.walletBalance)} available, ${FormatUtils.formatPkr(tournament.entryFee)} required).",
                            style = MaterialTheme.typography.bodyMedium,
                            color = AmberGold
                        )
                        Spacer(Modifier.height(8.dp))
                        OutlinedButton(
                            onClick = {
                                onDismiss()
                                onGoToDeposit()
                            },
                            modifier = Modifier.fillMaxWidth().height(50.dp),
                            border = BorderStroke(1.dp, NeonCyan)
                        ) {
                            Text("Deposit Funds to Join", color = NeonCyan, fontWeight = FontWeight.Bold)
                        }
                    }
                }
                else -> {
                    Button(
                        onClick = { showConfirmJoin = true },
                        enabled = !isActionLoading,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("join_tournament_button"),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = NeonCyan, contentColor = VoidBlack)
                    ) {
                        Text(
                            text = if (tournament.entryFee == 0L) "Join Free Tournament" else "Join Now • ${FormatUtils.formatPkr(tournament.entryFee)}",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(Modifier.height(18.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Group, contentDescription = null, tint = ElectricPurple, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("REGISTERED PLAYERS (${registrations.size})", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
            }
            Spacer(Modifier.height(8.dp))

            if (registrations.isEmpty()) {
                Text("No players registered yet. Be the first to claim a slot!", style = MaterialTheme.typography.bodyMedium, color = TextMuted)
            } else {
                registrations.forEachIndexed { idx, reg ->
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        shape = RoundedCornerShape(12.dp),
                        color = ObsidianCard,
                        border = BorderStroke(1.dp, GlassBorder)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("#${idx + 1}", style = MaterialTheme.typography.labelMedium, color = NeonCyan, modifier = Modifier.width(30.dp))
                            PlayerAvatar(photoBase64 = reg.photoBase64, contentDescription = reg.username, size = 34.dp)
                            Spacer(Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(reg.username, style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                                Text(reg.playerId, style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                            }
                            if (!reg.placement.isNullOrBlank()) {
                                StatusBadge(status = reg.placement)
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(28.dp))
        }
    }

    if (showConfirmJoin) {
        AlertDialog(
            onDismissRequest = { showConfirmJoin = false },
            containerColor = ObsidianCard,
            titleContentColor = TextPrimary,
            textContentColor = TextSecondary,
            icon = { Icon(Icons.Filled.Lock, contentDescription = null, tint = NeonCyan) },
            title = { Text("Confirm Tournament Entry") },
            text = {
                Text(
                    if (tournament.entryFee == 0L) "Reserve your free slot in ${tournament.title}?"
                    else "${FormatUtils.formatPkr(tournament.entryFee)} will be deducted from your wallet to lock your slot in ${tournament.title}."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showConfirmJoin = false
                        onJoin()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NeonCyan, contentColor = VoidBlack)
                ) {
                    Text("Confirm & Join", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmJoin = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            }
        )
    }
}
