package com.pinwheel.platform

import com.pinwheel.core.plans.AuthAccount
import com.pinwheel.core.plans.AuthProvider

/** Explicit fake for P1 clients/tests. No token or claim of a verified Google account. */
class FakeAuthProvider(private val fakeAccount: AuthAccount = AuthAccount("fake-local", "Local test account")) : AuthProvider {
    override var account: AuthAccount? = null
        private set
    override fun signIn(): AuthAccount = fakeAccount.also { account = it }
    override fun signOut() { account = null }
}
