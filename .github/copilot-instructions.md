# GitHub Copilot Instructions - Drum Stroke Counter

## Architecture
- Clean Architecture: domain → data → UI layers
- MVI pattern with StateFlow for state management
- Hilt for dependency injection (@HiltViewModel, @Inject)

## Code Style
- All strings must use string resources (R.string), no hardcoded strings
- Support both English (values/) and Indonesian (values-in/) locales
- NO wildcard imports (`import ...*`) - use explicit imports only
- Group imports by: Android → AndroidX → Compose → Project → Third-party

## File Organization
- Each composable function should be in its own file
- Max file size: ~250 lines
- UI components go in `ui/components/` package
- Screens go in `ui/screens/` package
- Utility classes go in `util/` package
- Navigation logic goes in `navigation/` package

## Naming
- Composable functions: PascalCase (e.g., `PracticePage`, `AboutDialog`)
- ViewModel functions: camelCase prefixed with verb (e.g., `startPractice`, `stopCalibration`)
- State classes: PascalCase with State suffix (e.g., `PracticeUiState`)
- Files: match the main class/composable name exactly

## Compose
- Extract reusable composables to separate files in `ui/components/`
- Use `private` modifier for composables only used within the same file
- Pass ViewModel as parameter, don't create it inside composables
- Use `collectAsState()` for StateFlow observation
