package com.edunexa.app.data

import com.google.firebase.functions.FirebaseFunctions

object Backend {
    private val functions = FirebaseFunctions.getInstance("asia-south1")
    fun call(name: String, data: Map<String, Any> = emptyMap(), success: (Map<String, Any?>) -> Unit, failure: (String) -> Unit) {
        functions.getHttpsCallable(name).call(data).addOnSuccessListener { response ->
            val raw = response.data as? Map<*, *> ?: emptyMap<Any, Any>()
            success(raw.entries.associate { it.key.toString() to it.value })
        }.addOnFailureListener { failure(it.localizedMessage ?: "This service is temporarily unavailable. Try again.") }
    }
    fun rows(value: Any?): List<Map<String, Any>> = (value as? List<*>)?.mapNotNull { row ->
        (row as? Map<*, *>)?.entries?.filter { it.value != null }?.associate { it.key.toString() to it.value!! }
    } ?: emptyList()
}
