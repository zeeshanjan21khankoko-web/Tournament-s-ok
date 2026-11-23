package com.example.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
import com.example.data.repository.AdminRepository
import com.example.data.repository.TournamentRepository
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

@OptIn(ExperimentalCoroutinesApi::class)
class MainViewModel(
    private val tournamentRepo: TournamentRepository,
    private val adminRepo: AdminRepository,
    val currentUserId: String,
    val userEmail: String,
    val displayName: String
) : ViewModel() {

    private val _selectedTournamentId = MutableStateFlow<String?>(null)

    private val _actionMessage = MutableStateFlow<String?>(null)
    val actionMessage: StateFlow<String?> = _actionMessage.asStateFlow()

    private val _isActionLoading = MutableStateFlow(false)
    val isActionLoading: StateFlow<Boolean> = _isActionLoading.asStateFlow()

    val userProfileState: StateFlow<UiState<UserProfile?>> =
        tournamentRepo.observeUserProfile(currentUserId)
            .toUiStateFlow("Failed to load player profile")

    val isAdmin: StateFlow<Boolean> =
        tournamentRepo.observeIsAdminDoc(currentUserId)
            .catch { error ->
                unwrapFirestoreOrThrow(error, "observeIsAdminDoc")
                emit(false)
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), false)

    val tournamentsState: StateFlow<UiState<List<Tournament>>> =
        tournamentRepo.observeTournaments()
            .toUiStateFlow("Failed to load tournaments")

    val selectedRegistrations: StateFlow<List<TournamentRegistration>> =
        _selectedTournamentId.flatMapLatest { id ->
            if (id.isNullOrBlank()) flowOf(emptyList())
            else tournamentRepo.observeTournamentRegistrations(id)
                .catch { error ->
                    unwrapFirestoreOrThrow(error, "observeRegistrations")
                    emit(emptyList())
                }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val userTransactions: StateFlow<List<WalletTransaction>> =
        tournamentRepo.observeUserTransactions(currentUserId)
            .catch { error ->
                unwrapFirestoreOrThrow(error, "observeUserTransactions")
                emit(emptyList())
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val userDeposits: StateFlow<List<DepositRequest>> =
        tournamentRepo.observeUserDeposits(currentUserId)
            .catch { error ->
                unwrapFirestoreOrThrow(error, "observeUserDeposits")
                emit(emptyList())
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val userWithdrawals: StateFlow<List<WithdrawalRequest>> =
        tournamentRepo.observeUserWithdrawals(currentUserId)
            .catch { error ->
                unwrapFirestoreOrThrow(error, "observeUserWithdrawals")
                emit(emptyList())
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val winners: StateFlow<List<WinnerRecord>> =
        tournamentRepo.observeWinners()
            .catch { error ->
                unwrapFirestoreOrThrow(error, "observeWinners")
                emit(emptyList())
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val paymentConfig: StateFlow<PaymentConfig> =
        tournamentRepo.observePaymentConfig()
            .catch { error ->
                unwrapFirestoreOrThrow(error, "observePaymentConfig")
                emit(PaymentConfig())
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), PaymentConfig())

    // Admin-only streams: only subscribed when isAdmin == true
    val adminUsers: StateFlow<List<UserProfile>> =
        isAdmin.flatMapLatest { admin ->
            if (admin) adminRepo.observeAllUsers().catch { unwrapFirestoreOrThrow(it, "adminUsers"); emit(emptyList()) }
            else flowOf(emptyList())
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val adminList: StateFlow<List<AdminRecord>> =
        isAdmin.flatMapLatest { admin ->
            if (admin) adminRepo.observeAllAdmins().catch { unwrapFirestoreOrThrow(it, "adminList"); emit(emptyList()) }
            else flowOf(emptyList())
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val adminDeposits: StateFlow<List<DepositRequest>> =
        isAdmin.flatMapLatest { admin ->
            if (admin) adminRepo.observeAllDeposits().catch { unwrapFirestoreOrThrow(it, "adminDeposits"); emit(emptyList()) }
            else flowOf(emptyList())
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val adminWithdrawals: StateFlow<List<WithdrawalRequest>> =
        isAdmin.flatMapLatest { admin ->
            if (admin) adminRepo.observeAllWithdrawals().catch { unwrapFirestoreOrThrow(it, "adminWithdrawals"); emit(emptyList()) }
            else flowOf(emptyList())
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val adminTransactions: StateFlow<List<WalletTransaction>> =
        isAdmin.flatMapLatest { admin ->
            if (admin) adminRepo.observeAllTransactions().catch { unwrapFirestoreOrThrow(it, "adminTransactions"); emit(emptyList()) }
            else flowOf(emptyList())
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    fun selectTournament(tournamentId: String?) {
        _selectedTournamentId.value = tournamentId
    }

    fun clearMessage() {
        _actionMessage.value = null
    }

    private fun <T> Flow<T>.toUiStateFlow(fallbackMessage: String): StateFlow<UiState<T>> =
        this.map<T, UiState<T>> { UiState.Success(it) }
            .catch { error ->
                val root = unwrapFirestoreOrThrow(error, fallbackMessage)
                emit(UiState.Error(root.message ?: fallbackMessage))
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000L),
                initialValue = UiState.Loading
            )

    private fun unwrapFirestoreOrThrow(error: Throwable, tagContext: String): Throwable {
        val firestoreError = generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
        if (firestoreError == null && error is CancellationException) throw error
        val rootError = firestoreError ?: error
        Log.w("MainVM", "$tagContext error", rootError)
        return rootError
    }

    private fun launchAction(successMsg: String, block: suspend () -> Result<Unit>) {
        viewModelScope.launch {
            _isActionLoading.value = true
            val res = block()
            _isActionLoading.value = false
            _actionMessage.value = res.fold(
                onSuccess = { successMsg },
                onFailure = { it.localizedMessage ?: "Operation failed" }
            )
        }
    }

    // --- Player Actions ---
    fun createProfile(username: String, photoBase64: String?) =
        launchAction("Player profile activated!") {
            tournamentRepo.createProfile(username, userEmail, photoBase64)
        }

    fun updateProfile(username: String, photoBase64: String?) =
        launchAction("Profile updated!") {
            tournamentRepo.updateProfile(username, photoBase64)
        }

    fun joinTournament(tournamentId: String, profile: UserProfile) =
        launchAction("Successfully joined tournament! Slot reserved.") {
            tournamentRepo.joinTournament(tournamentId, profile)
        }

    fun submitDeposit(
        profile: UserProfile,
        amount: Long,
        method: PaymentMethod,
        referenceId: String,
        senderAccount: String,
        screenshotBase64: String
    ) = launchAction("Deposit submitted! Pending admin verification.") {
        tournamentRepo.submitDepositRequest(profile, amount, method, referenceId, senderAccount, screenshotBase64)
    }

    fun submitWithdrawal(
        profile: UserProfile,
        amount: Long,
        method: PaymentMethod,
        accountTitle: String,
        accountNumber: String,
        bankName: String?
    ) = launchAction("Withdrawal request submitted! Amount held safely.") {
        tournamentRepo.submitWithdrawalRequest(profile, amount, method, accountTitle, accountNumber, bankName)
    }

    // --- Admin Actions ---
    fun createTournament(
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
    ) = launchAction("Tournament created!") {
        adminRepo.createTournament(title, gameName, bannerKey, entryFee, prizePool, maxSlots, matchTimeEpochMs, matchTimeLabel, status, rulesSummary, roomId)
    }

    fun updateTournament(
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
    ) = launchAction("Tournament updated!") {
        adminRepo.updateTournament(tournamentId, title, gameName, bannerKey, entryFee, prizePool, maxSlots, matchTimeLabel, status, rulesSummary, roomId)
    }

    fun deleteTournament(tournamentId: String) =
        launchAction("Tournament deleted.") { adminRepo.deleteTournament(tournamentId) }

    fun addPlayerToTournament(tournament: Tournament, user: UserProfile) =
        launchAction("Added ${user.username} to ${tournament.title}.") {
            adminRepo.addPlayerToTournament(tournament, user)
        }

    fun removePlayerFromTournament(tournamentId: String, userId: String) =
        launchAction("Removed player from tournament.") {
            adminRepo.removePlayerFromTournament(tournamentId, userId)
        }

    fun declareWinner(
        tournament: Tournament,
        registration: TournamentRegistration,
        placement: String,
        prizeAmount: Long
    ) = launchAction("Winner declared and prize credited!") {
        adminRepo.declareWinner(tournament, registration, placement, prizeAmount)
    }

    fun approveDeposit(deposit: DepositRequest, note: String) =
        launchAction("Deposit approved & PKR ${deposit.amount} credited!") {
            adminRepo.approveDeposit(deposit, note)
        }

    fun rejectDeposit(deposit: DepositRequest, note: String) =
        launchAction("Deposit rejected.") { adminRepo.rejectDeposit(deposit, note) }

    fun approveWithdrawal(withdrawal: WithdrawalRequest, note: String) =
        launchAction("Withdrawal marked as paid.") { adminRepo.approveWithdrawal(withdrawal, note) }

    fun rejectWithdrawal(withdrawal: WithdrawalRequest, note: String) =
        launchAction("Withdrawal rejected & PKR ${withdrawal.amount} refunded.") {
            adminRepo.rejectWithdrawal(withdrawal, note)
        }

    fun toggleUserBlocked(userId: String, blocked: Boolean) =
        launchAction(if (blocked) "Account blocked." else "Account unblocked.") {
            adminRepo.setUserBlocked(userId, blocked)
        }

    fun adjustUserBalance(user: UserProfile, delta: Long, reason: String) =
        launchAction("Wallet balance updated.") {
            adminRepo.adjustUserBalance(user, delta, reason)
        }

    fun addUserByAdmin(username: String, email: String, startingBalance: Long) =
        launchAction("Player added to roster.") {
            adminRepo.addUserByAdmin(username, email, startingBalance)
        }

    fun deleteUserRecord(userId: String) =
        launchAction("User record removed.") { adminRepo.deleteUserRecord(userId) }

    fun setAdminRole(user: UserProfile, makeAdmin: Boolean) =
        launchAction(if (makeAdmin) "${user.username} promoted to Admin." else "Admin access revoked.") {
            adminRepo.setAdminRole(user, makeAdmin)
        }

    fun savePaymentConfig(config: PaymentConfig) =
        launchAction("Payment accounts & instructions saved.") {
            adminRepo.savePaymentConfig(config)
        }
}
