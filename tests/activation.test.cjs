'use strict';
const {test} = require('node:test');
const assert = require('node:assert/strict');
const {PROJECT, WEB_DOMAIN, adminArguments, authorizedDomains, probeBackend} = require('../scripts/activation.cjs');
const {setAdministrator} = require('../scripts/set-admin.cjs');
const {authorizeWeb} = require('../scripts/authorize-web.cjs');
const account = {uid: 'owner', email: 'owner@example.com', emailVerified: true, disabled: false};
const silent = () => {};
const response = (status, body) => ({status, ok: status >= 200 && status < 300, json: async () => body});

test('admin selection rejects missing, ambiguous and accidental flag values', () => {
  for (const args of [[], ['--uid'], ['--uid', '--apply'], ['--uid', 'x', '--email', 'x@example.com'], ['--apply', '--apply'], ['--unknown']]) {
    assert.throws(() => adminArguments(args));
  }
  assert.deepEqual(adminArguments(['--email', account.email]), {apply: false, email: account.email});
});

test('admin dry run never writes even when the account exists', async () => {
  await setAdministrator({email: account.email, apply: false}, {getUserByEmail: async () => account}, {
    collection: () => { throw Error('Unexpected privilege write'); }
  }, silent);
});

test('disabled, unverified and mismatched accounts cannot become administrators', async () => {
  for (const invalid of [{...account, disabled: true}, {...account, emailVerified: false}, {...account, email: 'someone@example.com'}, {...account, uid: 'different'}]) {
    await assert.rejects(() => setAdministrator({email: account.email, uid: account.uid, apply: true}, {getUser: async () => invalid}, {
      collection: () => { throw Error('Unexpected privilege write'); }
    }, silent), /verified email|different account/);
  }
});

test('explicit admin apply updates only the verified selected account and merges its profile', async () => {
  const writes = [];
  const db = {collection: name => ({doc: uid => ({get: async () => ({data: () => ({name: 'My chosen profile name', className: '10'})}), set: async (data, options) => writes.push({name, uid, data, options})})})};
  await setAdministrator({uid: account.uid, apply: true}, {getUser: async uid => { assert.equal(uid, account.uid); return account; }}, db, silent);
  assert.equal(writes.length, 1);
  assert.equal(writes[0].name, 'users');
  assert.equal(writes[0].uid, account.uid);
  assert.equal(writes[0].data.role, 'admin');
  assert.equal(writes[0].data.name, undefined);
  assert.equal(writes[0].data.className, undefined);
  assert.deepEqual(writes[0].options, {merge: true});
});

test('web domain addition preserves existing redirects and is idempotent', () => {
  const original = ['localhost', `${PROJECT}.firebaseapp.com`, 'school.example.com'];
  const domains = authorizedDomains({authorizedDomains: original});
  assert.deepEqual(domains, [...original, WEB_DOMAIN]);
  assert.deepEqual(original, ['localhost', `${PROJECT}.firebaseapp.com`, 'school.example.com']);
  assert.deepEqual(authorizedDomains({authorizedDomains: domains}), domains);
  assert.throws(() => authorizedDomains({authorizedDomains: 'invalid'}));
});

test('web authorization dry run and already-authorized config perform no patch', async () => {
  const credential = {getAccessToken: async () => ({access_token: 'mock-only'})};
  for (const [apply, domains] of [[false, ['localhost']], [true, ['localhost', WEB_DOMAIN]]]) {
    let calls = 0;
    await authorizeWeb(credential, apply, async (url, options) => {
      calls++;
      assert.equal(options.method, undefined);
      return response(200, {authorizedDomains: domains});
    }, silent);
    assert.equal(calls, 1);
  }
});

test('web authorization applies only the domain field, preserving providers', async () => {
  const calls = [];
  await authorizeWeb({getAccessToken: async () => ({access_token: 'mock-only'})}, true, async (url, options) => {
    calls.push({url, options});
    return response(200, calls.length === 1 ? {authorizedDomains: ['localhost'], signIn: {email: {enabled: true}}} : {authorizedDomains: ['localhost', WEB_DOMAIN]});
  }, silent);
  assert.equal(calls.length, 2);
  assert.equal(new URL(calls[1].url).searchParams.get('updateMask'), 'authorizedDomains');
  assert.deepEqual(JSON.parse(calls[1].options.body), {authorizedDomains: ['localhost', WEB_DOMAIN]});
});

test('failed web configuration access does not attempt a write or print credentials', async () => {
  let calls = 0;
  await assert.rejects(() => authorizeWeb({getAccessToken: async () => ({access_token: 'mock-secret'})}, true, async () => {
    calls++; return response(403, {error: {message: 'mock-secret'}});
  }, silent), /HTTP 403/);
  assert.equal(calls, 1);
});

test('backend probe accepts only a deployed callable rejecting missing authentication', async () => {
  const result = await probeBackend(async (url, options) => {
    assert.equal(url, `https://asia-south1-${PROJECT}.cloudfunctions.net/dashboard`);
    assert.equal(options.method, 'POST');
    assert.deepEqual(JSON.parse(options.body), {data: {}});
    assert.equal(options.headers.Authorization, undefined);
    return response(401, {error: {status: 'UNAUTHENTICATED'}});
  }, ['dashboard']);
  assert.equal(result[0].ready, true);
});

test('backend probe rejects missing endpoints, proxy pages and unauthenticated success', async () => {
  for (const result of [response(404, {}), response(200, {result: {}}), response(401, {}), response(403, {error: {status: 'PERMISSION_DENIED'}})]) {
    assert.equal((await probeBackend(async () => result, ['dashboard']))[0].ready, false);
  }
  assert.equal((await probeBackend(async () => {throw Error('offline');}, ['dashboard']))[0].ready, false);
});
