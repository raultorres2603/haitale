# HaiTale — AI-Powered Mod Helper for HyTale

[![Latest Release](https://img.shields.io/github/v/release/raultorres2603/haitale?style=flat-square)](https://github.com/raultorres2603/haitale/releases/latest)

[![Download latest JAR](https://img.shields.io/badge/download-latest-brightgreen?style=flat-square)](https://github.com/raultorres2603/haitale/releases/latest/download/haitale-latest.jar)

[![Verify checksum](https://img.shields.io/badge/verify-checksum-blue?style=flat-square)](https://github.com/raultorres2603/haitale/releases/latest/download/haitale-latest.jar.sha256)

HaiTale helps you find and install mods for HyTale by describing the kind of world you want to create. Tell it in plain English (for example: "medieval world with dragons and magic") and it suggests mods from CurseForge — then you can install the ones you like.

This short guide is for non-technical users and shows only what you need to run the ready-made JAR downloaded from the project's Releases on GitHub.

---

**Download the latest JAR:**

- Click here to go straight to the latest release and download the JAR: https://github.com/raultorres2603/haitale/releases/latest

**Download the checksum:**

- Verify the JAR you downloaded by comparing its SHA-256 checksum with the one published here: https://github.com/raultorres2603/haitale/releases/latest/download/haitale-latest.jar.sha256

---

## 1) Get API Keys

### CurseForge API Key (Required)

HaiTale fetches Hytale mods from CurseForge. You need a free API key:

1. Go to https://console.curseforge.com/
2. Create an account or log in
3. Navigate to "API Keys" section
4. Create a new API key
5. Copy your API key

### OpenRouter API Key (Optional but recommended)

For AI-powered mod recommendations:

1. Go to https://openrouter.ai/ and create an account
2. Open your API keys page and create/copy a key

If you don't set an OpenRouter key, HaiTale still works using simple keyword matching.

---

## 2) Set your API keys locally

Set the API keys as environment variables before running the JAR.

- macOS / Linux (temporary for current terminal):

```bash
export CURSEFORGE_API_KEY="your-curseforge-key-here"
export OPENROUTER_API_KEY="sk-or-v1-your-key-here"  # Optional
```

- macOS / Linux (persistent for future terminals — add to your ~/.zshrc):

```bash
echo 'export CURSEFORGE_API_KEY="your-curseforge-key-here"' >> ~/.zshrc
echo 'export OPENROUTER_API_KEY="sk-or-v1-your-key-here"' >> ~/.zshrc  # Optional
source ~/.zshrc
```

- Windows (Command Prompt):

```cmd
setx CURSEFORGE_API_KEY "your-curseforge-key-here"
setx OPENROUTER_API_KEY "sk-or-v1-your-key-here"
```

- macOS (make the keys available to GUI apps launched from Finder/Dock):

Create `~/Library/LaunchAgents/com.haitale.env.plist` with your variables under `EnvironmentVariables`, then load it:

```bash
cat > ~/Library/LaunchAgents/com.haitale.env.plist <<'PLIST'
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE plist PUBLIC "-//Apple Computer//DTD PLIST 1.0//EN" "">
<plist version="1.0">
  <dict>
    <key>Label</key>
    <string>com.haitale.env</string>
    <key>EnvironmentVariables</key>
    <dict>
      <key>OPENROUTER_API_KEY</key>
      <string>sk-or-v1-your-key-here</string>
    </dict>
  </dict>
</plist>
PLIST

# Load it
launchctl unload ~/Library/LaunchAgents/com.haitale.env.plist 2>/dev/null || true
launchctl load ~/Library/LaunchAgents/com.haitale.env.plist
```

- Linux (persistent for future terminals): add to your shell profile (`~/.bashrc`, `~/.profile`, or `~/.bash_profile` depending on distro):

```bash
echo 'export OPENROUTER_API_KEY="sk-or-v1-your-key-here"' >> ~/.bashrc
# then reload
source ~/.bashrc
```

- Windows (PowerShell persistent for current user):

```powershell
setx OPENROUTER_API_KEY "sk-or-v1-your-key-here"
# restart PowerShell/Terminal to pick it up
```

- Windows (PowerShell profile):

```powershell
Add-Content -Path $PROFILE -Value 'setx OPENROUTER_API_KEY "sk-or-v1-your-key-here"'
```

Alternatives and secure options

- Use a local `.env` file (convenient for per-project use): create a `.env` file in your project and add it to `.gitignore`:

```
# .env
OPENROUTER_API_KEY=sk-or-v1-your-key-here
```

Then load it before running the JAR (example using `direnv` or a small shell helper script). Make sure `.env` is in `.gitignore`.

- macOS Keychain (recommended for more security): store and retrieve the key using the `security` CLI and a small wrapper script so the key is not left in plain text files.

```bash
# store (one-time)
security add-generic-password -a "$USER" -s "openrouter-api" -w "sk-or-v1-your-key-here"
# retrieve (in a script)
OPENROUTER_API_KEY=$(security find-generic-password -a "$USER" -s "openrouter-api" -w)
export OPENROUTER_API_KEY
```

Important security notes

- Never commit your API key to Git. If you accidentally commit it, rotate/revoke it immediately at OpenRouter and remove it from git history.
- Prefer environment variables, Keychain, or a secrets manager for production use rather than storing the key inside files in the repository.

---

## 3) When is a new JAR built (and where to download it)

We build and publish a ready-to-run JAR automatically whenever code is pushed to the `master` branch. Each push creates an automated GitHub Release with the built JAR attached.

- How it works (simple):
  - You (or a maintainer) push to `master` → GitHub Actions runs a build → a Release is created for that commit and the JAR is attached.
  - You can also run the workflow manually from GitHub's Actions tab.

- Where to download:
  1. Open the project on GitHub: https://github.com/raultorres2603/haitale
  2. Click the **Releases** tab
  3. Download the latest `haitale-<commit-sha>.jar` or click the badge above to download the latest stable JAR (`haitale-latest.jar`).

This means a new JAR is produced on every push to `master` (or when manually triggered). If you want to publish only on version bumps instead, tell me and I can change the process to build only on tags.

---

## 4) Run the JAR (examples)

Open a terminal and run the downloaded JAR with Java 21 (or newer).

- Get AI-powered recommendations (if API key set):

```bash
java -jar haitale-<commit-sha>.jar recommend "I want a medieval fantasy world with dragons and magic"
```

- Search for mods by keyword:

```bash
java -jar haitale-<commit-sha>.jar search magic
```

- Install mod(s) by ID (IDs are shown in recommendation/search output):

```bash
java -jar haitale-<commit-sha>.jar install magic-realms-2 fantasy-creatures-6
```

- List installed mods:

```bash
java -jar haitale-<commit-sha>.jar list
```

Replace `haitale-<commit-sha>.jar` with the actual filename you downloaded from Releases.

---

## Safety & privacy

- HaiTale only installs mods with free/open-source licenses (MIT, GPL, Apache, etc.).
- Every download is checksum-verified to reduce the risk of corrupted or tampered files.
- The tool creates backups of your mods folder before installing anything.
- Your OpenRouter API key is kept local — do not commit it to source control.

---

## Need help?

If you'd like:
- A one-line helper example to set the API key and run a sample command, or
- A short GIF showing how to download the JAR from Releases and run it — I can create that for you.

Tell me which and I’ll add it.
