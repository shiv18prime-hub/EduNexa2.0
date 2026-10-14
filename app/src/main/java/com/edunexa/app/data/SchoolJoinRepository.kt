package com.edunexa.app.data
class SchoolJoinRepository {
 private val workflows=SchoolWorkflowRepository()
 fun joinByCode(code:String,onResult:(Boolean,String)->Unit)=joinSchool(code,onResult)
 fun joinSchool(code:String,onResult:(Boolean,String)->Unit)=workflows.join(code,onResult)
}
