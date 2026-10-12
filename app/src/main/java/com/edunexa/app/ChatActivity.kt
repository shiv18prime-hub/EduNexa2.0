package com.edunexa.app
import android.os.Bundle
import android.widget.*
import com.edunexa.app.data.Backend
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query

class ChatActivity:EduActivity(){
 private lateinit var root:LinearLayout;private lateinit var status:TextView;private var listener:ListenerRegistration?=null;private var conversationId:String?=null;private var contactName="";private var messageBox:LinearLayout?=null
 override fun onCreate(state:Bundle?){super.onCreate(state);root=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setPadding(28,20,28,24)};setContentView(root);root.addView(TextView(this).apply{text="School Messages";textSize=25f;setTypeface(typeface,android.graphics.Typeface.BOLD)});status=TextView(this);root.addView(status);conversationId=state?.getString("conversation");contactName=state?.getString("name").orEmpty();if(conversationId!=null)showChat()else loadContacts()}
 private fun loadContacts(){status.text="Loading school contacts…";Backend.call("contacts",success={r->status.text="Select a school member";val contacts=Backend.rows(r["contacts"]);if(contacts.isEmpty())status.text="No other connected members yet";val scroll=ScrollView(this);val list=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL};scroll.addView(list);root.addView(scroll,LinearLayout.LayoutParams(-1,0,1f));contacts.forEach{c->list.addView(Button(this).apply{text="${c["name"]} • ${c["role"]}";isAllCaps=false;setOnClickListener{isEnabled=false;Backend.call("conversation",mapOf("otherUid" to c["uid"].toString()),{r->conversationId=r["id"].toString();contactName=r["name"].toString();showChat()},{error->isEnabled=true;status.text=error})}})}},failure={status.text=it})}
 private fun showChat(){listener?.remove();while(root.childCount>2)root.removeViewAt(2);status.text=contactName;val scroll=ScrollView(this);val box=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL};messageBox=box;scroll.addView(box);root.addView(scroll,LinearLayout.LayoutParams(-1,0,1f));val input=EditText(this).apply{hint="Write a message";maxLines=4};root.addView(input);root.addView(Button(this).apply{text="Send";setOnClickListener{val body=input.text.toString().trim();if(body.isBlank())return@setOnClickListener;isEnabled=false;Backend.call("sendMessage",mapOf("conversationId" to conversationId.orEmpty(),"text" to body),{input.text.clear();isEnabled=true},{error->isEnabled=true;Toast.makeText(this@ChatActivity,error,Toast.LENGTH_LONG).show()})}});observe(scroll)}
 private fun observe(scroll:ScrollView?=null){val id=conversationId?:return;listener?.remove();listener=FirebaseFirestore.getInstance().collection("conversations").document(id).collection("messages").orderBy("createdAt",Query.Direction.DESCENDING).limit(100).addSnapshotListener{snap,e->if(e!=null){status.text=e.message?:"Could not load messages";return@addSnapshotListener};val box=messageBox?:return@addSnapshotListener;box.removeAllViews();snap?.documents?.reversed()?.forEach{d->box.addView(TextView(this).apply{text="${if(d.getString("senderId")==FirebaseAuth.getInstance().currentUser?.uid) "You" else contactName}: ${d.getString("text")}";textSize=16f;setPadding(12,14,12,14)})};scroll?.post{scroll.fullScroll(android.view.View.FOCUS_DOWN)}}}
 override fun onStart(){super.onStart();if(conversationId!=null&&messageBox!=null)observe()}
 override fun onStop(){listener?.remove();listener=null;super.onStop()}
 override fun onSaveInstanceState(out:Bundle){out.putString("conversation",conversationId);out.putString("name",contactName);super.onSaveInstanceState(out)}
}
