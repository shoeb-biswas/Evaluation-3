package com.example.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.RentShareContainer
import com.example.data.local.PropertyEntity
import com.example.data.local.UserEntity
import com.example.domain.UserRole
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

private const val TAG = "MainViewModel"

data class AssistantChatMessage(val fromUser: Boolean, val text: String)


class MainViewModel(private val container: RentShareContainer) : ViewModel() {

    private val repo = container.repository
    private val finRepo = container.financeRepository
    private val aiRepo = container.aiRepository

    private val _filter = MutableStateFlow(MarketplaceFilter())
    val filter: StateFlow<MarketplaceFilter> = _filter.asStateFlow()

    private val _aiRentalState = MutableStateFlow<AiUiState<LandlordRentalUiIntent>>(AiUiState.Idle)
    val aiRentalState: StateFlow<AiUiState<LandlordRentalUiIntent>> = _aiRentalState.asStateFlow()

    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage: StateFlow<String?> = _statusMessage.asStateFlow()

    private val _assistantMessages = MutableStateFlow(
        listOf(AssistantChatMessage(false,
            "Welcome! 🌿 I’m your RentShare BD assistant. Ask me about finding a flat, good roommate habits, or budgeting your home."))
    )
    val assistantMessages: StateFlow<List<AssistantChatMessage>> = _assistantMessages.asStateFlow()

    private val _assistantBusy = MutableStateFlow(false)
    val assistantBusy: StateFlow<Boolean> = _assistantBusy.asStateFlow()

    private val _isSeeded = MutableStateFlow(false)

    init {
        viewModelScope.launch {
            repo.ensureSeeded()
            _isSeeded.value = true
        }
    }

    private val usersPart = combine(
        repo.loggedInUserId,
        repo.observeUsers(),
        repo.observeTenantProfiles(),
        repo.observeLandlordProfiles(),
        repo.observePreferences()
    ) { uid, users, tps, lps, prefs ->
        UsersBundle(uid, users, tps, lps, prefs)
    }

    private val propPart = combine(
        repo.observeProperties(),
        repo.observeRooms(),
        repo.loggedInUserId.flatMapLatest { uid ->
            if (uid == null) flowOf(emptyList()) else repo.observeSaved(uid)
        },
        repo.observeRoommateRequests(),
        repo.observeRentalRequests()
    ) { props, rooms, saved, rmReqs, rentReqs ->
        PropBundle(props, rooms, saved.map { it.propertyId }.toSet(), rmReqs, rentReqs)
    }

    private val socialPart = combine(
        repo.observeAgreements(),
        repo.observeGroups(),
        repo.observeGroupMembers(),
        repo.observeMessages(),
        finRepo.observeNotifications()
    ) { ags, groups, members, msgs, notifs ->
        SocialBundle(ags, groups, members, msgs, notifs)
    }

    private val financePart = combine(
        finRepo.observeExpenses(),
        finRepo.observeExpenseSplits(),
        finRepo.observeSettlements(),
        finRepo.observeUtilityBills(),
        finRepo.observeBillPayments()
    ) { exps, splits, settles, bills, pays ->
        FinanceBundle(exps, splits, settles, bills, pays)
    }

    private val livingPart = combine(
        finRepo.observeMealPlans(),
        finRepo.observeMealItems(),
        finRepo.observeMealParticipants(),
        finRepo.observeGroceryLists(),
        finRepo.observeGroceryItems()
    ) { mps, mis, mparts, gls, gis ->
        LivingBundle(mps, mis, mparts, gls, gis)
    }

    private val modPart = combine(
        finRepo.observeMaintenanceRequests(),
        finRepo.observeReviews(),
        finRepo.observeComplaints(),
        finRepo.observeVerifications()
    ) { maints, revs, comps, verifs ->
        ModBundle(maints, revs, comps, verifs)
    }

    val snapshot: StateFlow<AppSnapshot> = combine(
        combine(_isSeeded, usersPart) { seeded, u -> seeded to u },
        propPart,
        socialPart,
        financePart,
        combine(livingPart, modPart) { l, m -> l to m }
    ) { (seeded, u), p, s, f, (l, m) ->
        AppSnapshot(
            isReady = seeded,
            currentUser = u.users.firstOrNull { it.id == u.uid },
            users = u.users,
            tenantProfiles = u.tps,
            landlordProfiles = u.lps,
            preferences = u.prefs,
            properties = p.props,
            rooms = p.rooms,
            savedPropertyIds = p.savedIds,
            roommateRequests = p.rmReqs,
            rentalRequests = p.rentReqs,
            agreements = s.ags,
            groups = s.groups,
            groupMembers = s.members,
            messages = s.msgs,
            expenses = f.exps,
            expenseSplits = f.splits,
            settlements = f.settles,
            utilityBills = f.bills,
            billPayments = f.pays,
            mealPlans = l.mps,
            mealItems = l.mis,
            mealParticipants = l.mparts,
            groceryLists = l.gls,
            groceryItems = l.gis,
            maintenanceRequests = m.maints,
            reviews = m.revs,
            complaints = m.comps,
            verifications = m.verifs,
            notifications = s.notifs
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AppSnapshot())

    fun clearMessage() {
        _statusMessage.value = null
    }

    fun showMessage(msg: String) {
        _statusMessage.value = msg
    }

    fun updateFilter(transform: (MarketplaceFilter) -> MarketplaceFilter) {
        _filter.value = transform(_filter.value)
    }

    fun resetFilters() {
        _filter.value = MarketplaceFilter()
        _aiRentalState.value = AiUiState.Idle
    }

    fun filteredProperties(all: List<PropertyEntity>, usersById: Map<Long, UserEntity>): List<PropertyEntity> {
        val f = _filter.value
        val q = f.searchQuery.trim().lowercase()
        return all.filter { p ->
            val publicVisible = p.verificationStatus == "approved" && p.availabilityStatus != "suspended"
            if (!publicVisible) return@filter false
            val matchesQ = q.isEmpty() ||
                p.title.lowercase().contains(q) ||
                p.area.lowercase().contains(q) ||
                p.address.lowercase().contains(q) ||
                p.description.lowercase().contains(q)
            val matchesArea = f.area == "All" || p.area.equals(f.area, ignoreCase = true)
            val matchesRoom = f.roomType == "All" || p.roomType.equals(f.roomType, ignoreCase = true)
            val matchesRent = f.maxRent == null || p.monthlyRent <= f.maxRent
            val matchesFurnished = !f.furnishedOnly || p.furnished
            val matchesWifi = !f.wifiRequired || p.hasWifi
            val landlordVerified = usersById[p.landlordId]?.identityVerified == true
            val matchesVerified = !f.verifiedOnly || landlordVerified
            matchesQ && matchesArea && matchesRoom && matchesRent && matchesFurnished && matchesWifi && matchesVerified
        }
    }

    fun runAiRentalAssistant(naturalQuery: String) {
        if (naturalQuery.isBlank()) return
        viewModelScope.launch {
            _aiRentalState.value = AiUiState.Loading
            try {
                val parsed = aiRepo.parseRentalQuery(naturalQuery)
                _filter.value = _filter.value.copy(
                    area = parsed.area ?: "All",
                    maxRent = parsed.maxBudget,
                    roomType = parsed.roomType ?: "All",
                    searchQuery = ""
                )
                val snap = snapshot.value
                val matched = filteredProperties(snap.properties, snap.usersById).size
                _aiRentalState.value = AiUiState.Success(LandlordRentalUiIntent(parsed, matched))
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "AI Rental Assistant failed", e)
                _aiRentalState.value = AiUiState.Error(e.localizedMessage ?: "AI Rental Assistant failed.")
            }
        }
    }

    fun askAssistant(rawQuestion: String) {
        val question = rawQuestion.trim().take(1000)
        if (question.isEmpty() || _assistantBusy.value) return
        val history = _assistantMessages.value.takeLast(8).map {
            (if (it.fromUser) "User" else "Assistant") to it.text
        }
        _assistantMessages.value = _assistantMessages.value +
            AssistantChatMessage(true, question) + AssistantChatMessage(false, "")
        _assistantBusy.value = true
        viewModelScope.launch {
            try {
                aiRepo.streamConciergeReply(question, history).collect { reply ->
                    _assistantMessages.value = _assistantMessages.value.dropLast(1) +
                        AssistantChatMessage(false, reply)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _assistantMessages.value = _assistantMessages.value.dropLast(1) +
                    AssistantChatMessage(false, "AI is unavailable right now. Check your internet and Firebase App Check setup, then try again.")
            } finally {
                _assistantBusy.value = false
            }
        }
    }

    fun login(email: String, password: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            repo.login(email, password)
                .onSuccess {
                    _statusMessage.value = "Signed in as ${it.fullName}"
                    onSuccess()
                }
                .onFailure { _statusMessage.value = it.message ?: "Sign in failed" }
        }
    }

    fun loginDemo(role: UserRole, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            repo.loginDemoRole(role)
                .onSuccess {
                    _statusMessage.value = "Switched to ${it.fullName}"
                    onSuccess()
                }
                .onFailure { _statusMessage.value = it.message ?: "Demo switch failed" }
        }
    }

    fun register(fullName: String, email: String, phone: String, password: String, role: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            repo.register(fullName, email, phone, password, role)
                .onSuccess {
                    _statusMessage.value = "Account created for ${it.fullName}"
                    onSuccess()
                }
                .onFailure { _statusMessage.value = it.message ?: "Registration failed" }
        }
    }

    fun resetPasswordDemo(email: String, newPassword: String, onDone: () -> Unit) {
        viewModelScope.launch {
            repo.resetPasswordDemo(email, newPassword)
                .onSuccess {
                    _statusMessage.value = "Password updated. You can now sign in."
                    onDone()
                }
                .onFailure { _statusMessage.value = it.message ?: "Reset failed" }
        }
    }

    fun logout() {
        viewModelScope.launch {
            repo.logout()
            _statusMessage.value = "Signed out."
        }
    }

    fun toggleSaved(propertyId: Long) {
        val uid = snapshot.value.currentUser?.id ?: run {
            _statusMessage.value = "Sign in to save flats."
            return
        }
        viewModelScope.launch {
            repo.toggleSaveProperty(uid, propertyId)
        }
    }

    private data class UsersBundle(
        val uid: Long?,
        val users: List<com.example.data.local.UserEntity>,
        val tps: List<com.example.data.local.TenantProfileEntity>,
        val lps: List<com.example.data.local.LandlordProfileEntity>,
        val prefs: List<com.example.data.local.RoommatePreferenceEntity>
    )

    private data class PropBundle(
        val props: List<PropertyEntity>,
        val rooms: List<com.example.data.local.RoomEntity>,
        val savedIds: Set<Long>,
        val rmReqs: List<com.example.data.local.RoommateRequestEntity>,
        val rentReqs: List<com.example.data.local.RentalRequestEntity>
    )

    private data class SocialBundle(
        val ags: List<com.example.data.local.RentalAgreementEntity>,
        val groups: List<com.example.data.local.RoommateGroupEntity>,
        val members: List<com.example.data.local.GroupMemberEntity>,
        val msgs: List<com.example.data.local.MessageEntity>,
        val notifs: List<com.example.data.local.NotificationEntity>
    )

    private data class FinanceBundle(
        val exps: List<com.example.data.local.ExpenseEntity>,
        val splits: List<com.example.data.local.ExpenseSplitEntity>,
        val settles: List<com.example.data.local.SettlementEntity>,
        val bills: List<com.example.data.local.UtilityBillEntity>,
        val pays: List<com.example.data.local.BillPaymentEntity>
    )

    private data class LivingBundle(
        val mps: List<com.example.data.local.MealPlanEntity>,
        val mis: List<com.example.data.local.MealItemEntity>,
        val mparts: List<com.example.data.local.MealParticipantEntity>,
        val gls: List<com.example.data.local.GroceryListEntity>,
        val gis: List<com.example.data.local.GroceryItemEntity>
    )

    private data class ModBundle(
        val maints: List<com.example.data.local.MaintenanceRequestEntity>,
        val revs: List<com.example.data.local.ReviewEntity>,
        val comps: List<com.example.data.local.ComplaintEntity>,
        val verifs: List<com.example.data.local.VerificationRecordEntity>
    )
}
