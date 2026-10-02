package com.example.data.firebase

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class FirebaseIdentityService(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    suspend fun createAccount(email: String, password: String): String {
        require(email.trim().isNotEmpty()) { "Email is required" }
        require(password.length >= 6) { "Password must be at least 6 characters" }
        return auth.createUserWithEmailAndPassword(email.trim(), password).await().user?.uid
            ?: error("Firebase account creation failed")
    }

    suspend fun login(email: String, password: String): String {
        return auth.signInWithEmailAndPassword(email.trim(), password).await().user?.uid
            ?: error("Firebase login failed")
    }

    suspend fun resetPassword(email: String) {
        auth.sendPasswordResetEmail(email.trim()).await()
    }

    fun logout() {
        auth.signOut()
    }

    suspend fun ensureSignedIn(): String {
        val current = auth.currentUser
        return current?.uid ?: error("Please login with your email first")
    }

    suspend fun reserveUsername(username: String, displayName: String, photoUrl: String? = null): Boolean {
        val normalized = username.trim().lowercase().replace(Regex("[^a-z0-9_.]"), "")
        require(normalized.length in 3..30) { "Username must be 3-30 characters" }
        val uid = ensureSignedIn()
        val ref = db.collection("usernames").document(normalized)
        return db.runTransaction { tx ->
            val existing = tx.get(ref)
            if (existing.exists() && existing.getString("uid") != uid) {
                false
            } else {
                tx.set(ref, mapOf("uid" to uid, "username" to normalized, "updatedAt" to FieldValue.serverTimestamp()))
                tx.set(
                    db.collection("users").document(uid),
                    mapOf("uid" to uid, "username" to normalized, "displayName" to displayName, "photoUrl" to photoUrl, "updatedAt" to FieldValue.serverTimestamp()),
                    com.google.firebase.firestore.SetOptions.merge()
                )
                true
            }
        }.await()
    }

    suspend fun getProfile(uid: String): Map<String, Any>? {
        return db.collection("users").document(uid).get().await().data
    }

    suspend fun findUserByUsername(username: String): Map<String, Any>? {
        val normalized = username.trim().lowercase()
        val reservation = db.collection("usernames").document(normalized).get().await()
        val uid = reservation.getString("uid") ?: return null
        return db.collection("users").document(uid).get().await().data
    }
}
