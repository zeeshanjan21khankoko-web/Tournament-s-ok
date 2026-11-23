package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.AdminRecord
import com.example.data.model.DepositRequest
import com.example.data.model.PaymentConfig
import com.example.data.model.PaymentMethod
import com.example.data.model.Tournament
import com.example.data.model.TournamentRegistration
import com.example.data.model.UserProfile
import com.example.data.model.WalletTransaction
import com.example.data.model.WinnerRecord
import com.example.data.model.WithdrawalRequest
import com.example.ui.components.CyberCard
import com.example.ui.components.EmptyArenaState
import com.example.ui.components.PlayerAvatar
import com.example.ui.components.StatusBadge
import com.example.ui.components.TransactionRow
import com.example.ui.theme.AmberGold
import com.example.ui.theme.CyberMagenta
import com.example.ui.theme.DangerCrimson
import com.example.ui.theme.ElectricPurple
import com.example.ui.theme.GlassBorder
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonEmerald
import com.example.ui.theme.ObsidianCard
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.VoidBlack
import com.example.util.FormatUtils

@Composable
fun AdminDashboardScreen(
    users: List<UserProfile>,
    admins: List<AdminRecord>,
    tournaments: List<Tournament>,
    deposits: List<DepositRequest>,
    withdrawals: List<WithdrawalRequest>,
    transactions: List<WalletTransaction>,
    winners: List<WinnerRecord>,
    paymentConfig: PaymentConfig,
    selectedRegistrations: List<TournamentRegistration>,
    onObserveTournamentPlayers: (String?) -> Unit,
    onCreateTournament: (String, String, String, Long, Long, Long, Long, String, String, String, String?) -> Unit,
    onUpdateTournament: (String, String, String, String, Long, Long, Long, String, String, String, String?) -> Unit,
    onDeleteTournament: (String) -> Unit,
    onAddPlayerToTournament: (Tournament, UserProfile) -> Unit,
    onRemovePlayerFromTournament: (String, String) -> Unit,
    onDeclareWinner: (Tournament, TournamentRegistration, String, Long) -> Unit,
    onApproveDeposit: (DepositRequest, String) -> Unit,
    onRejectDeposit: (DepositRequest, String) -> Unit,
    onApproveWithdrawal: (WithdrawalRequest, String) -> Unit,
    onRejectWithdrawal: (WithdrawalRequest, String) -> Unit,
    onToggleUserBlocked: (String, Boolean) -> Unit,
    onAdjustUserBalance: (UserProfile, Long, String) -> Unit,
    onAddUser: (String, String, Long) -> Unit,
    onDeleteUser: (String) -> Unit,
    onSetAdminRole: (UserProfile, Boolean) -> Unit,
    onSavePaymentConfig: (PaymentConfig) -> Unit
) {
    var selectedSection by remember { mutableIntStateOf(0) }
    val pendingDepCount = remember(deposits) { deposits.count { it.status == "PENDING" } }
    val pendingWdCount = remember(withdrawals) { withdrawals.count { it.status == "PENDING" } }

    var showTournamentEditor by remember { mutableStateOf(false) }
    var editingTournament by remember { mutableStateOf<Tournament?>(null) }
    var playersTargetTournament by remember { mutableStateOf<Tournament?>(null) }
    var winnerTargetTournament by remember { mutableStateOf<Tournament?>(null) }
    var previewDeposit by remember { mutableStateOf<DepositRequest?>(null) }
    var noteDepositAction by remember { mutableStateOf<Pair<DepositRequest, Boolean>?>(null) }
    var noteWithdrawalAction by remember { mutableStateOf<Pair<WithdrawalRequest, Boolean>?>(null) }
    var showAddUserDialog by remember { mutableStateOf(false) }
    var balanceUser by remember { mutableStateOf<UserProfile?>(null) }
    var showPaymentConfigDialog by remember { mutableStateOf(false) }
    var userSearch by remember { mutableStateOf("") }

    val adminUidSet = remember(admins) { admins.map { it.userId }.toSet() }

    val tabs = listOf(
        "Reports",
        "Deposits ($pendingDepCount)",
        "Withdrawals ($pendingWdCount)",
        "Tournaments",
        "Users (${users.size})",
        "Ledger"
    )

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("ADMIN COMMAND", style = MaterialTheme.typography.headlineLarge, color = ElectricPurple)
                    Text("Full platform, financial & tournament control", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                }
                StatusBadge(status = "ADMIN")
            }
            Spacer(Modifier.height(10.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(tabs.indices.toList()) { index ->
                    FilterChip(
                        selected = selectedSection == index,
                        onClick = { selectedSection = index },
                        label = { Text(tabs[index]) },
                        modifier = Modifier.testTag("admin_tab_$index"),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = ElectricPurple.copy(alpha = 0.25f),
                            selectedLabelColor = NeonCyan,
                            containerColor = ObsidianCard,
                            labelColor = TextSecondary
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = selectedSection == index,
                            borderColor = GlassBorder,
                            selectedBorderColor = ElectricPurple
                        )
                    )
                }
            }
        }

        when (selectedSection) {
            // 0: FINANCIAL & PLATFORM REPORTS
            0 -> {
                val totalApprovedDeposits = deposits.filter { it.status == "APPROVED" }.sumOf { it.amount }
                val totalApprovedWithdrawals = withdrawals.filter { it.status == "APPROVED" }.sumOf { it.amount }
                val totalPrizesPaid = winners.sumOf { it.prizeAmount }
                val totalUserBalances = users.sumOf { it.walletBalance }
                val blockedCount = users.count { it.blocked }
                val liveOrUpcoming = tournaments.count { it.status == "LIVE" || it.status == "UPCOMING" }

                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                        ReportMetricCard("Total Users", users.size.toString(), "$blockedCount blocked", NeonCyan, Modifier.weight(1f))
                        ReportMetricCard("Tournaments", tournaments.size.toString(), "$liveOrUpcoming active", ElectricPurple, Modifier.weight(1f))
                    }
                }
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                        ReportMetricCard("Approved Deposits", FormatUtils.formatPkr(totalApprovedDeposits), "$pendingDepCount pending", NeonEmerald, Modifier.weight(1f))
                        ReportMetricCard("Paid Withdrawals", FormatUtils.formatPkr(totalApprovedWithdrawals), "$pendingWdCount pending", CyberMagenta, Modifier.weight(1f))
                    }
                }
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                        ReportMetricCard("Prizes Awarded", FormatUtils.formatPkr(totalPrizesPaid), "${winners.size} winners", AmberGold, Modifier.weight(1f))
                        ReportMetricCard("User Wallets", FormatUtils.formatPkr(totalUserBalances), "Total float", NeonCyan, Modifier.weight(1f))
                    }
                }
                item {
                    CyberCard(modifier = Modifier.fillMaxWidth()) {
                        Text("PAKISTAN PAYMENT ACCOUNTS", style = MaterialTheme.typography.titleMedium, color = NeonCyan)
                        Spacer(Modifier.height(6.dp))
                        Text("JazzCash: ${paymentConfig.jazzCashNumber} (${paymentConfig.jazzCashTitle})", style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
                        Text("EasyPaisa: ${paymentConfig.easyPaisaNumber} (${paymentConfig.easyPaisaTitle})", style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
                        Text("Bank: ${paymentConfig.bankName} • ${paymentConfig.bankAccountNumber}", style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
                        Spacer(Modifier.height(10.dp))
                        OutlinedButton(
                            onClick = { showPaymentConfigDialog = true },
                            border = BorderStroke(1.dp, NeonCyan),
                            modifier = Modifier.fillMaxWidth().testTag("admin_edit_payment_config")
                        ) {
                            Icon(Icons.Filled.AccountBalance, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Update Payment Instructions", color = NeonCyan)
                        }
                    }
                }
            }

            // 1: DEPOSIT REQUESTS
            1 -> {
                if (deposits.isEmpty()) {
                    item { EmptyArenaState("No Deposit Requests", "User JazzCash, EasyPaisa, and Bank Transfer proofs appear here.", Icons.Filled.Payments) }
                } else {
                    items(deposits, key = { it.id }) { dep ->
                        CyberCard(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("${dep.username} (${dep.playerId})", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                                    Text("${PaymentMethod.fromRaw(dep.paymentMethod).label} • ${FormatUtils.formatPkr(dep.amount)}", style = MaterialTheme.typography.titleLarge, color = NeonCyan)
                                    Text("TID: ${dep.referenceId} • Sender: ${dep.senderAccount}", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                                }
                                StatusBadge(status = dep.status)
                            }
                            Spacer(Modifier.height(10.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                                OutlinedButton(
                                    onClick = { previewDeposit = dep },
                                    border = BorderStroke(1.dp, GlassBorder),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Filled.Image, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("Proof", color = TextPrimary)
                                }
                                if (dep.status == "PENDING") {
                                    Button(
                                        onClick = { noteDepositAction = dep to true },
                                        colors = ButtonDefaults.buttonColors(containerColor = NeonEmerald, contentColor = VoidBlack),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(Modifier.width(4.dp))
                                        Text("Approve", fontWeight = FontWeight.Bold)
                                    }
                                    Button(
                                        onClick = { noteDepositAction = dep to false },
                                        colors = ButtonDefaults.buttonColors(containerColor = DangerCrimson, contentColor = TextPrimary),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(Icons.Filled.Close, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(Modifier.width(4.dp))
                                        Text("Reject", fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 2: WITHDRAWAL REQUESTS
            2 -> {
                if (withdrawals.isEmpty()) {
                    item { EmptyArenaState("No Withdrawal Requests", "Player payout requests will appear here for manual verification.", Icons.Filled.Payments) }
                } else {
                    items(withdrawals, key = { it.id }) { wd ->
                        CyberCard(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("${wd.username} (${wd.playerId})", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                                    Text("${PaymentMethod.fromRaw(wd.paymentMethod).label} • ${FormatUtils.formatPkr(wd.amount)}", style = MaterialTheme.typography.titleLarge, color = ElectricPurple)
                                    if (!wd.bankName.isNullOrBlank()) {
                                        Text("Bank: ${wd.bankName}", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                                    }
                                    Text("${wd.accountTitle} • ${wd.accountNumber}", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                                }
                                StatusBadge(status = wd.status)
                            }
                            if (wd.status == "PENDING") {
                                Spacer(Modifier.height(10.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                                    Button(
                                        onClick = { noteWithdrawalAction = wd to true },
                                        colors = ButtonDefaults.buttonColors(containerColor = NeonEmerald, contentColor = VoidBlack),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text("Mark Paid", fontWeight = FontWeight.Bold)
                                    }
                                    Button(
                                        onClick = { noteWithdrawalAction = wd to false },
                                        colors = ButtonDefaults.buttonColors(containerColor = DangerCrimson, contentColor = TextPrimary),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text("Reject & Refund", fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 3: TOURNAMENT MANAGEMENT
            3 -> {
                item {
                    Button(
                        onClick = {
                            editingTournament = null
                            showTournamentEditor = true
                        },
                        modifier = Modifier.fillMaxWidth().height(50.dp).testTag("admin_create_tournament_button"),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = NeonCyan, contentColor = VoidBlack)
                    ) {
                        Icon(Icons.Filled.Add, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Create New Tournament", fontWeight = FontWeight.Bold)
                    }
                }
                if (tournaments.isEmpty()) {
                    item { EmptyArenaState("No Tournaments Created", "Tap Create New Tournament above to launch an event.") }
                } else {
                    items(tournaments, key = { it.id }) { t ->
                        CyberCard(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(t.title, style = MaterialTheme.typography.titleLarge, color = TextPrimary)
                                    Text("${t.gameName} • ${t.filledSlots}/${t.maxSlots} slots • Prize ${FormatUtils.formatPkr(t.prizePool)}", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                                    if (!t.winnerUsername.isNullOrBlank()) {
                                        Text("Winner: ${t.winnerUsername}", style = MaterialTheme.typography.labelMedium, color = AmberGold)
                                    }
                                }
                                StatusBadge(status = t.status)
                            }
                            Spacer(Modifier.height(10.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                                OutlinedButton(
                                    onClick = {
                                        editingTournament = t
                                        showTournamentEditor = true
                                    },
                                    border = BorderStroke(1.dp, GlassBorder),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Filled.Edit, contentDescription = "Edit", modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("Edit")
                                }
                                OutlinedButton(
                                    onClick = {
                                        onObserveTournamentPlayers(t.id)
                                        playersTargetTournament = t
                                    },
                                    border = BorderStroke(1.dp, NeonCyan.copy(alpha = 0.6f)),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Filled.Group, contentDescription = "Players", tint = NeonCyan, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("Players", color = NeonCyan)
                                }
                                OutlinedButton(
                                    onClick = {
                                        onObserveTournamentPlayers(t.id)
                                        winnerTargetTournament = t
                                    },
                                    border = BorderStroke(1.dp, AmberGold.copy(alpha = 0.7f)),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Filled.EmojiEvents, contentDescription = "Winner", tint = AmberGold, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("Winner", color = AmberGold)
                                }
                                IconButton(onClick = { onDeleteTournament(t.id) }) {
                                    Icon(Icons.Filled.Delete, contentDescription = "Delete tournament", tint = DangerCrimson)
                                }
                            }
                        }
                    }
                }
            }

            // 4: USER MANAGEMENT
            4 -> {
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(
                            value = userSearch,
                            onValueChange = { userSearch = it },
                            placeholder = { Text("Search username, email, ID...") },
                            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null, tint = NeonCyan) },
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = NeonCyan,
                                unfocusedBorderColor = GlassBorder,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary
                            )
                        )
                        Button(
                            onClick = { showAddUserDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = NeonCyan, contentColor = VoidBlack),
                            modifier = Modifier.height(54.dp)
                        ) {
                            Icon(Icons.Filled.PersonAdd, contentDescription = "Add user")
                        }
                    }
                }
                val filteredUsers = users.filter {
                    val q = userSearch.trim()
                    q.isBlank() || it.username.contains(q, true) || it.email.contains(q, true) || it.playerId.contains(q, true)
                }
                items(filteredUsers, key = { it.userId }) { u ->
                    val uIsAdmin = u.userId in adminUidSet
                    CyberCard(modifier = Modifier.fillMaxWidth()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            PlayerAvatar(photoBase64 = u.photoBase64, contentDescription = u.username, size = 44.dp)
                            Spacer(Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text(u.username, style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                                    if (u.blocked) StatusBadge("BLOCKED")
                                    if (uIsAdmin) StatusBadge("ADMIN")
                                }
                                Text("${u.playerId} • ${u.email}", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                                Text("Wallet: ${FormatUtils.formatPkr(u.walletBalance)} • Wins: ${u.wins}", style = MaterialTheme.typography.labelMedium, color = NeonCyan)
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                            TextButton(onClick = { balanceUser = u }) {
                                Text("Wallet", color = NeonCyan)
                            }
                            TextButton(onClick = { onToggleUserBlocked(u.userId, !u.blocked) }) {
                                Icon(Icons.Filled.Block, contentDescription = null, tint = if (u.blocked) NeonEmerald else DangerCrimson, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text(if (u.blocked) "Unblock" else "Block", color = if (u.blocked) NeonEmerald else DangerCrimson)
                            }
                            TextButton(onClick = { onSetAdminRole(u, !uIsAdmin) }) {
                                Icon(Icons.Filled.AdminPanelSettings, contentDescription = null, tint = ElectricPurple, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text(if (uIsAdmin) "Revoke" else "Make Admin", color = ElectricPurple)
                            }
                            IconButton(onClick = { onDeleteUser(u.userId) }) {
                                Icon(Icons.Filled.Delete, contentDescription = "Delete user", tint = TextMuted)
                            }
                        }
                    }
                }
            }

            // 5: ALL TRANSACTIONS LEDGER
            else -> {
                if (transactions.isEmpty()) {
                    item { EmptyArenaState("No Platform Transactions", "All deposits, entry fees, prize credits, and withdrawals appear here.") }
                } else {
                    items(transactions, key = { it.id }) { tx ->
                        Column {
                            Text("${tx.username} • ${tx.type}", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                            Spacer(Modifier.height(2.dp))
                            TransactionRow(tx = tx)
                        }
                    }
                }
            }
        }

        item { Spacer(Modifier.height(16.dp)) }
    }

    if (showTournamentEditor) {
        TournamentEditorDialog(
            existing = editingTournament,
            onDismiss = { showTournamentEditor = false },
            onSave = { title, game, banner, fee, prize, maxSlots, timeLabel, status, rules, roomId ->
                val ex = editingTournament
                if (ex == null) {
                    onCreateTournament(title, game, banner, fee, prize, maxSlots, System.currentTimeMillis() + 3_600_000L, timeLabel, status, rules, roomId)
                } else {
                    onUpdateTournament(ex.id, title, game, banner, fee, prize, maxSlots, timeLabel, status, rules, roomId)
                }
                showTournamentEditor = false
            }
        )
    }

    playersTargetTournament?.let { t ->
        ManagePlayersDialog(
            tournament = t,
            registrations = selectedRegistrations,
            allUsers = users,
            onAddPlayer = { u -> onAddPlayerToTournament(t, u) },
            onRemovePlayer = { uid -> onRemovePlayerFromTournament(t.id, uid) },
            onDismiss = {
                playersTargetTournament = null
                onObserveTournamentPlayers(null)
            }
        )
    }

    winnerTargetTournament?.let { t ->
        DeclareWinnerDialog(
            tournament = t,
            registrations = selectedRegistrations,
            onDeclare = { reg, placement, prize ->
                onDeclareWinner(t, reg, placement, prize)
                winnerTargetTournament = null
                onObserveTournamentPlayers(null)
            },
            onDismiss = {
                winnerTargetTournament = null
                onObserveTournamentPlayers(null)
            }
        )
    }

    previewDeposit?.let { dep ->
        ScreenshotPreviewDialog(
            screenshotBase64 = dep.screenshotBase64,
            title = "${dep.username} • TID ${dep.referenceId}",
            onDismiss = { previewDeposit = null }
        )
    }

    noteDepositAction?.let { (dep, approve) ->
        AdminNoteDialog(
            title = if (approve) "Approve ${FormatUtils.formatPkr(dep.amount)} Deposit?" else "Reject Deposit?",
            confirmLabel = if (approve) "Approve & Credit" else "Reject",
            isDestructive = !approve,
            onConfirm = { note ->
                if (approve) onApproveDeposit(dep, note.ifBlank { "Verified by Admin" })
                else onRejectDeposit(dep, note.ifBlank { "Invalid payment proof" })
                noteDepositAction = null
            },
            onDismiss = { noteDepositAction = null }
        )
    }

    noteWithdrawalAction?.let { (wd, approve) ->
        AdminNoteDialog(
            title = if (approve) "Confirm Payout of ${FormatUtils.formatPkr(wd.amount)}?" else "Reject & Refund ${FormatUtils.formatPkr(wd.amount)}?",
            confirmLabel = if (approve) "Mark Paid" else "Reject & Refund",
            isDestructive = !approve,
            onConfirm = { note ->
                if (approve) onApproveWithdrawal(wd, note.ifBlank { "Paid via ${PaymentMethod.fromRaw(wd.paymentMethod).label}" })
                else onRejectWithdrawal(wd, note.ifBlank { "Account details mismatch" })
                noteWithdrawalAction = null
            },
            onDismiss = { noteWithdrawalAction = null }
        )
    }

    if (showAddUserDialog) {
        AddUserDialog(
            onAdd = { name, email, bal ->
                onAddUser(name, email, bal)
                showAddUserDialog = false
            },
            onDismiss = { showAddUserDialog = false }
        )
    }

    balanceUser?.let { u ->
        AdjustBalanceDialog(
            user = u,
            onApply = { delta, reason ->
                onAdjustUserBalance(u, delta, reason)
                balanceUser = null
            },
            onDismiss = { balanceUser = null }
        )
    }

    if (showPaymentConfigDialog) {
        PaymentConfigDialog(
            current = paymentConfig,
            onSave = { cfg ->
                onSavePaymentConfig(cfg)
                showPaymentConfigDialog = false
            },
            onDismiss = { showPaymentConfigDialog = false }
        )
    }
}

@Composable
private fun ReportMetricCard(
    label: String,
    value: String,
    sub: String,
    accent: Color,
    modifier: Modifier = Modifier
) {
    CyberCard(
        modifier = modifier,
        accentBrush = Brush.linearGradient(listOf(accent.copy(alpha = 0.7f), ElectricPurple.copy(alpha = 0.35f)))
    ) {
        Text(label.uppercase(), style = MaterialTheme.typography.labelSmall, color = TextSecondary)
        Spacer(Modifier.height(4.dp))
        Text(value, style = MaterialTheme.typography.headlineMedium, color = accent)
        Text(sub, style = MaterialTheme.typography.bodyMedium, color = TextMuted)
    }
}
