package com.iumrah.beta.data.account

object IumrahAccountRoutes {
    const val ACTIVATE = "/api/catalog/hotels/client/account/activate"
    const val LOGIN = "/api/package/client/account/login"
    const val SESSION = "/api/catalog/hotels/client/account/session"
    const val LOGOUT = "/api/catalog/hotels/client/account/logout"
    const val PROFILE = "/api/catalog/hotels/client/account/profile"
    const val TRIPS = "/api/catalog/hotels/client/trips"
    const val LINK_BOOKING = "/api/catalog/hotels/client/account/link-booking"
    const val SECURITY_REGISTER = "/api/package/client/account/security/register"
    const val SECURITY = "/api/package/client/account/security"
    const val SECURITY_CLAIM_PRIMARY = "/api/package/client/account/security/claim-primary"
    const val FRIENDS = "/api/package/client/account/friends"
    const val APPLE_LINK = "/api/package/client/account/apple/link"
    const val APPLE_SIGN_IN = "/api/package/client/account/apple/sign-in"
    const val EMAIL_START = "/api/package/client/account/email/start"
    const val EMAIL_CONFIRM = "/api/package/client/account/email/confirm"
    const val PASSWORD_RECOVERY_START = "/api/package/client/account/password/recovery/start"
    const val PASSWORD_RECOVERY_CONFIRM = "/api/package/client/account/password/recovery/confirm"

    fun tripDetail(bookingID: String) = "/api/catalog/hotels/client/trips/$bookingID"
    fun checkout(bookingID: String) = "/api/catalog/hotels/client/trips/$bookingID/checkout"
    fun traveler(bookingID: String, position: Int) = "/api/catalog/hotels/client/trips/$bookingID/travelers/$position"
    fun travelerPassport(bookingID: String, position: Int) = traveler(bookingID, position) + "/passport"
    fun receipt(bookingID: String) = "/api/catalog/hotels/client/trips/$bookingID/receipt"
    fun terminateSession(id: String) = "/api/package/client/account/security/sessions/$id"
}
