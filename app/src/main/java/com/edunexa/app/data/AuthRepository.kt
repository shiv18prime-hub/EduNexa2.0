package com.edunexa.app.data

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class AuthRepository {
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    fun register(
        name: String,
        email: String,
        password: String,
        role: String,
        schoolName: String = "",
        className: String = "",
        contactNumber: String = "",
        address: String = "",
        onResult: (Boolean, String) -> Unit
    ) {
        auth.createUserWithEmailAndPassword(email, password)
            .addOnSuccessListener { result ->
                val uid = result.user?.uid ?: return@addOnSuccessListener onResult(false, "Account error")
                val profile = UserProfile(
                    uid = uid,
                    name = name,
                    email = email,
                    role = role,
                    schoolName = schoolName,
                    className = className,
                    contactNumber = contactNumber,
                    address = address,
                    schoolApproved = role != "school"
                )
                db.collection("users").document(uid).set(profile)
                    .addOnSuccessListener { onResult(true, if (role == "school") "School account submitted for approval" else "Student account created") }
                    .addOnFailureListener { onResult(false, it.message ?: "Profile save failed") }
            }
            .addOnFailureListener { onResult(false, it.message ?: "Registration failed") }
    }

    fun login(email: String, password: String, onResult: (Boolean, String) -> Unit) {
        auth.signInWithEmailAndPassword(email, password)
            .addOnSuccessListener { onResult(true, "Login successful") }
            .addOnFailureListener { onResult(false, it.message ?: "Login failed") }
    }

    fun signOut() = auth.signOut()
}
