'use strict';
const {PROJECT, WEB_DOMAIN, authorizedDomains} = require('./activation.cjs');
const CONFIG_URL = `https://identitytoolkit.googleapis.com/admin/v2/projects/${PROJECT}/config`;

async function authorizeWeb(credential, apply, fetcher = fetch, log = console.log) {
  async function request(url, options = {}) {
    const token = await credential.getAccessToken();
    const response = await fetcher(url, {...options, headers: {
      'Authorization': `Bearer ${token.access_token}`, 'Content-Type': 'application/json'
    }, signal: AbortSignal.timeout(15000), redirect: 'error'});
    if (!response.ok) throw Error(`Firebase Authentication configuration returned HTTP ${response.status}. Check owner permissions.`);
    return response.json();
  }
  const config = await request(CONFIG_URL);
  const domains = authorizedDomains(config);
  if (config.authorizedDomains?.includes(WEB_DOMAIN)) { log('Web domain is already authorized.'); return; }
  if (!apply) { log(`Dry run: authorize ${WEB_DOMAIN}, preserving all existing domains and sign-in providers.`); return; }
  // A field mask ensures unrelated provider, password, MFA and redirect settings remain intact.
  const updated = await request(`${CONFIG_URL}?updateMask=authorizedDomains`, {
    method: 'PATCH', body: JSON.stringify({authorizedDomains: domains})
  });
  if (!updated.authorizedDomains?.includes(WEB_DOMAIN)) throw Error('Web authorization could not be confirmed.');
  log(`Authorized web domain: ${WEB_DOMAIN}.`);
}

if (require.main === module) {
  (async () => {
    const args = process.argv.slice(2);
    if (args.some(arg => arg !== '--apply') || args.length > 1) throw Error('Usage: node scripts/authorize-web.cjs [--apply]');
    const {applicationDefault} = require('../functions/node_modules/firebase-admin/app');
    await authorizeWeb(applicationDefault(), args.includes('--apply'));
  })().catch(error => { console.error(`Web authorization stopped: ${error.code || error.message}`); process.exitCode = 1; });
}

module.exports = {authorizeWeb};
