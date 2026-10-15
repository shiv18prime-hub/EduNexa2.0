'use strict';
const {initializeApp}=require('firebase-admin/app');
const {getFirestore}=require('firebase-admin/firestore');
const {onCall,onRequest,HttpsError}=require('firebase-functions/v2/https');
const {onDocumentCreated}=require('firebase-functions/v2/firestore');
const {getMessaging}=require('firebase-admin/messaging');
const {signature,AppError}=require('./lib/domain.cjs');
const {services}=require('./lib/services.cjs');
initializeApp();
const db=getFirestore(),api=services(db);
const region='asia-south1';
const names=['joinSchool','reviewJoin','cancelJoin','disconnectMember','rotateCode','publishMcq','studentTests','startMcq','submitMcq','saveResult','publishResult','schoolStudents','saveAttendance','publishNotice','dashboard','createMarksheet','verifyMarksheet','revokeMarksheet','contacts','conversation','sendMessage','grantPro','redeemCoupon'];
function callable(name,options={}){return onCall({region,maxInstances:10,...options},async request=>{try{return await api[name](request.auth?.uid,request.data||{});}catch(e){if(e instanceof AppError)throw new HttpsError(e.code,e.message);console.error(name,e.code||e.name);throw new HttpsError('internal','Could not complete this action. Try again.');}});}
for(const name of names)exports[name]=callable(name);
exports.createProOrder=callable('createProOrder',{secrets:['RAZORPAY_KEY_ID','RAZORPAY_KEY_SECRET']});
exports.verifyProPayment=callable('verifyProPayment',{secrets:['RAZORPAY_KEY_ID','RAZORPAY_KEY_SECRET']});
exports.paymentWebhook=onRequest({region,secrets:['RAZORPAY_WEBHOOK_SECRET']},async(req,res)=>{
 if(req.method!=='POST')return res.status(405).send('Method not allowed');
 if(!process.env.RAZORPAY_WEBHOOK_SECRET)return res.status(503).send('Webhook not configured');
 if(!signature(process.env.RAZORPAY_WEBHOOK_SECRET,req.rawBody,req.get('x-razorpay-signature')))return res.status(401).send('Invalid signature');
 try{const entity=req.body?.payload?.payment?.entity;if(entity&&(await db.collection('proOrders').doc(entity.order_id).get()).exists){if(req.body.event==='payment.captured')await api.fulfill(entity.order_id,entity);if(req.body.event==='payment.refunded')await api.refundPayment(entity);}res.status(200).send('OK');}catch(e){console.error('paymentWebhook',e.code||e.name);res.status(500).send('Retry');}
});
exports.chatNotification=onDocumentCreated({region,document:'conversations/{conversationId}/messages/{messageId}'},async event=>{
 const message=event.data.data(),c=(await db.collection('conversations').doc(event.params.conversationId).get()).data();if(!c)return;
 for(const uid of c.members.filter(x=>x!==message.senderId)){const u=(await db.collection('users').doc(uid).get()).data();if(!u?.fcmToken)continue;try{await getMessaging().send({token:u.fcmToken,data:{recipientUid:uid,title:'New EduNexa message',body:'Open EduNexa to read your school message.'}});}catch(e){console.error('chatNotification',e.code);}}
});
