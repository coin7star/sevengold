package com.sevengold.signalapp.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.sevengold.signalapp.data.model.AppUser
import com.sevengold.signalapp.data.model.Role
import kotlinx.coroutines.tasks.await

class AuthRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    val currentUid: String? get() = auth.currentUser?.uid

    suspend fun register(email: String, password: String): Result<String> = runCatching {
        val result = auth.createUserWithEmailAndPassword(email, password).await()
        val uid = result.user?.uid ?: error("Gagal membuat akun")

        // Setiap akun baru otomatis dibuatkan profil dengan role USER
        val newUser = AppUser(uid = uid, email = email, role = Role.USER)
        db.collection("users").document(uid).set(newUser.toMap()).await()
        uid
    }

    suspend fun login(email: String, password: String): Result<String> = runCatching {
        val result = auth.signInWithEmailAndPassword(email, password).await()
        result.user?.uid ?: error("Gagal login")
    }

    fun logout() = auth.signOut()
}
