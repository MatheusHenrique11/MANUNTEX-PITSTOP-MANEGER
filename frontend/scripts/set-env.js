const fs = require('fs');
const path = require('path');

const envFile = path.join(__dirname, '../src/environments/environment.prod.ts');
const backendUrl = (process.env.RAILWAY_BACKEND_URL || '').replace(/\/$/, '');

if (!backendUrl) {
  console.warn('[set-env] RAILWAY_BACKEND_URL não definido — usando placeholder vazio.');
}

let content = fs.readFileSync(envFile, 'utf8');
content = content.replace('RAILWAY_BACKEND_URL', backendUrl);
fs.writeFileSync(envFile, content);

console.log(`[set-env] apiUrl configurado para: ${backendUrl}/api/v1`);
