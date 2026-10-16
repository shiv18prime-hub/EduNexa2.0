package com.edunexa.app

import android.os.Bundle
import android.widget.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.*
import java.security.MessageDigest

/** School chat uses rule-validated Firestore reads and writes, so it works on Spark. */
class ChatActivity:EduActivity(){
 private val db by lazy{FirebaseFirestore.getInstance()}
 private val auth by lazy{FirebaseAuth.getInstance()}
 private lateinit var root:LinearLayout
 private lateinit var status:TextView
 private var listener:ListenerRegistration?=null
 private var conversationId:String?=null
 private var schoolId=""
 private var contactName=""
 private var messageBox:LinearLayout?=null

 override fun onCreate(state:Bundle?){super.onCreate(state);root=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setPadding(28,20,28,24)};setContentView(root);root.addView(TextView(this).apply{text="School Messages";textSize=25f;setTypeface(typeface,android.graphics.Typeface.BOLD)});status=TextView(this);root.addView(status);val uid=auth.currentUser?.uid?:return;db.collection("users").document(uid).get().addOnSuccessListener{me->val role=me.getString("role").orEmpty();schoolId=if(role=="school")uid else me.getString("schoolId").orEmpty();if(schoolId.isBlank()||(role!="school"&&me.getString("schoolJoinStatus")!="connected")){status.text="Connect to an approved school before messaging.";return@addOnSuccessListener};val restored=state?.getString("conversation");if(restored!=null){conversationId=restored;contactName=state?.getString("name").orEmpty();showChat()}else loadContacts(role)}.addOnFailureListener{status.text=it.message?:"Could not load your school"}}

 private fun loadContacts(role:String){status.text="Loading school contacts…";db.collection("memberships").whereEqualTo("schoolId",schoolId).whereEqualTo("active",true).get().addOnSuccessListener{snap->val contacts=snap.documents.filter{it.getString("uid")!=auth.currentUser?.uid&&it.getString("role") in listOf("student","teacher")}.map{it.id to (it.getString("name").orEmpty().ifBlank{"School member"} to it.getString("role").orEmpty())}.toMutableList();if(role!="school")db.collection("users").document(schoolId).get().addOnSuccessListener{school->if(school.exists())contacts.add(schoolId to ((school.getString("schoolName")?:school.getString("name").orEmpty()) to "school"));showContacts(contacts)}.addOnFailureListener{showContacts(contacts)}else showContacts(contacts)}.addOnFailureListener{status.text=it.message?:"Could not load school members"}}

 private fun showContacts(contacts:List<Pair<String,Pair<String,String>>>){status.text="Select a school member";val scroll=ScrollView(this);val list=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL};scroll.addView(list);root.addView(scroll,LinearLayout.LayoutParams(-1,0,1f));if(contacts.isEmpty())status.text="No other connected members yet";contacts.forEach{(uid,details)->list.addView(Button(this).apply{text="${details.first} • ${details.second}";isAllCaps=false;setOnClickListener{isEnabled=false;openConversation(uid,details.first){isEnabled=true}}})}}

 private fun openConversation(otherUid:String,name:String,done:(()->Unit)?=null){
  val me=auth.currentUser?.uid?:return;val members=listOf(me,otherUid).sorted()
  val digest=MessageDigest.getInstance("SHA-256").digest("$schoolId|${members.joinToString("|")}".toByteArray())
  val id=digest.joinToString(""){"%02x".format(it.toInt() and 0xff)}
  val ref=db.collection("conversations").document(id)
  ref.get().addOnSuccessListener{snap->
   if(snap.exists()){conversationId=id;contactName=name;showChat();done?.invoke()}
   else ref.set(mapOf("schoolId" to schoolId,"members" to members,"createdAt" to System.currentTimeMillis())).addOnSuccessListener{conversationId=id;contactName=name;showChat();done?.invoke()}.addOnFailureListener{status.text=it.message?:"Could not start conversation";done?.invoke()}
  }.addOnFailureListener{status.text=it.message?:"Could not open conversation";done?.invoke()}
 }

 private fun showChat(){listener?.remove();while(root.childCount>2)root.removeViewAt(2);status.text="$contactName · Spark messages (no push notifications)";val scroll=ScrollView(this);val box=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL};messageBox=box;scroll.addView(box);root.addView(scroll,LinearLayout.LayoutParams(-1,0,1f));val input=EditText(this).apply{hint="Write a message";maxLines=4};root.addView(input);root.addView(Button(this).apply{text="Send";setOnClickListener{val body=input.text.toString().trim();if(body.isBlank())return@setOnClickListener;if(body.length>2000){Toast.makeText(this@ChatActivity,"Messages can contain up to 2,000 characters",Toast.LENGTH_SHORT).show();return@setOnClickListener};isEnabled=false;db.collection("conversations").document(conversationId.orEmpty()).collection("messages").add(mapOf("senderId" to auth.currentUser?.uid,"text" to body,"createdAt" to FieldValue.serverTimestamp())).addOnSuccessListener{input.text.clear();isEnabled=true}.addOnFailureListener{isEnabled=true;Toast.makeText(this@ChatActivity,it.message?:"Could not send message",Toast.LENGTH_LONG).show()}}});observe(scroll)}

 private fun observe(scroll:ScrollView?=null){val id=conversationId?:return;listener?.remove();listener=db.collection("conversations").document(id).collection("messages").orderBy("createdAt",Query.Direction.DESCENDING).limit(100).addSnapshotListener{snap,e->if(e!=null){status.text=e.message?:"Could not load messages";return@addSnapshotListener};val box=messageBox?:return@addSnapshotListener;box.removeAllViews();snap?.documents?.reversed()?.forEach{d->box.addView(TextView(this).apply{text="${if(d.getString("senderId")==auth.currentUser?.uid)"You" else contactName}: ${d.getString("text")}";textSize=16f;setPadding(12,14,12,14)})};scroll?.post{scroll.fullScroll(android.view.View.FOCUS_DOWN)}}}
 override fun onStart(){super.onStart();if(conversationId!=null&&messageBox!=null)observe()}
 override fun onStop(){listener?.remove();listener=null;super.onStop()}
 override fun onSaveInstanceState(out:Bundle){out.putString("conversation",conversationId);out.putString("name",contactName);super.onSaveInstanceState(out)}
}
