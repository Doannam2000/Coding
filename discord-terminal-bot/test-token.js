const { execSync } = require('child_process');

console.log('Testing OpenCode token parsing...\n');

try {
  const output = execSync('opencode run hello --pure --format json --model opencode/minimax-m2.5-free', {
    encoding: 'utf8',
    timeout: 30000,
    windowsHide: true
  });

  console.log('=== RAW OUTPUT ===');
  console.log(output);
  console.log('\n=== PARSING ===');

  const lines = output.split('\n').filter(l => l.trim());

  for (const line of lines) {
    try {
      const data = JSON.parse(line);
      console.log('JSON type:', data.type);
      if (data.type === 'step_finish') {
        console.log('FOUND step_finish!');
        console.log('tokens:', JSON.stringify(data.part?.tokens, null, 2));
      }
    } catch (e) {
      // skip invalid JSON
    }
  }
} catch (error) {
  console.error('Error:', error.message);
}
