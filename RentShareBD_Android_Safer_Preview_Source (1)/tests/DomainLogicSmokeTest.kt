package com.example.domain

import java.time.LocalDate
import kotlin.math.abs

fun main() {
    var count = 0
    fun checkIt(condition: Boolean, description: String) {
        check(condition) { "FAILED: $description" }
        println("PASS $description")
        count++
    }

    val monthly = DomainLogic.calculateTotalMonthlyCost(
        monthlyRent = 12000, serviceCharge = 1000, electricity = 800,
        water = 200, gas = 600, internet = 500, otherCharges = 100
    )
    checkIt(monthly.totalMonthlyCost == 15200, "monthly living cost sums correctly")

    val equal = DomainLogic.calculateExpenseSplits(
        100.00, SplitType.EQUAL,
        listOf(SplitParticipantInput(1L), SplitParticipantInput(2L), SplitParticipantInput(3L))
    )
    checkIt(abs(equal.sumOf { it.shareAmount } - 100.0) < 0.001, "equal split preserves total across odd number")
    checkIt(equal.map { it.userId }.distinct().size == 3, "equal split allocates to all participants")

    val percent = DomainLogic.calculateExpenseSplits(
        999.99, SplitType.PERCENTAGE,
        listOf(SplitParticipantInput(1L, 70.0), SplitParticipantInput(2L, 30.0))
    )
    checkIt(abs(percent.sumOf { it.shareAmount } - 999.99) < 0.001, "percentage split preserves value")

    val settled = DomainLogic.calculateSettlements(
        expenses = listOf(ExpenseLedgerRecord(1L, 1L, 300.0)),
        splits = listOf(SplitLedgerRecord(1L, 1L, 100.0), SplitLedgerRecord(1L, 2L, 100.0), SplitLedgerRecord(1L, 3L, 100.0)),
        recordedSettlements = emptyList()
    )
    checkIt(settled.transfers.size == 2, "two roommates owe payer")
    checkIt(abs(settled.transfers.sumOf { it.amount } - 200.0) < 0.001, "settlement suggestions sum correctly")

    checkIt(DomainLogic.getUtilityBillStatus("2026-11-22", null, LocalDate.parse("2026-11-23")).status == "Overdue", "late utility status")
    checkIt(DomainLogic.getUtilityBillStatus("2026-11-23", null, LocalDate.parse("2026-11-23")).status == "Due soon", "same-day utility status")
    checkIt(DomainLogic.getUtilityBillStatus("2026-11-23", "2026-11-22", LocalDate.parse("2026-11-23")).status == "Paid", "manually-recorded paid status")

    val match = DomainLogic.calculateRoommateCompatibility(
        RoommatePreferenceProfile(), RoommatePreferenceProfile()
    )
    checkIt(match.score in 0..100, "compatibility score is bounded")

    println("$count domain checks passed")
}
