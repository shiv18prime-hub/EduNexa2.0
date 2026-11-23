package com.edunexa.app.data

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class DashboardRepository {
    private val auth=FirebaseAuth.getInstance()
    private val db=FirebaseFirestore.getInstance()
    fun load(onResult:(Map<String,Any?>)->Unit,onError:(String)->Unit){
        val uid=auth.currentUser?.uid?:return onError("Please login first")
        val userRef=db.collection("users").document(uid)
        db.collection("results").whereEqualTo("userId",uid).get().addOnSuccessListener{results->
            db.collection("schoolResults").whereEqualTo("studentId",uid).whereEqualTo("published",true).get().addOnSuccessListener{schoolResults->
                db.collection("attendance").whereEqualTo("studentId",uid).get().addOnSuccessListener{attendance->
                    userRef.get().addOnSuccessListener{profile->
                        val schoolRole=profile.getString("role")=="school"
                        val approvalTask=if(schoolRole)db.collection("schoolApprovals").document(uid).get() else null
                        fun complete(approved:Boolean){
                            db.collection("proEntitlements").document(uid).get().addOnSuccessListener{pro->
                                val present=attendance.documents.count{it.getBoolean("present")==true}
                                val days=attendance.size()
                                val personalLatest=results.documents.maxByOrNull{it.getLong("createdAt")?:0L}
                                val schoolLatest=schoolResults.documents.maxByOrNull{it.getLong("createdAt")?:0L}
                                val personalTime=personalLatest?.getLong("createdAt")?:0L
                                val schoolTime=schoolLatest?.getLong("createdAt")?:0L
                                val latestPercentage=if(schoolTime>personalTime)schoolLatest?.getDouble("percentage") else personalLatest?.getDouble("percentage")
                                val until=pro.getLong("until")?:0L
                                onResult(mapOf("latestPercentage" to latestPercentage,
                                    "attendancePercentage" to if(days==0)null else present*100.0/days,
                                    "results" to (results.size()+schoolResults.size()),"attendanceDays" to days,
                                    "schoolName" to (profile.getString("schoolName")?:""),
                                    "schoolJoinStatus" to (profile.getString("schoolJoinStatus")?:""),
                                    "schoolApproved" to approved,"pro" to (until>System.currentTimeMillis()),"proUntil" to until))
                            }.addOnFailureListener{onError(it.message?:"Could not read membership")}
                        }
                        if(approvalTask==null)complete(false) else approvalTask.addOnSuccessListener{complete(it.getBoolean("approved")==true)}
                            .addOnFailureListener{onError(it.message?:"Could not check school approval")}
                    }.addOnFailureListener{onError(it.message?:"Could not read profile")}
                }.addOnFailureListener{onError(it.message?:"Could not load attendance")}
            }.addOnFailureListener{onError(it.message?:"Could not load published school results")}
        }.addOnFailureListener{onError(it.message?:"Could not load results")}
    }
}
