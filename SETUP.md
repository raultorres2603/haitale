# HaiTale Setup Guide

## Getting Your OpenRouter API Key

HaiTale uses OpenRouter to access free AI models for intelligent mod recommendations. Follow these steps to get started:

### Step 1: Sign Up for OpenRouter

1. Go to [OpenRouter.ai](https://openrouter.ai/)
2. Click "Sign Up" or "Get Started"
3. Create an account (you can use GitHub, Google, or email)

### Step 2: Get Your API Key

1. Once logged in, go to your [API Keys page](https://openrouter.ai/keys)
2. Click "Create Key"
3. Give your key a name (e.g., "HaiTale")
4. Copy the API key (it starts with `sk-or-...`)

### Step 3: Set Up Your Environment

You have two options to provide your API key:

#### Option 1: Environment Variable (Recommended)

**macOS/Linux:**
```bash
export OPENROUTER_API_KEY="sk-or-v1-your-key-here"
```

To make it permanent, add it to your `~/.zshrc` or `~/.bashrc`:
```bash
echo 'export OPENROUTER_API_KEY="sk-or-v1-your-key-here"' >> ~/.zshrc
source ~/.zshrc
```

**Windows (PowerShell):**
```powershell
$env:OPENROUTER_API_KEY="sk-or-v1-your-key-here"
```

To make it permanent, add it to your system environment variables:
1. Search for "Environment Variables" in Windows
2. Click "Environment Variables"
3. Under "User variables", click "New"
4. Variable name: `OPENROUTER_API_KEY`
5. Variable value: `sk-or-v1-your-key-here`

#### Option 2: Configuration File

Edit `src/main/resources/application.properties`:
```properties
openrouter.api.key=sk-or-v1-your-key-here
```

**⚠️ Warning:** Don't commit your API key to version control!

### Step 4: Verify Setup

Run HaiTale with a test recommendation:
```bash
./gradlew run --args="recommend I want a magical fantasy world with dragons"
```

If you see "Using AI-powered recommendations" in the logs, it's working!

## Free Models Available

HaiTale is configured to use free models by default:

- **meta-llama/llama-3.2-3b-instruct:free** (Default)
- **google/gemini-2.0-flash-exp:free**
- **mistralai/mistral-7b-instruct:free**
- **qwen/qwen-2-7b-instruct:free**

To change the model, edit `application.properties`:
```properties
openrouter.model=google/gemini-2.0-flash-exp:free
```

## OpenRouter Credits

OpenRouter provides free tier usage for all users:
- No credit card required for free models
- Free models are marked with `:free` suffix
- Check [OpenRouter models page](https://openrouter.ai/models) for latest free options

## Troubleshooting

### "OpenRouter API key not configured"
- Make sure your environment variable is set
- Restart your terminal after setting the variable
- Verify with: `echo $OPENROUTER_API_KEY` (macOS/Linux) or `echo $env:OPENROUTER_API_KEY` (Windows)

### "Error calling OpenRouter API"
- Check your internet connection
- Verify your API key is valid
- Check OpenRouter's [status page](https://status.openrouter.ai/)

### "Using fallback rule-based recommendations"
- This means HaiTale couldn't reach OpenRouter
- The tool will still work with keyword-based matching
- Check your API key configuration

## Privacy

- Your API key is never logged or shared
- Queries are sent directly to OpenRouter's API
- No personal data is collected by HaiTale
- Check [OpenRouter's privacy policy](https://openrouter.ai/privacy) for their data handling
