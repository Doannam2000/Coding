const { aiService } = require('./dist/services');

async function test() {
  console.log('Starting AI service test...');
  
  const ok = await aiService.initialize();
  if (!ok) {
    console.log('Failed to initialize');
    return;
  }
  
  console.log('Testing chat...');
  const response = await aiService.chat('Say hello in 10 words', (chunk) => {
    console.log('Chunk:', chunk.slice(-100));
  });
  
  console.log('Final:', response.text);
  console.log('Tokens:', response.tokens);
  
  await aiService.shutdown();
}

test().catch(console.error);
