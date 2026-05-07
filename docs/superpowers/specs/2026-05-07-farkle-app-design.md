# Farkle App Design Spec

**Studio:** Orange Zest Studios
**Package:** `com.orangezest.farkle`
**Platform:** Android (Kotlin + Jetpack Compose)
**Target:** Samsung foldable phones (minSdk 33, targetSdk 35)

## Overview

A pass-and-play Farkle dice game for 2-4 players on a single device, replacing an existing app that has incorrect scoring rules and questionable randomness. The app implements the official Legendary Games / PlayMonster Farkle rules as configurable defaults.

## Game Rules (Official Defaults)

### Scoring Table

| Combination | Points |
|---|---|
| Single 1 | 100 |
| Single 5 | 50 |
| Three 1s | 300 |
| Three 2s | 200 |
| Three 3s | 300 |
| Three 4s | 400 |
| Three 5s | 500 |
| Three 6s | 600 |
| Four of any number | 1,000 |
| Five of any number | 2,000 |
| Six of any number | 3,000 |
| 1-6 Straight | 1,500 |
| Three Pairs | 1,500 |
| Four + a Pair | 1,500 |
| Two Triplets | 2,500 |

### Mechanics

- **Target score:** 10,000 points
- **Minimum to get on the board:** 500 points in a single turn (first bank only)
- **Hot dice:** If all 6 dice are set aside as scoring, player rolls all 6 again and keeps building
- **Dice selection:** Player chooses which scoring dice to keep (not required to take all)
- **Farkle:** Roll with no scoring dice = lose running total for that turn
- **Game end:** First player to reach 10,000 triggers a final round; all other players get one more turn; highest score wins
- **Piggybacking (toggle, off by default):** After a player banks, the next player may choose to "steal the roll" — picking up the remaining dice with the previous player's banked-turn value as their starting running total. Player 1 keeps their banked points regardless. If Player 2 Farkles on the stolen roll, they lose the inherited total plus anything they added (Player 1 is unaffected).

### Configurable Settings

| Setting | Default | Range |
|---|---|---|
| Point values per combination | Official (see table) | Custom integers |
| Target score | 10,000 | Any positive integer |
| Minimum to get on board | 500 | 0+ |
| Hot dice | On | On/Off |
| Piggybacking | Off | On/Off |

Rules are configurable only before starting a game, not mid-game.

## Architecture

### Pattern: MVI (Model-View-Intent) with Unidirectional Data Flow

Single `StateFlow<GameUiState>` exposed from ViewModel, driven by a pure reducer function that processes `GameEvent` sealed classes. Illegal states are unrepresentable at the type level via sealed interfaces.

### Module Structure

```
farkle/
├── app/                     # Android app module (UI, ViewModels, Data)
│   ├── src/main/kotlin/     # Compose UI, ViewModels, DataStore, DI
│   ├── src/main/res/        # Resources (sounds, drawables)
│   ├── src/test/            # ViewModel + integration tests (JVM)
│   └── src/androidTest/     # Compose UI tests (instrumented)
├── game-engine/             # Pure Kotlin module (no Android dependencies)
│   ├── src/main/kotlin/     # Scoring engine, dice roller, state machine
│   └── src/test/kotlin/     # Unit tests (fast, JVM only)
├── .github/workflows/       # CI/CD pipeline
├── build.gradle.kts         # Root build file
└── gradle/                  # Gradle wrapper
```

The `:game-engine` module has zero Android dependencies — if it accidentally imports `android.*`, the build fails. This enforces clean separation and enables instant unit testing.

### State Management

```
GameEvent (user action)
  → ViewModel.onEvent()
    → reduce(currentState, event) → new GameUiState
      → UI recomposes
```

#### Game State (sealed interfaces)

```kotlin
sealed interface TurnPhase {
    data object WaitingToRoll : TurnPhase
    data class SelectingDice(val rollResult: List<Int>) : TurnPhase
    data class Farkled(val lostPoints: Int) : TurnPhase
    data class OfferSteal(val remainingDice: Int, val previousTotal: Int) : TurnPhase
    data object PassingDevice : TurnPhase
    data class GameOver(val winner: Player) : TurnPhase
}
```

#### Undo

State history stack — each meaningful action saves the previous state. Undo pops the stack.

### Persistence (Three Layers)

| Layer | Mechanism | Survives |
|---|---|---|
| In-memory game state | ViewModel (StateFlow) | Configuration changes (fold/unfold) |
| Process death recovery | SavedStateHandle | Android killing the process |
| Cross-session persistence | Proto DataStore | App kill, reboot, indefinite |

- `schemaVersion` field in Proto DataStore enables forward-compatible data migration
- Only one game saved at a time
- Kotlin Serialization for all state (not Parcelable)

### Dependency Injection

Hilt with minimal setup:
- One `@Module` providing DiceRoller, ScoringEngine, DataStore
- ViewModels annotated with `@HiltViewModel`

### Scoring Engine (Strategy Pattern)

Each scoring rule is a self-contained object implementing a sealed interface:

```kotlin
sealed interface ScoringRule {
    val name: String
    fun evaluate(dice: List<Int>): ScoringResult?
}
```

Rules are evaluated in priority order. The engine finds all valid scoring options for a given roll. Configurable point values are injected via a `GameConfig` data class.

### Dice Roller

```kotlin
fun interface DiceRoller {
    fun roll(count: Int): List<Int>
}
```

Production implementation uses `java.security.SecureRandom`. Tests inject a deterministic fake.

## Screens & Navigation

Three screens, flat navigation (no nested nav graphs):

### 1. Setup Screen (app launch)

- Number of players (2-4)
- Player name entry
- "Start Game" button
- Settings access (gear icon)
- "Resume Game" prompt if saved game exists

### 2. Game Screen

- **Scoreboard:** All players' total scores (always visible, does not rotate). Players not yet "on the board" show 0 with a subtle indicator that they need 500 to qualify.
- **Dice area:** 6 dice, tap to select/deselect
  - Smart grouping: tapping one die auto-selects the full group when the die ONLY scores as part of that group (e.g., three 6s — a single 6 is worthless). Dice that can score individually (1s, 5s) are always selected individually regardless of groups present.
  - Non-scoring dice are unselectable (visually distinct / grayed out)
- **Action bar:** Red "Roll" button + Green "Bank (+X)" button
  - Roll: disabled if no dice selected
  - Bank: disabled if no dice selected; disabled if below 500 and not yet on board
- **Undo button:** Reverts last dice selection
- **Farkle overlay:** "Farkle!" notification, auto-dismisses after 2 seconds
- **Pass device interstitial:** "[Player Name]'s turn" centered screen, player taps "Ready"
  - When piggybacking enabled: includes "Start fresh" vs "Steal the roll (X pts, Y dice)" choice
- **Content rotation:** Game content rotates between turns (180° for 2 players, 90° for 3-4) using `Modifier.rotate()` with animated transitions

### 3. Settings Screen (accessible from Setup only)

- Scoring rule values (editable)
- Target score
- Minimum to get on board
- Hot dice toggle
- Piggybacking toggle
- Sound effects on/off
- Haptic feedback on/off
- Theme: Light / Dark / System

## Foldable Behavior

### Screen Adaptation

- **Outer screen (cover, ~6.2" narrow):** Compact single-column layout
- **Inner screen (unfolded, ~7.6" near-square):** Expanded layout, scoreboard beside dice area
- **Fold/unfold transition:** No restart, no state loss; layout recomposes based on window dimensions

### Implementation

- Portrait orientation locked at system level
- Content rotation handled in-app via `Modifier.rotate()`
- Jetpack WindowManager `WindowInfoTracker` detects fold state
- ViewModel is fold-agnostic — fold awareness is a UI-only concern
- `WindowSizeClass` drives adaptive layout selection

### Critical Risk: Touch Targets After Rotation

`Modifier.rotate()` correctly transforms both visual content and input coordinates. Must be tested on real hardware in the first week of development.

## Sound & Haptics

- **Sound:** SoundPool API for low-latency game effects (dice roll, score, farkle, game over)
- **Haptics:** View-based `HapticFeedbackConstants` (no permission needed) for dice selection, rolling, farkle
- **Architecture:** `GameEffectsPlayer` in the UI layer observes state transitions and triggers effects. Never in the ViewModel.
- **User control:** Both independently toggleable in Settings

## Technical Infrastructure

### CI/CD (GitHub Actions on Self-Hosted ARC Runners)

Existing ARC controller (v0.14.1) on bare-metal Talos cluster. Add new runner scale set with custom Android SDK image.

**On every PR:**
- Build the app (Gradle)
- Run `:game-engine` unit tests
- Run lint (detekt/ktlint)
- Run Compose UI tests on emulator (KVM available on bare-metal nodes)

**On merge to main:**
- All of the above
- Build release APK, upload as artifact (downloadable for sideloading)

### Development Workflow

| Task | Where |
|---|---|
| Write code | Mac with Claude Code (text files only, no IDE needed) |
| Build + test | GitHub Actions on ARC cluster |
| Get APK | Download from GitHub Actions artifacts on phone |
| Visual testing | Run on physical Samsung phone |

Zero local software installations required.

### Repository

- **Public** (unlimited free CI on GitHub Actions)
- **GitHub org/user:** `jadunawa`
- **Branch strategy:** Feature branches → PR → main

### Key Libraries

| Library | Purpose |
|---|---|
| Jetpack Compose (BOM) | UI framework |
| Material 3 | Theming (light/dark/system) |
| Jetpack WindowManager | Foldable awareness |
| Hilt | Dependency injection |
| Proto DataStore | Game state persistence |
| Preferences DataStore | App settings |
| Kotlin Serialization | State serialization |
| SoundPool | Audio effects |

### Costs

**$0.** All tools, libraries, CI infrastructure, and development workflow are free. Optional $25 one-time fee for Google Play Store publishing (deferred decision).

## Testing Strategy

### Layer 1: Unit Tests (`:game-engine`, JVM, no emulator)

- Every scoring combination and edge case
- State machine transitions (valid and invalid)
- Dice roller statistical distribution
- Undo correctness
- Piggybacking mechanics

### Layer 2: ViewModel Tests (`:app`, JVM, no emulator)

- Event → state transition correctness
- SavedStateHandle persistence/restoration
- Full turn sequence simulations
- Game end conditions

### Layer 3: Compose UI Tests (instrumented, emulator on ARC runner)

- Dice tap → selection state
- Button enabled/disabled states
- Farkle overlay appears and dismisses
- Content rotation between turns
- Adaptive layout at different screen sizes

### Layer 4: Manual Testing (physical device)

- Full game playthrough
- Fold/unfold transitions
- Sound/haptics feel
- General UX validation

## Backlog (Future Features)

- Score history / statistics
- Player profiles / avatars
- House rules presets (save/load named configurations)
- Crash reporting / analytics
- Roll distribution stats (prove randomness)
- Tabletop mode (half-folded phone posture)
- Tutorial / onboarding for new players
