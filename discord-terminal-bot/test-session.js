const http = require('http');

const postData = JSON.stringify({
  parts: [{ type: 'text', text: 'Say hello in 10 words' }],
  model: 'opencode/big-pickle',
});

const options = {
  hostname: '127.0.0.1',
  port: 4096,
  path: '/session/ses_test123/message',
  method: 'POST',
  headers: {
    'Content-Type': 'application/json',
    'Content-Length': Buffer.byteLength(postData),
  },
};

const { spawn } = require('child_process');
const proc = spawn('powershell.exe', [
  '-NoProfile', '-ExecutionPolicy', 'Bypass', '-Command', 'opencode serve --port 4096'
], { stdio: 'ignore', detached: true });
proc.unref();

setTimeout(() => {
  console.log('Creating session...');
  
  const createReq = http.request({
    hostname: '127.0.0.1',
    port: 4096,
    path: '/session',
    method: 'POST',
    headers: { 'Content-Type': 'application/json', 'Content-Length': '2' },
  }, (res) => {
    let data = '';
    res.on('data', chunk => data += chunk);
    res.on('end', () => {
      console.log('Session response:', data);
      try {
        const session = JSON.parse(data);
        console.log('Session ID:', session.id);
        
        const msgOptions = {
          ...options,
          path: `/session/${session.id}/message`,
        };
        
        const msgReq = http.request(msgOptions, (msgRes) => {
          console.log('Message status:', msgRes.statusCode);
          let msgData = '';
          msgRes.on('data', chunk => {
            msgData += chunk.toString();
            process.stdout.write(chunk.toString().substring(0, 500) + '\n');
          });
          msgRes.on('end', () => {
            console.log('\n--- FULL RESPONSE ---');
            console.log(msgData);
            process.exit(0);
          });
        });
        
        msgReq.on('error', (e) => {
          console.error('Message error:', e.message);
          process.exit(1);
        });
        
        msgReq.write(postData);
        msgReq.end();
        
      } catch (e) {
        console.error('Parse error:', e.message);
        process.exit(1);
      }
    });
  });
  
  createReq.on('error', (e) => {
    console.error('Session error:', e.message);
    process.exit(1);
  });
  
  createReq.write('{}');
  createReq.end();
  
}, 5000);
