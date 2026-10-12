package com.edunexa.app.data
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
object SchoolScope {
 fun resolve(success:(String)->Unit,failure:(String)->Unit){val uid=FirebaseAuth.getInstance().currentUser?.uid?:return failure("Please sign in");FirebaseFirestore.getInstance().collection("users").document(uid).get().addOnSuccessListener{d->when{d.getString("role")=="school"&&d.getBoolean("schoolApproved")==true->success(uid);d.getString("role")=="teacher"&&d.getString("schoolJoinStatus")=="connected"->success(d.getString("schoolId").orEmpty());else->failure("Connect to an approved school first")}}.addOnFailureListener{failure(it.message?:"Could not load school")}}
}
