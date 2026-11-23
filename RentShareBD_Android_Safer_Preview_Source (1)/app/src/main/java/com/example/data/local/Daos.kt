package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {
    @Query("SELECT * FROM users")
    fun observeAllUsers(): Flow<List<UserEntity>>

    @Query("SELECT * FROM users")
    suspend fun getAllUsers(): List<UserEntity>

    @Query("SELECT * FROM users WHERE id = :id LIMIT 1")
    suspend fun getUserById(id: Long): UserEntity?

    @Query("SELECT * FROM users WHERE LOWER(email) = LOWER(:email) LIMIT 1")
    suspend fun getUserByEmail(email: String): UserEntity?

    @Query("SELECT COUNT(*) FROM users")
    suspend fun countUsers(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: UserEntity): Long

    @Update
    suspend fun updateUser(user: UserEntity)

    @Query("SELECT * FROM tenant_profiles")
    fun observeTenantProfiles(): Flow<List<TenantProfileEntity>>

    @Query("SELECT * FROM tenant_profiles WHERE userId = :userId LIMIT 1")
    suspend fun getTenantProfile(userId: Long): TenantProfileEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTenantProfile(profile: TenantProfileEntity): Long

    @Query("SELECT * FROM landlord_profiles")
    fun observeLandlordProfiles(): Flow<List<LandlordProfileEntity>>

    @Query("SELECT * FROM landlord_profiles WHERE userId = :userId LIMIT 1")
    suspend fun getLandlordProfile(userId: Long): LandlordProfileEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLandlordProfile(profile: LandlordProfileEntity): Long

    @Query("SELECT * FROM roommate_preferences")
    fun observeAllPreferences(): Flow<List<RoommatePreferenceEntity>>

    @Query("SELECT * FROM roommate_preferences WHERE userId = :userId LIMIT 1")
    suspend fun getPreferences(userId: Long): RoommatePreferenceEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPreferences(pref: RoommatePreferenceEntity): Long
}

@Dao
interface PropertyDao {
    @Query("SELECT * FROM properties ORDER BY createdAt DESC")
    fun observeAllProperties(): Flow<List<PropertyEntity>>

    @Query("SELECT * FROM properties ORDER BY createdAt DESC")
    suspend fun getAllProperties(): List<PropertyEntity>

    @Query("SELECT * FROM properties WHERE id = :id LIMIT 1")
    suspend fun getPropertyById(id: Long): PropertyEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProperty(property: PropertyEntity): Long

    @Update
    suspend fun updateProperty(property: PropertyEntity)

    @Query("SELECT * FROM rooms")
    fun observeAllRooms(): Flow<List<RoomEntity>>

    @Query("SELECT * FROM rooms WHERE propertyId = :propertyId")
    suspend fun getRoomsForProperty(propertyId: Long): List<RoomEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRoom(room: RoomEntity): Long

    @Update
    suspend fun updateRoom(room: RoomEntity)

    @Query("SELECT * FROM saved_properties WHERE userId = :userId")
    fun observeSavedForUser(userId: Long): Flow<List<SavedPropertyEntity>>

    @Query("SELECT * FROM saved_properties WHERE userId = :userId AND propertyId = :propertyId LIMIT 1")
    suspend fun getSaved(userId: Long, propertyId: Long): SavedPropertyEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSaved(saved: SavedPropertyEntity): Long

    @Query("DELETE FROM saved_properties WHERE userId = :userId AND propertyId = :propertyId")
    suspend fun deleteSaved(userId: Long, propertyId: Long)
}

@Dao
interface SocialAndRentalDao {
    @Query("SELECT * FROM roommate_requests ORDER BY createdAt DESC")
    fun observeRoommateRequests(): Flow<List<RoommateRequestEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRoommateRequest(req: RoommateRequestEntity): Long

    @Update
    suspend fun updateRoommateRequest(req: RoommateRequestEntity)

    @Query("SELECT * FROM rental_requests ORDER BY createdAt DESC")
    fun observeRentalRequests(): Flow<List<RentalRequestEntity>>

    @Query("SELECT * FROM rental_requests WHERE id = :id LIMIT 1")
    suspend fun getRentalRequestById(id: Long): RentalRequestEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRentalRequest(req: RentalRequestEntity): Long

    @Update
    suspend fun updateRentalRequest(req: RentalRequestEntity)

    @Query("SELECT * FROM rental_agreements ORDER BY id DESC")
    fun observeAgreements(): Flow<List<RentalAgreementEntity>>

    @Query("SELECT * FROM rental_agreements WHERE id = :id LIMIT 1")
    suspend fun getAgreementById(id: Long): RentalAgreementEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAgreement(ag: RentalAgreementEntity): Long

    @Update
    suspend fun updateAgreement(ag: RentalAgreementEntity)

    @Query("SELECT * FROM roommate_groups ORDER BY id DESC")
    fun observeGroups(): Flow<List<RoommateGroupEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGroup(group: RoommateGroupEntity): Long

    @Query("SELECT * FROM group_members")
    fun observeGroupMembers(): Flow<List<GroupMemberEntity>>

    @Query("SELECT * FROM group_members WHERE groupId = :groupId")
    suspend fun getMembersForGroup(groupId: Long): List<GroupMemberEntity>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertGroupMember(member: GroupMemberEntity): Long

    @Query("DELETE FROM group_members WHERE groupId = :groupId AND userId = :userId")
    suspend fun deleteGroupMember(groupId: Long, userId: Long)

    @Query("SELECT * FROM messages ORDER BY createdAt ASC")
    fun observeMessages(): Flow<List<MessageEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(msg: MessageEntity): Long
}

@Dao
interface FinanceAndLivingDao {
    @Query("SELECT * FROM expenses ORDER BY expenseDate DESC, id DESC")
    fun observeExpenses(): Flow<List<ExpenseEntity>>

    @Query("SELECT * FROM expenses WHERE groupId = :groupId ORDER BY expenseDate DESC, id DESC")
    suspend fun getExpensesByGroup(groupId: Long): List<ExpenseEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExpense(exp: ExpenseEntity): Long

    @Update
    suspend fun updateExpense(exp: ExpenseEntity)

    @Query("DELETE FROM expenses WHERE id = :id")
    suspend fun deleteExpenseById(id: Long)

    @Query("SELECT * FROM expense_splits")
    fun observeExpenseSplits(): Flow<List<ExpenseSplitEntity>>

    @Query("SELECT * FROM expense_splits WHERE expenseId = :expenseId")
    suspend fun getSplitsForExpense(expenseId: Long): List<ExpenseSplitEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExpenseSplit(split: ExpenseSplitEntity): Long

    @Query("DELETE FROM expense_splits WHERE expenseId = :expenseId")
    suspend fun deleteSplitsForExpense(expenseId: Long)

    @Query("SELECT * FROM settlements ORDER BY settledAt DESC")
    fun observeSettlements(): Flow<List<SettlementEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSettlement(s: SettlementEntity): Long

    @Query("SELECT * FROM utility_bills ORDER BY dueDate ASC")
    fun observeUtilityBills(): Flow<List<UtilityBillEntity>>

    @Query("SELECT * FROM utility_bills WHERE id = :id LIMIT 1")
    suspend fun getUtilityBillById(id: Long): UtilityBillEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUtilityBill(bill: UtilityBillEntity): Long

    @Update
    suspend fun updateUtilityBill(bill: UtilityBillEntity)

    @Query("DELETE FROM utility_bills WHERE id = :id")
    suspend fun deleteUtilityBillById(id: Long)

    @Query("SELECT * FROM bill_payments ORDER BY paidAt DESC")
    fun observeBillPayments(): Flow<List<BillPaymentEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBillPayment(payment: BillPaymentEntity): Long

    @Query("SELECT * FROM meal_plans")
    fun observeMealPlans(): Flow<List<MealPlanEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMealPlan(plan: MealPlanEntity): Long

    @Query("SELECT * FROM meal_items")
    fun observeMealItems(): Flow<List<MealItemEntity>>

    @Query("SELECT * FROM meal_items WHERE id = :id LIMIT 1")
    suspend fun getMealItemById(id: Long): MealItemEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMealItem(item: MealItemEntity): Long

    @Update
    suspend fun updateMealItem(item: MealItemEntity)

    @Query("DELETE FROM meal_items WHERE id = :id")
    suspend fun deleteMealItemById(id: Long)

    @Query("SELECT * FROM meal_participants")
    fun observeMealParticipants(): Flow<List<MealParticipantEntity>>

    @Query("SELECT * FROM meal_participants WHERE mealItemId = :mealItemId AND userId = :userId LIMIT 1")
    suspend fun getMealParticipant(mealItemId: Long, userId: Long): MealParticipantEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMealParticipant(part: MealParticipantEntity): Long

    @Query("SELECT * FROM grocery_lists")
    fun observeGroceryLists(): Flow<List<GroceryListEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGroceryList(list: GroceryListEntity): Long

    @Query("SELECT * FROM grocery_items")
    fun observeGroceryItems(): Flow<List<GroceryItemEntity>>

    @Query("SELECT * FROM grocery_items WHERE id = :id LIMIT 1")
    suspend fun getGroceryItemById(id: Long): GroceryItemEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGroceryItem(item: GroceryItemEntity): Long

    @Update
    suspend fun updateGroceryItem(item: GroceryItemEntity)

    @Query("DELETE FROM grocery_items WHERE id = :id")
    suspend fun deleteGroceryItemById(id: Long)

    @Query("SELECT * FROM maintenance_requests ORDER BY createdAt DESC")
    fun observeMaintenanceRequests(): Flow<List<MaintenanceRequestEntity>>

    @Query("SELECT * FROM maintenance_requests WHERE id = :id LIMIT 1")
    suspend fun getMaintenanceById(id: Long): MaintenanceRequestEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMaintenance(req: MaintenanceRequestEntity): Long

    @Update
    suspend fun updateMaintenance(req: MaintenanceRequestEntity)

    @Query("SELECT * FROM reviews ORDER BY createdAt DESC")
    fun observeReviews(): Flow<List<ReviewEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReview(review: ReviewEntity): Long

    @Query("SELECT * FROM complaints ORDER BY createdAt DESC")
    fun observeComplaints(): Flow<List<ComplaintEntity>>

    @Query("SELECT * FROM complaints WHERE id = :id LIMIT 1")
    suspend fun getComplaintById(id: Long): ComplaintEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertComplaint(complaint: ComplaintEntity): Long

    @Update
    suspend fun updateComplaint(complaint: ComplaintEntity)

    @Query("SELECT * FROM verification_records ORDER BY id DESC")
    fun observeVerifications(): Flow<List<VerificationRecordEntity>>

    @Query("SELECT * FROM verification_records WHERE id = :id LIMIT 1")
    suspend fun getVerificationById(id: Long): VerificationRecordEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVerification(v: VerificationRecordEntity): Long

    @Update
    suspend fun updateVerification(v: VerificationRecordEntity)

    @Query("SELECT * FROM notifications ORDER BY createdAt DESC")
    fun observeNotifications(): Flow<List<NotificationEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotification(n: NotificationEntity): Long

    @Query("UPDATE notifications SET isRead = 1 WHERE userId = :userId")
    suspend fun markAllNotificationsRead(userId: Long)
}
