package com.example.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.RentShareContainer
import com.example.data.local.*
import com.example.domain.SplitParticipantInput
import com.example.domain.SplitType
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

private const val TAG = "TenantViewModel"

class TenantViewModel(private val container: RentShareContainer) : ViewModel() {

    private val repo = container.repository
    private val finRepo = container.financeRepository
    private val aiRepo = container.aiRepository

    private val _coachState = MutableStateFlow<AiUiState<String>>(AiUiState.Idle)
    val coachState: StateFlow<AiUiState<String>> = _coachState.asStateFlow()

    fun askFinancialCoach(snapshot: AppSnapshot, question: String) {
        val uid = snapshot.currentUser?.id ?: return
        val group = snapshot.activeGroupFor(uid)
        val groupName = group?.name ?: "Shared Living Workspace"
        val summary = if (group != null) {
            snapshot.financialSummaryForGroup(group.id)
        } else {
            snapshot.financialSummaryForGroup(1L)
        }
        val cleanQuestion = question.trim().ifBlank {
            "Analyze our monthly shared spending, utilities, and top areas where we can save money in Dhaka."
        }

        viewModelScope.launch {
            _coachState.value = AiUiState.Loading
            var latest = ""
            try {
                aiRepo.streamFinancialAdvice(
                    groupName = groupName,
                    currentMonthTotal = summary.currentMonthTotal,
                    previousMonthTotal = summary.previousMonthTotal,
                    predictedNextMonth = summary.predictedNextMonth,
                    categoryBreakdown = summary.categoryTotals,
                    question = cleanQuestion
                ).collect { text ->
                    latest = text
                    _coachState.value = AiUiState.Streaming(text)
                }
                _coachState.value = if (latest.isNotBlank()) {
                    AiUiState.Success(latest)
                } else {
                    AiUiState.Error("AI Financial Coach returned an empty response.")
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "AI Financial Coach failed", e)
                _coachState.value = AiUiState.Error(e.localizedMessage ?: "AI Financial Coach request failed.")
            }
        }
    }

    fun updateProfileAndPrefs(
        user: UserEntity,
        profile: TenantProfileEntity,
        prefs: RoommatePreferenceEntity,
        onFeedback: (String) -> Unit
    ) {
        viewModelScope.launch {
            repo.updateTenantProfileAndPrefs(user, profile, prefs)
            onFeedback("Profile & roommate preferences saved.")
        }
    }

    fun verifyEmail(userId: Long, onFeedback: (String) -> Unit) {
        viewModelScope.launch {
            repo.verifyEmailForUser(userId)
            onFeedback("Email verified! Trust score updated.")
        }
    }

    fun submitVerification(userId: Long, type: String, docRef: String, onFeedback: (String) -> Unit) {
        viewModelScope.launch {
            repo.submitVerification(userId, type, docRef)
            onFeedback("Verification submitted for review.")
        }
    }

    fun sendRoommateRequest(senderId: Long, receiverId: Long, message: String, onFeedback: (String) -> Unit) {
        viewModelScope.launch {
            repo.sendRoommateRequest(senderId, receiverId, message)
            onFeedback("Roommate connection request sent.")
        }
    }

    fun respondRoommateRequest(req: RoommateRequestEntity, status: String, onFeedback: (String) -> Unit) {
        viewModelScope.launch {
            repo.respondRoommateRequest(req, status)
            onFeedback("Request $status.")
        }
    }

    fun sendRentalRequest(
        property: PropertyEntity,
        tenantId: Long,
        moveInDate: String,
        durationMonths: Int,
        message: String,
        onFeedback: (String) -> Unit
    ) {
        viewModelScope.launch {
            repo.sendRentalRequest(property, tenantId, moveInDate, durationMonths, message)
            onFeedback("Rental application sent to landlord.")
        }
    }

    fun acceptAgreement(agreement: RentalAgreementEntity, onFeedback: (String) -> Unit) {
        viewModelScope.launch {
            repo.acceptRentalAgreement(agreement)
            onFeedback("Digital rental agreement accepted and activated.")
        }
    }

    fun createGroup(name: String, creatorId: Long, memberIds: List<Long>, onFeedback: (String) -> Unit) {
        viewModelScope.launch {
            repo.createGroup(name, creatorId, memberIds)
            onFeedback("Roommate group \"$name\" created.")
        }
    }

    fun addMember(groupId: Long, userId: Long) {
        viewModelScope.launch { repo.addMemberToGroup(groupId, userId) }
    }

    fun removeMember(groupId: Long, userId: Long) {
        viewModelScope.launch { repo.removeMemberFromGroup(groupId, userId) }
    }

    fun sendGroupMessage(groupId: Long, senderId: Long, content: String) {
        viewModelScope.launch { repo.sendGroupMessage(groupId, senderId, content) }
    }

    fun sendPrivateMessage(senderId: Long, receiverId: Long, content: String) {
        viewModelScope.launch { repo.sendPrivateMessage(senderId, receiverId, content) }
    }

    fun addExpense(
        groupId: Long,
        paidByUserId: Long,
        category: String,
        title: String,
        amount: Double,
        splitType: SplitType,
        participants: List<SplitParticipantInput>,
        expenseDate: String,
        notes: String,
        onFeedback: (String) -> Unit
    ) {
        viewModelScope.launch {
            finRepo.addExpenseWithSplits(
                groupId, paidByUserId, category, title, amount, splitType, participants, expenseDate, notes
            ).onSuccess {
                onFeedback("Shared expense saved & split.")
            }.onFailure {
                onFeedback(it.message ?: "Invalid split input.")
            }
        }
    }

    fun deleteExpense(expenseId: Long) {
        viewModelScope.launch { finRepo.deleteExpense(expenseId) }
    }

    fun recordSettlement(
        groupId: Long,
        fromId: Long,
        toId: Long,
        amount: Double,
        method: String,
        ref: String,
        onFeedback: (String) -> Unit
    ) {
        viewModelScope.launch {
            finRepo.recordSettlement(groupId, fromId, toId, amount, method, ref)
            onFeedback("Settlement recorded.")
        }
    }

    fun addUtilityBill(groupId: Long, category: String, name: String, amount: Double, dueDate: String, autoPay: String) {
        viewModelScope.launch { finRepo.addUtilityBill(groupId, category, name, amount, dueDate, autoPay) }
    }

    fun markBillPaid(bill: UtilityBillEntity, paidBy: Long, provider: String, ref: String, onFeedback: (String) -> Unit) {
        viewModelScope.launch {
            finRepo.markBillPaidManually(bill, paidBy, provider, ref)
            onFeedback("${bill.billName} marked paid.")
        }
    }

    fun toggleAutoPay(bill: UtilityBillEntity, provider: String) {
        viewModelScope.launch { finRepo.toggleBillAutoPay(bill, provider) }
    }

    fun deleteUtilityBill(id: Long) {
        viewModelScope.launch { finRepo.deleteUtilityBill(id) }
    }

    fun addMeal(groupId: Long, planId: Long?, createdBy: Long, day: String, slot: String, name: String, desc: String) {
        viewModelScope.launch { finRepo.addMealItem(groupId, planId, createdBy, day, slot, name, desc) }
    }

    fun voteMeal(item: MealItemEntity, userId: Long) {
        viewModelScope.launch { finRepo.voteMealItem(item, userId) }
    }

    fun deleteMeal(id: Long) {
        viewModelScope.launch { finRepo.deleteMealItem(id) }
    }

    fun addGrocery(groupId: Long, listId: Long?, name: String, qty: String, price: Double, assignedTo: Long?) {
        viewModelScope.launch { finRepo.addGroceryItem(groupId, listId, name, qty, price, assignedTo) }
    }

    fun toggleGrocery(item: GroceryItemEntity, userId: Long) {
        viewModelScope.launch { finRepo.toggleGroceryPurchased(item, userId) }
    }

    fun deleteGrocery(id: Long) {
        viewModelScope.launch { finRepo.deleteGroceryItem(id) }
    }

    fun syncGroceriesToExpense(
        groupId: Long,
        payerId: Long,
        purchased: List<GroceryItemEntity>,
        memberIds: List<Long>,
        onFeedback: (String) -> Unit
    ) {
        viewModelScope.launch {
            finRepo.syncPurchasedGroceriesToExpense(groupId, payerId, purchased, memberIds)
                .onSuccess { total -> onFeedback("Synced ৳${total.toInt()} grocery purchase to shared expenses.") }
                .onFailure { onFeedback(it.message ?: "Nothing to sync.") }
        }
    }

    fun submitMaintenance(
        propertyId: Long,
        tenantId: Long,
        landlordId: Long,
        category: String,
        title: String,
        desc: String,
        priority: String,
        onFeedback: (String) -> Unit
    ) {
        viewModelScope.launch {
            finRepo.submitMaintenance(propertyId, tenantId, landlordId, category, title, desc, priority)
            onFeedback("Maintenance ticket submitted.")
        }
    }

    fun submitReview(propertyId: Long, reviewerId: Long, landlordId: Long, rating: Int, comment: String, onFeedback: (String) -> Unit) {
        viewModelScope.launch {
            finRepo.submitReview(propertyId, reviewerId, landlordId, rating, comment)
            onFeedback("Thank you! Review published.")
        }
    }

    fun fileComplaint(reporterId: Long, targetUserId: Long?, propertyId: Long?, reason: String, details: String, onFeedback: (String) -> Unit) {
        viewModelScope.launch {
            repo.fileComplaint(reporterId, targetUserId, propertyId, reason, details)
            onFeedback("Report submitted to moderation team.")
        }
    }

    fun markNotificationsRead(userId: Long) {
        viewModelScope.launch { finRepo.markAllNotificationsRead(userId) }
    }

    fun changePassword(userId: Long, current: String, next: String, onFeedback: (String) -> Unit) {
        viewModelScope.launch {
            repo.changePassword(userId, current, next)
                .onSuccess { onFeedback("Password updated successfully.") }
                .onFailure { onFeedback(it.message ?: "Password change failed.") }
        }
    }
}
