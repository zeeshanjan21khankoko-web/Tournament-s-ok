package com.example.ui.screens

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.UserProfile
import com.example.data.model.WinnerRecord
import com.example.ui.components.CyberCard
import com.example.ui.components.EmptyArenaState
import com.example.ui.components.PlayerAvatar
import com.example.ui.components.StatusBadge
import com.example.ui.theme.AmberGold
import com.example.ui.theme.DangerCrimson
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
import com.example.util.ImageUtils
import kotlinx.coroutines.launch

@Composable
fun ProfileScreen(
    profile: UserProfile,
    isAdmin: Boolean,
    myWins: List<WinnerRecord>,
    isActionLoading: Boolean,
    onUpdateProfile: (String, String?) -> Unit,
    onOpenAdmin: () -> Unit,
    onSignOut: () -> Unit
) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val scope = rememberCoroutineScope()
    var showEditDialog by remember { mutableStateOf(false) }
    var showLogoutDialog by remember { mutableStateOf(false) }

    val avatarPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                ImageUtils.encodeUriToBase64Jpeg(context, uri, maxDimension = 360, maxBase64Length = 120_000)
                    .onSuccess { encoded ->
                        onUpdateProfile(profile.username, encoded)
                    }
                    .onFailure {
                        Toast.makeText(context, it.localizedMessage ?: "Invalid image", Toast.LENGTH_SHORT).show()
                    }
            }
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            CyberCard(
                modifier = Modifier.fillMaxWidth(),
                accentBrush = Brush.linearGradient(listOf(NeonCyan, ElectricPurple))
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(contentAlignment = Alignment.BottomEnd) {
                        PlayerAvatar(
                            photoBase64 = profile.photoBase64,
                            contentDescription = profile.username,
                            size = 96.dp,
                            borderColor = NeonCyan
                        )
                        Surface(
                            onClick = {
                                avatarPicker.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                            shape = CircleShape,
                            color = NeonCyan,
                            modifier = Modifier.size(34.dp).testTag("profile_change_photo")
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Filled.CameraAlt, contentDescription = "Change avatar", tint = VoidBlack, modifier = Modifier.size(18.dp))
                            }
                        }
                    }

                    Spacer(Modifier.height(12.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(profile.username, style = MaterialTheme.typography.headlineMedium, color = TextPrimary)
                        IconButton(onClick = { showEditDialog = true }, modifier = Modifier.testTag("profile_edit_name")) {
                            Icon(Icons.Filled.Edit, contentDescription = "Edit username", tint = NeonCyan, modifier = Modifier.size(18.dp))
                        }
                    }
                    Text(profile.email, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                    Spacer(Modifier.height(8.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            onClick = {
                                clipboard.setText(AnnotatedString(profile.playerId))
                                Toast.makeText(context, "Player ID copied", Toast.LENGTH_SHORT).show()
                            },
                            shape = RoundedCornerShape(50),
                            color = ObsidianElevated,
                            border = BorderStroke(1.dp, NeonCyan.copy(alpha = 0.6f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Player ID: ${profile.playerId}", style = MaterialTheme.typography.labelMedium, color = NeonCyan)
                                Spacer(Modifier.width(6.dp))
                                Icon(Icons.Filled.ContentCopy, contentDescription = "Copy Player ID", tint = NeonCyan, modifier = Modifier.size(14.dp))
                            }
                        }
                        if (isAdmin) {
                            StatusBadge(status = "ADMIN")
                        }
                    }

                    Spacer(Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(FormatUtils.formatPkr(profile.walletBalance), style = MaterialTheme.typography.titleLarge, color = NeonEmerald)
                            Text("Balance", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(profile.matchesPlayed.toString(), style = MaterialTheme.typography.titleLarge, color = NeonCyan)
                            Text("Played", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(profile.wins.toString(), style = MaterialTheme.typography.titleLarge, color = AmberGold)
                            Text("Wins", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                        }
                    }
                }
            }
        }

        if (isAdmin) {
            item {
                Button(
                    onClick = onOpenAdmin,
                    modifier = Modifier.fillMaxWidth().height(50.dp).testTag("profile_open_admin"),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = ElectricPurple, contentColor = TextPrimary)
                ) {
                    Icon(Icons.Filled.Shield, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Open Admin Control Panel", fontWeight = FontWeight.Bold)
                }
            }
        }

        item {
            Text("MY TOURNAMENT VICTORIES", style = MaterialTheme.typography.titleLarge, color = TextPrimary)
        }

        if (myWins.isEmpty()) {
            item {
                EmptyArenaState(
                    title = "No Trophies Yet",
                    subtitle = "Join tournaments and climb the podium to see your declared wins here.",
                    icon = Icons.Filled.EmojiEvents
                )
            }
        } else {
            items(myWins, key = { it.id }) { win ->
                CyberCard(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(win.tournamentTitle, style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                            Text("${win.gameName} • ${win.placement}", style = MaterialTheme.typography.bodyMedium, color = AmberGold)
                        }
                        Text(FormatUtils.formatPkr(win.prizeAmount), style = MaterialTheme.typography.titleMedium, color = NeonEmerald)
                    }
                }
            }
        }

        item {
            OutlinedButton(
                onClick = { showLogoutDialog = true },
                modifier = Modifier.fillMaxWidth().height(50.dp).testTag("logout_button"),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, DangerCrimson.copy(alpha = 0.7f))
            ) {
                Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null, tint = DangerCrimson)
                Spacer(Modifier.width(8.dp))
                Text("Sign Out", color = DangerCrimson, fontWeight = FontWeight.Bold)
            }
        }

        item { Spacer(Modifier.height(16.dp)) }
    }

    if (showEditDialog) {
        var newName by remember { mutableStateOf(profile.username) }
        AlertDialog(
            onDismissRequest = { showEditDialog = false },
            containerColor = ObsidianCard,
            title = { Text("Update Username", color = TextPrimary) },
            text = {
                OutlinedTextField(
                    value = newName,
                    onValueChange = { if (it.length <= 24) newName = it },
                    label = { Text("Username (3-24 chars)") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NeonCyan,
                        unfocusedBorderColor = GlassBorder,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    )
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newName.trim().length >= 3) {
                            onUpdateProfile(newName, null)
                            showEditDialog = false
                        }
                    },
                    enabled = !isActionLoading && newName.trim().length >= 3,
                    colors = ButtonDefaults.buttonColors(containerColor = NeonCyan, contentColor = VoidBlack)
                ) {
                    Text("Save", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditDialog = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            }
        )
    }

    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            containerColor = ObsidianCard,
            title = { Text("Sign Out?", color = TextPrimary) },
            text = { Text("You can sign back in anytime with your Google account.", color = TextSecondary) },
            confirmButton = {
                Button(
                    onClick = {
                        showLogoutDialog = false
                        onSignOut()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = DangerCrimson, contentColor = TextPrimary)
                ) {
                    Text("Sign Out")
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            }
        )
    }
}
