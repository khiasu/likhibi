const https = require('https');

const apiKey = 'AIzaSyAupKvqZa9IEmrHTsP74ogmWbfJoh1XZCY';
const url = `https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=${apiKey}`;

const data = JSON.stringify({
  contents: [
    {
      parts: [
        {
          text: "You are a keyboard assistant for Nagamese language.\nNagamese is a creole spoken in Nagaland, India.\nIt mixes Assamese, Hindi, tribal languages and English freely.\nSpelling is inconsistent — accept all variants.\nThe user has typed: Moi\nSuggest the 3 most likely next words.\nReply with exactly 3 words separated by commas.\nNo explanation. No punctuation. Just the 3 words."
        }
      ]
    }
  ],
  generationConfig: {
    temperature: 0.2,
    maxOutputTokens: 24,
    thinkingConfig: {
      thinkingBudget: 0
    }
  }
});

const options = {
  method: 'POST',
  headers: {
    'Content-Type': 'application/json',
    'Content-Length': Buffer.byteLength(data)
  }
};

console.log("Sending request to Gemini API...");
const req = https.request(url, options, (res) => {
  console.log(`Status Code: ${res.statusCode}`);
  let responseData = '';

  res.on('data', (chunk) => {
    responseData += chunk;
  });

  res.on('end', () => {
    console.log("Response data:");
    try {
      const parsed = JSON.parse(responseData);
      console.log(JSON.stringify(parsed, null, 2));
    } catch (e) {
      console.log(responseData);
    }
  });
});

req.on('error', (error) => {
  console.error('Error:', error);
});

req.write(data);
req.end();
