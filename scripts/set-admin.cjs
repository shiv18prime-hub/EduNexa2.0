// Run only in the Firebase owner's authenticated environment.
const {initializeApp}=require('../functions/node_modules/firebase-admin/app');
const {getFirestore}=require('../functions/node_modules/firebase-admin/firestore');
const {getAuth}=require('../functions/node_modules/firebase-admin/auth');
const args=process.argv.slice(2),uid=args[args.indexOf('--uid')+1];
if(!uid||uid.startsWith('--'))throw Error('Usage: node scripts/set-admin.cjs --uid EXISTING_UID --apply');
initializeApp({projectId:'student-result-pro'});
(async()=>{const account=await getAuth().getUser(uid);if(!args.includes('--apply')){console.log('Verified existing account; add --apply to grant administrator role.');return;}await getFirestore().collection('users').doc(uid).set({uid,name:account.displayName||'Administrator',email:account.email||'',role:'admin'},{merge:true});console.log('Administrator role granted to the selected account.');})().catch(()=>{console.error('Could not update administrator. Check project access and UID.');process.exit(1)});
