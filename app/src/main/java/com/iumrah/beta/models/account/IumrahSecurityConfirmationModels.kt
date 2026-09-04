package com.iumrah.beta.models.account

import kotlinx.serialization.Serializable

@Serializable
data class IumrahSecurityConfirmation(
    val bookingID: String,
    val status: String,
    val firstName: String,
    val lastName: String,
    val passportLast4: String,
    val verificationMethod: String,
    val reusedIdentity: Boolean,
    val hasPassportPhoto: Boolean,
    val reviewNote: String,
    val submittedAt: String,
    val updatedAt: String,
) {
    val normalizedStatus: String get() = status.lowercase()
    val isConfirmed: Boolean get() = normalizedStatus == "confirmed"
    val isPendingReview: Boolean get() = normalizedStatus in setOf("submitted", "under_review")
    val needsResubmission: Boolean get() = normalizedStatus in setOf("rejected", "needs_resubmission")
    val isDraft: Boolean get() = normalizedStatus == "draft"
    val isSubmitted: Boolean get() = isPendingReview || isConfirmed
    val canEdit: Boolean get() = !isConfirmed && !isPendingReview
}

@Serializable data class IumrahSecurityConfirmationResponse(val ok: Boolean, val confirmation: IumrahSecurityConfirmation? = null)
@Serializable data class IumrahSecurityConfirmationRequest(val firstName: String, val lastName: String, val passportNumber: String, val holderConfirmed: Boolean)
