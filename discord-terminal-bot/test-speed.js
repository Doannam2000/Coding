const { execSync } = require('child_process');

console.log('Testing different methods...\n');

const test1 = () => {
  console.log('=== Method 1: Direct opencode ===');
  const start = Date.now();
  try {
    const output = execSync('opencode run --pure --format json --model opencode/minimax-m2.5-free hello', {
      encoding: 'utf8',
      timeout: 30000,
      windowsHide: true,
    });
    console.log('Time:', Date.now() - start, 'ms');
    console.log('Success! Length:', output.length);
    return true;
  } catch (e) {
    console.log('Error:', e.message);
    return false;
  }
};

const test2 = () => {
  console.log('\n=== Method 2: cmd /c ===');
  const start = Date.now();
  try {
    const output = execSync('cmd /c "opencode run --pure --format json --model opencode/minimax-m2.5-free hello"', {
      encoding: 'utf8',
      timeout: 30000,
      windowsHide: true,
    });
    console.log('Time:', Date.now() - start, 'ms');
    console.log('Success! Length:', output.length);
    return true;
  } catch (e) {
    console.log('Error:', e.message);
    return false;
  }
};

test1();
test2();
