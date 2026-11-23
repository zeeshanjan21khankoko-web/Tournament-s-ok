package com.example.ui.screens

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults.SecondaryIndicator
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.model.DepositRequest
import com.example.data.model.PaymentConfig
import com.example.data.model.PaymentMethod
import com.example.data.model.UserProfile
import com.example.data.model.WalletTransaction
import com.example.data.model.WithdrawalRequest
import com.example.ui.components.CyberCard
import com.example.ui.components.EmptyArenaState
import com.example.ui.components.StatusBadge
import com.example.ui.components.TransactionRow
import com.example.ui.theme.AmberGold
import com.example.ui.theme.BankBlue
import com.example.ui.theme.DangerCrimson
import com.example.ui.theme.EasyPaisaGreen
import com.example.ui.theme.ElectricPurple
import com.example.ui.theme.GlassBorder
import com.example.ui.theme.JazzCashRed
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
fun WalletScreen(
    profile: UserProfile,
    paymentConfig: PaymentConfig,
    transactions: List<WalletTransaction>,
    deposits: List<DepositRequest>,
    withdrawals: List<WithdrawalRequest>,
    initialTab: Int = 0,
    isActionLoading: Boolean,
    onSubmitDeposit: (Long, PaymentMethod, String, String, String) -> Unit,
    onSubmitWithdrawal: (Long, PaymentMethod, String, String, String?) -> Unit
) {
    var activeTab by remember(initialTab) { mutableIntStateOf(initialTab) }
    LaunchedEffect(initialTab) { activeTab = initialTab }

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
                Text("AVAILABLE BALANCE", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                Spacer(Modifier.height(4.dp))
                Text(
                    text = FormatUtils.formatPkr(profile.walletBalance),
                    style = MaterialTheme.typography.displayLarge,
                    color = NeonCyan,
                    modifier = Modifier.testTag("wallet_balance_display")
                )
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    WalletModeChip(
                        label = "History",
                        selected = activeTab == 0,
                        modifier = Modifier.weight(1f).testTag("wallet_tab_history"),
                        onClick = { activeTab = 0 }
                    )
                    WalletModeChip(
                        label = "Deposit",
                        selected = activeTab == 1,
                        modifier = Modifier.weight(1f).testTag("wallet_tab_deposit"),
                        onClick = { activeTab = 1 }
                    )
                    WalletModeChip(
                        label = "Withdraw",
                        selected = activeTab == 2,
                        modifier = Modifier.weight(1f).testTag("wallet_tab_withdraw"),
                        onClick = { activeTab = 2 }
                    )
                }
            }
        }

        when (activeTab) {
            1 -> {
                item {
                    DepositFormSection(
                        paymentConfig = paymentConfig,
                        isLoading = isActionLoading,
                        onSubmit = { amt, method, refId, sender, img ->
                            onSubmitDeposit(amt, method, refId, sender, img)
                        }
                    )
                }
            }
            2 -> {
                item {
                    WithdrawFormSection(
                        availableBalance = profile.walletBalance,
                        isLoading = isActionLoading,
                        onSubmit = onSubmitWithdrawal
                    )
                }
            }
            else -> {
                item {
                    WalletHistorySection(
                        transactions = transactions,
                        deposits = deposits,
                        withdrawals = withdrawals
                    )
                }
            }
        }

        item { Spacer(Modifier.height(16.dp)) }
    }
}

@Composable
private fun WalletModeChip(
    label: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = modifier.height(44.dp),
        shape = RoundedCornerShape(12.dp),
        color = if (selected) NeonCyan else ObsidianElevated,
        border = BorderStroke(1.dp, if (selected) NeonCyan else GlassBorder)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                color = if (selected) VoidBlack else TextPrimary,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun PaymentMethodSelector(
    selected: PaymentMethod,
    onSelect: (PaymentMethod) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        PaymentMethod.entries.forEach { method ->
            val accent = when (method) {
                PaymentMethod.JAZZCASH -> JazzCashRed
                PaymentMethod.EASYPAISA -> EasyPaisaGreen
                PaymentMethod.BANK_TRANSFER -> BankBlue
            }
            val icon = if (method == PaymentMethod.BANK_TRANSFER) Icons.Filled.AccountBalance else Icons.Filled.PhoneAndroid
            val isSelected = selected == method
            Surface(
                onClick = { onSelect(method) },
                modifier = Modifier
                    .weight(1f)
                    .testTag("payment_method_${method.name}"),
                shape = RoundedCornerShape(12.dp),
                color = if (isSelected) accent.copy(alpha = 0.18f) else ObsidianElevated,
                border = BorderStroke(1.5.dp, if (isSelected) accent else GlassBorder)
            ) {
                Column(
                    modifier = Modifier.padding(vertical = 12.dp, horizontal = 6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(icon, contentDescription = method.label, tint = if (isSelected) accent else TextSecondary, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = method.label,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isSelected) TextPrimary else TextSecondary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun DepositFormSection(
    paymentConfig: PaymentConfig,
    isLoading: Boolean,
    onSubmit: (Long, PaymentMethod, String, String, String) -> Unit
) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val scope = rememberCoroutineScope()

    var method by remember { mutableStateOf(PaymentMethod.JAZZCASH) }
    var amountText by remember { mutableStateOf("") }
    var senderAccount by remember { mutableStateOf("") }
    var referenceId by remember { mutableStateOf("") }
    var screenshotBase64 by remember { mutableStateOf<String?>(null) }
    var localError by remember { mutableStateOf<String?>(null) }
    var isCompressing by remember { mutableStateOf(false) }

    val screenshotPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            isCompressing = true
            localError = null
            scope.launch {
                ImageUtils.encodeUriToBase64Jpeg(context, uri, maxDimension = 820, maxBase64Length = 250_000)
                    .onSuccess {
                        screenshotBase64 = it
                        isCompressing = false
                    }
                    .onFailure {
                        localError = it.localizedMessage ?: "Failed to read screenshot"
                        isCompressing = false
                    }
            }
        }
    }

    val (accTitle, accNumber, extraBank) = when (method) {
        PaymentMethod.JAZZCASH -> Triple(paymentConfig.jazzCashTitle, paymentConfig.jazzCashNumber, null)
        PaymentMethod.EASYPAISA -> Triple(paymentConfig.easyPaisaTitle, paymentConfig.easyPaisaNumber, null)
        PaymentMethod.BANK_TRANSFER -> Triple(paymentConfig.bankAccountTitle, paymentConfig.bankAccountNumber, paymentConfig.bankName)
    }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        CyberCard(modifier = Modifier.fillMaxWidth()) {
            Text("1. SELECT PAKISTAN PAYMENT METHOD", style = MaterialTheme.typography.titleMedium, color = NeonCyan)
            Spacer(Modifier.height(10.dp))
            PaymentMethodSelector(selected = method, onSelect = { method = it })

            Spacer(Modifier.height(14.dp))
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = ObsidianElevated,
                border = BorderStroke(1.dp, GlassBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("SEND PAYMENT TO", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                    Spacer(Modifier.height(4.dp))
                    if (extraBank != null) {
                        Text("Bank: $extraBank", style = MaterialTheme.typography.titleMedium, color = BankBlue)
                    }
                    Text("Title: $accTitle", style = MaterialTheme.typography.bodyLarge, color = TextPrimary)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(accNumber, style = MaterialTheme.typography.titleLarge, color = NeonCyan)
                        IconButton(
                            onClick = {
                                clipboard.setText(AnnotatedString(accNumber))
                                Toast.makeText(context, "Account number copied", Toast.LENGTH_SHORT).show()
                            }
                        ) {
                            Icon(Icons.Filled.ContentCopy, contentDescription = "Copy account number", tint = NeonCyan)
                        }
                    }
                    Text(paymentConfig.instructions, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                }
            }
        }

        CyberCard(modifier = Modifier.fillMaxWidth()) {
            Text("2. SUBMIT PAYMENT PROOF", style = MaterialTheme.typography.titleMedium, color = ElectricPurple)
            Spacer(Modifier.height(10.dp))

            OutlinedTextField(
                value = amountText,
                onValueChange = { if (it.all(Char::isDigit) && it.length <= 6) amountText = it },
                label = { Text("Amount (PKR 50 - 500,000)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.fillMaxWidth().testTag("deposit_amount_input"),
                colors = walletFieldColors()
            )
            Spacer(Modifier.height(10.dp))
            OutlinedTextField(
                value = senderAccount,
                onValueChange = { if (it.length <= 40) senderAccount = it },
                label = { Text("Sender Mobile / Account No.") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().testTag("deposit_sender_input"),
                colors = walletFieldColors()
            )
            Spacer(Modifier.height(10.dp))
            OutlinedTextField(
                value = referenceId,
                onValueChange = { if (it.length <= 60) referenceId = it },
                label = { Text("Transaction ID (TID / Reference)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().testTag("deposit_tid_input"),
                colors = walletFieldColors()
            )

            Spacer(Modifier.height(12.dp))
            OutlinedButton(
                onClick = {
                    screenshotPicker.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                },
                enabled = !isCompressing,
                modifier = Modifier.fillMaxWidth().height(50.dp).testTag("pick_screenshot_button"),
                border = BorderStroke(1.dp, if (screenshotBase64 != null) NeonEmerald else NeonCyan)
            ) {
                if (isCompressing) {
                    CircularProgressIndicator(Modifier.size(18.dp), color = NeonCyan, strokeWidth = 2.dp)
                    Spacer(Modifier.width(8.dp))
                    Text("Processing screenshot...", color = NeonCyan)
                } else {
                    Icon(
                        imageVector = if (screenshotBase64 != null) Icons.Filled.CheckCircle else Icons.Filled.AddPhotoAlternate,
                        contentDescription = null,
                        tint = if (screenshotBase64 != null) NeonEmerald else NeonCyan
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = if (screenshotBase64 != null) "Screenshot Attached (Tap to change)" else "Upload Payment Screenshot",
                        color = if (screenshotBase64 != null) NeonEmerald else NeonCyan
                    )
                }
            }

            val preview = remember(screenshotBase64) { ImageUtils.decodeBase64ToImageBitmap(screenshotBase64) }
            if (preview != null) {
                Spacer(Modifier.height(10.dp))
                Image(
                    bitmap = preview,
                    contentDescription = "Payment screenshot preview",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp)
                        .clip(RoundedCornerShape(12.dp)),
                    contentScale = ContentScale.Crop
                )
            }

            if (!localError.isNullOrBlank()) {
                Spacer(Modifier.height(8.dp))
                Text(localError.orEmpty(), style = MaterialTheme.typography.bodyMedium, color = DangerCrimson)
            }

            Spacer(Modifier.height(14.dp))
            Button(
                onClick = {
                    val amt = amountText.toLongOrNull() ?: 0L
                    when {
                        amt !in 50L..500_000L -> localError = "Enter an amount between PKR 50 and PKR 500,000."
                        senderAccount.trim().length < 4 -> localError = "Enter your sender mobile/account number."
                        referenceId.trim().length < 4 -> localError = "Enter a valid Transaction ID (TID)."
                        screenshotBase64.isNullOrBlank() -> localError = "Please upload your payment screenshot."
                        else -> {
                            localError = null
                            onSubmit(amt, method, referenceId, senderAccount, screenshotBase64!!)
                            amountText = ""
                            referenceId = ""
                            screenshotBase64 = null
                        }
                    }
                },
                enabled = !isLoading && !isCompressing,
                modifier = Modifier.fillMaxWidth().height(52.dp).testTag("submit_deposit_button"),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = NeonCyan, contentColor = VoidBlack)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(Modifier.size(20.dp), color = VoidBlack, strokeWidth = 2.dp)
                } else {
                    Text("Submit Deposit Request", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun WithdrawFormSection(
    availableBalance: Long,
    isLoading: Boolean,
    onSubmit: (Long, PaymentMethod, String, String, String?) -> Unit
) {
    var method by remember { mutableStateOf(PaymentMethod.JAZZCASH) }
    var amountText by remember { mutableStateOf("") }
    var accountTitle by remember { mutableStateOf("") }
    var accountNumber by remember { mutableStateOf("") }
    var bankName by remember { mutableStateOf("") }
    var localError by remember { mutableStateOf<String?>(null) }

    CyberCard(modifier = Modifier.fillMaxWidth()) {
        Text("REQUEST WITHDRAWAL", style = MaterialTheme.typography.titleMedium, color = ElectricPurple)
        Spacer(Modifier.height(4.dp))
        Text(
            "Available to withdraw: ${FormatUtils.formatPkr(availableBalance)} (Minimum PKR 100)",
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary
        )
        Spacer(Modifier.height(12.dp))
        PaymentMethodSelector(selected = method, onSelect = { method = it })
        Spacer(Modifier.height(12.dp))

        OutlinedTextField(
            value = amountText,
            onValueChange = { if (it.all(Char::isDigit) && it.length <= 6) amountText = it },
            label = { Text("Withdrawal Amount (PKR)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            modifier = Modifier.fillMaxWidth().testTag("withdraw_amount_input"),
            colors = walletFieldColors()
        )
        Spacer(Modifier.height(10.dp))
        if (method == PaymentMethod.BANK_TRANSFER) {
            OutlinedTextField(
                value = bankName,
                onValueChange = { if (it.length <= 60) bankName = it },
                label = { Text("Bank Name (e.g., Meezan, HBL, SadaPay)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                colors = walletFieldColors()
            )
            Spacer(Modifier.height(10.dp))
        }
        OutlinedTextField(
            value = accountTitle,
            onValueChange = { if (it.length <= 60) accountTitle = it },
            label = { Text("Account Holder Title") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().testTag("withdraw_title_input"),
            colors = walletFieldColors()
        )
        Spacer(Modifier.height(10.dp))
        OutlinedTextField(
            value = accountNumber,
            onValueChange = { if (it.length <= 40) accountNumber = it },
            label = { Text(if (method == PaymentMethod.BANK_TRANSFER) "IBAN / Account Number" else "${method.label} Mobile Number") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().testTag("withdraw_number_input"),
            colors = walletFieldColors()
        )

        val parsedAmount = amountText.toLongOrNull() ?: 0L
        if (parsedAmount > availableBalance) {
            Spacer(Modifier.height(8.dp))
            Text(
                "Requested amount exceeds your available balance of ${FormatUtils.formatPkr(availableBalance)}.",
                style = MaterialTheme.typography.bodyMedium,
                color = DangerCrimson
            )
        } else if (!localError.isNullOrBlank()) {
            Spacer(Modifier.height(8.dp))
            Text(localError.orEmpty(), style = MaterialTheme.typography.bodyMedium, color = DangerCrimson)
        }

        Spacer(Modifier.height(14.dp))
        Button(
            onClick = {
                when {
                    parsedAmount < 100L -> localError = "Minimum withdrawal is PKR 100."
                    parsedAmount > availableBalance -> localError = "Cannot withdraw more than available balance."
                    method == PaymentMethod.BANK_TRANSFER && bankName.trim().isBlank() -> localError = "Please enter your Bank Name."
                    accountTitle.trim().length < 2 -> localError = "Enter account holder title."
                    accountNumber.trim().length < 5 -> localError = "Enter valid account/mobile number."
                    else -> {
                        localError = null
                        onSubmit(parsedAmount, method, accountTitle, accountNumber, bankName.takeIf { method == PaymentMethod.BANK_TRANSFER })
                        amountText = ""
                    }
                }
            },
            enabled = !isLoading && parsedAmount <= availableBalance,
            modifier = Modifier.fillMaxWidth().height(52.dp).testTag("submit_withdraw_button"),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = ElectricPurple, contentColor = TextPrimary)
        ) {
            if (isLoading) {
                CircularProgressIndicator(Modifier.size(20.dp), color = TextPrimary, strokeWidth = 2.dp)
            } else {
                Text("Submit Withdrawal Request", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun WalletHistorySection(
    transactions: List<WalletTransaction>,
    deposits: List<DepositRequest>,
    withdrawals: List<WithdrawalRequest>
) {
    var subTab by remember { mutableIntStateOf(0) }
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        TabRow(
            selectedTabIndex = subTab,
            containerColor = ObsidianCard,
            contentColor = NeonCyan,
            indicator = { tabPositions ->
                SecondaryIndicator(
                    modifier = Modifier.tabIndicatorOffset(tabPositions[subTab]),
                    color = NeonCyan
                )
            }
        ) {
            Tab(selected = subTab == 0, onClick = { subTab = 0 }, text = { Text("Ledger (${transactions.size})") })
            Tab(selected = subTab == 1, onClick = { subTab = 1 }, text = { Text("Deposits (${deposits.size})") })
            Tab(selected = subTab == 2, onClick = { subTab = 2 }, text = { Text("Withdrawals (${withdrawals.size})") })
        }

        when (subTab) {
            0 -> {
                if (transactions.isEmpty()) {
                    EmptyArenaState("No Wallet Transactions", "Every deposit, entry fee, prize win, and withdrawal is logged here.", Icons.Filled.AccountBalanceWallet)
                } else {
                    transactions.forEach { tx ->
                        TransactionRow(tx = tx)
                    }
                }
            }
            1 -> {
                if (deposits.isEmpty()) {
                    EmptyArenaState("No Deposit Requests", "Tap Deposit above to fund your wallet via JazzCash, EasyPaisa, or Bank Transfer.", Icons.Filled.ArrowDownward)
                } else {
                    deposits.forEach { dep ->
                        CyberCard(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("${PaymentMethod.fromRaw(dep.paymentMethod).label} • ${FormatUtils.formatPkr(dep.amount)}", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                                    Text("TID: ${dep.referenceId} • From: ${dep.senderAccount}", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                                    if (!dep.adminNote.isNullOrBlank()) {
                                        Text("Note: ${dep.adminNote}", style = MaterialTheme.typography.bodyMedium, color = AmberGold)
                                    }
                                }
                                StatusBadge(status = dep.status)
                            }
                        }
                    }
                }
            }
            else -> {
                if (withdrawals.isEmpty()) {
                    EmptyArenaState("No Withdrawal Requests", "Cash out your tournament winnings via JazzCash, EasyPaisa, or Bank Transfer.", Icons.Filled.ArrowUpward)
                } else {
                    withdrawals.forEach { wd ->
                        CyberCard(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("${PaymentMethod.fromRaw(wd.paymentMethod).label} • ${FormatUtils.formatPkr(wd.amount)}", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                                    Text("${wd.accountTitle} (${wd.accountNumber})", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                                    if (!wd.adminNote.isNullOrBlank()) {
                                        Text("Note: ${wd.adminNote}", style = MaterialTheme.typography.bodyMedium, color = AmberGold)
                                    }
                                }
                                StatusBadge(status = wd.status)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun walletFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = NeonCyan,
    unfocusedBorderColor = GlassBorder,
    focusedLabelColor = NeonCyan,
    focusedTextColor = TextPrimary,
    unfocusedTextColor = TextPrimary
)
