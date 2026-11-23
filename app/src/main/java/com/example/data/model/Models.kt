package com.example.data.model

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

enum class TournamentStatus(val label: String) {
    UPCOMING("Upcoming"),
    LIVE("Live"),
    COMPLETED("Completed");

    companion object {
        fun fromRaw(raw: String?): TournamentStatus =
            entries.firstOrNull { it.name == raw } ?: UPCOMING
    }
}

enum class PaymentMethod(val label: String, val shortCode: String) {
    JAZZCASH("JazzCash", "JC"),
    EASYPAISA("EasyPaisa", "EP"),
    BANK_TRANSFER("Bank Transfer", "BANK");

    companion object {
        fun fromRaw(raw: String?): PaymentMethod =
            entries.firstOrNull { it.name == raw } ?: JAZZCASH
    }
}

enum class RequestStatus(val label: String) {
    PENDING("Pending"),
    APPROVED("Approved"),
    REJECTED("Rejected");

    companion object {
        fun fromRaw(raw: String?): RequestStatus =
            entries.firstOrNull { it.name == raw } ?: PENDING
    }
}

enum class TransactionType(val label: String) {
    DEPOSIT("Deposit"),
    WITHDRAWAL("Withdrawal"),
    WITHDRAWAL_REFUND("Withdrawal Refund"),
    ENTRY_FEE("Tournament Entry"),
    PRIZE_WIN("Prize Won"),
    ADMIN_ADJUSTMENT("Admin Adjustment");

    companion object {
        fun fromRaw(raw: String?): TransactionType =
            entries.firstOrNull { it.name == raw } ?: DEPOSIT
    }
}

enum class TournamentBannerKey(val key: String, val label: String) {
    ARENA("arena", "Championship Stage"),
    BATTLE_ROYALE("battle_royale", "Battle Royale"),
    TACTICAL_FPS("tactical_fps", "Tactical FPS");

    companion object {
        fun fromRaw(raw: String?): TournamentBannerKey =
            entries.firstOrNull { it.key == raw } ?: ARENA
    }
}

@Keep
@IgnoreExtraProperties
data class UserProfile(
    @DocumentId val id: String = "",
    val userId: String = "",
    val username: String = "",
    val email: String = "",
    val playerId: String = "",
    val photoBase64: String? = null,
    val walletBalance: Long = 0L,
    val matchesPlayed: Long = 0L,
    val wins: Long = 0L,
    val blocked: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class AdminRecord(
    @DocumentId val id: String = "",
    val userId: String = "",
    val email: String = "",
    val displayName: String = "",
    val role: String = "ADMIN",
    @ServerTimestamp val createdAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class Tournament(
    @DocumentId val id: String = "",
    val title: String = "",
    val gameName: String = "",
    val bannerKey: String = "arena",
    val entryFee: Long = 0L,
    val prizePool: Long = 0L,
    val maxSlots: Long = 16L,
    val filledSlots: Long = 0L,
    val matchTimeEpochMs: Long = 0L,
    val matchTimeLabel: String = "",
    val status: String = "UPCOMING",
    val rulesSummary: String = "",
    val roomId: String? = null,
    val winnerUserId: String? = null,
    val winnerUsername: String? = null,
    val winnerPlayerId: String? = null,
    val createdBy: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    val availableSlots: Long get() = (maxSlots - filledSlots).coerceAtLeast(0L)
    val parsedStatus: TournamentStatus get() = TournamentStatus.fromRaw(status)
    val parsedBanner: TournamentBannerKey get() = TournamentBannerKey.fromRaw(bannerKey)
}

@Keep
@IgnoreExtraProperties
data class TournamentRegistration(
    @DocumentId val id: String = "",
    val userId: String = "",
    val tournamentId: String = "",
    val username: String = "",
    val playerId: String = "",
    val photoBase64: String? = null,
    val entryFeePaid: Long = 0L,
    val placement: String? = null,
    val prizeWon: Long = 0L,
    @ServerTimestamp val joinedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class DepositRequest(
    @DocumentId val id: String = "",
    val userId: String = "",
    val username: String = "",
    val playerId: String = "",
    val amount: Long = 0L,
    val paymentMethod: String = "JAZZCASH",
    val referenceId: String = "",
    val senderAccount: String = "",
    val screenshotBase64: String = "",
    val status: String = "PENDING",
    val adminNote: String? = null,
    val reviewedBy: String? = null,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class WithdrawalRequest(
    @DocumentId val id: String = "",
    val userId: String = "",
    val username: String = "",
    val playerId: String = "",
    val amount: Long = 0L,
    val paymentMethod: String = "JAZZCASH",
    val accountTitle: String = "",
    val accountNumber: String = "",
    val bankName: String? = null,
    val status: String = "PENDING",
    val adminNote: String? = null,
    val reviewedBy: String? = null,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class WalletTransaction(
    @DocumentId val id: String = "",
    val userId: String = "",
    val username: String = "",
    val type: String = "DEPOSIT",
    val amount: Long = 0L,
    val direction: String = "CREDIT",
    val status: String = "PENDING",
    val description: String = "",
    val referenceId: String? = null,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class WinnerRecord(
    @DocumentId val id: String = "",
    val tournamentId: String = "",
    val tournamentTitle: String = "",
    val gameName: String = "",
    val userId: String = "",
    val username: String = "",
    val playerId: String = "",
    val prizeAmount: Long = 0L,
    val placement: String = "1st Place",
    val declaredBy: String = "",
    @ServerTimestamp val declaredAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class PaymentConfig(
    @DocumentId val id: String = "",
    val jazzCashTitle: String = "Pro Tournament Official",
    val jazzCashNumber: String = "0300-8459201",
    val easyPaisaTitle: String = "Pro Tournament Official",
    val easyPaisaNumber: String = "0345-8459201",
    val bankName: String = "Meezan Bank",
    val bankAccountTitle: String = "Pro Tournament Esports",
    val bankAccountNumber: String = "PK42MEZN0002810104829103",
    val instructions: String = "Send the exact deposit amount to the selected account, then enter your Transaction ID (TID) and upload a clear payment screenshot. Admin verifies deposits within 10-30 minutes.",
    @ServerTimestamp val updatedAt: Timestamp? = null
)
