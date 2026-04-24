const { aiService } = require('./dist/services/AIService');

async function main() {
  aiService.setCLI('codex');

  let streamed = '';
  const response = await aiService.chat(
    'Reply with exactly: smoke-test-ok',
    (chunk) => {
      streamed = chunk;
    },
    { workdir: process.cwd() }
  );

  const text = (response?.text || '').trim();
  console.log('CLI:', aiService.getCLI());
  console.log('Streamed:', streamed.trim());
  console.log('Final:', text);

  if (!text) {
    throw new Error('Codex returned empty text');
  }

  if (!/smoke-test-ok/i.test(text)) {
    throw new Error(`Unexpected Codex output: ${text}`);
  }

  console.log('PASS: codex chat smoke test');
}

main().catch((error) => {
  console.error('FAIL:', error?.message || error);
  process.exit(1);
});
