# HaiTale Examples

This guide shows examples of using HaiTale with and without OpenRouter AI.

## Example 1: AI-Powered Recommendations (with OpenRouter)

When you have an OpenRouter API key configured, HaiTale uses AI to understand your world description:

```bash
export OPENROUTER_API_KEY="sk-or-v1-..."
java -jar haitale-0.1-all.jar recommend I want a steampunk world with flying machines and Victorian architecture
```

**AI-powered output:**
```
Analyzing your world description...
Description: I want a steampunk world with flying machines and Victorian architecture

Using AI-powered recommendations...

Found 3 recommended mods:
================================================

1. Tech Revolution v1.5.2 [Score: 95%]
   Author: TechWizard
   License: GPL-3.0
   Description: Adds machinery, automation, and technological advancement systems
   Why recommended: Perfect for steampunk! Includes mechanical systems and industrial-era technology that fits Victorian aesthetics. Great for airship construction.
   ID: tech-revolution-3

2. Enhanced Building Tools v1.0.0 [Score: 85%]
   Author: BuilderPro
   License: MIT
   Description: Adds advanced building tools, templates, and blueprints for complex structures
   Why recommended: Essential for creating detailed Victorian architecture with intricate designs and period-accurate building elements.
   ID: enhanced-building-1

3. Adventure Quest Pack v3.0.0 [Score: 60%]
   Author: QuestMaster
   License: MIT
   Description: Hundreds of quests, dungeons, and adventures with dynamic storytelling
   Why recommended: Adds narrative depth to your steampunk world with exploration missions and inventor-themed quests.
   ID: adventure-quests-4
```

## Example 2: Fallback Mode (without API key)

Without an OpenRouter API key, HaiTale uses keyword-based matching:

```bash
java -jar haitale-0.1-all.jar recommend magical fantasy world with dragons
```

**Keyword-based output:**
```
Analyzing your world description...
Description: magical fantasy world with dragons

Using fallback rule-based recommendations

Found 2 recommended mods:
================================================

1. Magic Realms v2.1.0 [Score: 60%]
   Author: MysticCoder
   License: Apache-2.0
   Description: Introduces magical spells, enchantments, and mystical creatures to your world
   Why recommended: Brings magical gameplay.
   ID: magic-realms-2

2. Fantasy Creatures Expansion v2.0.1 [Score: 50%]
   Author: CreatureDesigner
   License: BSD-3-Clause
   Description: Dragons, griffins, unicorns and other mythical creatures
   Why recommended: Matches your world description keywords.
   ID: fantasy-creatures-6
```

## Example 3: Installing Recommended Mods

After getting recommendations, install your favorite mods:

```bash
# Install single mod
java -jar haitale-0.1-all.jar install magic-realms-2

# Install multiple mods
java -jar haitale-0.1-all.jar install magic-realms-2 fantasy-creatures-6

# Skip confirmation with -y flag
java -jar haitale-0.1-all.jar install -y tech-revolution-3 enhanced-building-1
```

**Output:**
```
Preparing to install 2 mod(s)...

The following mods will be installed:
=====================================
  • Magic Realms v2.1.0
    License: Apache-2.0
    Size: 3.0 MB

  • Fantasy Creatures Expansion v2.0.1
    License: BSD-3-Clause
    Size: 3.5 MB

Proceed with installation? (y/N): y

Installing Magic Realms...
Downloading mod: Magic Realms v2.1.0
Download complete: /Users/you/.haitale/downloads/Magic_Realms-2.1.0.jar
Checksum verification passed
✓ Successfully installed Magic Realms

Installing Fantasy Creatures Expansion...
Downloading mod: Fantasy Creatures Expansion v2.0.1
Download complete: /Users/you/.haitale/downloads/Fantasy_Creatures_Expansion-2.0.1.jar
Checksum verification passed
✓ Successfully installed Fantasy Creatures Expansion

Installation complete!
Successfully installed: 2/2 mods

Restart HyTale to load the new mods.
```

## Example 4: Searching for Specific Mods

Search by keyword:

```bash
java -jar haitale-0.1-all.jar search building
```

**Output:**
```
Searching for mods matching: building

Found 1 mod(s):
======================

Name: Enhanced Building Tools v1.0.0
ID: enhanced-building-1
Author: BuilderPro
License: MIT ✓
Description: Adds advanced building tools, templates, and blueprints for complex structures
Source: modrinth
```

## Example 5: Listing Installed Mods

View what's currently installed:

```bash
java -jar haitale-0.1-all.jar list
```

**Output:**
```
Installed Mods:
===============

Name: Magic Realms v2.1.0
ID: magic-realms-2
Installed: 2026-01-30 15:23:45
Path: /Users/you/.haitale/mods/Magic_Realms-2.1.0.jar
Checksum: def456ghi789

Name: Fantasy Creatures Expansion v2.0.1
ID: fantasy-creatures-6
Installed: 2026-01-30 15:23:52
Path: /Users/you/.haitale/mods/Fantasy_Creatures_Expansion-2.0.1.jar
Checksum: pqr678stu901

Total: 2 mod(s) installed
```

## Example 6: Complex World Description

The AI understands complex, multi-faceted descriptions:

```bash
java -jar haitale-0.1-all.jar recommend "I want to create a medieval kingdom with magic schools, dragon riders, epic quest lines, and the ability to build massive castles with intricate designs"
```

**AI analyzes and considers:**
- Theme: Medieval
- Features: Magic, dragons, quests, building
- Scale: Large, intricate structures
- Gameplay: Combat, exploration, construction

**Returns relevant mods for:**
- Medieval building tools and templates
- Magic systems
- Dragon/creature mods
- Quest content
- Castle blueprints

## Tips for Best Results

### With AI (OpenRouter):
- Be descriptive about your vision
- Mention specific themes (medieval, sci-fi, fantasy)
- Include gameplay preferences (building, combat, exploration)
- Specify atmosphere (dark, whimsical, realistic)

### Without AI (Keyword mode):
- Use clear, specific keywords
- Mention exact categories (magic, building, tech)
- Keep descriptions focused
- Try multiple searches with different terms

## Free AI Models Comparison

| Model | Speed | Quality | Best For |
|-------|-------|---------|----------|
| meta-llama/llama-3.2-3b-instruct:free | Fast | Good | General recommendations |
| google/gemini-2.0-flash-exp:free | Very Fast | Excellent | Complex descriptions |
| mistralai/mistral-7b-instruct:free | Medium | Very Good | Balanced performance |
| qwen/qwen-2-7b-instruct:free | Medium | Good | Detailed reasoning |

Change model in `application.properties`:
```properties
openrouter.model=google/gemini-2.0-flash-exp:free
```
