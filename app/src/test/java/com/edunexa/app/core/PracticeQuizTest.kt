package com.edunexa.app.core

import org.junit.Assert.*
import org.junit.Test

class PracticeQuizTest {
    @Test fun everyQuizCanBeScoredAndReviewedWithoutTheBackend() {
        for(set in PracticeQuiz.sets) {
            val score=PracticeQuiz.score(set.questions,set.questions.map{it.correct})
            assertEquals(set.questions.size,score.correct);assertEquals(100.0,score.percentage,0.0)
            assertTrue(set.questions.all{it.explanation.isNotBlank()&&it.options.distinct().size==it.options.size})
        }
    }
    @Test fun scoringDistinguishesIncorrectAndUnansweredQuestions() {
        val q=PracticeQuiz.sets.first().questions
        val answers=q.mapIndexed{i,item->when(i){0->item.correct;1->(item.correct+1)%item.options.size;else->-1}}
        val score=PracticeQuiz.score(q,answers)
        assertEquals(1,score.correct);assertEquals(1,score.wrong);assertEquals(q.size-2,score.skipped)
        assertEquals(q.size,score.correct+score.wrong+score.skipped)
    }
    @Test fun malformedSessionsCannotSilentlyProduceAScore() {
        val q=PracticeQuiz.sets.first().questions
        assertThrows(IllegalArgumentException::class.java){PracticeQuiz.score(q,listOf(0))}
        assertThrows(IllegalArgumentException::class.java){PracticeQuiz.score(q,List(q.size){99})}
        assertThrows(IllegalArgumentException::class.java){PracticeQuiz.score(emptyList(),emptyList())}
    }
}
