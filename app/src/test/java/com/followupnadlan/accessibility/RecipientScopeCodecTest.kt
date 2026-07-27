package com.followupnadlan.accessibility

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * The pure codec the nullable per-moment scope stores delegate to. `null` (= "כמו הכללי" / follow
 * general) round-trips to an absent stored value; a concrete scope round-trips through its enum
 * name. This is where "an untouched moment follows general" comes from: no stored value decodes to
 * null. Pins the null-handling without needing an Android Context.
 */
class RecipientScopeCodecTest {

    @Test
    fun nullEncodesToNull() {
        assertNull(RecipientScopeCodec.encode(null))
    }

    @Test
    fun absentStorageDecodesToNullFollowGeneral() {
        assertNull(RecipientScopeCodec.decode(null))
    }

    @Test
    fun unrecognizedStoredValueDecodesToNull() {
        // A stale/garbage value must degrade to "follow general", never crash.
        assertNull(RecipientScopeCodec.decode("SOMETHING_ELSE"))
    }

    @Test
    fun everyScopeRoundTrips() {
        RecipientScope.entries.forEach { scope ->
            val encoded = RecipientScopeCodec.encode(scope)
            assertEquals(scope.name, encoded)
            assertEquals(scope, RecipientScopeCodec.decode(encoded))
        }
    }
}
