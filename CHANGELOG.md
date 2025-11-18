# Changelog

## [Unreleased]

## [0.0.4] - 2025-11-18

### Changed

- **Template parameter editor** - improved UX for configuring template variables
  - `VariableType` enum is now used consistently instead of raw string types
  - Legacy (unused) `PACKAGE` and `NUMBER` variable types removed
  - DROPDOWN parameters use an explicit options list with Add/Edit/Delete instead of comma-separated text
  - Default value for DROPDOWN is selected from options, respects `required` flag (no empty option when required)
  - Options list auto-resizes up to 5 items and then scrolls
- **Template engine variables** - removed hidden helper `packagePath`
  - Templates should now use explicit FreeMarker expression like `${packageName?replace(".", "/")}` when needed
  - DROPDOWN options are read from the `<options>` tag in `template.xml` and passed to the generator
- **Module generation UI** - MULTILINE_TEXT parameters now use a bordered text area with consistent styling

### Fixed

- **Nested module paths in settings.gradle.kts** - fixed incorrect module path generation for nested module structures
  - Previously, for structure like `src/cool-feature/api/build.gradle.kts`, it would generate `:src:api` instead of
    `:src:cool-feature:api`
  - Now correctly extracts full module paths from template structure (e.g., `cool-feature/api`) and combines them with
    target path
  - Supports any nesting level and correctly generates Gradle module paths like `:src:cool-feature:api`

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

[Unreleased]: https://github.com/NonoxyS/kmp-module-generator/compare/v0.0.4...HEAD

[0.0.4]: https://github.com/NonoxyS/kmp-module-generator/compare/v0.0.3...v0.0.4

[0.0.3]: https://github.com/NonoxyS/kmp-module-generator/compare/v0.0.2...v0.0.3

[0.0.2]: https://github.com/NonoxyS/kmp-module-generator/compare/v0.0.1...v0.0.2

[0.0.1]: https://github.com/NonoxyS/kmp-module-generator/commits/v0.0.1
