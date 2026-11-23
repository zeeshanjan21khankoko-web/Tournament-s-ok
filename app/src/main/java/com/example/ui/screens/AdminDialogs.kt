package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.model.PaymentConfig
import com.example.data.model.Tournament
import com.example.data.model.TournamentBannerKey
import com.example.data.model.TournamentRegistration
import com.example.data.model.UserProfile
import com.example.ui.theme.DangerCrimson
import com.example.ui.theme.ElectricPurple
import com.example.ui.theme.GlassBorder
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonEmerald
import com.example.ui.theme.ObsidianCard
import com.example.ui.theme.ObsidianElevated
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.VoidBlack
import com.example.util.FormatUtils
import com.example.util.ImageUtils

@Composable
fun TournamentEditorDialog(
    existing: Tournament?,
    onDismiss: () -> Unit,
    onSave: (
        title: String,
        gameName: String,
        bannerKey: String,
        entryFee: Long,
        prizePool: Long,
        maxSlots: Long,
        matchTimeLabel: String,
        status: String,
        rulesSummary: String,
        roomId: String?
    ) -> Unit
) {
    var title by remember { mutableStateOf(existing?.title ?: "") }
    var gameName by remember { mutableStateOf(existing?.gameName ?: "PUBG Mobile") }
    var bannerKey by remember { mutableStateOf(existing?.bannerKey ?: "battle_royale") }
    var entryFee by remember { mutableStateOf((existing?.entryFee ?: 100L).toString()) }
    var prizePool by remember { mutableStateOf((existing?.prizePool ?: 5000L).toString()) }
    var maxSlots by remember { mutableStateOf((existing?.maxSlots ?: 32L).toString()) }
    var matchTimeLabel by remember { mutableStateOf(existing?.matchTimeLabel ?: "Tonight 9:00 PM PKT") }
    var status by remember { mutableStateOf(existing?.status ?: "UPCOMING") }
    var rulesSummary by remember { mutableStateOf(existing?.rulesSummary ?: "Squad TPP • Fair play required • Room ID shared 15 min prior.") }
    var roomId by remember { mutableStateOf(existing?.roomId ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = ObsidianCard,
        title = { Text(if (existing == null) "Create Tournament" else "Edit Tournament", color = TextPrimary) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 440.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AdminField(title, { title = it }, "Tournament Title")
                AdminField(gameName, { gameName = it }, "Game Name")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AdminField(entryFee, { if (it.all(Char::isDigit)) entryFee = it }, "Entry (PKR)", KeyboardType.Number, Modifier.weight(1f))
                    AdminField(prizePool, { if (it.all(Char::isDigit)) prizePool = it }, "Prize (PKR)", KeyboardType.Number, Modifier.weight(1f))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AdminField(maxSlots, { if (it.all(Char::isDigit)) maxSlots = it }, "Max Slots", KeyboardType.Number, Modifier.weight(1f))
                    AdminField(matchTimeLabel, { matchTimeLabel = it }, "Match Time", KeyboardType.Text, Modifier.weight(1.4f))
                }
                Text("Status", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("UPCOMING", "LIVE", "COMPLETED").forEach { st ->
                        FilterChip(
                            selected = status == st,
                            onClick = { status = st },
                            label = { Text(st, style = MaterialTheme.typography.labelSmall) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = NeonCyan.copy(alpha = 0.2f),
                                selectedLabelColor = NeonCyan
                            )
                        )
                    }
                }
                Text("Banner Art", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    TournamentBannerKey.entries.forEach { b ->
                        FilterChip(
                            selected = bannerKey == b.key,
                            onClick = { bannerKey = b.key },
                            label = { Text(b.label.substringBefore(" "), style = MaterialTheme.typography.labelSmall) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = ElectricPurple.copy(alpha = 0.25f),
                                selectedLabelColor = TextPrimary
                            )
                        )
                    }
                }
                AdminField(roomId, { roomId = it }, "Room ID / Password (Optional)")
                AdminField(rulesSummary, { rulesSummary = it }, "Rules Summary")
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val fee = entryFee.toLongOrNull() ?: 0L
                    val prize = prizePool.toLongOrNull() ?: 0L
                    val slots = (maxSlots.toLongOrNull() ?: 16L).coerceIn(2L, 200L)
                    if (title.trim().length >= 3 && gameName.trim().length >= 2) {
                        onSave(title, gameName, bannerKey, fee, prize, slots, matchTimeLabel, status, rulesSummary, roomId.takeIf { it.isNotBlank() })
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = NeonCyan, contentColor = VoidBlack)
            ) {
                Text(if (existing == null) "Launch" else "Save", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = TextSecondary) }
        }
    )
}

@Composable
fun ManagePlayersDialog(
    tournament: Tournament,
    registrations: List<TournamentRegistration>,
    allUsers: List<UserProfile>,
    onAddPlayer: (UserProfile) -> Unit,
    onRemovePlayer: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val registeredIds = remember(registrations) { registrations.map { it.userId }.toSet() }
    val availableUsers = remember(allUsers, registeredIds) {
        allUsers.filter { !it.blocked && it.userId !in registeredIds }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = ObsidianCard,
        title = { Text("Players: ${tournament.title}", color = TextPrimary) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 420.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("Registered (${registrations.size}/${tournament.maxSlots})", style = MaterialTheme.typography.titleMedium, color = NeonCyan)
                if (registrations.isEmpty()) {
                    Text("No players registered yet.", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                } else {
                    registrations.forEach { reg ->
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = ObsidianElevated,
                            border = BorderStroke(1.dp, GlassBorder),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(reg.username, color = TextPrimary, style = MaterialTheme.typography.titleMedium)
                                    Text(reg.playerId, color = TextSecondary, style = MaterialTheme.typography.labelSmall)
                                }
                                TextButton(onClick = { onRemovePlayer(reg.userId) }) {
                                    Text("Remove", color = DangerCrimson)
                                }
                            }
                        }
                    }
                }

                Spacer(Modifier.height(6.dp))
                Text("Add Player", style = MaterialTheme.typography.titleMedium, color = ElectricPurple)
                if (availableUsers.isEmpty()) {
                    Text("No more eligible users available to add.", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                } else {
                    availableUsers.forEach { u ->
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = ObsidianElevated,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(u.username, color = TextPrimary, style = MaterialTheme.typography.titleMedium)
                                    Text("${u.playerId} • ${FormatUtils.formatPkr(u.walletBalance)}", color = TextSecondary, style = MaterialTheme.typography.labelSmall)
                                }
                                TextButton(onClick = { onAddPlayer(u) }) {
                                    Text("Add", color = NeonEmerald, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Done", color = NeonCyan) }
        }
    )
}

@Composable
fun DeclareWinnerDialog(
    tournament: Tournament,
    registrations: List<TournamentRegistration>,
    onDeclare: (TournamentRegistration, String, Long) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedReg by remember { mutableStateOf(registrations.firstOrNull()) }
    var placement by remember { mutableStateOf("1st Place") }
    var prizeText by remember { mutableStateOf(tournament.prizePool.toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = ObsidianCard,
        title = { Text("Declare Winner", color = TextPrimary) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 380.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (registrations.isEmpty()) {
                    Text("No registered players in this tournament yet. Add a player first.", color = DangerCrimson)
                } else {
                    Text("Select Winner", style = MaterialTheme.typography.labelMedium, color = NeonCyan)
                    registrations.forEach { reg ->
                        val isSelected = selectedReg?.userId == reg.userId
                        Surface(
                            onClick = { selectedReg = reg },
                            shape = RoundedCornerShape(10.dp),
                            color = if (isSelected) NeonCyan.copy(alpha = 0.18f) else ObsidianElevated,
                            border = BorderStroke(1.dp, if (isSelected) NeonCyan else GlassBorder),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(modifier = Modifier.padding(10.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("${reg.username} (${reg.playerId})", color = TextPrimary)
                            }
                        }
                    }
                    AdminField(placement, { placement = it }, "Placement (e.g., 1st Place)")
                    AdminField(prizeText, { if (it.all(Char::isDigit)) prizeText = it }, "Prize Credit (PKR)", KeyboardType.Number)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val reg = selectedReg
                    val prize = prizeText.toLongOrNull() ?: 0L
                    if (reg != null && placement.isNotBlank()) {
                        onDeclare(reg, placement, prize)
                    }
                },
                enabled = selectedReg != null,
                colors = ButtonDefaults.buttonColors(containerColor = NeonEmerald, contentColor = VoidBlack)
            ) {
                Text("Declare & Credit", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = TextSecondary) }
        }
    )
}

@Composable
fun ScreenshotPreviewDialog(
    screenshotBase64: String,
    title: String,
    onDismiss: () -> Unit
) {
    val bitmap = remember(screenshotBase64) { ImageUtils.decodeBase64ToImageBitmap(screenshotBase64) }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = ObsidianCard,
        title = { Text(title, color = TextPrimary) },
        text = {
            if (bitmap != null) {
                Image(
                    bitmap = bitmap,
                    contentDescription = "Payment proof screenshot",
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 420.dp)
                        .clip(RoundedCornerShape(12.dp)),
                    contentScale = ContentScale.Fit
                )
            } else {
                Text("Screenshot preview unavailable.", color = TextSecondary)
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Close", color = NeonCyan) }
        }
    )
}

@Composable
fun AdminNoteDialog(
    title: String,
    confirmLabel: String,
    isDestructive: Boolean,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var note by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = ObsidianCard,
        title = { Text(title, color = TextPrimary) },
        text = {
            AdminField(note, { if (it.length <= 180) note = it }, "Admin Note / Reason")
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(note) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isDestructive) DangerCrimson else NeonEmerald,
                    contentColor = if (isDestructive) TextPrimary else VoidBlack
                )
            ) {
                Text(confirmLabel, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = TextSecondary) }
        }
    )
}

@Composable
fun AddUserDialog(
    onAdd: (username: String, email: String, startingBalance: Long) -> Unit,
    onDismiss: () -> Unit
) {
    var username by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var balance by remember { mutableStateOf("0") }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = ObsidianCard,
        title = { Text("Add User Record", color = TextPrimary) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                AdminField(username, { if (it.length <= 24) username = it }, "Username (3-24 chars)")
                AdminField(email, { if (it.length <= 160) email = it }, "Email Address")
                AdminField(balance, { if (it.all(Char::isDigit)) balance = it }, "Initial Wallet (PKR)", KeyboardType.Number)
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val bal = balance.toLongOrNull() ?: 0L
                    if (username.trim().length >= 3 && email.contains("@")) {
                        onAdd(username, email, bal)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = NeonCyan, contentColor = VoidBlack)
            ) {
                Text("Add User", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = TextSecondary) }
        }
    )
}

@Composable
fun AdjustBalanceDialog(
    user: UserProfile,
    onApply: (delta: Long, reason: String) -> Unit,
    onDismiss: () -> Unit
) {
    var amountText by remember { mutableStateOf("") }
    var isCredit by remember { mutableStateOf(true) }
    var reason by remember { mutableStateOf("Manual admin adjustment") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = ObsidianCard,
        title = { Text("Wallet: ${user.username}", color = TextPrimary) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Current: ${FormatUtils.formatPkr(user.walletBalance)}", color = NeonCyan)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = isCredit,
                        onClick = { isCredit = true },
                        label = { Text("+ Credit") }
                    )
                    FilterChip(
                        selected = !isCredit,
                        onClick = { isCredit = false },
                        label = { Text("- Debit") }
                    )
                }
                AdminField(amountText, { if (it.all(Char::isDigit)) amountText = it }, "Amount (PKR)", KeyboardType.Number)
                AdminField(reason, { reason = it }, "Reason")
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amt = amountText.toLongOrNull() ?: 0L
                    if (amt > 0L) onApply(if (isCredit) amt else -amt, reason)
                },
                colors = ButtonDefaults.buttonColors(containerColor = NeonCyan, contentColor = VoidBlack)
            ) {
                Text("Apply", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = TextSecondary) }
        }
    )
}

@Composable
fun PaymentConfigDialog(
    current: PaymentConfig,
    onSave: (PaymentConfig) -> Unit,
    onDismiss: () -> Unit
) {
    var jcTitle by remember { mutableStateOf(current.jazzCashTitle) }
    var jcNum by remember { mutableStateOf(current.jazzCashNumber) }
    var epTitle by remember { mutableStateOf(current.easyPaisaTitle) }
    var epNum by remember { mutableStateOf(current.easyPaisaNumber) }
    var bankName by remember { mutableStateOf(current.bankName) }
    var bankTitle by remember { mutableStateOf(current.bankAccountTitle) }
    var bankNum by remember { mutableStateOf(current.bankAccountNumber) }
    var instr by remember { mutableStateOf(current.instructions) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = ObsidianCard,
        title = { Text("Pakistan Payment Accounts", color = TextPrimary) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 420.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AdminField(jcTitle, { jcTitle = it }, "JazzCash Title")
                AdminField(jcNum, { jcNum = it }, "JazzCash Number")
                AdminField(epTitle, { epTitle = it }, "EasyPaisa Title")
                AdminField(epNum, { epNum = it }, "EasyPaisa Number")
                AdminField(bankName, { bankName = it }, "Bank Name")
                AdminField(bankTitle, { bankTitle = it }, "Bank Account Title")
                AdminField(bankNum, { bankNum = it }, "Bank Account / IBAN")
                AdminField(instr, { instr = it }, "Deposit Instructions")
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(
                        PaymentConfig(
                            jazzCashTitle = jcTitle,
                            jazzCashNumber = jcNum,
                            easyPaisaTitle = epTitle,
                            easyPaisaNumber = epNum,
                            bankName = bankName,
                            bankAccountTitle = bankTitle,
                            bankAccountNumber = bankNum,
                            instructions = instr
                        )
                    )
                },
                colors = ButtonDefaults.buttonColors(containerColor = NeonCyan, contentColor = VoidBlack)
            ) {
                Text("Save Config", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = TextSecondary) }
        }
    )
}

@Composable
private fun AdminField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    keyboardType: KeyboardType = KeyboardType.Text,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        modifier = modifier.fillMaxWidth(),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = NeonCyan,
            unfocusedBorderColor = GlassBorder,
            focusedLabelColor = NeonCyan,
            focusedTextColor = TextPrimary,
            unfocusedTextColor = TextPrimary
        )
    )
}
