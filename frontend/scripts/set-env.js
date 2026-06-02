const fs = require('fs');
const path = require('path');

const targetDir = path.join(__dirname, '../src/environments');
const targetFile = path.join(targetDir, 'environment.prod.ts');

// VITE_API_URL = https://api.managerpitstop.com.br/api  →  apiUrl = .../api/v1
const apiUrl = (process.env.VITE_API_URL || '').replace(/\/$/, '');

if (!apiUrl) {
  console.error('[set-env] ERRO: VITE_API_URL não está definido. Configure na Vercel: VITE_API_URL=https://api.managerpitstop.com.br/api');
  process.exitCode = 1;
}

fs.mkdirSync(targetDir, { recursive: true });

const content = `export const environment = {
  production: true,
  apiUrl: '${apiUrl}/v1',
};
`;

fs.writeFileSync(targetFile, content, 'utf8');

console.log(`[set-env] apiUrl configurado para: ${apiUrl || '(vazio!)'}/v1`);
