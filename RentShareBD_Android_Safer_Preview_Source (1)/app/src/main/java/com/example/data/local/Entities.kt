package com.example.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "users",
    indices = [Index(value = ["email"], unique = true)]
)
data class UserEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val email: String,
    val passwordHash: String,
    val role: String = "tenant", // tenant, landlord, admin
    val fullName: String,
    val phone: String = "",
    val avatarUrl: String = "",
    val emailVerified: Boolean = false,
    val phoneVerified: Boolean = false,
    val identityVerified: Boolean = false,
    val isSuspended: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "tenant_profiles",
    foreignKeys = [ForeignKey(entity = UserEntity::class, parentColumns = ["id"], childColumns = ["userId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index(value = ["userId"], unique = true)]
)
data class TenantProfileEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val userId: Long,
    val gender: String = "",
    val occupation: String = "Student",
    val institutionOrWorkplace: String = "",
    val preferredLocation: String = "Mirpur",
    val monthlyBudget: Int = 9000,
    val moveInDate: String = "2026-12-01",
    val preferredRoomType: String = "Shared Room",
    val bio: String = "",
    val completedRentals: Int = 0,
    val cancellations: Int = 0
)

@Entity(
    tableName = "landlord_profiles",
    foreignKeys = [ForeignKey(entity = UserEntity::class, parentColumns = ["id"], childColumns = ["userId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index(value = ["userId"], unique = true)]
)
data class LandlordProfileEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val userId: Long,
    val address: String = "",
    val verificationStatus: String = "unverified", // unverified, pending, verified, rejected
    val nidLast4: String = "",
    val completedRentals: Int = 0,
    val cancellations: Int = 0
)

@Entity(
    tableName = "roommate_preferences",
    foreignKeys = [ForeignKey(entity = UserEntity::class, parentColumns = ["id"], childColumns = ["userId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index(value = ["userId"], unique = true)]
)
data class RoommatePreferenceEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val userId: Long,
    val monthlyBudget: Int = 9000,
    val preferredLocation: String = "Mirpur",
    val sleepSchedule: String = "moderate",
    val wakeSchedule: String = "07:30 AM",
    val smokingPreference: String = "non_smoker",
    val cookingHabits: String = "occasional",
    val cleanlinessLevel: Int = 4,
    val noisePreference: String = "quiet",
    val studyWorkSchedule: String = "9 AM - 5 PM",
    val guestPreference: String = "occasional_daytime",
    val sharingPreference: String = "Open to sharing kitchen & utilities"
)

@Entity(
    tableName = "properties",
    foreignKeys = [ForeignKey(entity = UserEntity::class, parentColumns = ["id"], childColumns = ["landlordId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index(value = ["landlordId"]), Index(value = ["area"])]
)
data class PropertyEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val landlordId: Long,
    val title: String,
    val description: String,
    val area: String,
    val address: String,
    val latitude: Double = 23.8069,
    val longitude: Double = 90.3687,
    val roomType: String = "Single Room",
    val totalRooms: Int = 3,
    val furnished: Boolean = true,
    val attachedBath: Boolean = false,
    val hasBalcony: Boolean = true,
    val hasWifi: Boolean = true,
    val hasParking: Boolean = false,
    val hasGenerator: Boolean = true,
    val monthlyRent: Int,
    val advanceAmount: Int = 0,
    val serviceCharge: Int = 0,
    val estimatedElectricity: Int = 0,
    val estimatedWater: Int = 0,
    val estimatedGas: Int = 0,
    val estimatedInternet: Int = 0,
    val otherRecurringCharges: Int = 0,
    val availableFrom: String,
    val availabilityStatus: String = "available", // available, occupied, pending_verification, suspended
    val verificationStatus: String = "pending", // pending, approved, rejected
    val isSuspicious: Boolean = false,
    val suspiciousReason: String = "",
    val primaryImageKey: String = "img_prop_mirpur",
    val facilitiesCsv: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "rooms",
    foreignKeys = [ForeignKey(entity = PropertyEntity::class, parentColumns = ["id"], childColumns = ["propertyId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index(value = ["propertyId"])]
)
data class RoomEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val propertyId: Long,
    val roomLabel: String,
    val roomType: String = "Single Room",
    val monthlyRent: Int,
    val isAvailable: Boolean = true,
    val availableFrom: String = "2026-12-01"
)

@Entity(
    tableName = "saved_properties",
    indices = [Index(value = ["userId", "propertyId"], unique = true)]
)
data class SavedPropertyEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val userId: Long,
    val propertyId: Long,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "roommate_requests")
data class RoommateRequestEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val senderId: Long,
    val receiverId: Long,
    val message: String,
    val status: String = "pending", // pending, accepted, rejected, cancelled
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "rental_requests")
data class RentalRequestEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val propertyId: Long,
    val tenantId: Long,
    val moveInDate: String,
    val durationMonths: Int = 12,
    val message: String,
    val status: String = "pending", // pending, accepted, rejected, cancelled
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "rental_agreements")
data class RentalAgreementEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val propertyId: Long,
    val tenantId: Long,
    val landlordId: Long,
    val monthlyRent: Int,
    val advanceAmount: Int,
    val durationMonths: Int = 12,
    val noticePeriodDays: Int = 30,
    val utilityTerms: String,
    val houseRules: String,
    val startDate: String,
    val endDate: String,
    val tenantAccepted: Boolean = false,
    val tenantAcceptedAt: String? = null,
    val landlordAccepted: Boolean = true,
    val status: String = "pending_tenant" // draft, pending_tenant, active, expired, terminated
)

@Entity(tableName = "roommate_groups")
data class RoommateGroupEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val propertyId: Long? = null,
    val createdBy: Long,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "group_members",
    indices = [Index(value = ["groupId", "userId"], unique = true)]
)
data class GroupMemberEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val groupId: Long,
    val userId: Long,
    val role: String = "member", // admin, member
    val joinedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "messages")
data class MessageEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val groupId: Long? = null,
    val senderId: Long,
    val receiverId: Long? = null,
    val content: String,
    val isRead: Boolean = false,
    val createdAtLabel: String = "Now",
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "expenses")
data class ExpenseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val groupId: Long,
    val paidByUserId: Long,
    val category: String,
    val title: String,
    val amount: Double,
    val splitType: String = "equal", // equal, custom, percentage
    val expenseDate: String,
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "expense_splits",
    foreignKeys = [ForeignKey(entity = ExpenseEntity::class, parentColumns = ["id"], childColumns = ["expenseId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index(value = ["expenseId"])]
)
data class ExpenseSplitEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val expenseId: Long,
    val userId: Long,
    val shareAmount: Double,
    val isSettled: Boolean = false
)

@Entity(tableName = "settlements")
data class SettlementEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val groupId: Long,
    val fromUserId: Long,
    val toUserId: Long,
    val amount: Double,
    val paymentMethod: String = "Cash / Manual",
    val referenceNote: String = "",
    val settledAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "utility_bills")
data class UtilityBillEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val groupId: Long,
    val category: String,
    val billName: String,
    val amount: Double,
    val dueDate: String,
    val billingPeriod: String = "Nov 2026",
    val autoPayEnabled: Boolean = false,
    val autoPayProvider: String = "",
    val paidAt: String? = null
)

@Entity(tableName = "bill_payments")
data class BillPaymentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val billId: Long,
    val paidByUserId: Long,
    val amount: Double,
    val provider: String = "Manual",
    val transactionRef: String = "",
    val paidAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "meal_plans")
data class MealPlanEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val groupId: Long,
    val weekLabel: String,
    val createdBy: Long
)

@Entity(
    tableName = "meal_items",
    foreignKeys = [ForeignKey(entity = MealPlanEntity::class, parentColumns = ["id"], childColumns = ["mealPlanId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index(value = ["mealPlanId"])]
)
data class MealItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val mealPlanId: Long,
    val dayOfWeek: String,
    val mealSlot: String,
    val mealName: String,
    val description: String = "",
    val votes: Int = 0
)

@Entity(
    tableName = "meal_participants",
    indices = [Index(value = ["mealItemId", "userId"], unique = true)]
)
data class MealParticipantEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val mealItemId: Long,
    val userId: Long,
    val voted: Boolean = false
)

@Entity(tableName = "grocery_lists")
data class GroceryListEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val groupId: Long,
    val title: String,
    val syncedExpenseId: Long? = null
)

@Entity(
    tableName = "grocery_items",
    foreignKeys = [ForeignKey(entity = GroceryListEntity::class, parentColumns = ["id"], childColumns = ["groceryListId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index(value = ["groceryListId"])]
)
data class GroceryItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val groceryListId: Long,
    val itemName: String,
    val quantity: String,
    val estimatedPrice: Double = 0.0,
    val assignedToUserId: Long? = null,
    val isPurchased: Boolean = false,
    val purchasedByUserId: Long? = null
)

@Entity(tableName = "maintenance_requests")
data class MaintenanceRequestEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val propertyId: Long,
    val tenantId: Long,
    val landlordId: Long,
    val category: String,
    val title: String,
    val description: String,
    val priority: String = "Medium", // Low, Medium, High, Urgent
    val status: String = "Submitted", // Submitted, Assigned, In Progress, Completed, Rejected
    val landlordResponse: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "reviews")
data class ReviewEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val propertyId: Long,
    val reviewerId: Long,
    val landlordId: Long,
    val rating: Int,
    val comment: String,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "complaints")
data class ComplaintEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val reporterId: Long,
    val targetUserId: Long? = null,
    val propertyId: Long? = null,
    val reason: String,
    val details: String,
    val status: String = "open", // open, investigating, resolved, dismissed
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "verification_records")
data class VerificationRecordEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val userId: Long,
    val verificationType: String, // email, phone, student_id, nid, landlord_id
    val documentRef: String,
    val status: String = "pending", // pending, approved, rejected
    val reviewedBy: Long? = null,
    val notes: String = ""
)

@Entity(tableName = "notifications")
data class NotificationEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val userId: Long,
    val type: String,
    val title: String,
    val message: String,
    val isRead: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)
