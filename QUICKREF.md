# HaiTale Quick Reference

## 🚀 Quick Start (3 Steps)

```bash
# 1. Get free API key from https://openrouter.ai/keys
export OPENROUTER_API_KEY="sk-or-v1-your-key-here"

# 2. Build the project
./gradlew build

# 3. Run a recommendation
java -jar build/libs/haitale-0.1-all.jar recommend "magical fantasy world with dragons"
```

## 📋 All Commands

| Command | Description | Example |
|---------|-------------|---------|
| `recommend <description>` | Get AI-powered mod suggestions | `recommend medieval world with magic` |
| `install <mod-ids...>` | Install one or more mods | `install magic-realms-2 tech-revolution-3` |
| `search <keywords>` | Search for mods | `search building tools` |
| `list` | Show installed mods | `list` |
| `--help` | Show help | `--help` |

## 🎯 Common Use Cases

### Get recommendations for your world
```bash
java -jar haitale-0.1-all.jar recommend "I want a steampunk world with airships and Victorian buildings"
```

### Install recommended mods
```bash
java -jar haitale-0.1-all.jar install enhanced-building-1 tech-revolution-3
```

### Install without confirmation
```bash
java -jar haitale-0.1-all.jar install -y magic-realms-2
```

### Search for specific mod types
```bash
java -jar haitale-0.1-all.jar search adventure
```

### Check what's installed
```bash
java -jar haitale-0.1-all.jar list
```

## ⚙️ Configuration Files

### Environment Variable (Recommended)
```bash
# ~/.zshrc or ~/.bashrc
export OPENROUTER_API_KEY="sk-or-v1-your-key-here"
```

### Application Properties
```properties
# src/main/resources/application.properties
openrouter.api.key=${OPENROUTER_API_KEY:}
openrouter.model=meta-llama/llama-3.2-3b-instruct:free
```

## 🤖 Free AI Models

Change model in `application.properties`:

```properties
# Fast and accurate (default)
openrouter.model=meta-llama/llama-3.2-3b-instruct:free

# Very fast, excellent quality
openrouter.model=google/gemini-2.0-flash-exp:free

# Balanced
openrouter.model=mistralai/mistral-7b-instruct:free

# Good reasoning
openrouter.model=qwen/qwen-2-7b-instruct:free
```

## 📁 Sample Mods Available

| ID | Name | Description |
|----|------|-------------|
| `enhanced-building-1` | Enhanced Building Tools | Advanced building tools and blueprints |
| `magic-realms-2` | Magic Realms | Magical spells and mystical creatures |
| `tech-revolution-3` | Tech Revolution | Machinery and automation systems |
| `adventure-quests-4` | Adventure Quest Pack | Quests and dynamic storytelling |
| `medieval-pack-5` | Medieval Immersion | Castles, knights, and siege weapons |
| `fantasy-creatures-6` | Fantasy Creatures | Dragons, griffins, and mythical beasts |

## 🔒 Security Features

- ✅ Only free/open-source licensed mods
- ✅ SHA-256 checksum verification
- ✅ Automatic backups before installation
- ✅ No malware or tampered files

## 🐛 Troubleshooting

### "OpenRouter API key not configured"
```bash
# Make sure to set the environment variable
export OPENROUTER_API_KEY="sk-or-v1-your-key"

# Verify it's set
echo $OPENROUTER_API_KEY
```

### "Using fallback rule-based recommendations"
This is normal if:
- No API key is configured (tool still works!)
- API key is invalid
- OpenRouter is unreachable

### Build fails
```bash
# Clean and rebuild
./gradlew clean build
```

## 📚 Documentation Links

- **Full README**: [README.md](README.md)
- **Setup Guide**: [SETUP.md](SETUP.md)
- **Examples**: [EXAMPLES.md](EXAMPLES.md)
- **Implementation**: [IMPLEMENTATION.md](IMPLEMENTATION.md)

## 🎓 Tips for Best AI Recommendations

1. **Be specific**: "medieval fantasy with magic schools" > "fantasy world"
2. **Mention gameplay**: Include what you want to do (build, fight, explore)
3. **Describe atmosphere**: Dark, whimsical, realistic, etc.
4. **List features**: Magic, technology, creatures, etc.

## 🌐 Useful Links

- **OpenRouter**: https://openrouter.ai/
- **Get API Key**: https://openrouter.ai/keys
- **Free Models**: https://openrouter.ai/models?max_price=0
- **OpenRouter Docs**: https://openrouter.ai/docs

## 🎉 Example Session

```bash
$ export OPENROUTER_API_KEY="sk-or-v1-..."

$ java -jar haitale-0.1-all.jar recommend "dark fantasy with necromancy and undead armies"

Analyzing your world description...
Using AI-powered recommendations...

Found 2 recommended mods:
================================================

1. Magic Realms v2.1.0 [Score: 85%]
   Why recommended: Includes dark magic systems and necromancy spells...
   ID: magic-realms-2

2. Fantasy Creatures Expansion v2.0.1 [Score: 70%]
   Why recommended: Adds undead creatures and skeletal armies...
   ID: fantasy-creatures-6

$ java -jar haitale-0.1-all.jar install magic-realms-2

Preparing to install 1 mod(s)...
✓ Successfully installed Magic Realms

Restart HyTale to load the new mods.
```

---

**Happy Modding! 🎮✨**
