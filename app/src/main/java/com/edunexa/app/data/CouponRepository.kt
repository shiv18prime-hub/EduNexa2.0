package com.edunexa.app.data

import com.google.firebase.firestore.FirebaseFirestore

class CouponRepository {
    private val db = FirebaseFirestore.getInstance()

    fun createCoupon(code: String, discount: Int, onResult: (Boolean, String) -> Unit) {
        val normalized = code.trim().uppercase()
        if (normalized.isBlank() || discount !in 1..100) {
            onResult(false, "Enter a valid code and discount from 1 to 100")
            return
        }
        db.collection("coupons").document(normalized)
            .set(Coupon(normalized, discount, true))
            .addOnSuccessListener { onResult(true, "$normalized created with $discount% discount") }
            .addOnFailureListener { onResult(false, it.message ?: "Coupon creation failed") }
    }

    fun validateCoupon(code: String, onResult: (Coupon?) -> Unit) {
        db.collection("coupons").document(code.trim().uppercase()).get()
            .addOnSuccessListener { doc ->
                val coupon = doc.toObject(Coupon::class.java)
                onResult(if (coupon?.active == true) coupon else null)
            }
            .addOnFailureListener { onResult(null) }
    }
}
