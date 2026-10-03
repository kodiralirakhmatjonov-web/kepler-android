package com.iumrah.beta.core.auth

import android.app.Activity
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.iumrah.beta.models.account.IumrahGoogleCredential
import java.security.SecureRandom

object GoogleSignInSupport {
    const val SERVER_CLIENT_ID = "863185716777-8tec2kuch1qkra1f1coikpi20j6is65f.apps.googleusercontent.com"

    suspend fun signIn(activity: Activity): IumrahGoogleCredential {
        val nonce = randomNonce()
        val option = GetSignInWithGoogleOption.Builder(SERVER_CLIENT_ID)
            .setNonce(nonce)
            .build()
        val request = GetCredentialRequest.Builder()
            .addCredentialOption(option)
            .build()
        val result = CredentialManager.create(activity).getCredential(activity, request)
        val credential = result.credential
        if (credential !is CustomCredential || credential.type != GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
            error("Google did not return an identity token")
        }
        val google = GoogleIdTokenCredential.createFrom(credential.data)
        if (google.idToken.isBlank()) error("Google identity token is empty")
        return IumrahGoogleCredential(identityToken = google.idToken, nonce = nonce)
    }

    private fun randomNonce(length: Int = 32): String {
        val chars = "0123456789ABCDEFGHIJKLMNOPQRSTUVXYZabcdefghijklmnopqrstuvwxyz-._"
        val random = SecureRandom()
        return buildString(length) {
            repeat(length) { append(chars[random.nextInt(chars.length)]) }
        }
    }
}
