# Changelog

## [Unreleased]

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

[Unreleased]: https://github.com/NonoxyS/kmp-module-generator/compare/v0.0.2...HEAD
[0.0.2]: https://github.com/NonoxyS/kmp-module-generator/compare/v0.0.1...v0.0.2
[0.0.1]: https://github.com/NonoxyS/kmp-module-generator/commits/v0.0.1
