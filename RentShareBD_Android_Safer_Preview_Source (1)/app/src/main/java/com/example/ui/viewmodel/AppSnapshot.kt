package com.example.ui.viewmodel

import com.example.data.local.*
import com.example.domain.CompatibilityResult
import com.example.domain.DomainLogic
import com.example.domain.FinancialSummary
import com.example.domain.ParsedRentalIntent
import com.example.domain.RoommatePreferenceProfile
import com.example.domain.TrustScoreInput
import com.example.domain.TrustSummary
import java.time.YearMonth

sealed interface AiUiState<out T> {
    data object Idle : AiUiState<Nothing>
    data object Loading : AiUiState<Nothing>
    data class Streaming(val text: String) : AiUiState<Nothing>
    data class Success<T>(val data: T) : AiUiState<T>
    data class Error(val message: String) : AiUiState<Nothing>
}

data class MarketplaceFilter(
    val searchQuery: String = "",
    val area: String = "All",
    val roomType: String = "All",
    val maxRent: Int? = null,
    val verifiedOnly: Boolean = false,
    val furnishedOnly: Boolean = false,
    val wifiRequired: Boolean = false
)

data class AppSnapshot(
    val isReady: Boolean = false,
    val currentUser: UserEntity? = null,
    val users: List<UserEntity> = emptyList(),
    val tenantProfiles: List<TenantProfileEntity> = emptyList(),
    val landlordProfiles: List<LandlordProfileEntity> = emptyList(),
    val preferences: List<RoommatePreferenceEntity> = emptyList(),
    val properties: List<PropertyEntity> = emptyList(),
    val rooms: List<RoomEntity> = emptyList(),
    val savedPropertyIds: Set<Long> = emptySet(),
    val roommateRequests: List<RoommateRequestEntity> = emptyList(),
    val rentalRequests: List<RentalRequestEntity> = emptyList(),
    val agreements: List<RentalAgreementEntity> = emptyList(),
    val groups: List<RoommateGroupEntity> = emptyList(),
    val groupMembers: List<GroupMemberEntity> = emptyList(),
    val messages: List<MessageEntity> = emptyList(),
    val expenses: List<ExpenseEntity> = emptyList(),
    val expenseSplits: List<ExpenseSplitEntity> = emptyList(),
    val settlements: List<SettlementEntity> = emptyList(),
    val utilityBills: List<UtilityBillEntity> = emptyList(),
    val billPayments: List<BillPaymentEntity> = emptyList(),
    val mealPlans: List<MealPlanEntity> = emptyList(),
    val mealItems: List<MealItemEntity> = emptyList(),
    val mealParticipants: List<MealParticipantEntity> = emptyList(),
    val groceryLists: List<GroceryListEntity> = emptyList(),
    val groceryItems: List<GroceryItemEntity> = emptyList(),
    val maintenanceRequests: List<MaintenanceRequestEntity> = emptyList(),
    val reviews: List<ReviewEntity> = emptyList(),
    val complaints: List<ComplaintEntity> = emptyList(),
    val verifications: List<VerificationRecordEntity> = emptyList(),
    val notifications: List<NotificationEntity> = emptyList()
) {
    val usersById: Map<Long, UserEntity> get() = users.associateBy { it.id }

    fun trustForUser(userId: Long): TrustSummary {
        val u = usersById[userId] ?: return DomainLogic.calculateTrustScore(
            TrustScoreInput(false, false, false, 0, 0.0, 0, 0, 0)
        )
        val tp = tenantProfiles.firstOrNull { it.userId == userId }
        val lp = landlordProfiles.firstOrNull { it.userId == userId }
        val userReviews = reviews.filter { it.landlordId == userId }
        val avgRating = if (userReviews.isNotEmpty()) userReviews.map { it.rating }.average() else 0.0
        val confirmedComplaints = complaints.count { it.targetUserId == userId && it.status == "resolved" }
        val completed = (tp?.completedRentals ?: 0) + (lp?.completedRentals ?: 0)
        val cancels = (tp?.cancellations ?: 0) + (lp?.cancellations ?: 0)
        return DomainLogic.calculateTrustScore(
            TrustScoreInput(
                emailVerified = u.emailVerified,
                phoneVerified = u.phoneVerified,
                identityVerified = u.identityVerified,
                completedRentals = completed,
                averageRating = avgRating,
                reviewCount = userReviews.size,
                confirmedComplaints = confirmedComplaints,
                cancellations = cancels
            )
        )
    }

    fun prefProfileFor(userId: Long): RoommatePreferenceProfile {
        val pref = preferences.firstOrNull { it.userId == userId }
        val tp = tenantProfiles.firstOrNull { it.userId == userId }
        return if (pref != null) {
            RoommatePreferenceProfile(
                monthlyBudget = pref.monthlyBudget,
                preferredLocation = pref.preferredLocation,
                smokingPreference = pref.smokingPreference,
                cleanlinessLevel = pref.cleanlinessLevel,
                sleepSchedule = pref.sleepSchedule,
                noisePreference = pref.noisePreference,
                cookingHabits = pref.cookingHabits,
                guestPreference = pref.guestPreference
            )
        } else {
            RoommatePreferenceProfile(
                monthlyBudget = tp?.monthlyBudget ?: 9000,
                preferredLocation = tp?.preferredLocation ?: "Mirpur"
            )
        }
    }

    fun compatibilityBetween(userA: Long, userB: Long): CompatibilityResult {
        return DomainLogic.calculateRoommateCompatibility(prefProfileFor(userA), prefProfileFor(userB))
    }

    fun activeGroupFor(userId: Long): RoommateGroupEntity? {
        val memberGroupIds = groupMembers.filter { it.userId == userId }.map { it.groupId }.toSet()
        return groups.firstOrNull { it.id in memberGroupIds }
    }

    fun membersOfGroup(groupId: Long): List<UserEntity> {
        val ids = groupMembers.filter { it.groupId == groupId }.map { it.userId }.toSet()
        return users.filter { it.id in ids }
    }

    fun financialSummaryForGroup(groupId: Long): FinancialSummary {
        val groupExp = expenses.filter { it.groupId == groupId }
        val ymStrings = groupExp.mapNotNull { it.expenseDate.take(7).takeIf { s -> s.length == 7 } }.distinct().sortedDescending()
        val currentYm = ymStrings.firstOrNull() ?: YearMonth.now().toString()
        val prevYm = ymStrings.getOrNull(1)
        val curTotal = groupExp.filter { it.expenseDate.startsWith(currentYm) }.sumOf { it.amount }.toInt()
        val prevTotal = if (prevYm != null) groupExp.filter { it.expenseDate.startsWith(prevYm) }.sumOf { it.amount }.toInt() else 0
        val predicted = when {
            curTotal > 0 && prevTotal > 0 -> ((curTotal + prevTotal) / 2.0).toInt()
            curTotal > 0 -> curTotal
            else -> null
        }
        val catTotals = groupExp.filter { it.expenseDate.startsWith(currentYm) }
            .groupBy { it.category }
            .mapValues { (_, list) -> list.sumOf { it.amount }.toInt() }
        return FinancialSummary(
            currentMonthTotal = curTotal,
            previousMonthTotal = prevTotal,
            predictedNextMonth = predicted,
            categoryTotals = catTotals
        )
    }

    fun unreadNotificationCount(): Int {
        val uid = currentUser?.id ?: return 0
        return notifications.count { it.userId == uid && !it.isRead }
    }
}

data class LandlordRentalUiIntent(
    val parsed: ParsedRentalIntent,
    val matchedCount: Int
)
