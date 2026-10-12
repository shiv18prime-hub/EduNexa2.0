package com.edunexa.app.data
class SchoolJoinRepository {
 fun joinByCode(code:String,onResult:(Boolean,String)->Unit)=joinSchool(code,onResult)
 fun joinSchool(code:String,onResult:(Boolean,String)->Unit){Backend.call("joinSchool",mapOf("code" to code.trim().uppercase()),{onResult(true,it["message"].toString())},{onResult(false,it)})}
}
