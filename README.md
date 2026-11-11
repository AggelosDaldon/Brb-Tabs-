# BRB Tabs — Forge 1.20.1

An independent add-on project for **Better Recipe Books** (BRB), targeting Minecraft 1.20.1 / Forge.

## Goal

1. Use the same ordered vanilla **and modded creative tabs** as the player's current game in the crafting recipe-book UI.
2. Categorize *recipes* by the creative tabs containing their **output item**. Preserve recipe identity, even when different recipes have the same ingredients.
3. Show separate choices for conflicting recipes instead of silently collapsing them.
4. When a player chooses one conflicting recipe, craft **that recipe's server-validated output**, never just change the client-side preview.

## Important implementation constraints

- **Creative tabs are not recipe categories**: some items appear in multiple tabs; some modded items appear in none. Tabs must be read dynamically after mods register them, with a fallback category.
- **Duplicate ingredient patterns** cannot be safely resolved with a client-only GUI hook. The desired recipe ID must reach the authoritative server; the server must verify that the supplied grid matches it, that it is unlocked/allowed, and must preserve the selected recipe through result extraction and remaining-item handling.
- In vanilla 1.20.1, normal crafting uses the first matching recipe chosen by the RecipeManager. A new result-slot image alone will NOT make the other output obtainable.
- Custom recipe types / recipe books, dynamic datapack reloads, multiplayer permission rules, and modded crafting menus require explicit compatibility tests.

## Compatibility target

| Dependency | Version |
| --- | --- |
| Minecraft | 1.20.1 |
| Forge | 47.x |
| Java | 17 |
| Better Recipe Books | Forge build for 1.20.1 |
| Architectury API | Matching Forge build required by BRB |

BRB source: https://github.com/mrshmllow/BetterRecipeBook (MIT).

## Implementation phases

- [x] Repository initialized and behavior specified
- [ ] Forge/Gradle project and automated compilation
- [ ] Creative-tab-aware recipe listing and scrolling UI
- [ ] Selected-recipe synchronization and server-side crafting resolution
- [ ] BRB integration / compatibility tests
- [ ] Verified Forge 1.20.1 JAR

**Status:** Design groundwork only. This is not yet a playable mod. We will not publish a misleading JAR before the build and multiplayer collision tests pass.
