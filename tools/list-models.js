const https = require('https');

const apiKey = 'AIzaSyAupKvqZa9IEmrHTsP74ogmWbfJoh1XZCY';
const url = `https://generativelanguage.googleapis.com/v1beta/models?key=${apiKey}`;

console.log("Listing models...");
https.get(url, (res) => {
  let responseData = '';
  res.on('data', (chunk) => {
    responseData += chunk;
  });
  res.on('end', () => {
    try {
      const parsed = JSON.parse(responseData);
      if (parsed.models) {
        console.log("Available models:");
        parsed.models.forEach(m => {
          console.log(`- ${m.name} (${m.displayName}) - supports: ${m.supportedGenerationMethods.join(', ')}`);
        });
      } else {
        console.log(JSON.stringify(parsed, null, 2));
      }
    } catch (e) {
      console.log(responseData);
    }
  });
}).on('error', (e) => {
  console.error(e);
});
