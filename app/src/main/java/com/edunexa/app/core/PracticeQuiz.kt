package com.edunexa.app.core

data class PracticeQuestion(val prompt: String, val options: List<String>, val correct: Int, val explanation: String)
data class PracticeSet(val id: String, val title: String, val description: String, val questions: List<PracticeQuestion>)
data class PracticeScore(val correct: Int, val wrong: Int, val skipped: Int, val total: Int) {
    val percentage: Double get() = correct * 100.0 / total
}

object PracticeQuiz {
    fun score(questions: List<PracticeQuestion>, answers: List<Int>): PracticeScore {
        require(questions.isNotEmpty() && questions.size == answers.size) { "Invalid practice session" }
        questions.forEachIndexed { index, question ->
            require(question.correct in question.options.indices && (answers[index] == -1 || answers[index] in question.options.indices)) { "Invalid answer" }
        }
        val correct = questions.indices.count { answers[it] == questions[it].correct }
        val skipped = answers.count { it == -1 }
        return PracticeScore(correct, questions.size - correct - skipped, skipped, questions.size)
    }

    val sets = listOf(
        PracticeSet("computers", "Computer fundamentals", "Hardware, software and digital basics", listOf(
            PracticeQuestion("What does CPU stand for?", listOf("Central Processing Unit", "Computer Personal Unit", "Central Program Utility", "Control Power Unit"), 0, "The Central Processing Unit executes instructions and processes data."),
            PracticeQuestion("Which memory normally loses its data when power is turned off?", listOf("SSD", "RAM", "ROM", "DVD"), 1, "RAM is volatile memory. It holds working data while the computer is powered on."),
            PracticeQuestion("Which device is primarily used for input?", listOf("Monitor", "Speaker", "Keyboard", "Projector"), 2, "A keyboard sends typed instructions and data into a computer."),
            PracticeQuestion("Which pair contains the digits used in binary?", listOf("1 and 2", "0 and 9", "2 and 8", "0 and 1"), 3, "Binary uses only two digits: 0 and 1."),
            PracticeQuestion("What is the main role of an operating system?", listOf("Manage hardware and applications", "Only draw pictures", "Replace the processor", "Supply electrical power"), 0, "An operating system manages resources, files and the execution of applications."),
            PracticeQuestion("An algorithm is best described as…", listOf("A storage device", "A sequence of steps to solve a problem", "A screen resolution", "A network cable"), 1, "An algorithm describes a finite set of steps for completing a task or solving a problem.")
        )),
        PracticeSet("math", "Math essentials", "Percentages, averages and logical thinking", listOf(
            PracticeQuestion("What is 25% of 240?", listOf("24", "48", "60", "80"), 2, "25% is one quarter. 240 ÷ 4 = 60."),
            PracticeQuestion("A student scores 360 out of 500. What is the percentage?", listOf("62%", "68%", "72%", "80%"), 2, "Percentage = (360 ÷ 500) × 100 = 72%."),
            PracticeQuestion("What is the average of 6, 8 and 10?", listOf("6", "7", "8", "9"), 2, "Add the values and divide by their count: (6 + 8 + 10) ÷ 3 = 8."),
            PracticeQuestion("Solve: 3x + 5 = 20", listOf("3", "5", "7", "15"), 1, "Subtract 5 from both sides, then divide by 3: x = 15 ÷ 3 = 5."),
            PracticeQuestion("A ₹500 item has a 10% discount. What is the final price?", listOf("₹400", "₹450", "₹490", "₹550"), 1, "The discount is ₹50. The final price is ₹500 − ₹50 = ₹450."),
            PracticeQuestion("What comes next: 2, 4, 8, 16, …?", listOf("18", "24", "30", "32"), 3, "Each number is twice the previous number. 16 × 2 = 32.")
        )),
        PracticeSet("english", "English foundations", "Everyday grammar and sentence building", listOf(
            PracticeQuestion("Choose the correct sentence.", listOf("She go to school.", "She goes to school.", "She going school.", "She gone to school."), 1, "In the simple present, use 'goes' with the third-person singular subject 'she'."),
            PracticeQuestion("Complete: They ___ studying now.", listOf("is", "am", "are", "be"), 2, "Use 'are' with 'they' in the present continuous: They are studying now."),
            PracticeQuestion("Choose the plural of 'child'.", listOf("Childs", "Childes", "Children", "Childrens"), 2, "'Children' is the irregular plural of 'child'."),
            PracticeQuestion("Complete: I have ___ umbrella.", listOf("a", "an", "the only", "many"), 1, "Use 'an' before a word beginning with a vowel sound: an umbrella."),
            PracticeQuestion("Which word is closest in meaning to 'happy'?", listOf("Glad", "Angry", "Tired", "Afraid"), 0, "'Glad' and 'happy' both describe pleasure or joy."),
            PracticeQuestion("Choose the correct past-tense sentence.", listOf("We visit yesterday.", "We visits yesterday.", "We visiting yesterday.", "We visited yesterday."), 3, "'Yesterday' refers to the past. The regular past form of 'visit' is 'visited'.")
        ))
    )
}
