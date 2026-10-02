package com.pinwheel.platform

import com.pinwheel.core.plans.AuthAccount
import org.junit.jupiter.api.Test
import kotlin.test.*

class FakeAuthProviderTest {
    @Test fun fakeStartsSignedOutAndRetainsExplicitIdentityUntilSignOut() {
        val account = AuthAccount("fake-subject", "Tester"); val provider = FakeAuthProvider(account)
        assertNull(provider.account); assertEquals(account, provider.signIn()); assertEquals(account, provider.account)
        provider.signOut(); assertNull(provider.account)
    }
}
