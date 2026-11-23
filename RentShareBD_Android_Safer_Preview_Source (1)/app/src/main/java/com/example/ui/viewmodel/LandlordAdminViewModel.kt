package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.RentShareContainer
import com.example.data.local.*
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

class LandlordAdminViewModel(private val container: RentShareContainer) : ViewModel() {

    private val repo = container.repository
    private val finRepo = container.financeRepository

    fun saveProperty(
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
        facilitiesCsv: String,
        onDone: (Long) -> Unit
    ) {
        viewModelScope.launch {
            val id = repo.saveOrUpdateProperty(
                existingId = existingId,
                landlordId = landlordId,
                title = title,
                description = description,
                area = area,
                address = address,
                roomType = roomType,
                totalRooms = totalRooms,
                monthlyRent = monthlyRent,
                advanceAmount = advanceAmount,
                serviceCharge = serviceCharge,
                electricity = electricity,
                water = water,
                gas = gas,
                internet = internet,
                otherCharges = otherCharges,
                availableFrom = availableFrom,
                furnished = furnished,
                attachedBath = attachedBath,
                hasBalcony = hasBalcony,
                hasWifi = hasWifi,
                hasParking = hasParking,
                hasGenerator = hasGenerator,
                imageKeyOrUri = imageKeyOrUri,
                facilitiesCsv = facilitiesCsv
            )
            onDone(id)
        }
    }

    fun toggleRoom(room: RoomEntity) {
        viewModelScope.launch { repo.toggleRoomAvailability(room) }
    }

    fun updateRentalApplication(req: RentalRequestEntity, status: String, onFeedback: (String) -> Unit) {
        viewModelScope.launch {
            repo.updateRentalRequestStatus(req, status)
            onFeedback("Application marked as $status.")
        }
    }

    fun updateMaintenance(req: MaintenanceRequestEntity, status: String, response: String, onFeedback: (String) -> Unit) {
        viewModelScope.launch {
            finRepo.updateMaintenanceStatus(req, status, response)
            onFeedback("Ticket updated to $status.")
        }
    }

    fun toggleUserSuspended(user: UserEntity, onFeedback: (String) -> Unit) {
        viewModelScope.launch {
            repo.toggleUserSuspended(user)
            onFeedback(if (user.isSuspended) "User unsuspended." else "User suspended.")
        }
    }

    fun reviewVerification(record: VerificationRecordEntity, adminId: Long, approve: Boolean, onFeedback: (String) -> Unit) {
        viewModelScope.launch {
            repo.reviewVerification(record, adminId, approve)
            onFeedback(if (approve) "Verification approved." else "Verification rejected.")
        }
    }

    fun reviewProperty(property: PropertyEntity, status: String, clearSuspicious: Boolean, onFeedback: (String) -> Unit) {
        viewModelScope.launch {
            repo.reviewProperty(property, status, clearSuspicious)
            onFeedback("Listing updated to $status.")
        }
    }

    fun resolveComplaint(complaint: ComplaintEntity, status: String, onFeedback: (String) -> Unit) {
        viewModelScope.launch {
            repo.resolveComplaint(complaint, status)
            onFeedback("Complaint marked $status.")
        }
    }

    fun buildPlatformReportJson(snapshot: AppSnapshot): String {
        val root = JSONObject()
        root.put("platform", "RentShare BD Android")
        root.put("totalUsers", snapshot.users.size)
        root.put("totalProperties", snapshot.properties.size)
        root.put("suspiciousProperties", snapshot.properties.count { it.isSuspicious })
        root.put("activeAgreements", snapshot.agreements.count { it.status == "active" })
        root.put("openComplaints", snapshot.complaints.count { it.status == "open" })
        val areaBreakdown = JSONArray()
        snapshot.properties.groupBy { it.area }.forEach { (area, props) ->
            val obj = JSONObject()
            obj.put("area", area)
            obj.put("listings", props.size)
            obj.put("avgMonthlyRentBdt", props.map { it.monthlyRent }.average().toInt())
            areaBreakdown.put(obj)
        }
        root.put("areas", areaBreakdown)
        return root.toString(2)
    }
}
