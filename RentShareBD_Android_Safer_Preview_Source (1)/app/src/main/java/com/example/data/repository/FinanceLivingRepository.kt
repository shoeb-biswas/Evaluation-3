package com.example.data.repository

import com.example.data.local.*
import com.example.domain.*
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

class FinanceLivingRepository(db: AppDatabase) {
    private val finDao = db.financeAndLivingDao()

    fun observeExpenses(): Flow<List<ExpenseEntity>> = finDao.observeExpenses()
    fun observeExpenseSplits(): Flow<List<ExpenseSplitEntity>> = finDao.observeExpenseSplits()
    fun observeSettlements(): Flow<List<SettlementEntity>> = finDao.observeSettlements()
    fun observeUtilityBills(): Flow<List<UtilityBillEntity>> = finDao.observeUtilityBills()
    fun observeBillPayments(): Flow<List<BillPaymentEntity>> = finDao.observeBillPayments()
    fun observeMealPlans(): Flow<List<MealPlanEntity>> = finDao.observeMealPlans()
    fun observeMealItems(): Flow<List<MealItemEntity>> = finDao.observeMealItems()
    fun observeMealParticipants(): Flow<List<MealParticipantEntity>> = finDao.observeMealParticipants()
    fun observeGroceryLists(): Flow<List<GroceryListEntity>> = finDao.observeGroceryLists()
    fun observeGroceryItems(): Flow<List<GroceryItemEntity>> = finDao.observeGroceryItems()
    fun observeMaintenanceRequests(): Flow<List<MaintenanceRequestEntity>> = finDao.observeMaintenanceRequests()
    fun observeReviews(): Flow<List<ReviewEntity>> = finDao.observeReviews()
    fun observeComplaints(): Flow<List<ComplaintEntity>> = finDao.observeComplaints()
    fun observeVerifications(): Flow<List<VerificationRecordEntity>> = finDao.observeVerifications()
    fun observeNotifications(): Flow<List<NotificationEntity>> = finDao.observeNotifications()

    suspend fun addExpenseWithSplits(
        groupId: Long,
        paidByUserId: Long,
        category: String,
        title: String,
        amount: Double,
        splitType: SplitType,
        participants: List<SplitParticipantInput>,
        expenseDate: String,
        notes: String
    ): Result<Long> {
        return runCatching {
            val computed = DomainLogic.calculateExpenseSplits(amount, splitType, participants)
            val expId = finDao.insertExpense(
                ExpenseEntity(
                    groupId = groupId,
                    paidByUserId = paidByUserId,
                    category = category,
                    title = title.trim(),
                    amount = amount,
                    splitType = splitType.key,
                    expenseDate = expenseDate.ifBlank { LocalDate.now().toString() },
                    notes = notes.trim()
                )
            )
            computed.forEach { sp ->
                finDao.insertExpenseSplit(
                    ExpenseSplitEntity(
                        expenseId = expId,
                        userId = sp.userId,
                        shareAmount = sp.shareAmount
                    )
                )
            }
            expId
        }
    }

    suspend fun deleteExpense(expenseId: Long) {
        finDao.deleteSplitsForExpense(expenseId)
        finDao.deleteExpenseById(expenseId)
    }

    suspend fun recordSettlement(
        groupId: Long,
        fromUserId: Long,
        toUserId: Long,
        amount: Double,
        method: String,
        reference: String
    ) {
        if (amount <= 0.0) return
        finDao.insertSettlement(
            SettlementEntity(
                groupId = groupId,
                fromUserId = fromUserId,
                toUserId = toUserId,
                amount = amount,
                paymentMethod = method.ifBlank { "Cash / Manual" },
                referenceNote = reference.trim()
            )
        )
        finDao.insertNotification(
            NotificationEntity(
                userId = toUserId,
                type = "settlement",
                title = "Settlement Recorded",
                message = "Recorded manual settlement of ${DomainLogic.formatBdt(amount)}."
            )
        )
    }

    suspend fun addUtilityBill(
        groupId: Long,
        category: String,
        billName: String,
        amount: Double,
        dueDate: String,
        autoPayProvider: String
    ) {
        if (billName.isBlank() || amount <= 0.0) return
        finDao.insertUtilityBill(
            UtilityBillEntity(
                groupId = groupId,
                category = category,
                billName = billName.trim(),
                amount = amount,
                dueDate = dueDate.ifBlank { LocalDate.now().plusDays(5).toString() },
                autoPayEnabled = false, // No real provider agreement or payment authorization.
                autoPayProvider = ""
            )
        )
    }

    suspend fun markBillPaidManually(
        bill: UtilityBillEntity,
        paidByUserId: Long,
        provider: String,
        transactionRef: String
    ) {
        val now = LocalDate.now().toString()
        finDao.updateUtilityBill(bill.copy(paidAt = now))
        finDao.insertBillPayment(
            BillPaymentEntity(
                billId = bill.id,
                paidByUserId = paidByUserId,
                amount = bill.amount,
                provider = provider.ifBlank { "Cash / Manual" },
                transactionRef = transactionRef.ifBlank { "MANUAL-${bill.id}" }
            )
        )
    }

    suspend fun toggleBillAutoPay(bill: UtilityBillEntity, provider: String) {
        // Intentionally disabled. Auto-pay must require a provider-approved mandate,
        // server-side signed confirmation, idempotency, and a ledger.
        return
    }

    suspend fun deleteUtilityBill(billId: Long) {
        finDao.deleteUtilityBillById(billId)
    }

    suspend fun addMealItem(
        groupId: Long,
        mealPlanId: Long?,
        createdBy: Long,
        dayOfWeek: String,
        mealSlot: String,
        mealName: String,
        description: String
    ) {
        if (mealName.isBlank()) return
        val mpId = mealPlanId ?: finDao.insertMealPlan(
            MealPlanEntity(groupId = groupId, weekLabel = "Current Week Plan", createdBy = createdBy)
        )
        finDao.insertMealItem(
            MealItemEntity(
                mealPlanId = mpId,
                dayOfWeek = dayOfWeek,
                mealSlot = mealSlot,
                mealName = mealName.trim(),
                description = description.trim(),
                votes = 1
            )
        )
    }

    suspend fun voteMealItem(item: MealItemEntity, userId: Long) {
        val existing = finDao.getMealParticipant(item.id, userId)
        if (existing != null && existing.voted) return
        finDao.insertMealParticipant(MealParticipantEntity(mealItemId = item.id, userId = userId, voted = true))
        finDao.updateMealItem(item.copy(votes = item.votes + 1))
    }

    suspend fun deleteMealItem(id: Long) {
        finDao.deleteMealItemById(id)
    }

    suspend fun addGroceryItem(
        groupId: Long,
        listId: Long?,
        itemName: String,
        quantity: String,
        estimatedPrice: Double,
        assignedToUserId: Long?
    ) {
        if (itemName.isBlank()) return
        val glId = listId ?: finDao.insertGroceryList(
            GroceryListEntity(groupId = groupId, title = "Shared Grocery List")
        )
        finDao.insertGroceryItem(
            GroceryItemEntity(
                groceryListId = glId,
                itemName = itemName.trim(),
                quantity = quantity.ifBlank { "1 unit" },
                estimatedPrice = estimatedPrice,
                assignedToUserId = assignedToUserId
            )
        )
    }

    suspend fun toggleGroceryPurchased(item: GroceryItemEntity, userId: Long) {
        val next = !item.isPurchased
        finDao.updateGroceryItem(
            item.copy(
                isPurchased = next,
                purchasedByUserId = if (next) userId else null
            )
        )
    }

    suspend fun deleteGroceryItem(id: Long) {
        finDao.deleteGroceryItemById(id)
    }

    suspend fun syncPurchasedGroceriesToExpense(
        groupId: Long,
        payerId: Long,
        purchasedItems: List<GroceryItemEntity>,
        memberIds: List<Long>
    ): Result<Double> {
        val total = purchasedItems.sumOf { it.estimatedPrice }
        if (purchasedItems.isEmpty() || total <= 0.0) {
            return Result.failure(IllegalArgumentException("Select/purchase priced grocery items first."))
        }
        val names = purchasedItems.joinToString(", ") { it.itemName }
        val participants = (memberIds.ifEmpty { listOf(payerId) }).map { SplitParticipantInput(it) }
        return addExpenseWithSplits(
            groupId = groupId,
            paidByUserId = payerId,
            category = "Grocery",
            title = "Grocery Sync (${purchasedItems.size} items)",
            amount = total,
            splitType = SplitType.EQUAL,
            participants = participants,
            expenseDate = LocalDate.now().toString(),
            notes = "Synced items: $names"
        ).map { total }
    }

    suspend fun submitMaintenance(
        propertyId: Long,
        tenantId: Long,
        landlordId: Long,
        category: String,
        title: String,
        description: String,
        priority: String
    ) {
        if (title.isBlank()) return
        finDao.insertMaintenance(
            MaintenanceRequestEntity(
                propertyId = propertyId,
                tenantId = tenantId,
                landlordId = landlordId,
                category = category,
                title = title.trim(),
                description = description.trim(),
                priority = priority
            )
        )
        finDao.insertNotification(
            NotificationEntity(
                userId = landlordId,
                type = "maintenance",
                title = "New Maintenance Ticket",
                message = "$priority priority: ${title.trim()}"
            )
        )
    }

    suspend fun updateMaintenanceStatus(req: MaintenanceRequestEntity, status: String, response: String) {
        finDao.updateMaintenance(req.copy(status = status, landlordResponse = response.trim()))
        finDao.insertNotification(
            NotificationEntity(
                userId = req.tenantId,
                type = "maintenance",
                title = "Maintenance Updated ($status)",
                message = "${req.title}: ${response.ifBlank { status }}"
            )
        )
    }

    suspend fun submitReview(propertyId: Long, reviewerId: Long, landlordId: Long, rating: Int, comment: String) {
        if (comment.isBlank()) return
        finDao.insertReview(
            ReviewEntity(
                propertyId = propertyId,
                reviewerId = reviewerId,
                landlordId = landlordId,
                rating = rating.coerceIn(1, 5),
                comment = comment.trim()
            )
        )
    }

    suspend fun markAllNotificationsRead(userId: Long) {
        finDao.markAllNotificationsRead(userId)
    }
}
