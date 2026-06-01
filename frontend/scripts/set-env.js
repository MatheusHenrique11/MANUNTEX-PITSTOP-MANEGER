const fs = require('fs');
const path = require('path');

const targetDir = path.join(__dirname, '../src/environments');
const targetFile = path.join(targetDir, 'environment.prod.ts');
const backendUrl = (
  process.env.API_BASE_URL ||
  process.env.RAILWAY_BACKEND_URL ||
  ''
).replace(/\/$/, '');

if (!backendUrl) {
  console.error('[set-env] ERRO: API_BASE_URL não está definido. O build vai usar URL relativa e falhar em produção.');
  process.exitCode = 1;
}

fs.mkdirSync(targetDir, { recursive: true });

const content = `export const environment = {
  production: true,
  apiUrl: '${backendUrl}/api/v1',
};
`;

fs.writeFileSync(targetFile, content, 'utf8');

console.log(`[set-env] apiUrl configurado para: ${backendUrl || '(vazio!)'}/api/v1`);
