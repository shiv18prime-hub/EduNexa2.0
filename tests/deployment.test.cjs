'use strict';
const {test} = require('node:test');
const assert = require('node:assert/strict');
const {mkdtempSync, mkdirSync, copyFileSync, writeFileSync, readFileSync, rmSync} = require('node:fs');
const {tmpdir} = require('node:os');
const {join} = require('node:path');
const {spawnSync} = require('node:child_process');

// Exercise the real shell entry points with logged local stubs, never Firebase
// accounts, credentials, billing APIs or a production deployment.
function run(script, args, extraEnv = {}) {
  const root = mkdtempSync(join(tmpdir(), 'edunexa-deployment-'));
  try {
    mkdirSync(join(root, 'scripts'));
    mkdirSync(join(root, 'bin'));
    mkdirSync(join(root, 'node_modules', '.bin'), {recursive: true});
    for (const name of ['activate-project.sh', 'deploy-core.sh', 'deploy-spark.sh']) {
      copyFileSync(join(__dirname, '..', 'scripts', name), join(root, 'scripts', name));
    }
    const log = join(root, 'calls');
    writeFileSync(log, '');
    for (const name of ['node', 'gcloud', 'npm']) {
      writeFileSync(join(root, 'bin', name), `#!/usr/bin/env bash\nprintf '%s\\n' '${name} '"$*" >> "$EDUNEXA_TEST_LOG"\n`, {mode: 0o755});
    }
    writeFileSync(join(root, 'node_modules', '.bin', 'firebase'), `#!/usr/bin/env bash
printf '%s\\n' "firebase $*" >> "$EDUNEXA_TEST_LOG"
if [[ "$1" == deploy && "\${EDUNEXA_TEST_FAIL_DEPLOY:-}" == true ]]; then exit 37; fi
`, {mode: 0o755});
    const result = spawnSync('bash', [join(root, 'scripts', script), ...args], {
      encoding: 'utf8', timeout: 10000,
      env: {...process.env, PATH: `${join(root, 'bin')}:${process.env.PATH}`, EDUNEXA_TEST_LOG: log, ...extraEnv}
    });
    assert.ifError(result.error);
    return {...result, calls: readFileSync(log, 'utf8').trim().split('\n').filter(Boolean)};
  } finally { rmSync(root, {recursive: true, force: true}); }
}

test('default and explicit Spark activation never deploy Functions/Storage or probe absent endpoints', () => {
  for (const flags of [[], ['--spark']]) {
    const result = run('activate-project.sh', [...flags, '--installed', '--email', 'admin@example.com']);
    assert.equal(result.status, 0, result.stderr);
    assert.deepEqual(result.calls.filter(c => c.startsWith('firebase deploy')), [
      'firebase deploy --project student-result-pro --only firestore'
    ]);
    assert.ok(result.calls.includes('node scripts/set-admin.cjs --email admin@example.com --apply'));
    assert.ok(result.calls.includes('node scripts/authorize-web.cjs --apply'));
    assert.ok(result.calls.every(c => !/billing|check-backend|npm /.test(c)));
    assert.match(result.stdout, /not full app activation/);
  }
});

test('failed Firestore deployment prevents domain and administrator writes', () => {
  const result = run('activate-project.sh', ['--spark', '--installed', '--email', 'admin@example.com'], {EDUNEXA_TEST_FAIL_DEPLOY: 'true'});
  assert.equal(result.status, 37);
  assert.ok(result.calls.includes('node scripts/set-admin.cjs --email admin@example.com'));
  assert.ok(result.calls.every(c => !c.endsWith('--apply')));
  assert.doesNotMatch(result.stdout, /setup completed/);
});

test('old core command refuses billed deployment without explicit plan selection', () => {
  for (const flags of [[], ['--installed']]) {
    const result = run('deploy-core.sh', flags);
    assert.equal(result.status, 1);
    assert.deepEqual(result.calls, []);
    assert.match(result.stderr, /no-billing setup/);
  }
});

test('ambiguous activation options fail before installing or authenticating', () => {
  for (const flags of [['--spark', '--blaze'], ['--installed', '--installed'], ['--email'], ['--email', '--blaze'], ['--unknown']]) {
    const result = run('activate-project.sh', flags);
    assert.equal(result.status, 1);
    assert.deepEqual(result.calls, []);
  }
});

test('explicit billed activation preserves the full core deployment and verification path', () => {
  const result = run('activate-project.sh', ['--blaze', '--installed', '--email', 'admin@example.com']);
  assert.equal(result.status, 0, result.stderr);
  const deploy = result.calls.find(c => c.startsWith('firebase deploy'));
  assert.match(deploy, /--only firestore,storage,functions:joinSchool,/);
  assert.match(deploy, /functions:chatNotification$/);
  assert.doesNotMatch(deploy, /createProOrder|verifyProPayment|paymentWebhook/);
  assert.ok(result.calls.includes('node scripts/check-backend.cjs'));
});
