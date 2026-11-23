package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        UserEntity::class,
        TenantProfileEntity::class,
        LandlordProfileEntity::class,
        RoommatePreferenceEntity::class,
        PropertyEntity::class,
        RoomEntity::class,
        SavedPropertyEntity::class,
        RoommateRequestEntity::class,
        RentalRequestEntity::class,
        RentalAgreementEntity::class,
        RoommateGroupEntity::class,
        GroupMemberEntity::class,
        MessageEntity::class,
        ExpenseEntity::class,
        ExpenseSplitEntity::class,
        SettlementEntity::class,
        UtilityBillEntity::class,
        BillPaymentEntity::class,
        MealPlanEntity::class,
        MealItemEntity::class,
        MealParticipantEntity::class,
        GroceryListEntity::class,
        GroceryItemEntity::class,
        MaintenanceRequestEntity::class,
        ReviewEntity::class,
        ComplaintEntity::class,
        VerificationRecordEntity::class,
        NotificationEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun propertyDao(): PropertyDao
    abstract fun socialAndRentalDao(): SocialAndRentalDao
    abstract fun financeAndLivingDao(): FinanceAndLivingDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "rentshare_bd.db"
                ).build().also { INSTANCE = it }
            }
        }
    }
}

object DatabaseSeeder {
    suspend fun seedIfEmpty(db: AppDatabase) {
        val userDao = db.userDao()
        if (userDao.countUsers() > 0) return

        val propDao = db.propertyDao()
        val socialDao = db.socialAndRentalDao()
        val finDao = db.financeAndLivingDao()

        val demoPass = LocalPasswordHasher.hash("Demo1234!")

        val simiId = userDao.insertUser(
            UserEntity(
                email = "simi@rentshare.bd",
                passwordHash = demoPass,
                role = "tenant",
                fullName = "Simi Akter (Demo)",
                phone = "+8801711000101",
                emailVerified = true,
                phoneVerified = true,
                identityVerified = true
            )
        )
        val rahimId = userDao.insertUser(
            UserEntity(
                email = "rahim@rentshare.bd",
                passwordHash = demoPass,
                role = "tenant",
                fullName = "Rahim Uddin (Demo)",
                phone = "+8801711000102",
                emailVerified = true,
                phoneVerified = true,
                identityVerified = true
            )
        )
        val tanvirId = userDao.insertUser(
            UserEntity(
                email = "tanvir@rentshare.bd",
                passwordHash = demoPass,
                role = "tenant",
                fullName = "Tanvir Hasan (Demo)",
                phone = "+8801711000103",
                emailVerified = true,
                phoneVerified = true,
                identityVerified = false
            )
        )
        val nusratId = userDao.insertUser(
            UserEntity(
                email = "nusrat@rentshare.bd",
                passwordHash = demoPass,
                role = "tenant",
                fullName = "Nusrat Jahan (Demo)",
                phone = "+8801711000104",
                emailVerified = true,
                phoneVerified = false,
                identityVerified = true
            )
        )
        val karimLandlordId = userDao.insertUser(
            UserEntity(
                email = "karim.landlord@rentshare.bd",
                passwordHash = demoPass,
                role = "landlord",
                fullName = "Kamrul Karim (Demo Landlord)",
                phone = "+8801811000201",
                emailVerified = true,
                phoneVerified = true,
                identityVerified = true
            )
        )
        val farhanaLandlordId = userDao.insertUser(
            UserEntity(
                email = "farhana.landlord@rentshare.bd",
                passwordHash = demoPass,
                role = "landlord",
                fullName = "Farhana Begum (Demo Landlord)",
                phone = "+8801811000202",
                emailVerified = true,
                phoneVerified = true,
                identityVerified = false
            )
        )
        val adminId = userDao.insertUser(
            UserEntity(
                email = "admin@rentshare.bd",
                passwordHash = demoPass,
                role = "admin",
                fullName = "Platform Admin (Demo)",
                phone = "+8801911000301",
                emailVerified = true,
                phoneVerified = true,
                identityVerified = true
            )
        )

        // Tenant profiles
        userDao.insertTenantProfile(
            TenantProfileEntity(
                userId = simiId,
                gender = "Female",
                occupation = "CSE Student",
                institutionOrWorkplace = "Daffodil International University",
                preferredLocation = "Mirpur",
                monthlyBudget = 9500,
                moveInDate = "2026-12-01",
                preferredRoomType = "Single Room",
                bio = "Quiet final-year student looking for a clean shared flat near Mirpur 10 metro.",
                completedRentals = 1
            )
        )
        userDao.insertTenantProfile(
            TenantProfileEntity(
                userId = rahimId,
                gender = "Male",
                occupation = "Junior Software Engineer",
                institutionOrWorkplace = "Karwan Bazar Tech Hub",
                preferredLocation = "Mirpur",
                monthlyBudget = 9000,
                moveInDate = "2026-12-01",
                preferredRoomType = "Single Room",
                bio = "Remote-hybrid developer, non-smoker, keeps kitchen tidy and respects quiet study hours.",
                completedRentals = 2
            )
        )
        userDao.insertTenantProfile(
            TenantProfileEntity(
                userId = tanvirId,
                gender = "Male",
                occupation = "BBA Student",
                institutionOrWorkplace = "North South University",
                preferredLocation = "Bashundhara",
                monthlyBudget = 12000,
                moveInDate = "2026-12-01",
                preferredRoomType = "Single Room",
                bio = "Looking for an active shared apartment in Bashundhara R/A Block C or D.",
                completedRentals = 0
            )
        )
        userDao.insertTenantProfile(
            TenantProfileEntity(
                userId = nusratId,
                gender = "Female",
                occupation = "UI/UX Designer",
                institutionOrWorkplace = "Banani Studio",
                preferredLocation = "Dhanmondi",
                monthlyBudget = 10500,
                moveInDate = "2026-12-05",
                preferredRoomType = "Shared Room",
                bio = "Clean, creative professional who cooks occasionally and loves calm mornings.",
                completedRentals = 1
            )
        )

        // Roommate preferences
        userDao.insertPreferences(
            RoommatePreferenceEntity(
                userId = simiId,
                monthlyBudget = 9500,
                preferredLocation = "Mirpur",
                sleepSchedule = "moderate",
                wakeSchedule = "07:30 AM",
                smokingPreference = "non_smoker",
                cookingHabits = "daily_cook",
                cleanlinessLevel = 5,
                noisePreference = "quiet",
                studyWorkSchedule = "8:30 AM - 5:00 PM",
                guestPreference = "occasional_daytime",
                sharingPreference = "Share kitchen, Wi-Fi & utilities"
            )
        )
        userDao.insertPreferences(
            RoommatePreferenceEntity(
                userId = rahimId,
                monthlyBudget = 9000,
                preferredLocation = "Mirpur",
                sleepSchedule = "moderate",
                wakeSchedule = "08:00 AM",
                smokingPreference = "non_smoker",
                cookingHabits = "occasional",
                cleanlinessLevel = 4,
                noisePreference = "quiet",
                studyWorkSchedule = "9:30 AM - 6:30 PM",
                guestPreference = "occasional_daytime",
                sharingPreference = "Share bills, groceries & weekly meals"
            )
        )
        userDao.insertPreferences(
            RoommatePreferenceEntity(
                userId = tanvirId,
                monthlyBudget = 12000,
                preferredLocation = "Bashundhara",
                sleepSchedule = "night_owl",
                wakeSchedule = "09:30 AM",
                smokingPreference = "outside_only",
                cookingHabits = "rarely_order_out",
                cleanlinessLevel = 3,
                noisePreference = "lively",
                studyWorkSchedule = "11:00 AM - 7:00 PM",
                guestPreference = "flexible",
                sharingPreference = "Share rent & internet"
            )
        )
        userDao.insertPreferences(
            RoommatePreferenceEntity(
                userId = nusratId,
                monthlyBudget = 10500,
                preferredLocation = "Dhanmondi",
                sleepSchedule = "early_bird",
                wakeSchedule = "06:45 AM",
                smokingPreference = "non_smoker",
                cookingHabits = "daily_cook",
                cleanlinessLevel = 5,
                noisePreference = "quiet",
                studyWorkSchedule = "9:00 AM - 5:30 PM",
                guestPreference = "no_guests",
                sharingPreference = "Share kitchen & cleaning"
            )
        )

        // Landlord profiles
        userDao.insertLandlordProfile(
            LandlordProfileEntity(
                userId = karimLandlordId,
                address = "House 14, Road 4, Mirpur 10, Dhaka",
                verificationStatus = "verified",
                nidLast4 = "4821",
                completedRentals = 4
            )
        )
        userDao.insertLandlordProfile(
            LandlordProfileEntity(
                userId = farhanaLandlordId,
                address = "Sector 11, Road 9, Uttara, Dhaka",
                verificationStatus = "pending",
                nidLast4 = "9104",
                completedRentals = 0
            )
        )

        // Properties
        val p1 = propDao.insertProperty(
            PropertyEntity(
                landlordId = karimLandlordId,
                title = "Sunlit 3-Room Shared Flat near Mirpur 10 Metro",
                description = "Bright 4th-floor shared apartment just 4 minutes walk from Mirpur 10 Metro Station. Ideal for university students and young professionals. Includes backup generator, high-speed fiber Wi-Fi, filtered water, and dedicated study desk.",
                area = "Mirpur",
                address = "Flat 4B, House 14, Block C, Mirpur 10, Dhaka",
                latitude = 23.8069,
                longitude = 90.3687,
                roomType = "Single Room",
                totalRooms = 3,
                furnished = true,
                attachedBath = true,
                hasBalcony = true,
                hasWifi = true,
                hasParking = true,
                hasGenerator = true,
                monthlyRent = 7000,
                advanceAmount = 14000,
                serviceCharge = 500,
                estimatedElectricity = 1000,
                estimatedWater = 200,
                estimatedGas = 300,
                estimatedInternet = 500,
                availableFrom = "2026-12-01",
                availabilityStatus = "available",
                verificationStatus = "approved",
                isSuspicious = false,
                primaryImageKey = "img_prop_mirpur",
                facilitiesCsv = "High-Speed Wi-Fi,Generator Backup,Balcony,Attached Bath,Water Filter,24/7 Guard"
            )
        )
        val p2 = propDao.insertProperty(
            PropertyEntity(
                landlordId = karimLandlordId,
                title = "Modern Lake-View Shared Apartment in Uttara Sector 11",
                description = "Spacious shared living in a quiet residential block of Uttara Sector 11. Features tiled lounge, large shared dining space, lift, 24/7 security guard, and full kitchen access.",
                area = "Uttara",
                address = "House 22, Road 9, Sector 11, Uttara, Dhaka",
                latitude = 23.8759,
                longitude = 90.3795,
                roomType = "Single Room",
                totalRooms = 4,
                furnished = true,
                attachedBath = true,
                hasBalcony = true,
                hasWifi = true,
                hasParking = true,
                hasGenerator = true,
                monthlyRent = 9500,
                advanceAmount = 19000,
                serviceCharge = 800,
                estimatedElectricity = 1200,
                estimatedWater = 250,
                estimatedGas = 350,
                estimatedInternet = 500,
                availableFrom = "2026-12-01",
                availabilityStatus = "available",
                verificationStatus = "approved",
                isSuspicious = false,
                primaryImageKey = "img_prop_uttara",
                facilitiesCsv = "High-Speed Wi-Fi,Generator Backup,Lift,Parking,Balcony,CCTV"
            )
        )
        val p3 = propDao.insertProperty(
            PropertyEntity(
                landlordId = karimLandlordId,
                title = "Cozy Student Seat & Study Flat near Dhanmondi 15",
                description = "Walking distance to Dhanmondi Lake, Star Kabab, and major private universities. Furnished with wooden wardrobe, single bed, study lamp, and dependable line gas.",
                area = "Dhanmondi",
                address = "Apt 3A, Road 15/A, Dhanmondi, Dhaka",
                latitude = 23.7461,
                longitude = 90.3742,
                roomType = "Shared Room",
                totalRooms = 3,
                furnished = true,
                attachedBath = false,
                hasBalcony = true,
                hasWifi = true,
                hasParking = false,
                hasGenerator = true,
                monthlyRent = 8200,
                advanceAmount = 16400,
                serviceCharge = 600,
                estimatedElectricity = 1100,
                estimatedWater = 200,
                estimatedGas = 300,
                estimatedInternet = 500,
                otherRecurringCharges = 100,
                availableFrom = "2026-12-05",
                availabilityStatus = "available",
                verificationStatus = "approved",
                isSuspicious = false,
                primaryImageKey = "img_prop_dhanmondi",
                facilitiesCsv = "High-Speed Wi-Fi,Generator Backup,Line Gas,Study Desk,Balcony"
            )
        )
        val p4 = propDao.insertProperty(
            PropertyEntity(
                landlordId = karimLandlordId,
                title = "Executive Shared Suite near NSU & IUB, Bashundhara R/A",
                description = "Contemporary apartment in Block C Bashundhara R/A with open kitchen bar, inverter AC readiness, 24/7 generator backup, and gated security.",
                area = "Bashundhara",
                address = "Plot 112, Block C, Bashundhara R/A, Dhaka",
                latitude = 23.8191,
                longitude = 90.4526,
                roomType = "Master Suite",
                totalRooms = 3,
                furnished = true,
                attachedBath = true,
                hasBalcony = true,
                hasWifi = true,
                hasParking = true,
                hasGenerator = true,
                monthlyRent = 11500,
                advanceAmount = 23000,
                serviceCharge = 1000,
                estimatedElectricity = 1400,
                estimatedWater = 300,
                estimatedGas = 400,
                estimatedInternet = 600,
                availableFrom = "2026-12-01",
                availabilityStatus = "available",
                verificationStatus = "approved",
                isSuspicious = false,
                primaryImageKey = "img_prop_bashundhara",
                facilitiesCsv = "High-Speed Wi-Fi,Generator Backup,Parking,Attached Bath,AC Ready,Gated Block"
            )
        )
        val p5 = propDao.insertProperty(
            PropertyEntity(
                landlordId = farhanaLandlordId,
                title = "Urgent Cheap Room in Mohammadpur Tajmahal Road",
                description = "Very low monthly rent for quick booking. Must pay 8 months advance deposit immediately before viewing.",
                area = "Mohammadpur",
                address = "House 8, Tajmahal Road, Mohammadpur, Dhaka",
                latitude = 23.7654,
                longitude = 90.3588,
                roomType = "Single Room",
                totalRooms = 2,
                furnished = false,
                attachedBath = false,
                hasBalcony = false,
                hasWifi = true,
                hasParking = false,
                hasGenerator = false,
                monthlyRent = 3200,
                advanceAmount = 28000,
                serviceCharge = 300,
                estimatedElectricity = 700,
                estimatedWater = 150,
                estimatedGas = 250,
                estimatedInternet = 400,
                availableFrom = "2026-11-20",
                availabilityStatus = "pending_verification",
                verificationStatus = "pending",
                isSuspicious = true,
                suspiciousReason = "Listed rent (৳3,200) is significantly below area median and demands excessive advance (৳28,000, >4x rent) from an unverified landlord.",
                primaryImageKey = "img_prop_mirpur",
                facilitiesCsv = "Wi-Fi"
            )
        )

        // Rooms
        propDao.insertRoom(RoomEntity(propertyId = p1, roomLabel = "Room A (Balcony Single)", roomType = "Single Room", monthlyRent = 7000, isAvailable = true))
        propDao.insertRoom(RoomEntity(propertyId = p1, roomLabel = "Room B (Study Single)", roomType = "Single Room", monthlyRent = 6800, isAvailable = true))
        propDao.insertRoom(RoomEntity(propertyId = p1, roomLabel = "Room C (Occupied by Simi)", roomType = "Single Room", monthlyRent = 7000, isAvailable = false))
        propDao.insertRoom(RoomEntity(propertyId = p2, roomLabel = "Master Suite", roomType = "Master Suite", monthlyRent = 9500, isAvailable = true))
        propDao.insertRoom(RoomEntity(propertyId = p2, roomLabel = "East Bedroom", roomType = "Single Room", monthlyRent = 8800, isAvailable = true))

        propDao.insertSaved(SavedPropertyEntity(userId = simiId, propertyId = p3))

        // Rental requests & agreement
        socialDao.insertRentalRequest(
            RentalRequestEntity(
                propertyId = p1,
                tenantId = simiId,
                moveInDate = "2026-11-01",
                durationMonths = 12,
                message = "Interested in Flat 4B Room C for 1 year academic stay.",
                status = "accepted"
            )
        )
        socialDao.insertRentalRequest(
            RentalRequestEntity(
                propertyId = p2,
                tenantId = rahimId,
                moveInDate = "2026-12-01",
                durationMonths = 12,
                message = "Software engineer working nearby, clean and non-smoker.",
                status = "pending"
            )
        )
        socialDao.insertAgreement(
            RentalAgreementEntity(
                propertyId = p1,
                tenantId = simiId,
                landlordId = karimLandlordId,
                monthlyRent = 7000,
                advanceAmount = 14000,
                durationMonths = 12,
                noticePeriodDays = 30,
                utilityTerms = "DESCO electricity, Titas gas, WASA water, and fiber Wi-Fi split equally among active flatmates.",
                houseRules = "No indoor smoking. Quiet hours after 11:00 PM. Guests require prior flatmate notice.",
                startDate = "2026-11-01",
                endDate = "2027-10-31",
                tenantAccepted = true,
                tenantAcceptedAt = "2026-10-25 14:00:00",
                landlordAccepted = true,
                status = "active"
            )
        )

        // Roommate group
        val groupId = socialDao.insertGroup(
            RoommateGroupEntity(name = "Flat 4B - Roommate Group", propertyId = p1, createdBy = simiId)
        )
        socialDao.insertGroupMember(GroupMemberEntity(groupId = groupId, userId = simiId, role = "admin"))
        socialDao.insertGroupMember(GroupMemberEntity(groupId = groupId, userId = rahimId, role = "member"))
        socialDao.insertGroupMember(GroupMemberEntity(groupId = groupId, userId = nusratId, role = "member"))

        // Roommate requests
        socialDao.insertRoommateRequest(
            RoommateRequestEntity(
                senderId = rahimId,
                receiverId = simiId,
                message = "Hey! Our Mirpur compatibility score is 92%. Would love to share Flat 4B.",
                status = "accepted"
            )
        )
        socialDao.insertRoommateRequest(
            RoommateRequestEntity(
                senderId = tanvirId,
                receiverId = simiId,
                message = "Hi Simi, noticed we both prefer quiet clean flats in Dhaka.",
                status = "pending"
            )
        )

        // Chat messages
        socialDao.insertMessage(MessageEntity(groupId = groupId, senderId = simiId, content = "Assalamu Alaikum everyone! Welcome to Flat 4B group chat.", isRead = true, createdAtLabel = "09:30"))
        socialDao.insertMessage(MessageEntity(groupId = groupId, senderId = rahimId, content = "Walaikum Assalam! I paid the DESCO electricity bill for November.", isRead = true, createdAtLabel = "10:15"))
        socialDao.insertMessage(MessageEntity(groupId = groupId, senderId = nusratId, content = "Thanks Rahim! Let us update the Saturday meal plan and grocery list tonight.", isRead = true, createdAtLabel = "18:45"))

        socialDao.insertMessage(MessageEntity(senderId = simiId, receiverId = karimLandlordId, content = "Hello Kamrul bhai, the bathroom tap in Flat 4B is dripping slightly.", isRead = true, createdAtLabel = "11:00"))
        socialDao.insertMessage(MessageEntity(senderId = karimLandlordId, receiverId = simiId, content = "Noted Simi, I have assigned a plumber for tomorrow afternoon.", isRead = true, createdAtLabel = "11:25"))

        // Expenses (Oct + Nov 2026 for AI Financial Coach MoM analysis)
        suspend fun addEqualExp(paidBy: Long, cat: String, title: String, amt: Double, date: String, notes: String) {
            val eid = finDao.insertExpense(
                ExpenseEntity(groupId = groupId, paidByUserId = paidBy, category = cat, title = title, amount = amt, splitType = "equal", expenseDate = date, notes = notes)
            )
            val share = amt / 3.0
            finDao.insertExpenseSplit(ExpenseSplitEntity(expenseId = eid, userId = simiId, shareAmount = share))
            finDao.insertExpenseSplit(ExpenseSplitEntity(expenseId = eid, userId = rahimId, shareAmount = share))
            finDao.insertExpenseSplit(ExpenseSplitEntity(expenseId = eid, userId = nusratId, shareAmount = share))
        }

        addEqualExp(simiId, "House rent", "November Flat 4B Rent", 21000.0, "2026-11-02", "Paid to Kamrul Karim")
        addEqualExp(rahimId, "Electricity", "DESCO Prepaid Recharge", 3000.0, "2026-11-05", "Meter #44210")
        addEqualExp(nusratId, "Grocery", "Weekly Bazar & Rice/Chicken", 2850.0, "2026-11-10", "Mirpur 10 Kitchen Market")
        addEqualExp(simiId, "Food", "Late Night Takeout & Snacks", 1800.0, "2026-11-12", "Foodpanda group order")
        addEqualExp(simiId, "House rent", "October Flat 4B Rent", 21000.0, "2026-10-02", "October Rent")
        addEqualExp(rahimId, "Grocery", "October Shared Groceries", 2400.0, "2026-10-14", "October Bazar")
        addEqualExp(nusratId, "Food", "October Dining Out", 1200.0, "2026-10-19", "Group dinner")

        finDao.insertSettlement(
            SettlementEntity(
                groupId = groupId,
                fromUserId = rahimId,
                toUserId = simiId,
                amount = 5000.0,
                paymentMethod = "bKash (Manual Ref)",
                referenceNote = "TRX992817A"
            )
        )

        // Utility Bills
        finDao.insertUtilityBill(UtilityBillEntity(groupId = groupId, category = "Internet", billName = "Link3 Fiber 50Mbps", amount = 1500.0, dueDate = "2026-11-16", autoPayProvider = "bKash"))
        finDao.insertUtilityBill(UtilityBillEntity(groupId = groupId, category = "Gas", billName = "Titas Gas Monthly Bill", amount = 1080.0, dueDate = "2026-11-18", autoPayProvider = "Nagad"))
        finDao.insertUtilityBill(UtilityBillEntity(groupId = groupId, category = "Water", billName = "Dhaka WASA Bill", amount = 750.0, dueDate = "2026-11-12"))
        finDao.insertUtilityBill(UtilityBillEntity(groupId = groupId, category = "Electricity", billName = "DESCO Electricity Bill", amount = 3000.0, dueDate = "2026-11-05", autoPayProvider = "bKash", paidAt = "2026-11-05 14:20:00"))

        // Weekly Meal Plan
        val mpId = finDao.insertMealPlan(MealPlanEntity(groupId = groupId, weekLabel = "Week of Nov 14 - Nov 20, 2026", createdBy = simiId))
        val meals = listOf(
            Triple("Saturday", "Breakfast", "Paratha and Egg" to "Aloo bhaji + fried egg + milk tea" to 3),
            Triple("Saturday", "Lunch", "Rice and Chicken Curry" to "Steamed miniket rice, deshi chicken curry & dal" to 3),
            Triple("Saturday", "Dinner", "Bhuna Khichuri" to "Moong dal khichuri with begun bhaja & achar" to 2),
            Triple("Sunday", "Breakfast", "Bread and Omelette" to "Toasted bread, masala omelette & banana" to 2),
            Triple("Sunday", "Lunch", "Rice and Rui Fish" to "Rui fish dopeyaja, shak & thick dal" to 3),
            Triple("Sunday", "Dinner", "Rice and Mixed Vegetables" to "Seasonal labra, egg bhorta & rice" to 2),
            Triple("Monday", "Breakfast", "Oats & Banana" to "Quick morning oats with honey" to 2),
            Triple("Monday", "Lunch", "Chicken Tehari" to "Light home-style tehari with cucumber salad" to 3),
            Triple("Tuesday", "Dinner", "Ruti & Beef Bhuna" to "Whole wheat ruti with spicy beef bhuna" to 3),
            Triple("Wednesday", "Lunch", "Rice, Egg Curry & Dal" to "Budget mid-week homestyle lunch" to 2),
            Triple("Thursday", "Dinner", "Fried Rice & Chili Chicken" to "Thursday night group special" to 3),
            Triple("Friday", "Lunch", "Kacchi / Polao Feast" to "Friday Jummah special chicken roast & polao" to 3)
        )
        for ((d, s, info) in meals) {
            val (nameDesc, votes) = info
            finDao.insertMealItem(
                MealItemEntity(
                    mealPlanId = mpId,
                    dayOfWeek = d,
                    mealSlot = s,
                    mealName = nameDesc.first,
                    description = nameDesc.second,
                    votes = votes
                )
            )
        }

        // Grocery list
        val glId = finDao.insertGroceryList(GroceryListEntity(groupId = groupId, title = "Weekly Bazar - Nov Week 3"))
        finDao.insertGroceryItem(GroceryItemEntity(groceryListId = glId, itemName = "Miniket Rice", quantity = "5 kg", estimatedPrice = 420.0, assignedToUserId = rahimId, isPurchased = true))
        finDao.insertGroceryItem(GroceryItemEntity(groceryListId = glId, itemName = "Broiler Chicken", quantity = "3 kg", estimatedPrice = 660.0, assignedToUserId = rahimId, isPurchased = true))
        finDao.insertGroceryItem(GroceryItemEntity(groceryListId = glId, itemName = "Farm Eggs", quantity = "30 pieces", estimatedPrice = 390.0, assignedToUserId = nusratId, isPurchased = true))
        finDao.insertGroceryItem(GroceryItemEntity(groceryListId = glId, itemName = "Seasonal Vegetables", quantity = "5 kg", estimatedPrice = 480.0, assignedToUserId = simiId, isPurchased = false))
        finDao.insertGroceryItem(GroceryItemEntity(groceryListId = glId, itemName = "Soybean Oil & Spices", quantity = "2 L + pack", estimatedPrice = 900.0, assignedToUserId = simiId, isPurchased = false))

        // Maintenance
        finDao.insertMaintenance(
            MaintenanceRequestEntity(
                propertyId = p1,
                tenantId = simiId,
                landlordId = karimLandlordId,
                category = "Plumbing",
                title = "Dripping basin tap in Bathroom 2",
                description = "Cold water tap handle keeps dripping even when fully shut.",
                priority = "Medium",
                status = "Assigned",
                landlordResponse = "Plumber scheduled for tomorrow at 3 PM."
            )
        )

        // Reviews, Verifications, Complaints, Notifications
        finDao.insertReview(
            ReviewEntity(
                propertyId = p1,
                reviewerId = simiId,
                landlordId = karimLandlordId,
                rating = 5,
                comment = "Very responsive landlord, clean flat, and transparent utility billing."
            )
        )
        finDao.insertVerification(VerificationRecordEntity(userId = simiId, verificationType = "student_id", documentRef = "DIU-241-35-381", status = "approved", reviewedBy = adminId, notes = "Student ID verified"))
        finDao.insertVerification(VerificationRecordEntity(userId = karimLandlordId, verificationType = "landlord_id", documentRef = "NID-****-4821", status = "approved", reviewedBy = adminId, notes = "Landlord NID & deed verified"))
        finDao.insertVerification(VerificationRecordEntity(userId = farhanaLandlordId, verificationType = "nid", documentRef = "NID-****-9104", status = "pending", notes = "Awaiting admin review"))

        finDao.insertComplaint(
            ComplaintEntity(
                reporterId = tanvirId,
                targetUserId = farhanaLandlordId,
                propertyId = p5,
                reason = "Excessive advance deposit & suspicious low rent",
                details = "Landlord asked for 8 months advance before showing the room.",
                status = "open"
            )
        )

        finDao.insertNotification(NotificationEntity(userId = simiId, type = "utility", title = "Internet bill due tomorrow", message = "Link3 Fiber 50Mbps (৳1,500) is due on 2026-11-16."))
        finDao.insertNotification(NotificationEntity(userId = simiId, type = "roommate", title = "New roommate request", message = "Tanvir Hasan sent you a roommate connection request."))
        finDao.insertNotification(NotificationEntity(userId = simiId, type = "maintenance", title = "Maintenance status updated", message = "Kamrul Karim assigned a plumber for your basin tap issue."))
    }
}
