package com.example.domain

import java.text.NumberFormat
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.util.Locale
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

object DomainLogic {

    val DHAKA_AREAS = listOf(
        "Mirpur",
        "Uttara",
        "Dhanmondi",
        "Mohammadpur",
        "Bashundhara",
        "Savar",
        "Gazipur"
    )

    val EXPENSE_CATEGORIES = listOf(
        "House rent",
        "Electricity",
        "Gas",
        "Water",
        "Internet",
        "Food",
        "Grocery",
        "Cleaning",
        "Maintenance",
        "Transportation",
        "Other"
    )

    val MEAL_DAYS = listOf(
        "Saturday",
        "Sunday",
        "Monday",
        "Tuesday",
        "Wednesday",
        "Thursday",
        "Friday"
    )

    val MEAL_SLOTS = listOf("Breakfast", "Lunch", "Dinner")

    val DHAKA_LANDMARKS = listOf(
        DhakaLandmark("Dhaka University (TSC)", "University", 23.7336, 90.3955),
        DhakaLandmark("BUET Campus", "University", 23.7266, 90.3927),
        DhakaLandmark("North South University (NSU)", "University", 23.8151, 90.4255),
        DhakaLandmark("Daffodil Smart City / Dhanmondi Hub", "University", 23.7549, 90.3764),
        DhakaLandmark("Mirpur 10 Metro Station", "Bus & Metro Stop", 23.8069, 90.3687),
        DhakaLandmark("Farmgate / Karwan Bazar Tech Hub", "Workplace", 23.7561, 90.3872),
        DhakaLandmark("Square Hospital Panthapath", "Hospital", 23.7529, 90.3816),
        DhakaLandmark("Labaid Specialized Hospital", "Hospital", 23.7418, 90.3831),
        DhakaLandmark("Lazz Pharma Dhanmondi", "Pharmacy", 23.7458, 90.3738),
        DhakaLandmark("Shwapno Super Shop Mirpur", "Supermarket", 23.8045, 90.3698),
        DhakaLandmark("Dutch-Bangla ATM Booth", "ATM", 23.8075, 90.3679),
        DhakaLandmark("Uttara North Metro & Bus Hub", "Bus & Metro Stop", 23.8691, 90.3671)
    )

    fun formatBdt(amount: Number): String {
        val rounded = amount.toDouble().roundToInt()
        val nf = NumberFormat.getIntegerInstance(Locale.US)
        return "৳${nf.format(rounded)}"
    }

    fun calculateTotalMonthlyCost(
        monthlyRent: Int,
        serviceCharge: Int = 0,
        electricity: Int = 0,
        water: Int = 0,
        gas: Int = 0,
        internet: Int = 0,
        otherCharges: Int = 0
    ): TotalMonthlyCostBreakdown {
        val r = max(0, monthlyRent)
        val sc = max(0, serviceCharge)
        val el = max(0, electricity)
        val wa = max(0, water)
        val ga = max(0, gas)
        val net = max(0, internet)
        val oth = max(0, otherCharges)
        return TotalMonthlyCostBreakdown(
            rent = r,
            serviceCharge = sc,
            electricity = el,
            water = wa,
            gas = ga,
            internet = net,
            otherCharges = oth,
            totalMonthlyCost = r + sc + el + wa + ga + net + oth
        )
    }

    private fun sleepIndex(v: String): Int = when (v) {
        "early_bird" -> 0
        "night_owl" -> 2
        else -> 1
    }

    private fun noiseIndex(v: String): Int = when (v) {
        "quiet" -> 0
        "lively" -> 2
        else -> 1
    }

    private fun cookIndex(v: String): Int = when (v) {
        "daily_cook" -> 0
        "rarely_order_out" -> 2
        else -> 1
    }

    private fun guestIndex(v: String): Int = when (v) {
        "no_guests" -> 0
        "flexible" -> 2
        else -> 1
    }

    fun calculateRoommateCompatibility(
        a: RoommatePreferenceProfile,
        b: RoommatePreferenceProfile
    ): CompatibilityResult {
        val matches = mutableListOf<String>()
        val differences = mutableListOf<String>()

        // 1. Budget (20%)
        val maxBudget = max(max(a.monthlyBudget, b.monthlyBudget), 1)
        val budgetDiffRatio = abs(a.monthlyBudget - b.monthlyBudget).toDouble() / maxBudget.toDouble()
        val budgetScore = max(0, ((1.0 - min(budgetDiffRatio * 1.5, 1.0)) * 20.0).roundToInt())
        if (budgetScore >= 16) {
            matches += "Compatible monthly budgets"
        } else {
            differences += "Different monthly budget ranges"
        }

        // 2. Location (20%)
        val locA = a.preferredLocation.trim().lowercase()
        val locB = b.preferredLocation.trim().lowercase()
        val locationScore = when {
            locA.isNotEmpty() && locA == locB -> {
                matches += "Both prefer ${a.preferredLocation}"
                20
            }
            locA.isNotEmpty() && locB.isNotEmpty() && (locA.contains(locB) || locB.contains(locA)) -> {
                matches += "Overlapping preferred neighborhoods"
                15
            }
            else -> {
                differences += "Different target areas (${a.preferredLocation.ifBlank { "Any" }} vs ${b.preferredLocation.ifBlank { "Any" }})"
                6
            }
        }

        // 3. Smoking (15%)
        val smokingScore = when {
            a.smokingPreference == b.smokingPreference -> {
                matches += if (a.smokingPreference == "non_smoker") {
                    "Both are non-smokers"
                } else {
                    "Matching smoking preferences"
                }
                15
            }
            (a.smokingPreference == "non_smoker" && b.smokingPreference == "outside_only") ||
                (b.smokingPreference == "non_smoker" && a.smokingPreference == "outside_only") -> {
                differences += "Different smoking boundaries (non-smoker vs outside only)"
                7
            }
            else -> {
                differences += "Incompatible smoking preferences"
                0
            }
        }

        // 4. Cleanliness (15%)
        val cleanDiff = abs(a.cleanlinessLevel - b.cleanlinessLevel)
        val cleanlinessScore = max(0, (((4 - cleanDiff) / 4.0) * 15.0).roundToInt())
        if (cleanDiff <= 1) {
            matches += "Aligned cleanliness standards"
        } else {
            differences += "Different cleanliness expectations"
        }

        // 5. Sleep (10%)
        val sleepDiff = abs(sleepIndex(a.sleepSchedule) - sleepIndex(b.sleepSchedule))
        val sleepScore = when (sleepDiff) {
            0 -> 10
            1 -> 6
            else -> 2
        }
        if (sleepDiff == 0) matches += "Similar sleeping schedules" else differences += "Different sleeping schedules"

        // 6. Noise (10%)
        val noiseDiff = abs(noiseIndex(a.noisePreference) - noiseIndex(b.noisePreference))
        val noiseScore = when (noiseDiff) {
            0 -> 10
            1 -> 5
            else -> 1
        }
        if (noiseDiff == 0) {
            matches += if (a.noisePreference == "quiet") "Both prefer a quiet environment" else "Matching noise comfort level"
        } else {
            differences += "Different noise preferences"
        }

        // 7. Cooking (5%)
        val cookDiff = abs(cookIndex(a.cookingHabits) - cookIndex(b.cookingHabits))
        val cookingScore = when (cookDiff) {
            0 -> 5
            1 -> 3
            else -> 1
        }
        if (cookDiff == 0) matches += "Similar cooking routines" else differences += "Different cooking habits"

        // 8. Guest (5%)
        val guestDiff = abs(guestIndex(a.guestPreference) - guestIndex(b.guestPreference))
        val guestScore = when (guestDiff) {
            0 -> 5
            1 -> 3
            else -> 1
        }
        if (guestDiff == 0) matches += "Compatible visitor & guest expectations" else differences += "Different guest preferences"

        val total = (budgetScore + locationScore + smokingScore + cleanlinessScore +
            sleepScore + noiseScore + cookingScore + guestScore).coerceIn(0, 100)

        return CompatibilityResult(
            score = total,
            matches = matches,
            differences = differences,
            breakdown = CompatibilityBreakdown(
                budget = budgetScore,
                location = locationScore,
                smoking = smokingScore,
                cleanliness = cleanlinessScore,
                sleepSchedule = sleepScore,
                noise = noiseScore,
                cooking = cookingScore,
                guest = guestScore
            )
        )
    }

    fun calculateTrustScore(input: TrustScoreInput): TrustSummary {
        var score = 20
        if (input.emailVerified) score += 15
        if (input.phoneVerified) score += 15
        if (input.identityVerified) score += 20

        score += min(15, max(0, input.completedRentals) * 5)
        score += if (input.reviewCount > 0) {
            ((input.averageRating.coerceIn(0.0, 5.0) / 5.0) * 15.0).roundToInt()
        } else {
            5
        }
        score -= max(0, input.confirmedComplaints) * 15
        score -= max(0, input.cancellations) * 5

        val clamped = score.coerceIn(0, 100)
        val isVerified = input.emailVerified && input.identityVerified
        val tier = when {
            clamped >= 75 -> "High Trust"
            clamped >= 45 -> "Standard"
            else -> "Needs Verification"
        }

        return TrustSummary(
            score = clamped,
            tier = tier,
            isVerified = isVerified,
            completedRentals = input.completedRentals,
            averageRating = input.averageRating,
            reviewCount = input.reviewCount,
            complaintCount = input.confirmedComplaints,
            emailVerified = input.emailVerified,
            phoneVerified = input.phoneVerified,
            identityVerified = input.identityVerified
        )
    }

    fun detectSuspiciousListing(input: ScamDetectionInput): ScamDetectionResult {
        val reasons = mutableListOf<String>()
        var riskScore = 0

        val hasMedian = input.areaMedianRent > 0
        val significantlyBelowMedian = hasMedian && input.monthlyRent < input.areaMedianRent * 0.55
        val excessiveAdvance = input.monthlyRent > 0 && input.advanceAmount > input.monthlyRent * 4

        if (significantlyBelowMedian) {
            riskScore += 30
            reasons += "Listed rent (${formatBdt(input.monthlyRent)}) is over 45% below area median (${formatBdt(input.areaMedianRent)})"
        }
        if (excessiveAdvance) {
            riskScore += 30
            reasons += "High advance deposit requested (${formatBdt(input.advanceAmount)}, >4x monthly rent)"
        }
        if (!input.landlordVerified) {
            riskScore += 20
            reasons += "Landlord identity or property documents are not yet verified"
        }
        if (input.duplicateListingCount > 0) {
            riskScore += 25
            reasons += "Duplicate or repeated listing details detected across multiple postings"
        }
        if (input.complaintCount > 0) {
            riskScore += min(35, input.complaintCount * 15)
            reasons += "${input.complaintCount} user report(s) filed against this listing or landlord"
        }
        if (input.accountAgeDays < 3 && !input.landlordVerified) {
            riskScore += 10
            reasons += "Newly created unverified landlord account"
        }

        val onlyLowPrice = significantlyBelowMedian &&
            !excessiveAdvance &&
            input.landlordVerified &&
            input.duplicateListingCount == 0 &&
            input.complaintCount == 0

        val cappedRisk = min(100, riskScore)
        val suspicious = !onlyLowPrice && cappedRisk >= 45

        return ScamDetectionResult(
            suspicious = suspicious,
            riskScore = cappedRisk,
            reasons = reasons,
            label = if (suspicious) "Potentially Suspicious Listing" else "Normal"
        )
    }

    fun calculateExpenseSplits(
        totalAmount: Double,
        splitType: SplitType,
        participants: List<SplitParticipantInput>
    ): List<ComputedSplit> {
        require(participants.isNotEmpty()) { "At least one participant is required to split an expense" }
        val cleanTotal = (totalAmount * 100.0).roundToInt() / 100.0
        require(cleanTotal > 0.0) { "Expense amount must be greater than zero" }

        return when (splitType) {
            SplitType.EQUAL -> {
                val count = participants.size
                val base = floor((cleanTotal * 100.0) / count) / 100.0
                var remainderCents = (cleanTotal * 100.0).roundToInt() - (base * 100.0).roundToInt() * count
                participants.map { p ->
                    val extra = if (remainderCents > 0) {
                        remainderCents -= 1
                        0.01
                    } else 0.0
                    ComputedSplit(
                        userId = p.userId,
                        shareAmount = ((base + extra) * 100.0).roundToInt() / 100.0
                    )
                }
            }
            SplitType.PERCENTAGE -> {
                val sumPct = participants.sumOf { it.value ?: 0.0 }
                require(abs(sumPct - 100.0) <= 0.5) {
                    "Percentages must sum to 100% (got ${sumPct.roundToInt()}%)"
                }
                val raw = participants.map { p ->
                    val amt = floor(((cleanTotal * (p.value ?: 0.0)) / 100.0) * 100.0) / 100.0
                    ComputedSplit(p.userId, amt)
                }.toMutableList()
                val allocated = raw.sumOf { it.shareAmount }
                val diff = ((cleanTotal - allocated) * 100.0).roundToInt() / 100.0
                if (raw.isNotEmpty() && abs(diff) > 0.0) {
                    raw[0] = raw[0].copy(shareAmount = ((raw[0].shareAmount + diff) * 100.0).roundToInt() / 100.0)
                }
                raw
            }
            SplitType.CUSTOM -> {
                val customSum = participants.sumOf { it.value ?: 0.0 }
                require(abs(customSum - cleanTotal) <= 1.0) {
                    "Custom split amounts (${formatBdt(customSum)}) must equal total (${formatBdt(cleanTotal)})"
                }
                participants.map { p ->
                    ComputedSplit(
                        userId = p.userId,
                        shareAmount = ((p.value ?: 0.0) * 100.0).roundToInt() / 100.0
                    )
                }
            }
        }
    }

    fun calculateSettlements(
        expenses: List<ExpenseLedgerRecord>,
        splits: List<SplitLedgerRecord>,
        recordedSettlements: List<SettlementLedgerRecord>
    ): SettlementLedgerSummary {
        val net = mutableMapOf<Long, Double>()
        for (exp in expenses) {
            net[exp.paidByUserId] = (net[exp.paidByUserId] ?: 0.0) + exp.amount
        }
        for (sp in splits) {
            net[sp.userId] = (net[sp.userId] ?: 0.0) - sp.shareAmount
        }
        for (st in recordedSettlements) {
            net[st.fromUserId] = (net[st.fromUserId] ?: 0.0) + st.amount
            net[st.toUserId] = (net[st.toUserId] ?: 0.0) - st.amount
        }

        data class Node(val userId: Long, var amount: Double)

        val debtors = mutableListOf<Node>()
        val creditors = mutableListOf<Node>()
        val roundedNet = mutableMapOf<Long, Double>()

        for ((uid, rawBal) in net) {
            val bal = (rawBal * 100.0).roundToInt() / 100.0
            roundedNet[uid] = bal
            if (bal < -0.5) debtors += Node(uid, abs(bal))
            else if (bal > 0.5) creditors += Node(uid, bal)
        }

        debtors.sortByDescending { it.amount }
        creditors.sortByDescending { it.amount }

        val transfers = mutableListOf<TransferEdge>()
        var i = 0
        var j = 0
        while (i < debtors.size && j < creditors.size) {
            val transferAmt = min(debtors[i].amount, creditors[j].amount)
            val rounded = (transferAmt * 100.0).roundToInt() / 100.0
            if (rounded >= 1.0) {
                transfers += TransferEdge(
                    fromUserId = debtors[i].userId,
                    toUserId = creditors[j].userId,
                    amount = rounded
                )
            }
            debtors[i].amount -= transferAmt
            creditors[j].amount -= transferAmt
            if (debtors[i].amount < 0.5) i++
            if (creditors[j].amount < 0.5) j++
        }

        return SettlementLedgerSummary(netBalances = roundedNet, transfers = transfers)
    }

    fun getUtilityBillStatus(
        dueDateIso: String,
        paidAt: String?,
        referenceDate: LocalDate = LocalDate.now()
    ): UtilityBillStatusResult {
        if (!paidAt.isNullOrBlank()) {
            return UtilityBillStatusResult("Paid", null, 0)
        }
        val due = runCatching { LocalDate.parse(dueDateIso.take(10)) }.getOrElse { referenceDate }
        val diffDays = ChronoUnit.DAYS.between(referenceDate, due).toInt()
        return when {
            diffDays < 0 -> UtilityBillStatusResult("Overdue", "overdue", diffDays)
            diffDays == 0 -> UtilityBillStatusResult("Due soon", "due_today", 0)
            diffDays == 1 -> UtilityBillStatusResult("Due soon", "1_day", 1)
            diffDays <= 3 -> UtilityBillStatusResult("Due soon", "3_days", diffDays)
            else -> UtilityBillStatusResult("Upcoming", null, diffDays)
        }
    }

    fun calculateHaversineDistanceKm(lat1: Double, lng1: Double, lat2: Double, lng2: Double): Double {
        val r = 6371.0
        val dLat = (lat2 - lat1) * PI / 180.0
        val dLng = (lng2 - lng1) * PI / 180.0
        val a = sin(dLat / 2) * sin(dLat / 2) +
            cos(lat1 * PI / 180.0) * cos(lat2 * PI / 180.0) *
            sin(dLng / 2) * sin(dLng / 2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return ((r * c) * 100.0).roundToInt() / 100.0
    }

    fun estimateDhakaTransitCost(distanceKm: Double): DhakaTransitEstimate {
        val d = max(0.2, distanceKm)
        val busFare = if (d <= 2.0) 10 else min(60, (10.0 + (d - 2.0) * 3.0).roundToInt())
        val rickshawFare = if (d <= 3.0) max(20, (d * 25.0).roundToInt()) else (d * 22.0).roundToInt()
        val cngFare = max(50, (40.0 + d * 25.0).roundToInt())
        val minutes = max(5, (d * 6.5).roundToInt())
        return DhakaTransitEstimate(
            busFareBdt = busFare,
            rickshawFareBdt = rickshawFare,
            cngFareBdt = cngFare,
            estimatedMinutes = minutes
        )
    }

    fun computeNearbyLandmarks(lat: Double, lng: Double): List<NearbyLandmarkResult> {
        return DHAKA_LANDMARKS.map { lm ->
            val dist = calculateHaversineDistanceKm(lat, lng, lm.lat, lm.lng)
            NearbyLandmarkResult(
                landmark = lm,
                distanceKm = dist,
                transit = estimateDhakaTransitCost(dist)
            )
        }.sortedBy { it.distanceKm }.take(7)
    }
}
