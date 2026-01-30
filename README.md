# HaiTale — AI-Powered Mod Helper for HyTale

HaiTale helps you find and install mods for HyTale by describing the kind of world you want to create. Tell it in plain English (for example: "medieval world with dragons and magic") and it suggests mods — then you can install the ones you like.

This short guide is for non-technical users and shows only what you need to run the ready-made JAR downloaded from the project's Releases on GitHub.

---

## 1) Get a free OpenRouter API key (optional but recommended)

HaiTale can use a free OpenRouter AI model to provide smarter recommendations. If you want AI-powered suggestions, get a free key (no credit card required):

1. Go to https://openrouter.ai/ and create an account
2. Open your API keys page and create/copy a key

If you don't set a key, HaiTale still works using simple keyword matching.

---

## 2) Set your API key locally

Set the API key as an environment variable before running the JAR.

- macOS / Linux (temporary for current terminal):

```bash
export OPENROUTER_API_KEY="sk-or-v1-your-key-here"
```

- macOS / Linux (persistent for future terminals — add to your ~/.zshrc):

```bash
echo 'export OPENROUTER_API_KEY="sk-or-v1-your-key-here"' >> ~/.zshrc
source ~/.zshrc
```

- Windows (PowerShell temporary):

```powershell
$env:OPENROUTER_API_KEY = "sk-or-v1-your-key-here"
```

Important: Do not commit or share your API key. If it was accidentally shared, revoke/rotate it at OpenRouter immediately.

---

## 3) Download the ready-made JAR

HaiTale releases built JARs on GitHub Releases whenever the project maintainer publishes a tagged release. To download:

1. Open the project on GitHub: https://github.com/<your-repo-owner>/haitale
2. Click the **Releases** tab
3. Download the latest `haitale-<tag>.jar` asset (for example `haitale-v1.2.0.jar`)

If you prefer a direct link to the release, the Release page will show the download URL.

---

## 4) Run the JAR (examples)

Open a terminal and run the downloaded JAR with Java 21 (or newer).

- Get AI-powered recommendations (if API key set):

```bash
java -jar haitale-v1.2.0.jar recommend "I want a medieval fantasy world with dragons and magic"
```

- Search for mods by keyword:

```bash
java -jar haitale-v1.2.0.jar search magic
```

- Install mod(s) by ID (IDs are shown in recommendation/search output):

```bash
java -jar haitale-v1.2.0.jar install magic-realms-2 fantasy-creatures-6
```

- List installed mods:

```bash
java -jar haitale-v1.2.0.jar list
```

Replace `haitale-v1.2.0.jar` with the actual filename you downloaded.

---

## Safety & privacy

- HaiTale only installs mods with free/open-source licenses (MIT, GPL, Apache, etc.).
- Every download is checksum-verified to reduce the risk of corrupted or tampered files.
- The tool creates backups of your mods folder before installing anything.
- Your OpenRouter API key is kept local — do not commit it to source control.

---

## Need help?

If you'd like:
- A one-line helper script to set the API key and run a sample command, or
- A short GIF showing how to download the JAR from Releases and run it — I can create that for you.

Tell me which and I’ll add it.
