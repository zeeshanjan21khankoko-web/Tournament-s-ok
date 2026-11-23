package com.example.data.repository

import com.example.data.model.AdminRecord
import com.example.data.model.DepositRequest
import com.example.data.model.PaymentConfig
import com.example.data.model.Tournament
import com.example.data.model.TournamentRegistration
import com.example.data.model.UserProfile
import com.example.data.model.WalletTransaction
import com.example.data.model.WithdrawalRequest
import com.example.util.OperationType
import com.example.util.handleFirestoreError
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.util.UUID

class AdminRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    private fun requireAdminUid(): String =
        auth.currentUser?.uid ?: throw IllegalStateException("Admin must be signed in.")

    fun observeAllUsers(): Flow<List<UserProfile>> =
        db.collection("users").snapshots().map { snap ->
            snap.toObjects(UserProfile::class.java, ESTIMATE)
                .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
        }

    fun observeAllAdmins(): Flow<List<AdminRecord>> =
        db.collection("admins").snapshots().map { snap ->
            snap.toObjects(AdminRecord::class.java, ESTIMATE)
        }

    fun observeAllDeposits(): Flow<List<DepositRequest>> =
        db.collection("deposits").snapshots().map { snap ->
            snap.toObjects(DepositRequest::class.java, ESTIMATE)
                .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
        }

    fun observeAllWithdrawals(): Flow<List<WithdrawalRequest>> =
        db.collection("withdrawals").snapshots().map { snap ->
            snap.toObjects(WithdrawalRequest::class.java, ESTIMATE)
                .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
        }

    fun observeAllTransactions(): Flow<List<WalletTransaction>> =
        db.collection("transactions").snapshots().map { snap ->
            snap.toObjects(WalletTransaction::class.java, ESTIMATE)
                .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
        }

    suspend fun createTournament(
        title: String,
        gameName: String,
        bannerKey: String,
        entryFee: Long,
        prizePool: Long,
        maxSlots: Long,
        matchTimeEpochMs: Long,
        matchTimeLabel: String,
        status: String,
        rulesSummary: String,
        roomId: String?
    ): Result<Unit> {
        var path = "tournaments"
        return try {
            val adminUid = requireAdminUid()
            require(title.trim().length in 3..80) { "Title must be 3-80 characters." }
            require(gameName.trim().length in 2..40) { "Game name must be 2-40 characters." }
            require(maxSlots in 2L..200L) { "Max slots must be between 2 and 200." }
            require(entryFee >= 0L && prizePool >= 0L) { "Fees and prize pool must be >= 0." }

            val id = "tour_${UUID.randomUUID().toString().replace("-", "").take(14)}"
            val docRef = db.collection("tournaments").document(id)
            path = docRef.path

            val payload = mapOf(
                "title" to title.trim(),
                "gameName" to gameName.trim(),
                "bannerKey" to bannerKey,
                "entryFee" to entryFee,
                "prizePool" to prizePool,
                "maxSlots" to maxSlots,
                "filledSlots" to 0L,
                "matchTimeEpochMs" to matchTimeEpochMs,
                "matchTimeLabel" to matchTimeLabel.trim(),
                "status" to status,
                "rulesSummary" to rulesSummary.trim(),
                "roomId" to roomId?.trim()?.takeIf { it.isNotBlank() },
                "createdBy" to adminUid,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            ).filterValues { it != null }

            docRef.set(payload).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun updateTournament(
        tournamentId: String,
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
    ): Result<Unit> {
        val path = "tournaments/$tournamentId"
        return try {
            requireAdminUid()
            require(title.trim().length in 3..80) { "Title must be 3-80 characters." }
            require(maxSlots in 2L..200L) { "Max slots must be between 2 and 200." }
            val docRef = db.collection("tournaments").document(tournamentId)
            val updates = mapOf(
                "title" to title.trim(),
                "gameName" to gameName.trim(),
                "bannerKey" to bannerKey,
                "entryFee" to entryFee,
                "prizePool" to prizePool,
                "maxSlots" to maxSlots,
                "matchTimeLabel" to matchTimeLabel.trim(),
                "status" to status,
                "rulesSummary" to rulesSummary.trim(),
                "roomId" to (roomId?.trim()?.takeIf { it.isNotBlank() }),
                "updatedAt" to FieldValue.serverTimestamp()
            ).filterValues { it != null }
            docRef.update(updates).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun deleteTournament(tournamentId: String): Result<Unit> {
        val path = "tournaments/$tournamentId"
        return try {
            requireAdminUid()
            db.collection("tournaments").document(tournamentId).delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    suspend fun addPlayerToTournament(tournament: Tournament, user: UserProfile): Result<Unit> {
        val path = "tournaments/${tournament.id}/registrations/${user.userId}"
        return try {
            requireAdminUid()
            val tRef = db.collection("tournaments").document(tournament.id)
            val regRef = tRef.collection("registrations").document(user.userId)
            db.runTransaction { tx ->
                val tSnap = tx.get(tRef)
                val regSnap = tx.get(regRef)
                if (!tSnap.exists()) error("Tournament not found")
                if (regSnap.exists()) error("${user.username} is already registered.")
                val filled = tSnap.getLong("filledSlots") ?: 0L
                val max = tSnap.getLong("maxSlots") ?: 0L
                if (filled >= max) error("Tournament slots are full.")

                tx.update(tRef, mapOf("filledSlots" to (filled + 1L), "updatedAt" to FieldValue.serverTimestamp()))
                val payload = mapOf(
                    "userId" to user.userId,
                    "tournamentId" to tournament.id,
                    "username" to user.username,
                    "playerId" to user.playerId,
                    "photoBase64" to user.photoBase64,
                    "entryFeePaid" to 0L,
                    "prizeWon" to 0L,
                    "joinedAt" to FieldValue.serverTimestamp()
                ).filterValues { it != null }
                tx.set(regRef, payload)
            }.await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun removePlayerFromTournament(tournamentId: String, userId: String): Result<Unit> {
        val path = "tournaments/$tournamentId/registrations/$userId"
        return try {
            requireAdminUid()
            val tRef = db.collection("tournaments").document(tournamentId)
            val regRef = tRef.collection("registrations").document(userId)
            db.runTransaction { tx ->
                val tSnap = tx.get(tRef)
                val regSnap = tx.get(regRef)
                if (!regSnap.exists()) error("Player registration not found.")
                val filled = (tSnap.getLong("filledSlots") ?: 1L).coerceAtLeast(1L)
                tx.delete(regRef)
                tx.update(tRef, mapOf("filledSlots" to (filled - 1L), "updatedAt" to FieldValue.serverTimestamp()))
            }.await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    suspend fun declareWinner(
        tournament: Tournament,
        registration: TournamentRegistration,
        placement: String,
        prizeAmount: Long
    ): Result<Unit> {
        val path = "tournaments/${tournament.id}"
        return try {
            val adminUid = requireAdminUid()
            require(prizeAmount >= 0L) { "Prize amount must be >= 0." }
            val tRef = db.collection("tournaments").document(tournament.id)
            val regRef = tRef.collection("registrations").document(registration.userId)
            val userRef = db.collection("users").document(registration.userId)
            val winnerId = "win_${tournament.id.take(16)}_${registration.userId.take(12)}"
                .replace(Regex("[^a-zA-Z0-9_\\-]"), "_")
            val winnerRef = db.collection("winners").document(winnerId)
            val txId = "tx_win_${UUID.randomUUID().toString().replace("-", "").take(14)}"
            val txRef = db.collection("transactions").document(txId)

            db.runTransaction { tx ->
                val userSnap = tx.get(userRef)
                val curBalance = userSnap.getLong("walletBalance") ?: 0L
                val curWins = userSnap.getLong("wins") ?: 0L

                tx.update(
                    tRef,
                    mapOf(
                        "status" to "COMPLETED",
                        "winnerUserId" to registration.userId,
                        "winnerUsername" to registration.username,
                        "winnerPlayerId" to registration.playerId,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
                tx.update(
                    regRef,
                    mapOf(
                        "placement" to placement.trim(),
                        "prizeWon" to prizeAmount
                    )
                )
                if (userSnap.exists()) {
                    tx.update(
                        userRef,
                        mapOf(
                            "walletBalance" to (curBalance + prizeAmount),
                            "wins" to (curWins + 1L),
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    )
                }
                tx.set(
                    winnerRef,
                    mapOf(
                        "tournamentId" to tournament.id,
                        "tournamentTitle" to tournament.title,
                        "gameName" to tournament.gameName,
                        "userId" to registration.userId,
                        "username" to registration.username,
                        "playerId" to registration.playerId,
                        "prizeAmount" to prizeAmount,
                        "placement" to placement.trim(),
                        "declaredBy" to adminUid,
                        "declaredAt" to FieldValue.serverTimestamp()
                    )
                )
                if (prizeAmount > 0L) {
                    tx.set(
                        txRef,
                        mapOf(
                            "userId" to registration.userId,
                            "username" to registration.username,
                            "type" to "PRIZE_WIN",
                            "amount" to prizeAmount,
                            "direction" to "CREDIT",
                            "status" to "COMPLETED",
                            "description" to "$placement in ${tournament.title}",
                            "referenceId" to tournament.id,
                            "createdAt" to FieldValue.serverTimestamp(),
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    )
                }
            }.await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun approveDeposit(deposit: DepositRequest, note: String = "Verified by Admin"): Result<Unit> {
        val path = "deposits/${deposit.id}"
        return try {
            val adminUid = requireAdminUid()
            val depRef = db.collection("deposits").document(deposit.id)
            val userRef = db.collection("users").document(deposit.userId)
            val txRef = db.collection("transactions").document("tx_dep_${deposit.id}")

            db.runTransaction { tx ->
                val depSnap = tx.get(depRef)
                val userSnap = tx.get(userRef)
                val status = depSnap.getString("status") ?: "PENDING"
                if (status != "PENDING") error("Deposit has already been processed ($status).")
                val curBalance = userSnap.getLong("walletBalance") ?: 0L

                tx.update(
                    depRef,
                    mapOf(
                        "status" to "APPROVED",
                        "reviewedBy" to adminUid,
                        "adminNote" to note.trim().take(180),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
                tx.update(
                    userRef,
                    mapOf(
                        "walletBalance" to (curBalance + deposit.amount),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
                if (tx.get(txRef).exists()) {
                    tx.update(
                        txRef,
                        mapOf(
                            "status" to "COMPLETED",
                            "description" to "Deposit Approved (TID: ${deposit.referenceId})",
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    )
                }
            }.await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun rejectDeposit(deposit: DepositRequest, note: String): Result<Unit> {
        val path = "deposits/${deposit.id}"
        return try {
            val adminUid = requireAdminUid()
            val depRef = db.collection("deposits").document(deposit.id)
            val txRef = db.collection("transactions").document("tx_dep_${deposit.id}")
            val reason = note.trim().ifBlank { "Payment could not be verified" }.take(180)

            db.runTransaction { tx ->
                val depSnap = tx.get(depRef)
                val status = depSnap.getString("status") ?: "PENDING"
                if (status != "PENDING") error("Deposit already processed.")
                tx.update(
                    depRef,
                    mapOf(
                        "status" to "REJECTED",
                        "reviewedBy" to adminUid,
                        "adminNote" to reason,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
                if (tx.get(txRef).exists()) {
                    tx.update(
                        txRef,
                        mapOf(
                            "status" to "REJECTED",
                            "description" to "Deposit Rejected: $reason".take(160),
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    )
                }
            }.await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun approveWithdrawal(withdrawal: WithdrawalRequest, note: String = "Paid by Admin"): Result<Unit> {
        val path = "withdrawals/${withdrawal.id}"
        return try {
            val adminUid = requireAdminUid()
            val wdRef = db.collection("withdrawals").document(withdrawal.id)
            val txRef = db.collection("transactions").document("tx_wd_${withdrawal.id}")

            db.runTransaction { tx ->
                val wdSnap = tx.get(wdRef)
                if ((wdSnap.getString("status") ?: "PENDING") != "PENDING") {
                    error("Withdrawal already processed.")
                }
                tx.update(
                    wdRef,
                    mapOf(
                        "status" to "APPROVED",
                        "reviewedBy" to adminUid,
                        "adminNote" to note.trim().take(180),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
                if (tx.get(txRef).exists()) {
                    tx.update(
                        txRef,
                        mapOf(
                            "status" to "COMPLETED",
                            "description" to "Withdrawal Paid (${withdrawal.accountNumber})",
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    )
                }
            }.await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun rejectWithdrawal(withdrawal: WithdrawalRequest, note: String): Result<Unit> {
        val path = "withdrawals/${withdrawal.id}"
        return try {
            val adminUid = requireAdminUid()
            val wdRef = db.collection("withdrawals").document(withdrawal.id)
            val userRef = db.collection("users").document(withdrawal.userId)
            val txRef = db.collection("transactions").document("tx_wd_${withdrawal.id}")
            val refundTxRef = db.collection("transactions").document("tx_ref_${withdrawal.id}")
            val reason = note.trim().ifBlank { "Rejected by Admin" }.take(180)

            db.runTransaction { tx ->
                val wdSnap = tx.get(wdRef)
                val userSnap = tx.get(userRef)
                if ((wdSnap.getString("status") ?: "PENDING") != "PENDING") {
                    error("Withdrawal already processed.")
                }
                val curBalance = userSnap.getLong("walletBalance") ?: 0L

                tx.update(
                    wdRef,
                    mapOf(
                        "status" to "REJECTED",
                        "reviewedBy" to adminUid,
                        "adminNote" to reason,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
                if (userSnap.exists()) {
                    tx.update(
                        userRef,
                        mapOf(
                            "walletBalance" to (curBalance + withdrawal.amount),
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    )
                }
                if (tx.get(txRef).exists()) {
                    tx.update(
                        txRef,
                        mapOf(
                            "status" to "REJECTED",
                            "description" to "Withdrawal Rejected: $reason".take(160),
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    )
                }
                tx.set(
                    refundTxRef,
                    mapOf(
                        "userId" to withdrawal.userId,
                        "username" to withdrawal.username,
                        "type" to "WITHDRAWAL_REFUND",
                        "amount" to withdrawal.amount,
                        "direction" to "CREDIT",
                        "status" to "COMPLETED",
                        "description" to "Refund for rejected withdrawal",
                        "referenceId" to withdrawal.id,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
            }.await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun setUserBlocked(userId: String, blocked: Boolean): Result<Unit> {
        val path = "users/$userId"
        return try {
            requireAdminUid()
            db.collection("users").document(userId).update(
                mapOf("blocked" to blocked, "updatedAt" to FieldValue.serverTimestamp())
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun adjustUserBalance(user: UserProfile, delta: Long, reason: String): Result<Unit> {
        val path = "users/${user.userId}"
        return try {
            requireAdminUid()
            require(delta != 0L) { "Adjustment cannot be zero." }
            val userRef = db.collection("users").document(user.userId)
            val txId = "tx_adj_${UUID.randomUUID().toString().replace("-", "").take(14)}"
            val txRef = db.collection("transactions").document(txId)

            db.runTransaction { tx ->
                val snap = tx.get(userRef)
                val cur = snap.getLong("walletBalance") ?: 0L
                val next = cur + delta
                if (next < 0L) error("Resulting wallet balance cannot be negative.")
                tx.update(userRef, mapOf("walletBalance" to next, "updatedAt" to FieldValue.serverTimestamp()))
                tx.set(
                    txRef,
                    mapOf(
                        "userId" to user.userId,
                        "username" to user.username,
                        "type" to "ADMIN_ADJUSTMENT",
                        "amount" to kotlin.math.abs(delta),
                        "direction" to if (delta > 0L) "CREDIT" else "DEBIT",
                        "status" to "COMPLETED",
                        "description" to reason.trim().ifBlank { "Admin wallet adjustment" }.take(160),
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
            }.await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun addUserByAdmin(username: String, email: String, startingBalance: Long): Result<Unit> {
        var path = "users"
        return try {
            requireAdminUid()
            val cleanName = username.trim()
            val cleanEmail = email.trim()
            require(cleanName.length in 3..24) { "Username must be 3-24 characters." }
            require(cleanEmail.contains("@") && cleanEmail.length in 5..160) { "Valid email required." }
            require(startingBalance >= 0L) { "Starting balance cannot be negative." }
            val uid = "usr_${UUID.randomUUID().toString().replace("-", "").take(14)}"
            val playerId = "PT-${(100000..999999).random()}"
            val docRef = db.collection("users").document(uid)
            path = docRef.path
            docRef.set(
                mapOf(
                    "userId" to uid,
                    "username" to cleanName,
                    "email" to cleanEmail,
                    "playerId" to playerId,
                    "walletBalance" to startingBalance,
                    "matchesPlayed" to 0L,
                    "wins" to 0L,
                    "blocked" to false,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun deleteUserRecord(userId: String): Result<Unit> {
        val path = "users/$userId"
        return try {
            requireAdminUid()
            db.collection("users").document(userId).delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    suspend fun setAdminRole(user: UserProfile, makeAdmin: Boolean): Result<Unit> {
        val path = "admins/${user.userId}"
        return try {
            requireAdminUid()
            val ref = db.collection("admins").document(user.userId)
            if (makeAdmin) {
                ref.set(
                    mapOf(
                        "userId" to user.userId,
                        "email" to user.email,
                        "displayName" to user.username,
                        "role" to "ADMIN",
                        "createdAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } else {
                ref.delete().await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun savePaymentConfig(config: PaymentConfig): Result<Unit> {
        val path = "app_config/payment_methods"
        return try {
            requireAdminUid()
            db.collection("app_config").document("payment_methods").set(
                mapOf(
                    "jazzCashTitle" to config.jazzCashTitle.trim(),
                    "jazzCashNumber" to config.jazzCashNumber.trim(),
                    "easyPaisaTitle" to config.easyPaisaTitle.trim(),
                    "easyPaisaNumber" to config.easyPaisaNumber.trim(),
                    "bankName" to config.bankName.trim(),
                    "bankAccountTitle" to config.bankAccountTitle.trim(),
                    "bankAccountNumber" to config.bankAccountNumber.trim(),
                    "instructions" to config.instructions.trim(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }
}
