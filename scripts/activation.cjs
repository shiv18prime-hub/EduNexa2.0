'use strict';

const PROJECT = 'student-result-pro';
const REGION = 'asia-south1';
const WEB_DOMAIN = 'edunexa-school-hub.shiv18prime.chatgpt.site';
const PROBES = ['dashboard', 'joinSchool', 'studentTests', 'sendMessage', 'redeemCoupon'];

function adminArguments(args) {
  const result = {apply: false};
  for (let i = 0; i < args.length; i++) {
    const flag = args[i];
    if (flag === '--apply' && !result.apply) { result.apply = true; continue; }
    if (!['--uid', '--email'].includes(flag) || result.uid || result.email) {
      throw Error('Choose exactly one --uid EXISTING_UID or --email EXISTING_EMAIL; optionally add --apply.');
    }
    const value = args[++i];
    if (!value || value.startsWith('--') || value.trim() !== value) throw Error('Missing or invalid account selector.');
    result[flag.slice(2)] = value;
  }
  if (!result.uid && !result.email) throw Error('An existing administrator account must be selected explicitly.');
  return result;
}

function verifiedAdministrator(account, selection) {
  if (!account?.uid || account.disabled || !account.email || account.emailVerified !== true) {
    throw Error('Choose an enabled account with a verified email. Sign into EduNexa with Google first.');
  }
  if ((selection.uid && account.uid !== selection.uid) ||
      (selection.email && account.email.toLowerCase() !== selection.email.toLowerCase())) {
    throw Error('Firebase returned a different account. Administrator access was not granted.');
  }
  return account;
}

function authorizedDomains(config) {
  const existing = config.authorizedDomains ?? [];
  if (!Array.isArray(existing) || existing.some(domain => typeof domain !== 'string')) {
    throw Error('Unexpected Firebase authorized-domain configuration; no update performed.');
  }
  return existing.includes(WEB_DOMAIN) ? [...existing] : [...existing, WEB_DOMAIN];
}

async function probeBackend(fetcher = fetch, names = PROBES) {
  return Promise.all(names.map(async name => {
    try {
      const response = await fetcher(`https://${REGION}-${PROJECT}.cloudfunctions.net/${name}`, {
        method: 'POST', headers: {'Content-Type': 'application/json'}, body: JSON.stringify({data: {}}),
        signal: AbortSignal.timeout(15000), redirect: 'error'
      });
      const body = await response.json().catch(() => null);
      const ready = response.status === 401 && body?.error?.status === 'UNAUTHENTICATED';
      return {name, ready, status: response.status, detail: ready ? 'reachable; authentication required' : 'expected HTTP 401 / UNAUTHENTICATED'};
    } catch (error) {
      return {name, ready: false, detail: error.name === 'TimeoutError' ? 'request timed out' : 'request failed'};
    }
  }));
}

module.exports = {PROJECT, REGION, WEB_DOMAIN, PROBES, adminArguments, verifiedAdministrator, authorizedDomains, probeBackend};
