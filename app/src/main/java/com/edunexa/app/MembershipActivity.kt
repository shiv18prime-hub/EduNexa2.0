package com.edunexa.app
import android.os.Bundle
import android.widget.*
import com.edunexa.app.data.Backend
import com.google.firebase.firestore.FirebaseFirestore
import com.razorpay.Checkout
import com.razorpay.PaymentData
import com.razorpay.PaymentResultWithDataListener
import org.json.JSONObject

class MembershipActivity:EduActivity(),PaymentResultWithDataListener{
 private lateinit var status:TextView;private lateinit var coupon:EditText;private lateinit var buy:Button;private var orderId:String?=null
 override fun onCreate(state:Bundle?){super.onCreate(state);orderId=state?.getString("order");val root=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setPadding(32,24,32,24)};setContentView(ScrollView(this).apply{addView(root)});root.addView(TextView(this).apply{text="EduNexa Pro";textSize=28f;setTypeface(typeface,android.graphics.Typeface.BOLD)});root.addView(TextView(this).apply{text="Registered practice marksheets and advanced image tools. Core student and school services stay available."});status=TextView(this);root.addView(status);coupon=EditText(this).apply{hint="Coupon code (optional)"};root.addView(coupon);root.addView(Button(this).apply{text="Apply / Redeem Coupon";setOnClickListener{isEnabled=false;Backend.call("redeemCoupon",mapOf("code" to coupon.text.toString()),{r->isEnabled=true;status.text=r["message"].toString()},{e->isEnabled=true;status.text=e})}});buy=Button(this).apply{text="Paid membership unavailable";isEnabled=false;setOnClickListener{isEnabled=false;Backend.call("createProOrder",mapOf("code" to coupon.text.toString()),{r->orderId=r["orderId"].toString();try{val checkout=Checkout();checkout.setKeyID(r["keyId"].toString());checkout.open(this@MembershipActivity,JSONObject().put("name","EduNexa Pro").put("description","${r["days"]} day membership").put("order_id",orderId).put("currency",r["currency"]).put("amount",(r["amount"] as Number).toInt()).put("prefill",JSONObject().put("email",r["email"])).put("theme",JSONObject().put("color","#4338CA")))}catch(e:Exception){isEnabled=true;status.text=e.message}}, {e->isEnabled=true;status.text=e})}};root.addView(buy);refresh()}
 private fun refresh(){Backend.call("dashboard",success={r->status.text=if(r["pro"]==true)"Pro active until ${java.text.DateFormat.getDateInstance().format(java.util.Date((r["proUntil"] as Number).toLong()))}" else "Free membership • Redeem an eligible coupon below"},failure={status.text=it});FirebaseFirestore.getInstance().collection("plans").document("pro").get().addOnSuccessListener{d->if(d.getBoolean("enabled")==true){buy.isEnabled=true;buy.text="Buy Pro • ₹${(d.getLong("pricePaise")?:0)/100.0} / ${d.getLong("days")} days"}}}
 override fun onPaymentSuccess(paymentId:String?,data:PaymentData?){val id=data?.orderId?:orderId?:return;Backend.call("verifyProPayment",mapOf("orderId" to id,"paymentId" to (paymentId?:data?.paymentId.orEmpty()),"signature" to data?.signature.orEmpty()),{r->status.text=r["message"].toString();buy.isEnabled=true;refresh()},{e->status.text="Payment verification pending: $e";buy.isEnabled=true})}
 override fun onPaymentError(code:Int,message:String?,data:PaymentData?){status.text="Payment was not completed. You can retry.";buy.isEnabled=true}
 override fun onSaveInstanceState(out:Bundle){out.putString("order",orderId);super.onSaveInstanceState(out)}
}
