package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.crypto.CryptoEngine
import com.example.mesh.model.MeshPacket
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("MeshPulse", appName)
    }

    @Test
    fun `crypto engine generates permanent user id in valid format`() {
        val userId = CryptoEngine.generatePermanentUserId()
        assertTrue(userId.startsWith("MP-"))
        assertEquals(12, userId.length) // MP-XXXX-XXXX
    }

    @Test
    fun `crypto engine ecdh and aes gcm end to end encryption and decryption`() {
        val aliceKeys = CryptoEngine.generateIdentityKeyPair()
        val bobKeys = CryptoEngine.generateIdentityKeyPair()

        val secretMessage = "Meeting at the offline mesh relay node at 5 PM"

        // Alice encrypts for Bob using Bob's public key
        val encryptedPayload = CryptoEngine.encryptForRecipient(secretMessage, bobKeys.publicKeyBase64)

        // Bob decrypts with his private key and Alice's ephemeral key
        val decryptedMessage = CryptoEngine.decryptFromSender(encryptedPayload, bobKeys.privateKeyBase64)

        assertEquals(secretMessage, decryptedMessage)
    }

    @Test
    fun `mesh packet serialization and hop tracking`() {
        val packet = MeshPacket(
            packetId = "MSG-1001-2002-3003",
            type = MeshPacket.PacketType.CHAT_MESSAGE,
            senderUserId = "MP-1111-2222",
            recipientUserId = "MP-3333-4444",
            encryptedPayload = "sample_ciphertext",
            senderPublicKey = "sample_pubkey",
            timestamp = System.currentTimeMillis(),
            ttl = 5,
            hopCount = 0
        )

        // Serialize and Deserialize
        val json = packet.toJson()
        val parsed = MeshPacket.fromJson(json)
        assertNotNull(parsed)
        assertEquals(packet.packetId, parsed?.packetId)
        assertEquals(5, parsed?.ttl)

        // Traversed hop
        val hopped = packet.withHop("NODE-RELAY-1")
        assertEquals(4, hopped.ttl)
        assertEquals(1, hopped.hopCount)
        assertTrue(hopped.visitedNodeIds.contains("NODE-RELAY-1"))
    }
}
