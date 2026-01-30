# HaiTale - AI-Powered HyTale Mod Installer

HaiTale is an intelligent command-line tool that uses AI to recommend and install HyTale mods based on natural language descriptions. Simply describe the world you want to create, and HaiTale will suggest the best mods to match your vision.

## Features

- **🤖 AI-Powered Recommendations**: Uses OpenRouter with free AI models (LLaMA, Gemini, etc.) for intelligent mod suggestions
- **🔒 Security First**: 
  - Only installs mods with free/open-source licenses
  - Verifies SHA-256 checksums for every download
  - Prevents malware and tampered files
- **⚡ Easy Installation**: One command to install multiple mods
- **💾 Automatic Backups**: Creates backups before modifying your mods directory
- **📋 Installation Tracking**: Maintains a manifest of installed mods with metadata
- **🎯 Fallback Mode**: Works without API key using keyword-based matching

## Quick Start

### Prerequisites
- Java 21 or higher
- OpenRouter API key (free) - [Get yours here](https://openrouter.ai/keys)

### Setup

1. **Clone and build:**
```bash
git clone <repository-url>
cd HaiTale
./gradlew build
```

2. **Set your OpenRouter API key:**
```bash
export OPENROUTER_API_KEY="sk-or-v1-your-key-here"
```

📚 **Detailed setup instructions:** See [SETUP.md](SETUP.md)

### First Command

```bash
./gradlew run --args="recommend I want a magical fantasy world with dragons and epic quests"
```

## Usage

### Get Mod Recommendations

Describe the world you want to create, and HaiTale will recommend suitable mods:

```bash
# Basic recommendation
haitale recommend I want a medieval fantasy world with magic and dragons

# Building-focused world
haitale recommend A creative world with advanced building tools and blueprints

# Tech-focused world
haitale recommend A futuristic world with automation and machinery
```

### Install Mods

Install one or more mods using their IDs (found in search or recommend results):

```bash
# Install a single mod
haitale install enhanced-building-1

# Install multiple mods
haitale install magic-realms-2 fantasy-creatures-6

# Skip confirmation prompt
haitale install -y tech-revolution-3
```

### Search for Mods

Search for mods by keyword:

```bash
haitale search magic
haitale search building tools
haitale search adventure
```

### List Installed Mods

View all currently installed mods:

```bash
haitale list
```

## How It Works

### AI Recommendation Engine

HaiTale uses **OpenRouter** to access powerful AI models for intelligent mod recommendations:

1. **AI Analysis** (when API key is configured):
   - Sends your world description to a free AI model (LLaMA 3.2, Gemini 2.0, etc.)
   - AI analyzes available mods and matches them to your requirements
   - Returns recommendations with relevance scores and reasoning
   - Considers context, themes, and mod synergies

2. **Fallback Mode** (when no API key):
   - Uses rule-based keyword matching
   - Analyzes categories: Building, adventure, technology, magic
   - Matches themes: Medieval, fantasy, sci-fi, etc.
   - Still provides useful recommendations

**AI Models Used:** HaiTale uses free models from OpenRouter (no credit card required):
- Meta LLaMA 3.2 3B Instruct (default)
- Google Gemini 2.0 Flash
- Mistral 7B Instruct
- Qwen 2 7B Instruct

### Security Verification

Every mod download is verified for safety:

1. **License Check**: Only mods with approved open-source licenses (MIT, Apache, GPL, etc.) can be installed
2. **Checksum Verification**: SHA-256 checksums are verified against repository metadata
3. **Download Integrity**: Files are quarantined and verified before installation
4. **Backup Creation**: Automatic backups before any modifications

### Mod Sources

HaiTale supports multiple mod repositories:
- **Modrinth** (planned)
- **CurseForge** (planned)
- **GitHub Releases** (planned)

*Currently uses sample data for demonstration. Real repository integration coming soon.*

## Configuration

Edit `src/main/resources/application.properties` to configure:

```properties
# OpenRouter AI Configuration
openrouter.api.url=https://openrouter.ai/api/v1/chat/completions
openrouter.api.key=${OPENROUTER_API_KEY:}
openrouter.model=meta-llama/llama-3.2-3b-instruct:free
openrouter.site.url=https://github.com/yourusername/haitale
openrouter.site.name=HaiTale

# Mod Repository Configuration
mod.repository.cache.enabled=true
mod.repository.modrinth.enabled=true
mod.repository.curseforge.enabled=true

# Security Settings
mod.security.checksum.required=true
mod.security.free-license.required=true
```

**Change AI Model:**
```properties
# Try different free models
openrouter.model=google/gemini-2.0-flash-exp:free
openrouter.model=mistralai/mistral-7b-instruct:free
```

See [SETUP.md](SETUP.md) for detailed configuration instructions.

## Project Structure

```
src/
├── main/java/ai/haitale/
│   ├── HaitaleCommand.java          # Main CLI entry point
│   ├── commands/                     # CLI subcommands
│   │   ├── RecommendCommand.java     # AI recommendation command
│   │   ├── InstallCommand.java       # Mod installation command
│   │   ├── SearchCommand.java        # Mod search command
│   │   └── ListCommand.java          # List installed mods
│   ├── model/                        # Domain models
│   │   ├── Mod.java                  # Mod metadata
│   │   ├── ModRecommendation.java    # AI recommendation result
│   │   ├── WorldPreferences.java     # User preferences
│   │   └── InstallationManifest.java # Installation tracking
│   └── service/                      # Business logic
│       ├── AIRecommendationService.java    # AI-powered recommendations
│       ├── ModRepositoryService.java       # Mod catalog management
│       ├── ModDownloadService.java         # Secure download & verification
│       └── ModInstallationService.java     # Installation management
```

## Technology Stack

- **Framework**: Micronaut 4.10.7
- **CLI**: Picocli
- **Serialization**: Micronaut Serde (Jackson)
- **Build**: Gradle 8.14
- **Java**: 21
- **Testing**: JUnit 5

## Future Enhancements

- [ ] Integration with real mod repositories (Modrinth, CurseForge APIs)
- [ ] LLM integration for advanced AI recommendations (OpenAI, Anthropic, Ollama)
- [ ] Dependency resolution between mods
- [ ] Mod versioning and updates
- [ ] Conflict detection
- [ ] GUI interface
- [ ] Mod pack creation and sharing
- [ ] Community ratings and reviews integration

## Safety & Security

HaiTale takes security seriously:

- **License Verification**: Only free/open-source mods can be installed
- **Checksum Validation**: All downloads are verified with SHA-256 hashes
- **Automatic Backups**: Your mods directory is backed up before changes
- **No Root Access**: Runs with user permissions only
- **Transparent Operations**: All actions are logged and visible

## Contributing

Contributions are welcome! Please:

1. Fork the repository
2. Create a feature branch
3. Add tests for new functionality
4. Submit a pull request

## License

This project is open-source. License details to be determined.

## Support

For issues, questions, or suggestions, please open an issue on the project repository.

---

**Note**: HyTale is a trademark of Hypixel Studios. This is an independent fan project and is not affiliated with or endorsed by Hypixel Studios.
