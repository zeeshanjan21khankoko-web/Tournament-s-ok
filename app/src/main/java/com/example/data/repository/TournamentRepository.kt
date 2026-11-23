package com.example.data.repository

import com.example.data.auth.AuthHelper
import com.example.data.model.DepositRequest
import com.example.data.model.PaymentConfig
import com.example.data.model.PaymentMethod
import com.example.data.model.Tournament
import com.example.data.model.TournamentRegistration
import com.example.data.model.UserProfile
import com.example.data.model.WalletTransaction
import com.example.data.model.WinnerRecord
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
import kotlin.math.abs

class TournamentRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    private fun requireUserId(): String =
        auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")

    fun observeUserProfile(userId: String): Flow<UserProfile?> =
        db.collection("users").document(userId)
            .snapshots()
            .map { snapshot ->
                if (snapshot.exists()) snapshot.toObject(UserProfile::class.java, ESTIMATE) else null
            }

    fun observeIsAdminDoc(userId: String): Flow<Boolean> =
        db.collection("admins").document(userId)
            .snapshots()
            .map { snapshot -> snapshot.exists() || AuthHelper.isBootstrappedAdmin(auth.currentUser) }

    fun observeTournaments(): Flow<List<Tournament>> =
        db.collection("tournaments")
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(Tournament::class.java, ESTIMATE)
                    .sortedWith(
                        compareBy<Tournament> {
                            when (it.status) {
                                "LIVE" -> 0
                                "UPCOMING" -> 1
                                else -> 2
                            }
                        }.thenBy { it.matchTimeEpochMs }
                    )
            }

    fun observeTournamentRegistrations(tournamentId: String): Flow<List<TournamentRegistration>> =
        db.collection("tournaments").document(tournamentId)
            .collection("registrations")
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(TournamentRegistration::class.java, ESTIMATE)
                    .sortedBy { it.joinedAt ?: Timestamp(0, 0) }
            }

    fun observeUserTransactions(userId: String): Flow<List<WalletTransaction>> =
        db.collection("transactions")
            .whereEqualTo("userId", userId)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(WalletTransaction::class.java, ESTIMATE)
                    .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
            }

    fun observeUserDeposits(userId: String): Flow<List<DepositRequest>> =
        db.collection("deposits")
            .whereEqualTo("userId", userId)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(DepositRequest::class.java, ESTIMATE)
                    .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
            }

    fun observeUserWithdrawals(userId: String): Flow<List<WithdrawalRequest>> =
        db.collection("withdrawals")
            .whereEqualTo("userId", userId)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(WithdrawalRequest::class.java, ESTIMATE)
                    .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
            }

    fun observeWinners(): Flow<List<WinnerRecord>> =
        db.collection("winners")
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(WinnerRecord::class.java, ESTIMATE)
                    .sortedByDescending { it.declaredAt ?: Timestamp(0, 0) }
            }

    fun observePaymentConfig(): Flow<PaymentConfig> =
        db.collection("app_config").document("payment_methods")
            .snapshots()
            .map { snapshot ->
                if (snapshot.exists()) {
                    snapshot.toObject(PaymentConfig::class.java, ESTIMATE) ?: PaymentConfig()
                } else {
                    PaymentConfig()
                }
            }

    suspend fun createProfile(
        username: String,
        email: String,
        photoBase64: String?
    ): Result<Unit> {
        val cleanName = username.trim()
        var docPath = "users"
        return try {
            val uid = requireUserId()
            require(cleanName.length in 3..24) { "Username must be between 3 and 24 characters." }
            val numericCode = (abs(uid.hashCode()) % 900000) + 100000
            val playerId = "PT-$numericCode"
            val docRef = db.collection("users").document(uid)
            docPath = docRef.path

            val payload = mapOf(
                "userId" to uid,
                "username" to cleanName,
                "email" to email.ifBlank { auth.currentUser?.email ?: "player@protournament.pk" },
                "playerId" to playerId,
                "photoBase64" to photoBase64,
                "walletBalance" to 0L,
                "matchesPlayed" to 0L,
                "wins" to 0L,
                "blocked" to false,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            ).filterValues { it != null }

            docRef.set(payload).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun updateProfile(
        username: String,
        photoBase64: String?
    ): Result<Unit> {
        val cleanName = username.trim()
        var docPath = "users"
        return try {
            val uid = requireUserId()
            require(cleanName.length in 3..24) { "Username must be between 3 and 24 characters." }
            val docRef = db.collection("users").document(uid)
            docPath = docRef.path

            val updates = mutableMapOf<String, Any?>(
                "username" to cleanName,
                "updatedAt" to FieldValue.serverTimestamp()
            )
            if (photoBase64 != null) {
                updates["photoBase64"] = photoBase64
            }
            docRef.update(updates.filterValues { it != null }).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun joinTournament(
        tournamentId: String,
        userProfile: UserProfile
    ): Result<Unit> {
        var path = "tournaments/$tournamentId"
        return try {
            val uid = requireUserId()
            check(!userProfile.blocked) { "Account is suspended. Contact support." }
            val tournamentRef = db.collection("tournaments").document(tournamentId)
            val userRef = db.collection("users").document(uid)
            val regRef = tournamentRef.collection("registrations").document(uid)
            val txId = "tx_join_${tournamentId.take(16)}_${uid.take(12)}_${System.currentTimeMillis()}"
                .replace(Regex("[^a-zA-Z0-9_\\-]"), "_")
            val txRef = db.collection("transactions").document(txId)
            path = tournamentRef.path

            db.runTransaction { tx ->
                val tSnap = tx.get(tournamentRef)
                val uSnap = tx.get(userRef)
                val existingReg = tx.get(regRef)

                if (!tSnap.exists()) error("Tournament no longer exists.")
                if (!uSnap.exists()) error("Player profile not found.")
                if (existingReg.exists()) error("You are already registered for this tournament.")

                val status = tSnap.getString("status") ?: "UPCOMING"
                if (status == "COMPLETED") error("This tournament has already ended.")

                val filled = tSnap.getLong("filledSlots") ?: 0L
                val max = tSnap.getLong("maxSlots") ?: 0L
                if (filled >= max) error("Tournament is full. No slots remaining.")

                val entryFee = tSnap.getLong("entryFee") ?: 0L
                val balance = uSnap.getLong("walletBalance") ?: 0L
                val matchesPlayed = uSnap.getLong("matchesPlayed") ?: 0L
                val blocked = uSnap.getBoolean("blocked") ?: false
                if (blocked) error("Your account is suspended.")
                if (balance < entryFee) {
                    error("Insufficient wallet balance. Need PKR $entryFee (Available: PKR $balance).")
                }

                val title = tSnap.getString("title") ?: "Tournament"

                tx.update(
                    userRef,
                    mapOf(
                        "walletBalance" to (balance - entryFee),
                        "matchesPlayed" to (matchesPlayed + 1L),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
                tx.update(
                    tournamentRef,
                    mapOf(
                        "filledSlots" to (filled + 1L),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
                val regPayload = mapOf(
                    "userId" to uid,
                    "tournamentId" to tournamentId,
                    "username" to userProfile.username,
                    "playerId" to userProfile.playerId,
                    "photoBase64" to userProfile.photoBase64,
                    "entryFeePaid" to entryFee,
                    "prizeWon" to 0L,
                    "joinedAt" to FieldValue.serverTimestamp()
                ).filterValues { it != null }
                tx.set(regRef, regPayload)

                if (entryFee > 0L) {
                    tx.set(
                        txRef,
                        mapOf(
                            "userId" to uid,
                            "username" to userProfile.username,
                            "type" to "ENTRY_FEE",
                            "amount" to entryFee,
                            "direction" to "DEBIT",
                            "status" to "COMPLETED",
                            "description" to "Joined $title",
                            "referenceId" to tournamentId,
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

    suspend fun submitDepositRequest(
        userProfile: UserProfile,
        amount: Long,
        paymentMethod: PaymentMethod,
        referenceId: String,
        senderAccount: String,
        screenshotBase64: String
    ): Result<Unit> {
        val cleanRef = referenceId.trim().uppercase()
        val cleanSender = senderAccount.trim()
        var path = "deposits"
        return try {
            val uid = requireUserId()
            check(!userProfile.blocked) { "Account is suspended." }
            require(amount in 50L..500_000L) { "Deposit amount must be between PKR 50 and PKR 500,000." }
            require(cleanRef.length in 4..60) { "Transaction ID must be 4-60 characters." }
            require(cleanSender.length in 4..40) { "Sender account number is required." }
            require(screenshotBase64.length >= 20) { "Please attach a payment screenshot." }

            val sanitizedRefKey = "${paymentMethod.name}_${cleanRef}"
                .replace(Regex("[^a-zA-Z0-9_\\-]"), "_")
                .take(120)
            val refLockDoc = db.collection("used_references").document(sanitizedRefKey)

            // Pre-check duplicate transaction ID
            val existingLock = refLockDoc.get().await()
            if (existingLock.exists()) {
                return Result.failure(IllegalStateException("This Transaction ID ($cleanRef) has already been submitted."))
            }

            val depositId = "dep_${UUID.randomUUID().toString().replace("-", "").take(16)}"
            val depositRef = db.collection("deposits").document(depositId)
            val txRef = db.collection("transactions").document("tx_dep_$depositId")
            path = depositRef.path

            val batch = db.batch()
            batch.set(
                depositRef,
                mapOf(
                    "userId" to uid,
                    "username" to userProfile.username,
                    "playerId" to userProfile.playerId,
                    "amount" to amount,
                    "paymentMethod" to paymentMethod.name,
                    "referenceId" to cleanRef,
                    "senderAccount" to cleanSender,
                    "screenshotBase64" to screenshotBase64,
                    "status" to "PENDING",
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            batch.set(
                refLockDoc,
                mapOf(
                    "refKey" to sanitizedRefKey,
                    "userId" to uid,
                    "depositId" to depositId,
                    "createdAt" to FieldValue.serverTimestamp()
                )
            )
            batch.set(
                txRef,
                mapOf(
                    "userId" to uid,
                    "username" to userProfile.username,
                    "type" to "DEPOSIT",
                    "amount" to amount,
                    "direction" to "CREDIT",
                    "status" to "PENDING",
                    "description" to "${paymentMethod.label} Deposit (TID: $cleanRef)",
                    "referenceId" to depositId,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun submitWithdrawalRequest(
        userProfile: UserProfile,
        amount: Long,
        paymentMethod: PaymentMethod,
        accountTitle: String,
        accountNumber: String,
        bankName: String?
    ): Result<Unit> {
        val cleanTitle = accountTitle.trim()
        val cleanNumber = accountNumber.trim()
        val cleanBank = bankName?.trim()?.takeIf { it.isNotBlank() }
        var path = "withdrawals"
        return try {
            val uid = requireUserId()
            check(!userProfile.blocked) { "Account is suspended." }
            require(amount in 100L..500_000L) { "Minimum withdrawal is PKR 100." }
            require(cleanTitle.length in 2..60) { "Account title is required." }
            require(cleanNumber.length in 5..40) { "Valid account/mobile number is required." }
            if (paymentMethod == PaymentMethod.BANK_TRANSFER) {
                require(!cleanBank.isNullOrBlank()) { "Please enter your Bank Name." }
            }

            val withdrawalId = "wd_${UUID.randomUUID().toString().replace("-", "").take(16)}"
            val wdRef = db.collection("withdrawals").document(withdrawalId)
            val userRef = db.collection("users").document(uid)
            val txRef = db.collection("transactions").document("tx_wd_$withdrawalId")
            path = wdRef.path

            db.runTransaction { tx ->
                val uSnap = tx.get(userRef)
                val balance = uSnap.getLong("walletBalance") ?: 0L
                val blocked = uSnap.getBoolean("blocked") ?: false
                if (blocked) error("Account is suspended.")
                if (balance < amount) {
                    error("Cannot withdraw PKR $amount. Available balance is PKR $balance.")
                }

                tx.update(
                    userRef,
                    mapOf(
                        "walletBalance" to (balance - amount),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
                val wdPayload = mapOf(
                    "userId" to uid,
                    "username" to userProfile.username,
                    "playerId" to userProfile.playerId,
                    "amount" to amount,
                    "paymentMethod" to paymentMethod.name,
                    "accountTitle" to cleanTitle,
                    "accountNumber" to cleanNumber,
                    "bankName" to cleanBank,
                    "status" to "PENDING",
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                ).filterValues { it != null }
                tx.set(wdRef, wdPayload)

                tx.set(
                    txRef,
                    mapOf(
                        "userId" to uid,
                        "username" to userProfile.username,
                        "type" to "WITHDRAWAL",
                        "amount" to amount,
                        "direction" to "DEBIT",
                        "status" to "PENDING",
                        "description" to "${paymentMethod.label} Withdrawal ($cleanNumber)",
                        "referenceId" to withdrawalId,
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
}
