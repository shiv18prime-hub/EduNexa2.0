package com.edunexa.app.core
import org.junit.Assert.*
import org.junit.Test
class MarksCalculatorTest {
 @Test fun mandatoryFailureAndOptionalExclusion(){val r=MarksCalculator.calculate(listOf(SubjectMarks("Math",100,100,33),SubjectMarks("English",20,5,6),SubjectMarks("Art",100,0,33,true)));assertEquals(120,r.total);assertEquals(105,r.obtained);assertFalse(r.passed);assertEquals("A",r.grade)}
 @Test fun exactPassingBoundary(){assertTrue(MarksCalculator.calculate(listOf(SubjectMarks("Math",100,33,33))).passed)}
 @Test(expected=IllegalArgumentException::class) fun duplicateNames(){MarksCalculator.calculate(listOf(SubjectMarks("Math",100,50,33),SubjectMarks("math",100,50,33)))}
 @Test(expected=IllegalArgumentException::class) fun marksBeyondMaximum(){MarksCalculator.calculate(listOf(SubjectMarks("Math",100,101,33)))}
 @Test(expected=IllegalArgumentException::class) fun onlyOptionalSubjects(){MarksCalculator.calculate(listOf(SubjectMarks("Art",100,50,33,true)))}
}
