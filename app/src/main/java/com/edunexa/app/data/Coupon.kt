package com.edunexa.app.data

data class Coupon(
    val code: String = "",
    val discountPercent: Int = 0,
    val active: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)
