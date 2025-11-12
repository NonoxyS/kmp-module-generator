# Changelog

## [Unreleased]

## [0.0.3] - 2025-11-12

### Changed

- **Module detection** - modules are now detected by `build.gradle.kts` files instead of magic variables
  - No longer requires `moduleName`  parameter - modules are automatically detected from template structure
  - Supports templates with multiple modules (e.g., `api`, `impl`, `presentation`)
  - Module names are extracted from directory names containing `build.gradle.kts` files
  - All detected modules are automatically added to `settings.gradle.kts` with correct nested paths
  - Success/warning messages dynamically show all generated module names
- **Template Editor UI** - parameters table is now read-only to prevent accidental edits
  - Parameters can only be edited through the "Edit" button or double-click on a row
  - Prevents confusion where table edits appeared to work but weren't saved

## [0.0.2] - 2025-10-26

### Fixed

- **Settings UI on macOS** - fixed custom folder selection field not being clickable
- **Nested module paths** - correctly detect module hierarchy for `settings.gradle` includes
  - Creating module in `shared/` now generates `include(":shared:moduleName")` instead of `include(":moduleName")`
  - Supports any nesting level (e.g., `:shared:feature:moduleName`)

## [0.0.1] - 2025-10-26

### Added

- 🎨 **FreeMarker template system** - create module templates as simple text files
- 🖥️ **Visual template wizards** - create and edit templates through intuitive UI
- 📂 **Configurable template storage** - use custom folder or default `.idea/kmp-templates/`

[Unreleased]: https://github.com/NonoxyS/kmp-module-generator/compare/v0.0.3...HEAD

[0.0.3]: https://github.com/NonoxyS/kmp-module-generator/compare/v0.0.2...v0.0.3

[0.0.2]: https://github.com/NonoxyS/kmp-module-generator/compare/v0.0.1...v0.0.2

[0.0.1]: https://github.com/NonoxyS/kmp-module-generator/commits/v0.0.1
