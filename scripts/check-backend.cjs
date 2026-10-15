'use strict';
const {PROJECT, probeBackend} = require('./activation.cjs');

(async () => {
  const results = await probeBackend();
  for (const result of results) console.log(`${result.ready ? 'PASS' : 'FAIL'} ${result.name}: ${result.status ?? '-'}; ${result.detail}`);
  if (results.some(result => !result.ready)) {
    console.error(`Core backend is not ready at the configured ${PROJECT} endpoints. Check deployment logs and rerun this command.`);
    process.exitCode = 1;
  } else {
    console.log('Core endpoints are reachable and reject unauthenticated calls. Run the signed-in acceptance checks in DEPLOYMENT.md next.');
  }
})();
