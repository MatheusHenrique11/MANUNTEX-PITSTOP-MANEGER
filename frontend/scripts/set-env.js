const fs = require('fs');
const path = require('path');

const targetDir = path.join(__dirname, '../src/environments');
const targetFile = path.join(targetDir, 'environment.prod.ts');
const backendUrl = (process.env.RAILWAY_BACKEND_URL || '').replace(/\/$/, '');

if (!backendUrl) {
  console.warn('[set-env] RAILWAY_BACKEND_URL não definido — usando string vazia.');
}

fs.mkdirSync(targetDir, { recursive: true });

const content = `export const environment = {
  production: true,
  apiUrl: '${backendUrl}/api/v1',
};
`;

fs.writeFileSync(targetFile, content, 'utf8');

console.log(`[set-env] apiUrl configurado para: ${backendUrl}/api/v1`);
