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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.ArrowOutward
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.MilitaryTech
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.SportsEsports
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.Tournament
import com.example.data.model.UserProfile
import com.example.data.model.WalletTransaction
import com.example.data.model.WinnerRecord
import com.example.ui.components.CyberCard
import com.example.ui.components.EmptyArenaState
import com.example.ui.components.PlayerAvatar
import com.example.ui.components.SlotProgressBar
import com.example.ui.components.StatusBadge
import com.example.ui.components.TournamentBannerImage
import com.example.ui.components.TransactionRow
import com.example.ui.theme.AmberGold
import com.example.ui.theme.ElectricPurple
import com.example.ui.theme.GlassBorder
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonEmerald
import com.example.ui.theme.ObsidianCard
import com.example.ui.theme.ObsidianElevated
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.VoidBlack
import com.example.util.FormatUtils

@Composable
fun HomeDashboardScreen(
    profile: UserProfile,
    isAdmin: Boolean,
    tournaments: List<Tournament>,
    transactions: List<WalletTransaction>,
    winners: List<WinnerRecord>,
    onOpenTournament: (Tournament) -> Unit,
    onNavigateTournaments: () -> Unit,
    onNavigateDeposit: () -> Unit,
    onNavigateWithdraw: () -> Unit,
    onNavigateWallet: () -> Unit,
    onNavigateProfile: () -> Unit,
    onNavigateAdmin: () -> Unit
) {
    val activeTournaments = tournaments.filter { it.status == "LIVE" || it.status == "UPCOMING" }
    val winRate = if (profile.matchesPlayed > 0) {
        ((profile.wins.toFloat() / profile.matchesPlayed.toFloat()) * 100).toInt()
    } else 0

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Gamer Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    onClick = onNavigateProfile,
                    color = Color.Transparent,
                    modifier = Modifier.testTag("home_profile_header")
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        PlayerAvatar(
                            photoBase64 = profile.photoBase64,
                            contentDescription = "Player avatar",
                            size = 50.dp
                        )
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = profile.username,
                                    style = MaterialTheme.typography.titleLarge,
                                    color = TextPrimary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                if (isAdmin) {
                                    Spacer(Modifier.width(8.dp))
                                    StatusBadge(status = "ADMIN")
                                }
                            }
                            Text(
                                text = "ID: ${profile.playerId}",
                                style = MaterialTheme.typography.labelMedium,
                                color = NeonCyan
                            )
                        }
                    }
                }
                Spacer(Modifier.weight(1f))
                if (isAdmin) {
                    Surface(
                        onClick = onNavigateAdmin,
                        shape = RoundedCornerShape(12.dp),
                        color = ElectricPurple.copy(alpha = 0.2f),
                        border = BorderStroke(1.dp, ElectricPurple),
                        modifier = Modifier.testTag("home_admin_shortcut")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Filled.Shield, contentDescription = "Admin", tint = ElectricPurple, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Admin", style = MaterialTheme.typography.labelMedium, color = TextPrimary)
                        }
                    }
                }
            }
        }

        // 2. Neon Wallet & Stats Hero Card
        item {
            CyberCard(
                modifier = Modifier.fillMaxWidth(),
                accentBrush = Brush.linearGradient(listOf(NeonCyan, ElectricPurple)),
                containerColor = ObsidianCard
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "AVAILABLE WALLET BALANCE",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = FormatUtils.formatPkr(profile.walletBalance),
                            style = MaterialTheme.typography.displayLarge,
                            color = NeonCyan,
                            modifier = Modifier.testTag("home_wallet_balance")
                        )
                    }
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(
                                Brush.linearGradient(listOf(NeonCyan.copy(alpha = 0.22f), ElectricPurple.copy(alpha = 0.22f)))
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Filled.AccountBalanceWallet,
                            contentDescription = "Wallet",
                            tint = NeonCyan,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                }

                Spacer(Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = onNavigateDeposit,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("home_deposit_button"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = NeonCyan, contentColor = VoidBlack)
                    ) {
                        Icon(Icons.Filled.AddCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Deposit", fontWeight = FontWeight.Bold)
                    }
                    OutlinedButton(
                        onClick = onNavigateWithdraw,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("home_withdraw_button"),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, ElectricPurple)
                    ) {
                        Icon(Icons.Filled.ArrowOutward, contentDescription = null, tint = ElectricPurple, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Withdraw", color = TextPrimary, fontWeight = FontWeight.SemiBold)
                    }
                }

                Spacer(Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    StatTile(
                        label = "Matches",
                        value = profile.matchesPlayed.toString(),
                        icon = Icons.Filled.SportsEsports,
                        tint = NeonCyan,
                        modifier = Modifier.weight(1f)
                    )
                    StatTile(
                        label = "Wins",
                        value = profile.wins.toString(),
                        icon = Icons.Filled.EmojiEvents,
                        tint = AmberGold,
                        modifier = Modifier.weight(1f)
                    )
                    StatTile(
                        label = "Win Rate",
                        value = "$winRate%",
                        icon = Icons.Filled.MilitaryTech,
                        tint = NeonEmerald,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // 3. Live & Upcoming Tournaments
        item {
            SectionHeader(
                title = "LIVE & UPCOMING ARENAS",
                actionLabel = "See All",
                onAction = onNavigateTournaments
            )
        }

        item {
            if (activeTournaments.isEmpty()) {
                EmptyArenaState(
                    title = "No Active Tournaments Yet",
                    subtitle = if (isAdmin) "Head to the Admin tab to launch your first tournament." else "New esports tournaments will appear here soon."
                )
            } else {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(activeTournaments.take(8), key = { it.id }) { tournament ->
                        FeaturedTournamentCard(
                            tournament = tournament,
                            onClick = { onOpenTournament(tournament) }
                        )
                    }
                }
            }
        }

        // 4. Recent Champions
        if (winners.isNotEmpty()) {
            item {
                SectionHeader(title = "RECENT CHAMPIONS")
            }
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(winners.take(6), key = { it.id }) { win ->
                        CyberCard(
                            modifier = Modifier.width(220.dp),
                            accentBrush = Brush.linearGradient(listOf(AmberGold.copy(alpha = 0.6f), ElectricPurple.copy(alpha = 0.4f)))
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.EmojiEvents, contentDescription = null, tint = AmberGold, modifier = Modifier.size(20.dp))
                                Spacer(Modifier.width(6.dp))
                                Text(win.placement, style = MaterialTheme.typography.labelMedium, color = AmberGold)
                            }
                            Spacer(Modifier.height(6.dp))
                            Text(win.username, style = MaterialTheme.typography.titleLarge, color = TextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text("${win.tournamentTitle} • ${win.gameName}", style = MaterialTheme.typography.bodyMedium, color = TextSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Spacer(Modifier.height(6.dp))
                            Text(FormatUtils.formatPkr(win.prizeAmount), style = MaterialTheme.typography.labelMedium, color = NeonEmerald, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // 5. Recent Transactions
        item {
            SectionHeader(
                title = "RECENT TRANSACTIONS",
                actionLabel = "Full Ledger",
                onAction = onNavigateWallet
            )
        }

        if (transactions.isEmpty()) {
            item {
                EmptyArenaState(
                    title = "No Transactions Yet",
                    subtitle = "Deposit funds via JazzCash, EasyPaisa, or Bank Transfer to join paid tournaments.",
                    icon = Icons.Filled.AccountBalanceWallet
                )
            }
        } else {
            items(transactions.take(5), key = { it.id }) { tx ->
                TransactionRow(tx = tx)
            }
        }

        item { Spacer(Modifier.height(12.dp)) }
    }
}

@Composable
private fun StatTile(
    label: String,
    value: String,
    icon: ImageVector,
    tint: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = ObsidianElevated.copy(alpha = 0.85f),
        border = BorderStroke(1.dp, GlassBorder)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(18.dp))
            Spacer(Modifier.height(4.dp))
            Text(value, style = MaterialTheme.typography.titleLarge, color = TextPrimary)
            Text(label, style = MaterialTheme.typography.labelSmall, color = TextSecondary)
        }
    }
}

@Composable
private fun SectionHeader(
    title: String,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            color = TextPrimary
        )
        if (actionLabel != null && onAction != null) {
            TextButton(onClick = onAction) {
                Text(actionLabel, color = NeonCyan, style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}

@Composable
private fun FeaturedTournamentCard(
    tournament: Tournament,
    onClick: () -> Unit
) {
    CyberCard(
        modifier = Modifier
            .width(280.dp)
            .testTag("featured_tournament_${tournament.id}"),
        onClick = onClick
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(112.dp)
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
                    .background(Brush.verticalGradient(listOf(Color.Transparent, VoidBlack.copy(alpha = 0.85f))))
            )
            StatusBadge(
                status = tournament.status,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(8.dp)
            )
            Text(
                text = tournament.gameName,
                style = MaterialTheme.typography.labelMedium,
                color = NeonCyan,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(8.dp)
            )
        }
        Spacer(Modifier.height(10.dp))
        Text(
            text = tournament.title,
            style = MaterialTheme.typography.titleLarge,
            color = TextPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = tournament.matchTimeLabel,
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(Modifier.height(10.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text("ENTRY", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                Text(
                    text = if (tournament.entryFee == 0L) "FREE" else FormatUtils.formatPkr(tournament.entryFee),
                    style = MaterialTheme.typography.labelMedium,
                    color = NeonCyan,
                    fontWeight = FontWeight.Bold
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text("PRIZE POOL", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                Text(
                    text = FormatUtils.formatPkr(tournament.prizePool),
                    style = MaterialTheme.typography.labelMedium,
                    color = NeonEmerald,
                    fontWeight = FontWeight.Bold
                )
            }
        }
        Spacer(Modifier.height(10.dp))
        SlotProgressBar(filled = tournament.filledSlots, max = tournament.maxSlots)
        Spacer(Modifier.height(4.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "${tournament.filledSlots}/${tournament.maxSlots} Joined",
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondary
            )
            Text(
                text = "${tournament.availableSlots} left",
                style = MaterialTheme.typography.labelSmall,
                color = if (tournament.availableSlots <= 3) AmberGold else NeonCyan
            )
        }
    }
}
