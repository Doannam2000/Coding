const { terminalService } = require('./dist/services/TerminalService');

async function main() {
  let streamed = '';
  const result = await terminalService.executeCommand(
    'utf8-terminal-smoke',
    'tester',
    'node utf8-output-smoke.js',
    (chunk) => {
      streamed += chunk;
    },
    () => {}
  );

  console.log('streamed=' + JSON.stringify(streamed.trim()));
  console.log('final=' + JSON.stringify(result.trim()));
}

main().catch((error) => {
  console.error(error);
  process.exit(1);
});
