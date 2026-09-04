package com.iumrah.beta.models.account

import kotlinx.serialization.Serializable

@Serializable
data class IumrahFriendGift(
    val id: String,
    val code: String,
    val position: Int,
    val status: String,
    val redeemedBookingID: String? = null,
    val rewardStatus: String? = null,
    val rewardUsd: Double,
    val discountUsd: Double,
    val createdAt: String,
    val redeemedAt: String? = null,
) {
    val isAvailable: Boolean get() = status.lowercase() == "available"
    val isRewardPending: Boolean get() = rewardStatus?.lowercase() == "pending"
    val isRewardEarned: Boolean get() = rewardStatus?.lowercase() == "earned"
}

@Serializable
data class IumrahFriendsDashboard(
    val ok: Boolean,
    val availableCreditUsd: Double,
    val pendingRewardsUsd: Double,
    val earnedRewardsUsd: Double,
    val gifts: List<IumrahFriendGift>,
)

@Serializable
data class IumrahFriendsAppliedGift(val code: String, val discountUsd: Double, val rewardStatus: String) {
    val id: String get() = code
}

@Serializable
data class IumrahFriendsBookingSummary(
    val ok: Boolean,
    val bookingID: String,
    val identityConfirmed: Boolean,
    val totalUsd: Double,
    val maxDiscountUsd: Double,
    val giftDiscountUsd: Double,
    val creditAppliedUsd: Double,
    val totalDiscountUsd: Double,
    val remainingAllowanceUsd: Double,
    val payableUsd: Double,
    val availableCreditUsd: Double,
    val appliedGifts: List<IumrahFriendsAppliedGift>,
)

@Serializable data class IumrahFriendGiftRedeemRequest(val code: String)
@Serializable data class IumrahFriendCreditApplyRequest(val amountUsd: Int)
