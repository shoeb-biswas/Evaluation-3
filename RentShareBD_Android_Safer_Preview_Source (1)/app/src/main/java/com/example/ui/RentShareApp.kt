package com.example.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.example.R
import com.example.ui.navigation.*
import com.example.ui.screens.admin.*
import com.example.ui.screens.landlord.*
import com.example.ui.screens.public.*
import com.example.ui.screens.tenant.*
import com.example.ui.viewmodel.*

private data class TopTab(
    val label: String,
    val icon: ImageVector,
    val route: Any,
    val tag: String
)

@Composable
fun RentShareApp(
    mainVm: MainViewModel,
    tenantVm: TenantViewModel,
    landlordAdminVm: LandlordAdminViewModel
) {
    val navController = rememberNavController()
    val snapshot by mainVm.snapshot.collectAsStateWithLifecycle()
    val filter by mainVm.filter.collectAsStateWithLifecycle()
    val aiRentalState by mainVm.aiRentalState.collectAsStateWithLifecycle()
    val aiCoachState by tenantVm.coachState.collectAsStateWithLifecycle()
    val statusMsg by mainVm.statusMessage.collectAsStateWithLifecycle()
    val assistantMessages by mainVm.assistantMessages.collectAsStateWithLifecycle()
    val assistantBusy by mainVm.assistantBusy.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(statusMsg) {
        statusMsg?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            mainVm.clearMessage()
        }
    }

    val topTabs = listOf(
        TopTab(stringResource(R.string.nav_home), Icons.Filled.Explore, HomeRoute, "nav_explore"),
        TopTab(stringResource(R.string.nav_properties), Icons.Filled.Apartment, MarketplaceRoute, "nav_flats"),
        TopTab(stringResource(R.string.nav_roommates), Icons.Filled.Diversity3, RoommatesRoute, "nav_roommates"),
        TopTab(stringResource(R.string.nav_workspace), Icons.Filled.Dashboard, WorkspaceHomeRoute, "nav_workspace"),
        TopTab(stringResource(R.string.nav_account), Icons.Filled.Person, TenantSettingsRoute, "nav_account")
    )

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDest = navBackStackEntry?.destination
    val isTopLevel = currentDest?.let { dest ->
        dest.hasRoute<HomeRoute>() ||
            dest.hasRoute<MarketplaceRoute>() ||
            dest.hasRoute<RoommatesRoute>() ||
            dest.hasRoute<WorkspaceHomeRoute>() ||
            dest.hasRoute<TenantSettingsRoute>()
    } ?: true

    fun navigateTopTab(route: Any) {
        navController.navigate(route) {
            popUpTo(HomeRoute) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useRail = maxWidth >= 600.dp

        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) },
            bottomBar = {
                if (isTopLevel && !useRail) {
                    NavigationBar {
                        topTabs.forEach { tab ->
                            val selected = when (tab.route) {
                                HomeRoute -> currentDest?.hasRoute<HomeRoute>() == true
                                MarketplaceRoute -> currentDest?.hasRoute<MarketplaceRoute>() == true
                                RoommatesRoute -> currentDest?.hasRoute<RoommatesRoute>() == true
                                WorkspaceHomeRoute -> currentDest?.hasRoute<WorkspaceHomeRoute>() == true
                                TenantSettingsRoute -> currentDest?.hasRoute<TenantSettingsRoute>() == true
                                else -> false
                            }
                            NavigationBarItem(
                                selected = selected,
                                onClick = { navigateTopTab(tab.route) },
                                icon = { Icon(tab.icon, contentDescription = tab.label) },
                                label = { Text(tab.label, maxLines = 1) },
                                modifier = Modifier.testTag(tab.tag)
                            )
                        }
                    }
                }
            }
        ) { scaffoldPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = scaffoldPadding.calculateBottomPadding())
            ) {
                if (isTopLevel && useRail) {
                    NavigationRail {
                        Spacer(Modifier.height(12.dp))
                        topTabs.forEach { tab ->
                            val selected = when (tab.route) {
                                HomeRoute -> currentDest?.hasRoute<HomeRoute>() == true
                                MarketplaceRoute -> currentDest?.hasRoute<MarketplaceRoute>() == true
                                RoommatesRoute -> currentDest?.hasRoute<RoommatesRoute>() == true
                                WorkspaceHomeRoute -> currentDest?.hasRoute<WorkspaceHomeRoute>() == true
                                TenantSettingsRoute -> currentDest?.hasRoute<TenantSettingsRoute>() == true
                                else -> false
                            }
                            NavigationRailItem(
                                selected = selected,
                                onClick = { navigateTopTab(tab.route) },
                                icon = { Icon(tab.icon, contentDescription = tab.label) },
                                label = { Text(tab.label) },
                                modifier = Modifier.testTag("${tab.tag}_rail")
                            )
                        }
                    }
                }

                NavHost(
                    navController = navController,
                    startDestination = HomeRoute,
                    modifier = Modifier.weight(1f).fillMaxHeight()
                ) {
                    composable<HomeRoute> {
                        ExploreHomeScreen(
                            snapshot = snapshot,
                            onOpenMarketplace = { area ->
                                if (area != null) mainVm.updateFilter { it.copy(area = area) }
                                navigateTopTab(MarketplaceRoute)
                            },
                            onOpenProperty = { id -> navController.navigate(PropertyDetailsRoute(id)) },
                            onToggleSave = { mainVm.toggleSaved(it) },
                            onOpenRoommates = { navigateTopTab(RoommatesRoute) },
                            onOpenAssistant = { navController.navigate(AssistantRoute) },
                            onDemoSwitch = { role ->
                                mainVm.loginDemo(role) { navigateTopTab(WorkspaceHomeRoute) }
                            },
                            onOpenWorkspace = { navigateTopTab(WorkspaceHomeRoute) },
                            onOpenAuth = { navController.navigate(AuthRoute("login")) },
                            onOpenAbout = { navController.navigate(AboutRoute) },
                            onOpenHowItWorks = { navController.navigate(HowItWorksRoute) }
                        )
                    }

                    composable<AboutRoute> {
                        AboutScreen(onBack = { navController.popBackStack() })
                    }

                    composable<HowItWorksRoute> {
                        HowItWorksScreen(onBack = { navController.popBackStack() })
                    }

                    composable<MarketplaceRoute> {
                        MarketplaceScreen(
                            snapshot = snapshot,
                            filter = filter,
                            aiRentalState = aiRentalState,
                            onUpdateFilter = mainVm::updateFilter,
                            onResetFilters = mainVm::resetFilters,
                            onRunAiSearch = mainVm::runAiRentalAssistant,
                            onOpenProperty = { id -> navController.navigate(PropertyDetailsRoute(id)) },
                            onToggleSave = mainVm::toggleSaved
                        )
                    }

                    composable<PropertyDetailsRoute> { backStackEntry ->
                        val route = backStackEntry.toRoute<PropertyDetailsRoute>()
                        val prop = snapshot.properties.firstOrNull { it.id == route.propertyId }
                        PropertyDetailsScreen(
                            propertyId = route.propertyId,
                            snapshot = snapshot,
                            onBack = { navController.popBackStack() },
                            onToggleSave = mainVm::toggleSaved,
                            onApplyRental = { moveIn, months, msg ->
                                val uid = snapshot.currentUser?.id ?: return@PropertyDetailsScreen
                                if (prop != null) {
                                    tenantVm.sendRentalRequest(prop, uid, moveIn, months, msg) { mainVm.showMessage(it) }
                                }
                            },
                            onSubmitReview = { rating, comment ->
                                val uid = snapshot.currentUser?.id ?: return@PropertyDetailsScreen
                                if (prop != null) {
                                    tenantVm.submitReview(prop.id, uid, prop.landlordId, rating, comment) { mainVm.showMessage(it) }
                                }
                            },
                            onReportListing = { reason, details ->
                                val uid = snapshot.currentUser?.id
                                if (uid == null) {
                                    navController.navigate(AuthRoute("login"))
                                } else if (prop != null) {
                                    tenantVm.fileComplaint(uid, prop.landlordId, prop.id, reason, details) { mainVm.showMessage(it) }
                                }
                            },
                            onMessageLandlord = { lid -> navController.navigate(TenantPrivateChatRoute(lid)) },
                            onRequireAuth = { navController.navigate(AuthRoute("login")) }
                        )
                    }

                    composable<RoommatesRoute> {
                        RoommateDiscoveryScreen(
                            snapshot = snapshot,
                            onSendRequest = { receiverId, msg ->
                                val uid = snapshot.currentUser?.id ?: return@RoommateDiscoveryScreen
                                tenantVm.sendRoommateRequest(uid, receiverId, msg) { mainVm.showMessage(it) }
                            },
                            onMessagePeer = { peerId -> navController.navigate(TenantPrivateChatRoute(peerId)) },
                            onRequireAuth = { navController.navigate(AuthRoute("login")) }
                        )
                    }

                    composable<AuthRoute> { entry ->
                        val route = entry.toRoute<AuthRoute>()
                        AuthScreen(
                            initialMode = route.initialMode,
                            onBack = { navController.popBackStack() },
                            onLogin = { e, p -> mainVm.login(e, p) { navController.popBackStack() } },
                            onDemoLogin = { r ->
                                mainVm.loginDemo(r) {
                                    navController.navigate(WorkspaceHomeRoute) {
                                        popUpTo(HomeRoute)
                                        launchSingleTop = true
                                    }
                                }
                            },
                            onRegister = { fn, e, ph, pw, role ->
                                mainVm.register(fn, e, ph, pw, role) {
                                    navController.navigate(WorkspaceHomeRoute) {
                                        popUpTo(HomeRoute)
                                        launchSingleTop = true
                                    }
                                }
                            },
                            onResetPassword = { e, pw -> mainVm.resetPasswordDemo(e, pw) {} }
                        )
                    }

                    composable<AssistantRoute> {
                        RentShareAssistantScreen(
                            messages = assistantMessages,
                            loading = assistantBusy,
                            onBack = { navController.popBackStack() },
                            onSend = mainVm::askAssistant
                        )
                    }
                    composable<WalletPreviewRoute> {
                        RentShareWalletPreviewScreen(
                            onBack = { navController.popBackStack() },
                            onOpenBills = { navController.navigate(TenantUtilitiesRoute) }
                        )
                    }

                    composable<WorkspaceHomeRoute> {
                        val user = snapshot.currentUser
                        if (user == null) {
                            AuthScreen(
                                initialMode = "login",
                                onBack = null,
                                onLogin = { e, p -> mainVm.login(e, p) {} },
                                onDemoLogin = { r -> mainVm.loginDemo(r) {} },
                                onRegister = { fn, e, ph, pw, role -> mainVm.register(fn, e, ph, pw, role) {} },
                                onResetPassword = { e, pw -> mainVm.resetPasswordDemo(e, pw) {} }
                            )
                        } else {
                            when (user.role) {
                                "landlord" -> LandlordDashboardScreen(
                                    snapshot = snapshot,
                                    onNavigate = { dest -> navController.navigate(dest) }
                                )
                                "admin" -> AdminDashboardScreen(
                                    snapshot = snapshot,
                                    onNavigate = { dest -> navController.navigate(dest) }
                                )
                                else -> TenantDashboardScreen(
                                    snapshot = snapshot,
                                    onNavigate = { dest -> navController.navigate(dest) }
                                )
                            }
                        }
                    }

                    // Tenant destinations
                    composable<TenantProfileRoute> {
                        TenantProfileScreen(
                            snapshot = snapshot,
                            onBack = { navController.popBackStack() },
                            onSaveProfile = { u, tp, pref -> tenantVm.updateProfileAndPrefs(u, tp, pref) { mainVm.showMessage(it) } },
                            onVerifyEmail = { uid -> tenantVm.verifyEmail(uid) { mainVm.showMessage(it) } },
                            onSubmitVerification = { uid, type, ref -> tenantVm.submitVerification(uid, type, ref) { mainVm.showMessage(it) } }
                        )
                    }
                    composable<TenantMatchesRoute> {
                        TenantMatchesScreen(
                            snapshot = snapshot,
                            onBack = { navController.popBackStack() },
                            onRespondRequest = { req, status -> tenantVm.respondRoommateRequest(req, status) { mainVm.showMessage(it) } },
                            onMessagePeer = { peerId -> navController.navigate(TenantPrivateChatRoute(peerId)) }
                        )
                    }
                    composable<TenantRentalRequestsRoute> {
                        TenantRentalRequestsScreen(
                            snapshot = snapshot,
                            onBack = { navController.popBackStack() },
                            onOpenProperty = { pid -> navController.navigate(PropertyDetailsRoute(pid)) }
                        )
                    }
                    composable<TenantSavedRoute> {
                        TenantSavedScreen(
                            snapshot = snapshot,
                            onBack = { navController.popBackStack() },
                            onOpenProperty = { pid -> navController.navigate(PropertyDetailsRoute(pid)) },
                            onToggleSave = mainVm::toggleSaved
                        )
                    }
                    composable<TenantAgreementsRoute> {
                        TenantAgreementsScreen(
                            snapshot = snapshot,
                            onBack = { navController.popBackStack() },
                            onAcceptAgreement = { ag -> tenantVm.acceptAgreement(ag) { mainVm.showMessage(it) } }
                        )
                    }
                    composable<TenantGroupRoute> {
                        val uid = snapshot.currentUser?.id ?: 0L
                        TenantGroupScreen(
                            snapshot = snapshot,
                            onBack = { navController.popBackStack() },
                            onCreateGroup = { name, ids -> tenantVm.createGroup(name, uid, ids) { mainVm.showMessage(it) } },
                            onAddMember = tenantVm::addMember,
                            onRemoveMember = tenantVm::removeMember,
                            onOpenGroupChat = { navController.navigate(TenantGroupChatRoute) }
                        )
                    }
                    composable<TenantGroupChatRoute> {
                        val uid = snapshot.currentUser?.id ?: 0L
                        TenantGroupChatScreen(
                            snapshot = snapshot,
                            onBack = { navController.popBackStack() },
                            onSendGroupMessage = { gid, txt -> tenantVm.sendGroupMessage(gid, uid, txt) }
                        )
                    }
                    composable<TenantPrivateChatRoute> { entry ->
                        val route = entry.toRoute<TenantPrivateChatRoute>()
                        val uid = snapshot.currentUser?.id ?: 0L
                        TenantPrivateChatScreen(
                            initialPeerId = route.peerId,
                            snapshot = snapshot,
                            onBack = { navController.popBackStack() },
                            onSendPrivateMessage = { pid, txt -> tenantVm.sendPrivateMessage(uid, pid, txt) }
                        )
                    }
                    composable<TenantExpensesRoute> {
                        TenantExpensesScreen(
                            snapshot = snapshot,
                            onBack = { navController.popBackStack() },
                            onDeleteExpense = tenantVm::deleteExpense,
                            onOpenSplits = { navController.navigate(TenantSplitsRoute) }
                        )
                    }
                    composable<TenantSplitsRoute> {
                        TenantSplitsScreen(
                            snapshot = snapshot,
                            onBack = { navController.popBackStack() },
                            onAddExpense = { gid, paidBy, cat, title, amt, st, parts, notes ->
                                tenantVm.addExpense(gid, paidBy, cat, title, amt, st, parts, "", notes) { mainVm.showMessage(it) }
                            },
                            onRecordSettlement = { gid, f, t, a, m, r ->
                                tenantVm.recordSettlement(gid, f, t, a, m, r) { mainVm.showMessage(it) }
                            }
                        )
                    }
                    composable<TenantUtilitiesRoute> {
                        val uid = snapshot.currentUser?.id ?: 0L
                        TenantUtilitiesScreen(
                            snapshot = snapshot,
                            onBack = { navController.popBackStack() },
                            onAddBill = tenantVm::addUtilityBill,
                            onMarkPaid = { bill, prov, ref -> tenantVm.markBillPaid(bill, uid, prov, ref) { mainVm.showMessage(it) } },
                            onToggleAutoPay = tenantVm::toggleAutoPay,
                            onDeleteBill = tenantVm::deleteUtilityBill
                        )
                    }
                    composable<TenantMealsRoute> {
                        val uid = snapshot.currentUser?.id ?: 0L
                        TenantMealsScreen(
                            snapshot = snapshot,
                            onBack = { navController.popBackStack() },
                            onAddMeal = { gid, pid, d, s, n, desc -> tenantVm.addMeal(gid, pid, uid, d, s, n, desc) },
                            onVoteMeal = { item -> tenantVm.voteMeal(item, uid) },
                            onDeleteMeal = tenantVm::deleteMeal
                        )
                    }
                    composable<TenantGroceriesRoute> {
                        val uid = snapshot.currentUser?.id ?: 0L
                        TenantGroceriesScreen(
                            snapshot = snapshot,
                            onBack = { navController.popBackStack() },
                            onAddItem = tenantVm::addGrocery,
                            onToggleItem = { item -> tenantVm.toggleGrocery(item, uid) },
                            onDeleteItem = tenantVm::deleteGrocery,
                            onSyncToExpense = { gid, items, memberIds ->
                                tenantVm.syncGroceriesToExpense(gid, uid, items, memberIds) { mainVm.showMessage(it) }
                            }
                        )
                    }
                    composable<TenantMaintenanceRoute> {
                        val uid = snapshot.currentUser?.id ?: 0L
                        TenantMaintenanceScreen(
                            snapshot = snapshot,
                            onBack = { navController.popBackStack() },
                            onSubmitTicket = { pid, lid, cat, title, desc, pri ->
                                tenantVm.submitMaintenance(pid, uid, lid, cat, title, desc, pri) { mainVm.showMessage(it) }
                            }
                        )
                    }
                    composable<TenantAiCoachRoute> {
                        TenantAiCoachScreen(
                            snapshot = snapshot,
                            coachState = aiCoachState,
                            onBack = { navController.popBackStack() },
                            onAskCoach = { q -> tenantVm.askFinancialCoach(snapshot, q) }
                        )
                    }
                    composable<TenantNotificationsRoute> {
                        TenantNotificationsScreen(
                            snapshot = snapshot,
                            onBack = { navController.popBackStack() },
                            onMarkAllRead = tenantVm::markNotificationsRead
                        )
                    }
                    composable<TenantSettingsRoute> {
                        val canGoBack = navController.previousBackStackEntry != null && !isTopLevel
                        TenantSettingsScreen(
                            snapshot = snapshot,
                            onBack = if (canGoBack) { { navController.popBackStack() } } else null,
                            onChangePassword = { uid, c, n -> tenantVm.changePassword(uid, c, n) { mainVm.showMessage(it) } },
                            onSwitchDemoRole = { role -> mainVm.loginDemo(role) { navigateTopTab(WorkspaceHomeRoute) } },
                            onLogout = mainVm::logout,
                            onOpenAuth = { navController.navigate(AuthRoute("login")) }
                        )
                    }

                    // Landlord destinations
                    composable<LandlordPropertiesRoute> {
                        LandlordPropertiesScreen(
                            snapshot = snapshot,
                            onBack = { navController.popBackStack() },
                            onAddProperty = { navController.navigate(LandlordPropertyEditorRoute(null)) },
                            onEditProperty = { id -> navController.navigate(LandlordPropertyEditorRoute(id)) },
                            onToggleRoom = landlordAdminVm::toggleRoom
                        )
                    }
                    composable<LandlordPropertyEditorRoute> { entry ->
                        val route = entry.toRoute<LandlordPropertyEditorRoute>()
                        val uid = snapshot.currentUser?.id ?: 0L
                        LandlordPropertyEditorScreen(
                            propertyId = route.propertyId,
                            snapshot = snapshot,
                            onBack = { navController.popBackStack() },
                            onSave = { exId, title, desc, area, addr, rt, rooms, rent, adv, svc, el, wa, ga, net, oth, av, furn, bath, bal, wifi, park, gen, img, fac ->
                                landlordAdminVm.saveProperty(
                                    exId, uid, title, desc, area, addr, rt, rooms, rent, adv, svc, el, wa, ga, net, oth, av, furn, bath, bal, wifi, park, gen, img, fac
                                ) {
                                    mainVm.showMessage("Property saved.")
                                    navController.popBackStack()
                                }
                            }
                        )
                    }
                    composable<LandlordApplicationsRoute> {
                        LandlordApplicationsScreen(
                            snapshot = snapshot,
                            onBack = { navController.popBackStack() },
                            onUpdateApplication = { req, status -> landlordAdminVm.updateRentalApplication(req, status) { mainVm.showMessage(it) } }
                        )
                    }
                    composable<LandlordTenantsRoute> {
                        LandlordTenantsScreen(
                            snapshot = snapshot,
                            onBack = { navController.popBackStack() },
                            onMessageTenant = { tid -> navController.navigate(TenantPrivateChatRoute(tid)) }
                        )
                    }
                    composable<LandlordAgreementsRoute> {
                        LandlordAgreementsScreen(
                            snapshot = snapshot,
                            onBack = { navController.popBackStack() }
                        )
                    }
                    composable<LandlordMaintenanceRoute> {
                        LandlordMaintenanceScreen(
                            snapshot = snapshot,
                            onBack = { navController.popBackStack() },
                            onUpdateTicket = { t, s, r -> landlordAdminVm.updateMaintenance(t, s, r) { mainVm.showMessage(it) } }
                        )
                    }
                    composable<LandlordReviewsRoute> {
                        LandlordReviewsScreen(
                            snapshot = snapshot,
                            onBack = { navController.popBackStack() }
                        )
                    }

                    // Admin destinations
                    composable<AdminUsersRoute> {
                        AdminUsersScreen(
                            snapshot = snapshot,
                            onBack = { navController.popBackStack() },
                            onToggleSuspend = { u -> landlordAdminVm.toggleUserSuspended(u) { mainVm.showMessage(it) } }
                        )
                    }
                    composable<AdminVerificationsRoute> {
                        val adminId = snapshot.currentUser?.id ?: 0L
                        AdminVerificationsScreen(
                            snapshot = snapshot,
                            onBack = { navController.popBackStack() },
                            onReview = { v, approve -> landlordAdminVm.reviewVerification(v, adminId, approve) { mainVm.showMessage(it) } }
                        )
                    }
                    composable<AdminSuspiciousRoute> {
                        AdminSuspiciousScreen(
                            snapshot = snapshot,
                            onBack = { navController.popBackStack() },
                            onReviewProperty = { p, s, clear -> landlordAdminVm.reviewProperty(p, s, clear) { mainVm.showMessage(it) } }
                        )
                    }
                    composable<AdminComplaintsRoute> {
                        AdminComplaintsScreen(
                            snapshot = snapshot,
                            onBack = { navController.popBackStack() },
                            onResolve = { c, s -> landlordAdminVm.resolveComplaint(c, s) { mainVm.showMessage(it) } }
                        )
                    }
                    composable<AdminReportsRoute> {
                        AdminReportsScreen(
                            snapshot = snapshot,
                            reportJson = landlordAdminVm.buildPlatformReportJson(snapshot),
                            onBack = { navController.popBackStack() }
                        )
                    }
                }
            }
        }
    }
}
