package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.data.auth.AuthHelper
import com.example.data.auth.AuthHelper.authStateFlow
import com.example.data.model.Tournament
import com.example.data.model.UserProfile
import com.example.data.repository.AdminRepository
import com.example.data.repository.TournamentRepository
import com.example.ui.components.NeonBackdrop
import com.example.ui.screens.AdminDashboardScreen
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.BlockedAccountScreen
import com.example.ui.screens.HomeDashboardScreen
import com.example.ui.screens.ProfileScreen
import com.example.ui.screens.ProfileSetupScreen
import com.example.ui.screens.TournamentDetailSheet
import com.example.ui.screens.TournamentsScreen
import com.example.ui.screens.WalletScreen
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.ObsidianCard
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.ProTournamentTheme
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.VoidBlack
import com.example.ui.viewmodel.MainViewModel
import com.example.ui.viewmodel.UiState
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore

const val ROUTE_DASHBOARD = "dashboard"
const val ROUTE_TOURNAMENTS = "tournaments"
const val ROUTE_WALLET = "wallet"
const val ROUTE_PROFILE = "profile"
const val ROUTE_ADMIN = "admin"

private data class NavTab(
    val route: String,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ProTournamentTheme {
                ProTournamentRoot()
            }
        }
    }
}

@Composable
fun ProTournamentRoot() {
    val auth = Firebase.auth
    val currentUser by auth.authStateFlow().collectAsStateWithLifecycle(initialValue = auth.currentUser)

    val user = currentUser
    if (user == null) {
        AuthScreen(onAuthSuccess = { })
    } else {
        key(user.uid) {
            AuthenticatedAppHost(user = user)
        }
    }
}

@Composable
private fun AuthenticatedAppHost(user: FirebaseUser) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }

    val viewModel: MainViewModel = viewModel(
        key = user.uid,
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY])
                val databaseId = app.getString(R.string.firestore_database_id)
                val db = FirebaseFirestore.getInstance(databaseId)
                MainViewModel(
                    tournamentRepo = TournamentRepository(db),
                    adminRepo = AdminRepository(db),
                    currentUserId = user.uid,
                    userEmail = user.email.orEmpty(),
                    displayName = user.displayName.orEmpty()
                )
            }
        }
    )

    val profileState by viewModel.userProfileState.collectAsStateWithLifecycle()
    val isAdmin by viewModel.isAdmin.collectAsStateWithLifecycle()
    val actionMessage by viewModel.actionMessage.collectAsStateWithLifecycle()
    val isActionLoading by viewModel.isActionLoading.collectAsStateWithLifecycle()

    val performSignOut: () -> Unit = {
        AuthHelper.signOut(credentialManager, scope)
    }

    when (val state = profileState) {
        is UiState.Loading -> {
            NeonBackdrop {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = NeonCyan)
                        Spacer(Modifier.height(12.dp))
                        Text("Loading Pro Tournament...", color = TextSecondary)
                    }
                }
            }
        }
        is UiState.Error -> {
            NeonBackdrop {
                Box(modifier = Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                    Text(state.message, color = TextPrimary)
                }
            }
        }
        is UiState.Success -> {
            val profile = state.data
            when {
                profile == null -> {
                    ProfileSetupScreen(
                        email = user.email.orEmpty(),
                        suggestedName = user.displayName.orEmpty(),
                        isLoading = isActionLoading,
                        errorMessage = actionMessage,
                        onCreateProfile = { username, photo ->
                            viewModel.createProfile(username, photo)
                        },
                        onSignOut = performSignOut
                    )
                }
                profile.blocked -> {
                    BlockedAccountScreen(
                        username = profile.username,
                        playerId = profile.playerId,
                        onSignOut = performSignOut
                    )
                }
                else -> {
                    MainArenaScaffold(
                        profile = profile,
                        isAdmin = isAdmin,
                        viewModel = viewModel,
                        onSignOut = performSignOut
                    )
                }
            }
        }
    }
}

@Composable
private fun MainArenaScaffold(
    profile: UserProfile,
    isAdmin: Boolean,
    viewModel: MainViewModel,
    onSignOut: () -> Unit
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: ROUTE_DASHBOARD
    val snackbarHostState = remember { SnackbarHostState() }

    val tournamentsState by viewModel.tournamentsState.collectAsStateWithLifecycle()
    val tournaments = (tournamentsState as? UiState.Success)?.data.orEmpty()
    val selectedRegistrations by viewModel.selectedRegistrations.collectAsStateWithLifecycle()
    val userTransactions by viewModel.userTransactions.collectAsStateWithLifecycle()
    val userDeposits by viewModel.userDeposits.collectAsStateWithLifecycle()
    val userWithdrawals by viewModel.userWithdrawals.collectAsStateWithLifecycle()
    val winners by viewModel.winners.collectAsStateWithLifecycle()
    val paymentConfig by viewModel.paymentConfig.collectAsStateWithLifecycle()
    val isActionLoading by viewModel.isActionLoading.collectAsStateWithLifecycle()
    val actionMessage by viewModel.actionMessage.collectAsStateWithLifecycle()

    val adminUsers by viewModel.adminUsers.collectAsStateWithLifecycle()
    val adminList by viewModel.adminList.collectAsStateWithLifecycle()
    val adminDeposits by viewModel.adminDeposits.collectAsStateWithLifecycle()
    val adminWithdrawals by viewModel.adminWithdrawals.collectAsStateWithLifecycle()
    val adminTransactions by viewModel.adminTransactions.collectAsStateWithLifecycle()

    var activeDetailTournament by remember { mutableStateOf<Tournament?>(null) }
    var walletInitialTab by remember { mutableIntStateOf(0) }

    LaunchedEffect(actionMessage) {
        val msg = actionMessage
        if (!msg.isNullOrBlank()) {
            snackbarHostState.showSnackbar(msg)
            viewModel.clearMessage()
        }
    }

    val liveSelectedTournament = remember(tournaments, activeDetailTournament) {
        activeDetailTournament?.let { sel -> tournaments.firstOrNull { it.id == sel.id } ?: sel }
    }

    val tabs = remember(isAdmin) {
        buildList {
            add(NavTab(ROUTE_DASHBOARD, "Arena", Icons.Filled.Home, Icons.Outlined.Home))
            add(NavTab(ROUTE_TOURNAMENTS, "Matches", Icons.Filled.EmojiEvents, Icons.Outlined.EmojiEvents))
            add(NavTab(ROUTE_WALLET, "Wallet", Icons.Filled.AccountBalanceWallet, Icons.Outlined.AccountBalanceWallet))
            add(NavTab(ROUTE_PROFILE, "Profile", Icons.Filled.Person, Icons.Outlined.Person))
            if (isAdmin) {
                add(NavTab(ROUTE_ADMIN, "Admin", Icons.Filled.Shield, Icons.Outlined.Shield))
            }
        }
    }

    fun navigateTo(route: String) {
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }

    NeonBackdrop {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val useRail = maxWidth >= 600.dp
            Scaffold(
                containerColor = Color.Transparent,
                contentWindowInsets = WindowInsets.statusBars,
                snackbarHost = { SnackbarHost(snackbarHostState) },
                bottomBar = {
                    if (!useRail) {
                        NavigationBar(
                            containerColor = ObsidianSurface,
                            contentColor = TextPrimary,
                            windowInsets = WindowInsets.navigationBars
                        ) {
                            tabs.forEach { tab ->
                                val selected = currentRoute == tab.route
                                NavigationBarItem(
                                    selected = selected,
                                    onClick = { navigateTo(tab.route) },
                                    icon = {
                                        Icon(
                                            imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                            contentDescription = tab.label
                                        )
                                    },
                                    label = { Text(tab.label, style = MaterialTheme.typography.labelSmall) },
                                    modifier = Modifier.testTag("nav_${tab.route}"),
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = NeonCyan,
                                        selectedTextColor = NeonCyan,
                                        unselectedIconColor = TextSecondary,
                                        unselectedTextColor = TextSecondary,
                                        indicatorColor = ObsidianCard
                                    )
                                )
                            }
                        }
                    }
                }
            ) { innerPadding ->
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    if (useRail) {
                        NavigationRail(
                            containerColor = ObsidianSurface,
                            contentColor = TextPrimary,
                            modifier = Modifier
                                .fillMaxHeight()
                                .windowInsetsPadding(WindowInsets.navigationBars)
                        ) {
                            Spacer(Modifier.height(12.dp))
                            tabs.forEach { tab ->
                                val selected = currentRoute == tab.route
                                NavigationRailItem(
                                    selected = selected,
                                    onClick = { navigateTo(tab.route) },
                                    icon = {
                                        Icon(
                                            imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                            contentDescription = tab.label
                                        )
                                    },
                                    label = { Text(tab.label) },
                                    colors = NavigationRailItemDefaults.colors(
                                        selectedIconColor = NeonCyan,
                                        selectedTextColor = NeonCyan,
                                        unselectedIconColor = TextSecondary,
                                        unselectedTextColor = TextSecondary,
                                        indicatorColor = ObsidianCard
                                    )
                                )
                            }
                        }
                    }

                    NavHost(
                        navController = navController,
                        startDestination = ROUTE_DASHBOARD,
                        modifier = Modifier.weight(1f)
                    ) {
                        composable(ROUTE_DASHBOARD) {
                            HomeDashboardScreen(
                                profile = profile,
                                isAdmin = isAdmin,
                                tournaments = tournaments,
                                transactions = userTransactions,
                                winners = winners,
                                onOpenTournament = { t ->
                                    viewModel.selectTournament(t.id)
                                    activeDetailTournament = t
                                },
                                onNavigateTournaments = { navigateTo(ROUTE_TOURNAMENTS) },
                                onNavigateDeposit = {
                                    walletInitialTab = 1
                                    navigateTo(ROUTE_WALLET)
                                },
                                onNavigateWithdraw = {
                                    walletInitialTab = 2
                                    navigateTo(ROUTE_WALLET)
                                },
                                onNavigateWallet = {
                                    walletInitialTab = 0
                                    navigateTo(ROUTE_WALLET)
                                },
                                onNavigateProfile = { navigateTo(ROUTE_PROFILE) },
                                onNavigateAdmin = { if (isAdmin) navigateTo(ROUTE_ADMIN) }
                            )
                        }

                        composable(ROUTE_TOURNAMENTS) {
                            TournamentsScreen(
                                tournaments = tournaments,
                                onSelectTournament = { t ->
                                    viewModel.selectTournament(t.id)
                                    activeDetailTournament = t
                                }
                            )
                        }

                        composable(ROUTE_WALLET) {
                            WalletScreen(
                                profile = profile,
                                paymentConfig = paymentConfig,
                                transactions = userTransactions,
                                deposits = userDeposits,
                                withdrawals = userWithdrawals,
                                initialTab = walletInitialTab,
                                isActionLoading = isActionLoading,
                                onSubmitDeposit = { amt, method, refId, sender, img ->
                                    viewModel.submitDeposit(profile, amt, method, refId, sender, img)
                                },
                                onSubmitWithdrawal = { amt, method, title, number, bank ->
                                    viewModel.submitWithdrawal(profile, amt, method, title, number, bank)
                                }
                            )
                        }

                        composable(ROUTE_PROFILE) {
                            ProfileScreen(
                                profile = profile,
                                isAdmin = isAdmin,
                                myWins = winners.filter { it.userId == profile.userId },
                                isActionLoading = isActionLoading,
                                onUpdateProfile = { name, photo -> viewModel.updateProfile(name, photo) },
                                onOpenAdmin = { if (isAdmin) navigateTo(ROUTE_ADMIN) },
                                onSignOut = onSignOut
                            )
                        }

                        composable(ROUTE_ADMIN) {
                            if (isAdmin) {
                                AdminDashboardScreen(
                                    users = adminUsers,
                                    admins = adminList,
                                    tournaments = tournaments,
                                    deposits = adminDeposits,
                                    withdrawals = adminWithdrawals,
                                    transactions = adminTransactions,
                                    winners = winners,
                                    paymentConfig = paymentConfig,
                                    selectedRegistrations = selectedRegistrations,
                                    onObserveTournamentPlayers = { id -> viewModel.selectTournament(id) },
                                    onCreateTournament = { title, game, banner, fee, prize, maxSlots, epoch, label, status, rules, room ->
                                        viewModel.createTournament(title, game, banner, fee, prize, maxSlots, epoch, label, status, rules, room)
                                    },
                                    onUpdateTournament = { id, title, game, banner, fee, prize, maxSlots, label, status, rules, room ->
                                        viewModel.updateTournament(id, title, game, banner, fee, prize, maxSlots, label, status, rules, room)
                                    },
                                    onDeleteTournament = { id -> viewModel.deleteTournament(id) },
                                    onAddPlayerToTournament = { t, u -> viewModel.addPlayerToTournament(t, u) },
                                    onRemovePlayerFromTournament = { tId, uid -> viewModel.removePlayerFromTournament(tId, uid) },
                                    onDeclareWinner = { t, reg, placement, prize -> viewModel.declareWinner(t, reg, placement, prize) },
                                    onApproveDeposit = { dep, note -> viewModel.approveDeposit(dep, note) },
                                    onRejectDeposit = { dep, note -> viewModel.rejectDeposit(dep, note) },
                                    onApproveWithdrawal = { wd, note -> viewModel.approveWithdrawal(wd, note) },
                                    onRejectWithdrawal = { wd, note -> viewModel.rejectWithdrawal(wd, note) },
                                    onToggleUserBlocked = { uid, blk -> viewModel.toggleUserBlocked(uid, blk) },
                                    onAdjustUserBalance = { u, delta, reason -> viewModel.adjustUserBalance(u, delta, reason) },
                                    onAddUser = { name, email, bal -> viewModel.addUserByAdmin(name, email, bal) },
                                    onDeleteUser = { uid -> viewModel.deleteUserRecord(uid) },
                                    onSetAdminRole = { u, make -> viewModel.setAdminRole(u, make) },
                                    onSavePaymentConfig = { cfg -> viewModel.savePaymentConfig(cfg) }
                                )
                            } else {
                                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    Text("Admin access required", color = TextSecondary)
                                }
                            }
                        }
                    }
                }
            }
        }

        if (liveSelectedTournament != null) {
            TournamentDetailSheet(
                tournament = liveSelectedTournament,
                registrations = selectedRegistrations,
                profile = profile,
                isAdmin = isAdmin,
                isActionLoading = isActionLoading,
                onDismiss = {
                    activeDetailTournament = null
                    viewModel.selectTournament(null)
                },
                onJoin = { viewModel.joinTournament(liveSelectedTournament.id, profile) },
                onGoToDeposit = {
                    walletInitialTab = 1
                    navigateTo(ROUTE_WALLET)
                }
            )
        }
    }
}
