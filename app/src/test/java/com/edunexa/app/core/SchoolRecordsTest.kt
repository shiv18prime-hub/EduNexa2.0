package com.edunexa.app.core

import org.junit.Assert.*
import org.junit.Test

class SchoolRecordsTest {
    @Test fun resultCalculatesAConsistentGradeAndPassingThreshold() {
        val r=SchoolRecords.result("school","teacher","student","Student","Term 1","Math",2.0,7.0,1)
        assertEquals(3,r["passMarks"])
        assertEquals("FAIL",r["result"])
        assertEquals("F",r["grade"])
        assertEquals(2*100.0/7,r["percentage"] as Double,0.0001)
        assertEquals("teacher",r["issuerUid"])
    }
    @Test fun resultRejectsFractionalOutOfRangeAndNonFiniteMarks() {
        for((got,max) in listOf(1.5 to 10.0, 11.0 to 10.0, -1.0 to 10.0, 1.0 to 0.0, 1.0 to 10001.0, Double.NaN to 10.0, 1.0 to Double.POSITIVE_INFINITY)) {
            assertThrows(IllegalArgumentException::class.java){SchoolRecords.result("s","t","u","Name","Exam","Math",got,max,1)}
        }
    }
    @Test fun attendanceRejectsImpossibleDatesAndAcceptsLeapDay() {
        for(day in listOf("2026-02-29","2026-04-31","2026-13-01","2026-1-01","2026-01-01/extra")) {
            assertThrows(IllegalArgumentException::class.java){SchoolRecords.attendance("s","t","u","Name",day,true,1)}
        }
        assertEquals("2028-02-29",SchoolRecords.attendance("s","t","u","Name","2028-02-29",true,1)["date"])
    }
    @Test fun noticeValidatesTheRulesContractInsteadOfTruncatingUserContent() {
        assertThrows(IllegalArgumentException::class.java){SchoolRecords.notice("s","t","   ","Body","",false,1)}
        assertThrows(IllegalArgumentException::class.java){SchoolRecords.notice("s","t","Title","x".repeat(4001),"",false,1)}
        val r=SchoolRecords.notice("s","t"," Exam "," Bring a pen "," 8 ",true,1)
        assertEquals("Exam",r["title"]);assertEquals("Bring a pen",r["message"]);assertEquals("8",r["targetClass"])
    }
}
