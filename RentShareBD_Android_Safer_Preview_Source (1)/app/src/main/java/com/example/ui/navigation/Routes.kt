package com.example.ui.navigation

import kotlinx.serialization.Serializable

@Serializable data object HomeRoute
@Serializable data object AboutRoute
@Serializable data object HowItWorksRoute
@Serializable data object MarketplaceRoute
@Serializable data class PropertyDetailsRoute(val propertyId: Long)
@Serializable data object RoommatesRoute
@Serializable data class AuthRoute(val initialMode: String = "login")

@Serializable data object AssistantRoute
@Serializable data object WalletPreviewRoute
@Serializable data object WorkspaceHomeRoute

// Tenant routes
@Serializable data object TenantProfileRoute
@Serializable data object TenantMatchesRoute
@Serializable data object TenantRentalRequestsRoute
@Serializable data object TenantSavedRoute
@Serializable data object TenantAgreementsRoute
@Serializable data object TenantGroupRoute
@Serializable data object TenantGroupChatRoute
@Serializable data class TenantPrivateChatRoute(val peerId: Long? = null)
@Serializable data object TenantExpensesRoute
@Serializable data object TenantSplitsRoute
@Serializable data object TenantUtilitiesRoute
@Serializable data object TenantMealsRoute
@Serializable data object TenantGroceriesRoute
@Serializable data object TenantMaintenanceRoute
@Serializable data object TenantAiCoachRoute
@Serializable data object TenantNotificationsRoute
@Serializable data object TenantSettingsRoute

// Landlord routes
@Serializable data object LandlordPropertiesRoute
@Serializable data class LandlordPropertyEditorRoute(val propertyId: Long? = null)
@Serializable data object LandlordApplicationsRoute
@Serializable data object LandlordTenantsRoute
@Serializable data object LandlordAgreementsRoute
@Serializable data object LandlordMaintenanceRoute
@Serializable data object LandlordReviewsRoute

// Admin routes
@Serializable data object AdminUsersRoute
@Serializable data object AdminVerificationsRoute
@Serializable data object AdminSuspiciousRoute
@Serializable data object AdminComplaintsRoute
@Serializable data object AdminReportsRoute
