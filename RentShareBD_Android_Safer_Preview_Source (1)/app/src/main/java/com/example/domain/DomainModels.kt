package com.example.domain

import kotlinx.serialization.Serializable

enum class UserRole(val key: String, val label: String) {
    TENANT("tenant", "Tenant"),
    LANDLORD("landlord", "Landlord"),
    ADMIN("admin", "Admin");

    companion object {
        fun fromKey(key: String): UserRole = entries.firstOrNull { it.key == key } ?: TENANT
    }
}

data class TotalMonthlyCostBreakdown(
    val rent: Int,
    val serviceCharge: Int,
    val electricity: Int,
    val water: Int,
    val gas: Int,
    val internet: Int,
    val otherCharges: Int,
    val totalMonthlyCost: Int
)

data class RoommatePreferenceProfile(
    val monthlyBudget: Int = 9000,
    val preferredLocation: String = "Mirpur",
    val smokingPreference: String = "non_smoker", // non_smoker, outside_only, smoker
    val cleanlinessLevel: Int = 4, // 1..5
    val sleepSchedule: String = "moderate", // early_bird, moderate, night_owl
    val noisePreference: String = "quiet", // quiet, moderate, lively
    val cookingHabits: String = "occasional", // daily_cook, occasional, rarely_order_out
    val guestPreference: String = "occasional_daytime" // no_guests, occasional_daytime, flexible
)

data class CompatibilityBreakdown(
    val budget: Int,
    val location: Int,
    val smoking: Int,
    val cleanliness: Int,
    val sleepSchedule: Int,
    val noise: Int,
    val cooking: Int,
    val guest: Int
)

data class CompatibilityResult(
    val score: Int,
    val matches: List<String>,
    val differences: List<String>,
    val breakdown: CompatibilityBreakdown
)

data class TrustScoreInput(
    val emailVerified: Boolean,
    val phoneVerified: Boolean,
    val identityVerified: Boolean,
    val completedRentals: Int,
    val averageRating: Double,
    val reviewCount: Int,
    val confirmedComplaints: Int,
    val cancellations: Int
)

data class TrustSummary(
    val score: Int,
    val tier: String,
    val isVerified: Boolean,
    val completedRentals: Int = 0,
    val averageRating: Double = 0.0,
    val reviewCount: Int = 0,
    val complaintCount: Int = 0,
    val emailVerified: Boolean = false,
    val phoneVerified: Boolean = false,
    val identityVerified: Boolean = false
)

data class ScamDetectionInput(
    val monthlyRent: Int,
    val advanceAmount: Int,
    val areaMedianRent: Int,
    val landlordVerified: Boolean,
    val duplicateListingCount: Int,
    val complaintCount: Int,
    val accountAgeDays: Int
)

data class ScamDetectionResult(
    val suspicious: Boolean,
    val riskScore: Int,
    val reasons: List<String>,
    val label: String
)

enum class SplitType(val key: String, val label: String) {
    EQUAL("equal", "Equal Split"),
    CUSTOM("custom", "Custom Amount (৳)"),
    PERCENTAGE("percentage", "Percentage (%)");

    companion object {
        fun fromKey(key: String): SplitType = entries.firstOrNull { it.key == key } ?: EQUAL
    }
}

data class SplitParticipantInput(
    val userId: Long,
    val value: Double? = null
)

data class ComputedSplit(
    val userId: Long,
    val shareAmount: Double
)

data class ExpenseLedgerRecord(
    val id: Long,
    val paidByUserId: Long,
    val amount: Double
)

data class SplitLedgerRecord(
    val expenseId: Long,
    val userId: Long,
    val shareAmount: Double
)

data class SettlementLedgerRecord(
    val fromUserId: Long,
    val toUserId: Long,
    val amount: Double
)

data class TransferEdge(
    val fromUserId: Long,
    val toUserId: Long,
    val amount: Double
)

data class SettlementLedgerSummary(
    val netBalances: Map<Long, Double>,
    val transfers: List<TransferEdge>
)

data class UtilityBillStatusResult(
    val status: String, // Paid, Overdue, Due soon, Upcoming
    val reminderStage: String?,
    val daysRemaining: Int
)

data class DhakaLandmark(
    val name: String,
    val type: String,
    val lat: Double,
    val lng: Double
)

data class DhakaTransitEstimate(
    val busFareBdt: Int,
    val rickshawFareBdt: Int,
    val cngFareBdt: Int,
    val estimatedMinutes: Int
)

data class NearbyLandmarkResult(
    val landmark: DhakaLandmark,
    val distanceKm: Double,
    val transit: DhakaTransitEstimate
)

data class FinancialSummary(
    val currentMonthTotal: Int,
    val previousMonthTotal: Int,
    val predictedNextMonth: Int?,
    val categoryTotals: Map<String, Int>
)

@Serializable
data class ParsedRentalIntent(
    val area: String? = null,
    val maxBudget: Int? = null,
    val roomType: String? = null,
    val nonSmoking: Boolean = false,
    val quietEnvironment: Boolean = false
)
