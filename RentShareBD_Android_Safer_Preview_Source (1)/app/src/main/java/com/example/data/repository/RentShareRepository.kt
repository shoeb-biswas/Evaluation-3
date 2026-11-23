package com.example.data.repository

import com.example.data.local.*
import com.example.BuildConfig
import com.example.domain.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import java.time.LocalDate

class RentShareRepository(
    private val db: AppDatabase,
    private val sessionDataStore: SessionDataStore
) {
    private val userDao = db.userDao()
    private val propDao = db.propertyDao()
    private val socialDao = db.socialAndRentalDao()
    private val finDao = db.financeAndLivingDao()

    val loggedInUserId: Flow<Long?> = sessionDataStore.loggedInUserId

    fun observeUsers(): Flow<List<UserEntity>> = userDao.observeAllUsers()
    fun observeTenantProfiles(): Flow<List<TenantProfileEntity>> = userDao.observeTenantProfiles()
    fun observeLandlordProfiles(): Flow<List<LandlordProfileEntity>> = userDao.observeLandlordProfiles()
    fun observePreferences(): Flow<List<RoommatePreferenceEntity>> = userDao.observeAllPreferences()
    fun observeProperties(): Flow<List<PropertyEntity>> = propDao.observeAllProperties()
    fun observeRooms(): Flow<List<RoomEntity>> = propDao.observeAllRooms()
    fun observeSaved(userId: Long): Flow<List<SavedPropertyEntity>> = propDao.observeSavedForUser(userId)
    fun observeRoommateRequests(): Flow<List<RoommateRequestEntity>> = socialDao.observeRoommateRequests()
    fun observeRentalRequests(): Flow<List<RentalRequestEntity>> = socialDao.observeRentalRequests()
    fun observeAgreements(): Flow<List<RentalAgreementEntity>> = socialDao.observeAgreements()
    fun observeGroups(): Flow<List<RoommateGroupEntity>> = socialDao.observeGroups()
    fun observeGroupMembers(): Flow<List<GroupMemberEntity>> = socialDao.observeGroupMembers()
    fun observeMessages(): Flow<List<MessageEntity>> = socialDao.observeMessages()

    suspend fun ensureSeeded() {
        // Sample properties and universal demo admin account must never be seeded in release.
        if (BuildConfig.DEBUG) DatabaseSeeder.seedIfEmpty(db)
    }

    suspend fun login(email: String, password: String): Result<UserEntity> {
        val user = userDao.getUserByEmail(email.trim())
            ?: return Result.failure(IllegalArgumentException("No account found for $email"))
        if (!BuildConfig.DEBUG && (user.role == "admin" || user.email.endsWith("@rentshare.bd"))) {
            return Result.failure(SecurityException("Local demo/admin accounts cannot sign in to a release build."))
        }
        if (user.isSuspended) {
            return Result.failure(IllegalStateException("This account has been suspended by an administrator."))
        }
        if (!LocalPasswordHasher.verify(password, user.passwordHash)) {
            return Result.failure(IllegalArgumentException("Invalid email or password."))
        }
        if (!LocalPasswordHasher.isHashed(user.passwordHash)) {
            userDao.updateUser(user.copy(passwordHash = LocalPasswordHasher.hash(password)))
        }
        sessionDataStore.setLoggedInUserId(user.id)
        return Result.success(user)
    }

    suspend fun loginDemoRole(role: UserRole): Result<UserEntity> {
        if (!BuildConfig.DEBUG) return Result.failure(SecurityException("Demo accounts are unavailable in release builds."))
        val email = when (role) {
            UserRole.TENANT -> "simi@rentshare.bd"
            UserRole.LANDLORD -> "karim.landlord@rentshare.bd"
            UserRole.ADMIN -> "admin@rentshare.bd"
        }
        val user = userDao.getUserByEmail(email)
            ?: return Result.failure(IllegalStateException("Demo account missing."))
        sessionDataStore.setLoggedInUserId(user.id)
        return Result.success(user)
    }

    suspend fun register(
        fullName: String,
        email: String,
        phone: String,
        password: String,
        role: String
    ): Result<UserEntity> {
        val cleanEmail = email.trim().lowercase()
        if (fullName.isBlank() || !cleanEmail.contains("@") || password.length < 8) {
            return Result.failure(IllegalArgumentException("Enter full name, valid email, and password (8+ characters)."))
        }
        if (userDao.getUserByEmail(cleanEmail) != null) {
            return Result.failure(IllegalStateException("An account with this email already exists."))
        }
        val safeRole = if (role == "landlord") "landlord" else "tenant"
        val userId = userDao.insertUser(
            UserEntity(
                email = cleanEmail,
                passwordHash = LocalPasswordHasher.hash(password),
                role = safeRole,
                fullName = fullName.trim(),
                phone = phone.trim(),
                emailVerified = false,
                phoneVerified = false // Entering a phone number is NOT identity verification.
            )
        )
        if (safeRole == "tenant") {
            userDao.insertTenantProfile(TenantProfileEntity(userId = userId))
            userDao.insertPreferences(RoommatePreferenceEntity(userId = userId))
        } else {
            userDao.insertLandlordProfile(LandlordProfileEntity(userId = userId))
        }
        finDao.insertNotification(
            NotificationEntity(
                userId = userId,
                type = "welcome",
                title = "Welcome to RentShare BD",
                message = "Verify your email and profile to boost your Trust Score."
            )
        )
        sessionDataStore.setLoggedInUserId(userId)
        return Result.success(userDao.getUserById(userId)!!)
    }

    suspend fun logout() {
        sessionDataStore.setLoggedInUserId(null)
    }

    suspend fun verifyEmailForUser(userId: Long) {
        // No email token/OTP integration is available in this local prototype.
        // Never mark a user verified on tap alone.
    }

    suspend fun resetPasswordDemo(email: String, newPassword: String): Result<Unit> {
        return Result.failure(UnsupportedOperationException(
            "Password recovery is disabled until verified email authentication is connected."
        ))
    }

    suspend fun changePassword(userId: Long, current: String, next: String): Result<Unit> {
        val u = userDao.getUserById(userId) ?: return Result.failure(IllegalStateException("User not found."))
        if (!LocalPasswordHasher.verify(current, u.passwordHash)) return Result.failure(IllegalArgumentException("Current password is incorrect."))
        if (next.length < 8) return Result.failure(IllegalArgumentException("New password must be 8+ characters."))
        userDao.updateUser(u.copy(passwordHash = LocalPasswordHasher.hash(next)))
        return Result.success(Unit)
    }

    suspend fun updateTenantProfileAndPrefs(
        user: UserEntity,
        profile: TenantProfileEntity,
        prefs: RoommatePreferenceEntity
    ) {
        userDao.updateUser(user)
        userDao.insertTenantProfile(profile)
        userDao.insertPreferences(prefs)
    }

    suspend fun submitVerification(userId: Long, type: String, documentRef: String) {
        if (documentRef.isBlank()) return
        finDao.insertVerification(
            VerificationRecordEntity(
                userId = userId,
                verificationType = type,
                documentRef = documentRef.trim(),
                status = "pending",
                notes = "Submitted via mobile app"
            )
        )
        // Document submission is a REQUEST for verification, never verification itself.
    }

    suspend fun toggleSaveProperty(userId: Long, propertyId: Long) {
        val existing = propDao.getSaved(userId, propertyId)
        if (existing != null) propDao.deleteSaved(userId, propertyId)
        else propDao.insertSaved(SavedPropertyEntity(userId = userId, propertyId = propertyId))
    }

    suspend fun saveOrUpdateProperty(
        existingId: Long?,
        landlordId: Long,
        title: String,
        description: String,
        area: String,
        address: String,
        roomType: String,
        totalRooms: Int,
        monthlyRent: Int,
        advanceAmount: Int,
        serviceCharge: Int,
        electricity: Int,
        water: Int,
        gas: Int,
        internet: Int,
        otherCharges: Int,
        availableFrom: String,
        furnished: Boolean,
        attachedBath: Boolean,
        hasBalcony: Boolean,
        hasWifi: Boolean,
        hasParking: Boolean,
        hasGenerator: Boolean,
        imageKeyOrUri: String,
        facilitiesCsv: String
    ): Long {
        val allProps = propDao.getAllProperties()
        val areaRents = allProps.filter { it.area.equals(area, ignoreCase = true) && it.id != existingId }.map { it.monthlyRent }.sorted()
        val medianRent = if (areaRents.isNotEmpty()) areaRents[areaRents.size / 2] else 9000
        val duplicateCount = allProps.count {
            it.id != existingId &&
                it.title.trim().equals(title.trim(), ignoreCase = true) &&
                it.area.equals(area, ignoreCase = true)
        }
        val landlord = userDao.getUserById(landlordId)
        val scam = DomainLogic.detectSuspiciousListing(
            ScamDetectionInput(
                monthlyRent = monthlyRent,
                advanceAmount = advanceAmount,
                areaMedianRent = medianRent,
                landlordVerified = landlord?.identityVerified == true,
                duplicateListingCount = duplicateCount,
                complaintCount = 0,
                accountAgeDays = 30
            )
        )
        val coords = coordsForArea(area)
        val prev = existingId?.let { propDao.getPropertyById(it) }
        val entity = PropertyEntity(
            id = existingId ?: 0L,
            landlordId = landlordId,
            title = title.trim(),
            description = description.trim(),
            area = area,
            address = address.trim(),
            latitude = prev?.latitude ?: coords.first,
            longitude = prev?.longitude ?: coords.second,
            roomType = roomType,
            totalRooms = totalRooms.coerceAtLeast(1),
            furnished = furnished,
            attachedBath = attachedBath,
            hasBalcony = hasBalcony,
            hasWifi = hasWifi,
            hasParking = hasParking,
            hasGenerator = hasGenerator,
            monthlyRent = monthlyRent,
            advanceAmount = advanceAmount,
            serviceCharge = serviceCharge,
            estimatedElectricity = electricity,
            estimatedWater = water,
            estimatedGas = gas,
            estimatedInternet = internet,
            otherRecurringCharges = otherCharges,
            availableFrom = availableFrom.ifBlank { LocalDate.now().plusDays(14).toString() },
            availabilityStatus = if (scam.suspicious) "pending_verification" else (prev?.availabilityStatus ?: "available"),
            verificationStatus = "pending", // Only server-authorized admin review can approve a public listing.
            isSuspicious = scam.suspicious,
            suspiciousReason = scam.reasons.joinToString("; "),
            primaryImageKey = imageKeyOrUri.ifBlank { defaultImageForArea(area) },
            facilitiesCsv = facilitiesCsv
        )
        val id = propDao.insertProperty(entity)
        if (existingId == null) {
            for (idx in 1..totalRooms.coerceIn(1, 5)) {
                propDao.insertRoom(
                    RoomEntity(
                        propertyId = id,
                        roomLabel = "Room ${'A' + idx - 1}",
                        roomType = roomType,
                        monthlyRent = monthlyRent,
                        isAvailable = true,
                        availableFrom = entity.availableFrom
                    )
                )
            }
        }
        return id
    }

    suspend fun toggleRoomAvailability(room: RoomEntity) {
        propDao.updateRoom(room.copy(isAvailable = !room.isAvailable))
    }

    suspend fun sendRoommateRequest(senderId: Long, receiverId: Long, message: String) {
        socialDao.insertRoommateRequest(
            RoommateRequestEntity(senderId = senderId, receiverId = receiverId, message = message.trim())
        )
        finDao.insertNotification(
            NotificationEntity(
                userId = receiverId,
                type = "roommate",
                title = "New Roommate Request",
                message = "You received a new roommate connection request."
            )
        )
    }

    suspend fun respondRoommateRequest(req: RoommateRequestEntity, newStatus: String) {
        socialDao.updateRoommateRequest(req.copy(status = newStatus))
        finDao.insertNotification(
            NotificationEntity(
                userId = req.senderId,
                type = "roommate",
                title = "Roommate Request ${newStatus.replaceFirstChar { it.uppercase() }}",
                message = "Your roommate connection request was $newStatus."
            )
        )
    }

    suspend fun sendRentalRequest(property: PropertyEntity, tenantId: Long, moveInDate: String, durationMonths: Int, message: String) {
        socialDao.insertRentalRequest(
            RentalRequestEntity(
                propertyId = property.id,
                tenantId = tenantId,
                moveInDate = moveInDate,
                durationMonths = durationMonths,
                message = message.trim()
            )
        )
        finDao.insertNotification(
            NotificationEntity(
                userId = property.landlordId,
                type = "rental",
                title = "New Rental Application",
                message = "A tenant applied for ${property.title}."
            )
        )
    }

    suspend fun updateRentalRequestStatus(req: RentalRequestEntity, newStatus: String) {
        socialDao.updateRentalRequest(req.copy(status = newStatus))
        val property = propDao.getPropertyById(req.propertyId) ?: return
        if (newStatus == "accepted") {
            val start = runCatching { LocalDate.parse(req.moveInDate) }.getOrElse { LocalDate.now().plusDays(7) }
            val end = start.plusMonths(req.durationMonths.toLong()).minusDays(1)
            socialDao.insertAgreement(
                RentalAgreementEntity(
                    propertyId = property.id,
                    tenantId = req.tenantId,
                    landlordId = property.landlordId,
                    monthlyRent = property.monthlyRent,
                    advanceAmount = property.advanceAmount,
                    durationMonths = req.durationMonths,
                    noticePeriodDays = 30,
                    utilityTerms = "Electricity, Gas, Water, and Internet split equally among flatmates.",
                    houseRules = "Quiet hours after 11:00 PM. No indoor smoking. Prior notice for overnight guests.",
                    startDate = start.toString(),
                    endDate = end.toString(),
                    tenantAccepted = false,
                    landlordAccepted = true,
                    status = "pending_tenant"
                )
            )
            val existingGroup = socialDao.observeGroups().firstOrNull()?.firstOrNull { it.propertyId == property.id }
            val groupId = existingGroup?.id ?: socialDao.insertGroup(
                RoommateGroupEntity(
                    name = "${property.area} Shared Flat #${property.id}",
                    propertyId = property.id,
                    createdBy = req.tenantId
                )
            )
            socialDao.insertGroupMember(GroupMemberEntity(groupId = groupId, userId = req.tenantId, role = "admin"))
        }
        finDao.insertNotification(
            NotificationEntity(
                userId = req.tenantId,
                type = "rental",
                title = "Application ${newStatus.replaceFirstChar { it.uppercase() }}",
                message = "Your application for ${property.title} was $newStatus."
            )
        )
    }

    suspend fun acceptRentalAgreement(agreement: RentalAgreementEntity) {
        socialDao.updateAgreement(
            agreement.copy(
                tenantAccepted = true,
                tenantAcceptedAt = LocalDate.now().toString(),
                status = "active"
            )
        )
        finDao.insertNotification(
            NotificationEntity(
                userId = agreement.landlordId,
                type = "agreement",
                title = "Agreement Signed",
                message = "Digital rental agreement #AG-${agreement.id} is now active."
            )
        )
    }

    suspend fun createGroup(name: String, creatorId: Long, memberIds: List<Long>) {
        if (name.isBlank()) return
        val gid = socialDao.insertGroup(RoommateGroupEntity(name = name.trim(), createdBy = creatorId))
        socialDao.insertGroupMember(GroupMemberEntity(groupId = gid, userId = creatorId, role = "admin"))
        memberIds.distinct().filter { it != creatorId }.forEach { uid ->
            socialDao.insertGroupMember(GroupMemberEntity(groupId = gid, userId = uid, role = "member"))
        }
    }

    suspend fun addMemberToGroup(groupId: Long, userId: Long) {
        socialDao.insertGroupMember(GroupMemberEntity(groupId = groupId, userId = userId, role = "member"))
    }

    suspend fun removeMemberFromGroup(groupId: Long, userId: Long) {
        socialDao.deleteGroupMember(groupId, userId)
    }

    suspend fun sendGroupMessage(groupId: Long, senderId: Long, content: String) {
        if (content.isBlank()) return
        socialDao.insertMessage(
            MessageEntity(groupId = groupId, senderId = senderId, content = content.trim(), createdAtLabel = "Just now")
        )
    }

    suspend fun sendPrivateMessage(senderId: Long, receiverId: Long, content: String) {
        if (content.isBlank()) return
        socialDao.insertMessage(
            MessageEntity(senderId = senderId, receiverId = receiverId, content = content.trim(), createdAtLabel = "Just now")
        )
    }

    suspend fun fileComplaint(reporterId: Long, targetUserId: Long?, propertyId: Long?, reason: String, details: String) {
        finDao.insertComplaint(
            ComplaintEntity(
                reporterId = reporterId,
                targetUserId = targetUserId,
                propertyId = propertyId,
                reason = reason.trim(),
                details = details.trim()
            )
        )
    }

    // Admin actions
    suspend fun toggleUserSuspended(user: UserEntity) {
        userDao.updateUser(user.copy(isSuspended = !user.isSuspended))
    }

    suspend fun reviewVerification(record: VerificationRecordEntity, adminId: Long, approve: Boolean) {
        finDao.updateVerification(
            record.copy(
                status = if (approve) "approved" else "rejected",
                reviewedBy = adminId,
                notes = if (approve) "Verified by admin" else "Rejected by admin"
            )
        )
        if (approve) {
            userDao.getUserById(record.userId)?.let { u ->
                userDao.updateUser(u.copy(identityVerified = true))
            }
            userDao.getLandlordProfile(record.userId)?.let { lp ->
                userDao.insertLandlordProfile(lp.copy(verificationStatus = "verified"))
            }
        }
    }

    suspend fun reviewProperty(property: PropertyEntity, status: String, clearSuspicious: Boolean) {
        propDao.updateProperty(
            property.copy(
                verificationStatus = status,
                availabilityStatus = if (status == "approved") "available" else "suspended",
                isSuspicious = if (clearSuspicious) false else property.isSuspicious
            )
        )
    }

    suspend fun resolveComplaint(complaint: ComplaintEntity, status: String) {
        finDao.updateComplaint(complaint.copy(status = status))
    }

    private fun coordsForArea(area: String): Pair<Double, Double> = when (area) {
        "Mirpur" -> 23.8069 to 90.3687
        "Uttara" -> 23.8759 to 90.3795
        "Dhanmondi" -> 23.7461 to 90.3742
        "Mohammadpur" -> 23.7654 to 90.3588
        "Bashundhara" -> 23.8191 to 90.4526
        "Savar" -> 23.8583 to 90.2667
        "Gazipur" -> 23.9999 to 90.4203
        else -> 23.8103 to 90.4125
    }

    private fun defaultImageForArea(area: String): String = when (area) {
        "Uttara" -> "img_prop_uttara"
        "Dhanmondi" -> "img_prop_dhanmondi"
        "Bashundhara" -> "img_prop_bashundhara"
        else -> "img_prop_mirpur"
    }
}
