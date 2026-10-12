const {readFileSync}=require('node:fs');
const {test,before,after,beforeEach}=require('node:test');
const {initializeTestEnvironment,assertFails,assertSucceeds}=require('@firebase/rules-unit-testing');
const {doc,setDoc,updateDoc,getDoc,writeBatch,collection,query,where,getDocs,deleteField}=require('firebase/firestore');
let env;
before(async()=>{env=await initializeTestEnvironment({projectId:'demo-edunexa',firestore:{rules:readFileSync('firestore.rules','utf8')}})});
after(async()=>{await env.cleanup()});
beforeEach(async()=>{await env.clearFirestore();await env.withSecurityRulesDisabled(async c=>{
 const db=c.firestore();
 for(const [id,data] of Object.entries({s:{uid:'s',role:'student',schoolApproved:true,name:'Student'},school:{uid:'school',role:'school',schoolApproved:true,name:'Private school',contactNumber:'private'},other:{uid:'other',role:'school',schoolApproved:true},pending:{uid:'pending',role:'school',schoolApproved:false},admin:{uid:'admin',role:'admin'}}))await setDoc(doc(db,'users',id),data);
 await setDoc(doc(db,'schoolCodes','EDU-123'),{schoolId:'school',active:true,schoolName:'School'});
})});
const db=id=>env.authenticatedContext(id).firestore();
const profile=role=>({uid:'new',role,schoolApproved:role==='student',name:'New',email:'a@b.c',schoolName:'',className:'1',contactNumber:'',address:'',createdAt:1});
test('registration accepts students and unapproved schools; blocks approval/admin/entitlement injection',async()=>{
 await assertSucceeds(setDoc(doc(db('new'),'users','new'),profile('student')));
 await assertFails(setDoc(doc(db('new2'),'users','new2'),{...profile('school'),uid:'new2',schoolApproved:true}));
 await assertFails(setDoc(doc(db('new3'),'users','new3'),{...profile('admin'),uid:'new3'}));
 await assertFails(setDoc(doc(db('new4'),'users','new4'),{...profile('student'),uid:'new4',isPro:true}));
 await assertSucceeds(setDoc(doc(db('new5'),'users','new5'),{...profile('school'),uid:'new5'}));
});
test('self edit cannot change role, approval, membership or Pro; profile edits work',async()=>{
 const ref=doc(db('s'),'users','s');
 await assertSucceeds(updateDoc(ref,{name:'Edited',phone:'1234567890'}));
 for(const update of [{role:'admin'},{schoolApproved:false},{schoolId:'school'},{isPro:true},{schoolJoinStatus:'connected'}])await assertFails(updateDoc(ref,update));
});
async function join(){const d=db('s');const b=writeBatch(d);b.set(doc(d,'schoolJoinRequests','school_s'),{schoolId:'school',studentId:'s',schoolCode:'EDU-123',status:'pending',studentName:'Student',studentEmail:'a@b.c',className:'1',schoolName:'School',createdAt:1});b.update(doc(d,'users','s'),{schoolJoinStatus:'pending',pendingSchoolId:'school',pendingSchoolCode:'EDU-123'});await assertSucceeds(b.commit())}
test('join request and school approval work atomically; unrelated school cannot approve',async()=>{
 await join();await assertFails(updateDoc(doc(db('other'),'users','s'),{schoolId:'other',schoolJoinStatus:'connected'}));
 const d=db('school');const b=writeBatch(d);b.update(doc(d,'users','s'),{schoolId:'school',schoolCode:'EDU-123',schoolJoinStatus:'connected',schoolJoinedAt:1,pendingSchoolId:deleteField(),pendingSchoolCode:deleteField()});b.update(doc(d,'schoolJoinRequests','school_s'),{status:'approved',reviewedAt:1});await assertSucceeds(b.commit());
 await assertSucceeds(getDocs(query(collection(d,'users'),where('role','==','student'),where('schoolId','==','school'))));
 await assertFails(getDoc(doc(db('other'),'users','s')));
});
test('students cannot read private school profiles or fabricate school codes',async()=>{
 await assertFails(getDoc(doc(db('s'),'users','school')));
 await assertSucceeds(getDoc(doc(db('s'),'schoolCodes','EDU-123')));
 await assertFails(setDoc(doc(db('s'),'schoolCodes','FAKE'),{schoolId:'school',active:true}));
});
test('attendance ownership cannot be stolen',async()=>{
 await env.withSecurityRulesDisabled(c=>setDoc(doc(c.firestore(),'attendance','record'),{schoolId:'school',studentId:'s',present:true}));
 await assertFails(updateDoc(doc(db('other'),'attendance','record'),{schoolId:'other',present:false}));
 await assertSucceeds(updateDoc(doc(db('school'),'attendance','record'),{present:false}));
});
test('rejection is atomic and cannot be performed by a pending school',async()=>{
 await join();await assertFails(updateDoc(doc(db('pending'),'schoolJoinRequests','school_s'),{status:'rejected'}));
 await assertFails(updateDoc(doc(db('school'),'schoolJoinRequests','school_s'),{status:'rejected'}));
 const d=db('school');const b=writeBatch(d);b.update(doc(d,'schoolJoinRequests','school_s'),{status:'rejected',reviewedAt:1});b.update(doc(d,'users','s'),{schoolJoinStatus:'rejected',pendingSchoolId:deleteField(),pendingSchoolCode:deleteField()});await assertSucceeds(b.commit());
});
