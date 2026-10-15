package com.edunexa.app.data
class SchoolCodeAdminRepository {
 fun createForSchool(schoolId:String,onResult:(Boolean,String)->Unit){Backend.call("rotateCode",mapOf("schoolId" to schoolId),{onResult(true,it["code"].toString())},{onResult(false,it)})}
}
