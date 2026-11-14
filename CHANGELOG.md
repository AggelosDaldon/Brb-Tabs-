# Changelog

All notable changes to **Creative Recipe Book (BRB Tabs)** are documented here.

## Unreleased — Creative Tab Icon Crash Fix (2026-11-14)

### Fixed
- Prevented a crash when another mod's Creative Tab icon supplier throws a runtime exception.
- Affected tabs now display a vanilla book icon as a fallback rather than crashing the Recipe Book.
- Added a warning in the log the first time a tab has a broken icon supplier.

### Known limitations
- This workaround does **not** fix an item missing from the other mod's registry (for example, `cozywoodstoves:small_stove`).
- Conflicting recipes with identical ingredient patterns are not yet guaranteed to produce the selected output on a multiplayer server.
- The Creative Tab interface currently targets the Crafting Table recipe book; the Survival Inventory's 2×2 recipe book uses its normal interface.
- The changes passed compilation but have not been verified in every modpack.

## Earlier updates (November 2026)

### Added
- Vanilla and modded Creative Inventory tabs for browsing crafting recipes.
- Two-column layout with ten tabs per page and page navigation.
- `ALL` / `TAB` toggle to search all recipes or only the selected category.
- Tab tooltips and highlighted selected category.

### Improved
- Recipe Book-inspired background and Creative Inventory-style buttons.
- Simplified search-scope button and text without shadows.
- Custom sidebar visibility follows the Recipe Book's open/closed state.
- Original category buttons are hidden in the supported crafting interface.

### Fixed
- Corrected production Mixin mappings and refmap packaging for Forge 1.20.1.
- Reused original recipe collections to avoid an earlier RecipeButton rendering crash.

## Requirements

- Minecraft 1.20.1
- Forge 47.x
- Java 17
- Better Recipe Book for Minecraft 1.20.1

The current browsing UI is client-side. This file documents changes implemented in the project, not proof of in-game compatibility across all modpacks.
