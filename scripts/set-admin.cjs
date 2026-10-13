'use strict';
const backendRequire = require('node:module').createRequire(require.resolve('../functions/package.json'));
// Run only in the Firebase owner's authenticated environment. Dry run by default.
const {PROJECT, adminArguments, verifiedAdministrator} = require('./activation.cjs');

async function setAdministrator(selection, auth, db, log = console.log) {
  const found = selection.uid ? await auth.getUser(selection.uid) : await auth.getUserByEmail(selection.email);
  const account = verifiedAdministrator(found, selection);
  log(`Selected verified account: ${account.email} (UID ${account.uid}). Project: ${PROJECT}.`);
  if (!selection.apply) { log('Dry run: add --apply to change this account to the administrator role.'); return; }
  const profile = db.collection('users').doc(account.uid);
  const existing = (await profile.get()).data();
  const patch = {uid: account.uid, email: account.email, role: 'admin'};
  if (!existing?.name) patch.name = account.displayName || 'Administrator';
  await profile.set(patch, {merge: true});
  log('Administrator role granted. Sign out and sign in again in EduNexa.');
}

if (require.main === module) {
  (async () => {
    const selection = adminArguments(process.argv.slice(2));
    const {initializeApp, applicationDefault} = backendRequire('firebase-admin/app');
    const {getAuth} = backendRequire('firebase-admin/auth');
    const {getFirestore} = backendRequire('firebase-admin/firestore');
    const app = initializeApp({projectId: PROJECT, credential: applicationDefault()});
    await setAdministrator(selection, getAuth(app), getFirestore(app));
  })().catch(error => { console.error(`Administrator setup stopped: ${error.code || error.message}`); process.exitCode = 1; });
}

module.exports = {setAdministrator};
