package com.edunexa.app.core

import java.text.SimpleDateFormat
import java.util.Locale

/** Payloads shared by the Android school forms; Firestore rules enforce authority. */
object SchoolRecords {
    fun result(schoolId: String, issuerUid: String, studentId: String, studentName: String,
               exam: String, subject: String, obtained: Double, total: Double, now: Long): Map<String, Any> {
        require(obtained.isFinite() && total.isFinite() && obtained % 1.0 == 0.0 && total % 1.0 == 0.0) { "Enter whole-number marks" }
        require(total in 1.0..10000.0 && obtained in 0.0..total) { "Marks must be between zero and the total (maximum 10,000)" }
        val name = subject.trim()
        require(name.length in 1..80 && exam.trim().length in 1..120) { "Enter an exam (up to 120 characters) and subject (up to 80)" }
        val pass = kotlin.math.ceil(total * 0.33).toInt()
        val score = MarksCalculator.calculate(listOf(SubjectMarks(name, total.toInt(), obtained.toInt(), pass)))
        return mapOf("schoolId" to schoolId, "issuerUid" to issuerUid, "studentId" to studentId,
            "studentName" to studentName, "exam" to exam.trim(), "subject" to name,
            "obtained" to score.obtained, "total" to score.total, "passMarks" to pass,
            "percentage" to score.percentage, "grade" to score.grade, "result" to if (score.passed) "PASS" else "FAIL",
            "published" to true, "createdAt" to now)
    }

    fun attendance(schoolId: String, issuerUid: String, studentId: String, studentName: String,
                   date: String, present: Boolean, now: Long): Map<String, Any> {
        require(Regex("[0-9]{4}-[0-9]{2}-[0-9]{2}").matches(date)) { "Use a valid date in YYYY-MM-DD format" }
        require(runCatching { SimpleDateFormat("yyyy-MM-dd", Locale.US).apply { isLenient = false }.parse(date) }.getOrNull() != null) { "Enter a real calendar date" }
        return mapOf("schoolId" to schoolId, "issuerUid" to issuerUid, "studentId" to studentId,
            "studentName" to studentName, "date" to date, "present" to present, "updatedAt" to now)
    }

    fun notice(schoolId: String, issuerUid: String, title: String, message: String,
               targetClass: String, important: Boolean, now: Long): Map<String, Any> {
        require(title.trim().length in 1..120) { "Use a title of 1–120 characters" }
        require(message.trim().length in 1..4000) { "Use a message of 1–4,000 characters" }
        require(targetClass.trim().length <= 80) { "Class must be at most 80 characters" }
        return mapOf("schoolId" to schoolId, "issuerUid" to issuerUid, "title" to title.trim(),
            "message" to message.trim(), "targetClass" to targetClass.trim(), "important" to important,
            "active" to true, "createdAt" to now)
    }
}
