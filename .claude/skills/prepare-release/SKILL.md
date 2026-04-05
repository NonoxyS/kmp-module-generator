---
name: prepare-release
description: >
  Prepare a new release of the KMP Module Generator plugin. Use this skill whenever
  the user wants to release a new version, bump the version, publish to JetBrains
  Marketplace, or says things like "release", "новый релиз", "выпустить версию",
  "bump version", "готовим релиз". Covers all local preparation steps before push to main.
---

# Prepare Release — KMP Module Generator

## Release flow overview

This skill covers only the **local preparation**. After the changes are merged into main, CI takes over:
1. Runs build / test / verify
2. Automatically creates a **Draft Release** on GitHub from the `[Unreleased]` changelog section
3. User manually publishes the draft → CI publishes the plugin to JetBrains Marketplace

---

## Step 1: Determine the new version

Read the current version from `gradle.properties` (`pluginVersion` field).

Suggest three options following SemVer (MAJOR.MINOR.PATCH):
- **patch** — bug fixes only (e.g. 0.0.4 → 0.0.5)
- **minor** — new backwards-compatible features (e.g. 0.0.4 → 0.1.0)
- **major** — breaking changes (e.g. 0.0.4 → 1.0.0)

Ask the user which to pick, or let them provide a custom version.

---

## Step 2: Check CHANGELOG.md

Read `CHANGELOG.md` and inspect the `## [Unreleased]` section.

- If it is **empty** — warn the user: pushing without changelog entries will create a blank Draft Release on GitHub. Ask whether to add entries now or proceed anyway.
- If it is **populated** — proceed.

---

## Step 3: Update files

### gradle.properties
Replace `pluginVersion=X.X.X` with the new version.

### CHANGELOG.md
Update the reference links at the bottom of the file — add a new version link and update the `[Unreleased]` compare URL.

Example for bumping to `0.0.5`:

Before:
```
[Unreleased]: https://github.com/NonoxyS/kmp-module-generator/compare/v0.0.4...HEAD
[0.0.4]: https://github.com/NonoxyS/kmp-module-generator/compare/v0.0.3...v0.0.4
```

After:
```
[Unreleased]: https://github.com/NonoxyS/kmp-module-generator/compare/v0.0.5...HEAD
[0.0.5]: https://github.com/NonoxyS/kmp-module-generator/compare/v0.0.4...v0.0.5
[0.0.4]: https://github.com/NonoxyS/kmp-module-generator/compare/v0.0.3...v0.0.4
```

Leave the `## [Unreleased]` heading as-is — CI will call `patchChangelog` on publish and rename it to `## [0.0.5] - YYYY-MM-DD` automatically.

---

## Step 4: Run local checks

Run sequentially:

```bash
./gradlew check
```
Stop and report to the user if tests fail. Do not proceed until fixed.

```bash
./gradlew verifyPlugin
```
Stop and report to the user if verification fails.

---

## Step 5: Summarize what's ready

Show the user a summary of changes made:
- New version: `0.0.X`
- Files updated: `gradle.properties`, `CHANGELOG.md`
- Check results: ✅ passed

Then remind what to do next:

> **Next steps (yours):**
> 1. Commit the changes and open a PR into `main`
> 2. Once merged, CI will run and create a **Draft Release `v<VERSION>`** on GitHub
> 3. Go to https://github.com/NonoxyS/kmp-module-generator/releases, review the draft, and click **"Publish release"**
> 4. CI will automatically publish the plugin to JetBrains Marketplace
