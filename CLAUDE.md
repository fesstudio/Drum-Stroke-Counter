# Claude Code Instructions - Drum Stroke Counter

## Architecture
- Follow Clean Architecture: domain → data → UI layers
- Use MVI pattern with StateFlow for state management
- Use Hilt for dependency injection (@HiltViewModel, @Inject)
- All strings must use string resources (R.string), no hardcoded strings
- Support both English (values/) and Indonesian (values-in/) locales

## File Organization
- Each composable function should be in its own file
- Max file size: ~250 lines. If a file exceeds this, split into smaller files
- Keep MainActivity.kt lean (< 250 lines) - only Activity setup and top-level composable
- UI components go in `ui/components/` package
- Screens go in `ui/screens/` package
- Utility classes go in `util/` package
- Navigation logic goes in `navigation/` package

## Naming Conventions
- Composable functions: PascalCase (e.g., `PracticePage`, `AboutDialog`)
- Regular functions: camelCase (e.g., `formatTime`, `startPractice`)
- ViewModel functions: camelCase prefixed with verb (e.g., `startPractice`, `stopCalibration`)
- State classes: PascalCase with State suffix (e.g., `PracticeUiState`)
- Files: match the main class/composable name exactly

## Import Rules
- NO wildcard imports (`import ...*`) - use explicit imports only
- Group imports by: Android → AndroidX → Compose → Project → Third-party

## Compose Best Practices
- Extract reusable composables to separate files in `ui/components/`
- Use `private` modifier for composables only used within the same file
- Pass ViewModel as parameter, don't create it inside composables
- Use `collectAsState()` for StateFlow observation
- Use `remember` and `rememberSaveable` appropriately

## ViewModel Rules
- Single source of truth via `StateFlow<UiState>`
- Transient UI states (dialogs, toasts) can use `mutableStateOf`
- Use `viewModelScope.launch` for coroutines
- Never pass Context to ViewModel functions if avoidable

## Audio Engine Rules
- All audio engines go in `audio/engine/` package
- Use AudioConfig constants for configuration values
- Handle audio focus properly with AudioFocusManager
