package com.edunexa.app.core

data class SubjectMarks(val name: String, val max: Int, val obtained: Int, val passMarks: Int, val optional: Boolean = false) {
    fun asMap(): Map<String, Any> = mapOf("name" to name, "max" to max, "obtained" to obtained, "passMarks" to passMarks, "optional" to optional)
}
data class CalculatedResult(val total: Int, val obtained: Int, val percentage: Double, val grade: String, val passed: Boolean)
object MarksCalculator {
    fun grade(p: Double): String = when { p >= 90 -> "A+"; p >= 80 -> "A"; p >= 70 -> "B+"; p >= 60 -> "B"; p >= 50 -> "C"; p >= 33 -> "D"; else -> "F" }
    fun calculate(subjects: List<SubjectMarks>): CalculatedResult {
        require(subjects.isNotEmpty() && subjects.size <= 30) { "Add 1–30 subjects" }
        require(subjects.map { it.name.trim().lowercase() }.distinct().size == subjects.size) { "Use different subject names" }
        subjects.forEach { require(it.name.isNotBlank() && it.max in 1..10000 && it.obtained in 0..it.max && it.passMarks in 0..it.max) { "Enter valid marks for every subject" } }
        val main = subjects.filterNot { it.optional }; require(main.isNotEmpty()) { "At least one main subject is required" }
        val total = main.sumOf { it.max }; val got = main.sumOf { it.obtained }; val p = got * 100.0 / total
        return CalculatedResult(total, got, p, grade(p), main.all { it.obtained >= it.passMarks })
    }
}
