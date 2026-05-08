# Farkle App Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build a pass-and-play Farkle dice game for Android that implements official scoring rules with configurable settings, targeting Samsung foldable phones.

**Architecture:** MVI pattern with unidirectional data flow. A pure-Kotlin `:game-engine` module handles scoring/state logic (zero Android dependencies), while the `:app` module provides Jetpack Compose UI, Hilt DI, and DataStore persistence. State is driven by a single `StateFlow<GameUiState>` from sealed `GameEvent` inputs.

**Tech Stack:** Kotlin 2.0, Jetpack Compose (BOM), Material 3, Hilt, Proto DataStore, Preferences DataStore, Kotlin Serialization, Jetpack WindowManager

---

## File Structure

```
farkle/
├── build.gradle.kts                              # Root build: plugins, versions catalog
├── settings.gradle.kts                           # Module includes, repos
├── gradle/
│   └── libs.versions.toml                        # Version catalog
├── gradle.properties                             # Gradle/Kotlin config
├── game-engine/
│   ├── build.gradle.kts                          # Pure Kotlin module (no Android)
│   └── src/
│       ├── main/kotlin/com/orangezest/farkle/engine/
│       │   ├── DiceRoller.kt                     # DiceRoller fun interface + SecureRandom impl
│       │   ├── ScoringRule.kt                    # ScoringRule sealed interface
│       │   ├── ScoringRules.kt                   # All rule implementations
│       │   ├── ScoringEngine.kt                  # Evaluates rolls, finds valid selections
│       │   ├── GameConfig.kt                     # Configurable point values + settings
│       │   ├── GameState.kt                      # TurnPhase sealed interface, Player, GameUiState
│       │   ├── GameEvent.kt                      # Sealed interface for all user actions
│       │   └── GameReducer.kt                    # Pure function: (state, event) → state
│       └── test/kotlin/com/orangezest/farkle/engine/
│           ├── DiceRollerTest.kt                 # Distribution + deterministic fake
│           ├── ScoringRulesTest.kt               # Every combination, edge cases
│           ├── ScoringEngineTest.kt              # Full roll evaluation + selection validation
│           ├── GameReducerTest.kt                # State transitions, undo, hot dice, piggybacking
│           └── GameConfigTest.kt                 # Custom config validation
├── app/
│   ├── build.gradle.kts                          # Android app module
│   ├── proguard-rules.pro                        # Keep rules for Hilt, Serialization, DataStore
│   ├── src/main/
│   │   ├── AndroidManifest.xml                   # Single activity, portrait locked
│   │   ├── kotlin/com/orangezest/farkle/
│   │   │   ├── FarkleApp.kt                      # @HiltAndroidApp Application class
│   │   │   ├── MainActivity.kt                   # Single activity, @AndroidEntryPoint
│   │   │   ├── di/
│   │   │   │   └── GameModule.kt                 # Hilt @Module: DiceRoller, DataStore, Repositories
│   │   │   ├── data/
│   │   │   │   ├── GameStateSerializer.kt        # Proto DataStore serializer
│   │   │   │   ├── GameStateRepository.kt        # Save/load game state
│   │   │   │   └── SettingsRepository.kt         # Preferences DataStore for app settings
│   │   │   ├── viewmodel/
│   │   │   │   ├── GameViewModel.kt              # @HiltViewModel, state management
│   │   │   │   └── SettingsViewModel.kt          # @HiltViewModel, settings management
│   │   │   ├── ui/
│   │   │   │   ├── theme/
│   │   │   │   │   ├── Theme.kt                  # Material 3 dynamic theme (light/dark/system)
│   │   │   │   │   ├── Color.kt                  # Color definitions
│   │   │   │   │   └── Type.kt                   # Typography
│   │   │   │   ├── navigation/
│   │   │   │   │   └── FarkleNavGraph.kt         # Three-screen flat navigation
│   │   │   │   ├── setup/
│   │   │   │   │   └── SetupScreen.kt            # Player count, names, start/resume
│   │   │   │   ├── game/
│   │   │   │   │   ├── GameScreen.kt             # Main game screen orchestrator
│   │   │   │   │   ├── Scoreboard.kt             # Always-visible player scores
│   │   │   │   │   ├── DiceArea.kt               # 6 dice with tap selection
│   │   │   │   │   ├── ActionBar.kt              # Roll + Bank buttons
│   │   │   │   │   ├── FarkleOverlay.kt          # "Farkle!" dismissible overlay
│   │   │   │   │   ├── PassDeviceScreen.kt       # Turn interstitial + piggyback choice
│   │   │   │   │   └── GameOverScreen.kt         # Winner display
│   │   │   │   ├── settings/
│   │   │   │   │   └── SettingsScreen.kt         # All configurable options
│   │   │   │   └── components/
│   │   │   │       └── Die.kt                    # Single die composable (selected/unselected/disabled)
│   │   │   └── effects/
│   │   │       └── HapticFeedbackPlayer.kt       # Haptics observer
│   │   ├── res/
│   │   │   └── values/
│   │   │       └── strings.xml                   # String resources
│   │   └── proto/
│   │       └── game_state.proto                  # Proto DataStore schema
│   └── src/test/kotlin/com/orangezest/farkle/
│       ├── viewmodel/
│       │   ├── GameViewModelTest.kt              # Event → state, SavedStateHandle
│       │   └── SettingsViewModelTest.kt          # Settings persistence
│       └── data/
│           └── GameStateRepositoryTest.kt        # Serialization round-trip
├── .github/workflows/
│   ├── pr-checks.yml                             # Build, test, lint on PR
│   └── release.yml                               # Build release APK on merge to main
└── .gitignore                                    # Android + Gradle ignores
```

---

## Task 1: Project Scaffolding — Gradle Setup

**Files:**
- Create: `settings.gradle.kts`
- Create: `build.gradle.kts`
- Create: `gradle/libs.versions.toml`
- Create: `gradle.properties`
- Create: `.gitignore`
- Create: `game-engine/build.gradle.kts`
- Create: `app/build.gradle.kts`
- Create: `app/src/main/AndroidManifest.xml`

- [ ] **Step 1: Initialize Gradle wrapper**

Create manually:

`gradle/wrapper/gradle-wrapper.properties`:
```properties
distributionBase=GRADLE_USER_HOME
distributionPath=wrapper/dists
distributionUrl=https\://services.gradle.org/distributions/gradle-8.7-bin.zip
networkTimeout=10000
validateDistributionUrl=true
zipStoreBase=GRADLE_USER_HOME
zipStorePath=wrapper/dists
```

- [ ] **Step 2: Create version catalog**

`gradle/libs.versions.toml`:
```toml
[versions]
kotlin = "2.0.21"
agp = "8.5.2"
compose-bom = "2024.12.01"
hilt = "2.51.1"
datastore = "1.1.1"
kotlinx-serialization = "1.7.3"
window = "1.3.0"
junit5 = "5.10.3"
turbine = "1.1.0"
coroutines = "1.8.1"

[libraries]
compose-bom = { module = "androidx.compose:compose-bom", version.ref = "compose-bom" }
compose-ui = { module = "androidx.compose.ui:ui" }
compose-material3 = { module = "androidx.compose.material3:material3" }
compose-material3-windowsize = { module = "androidx.compose.material3:material3-window-size-class" }
compose-tooling-preview = { module = "androidx.compose.ui:ui-tooling-preview" }
compose-tooling = { module = "androidx.compose.ui:ui-tooling" }
compose-activity = { module = "androidx.activity:activity-compose", version = "1.9.3" }
compose-navigation = { module = "androidx.navigation:navigation-compose", version = "2.8.4" }
compose-lifecycle = { module = "androidx.lifecycle:lifecycle-runtime-compose", version = "2.8.7" }
compose-viewmodel = { module = "androidx.lifecycle:lifecycle-viewmodel-compose", version = "2.8.7" }

hilt-android = { module = "com.google.dagger:hilt-android", version.ref = "hilt" }
hilt-compiler = { module = "com.google.dagger:hilt-android-compiler", version.ref = "hilt" }
hilt-navigation-compose = { module = "androidx.hilt:hilt-navigation-compose", version = "1.2.0" }

datastore-proto = { module = "androidx.datastore:datastore", version.ref = "datastore" }
datastore-preferences = { module = "androidx.datastore:datastore-preferences", version.ref = "datastore" }
protobuf-javalite = { module = "com.google.protobuf:protobuf-javalite", version = "4.28.3" }

kotlinx-serialization-json = { module = "org.jetbrains.kotlinx:kotlinx-serialization-json", version.ref = "kotlinx-serialization" }
kotlinx-coroutines-core = { module = "org.jetbrains.kotlinx:kotlinx-coroutines-core", version.ref = "coroutines" }
kotlinx-coroutines-test = { module = "org.jetbrains.kotlinx:kotlinx-coroutines-test", version.ref = "coroutines" }

window = { module = "androidx.window:window", version.ref = "window" }

junit5-api = { module = "org.junit.jupiter:junit-jupiter-api", version.ref = "junit5" }
junit5-engine = { module = "org.junit.jupiter:junit-jupiter-engine", version.ref = "junit5" }
junit5-params = { module = "org.junit.jupiter:junit-jupiter-params", version.ref = "junit5" }
turbine = { module = "app.cash.turbine:turbine", version.ref = "turbine" }

[plugins]
android-application = { id = "com.android.application", version.ref = "agp" }
kotlin-android = { id = "org.jetbrains.kotlin.android", version.ref = "kotlin" }
kotlin-jvm = { id = "org.jetbrains.kotlin.jvm", version.ref = "kotlin" }
kotlin-serialization = { id = "org.jetbrains.kotlin.plugin.serialization", version.ref = "kotlin" }
compose-compiler = { id = "org.jetbrains.kotlin.plugin.compose", version.ref = "kotlin" }
hilt = { id = "com.google.dagger.hilt.android", version.ref = "hilt" }
ksp = { id = "com.google.devtools.ksp", version = "2.0.21-1.0.28" }
protobuf = { id = "com.google.protobuf", version = "0.9.4" }
```

- [ ] **Step 3: Create root build.gradle.kts**

`build.gradle.kts`:
```kotlin
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.compose.compiler) apply false
    alias(libs.plugins.hilt) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.protobuf) apply false
}
```

- [ ] **Step 4: Create settings.gradle.kts**

`settings.gradle.kts`:
```kotlin
pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "farkle"
include(":app")
include(":game-engine")
```

- [ ] **Step 5: Create gradle.properties**

`gradle.properties`:
```properties
org.gradle.jvmargs=-Xmx2048m -Dfile.encoding=UTF-8
android.useAndroidX=true
kotlin.code.style=official
android.nonTransitiveRClass=true
```

- [ ] **Step 6: Create game-engine/build.gradle.kts**

`game-engine/build.gradle.kts`:
```kotlin
plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
}

java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}

kotlin {
    jvmToolchain(21)
}

dependencies {
    implementation(libs.kotlinx.serialization.json)

    testImplementation(libs.junit5.api)
    testImplementation(libs.junit5.params)
    testRuntimeOnly(libs.junit5.engine)
}

tasks.test {
    useJUnitPlatform()
}
```

- [ ] **Step 7: Create app/build.gradle.kts**

`app/build.gradle.kts`:
```kotlin
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
    alias(libs.plugins.protobuf)
}

android {
    namespace = "com.orangezest.farkle"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.orangezest.farkle"
        minSdk = 33
        targetSdk = 35
        versionCode = 1
        versionName = "1.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    kotlinOptions {
        jvmTarget = "21"
    }

    buildFeatures {
        compose = true
    }
}

protobuf {
    protoc {
        artifact = "com.google.protobuf:protoc:4.28.3"
    }
    generateProtoTasks {
        all().forEach { task ->
            task.builtins {
                register("java") {
                    option("lite")
                }
            }
        }
    }
}

dependencies {
    implementation(project(":game-engine"))

    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.material3)
    implementation(libs.compose.material3.windowsize)
    implementation(libs.compose.tooling.preview)
    implementation(libs.compose.activity)
    implementation(libs.compose.navigation)
    implementation(libs.compose.lifecycle)
    implementation(libs.compose.viewmodel)
    debugImplementation(libs.compose.tooling)

    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.hilt.navigation.compose)

    implementation(libs.datastore.proto)
    implementation(libs.datastore.preferences)
    implementation(libs.protobuf.javalite)

    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.coroutines.core)

    implementation(libs.window)

    testImplementation(libs.junit5.api)
    testImplementation(libs.junit5.params)
    testRuntimeOnly(libs.junit5.engine)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.turbine)
}

tasks.withType<Test> {
    useJUnitPlatform()
}
```

- [ ] **Step 8: Create AndroidManifest.xml**

`app/src/main/AndroidManifest.xml`:
```xml
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android">

    <application
        android:name=".FarkleApp"
        android:label="@string/app_name"
        android:supportsRtl="true"
        android:theme="@style/Theme.Farkle">
        <activity
            android:name=".MainActivity"
            android:exported="true"
            android:screenOrientation="portrait">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>
    </application>

</manifest>
```

- [ ] **Step 9: Create .gitignore**

`.gitignore`:
```gitignore
# Gradle
.gradle/
build/
local.properties

# IDE
.idea/
*.iml

# Android
app/release/
app/debug/

# Kotlin
*.class

# OS
.DS_Store
Thumbs.db
```

- [ ] **Step 10: Create placeholder source files for compilation**

`app/src/main/kotlin/com/orangezest/farkle/FarkleApp.kt`:
```kotlin
package com.orangezest.farkle

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class FarkleApp : Application()
```

`app/src/main/kotlin/com/orangezest/farkle/MainActivity.kt`:
```kotlin
package com.orangezest.farkle

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { }
    }
}
```

`app/src/main/res/values/strings.xml`:
```xml
<resources>
    <string name="app_name">Farkle</string>
</resources>
```

`app/src/main/res/values/themes.xml`:
```xml
<resources>
    <style name="Theme.Farkle" parent="android:Theme.Material.Light.NoActionBar" />
</resources>
```

- [ ] **Step 11: Commit**

```bash
git add -A
git commit -m "feat: scaffold Gradle multi-module project structure"
```

---

## Task 2: Gradle Validation Spike

**Files:**
- None (CI validation only)

- [ ] **Step 1: Push Task 1 commit to CI**

```bash
git push origin main
```

- [ ] **Step 2: Verify game-engine compiles on CI**

Run (or confirm CI runs): `./gradlew :game-engine:compileKotlin`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: Verify protobuf plugin resolves**

Run: `./gradlew :app:compileDebugKotlin`
Expected: BUILD SUCCESSFUL — protobuf plugin resolves and generates sources.

- [ ] **Step 4: Gate subsequent tasks on green build**

If CI fails, fix build configuration before proceeding to Task 3. Common issues: version catalog resolution, JDK toolchain, protobuf plugin classpath.

---

## Task 3: Dice Roller

**Files:**
- Create: `game-engine/src/main/kotlin/com/orangezest/farkle/engine/DiceRoller.kt`
- Create: `game-engine/src/test/kotlin/com/orangezest/farkle/engine/DiceRollerTest.kt`

- [ ] **Step 1: Write the failing test**

`game-engine/src/test/kotlin/com/orangezest/farkle/engine/DiceRollerTest.kt`:
```kotlin
package com.orangezest.farkle.engine

import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DiceRollerTest {

    @Test
    fun `roll returns correct number of dice`() {
        val roller = SecureRandomDiceRoller()
        for (count in 1..6) {
            assertEquals(count, roller.roll(count).size)
        }
    }

    @Test
    fun `all dice values are between 1 and 6`() {
        val roller = SecureRandomDiceRoller()
        val results = (1..1000).flatMap { roller.roll(6) }
        assertTrue(results.all { it in 1..6 })
    }

    @Tag("statistical")
    @Test
    fun `distribution is roughly uniform over many rolls`() {
        val roller = SecureRandomDiceRoller()
        val results = (1..6000).map { roller.roll(1).first() }
        val counts = results.groupingBy { it }.eachCount()

        assertAll(
            (1..6).map { face ->
                { assertTrue(counts[face]!! in 700..1300, "Face $face count ${counts[face]} outside expected range") }
            }
        )
    }

    @Test
    fun `fake dice roller returns predetermined values`() {
        val fake = FakeDiceRoller(listOf(1, 2, 3, 4, 5, 6))
        assertEquals(listOf(1, 2, 3), fake.roll(3))
        assertEquals(listOf(4, 5, 6), fake.roll(3))
    }

    @Test
    fun `fake dice roller wraps around when exhausted`() {
        val fake = FakeDiceRoller(listOf(1, 1, 1))
        fake.roll(3)
        assertEquals(listOf(1, 1, 1), fake.roll(3))
    }
}
```

> Note: Exclude statistical test from CI with `--exclude-tags statistical` if flaky.

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :game-engine:test --tests "com.orangezest.farkle.engine.DiceRollerTest"`
Expected: Compilation failure — classes don't exist yet.

- [ ] **Step 3: Write minimal implementation**

`game-engine/src/main/kotlin/com/orangezest/farkle/engine/DiceRoller.kt`:
```kotlin
package com.orangezest.farkle.engine

import java.security.SecureRandom

fun interface DiceRoller {
    fun roll(count: Int): List<Int>
}

class SecureRandomDiceRoller : DiceRoller {
    private val random = SecureRandom()

    override fun roll(count: Int): List<Int> =
        List(count) { random.nextInt(6) + 1 }
}

class FakeDiceRoller(private val values: List<Int>) : DiceRoller {
    private var index = 0

    override fun roll(count: Int): List<Int> =
        List(count) {
            values[index % values.size].also { index++ }
        }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew :game-engine:test --tests "com.orangezest.farkle.engine.DiceRollerTest"`
Expected: All 5 tests PASS.

- [ ] **Step 5: Commit**

```bash
git add game-engine/src/
git commit -m "feat(engine): add DiceRoller interface with SecureRandom and fake implementations"
```

---

## Task 4: Game Config

**Files:**
- Create: `game-engine/src/main/kotlin/com/orangezest/farkle/engine/GameConfig.kt`
- Create: `game-engine/src/test/kotlin/com/orangezest/farkle/engine/GameConfigTest.kt`

- [ ] **Step 1: Write the failing test**

`game-engine/src/test/kotlin/com/orangezest/farkle/engine/GameConfigTest.kt`:
```kotlin
package com.orangezest.farkle.engine

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class GameConfigTest {

    @Test
    fun `default config matches official rules`() {
        val config = GameConfig.DEFAULT

        assertEquals(100, config.single1Points)
        assertEquals(50, config.single5Points)
        assertEquals(300, config.three1sPoints)
        assertEquals(200, config.three2sPoints)
        assertEquals(300, config.three3sPoints)
        assertEquals(400, config.three4sPoints)
        assertEquals(500, config.three5sPoints)
        assertEquals(600, config.three6sPoints)
        assertEquals(1000, config.fourOfAKindPoints)
        assertEquals(2000, config.fiveOfAKindPoints)
        assertEquals(3000, config.sixOfAKindPoints)
        assertEquals(1500, config.straightPoints)
        assertEquals(1500, config.threePairsPoints)
        assertEquals(1500, config.fourPlusAPairPoints)
        assertEquals(2500, config.twoTripletsPoints)
    }

    @Test
    fun `default target score is 10000`() {
        assertEquals(10_000, GameConfig.DEFAULT.targetScore)
    }

    @Test
    fun `default minimum to get on board is 500`() {
        assertEquals(500, GameConfig.DEFAULT.minimumToBoard)
    }

    @Test
    fun `default hot dice is on`() {
        assertEquals(true, GameConfig.DEFAULT.hotDiceEnabled)
    }

    @Test
    fun `default piggybacking is off`() {
        assertEquals(false, GameConfig.DEFAULT.piggybackingEnabled)
    }

    @Test
    fun `custom config overrides specific values`() {
        val custom = GameConfig.DEFAULT.copy(
            targetScore = 5000,
            single1Points = 200,
            piggybackingEnabled = true
        )
        assertEquals(5000, custom.targetScore)
        assertEquals(200, custom.single1Points)
        assertEquals(true, custom.piggybackingEnabled)
        assertEquals(50, custom.single5Points) // unchanged
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :game-engine:test --tests "com.orangezest.farkle.engine.GameConfigTest"`
Expected: Compilation failure — `GameConfig` doesn't exist.

- [ ] **Step 3: Write minimal implementation**

`game-engine/src/main/kotlin/com/orangezest/farkle/engine/GameConfig.kt`:
```kotlin
package com.orangezest.farkle.engine

import kotlinx.serialization.Serializable

@Serializable
data class GameConfig(
    val targetScore: Int = 10_000,
    val minimumToBoard: Int = 500,
    val hotDiceEnabled: Boolean = true,
    val piggybackingEnabled: Boolean = false,
    val single1Points: Int = 100,
    val single5Points: Int = 50,
    val three1sPoints: Int = 300,
    val three2sPoints: Int = 200,
    val three3sPoints: Int = 300,
    val three4sPoints: Int = 400,
    val three5sPoints: Int = 500,
    val three6sPoints: Int = 600,
    val fourOfAKindPoints: Int = 1_000,
    val fiveOfAKindPoints: Int = 2_000,
    val sixOfAKindPoints: Int = 3_000,
    val straightPoints: Int = 1_500,
    val threePairsPoints: Int = 1_500,
    val fourPlusAPairPoints: Int = 1_500,
    val twoTripletsPoints: Int = 2_500,
) {
    companion object {
        val DEFAULT = GameConfig()
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew :game-engine:test --tests "com.orangezest.farkle.engine.GameConfigTest"`
Expected: All 6 tests PASS.

- [ ] **Step 5: Commit**

```bash
git add game-engine/src/
git commit -m "feat(engine): add GameConfig data class with official default values"
```

---

## Task 5: Scoring Rules — Individual Dice (Singles)

**Files:**
- Create: `game-engine/src/main/kotlin/com/orangezest/farkle/engine/ScoringRule.kt`
- Create: `game-engine/src/main/kotlin/com/orangezest/farkle/engine/ScoringRules.kt`
- Create: `game-engine/src/test/kotlin/com/orangezest/farkle/engine/ScoringRulesTest.kt`

- [ ] **Step 1: Write the failing tests for singles**

`game-engine/src/test/kotlin/com/orangezest/farkle/engine/ScoringRulesTest.kt`:
```kotlin
package com.orangezest.farkle.engine

import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ScoringRulesTest {

    private val config = GameConfig.DEFAULT

    @Nested
    inner class Single1Rule {
        private val rule = SinglesRule(1, config)

        @Test
        fun `scores 100 for a single 1`() {
            val result = rule.evaluate(listOf(1))
            assertEquals(100, result?.points)
            assertEquals(listOf(1), result?.diceUsed)
        }

        @Test
        fun `returns null for no 1s`() {
            assertNull(rule.evaluate(listOf(2, 3, 4, 6)))
        }

        @Test
        fun `scores each 1 independently`() {
            val result = rule.evaluate(listOf(1, 1, 3, 4))
            assertEquals(100, result?.points)
            assertEquals(listOf(1), result?.diceUsed)
        }
    }

    @Nested
    inner class Single5Rule {
        private val rule = SinglesRule(5, config)

        @Test
        fun `scores 50 for a single 5`() {
            val result = rule.evaluate(listOf(5))
            assertEquals(50, result?.points)
            assertEquals(listOf(5), result?.diceUsed)
        }

        @Test
        fun `returns null for no 5s`() {
            assertNull(rule.evaluate(listOf(1, 2, 3, 4, 6)))
        }
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :game-engine:test --tests "com.orangezest.farkle.engine.ScoringRulesTest"`
Expected: Compilation failure.

- [ ] **Step 3: Write minimal implementation**

`game-engine/src/main/kotlin/com/orangezest/farkle/engine/ScoringRule.kt`:
```kotlin
package com.orangezest.farkle.engine

data class ScoringResult(
    val points: Int,
    val diceUsed: List<Int>,
    val name: String,
)

sealed interface ScoringRule {
    val name: String
    fun evaluate(dice: List<Int>): ScoringResult?
}
```

`game-engine/src/main/kotlin/com/orangezest/farkle/engine/ScoringRules.kt`:
```kotlin
package com.orangezest.farkle.engine

class SinglesRule(private val face: Int, private val config: GameConfig) : ScoringRule {
    override val name: String = "Single $face"

    private val pointValue: Int
        get() = when (face) {
            1 -> config.single1Points
            5 -> config.single5Points
            else -> 0
        }

    override fun evaluate(dice: List<Int>): ScoringResult? {
        if (face !in dice) return null
        return ScoringResult(
            points = pointValue,
            diceUsed = listOf(face),
            name = name,
        )
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew :game-engine:test --tests "com.orangezest.farkle.engine.ScoringRulesTest"`
Expected: All 5 tests PASS.

- [ ] **Step 5: Commit**

```bash
git add game-engine/src/
git commit -m "feat(engine): add ScoringRule interface and SinglesRule for 1s and 5s"
```

---

## Task 6: Scoring Rules — N-of-a-Kind

**Files:**
- Modify: `game-engine/src/main/kotlin/com/orangezest/farkle/engine/ScoringRules.kt`
- Modify: `game-engine/src/test/kotlin/com/orangezest/farkle/engine/ScoringRulesTest.kt`

- [ ] **Step 1: Write the failing tests**

Add to `ScoringRulesTest.kt`:
```kotlin
    @Nested
    inner class ThreeOfAKindRule {
        @Test
        fun `three 1s scores 300`() {
            val rule = NOfAKindRule(3, config)
            val result = rule.evaluate(listOf(1, 1, 1, 3, 4, 6))
            assertEquals(300, result?.points)
            assertEquals(listOf(1, 1, 1), result?.diceUsed)
        }

        @Test
        fun `three 2s scores 200`() {
            val rule = NOfAKindRule(3, config)
            val result = rule.evaluate(listOf(2, 2, 2))
            assertEquals(200, result?.points)
            assertEquals(listOf(2, 2, 2), result?.diceUsed)
        }

        @Test
        fun `three 3s scores 300`() {
            val rule = NOfAKindRule(3, config)
            val result = rule.evaluate(listOf(3, 3, 3, 5, 6, 2))
            assertEquals(300, result?.points)
        }

        @Test
        fun `three 4s scores 400`() {
            val rule = NOfAKindRule(3, config)
            val result = rule.evaluate(listOf(4, 4, 4))
            assertEquals(400, result?.points)
        }

        @Test
        fun `three 5s scores 500`() {
            val rule = NOfAKindRule(3, config)
            val result = rule.evaluate(listOf(5, 5, 5, 2, 3))
            assertEquals(500, result?.points)
        }

        @Test
        fun `three 6s scores 600`() {
            val rule = NOfAKindRule(3, config)
            val result = rule.evaluate(listOf(6, 6, 6))
            assertEquals(600, result?.points)
        }

        @Test
        fun `returns null when no three of a kind exists`() {
            val rule = NOfAKindRule(3, config)
            assertNull(rule.evaluate(listOf(1, 2, 3, 4, 5, 6)))
        }

        @Test
        fun `does not match four of a kind`() {
            val rule = NOfAKindRule(3, config)
            assertNull(rule.evaluate(listOf(2, 2, 2, 2, 3, 4)))
        }
    }

    @Nested
    inner class FourOfAKindRule {
        @Test
        fun `four of any number scores 1000`() {
            val rule = NOfAKindRule(4, config)
            val result = rule.evaluate(listOf(3, 3, 3, 3, 5, 6))
            assertEquals(1000, result?.points)
            assertEquals(listOf(3, 3, 3, 3), result?.diceUsed)
        }

        @Test
        fun `returns null when only three of a kind`() {
            val rule = NOfAKindRule(4, config)
            assertNull(rule.evaluate(listOf(3, 3, 3, 5, 6, 2)))
        }

        @Test
        fun `does not match five of a kind`() {
            val rule = NOfAKindRule(4, config)
            assertNull(rule.evaluate(listOf(3, 3, 3, 3, 3, 6)))
        }
    }

    @Nested
    inner class FiveOfAKindRule {
        @Test
        fun `five of any number scores 2000`() {
            val rule = NOfAKindRule(5, config)
            val result = rule.evaluate(listOf(4, 4, 4, 4, 4, 6))
            assertEquals(2000, result?.points)
            assertEquals(listOf(4, 4, 4, 4, 4), result?.diceUsed)
        }

        @Test
        fun `does not match six of a kind`() {
            val rule = NOfAKindRule(5, config)
            assertNull(rule.evaluate(listOf(4, 4, 4, 4, 4, 4)))
        }
    }

    @Nested
    inner class SixOfAKindRule {
        @Test
        fun `six of any number scores 3000`() {
            val rule = NOfAKindRule(6, config)
            val result = rule.evaluate(listOf(2, 2, 2, 2, 2, 2))
            assertEquals(3000, result?.points)
            assertEquals(listOf(2, 2, 2, 2, 2, 2), result?.diceUsed)
        }

        @Test
        fun `returns null when only five of a kind`() {
            val rule = NOfAKindRule(6, config)
            assertNull(rule.evaluate(listOf(2, 2, 2, 2, 2, 3)))
        }
    }
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :game-engine:test --tests "com.orangezest.farkle.engine.ScoringRulesTest"`
Expected: Compilation failure — `NOfAKindRule` doesn't exist.

- [ ] **Step 3: Write minimal implementation**

Add to `ScoringRules.kt`:
```kotlin
open class NOfAKindRule(private val count: Int, private val config: GameConfig) : ScoringRule {
    override val name: String = "$count of a Kind"

    override fun evaluate(dice: List<Int>): ScoringResult? {
        val face = dice.groupingBy { it }.eachCount()
            .entries.find { it.value == count }?.key
            ?: return null

        val points = when (count) {
            3 -> when (face) {
                1 -> config.three1sPoints
                2 -> config.three2sPoints
                3 -> config.three3sPoints
                4 -> config.three4sPoints
                5 -> config.three5sPoints
                6 -> config.three6sPoints
                else -> 0
            }
            4 -> config.fourOfAKindPoints
            5 -> config.fiveOfAKindPoints
            6 -> config.sixOfAKindPoints
            else -> 0
        }

        return ScoringResult(
            points = points,
            diceUsed = List(count) { face },
            name = name,
        )
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew :game-engine:test --tests "com.orangezest.farkle.engine.ScoringRulesTest"`
Expected: All tests PASS.

- [ ] **Step 5: Commit**

```bash
git add game-engine/src/
git commit -m "feat(engine): add NOfAKindRule (open class) for three/four/five/six of a kind"
```

---

## Task 7: Scoring Rules — Combination Rules (Straight, Pairs, Triplets)

**Files:**
- Modify: `game-engine/src/main/kotlin/com/orangezest/farkle/engine/ScoringRules.kt`
- Modify: `game-engine/src/test/kotlin/com/orangezest/farkle/engine/ScoringRulesTest.kt`

- [ ] **Step 1: Write the failing tests**

Add to `ScoringRulesTest.kt`:
```kotlin
    @Nested
    inner class StraightRule {
        private val rule = StraightRule(config)

        @Test
        fun `1-2-3-4-5-6 scores 1500`() {
            val result = rule.evaluate(listOf(1, 2, 3, 4, 5, 6))
            assertEquals(1500, result?.points)
            assertEquals(listOf(1, 2, 3, 4, 5, 6).sorted(), result?.diceUsed?.sorted())
        }

        @Test
        fun `order does not matter`() {
            val result = rule.evaluate(listOf(6, 5, 4, 3, 2, 1))
            assertEquals(1500, result?.points)
        }

        @Test
        fun `returns null for partial straight`() {
            assertNull(rule.evaluate(listOf(1, 2, 3, 4, 5, 5)))
        }

        @Test
        fun `requires exactly 6 dice`() {
            assertNull(rule.evaluate(listOf(1, 2, 3, 4, 5)))
        }
    }

    @Nested
    inner class ThreePairsRule {
        private val rule = ThreePairsRule(config)

        @Test
        fun `three pairs scores 1500`() {
            val result = rule.evaluate(listOf(2, 2, 3, 3, 6, 6))
            assertEquals(1500, result?.points)
            assertEquals(6, result?.diceUsed?.size)
        }

        @Test
        fun `order does not matter`() {
            val result = rule.evaluate(listOf(1, 5, 1, 5, 3, 3))
            assertEquals(1500, result?.points)
        }

        @Test
        fun `returns null for two pairs`() {
            assertNull(rule.evaluate(listOf(2, 2, 3, 3, 4, 5)))
        }

        @Test
        fun `requires exactly 6 dice`() {
            assertNull(rule.evaluate(listOf(2, 2, 3, 3)))
        }

        @Test
        fun `three of a kind counts as a pair plus extra - not three pairs`() {
            assertNull(rule.evaluate(listOf(2, 2, 2, 3, 3, 4)))
        }
    }

    @Nested
    inner class FourPlusAPairRule {
        private val rule = FourPlusAPairRule(config)

        @Test
        fun `four plus a pair scores 1500`() {
            val result = rule.evaluate(listOf(3, 3, 3, 3, 5, 5))
            assertEquals(1500, result?.points)
            assertEquals(6, result?.diceUsed?.size)
        }

        @Test
        fun `returns null for four without a pair`() {
            assertNull(rule.evaluate(listOf(3, 3, 3, 3, 5, 6)))
        }

        @Test
        fun `requires exactly 6 dice`() {
            assertNull(rule.evaluate(listOf(3, 3, 3, 3)))
        }
    }

    @Nested
    inner class TwoTripletsRule {
        private val rule = TwoTripletsRule(config)

        @Test
        fun `two triplets scores 2500`() {
            val result = rule.evaluate(listOf(2, 2, 2, 5, 5, 5))
            assertEquals(2500, result?.points)
            assertEquals(6, result?.diceUsed?.size)
        }

        @Test
        fun `returns null for one triplet`() {
            assertNull(rule.evaluate(listOf(2, 2, 2, 3, 4, 5)))
        }

        @Test
        fun `requires exactly 6 dice`() {
            assertNull(rule.evaluate(listOf(2, 2, 2)))
        }
    }
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :game-engine:test --tests "com.orangezest.farkle.engine.ScoringRulesTest"`
Expected: Compilation failure.

- [ ] **Step 3: Write minimal implementation**

Add to `ScoringRules.kt`:
```kotlin
class StraightRule(private val config: GameConfig) : ScoringRule {
    override val name: String = "Straight"

    override fun evaluate(dice: List<Int>): ScoringResult? {
        if (dice.size != 6) return null
        if (dice.sorted() != listOf(1, 2, 3, 4, 5, 6)) return null
        return ScoringResult(
            points = config.straightPoints,
            diceUsed = dice.sorted(),
            name = name,
        )
    }
}

class ThreePairsRule(private val config: GameConfig) : ScoringRule {
    override val name: String = "Three Pairs"

    override fun evaluate(dice: List<Int>): ScoringResult? {
        if (dice.size != 6) return null
        val counts = dice.groupingBy { it }.eachCount()
        if (counts.size != 3 || !counts.values.all { it == 2 }) return null
        return ScoringResult(
            points = config.threePairsPoints,
            diceUsed = dice.sorted(),
            name = name,
        )
    }
}

class FourPlusAPairRule(private val config: GameConfig) : ScoringRule {
    override val name: String = "Four + a Pair"

    override fun evaluate(dice: List<Int>): ScoringResult? {
        if (dice.size != 6) return null
        val counts = dice.groupingBy { it }.eachCount()
        if (counts.size != 2) return null
        val values = counts.values.sorted()
        if (values != listOf(2, 4)) return null
        return ScoringResult(
            points = config.fourPlusAPairPoints,
            diceUsed = dice.sorted(),
            name = name,
        )
    }
}

class TwoTripletsRule(private val config: GameConfig) : ScoringRule {
    override val name: String = "Two Triplets"

    override fun evaluate(dice: List<Int>): ScoringResult? {
        if (dice.size != 6) return null
        val counts = dice.groupingBy { it }.eachCount()
        if (counts.size != 2 || !counts.values.all { it == 3 }) return null
        return ScoringResult(
            points = config.twoTripletsPoints,
            diceUsed = dice.sorted(),
            name = name,
        )
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew :game-engine:test --tests "com.orangezest.farkle.engine.ScoringRulesTest"`
Expected: All tests PASS.

- [ ] **Step 5: Commit**

```bash
git add game-engine/src/
git commit -m "feat(engine): add Straight, ThreePairs, FourPlusAPair, TwoTriplets rules"
```

---

## Task 8: Scoring Engine

**Files:**
- Create: `game-engine/src/main/kotlin/com/orangezest/farkle/engine/ScoringEngine.kt`
- Create: `game-engine/src/test/kotlin/com/orangezest/farkle/engine/ScoringEngineTest.kt`

- [ ] **Step 1: Write the failing tests**

`game-engine/src/test/kotlin/com/orangezest/farkle/engine/ScoringEngineTest.kt`:
```kotlin
package com.orangezest.farkle.engine

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ScoringEngineTest {

    private val engine = ScoringEngine(GameConfig.DEFAULT)

    @Test
    fun `finds single 1 as scoring option`() {
        val options = engine.findScoringOptions(listOf(1, 2, 3, 4, 6, 6))
        assertTrue(options.any { it.name == "Single 1" && it.points == 100 })
    }

    @Test
    fun `finds single 5 as scoring option`() {
        val options = engine.findScoringOptions(listOf(5, 2, 3, 4, 6, 6))
        assertTrue(options.any { it.name == "Single 5" && it.points == 50 })
    }

    @Test
    fun `finds three of a kind`() {
        val options = engine.findScoringOptions(listOf(3, 3, 3, 2, 4, 6))
        assertTrue(options.any { it.name == "3 of a Kind" && it.points == 300 })
    }

    @Test
    fun `finds straight`() {
        val options = engine.findScoringOptions(listOf(1, 2, 3, 4, 5, 6))
        assertTrue(options.any { it.name == "Straight" && it.points == 1500 })
    }

    @Test
    fun `empty scoring options for non-scoring roll`() {
        val options = engine.findScoringOptions(listOf(2, 3, 4, 6, 6, 4))
        assertTrue(options.isEmpty())
    }

    @Test
    fun `isFarkle returns true for non-scoring roll`() {
        assertTrue(engine.isFarkle(listOf(2, 3, 4, 6, 6, 4)))
    }

    @Test
    fun `isFarkle returns false when scoring options exist`() {
        assertEquals(false, engine.isFarkle(listOf(1, 2, 3, 4, 6, 6)))
    }

    @Test
    fun `validates correct dice selection`() {
        val roll = listOf(1, 1, 3, 4, 5, 6)
        val selection = listOf(1, 1)
        val result = engine.scoreSelection(roll, selection)
        assertEquals(200, result)
    }

    @Test
    fun `validates single die selection from multiple scoring dice`() {
        val roll = listOf(1, 1, 5, 4, 3, 6)
        val selection = listOf(5)
        val result = engine.scoreSelection(roll, selection)
        assertEquals(50, result)
    }

    @Test
    fun `rejects invalid dice selection`() {
        val roll = listOf(2, 3, 4, 6, 6, 4)
        val selection = listOf(2)
        val result = engine.scoreSelection(roll, selection)
        assertEquals(0, result)
    }

    @Test
    fun `scores three of a kind selection`() {
        val roll = listOf(4, 4, 4, 2, 3, 6)
        val selection = listOf(4, 4, 4)
        val result = engine.scoreSelection(roll, selection)
        assertEquals(400, result)
    }

    @Test
    fun `scores mixed selection of singles and three of a kind`() {
        val roll = listOf(1, 4, 4, 4, 5, 6)
        val selection = listOf(1, 4, 4, 4, 5)
        val result = engine.scoreSelection(roll, selection)
        assertEquals(550, result)
    }

    @Test
    fun `uses custom config point values`() {
        val custom = GameConfig.DEFAULT.copy(single1Points = 200)
        val customEngine = ScoringEngine(custom)
        val options = customEngine.findScoringOptions(listOf(1, 2, 3, 4, 6, 6))
        val single1 = options.first { it.name == "Single 1" }
        assertEquals(200, single1.points)
    }

    @Test
    fun `four-plus-a-pair takes priority over four-of-a-kind plus singles`() {
        val roll = listOf(1, 1, 1, 1, 5, 5)
        val score = engine.scoreSelection(roll, roll)
        assertEquals(1500, score) // Four+Pair, not 1000+100
    }

    @Test
    fun `two-triplets takes priority over individual three-of-a-kinds`() {
        val roll = listOf(1, 1, 1, 5, 5, 5)
        val score = engine.scoreSelection(roll, roll)
        assertEquals(2500, score) // Two Triplets, not 300+500
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :game-engine:test --tests "com.orangezest.farkle.engine.ScoringEngineTest"`
Expected: Compilation failure — `ScoringEngine` doesn't exist.

- [ ] **Step 3: Write minimal implementation**

`game-engine/src/main/kotlin/com/orangezest/farkle/engine/ScoringEngine.kt`:
```kotlin
package com.orangezest.farkle.engine

class ScoringEngine(private val config: GameConfig) {

    private val rules: List<ScoringRule> by lazy {
        listOf(
            SixOfAKindRule(config),
            FiveOfAKindRule(config),
            FourPlusAPairRule(config),
            FourOfAKindRule(config),
            TwoTripletsRule(config),
            ThreePairsRule(config),
            StraightRule(config),
            ThreeOfAKindRule(config),
            SinglesRule(1, config),
            SinglesRule(5, config),
        )
    }

    fun findScoringOptions(dice: List<Int>): List<ScoringResult> =
        rules.mapNotNull { it.evaluate(dice) }

    fun isFarkle(dice: List<Int>): Boolean =
        findScoringOptions(dice).isEmpty()

    fun scoreSelection(roll: List<Int>, selection: List<Int>): Int {
        if (selection.isEmpty()) return 0

        val remaining = roll.toMutableList()
        for (die in selection) {
            if (!remaining.remove(die)) return 0
        }

        return calculateScore(selection)
    }

    private fun calculateScore(dice: List<Int>): Int {
        val mutable = dice.toMutableList()
        var total = 0

        val allDiceRules = listOf(
            StraightRule(config),
            TwoTripletsRule(config),
            ThreePairsRule(config),
            FourPlusAPairRule(config),
        )
        for (rule in allDiceRules) {
            rule.evaluate(mutable)?.let { return it.points }
        }

        for (n in listOf(6, 5, 4, 3)) {
            val nOfAKind = NOfAKindRule(n, config)
            nOfAKind.evaluate(mutable)?.let { result ->
                total += result.points
                result.diceUsed.forEach { mutable.remove(it) }
            }
        }

        val ones = mutable.count { it == 1 }
        total += ones * config.single1Points
        repeat(ones) { mutable.remove(1) }

        val fives = mutable.count { it == 5 }
        total += fives * config.single5Points
        repeat(fives) { mutable.remove(5) }

        if (mutable.isNotEmpty()) return 0

        return total
    }
}

private class SixOfAKindRule(config: GameConfig) : NOfAKindRule(6, config)
private class FiveOfAKindRule(config: GameConfig) : NOfAKindRule(5, config)
private class FourOfAKindRule(config: GameConfig) : NOfAKindRule(4, config)
private class ThreeOfAKindRule(config: GameConfig) : NOfAKindRule(3, config)
```

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew :game-engine:test --tests "com.orangezest.farkle.engine.ScoringEngineTest"`
Expected: All 16 tests PASS.

- [ ] **Step 5: Commit**

```bash
git add game-engine/src/
git commit -m "feat(engine): add ScoringEngine with rule evaluation, selection validation, and priority tests"
```

---

## Task 9: Game State Types

**Files:**
- Create: `game-engine/src/main/kotlin/com/orangezest/farkle/engine/GameState.kt`
- Create: `game-engine/src/main/kotlin/com/orangezest/farkle/engine/GameEvent.kt`

- [ ] **Step 1: Create state types (no test needed — pure data definitions)**

`game-engine/src/main/kotlin/com/orangezest/farkle/engine/GameState.kt`:
```kotlin
package com.orangezest.farkle.engine

import kotlinx.serialization.Serializable

@Serializable
data class Player(
    val name: String,
    val totalScore: Int = 0,
    val isOnBoard: Boolean = false,
)

@Serializable
sealed interface TurnPhase {
    @Serializable
    data object WaitingToRoll : TurnPhase

    @Serializable
    data class SelectingDice(
        val rollResult: List<Int>,
        val selectedIndices: Set<Int> = emptySet(),
        val turnScore: Int = 0,
    ) : TurnPhase

    @Serializable
    data class Farkled(val lostPoints: Int) : TurnPhase

    @Serializable
    data class OfferSteal(
        val remainingDice: Int,
        val previousTotal: Int,
    ) : TurnPhase

    @Serializable
    data object PassingDevice : TurnPhase

    @Serializable
    data class GameOver(val winnerIndex: Int) : TurnPhase
}

@Serializable
data class GameUiState(
    val players: List<Player>,
    val currentPlayerIndex: Int = 0,
    val turnPhase: TurnPhase = TurnPhase.WaitingToRoll,
    val runningTotal: Int = 0,
    val diceKept: List<Int> = emptyList(),
    val remainingDiceCount: Int = 6,
    val config: GameConfig = GameConfig.DEFAULT,
    @kotlinx.serialization.Transient
    val stateHistory: List<GameUiState> = emptyList(),
    val lastBankedAmount: Int = 0,
    val lastBankedDiceRemaining: Int = 0,
    val finalRoundTriggerIndex: Int? = null,
) {
    init {
        require(players.isEmpty() || currentPlayerIndex in players.indices) {
            "currentPlayerIndex $currentPlayerIndex out of bounds for ${players.size} players"
        }
    }
    val currentPlayer: Player get() = players[currentPlayerIndex]
}
```

- [ ] **Step 2: Create event types**

`game-engine/src/main/kotlin/com/orangezest/farkle/engine/GameEvent.kt`:
```kotlin
package com.orangezest.farkle.engine

sealed interface GameEvent {
    data object RollDice : GameEvent
    data class ToggleDie(val index: Int) : GameEvent
    data object Bank : GameEvent
    data object Undo : GameEvent
    data object AcknowledgeFarkle : GameEvent
    data object ReadyForTurn : GameEvent
    data object StartFresh : GameEvent
    data class StealRoll(val remainingDice: Int, val previousTotal: Int) : GameEvent
}
```

- [ ] **Step 3: Verify compilation**

Run: `./gradlew :game-engine:compileKotlin`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 4: Commit**

```bash
git add game-engine/src/
git commit -m "feat(engine): add GameUiState, TurnPhase, Player, and GameEvent types"
```

---

## Task 10: Game Reducer — Core Turn Flow

**Files:**
- Create: `game-engine/src/main/kotlin/com/orangezest/farkle/engine/GameReducer.kt`
- Create: `game-engine/src/test/kotlin/com/orangezest/farkle/engine/GameReducerTest.kt`

- [ ] **Step 1: Write failing tests for basic turn flow**

`game-engine/src/test/kotlin/com/orangezest/farkle/engine/GameReducerTest.kt`:
```kotlin
package com.orangezest.farkle.engine

import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class GameReducerTest {

    private val twoPlayers = listOf(Player("Alice"), Player("Bob"))
    private val fakeDice = FakeDiceRoller(listOf(1, 5, 3, 4, 6, 2))

    private fun reducer(diceRoller: DiceRoller = fakeDice) =
        GameReducer(ScoringEngine(GameConfig.DEFAULT), diceRoller)

    private fun initialState(players: List<Player> = twoPlayers) =
        GameUiState(players = players)

    @Nested
    inner class RollDice {
        @Test
        fun `transitions from WaitingToRoll to SelectingDice`() {
            val state = initialState()
            val newState = reducer().reduce(state, GameEvent.RollDice)
            assertIs<TurnPhase.SelectingDice>(newState.turnPhase)
        }

        @Test
        fun `roll result contains correct number of dice`() {
            val state = initialState()
            val newState = reducer().reduce(state, GameEvent.RollDice)
            val phase = newState.turnPhase as TurnPhase.SelectingDice
            assertEquals(6, phase.rollResult.size)
        }

        @Test
        fun `rolls only remaining dice after keeping some`() {
            val state = initialState().copy(remainingDiceCount = 3)
            val roller = FakeDiceRoller(listOf(1, 5, 3))
            val newState = reducer(roller).reduce(state, GameEvent.RollDice)
            val phase = newState.turnPhase as TurnPhase.SelectingDice
            assertEquals(3, phase.rollResult.size)
        }

        @Test
        fun `farkle when no scoring dice rolled`() {
            val noScoreRoller = FakeDiceRoller(listOf(2, 3, 4, 6, 6, 4))
            val state = initialState()
            val newState = reducer(noScoreRoller).reduce(state, GameEvent.RollDice)
            assertIs<TurnPhase.Farkled>(newState.turnPhase)
        }

        @Test
        fun `farkle loses running total`() {
            val noScoreRoller = FakeDiceRoller(listOf(2, 3, 4, 6, 6, 4))
            val state = initialState().copy(runningTotal = 300)
            val newState = reducer(noScoreRoller).reduce(state, GameEvent.RollDice)
            val phase = newState.turnPhase as TurnPhase.Farkled
            assertEquals(300, phase.lostPoints)
            assertEquals(0, newState.runningTotal)
        }

        @Test
        fun `saves state to history before rolling`() {
            val state = initialState()
            val newState = reducer().reduce(state, GameEvent.RollDice)
            assertEquals(1, newState.stateHistory.size)
        }
    }

    @Nested
    inner class ToggleDie {
        @Test
        fun `selects a die by index`() {
            val state = initialState().copy(
                turnPhase = TurnPhase.SelectingDice(
                    rollResult = listOf(1, 5, 3, 4, 6, 2),
                )
            )
            val newState = reducer().reduce(state, GameEvent.ToggleDie(0))
            val phase = newState.turnPhase as TurnPhase.SelectingDice
            assertTrue(0 in phase.selectedIndices)
        }

        @Test
        fun `deselects a previously selected die`() {
            val state = initialState().copy(
                turnPhase = TurnPhase.SelectingDice(
                    rollResult = listOf(1, 5, 3, 4, 6, 2),
                    selectedIndices = setOf(0),
                )
            )
            val newState = reducer().reduce(state, GameEvent.ToggleDie(0))
            val phase = newState.turnPhase as TurnPhase.SelectingDice
            assertTrue(0 !in phase.selectedIndices)
        }
    }

    @Nested
    inner class Bank {
        @Test
        fun `adds running total plus selection to player score`() {
            val state = initialState().copy(
                turnPhase = TurnPhase.SelectingDice(
                    rollResult = listOf(1, 5, 3, 4, 6, 2),
                    selectedIndices = setOf(0, 1),
                ),
                runningTotal = 200,
            )
            val newState = reducer().reduce(state, GameEvent.Bank)
            assertEquals(350, newState.players[0].totalScore)
        }

        @Test
        fun `transitions to PassingDevice`() {
            val state = initialState().copy(
                turnPhase = TurnPhase.SelectingDice(
                    rollResult = listOf(1, 5, 3, 4, 6, 2),
                    selectedIndices = setOf(0, 1),
                ),
                runningTotal = 400,
            )
            val newState = reducer().reduce(state, GameEvent.Bank)
            assertIs<TurnPhase.PassingDevice>(newState.turnPhase)
        }

        @Test
        fun `player gets on board when banking at least minimum`() {
            val state = initialState().copy(
                turnPhase = TurnPhase.SelectingDice(
                    rollResult = listOf(1, 1, 1, 5, 5, 2),
                    selectedIndices = setOf(0, 1, 2, 3, 4),
                ),
                runningTotal = 0,
            )
            val newState = reducer().reduce(state, GameEvent.Bank)
            assertTrue(newState.players[0].isOnBoard)
        }

        @Test
        fun `cannot bank below minimum when not on board`() {
            val state = initialState().copy(
                turnPhase = TurnPhase.SelectingDice(
                    rollResult = listOf(1, 5, 3, 4, 6, 2),
                    selectedIndices = setOf(0),
                ),
                runningTotal = 0,
            )
            val newState = reducer().reduce(state, GameEvent.Bank)
            assertIs<TurnPhase.SelectingDice>(newState.turnPhase)
        }
    }

    @Nested
    inner class Undo {
        @Test
        fun `restores previous state from history`() {
            val previousState = initialState()
            val currentState = previousState.copy(
                runningTotal = 150,
                stateHistory = listOf(previousState),
            )
            val newState = reducer().reduce(currentState, GameEvent.Undo)
            assertEquals(0, newState.runningTotal)
            assertTrue(newState.stateHistory.isEmpty())
        }

        @Test
        fun `no-op when history is empty`() {
            val state = initialState()
            val newState = reducer().reduce(state, GameEvent.Undo)
            assertEquals(state, newState)
        }
    }

    @Nested
    inner class TurnTransition {
        @Test
        fun `ReadyForTurn advances to next player`() {
            val state = initialState().copy(
                turnPhase = TurnPhase.PassingDevice,
                currentPlayerIndex = 0,
            )
            val newState = reducer().reduce(state, GameEvent.ReadyForTurn)
            assertEquals(1, newState.currentPlayerIndex)
            assertIs<TurnPhase.WaitingToRoll>(newState.turnPhase)
        }

        @Test
        fun `wraps around to first player`() {
            val state = initialState().copy(
                turnPhase = TurnPhase.PassingDevice,
                currentPlayerIndex = 1,
            )
            val newState = reducer().reduce(state, GameEvent.ReadyForTurn)
            assertEquals(0, newState.currentPlayerIndex)
        }

        @Test
        fun `resets running total and dice count on new turn`() {
            val state = initialState().copy(
                turnPhase = TurnPhase.PassingDevice,
                runningTotal = 500,
                remainingDiceCount = 3,
            )
            val newState = reducer().reduce(state, GameEvent.ReadyForTurn)
            assertEquals(0, newState.runningTotal)
            assertEquals(6, newState.remainingDiceCount)
        }

        @Test
        fun `AcknowledgeFarkle transitions to PassingDevice`() {
            val state = initialState().copy(
                turnPhase = TurnPhase.Farkled(lostPoints = 300),
            )
            val newState = reducer().reduce(state, GameEvent.AcknowledgeFarkle)
            assertIs<TurnPhase.PassingDevice>(newState.turnPhase)
        }
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :game-engine:test --tests "com.orangezest.farkle.engine.GameReducerTest"`
Expected: Compilation failure — `GameReducer` doesn't exist.

- [ ] **Step 3: Write minimal implementation**

`game-engine/src/main/kotlin/com/orangezest/farkle/engine/GameReducer.kt`:
```kotlin
package com.orangezest.farkle.engine

class GameReducer(
    private val scoringEngine: ScoringEngine,
    private val diceRoller: DiceRoller,
) {
    fun reduce(state: GameUiState, event: GameEvent): GameUiState = when (event) {
        is GameEvent.RollDice -> handleRoll(state)
        is GameEvent.ToggleDie -> handleToggle(state, event.index)
        is GameEvent.Bank -> handleBank(state)
        is GameEvent.Undo -> handleUndo(state)
        is GameEvent.AcknowledgeFarkle -> handleAcknowledgeFarkle(state)
        is GameEvent.ReadyForTurn -> handleReadyForTurn(state)
        is GameEvent.StartFresh -> handleStartFresh(state)
        is GameEvent.StealRoll -> handleStealRoll(state, event)
    }

    private fun handleRoll(state: GameUiState): GameUiState {
        val stateWithHistory = state.copy(
            stateHistory = state.stateHistory + state.copy(stateHistory = emptyList())
        )
        val roll = diceRoller.roll(state.remainingDiceCount)

        return if (scoringEngine.isFarkle(roll)) {
            stateWithHistory.copy(
                turnPhase = TurnPhase.Farkled(lostPoints = state.runningTotal),
                runningTotal = 0,
            )
        } else {
            stateWithHistory.copy(
                turnPhase = TurnPhase.SelectingDice(rollResult = roll),
            )
        }
    }

    private fun handleToggle(state: GameUiState, index: Int): GameUiState {
        val phase = state.turnPhase as? TurnPhase.SelectingDice ?: return state
        val newSelected = if (index in phase.selectedIndices) {
            phase.selectedIndices - index
        } else {
            phase.selectedIndices + index
        }
        return state.copy(
            turnPhase = phase.copy(selectedIndices = newSelected),
        )
    }

    private fun handleBank(state: GameUiState): GameUiState {
        val phase = state.turnPhase as? TurnPhase.SelectingDice ?: return state
        if (phase.selectedIndices.isEmpty()) return state

        val selectedDice = phase.selectedIndices.map { phase.rollResult[it] }
        val selectionScore = scoringEngine.scoreSelection(phase.rollResult, selectedDice)
        if (selectionScore == 0) return state

        val totalTurnScore = state.runningTotal + selectionScore
        val player = state.currentPlayer

        if (!player.isOnBoard && totalTurnScore < state.config.minimumToBoard) {
            return state
        }

        val updatedPlayer = player.copy(
            totalScore = player.totalScore + totalTurnScore,
            isOnBoard = true,
        )
        val updatedPlayers = state.players.toMutableList().apply {
            set(state.currentPlayerIndex, updatedPlayer)
        }

        val remainingDice = state.remainingDiceCount - phase.selectedIndices.size

        if (updatedPlayer.totalScore >= state.config.targetScore && state.finalRoundTriggerIndex == null) {
            return state.copy(
                players = updatedPlayers,
                turnPhase = TurnPhase.PassingDevice,
                runningTotal = 0,
                remainingDiceCount = 6,
                lastBankedAmount = totalTurnScore,
                lastBankedDiceRemaining = remainingDice,
                stateHistory = emptyList(),
                finalRoundTriggerIndex = state.currentPlayerIndex,
            )
        }

        return state.copy(
            players = updatedPlayers,
            turnPhase = TurnPhase.PassingDevice,
            runningTotal = 0,
            remainingDiceCount = 6,
            lastBankedAmount = totalTurnScore,
            lastBankedDiceRemaining = remainingDice,
            stateHistory = emptyList(),
        )
    }

    private fun handleUndo(state: GameUiState): GameUiState {
        if (state.stateHistory.isEmpty()) return state
        return state.stateHistory.last()
    }

    private fun handleAcknowledgeFarkle(state: GameUiState): GameUiState {
        if (state.turnPhase !is TurnPhase.Farkled) return state
        return state.copy(turnPhase = TurnPhase.PassingDevice)
    }

    private fun handleReadyForTurn(state: GameUiState): GameUiState {
        if (state.turnPhase != TurnPhase.PassingDevice) return state
        val nextPlayerIndex = (state.currentPlayerIndex + 1) % state.players.size

        if (state.finalRoundTriggerIndex != null && nextPlayerIndex == state.finalRoundTriggerIndex) {
            val winnerIndex = state.players.indices.maxBy { state.players[it].totalScore }
            return state.copy(
                turnPhase = TurnPhase.GameOver(winnerIndex = winnerIndex),
            )
        }

        if (state.config.piggybackingEnabled && state.lastBankedAmount > 0) {
            return state.copy(
                currentPlayerIndex = nextPlayerIndex,
                turnPhase = TurnPhase.OfferSteal(
                    remainingDice = state.lastBankedDiceRemaining,
                    previousTotal = state.lastBankedAmount,
                ),
                runningTotal = 0,
                remainingDiceCount = 6,
                diceKept = emptyList(),
                stateHistory = emptyList(),
            )
        }

        return state.copy(
            currentPlayerIndex = nextPlayerIndex,
            turnPhase = TurnPhase.WaitingToRoll,
            runningTotal = 0,
            remainingDiceCount = 6,
            diceKept = emptyList(),
            stateHistory = emptyList(),
        )
    }

    private fun handleStartFresh(state: GameUiState): GameUiState {
        return state.copy(
            turnPhase = TurnPhase.WaitingToRoll,
            runningTotal = 0,
            remainingDiceCount = 6,
        )
    }

    private fun handleStealRoll(state: GameUiState, event: GameEvent.StealRoll): GameUiState {
        return state.copy(
            turnPhase = TurnPhase.WaitingToRoll,
            runningTotal = event.previousTotal,
            remainingDiceCount = event.remainingDice,
        )
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew :game-engine:test --tests "com.orangezest.farkle.engine.GameReducerTest"`
Expected: All tests PASS.

- [ ] **Step 5: Commit**

```bash
git add game-engine/src/
git commit -m "feat(engine): add GameReducer with core turn flow, banking, undo, and turn transitions"
```

---

## Task 11: Game Reducer — Hot Dice, Final Round, Piggybacking

**Files:**
- Modify: `game-engine/src/main/kotlin/com/orangezest/farkle/engine/GameReducer.kt`
- Modify: `game-engine/src/test/kotlin/com/orangezest/farkle/engine/GameReducerTest.kt`

- [ ] **Step 1: Write failing tests for hot dice, final round, and piggybacking**

Add to `GameReducerTest.kt`:
```kotlin
    @Nested
    inner class HotDice {
        @Test
        fun `continuing after selecting all dice resets to 6 dice with running total`() {
            val state = initialState().copy(
                turnPhase = TurnPhase.SelectingDice(
                    rollResult = listOf(1, 5),
                    selectedIndices = setOf(0, 1),
                ),
                runningTotal = 300,
                remainingDiceCount = 2,
            )
            val roller = FakeDiceRoller(listOf(1, 2, 3, 4, 5, 6))
            val newState = reducer(roller).reduce(state, GameEvent.RollDice)
            val phase = newState.turnPhase as TurnPhase.SelectingDice
            assertEquals(6, phase.rollResult.size)
            assertEquals(450, newState.runningTotal)
        }

        @Test
        fun `hot dice disabled - cannot roll with zero remaining dice`() {
            val config = GameConfig.DEFAULT.copy(hotDiceEnabled = false)
            val engine = ScoringEngine(config)
            val roller = FakeDiceRoller(listOf(1, 5))
            val noHotReducer = GameReducer(engine, roller)

            val state = GameUiState(
                players = twoPlayers,
                turnPhase = TurnPhase.SelectingDice(
                    rollResult = listOf(1, 5),
                    selectedIndices = setOf(0, 1),
                ),
                runningTotal = 300,
                remainingDiceCount = 2,
                config = config,
            )
            // With hot dice off, selecting all dice should not allow another roll
            // (0 remaining, no reset to 6). Must bank instead.
            val newState = noHotReducer.reduce(state, GameEvent.RollDice)
            assertEquals(state.turnPhase, newState.turnPhase) // unchanged
        }
    }

    @Nested
    inner class FinalRound {
        @Test
        fun `reaching target score sets finalRoundTriggerIndex and transitions to PassingDevice`() {
            val state = initialState().copy(
                players = listOf(Player("Alice", totalScore = 9800, isOnBoard = true), Player("Bob")),
                turnPhase = TurnPhase.SelectingDice(
                    rollResult = listOf(1, 1, 5, 4, 6, 2),
                    selectedIndices = setOf(0, 1, 2),
                ),
                runningTotal = 0,
            )
            val newState = reducer().reduce(state, GameEvent.Bank)
            assertIs<TurnPhase.PassingDevice>(newState.turnPhase)
            assertEquals(0, newState.finalRoundTriggerIndex)
            assertEquals(10050, newState.players[0].totalScore)
        }

        @Test
        fun `other players get one turn after final round triggers`() {
            val state = initialState().copy(
                players = listOf(
                    Player("Alice", totalScore = 10050, isOnBoard = true),
                    Player("Bob", totalScore = 5000, isOnBoard = true),
                ),
                turnPhase = TurnPhase.PassingDevice,
                currentPlayerIndex = 0,
                finalRoundTriggerIndex = 0,
            )
            // Bob gets his turn
            val newState = reducer().reduce(state, GameEvent.ReadyForTurn)
            assertEquals(1, newState.currentPlayerIndex)
            assertIs<TurnPhase.WaitingToRoll>(newState.turnPhase)
        }

        @Test
        fun `game ends when rotation returns to trigger player`() {
            val state = initialState().copy(
                players = listOf(
                    Player("Alice", totalScore = 10050, isOnBoard = true),
                    Player("Bob", totalScore = 5300, isOnBoard = true),
                ),
                turnPhase = TurnPhase.PassingDevice,
                currentPlayerIndex = 1,
                finalRoundTriggerIndex = 0,
            )
            // Next player would be Alice (index 0) who triggered — game ends
            val newState = reducer().reduce(state, GameEvent.ReadyForTurn)
            assertIs<TurnPhase.GameOver>(newState.turnPhase)
            // Alice has highest score
            assertEquals(0, (newState.turnPhase as TurnPhase.GameOver).winnerIndex)
        }

        @Test
        fun `player who didn't trigger can win with higher score in final round`() {
            // Alice triggered at 10050, Bob has 9900, Bob banks 300 on final turn
            // Bob ends at 10200 > Alice's 10050 → Bob wins
            val state = initialState().copy(
                players = listOf(
                    Player("Alice", totalScore = 10050, isOnBoard = true),
                    Player("Bob", totalScore = 10200, isOnBoard = true),
                ),
                turnPhase = TurnPhase.PassingDevice,
                currentPlayerIndex = 1,
                finalRoundTriggerIndex = 0,
            )
            val newState = reducer().reduce(state, GameEvent.ReadyForTurn)
            assertIs<TurnPhase.GameOver>(newState.turnPhase)
            assertEquals(1, (newState.turnPhase as TurnPhase.GameOver).winnerIndex)
        }
    }

    @Nested
    inner class GameEnd {
        @Test
        fun `player score updates when reaching target`() {
            val state = initialState().copy(
                players = listOf(Player("Alice", totalScore = 9800, isOnBoard = true), Player("Bob")),
                turnPhase = TurnPhase.SelectingDice(
                    rollResult = listOf(1, 1, 5, 4, 6, 2),
                    selectedIndices = setOf(0, 1, 2),
                ),
                runningTotal = 0,
            )
            val newState = reducer().reduce(state, GameEvent.Bank)
            assertEquals(10050, newState.players[0].totalScore)
        }
    }

    @Nested
    inner class Piggybacking {
        @Test
        fun `offers steal when piggybacking enabled and previous player banked`() {
            val config = GameConfig.DEFAULT.copy(piggybackingEnabled = true)
            val state = GameUiState(
                players = twoPlayers,
                currentPlayerIndex = 0,
                turnPhase = TurnPhase.PassingDevice,
                config = config,
                lastBankedAmount = 400,
                lastBankedDiceRemaining = 3,
            )
            val engine = ScoringEngine(config)
            val r = GameReducer(engine, fakeDice)
            val newState = r.reduce(state, GameEvent.ReadyForTurn)
            assertIs<TurnPhase.OfferSteal>(newState.turnPhase)
            val offer = newState.turnPhase as TurnPhase.OfferSteal
            assertEquals(3, offer.remainingDice)
            assertEquals(400, offer.previousTotal)
        }

        @Test
        fun `StealRoll starts with previous total and remaining dice`() {
            val state = GameUiState(
                players = twoPlayers,
                currentPlayerIndex = 1,
                turnPhase = TurnPhase.OfferSteal(remainingDice = 3, previousTotal = 400),
            )
            val newState = reducer().reduce(state, GameEvent.StealRoll(remainingDice = 3, previousTotal = 400))
            assertIs<TurnPhase.WaitingToRoll>(newState.turnPhase)
            assertEquals(400, newState.runningTotal)
            assertEquals(3, newState.remainingDiceCount)
        }

        @Test
        fun `StartFresh ignores previous total`() {
            val state = GameUiState(
                players = twoPlayers,
                currentPlayerIndex = 1,
                turnPhase = TurnPhase.OfferSteal(remainingDice = 3, previousTotal = 400),
            )
            val newState = reducer().reduce(state, GameEvent.StartFresh)
            assertIs<TurnPhase.WaitingToRoll>(newState.turnPhase)
            assertEquals(0, newState.runningTotal)
            assertEquals(6, newState.remainingDiceCount)
        }
    }
```

- [ ] **Step 2: Run test to verify failures**

Run: `./gradlew :game-engine:test --tests "com.orangezest.farkle.engine.GameReducerTest"`
Expected: New tests may fail if hot dice logic isn't in the roll handler yet.

- [ ] **Step 3: Update GameReducer to handle hot dice on RollDice**

Update `handleRoll` in `GameReducer.kt`:
```kotlin
    private fun handleRoll(state: GameUiState): GameUiState {
        val phase = state.turnPhase
        var currentRunning = state.runningTotal
        var diceCount = state.remainingDiceCount

        if (phase is TurnPhase.SelectingDice && phase.selectedIndices.isNotEmpty()) {
            val selectedDice = phase.selectedIndices.map { phase.rollResult[it] }
            val selectionScore = scoringEngine.scoreSelection(phase.rollResult, selectedDice)
            currentRunning += selectionScore
            diceCount -= phase.selectedIndices.size

            if (diceCount == 0 && state.config.hotDiceEnabled) {
                diceCount = 6
            }
        }

        if (diceCount == 0) return state

        val stateWithHistory = state.copy(
            stateHistory = state.stateHistory + state.copy(stateHistory = emptyList())
        )
        val roll = diceRoller.roll(diceCount)

        return if (scoringEngine.isFarkle(roll)) {
            stateWithHistory.copy(
                turnPhase = TurnPhase.Farkled(lostPoints = currentRunning),
                runningTotal = 0,
                remainingDiceCount = diceCount,
            )
        } else {
            stateWithHistory.copy(
                turnPhase = TurnPhase.SelectingDice(rollResult = roll),
                runningTotal = currentRunning,
                remainingDiceCount = diceCount,
            )
        }
    }
```

- [ ] **Step 4: Run tests to verify they pass**

Run: `./gradlew :game-engine:test --tests "com.orangezest.farkle.engine.GameReducerTest"`
Expected: All tests PASS.

- [ ] **Step 5: Commit**

```bash
git add game-engine/src/
git commit -m "feat(engine): add hot dice, final round mechanic, and piggybacking to reducer"
```

---

## Task 12: Rotation Touch-Target Spike

**Files:**
- Create: `app/src/main/kotlin/com/orangezest/farkle/spike/RotationSpike.kt` (temporary)

- [ ] **Step 1: Create minimal compose screen with rotated button**

`app/src/main/kotlin/com/orangezest/farkle/spike/RotationSpike.kt`:
```kotlin
package com.orangezest.farkle.spike

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.unit.dp

@Composable
fun RotationSpike() {
    var tapCount by remember { mutableIntStateOf(0) }

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceEvenly,
    ) {
        Text("Tap count: $tapCount")

        // Test: does tap target work when rotated 180 degrees?
        Box(modifier = Modifier.rotate(180f)) {
            Button(
                onClick = { tapCount++ },
                modifier = Modifier.size(width = 200.dp, height = 56.dp),
            ) {
                Text("Rotated Button")
            }
        }

        // Control: unrotated button
        Button(
            onClick = { tapCount++ },
            modifier = Modifier.size(width = 200.dp, height = 56.dp),
        ) {
            Text("Normal Button")
        }
    }
}
```

- [ ] **Step 2: Build debug APK via CI**

Run: `./gradlew :app:assembleDebug`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: Test on Samsung foldable**

Install APK on Samsung foldable device. Verify:
- Rotated button tap targets work correctly (tap registers on the visual button area)
- No offset between visual and touch target
- Works on both inner and outer screens

- [ ] **Step 4: Document findings**

If broken: research alternatives (`graphicsLayer` vs `rotate`, layout-level rotation). Record which approach works for the game screen rotation feature.

- [ ] **Step 5: Delete spike code and commit**

```bash
rm -rf app/src/main/kotlin/com/orangezest/farkle/spike/
git add -A
git commit -m "spike: validate rotation touch targets on Samsung foldable (spike removed)"
```

---

## Task 13: Hilt Module and DI Setup

**Files:**
- Create: `app/src/main/kotlin/com/orangezest/farkle/di/GameModule.kt`

- [ ] **Step 1: Create the Hilt module with full DI graph**

`app/src/main/kotlin/com/orangezest/farkle/di/GameModule.kt`:
```kotlin
package com.orangezest.farkle.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.dataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import com.orangezest.farkle.data.GameStateRepository
import com.orangezest.farkle.data.GameStateSerializer
import com.orangezest.farkle.data.SettingsRepository
import com.orangezest.farkle.data.SettingsStore
import com.orangezest.farkle.data.PreferencesSettingsStore
import com.orangezest.farkle.data.proto.SavedGameState
import com.orangezest.farkle.engine.DiceRoller
import com.orangezest.farkle.engine.SecureRandomDiceRoller
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

private val Context.gameStateDataStore: DataStore<SavedGameState> by dataStore(
    fileName = "game_state.pb",
    serializer = GameStateSerializer,
)

private val Context.preferencesDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "settings",
)

@Module
@InstallIn(SingletonComponent::class)
object GameModule {

    @Provides
    @Singleton
    fun provideDiceRoller(): DiceRoller = SecureRandomDiceRoller()

    @Provides
    @Singleton
    fun provideGameStateDataStore(
        @ApplicationContext context: Context,
    ): DataStore<SavedGameState> = context.gameStateDataStore

    @Provides
    @Singleton
    fun providePreferencesDataStore(
        @ApplicationContext context: Context,
    ): DataStore<Preferences> = context.preferencesDataStore

    @Provides
    @Singleton
    fun provideSettingsStore(
        dataStore: DataStore<Preferences>,
    ): SettingsStore = PreferencesSettingsStore(dataStore)

    @Provides
    @Singleton
    fun provideSettingsRepository(
        store: SettingsStore,
    ): SettingsRepository = SettingsRepository(store)

    @Provides
    @Singleton
    fun provideGameStateRepository(
        dataStore: DataStore<SavedGameState>,
    ): GameStateRepository = GameStateRepository(dataStore)
}
```

> Note: `GameConfig`, `ScoringEngine`, and `GameReducer` are NOT provided here. `GameViewModel` creates them per-game from `SettingsRepository.gameConfig` flow, ensuring settings changes propagate to the next game.

- [ ] **Step 2: Create PreferencesSettingsStore**

`app/src/main/kotlin/com/orangezest/farkle/data/PreferencesSettingsStore.kt`:
```kotlin
package com.orangezest.farkle.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class PreferencesSettingsStore(
    private val dataStore: DataStore<Preferences>,
) : SettingsStore {

    override val settings: Flow<Map<String, Any>> = dataStore.data.map { prefs ->
        prefs.asMap().mapKeys { it.key.name }.mapValues { it.value as Any }
    }

    override suspend fun update(key: String, value: Any) {
        dataStore.edit { prefs ->
            when (value) {
                is Int -> prefs[intPreferencesKey(key)] = value
                is Boolean -> prefs[booleanPreferencesKey(key)] = value
                is String -> prefs[stringPreferencesKey(key)] = value
                is Float -> prefs[floatPreferencesKey(key)] = value
                is Long -> prefs[longPreferencesKey(key)] = value
            }
        }
    }
}
```

- [ ] **Step 3: Verify compilation**

Run: `./gradlew :app:compileDebugKotlin`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 4: Commit**

```bash
git add app/src/main/kotlin/com/orangezest/farkle/di/
git add app/src/main/kotlin/com/orangezest/farkle/data/PreferencesSettingsStore.kt
git commit -m "feat(app): add Hilt GameModule with DataStore, SettingsStore, and Repository providers"
```

---

## Task 14: Settings Repository (Preferences DataStore)

**Files:**
- Create: `app/src/main/kotlin/com/orangezest/farkle/data/SettingsRepository.kt`
- Create: `app/src/test/kotlin/com/orangezest/farkle/data/SettingsRepositoryTest.kt`

- [ ] **Step 1: Write the failing test**

`app/src/test/kotlin/com/orangezest/farkle/data/SettingsRepositoryTest.kt`:
```kotlin
package com.orangezest.farkle.data

import com.orangezest.farkle.engine.GameConfig
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class SettingsRepositoryTest {

    @TempDir
    lateinit var tempDir: File

    @Test
    fun `default settings match GameConfig defaults`() = runTest {
        val repo = SettingsRepository(InMemorySettingsStore())
        val config = repo.gameConfig.first()
        assertEquals(GameConfig.DEFAULT, config)
    }

    @Test
    fun `updates target score`() = runTest {
        val repo = SettingsRepository(InMemorySettingsStore())
        repo.updateTargetScore(5000)
        val config = repo.gameConfig.first()
        assertEquals(5000, config.targetScore)
    }

    @Test
    fun `updates piggybacking`() = runTest {
        val repo = SettingsRepository(InMemorySettingsStore())
        repo.updatePiggybacking(true)
        val config = repo.gameConfig.first()
        assertEquals(true, config.piggybackingEnabled)
    }

    @Test
    fun `updates sound enabled`() = runTest {
        val repo = SettingsRepository(InMemorySettingsStore())
        repo.updateSoundEnabled(false)
        assertFalse(repo.soundEnabled.first())
    }

    @Test
    fun `updates haptic enabled`() = runTest {
        val repo = SettingsRepository(InMemorySettingsStore())
        repo.updateHapticEnabled(false)
        assertFalse(repo.hapticEnabled.first())
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :app:test --tests "com.orangezest.farkle.data.SettingsRepositoryTest"`
Expected: Compilation failure.

- [ ] **Step 3: Write minimal implementation**

`app/src/main/kotlin/com/orangezest/farkle/data/SettingsRepository.kt`:
```kotlin
package com.orangezest.farkle.data

import com.orangezest.farkle.engine.GameConfig
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

interface SettingsStore {
    val settings: Flow<Map<String, Any>>
    suspend fun update(key: String, value: Any)
}

class InMemorySettingsStore : SettingsStore {
    private val _settings = MutableStateFlow<Map<String, Any>>(emptyMap())
    override val settings: Flow<Map<String, Any>> = _settings

    override suspend fun update(key: String, value: Any) {
        _settings.update { it + (key to value) }
    }
}

class SettingsRepository(private val store: SettingsStore) {

    val gameConfig: Flow<GameConfig> = store.settings.map { prefs ->
        GameConfig(
            targetScore = prefs["target_score"] as? Int ?: GameConfig.DEFAULT.targetScore,
            minimumToBoard = prefs["minimum_to_board"] as? Int ?: GameConfig.DEFAULT.minimumToBoard,
            hotDiceEnabled = prefs["hot_dice"] as? Boolean ?: GameConfig.DEFAULT.hotDiceEnabled,
            piggybackingEnabled = prefs["piggybacking"] as? Boolean ?: GameConfig.DEFAULT.piggybackingEnabled,
            single1Points = prefs["single_1_points"] as? Int ?: GameConfig.DEFAULT.single1Points,
            single5Points = prefs["single_5_points"] as? Int ?: GameConfig.DEFAULT.single5Points,
            three1sPoints = prefs["three_1s_points"] as? Int ?: GameConfig.DEFAULT.three1sPoints,
            three2sPoints = prefs["three_2s_points"] as? Int ?: GameConfig.DEFAULT.three2sPoints,
            three3sPoints = prefs["three_3s_points"] as? Int ?: GameConfig.DEFAULT.three3sPoints,
            three4sPoints = prefs["three_4s_points"] as? Int ?: GameConfig.DEFAULT.three4sPoints,
            three5sPoints = prefs["three_5s_points"] as? Int ?: GameConfig.DEFAULT.three5sPoints,
            three6sPoints = prefs["three_6s_points"] as? Int ?: GameConfig.DEFAULT.three6sPoints,
            fourOfAKindPoints = prefs["four_of_a_kind_points"] as? Int ?: GameConfig.DEFAULT.fourOfAKindPoints,
            fiveOfAKindPoints = prefs["five_of_a_kind_points"] as? Int ?: GameConfig.DEFAULT.fiveOfAKindPoints,
            sixOfAKindPoints = prefs["six_of_a_kind_points"] as? Int ?: GameConfig.DEFAULT.sixOfAKindPoints,
            straightPoints = prefs["straight_points"] as? Int ?: GameConfig.DEFAULT.straightPoints,
            threePairsPoints = prefs["three_pairs_points"] as? Int ?: GameConfig.DEFAULT.threePairsPoints,
            fourPlusAPairPoints = prefs["four_plus_a_pair_points"] as? Int ?: GameConfig.DEFAULT.fourPlusAPairPoints,
            twoTripletsPoints = prefs["two_triplets_points"] as? Int ?: GameConfig.DEFAULT.twoTripletsPoints,
        )
    }

    val soundEnabled: Flow<Boolean> = store.settings.map { prefs ->
        prefs["sound_enabled"] as? Boolean ?: true
    }

    val hapticEnabled: Flow<Boolean> = store.settings.map { prefs ->
        prefs["haptic_enabled"] as? Boolean ?: true
    }

    val themeMode: Flow<String> = store.settings.map { prefs ->
        prefs["theme_mode"] as? String ?: "system"
    }

    suspend fun updateTargetScore(value: Int) = store.update("target_score", value)
    suspend fun updateMinimumToBoard(value: Int) = store.update("minimum_to_board", value)
    suspend fun updateHotDice(enabled: Boolean) = store.update("hot_dice", enabled)
    suspend fun updatePiggybacking(enabled: Boolean) = store.update("piggybacking", enabled)
    suspend fun updateSoundEnabled(enabled: Boolean) = store.update("sound_enabled", enabled)
    suspend fun updateHapticEnabled(enabled: Boolean) = store.update("haptic_enabled", enabled)
    suspend fun updateThemeMode(mode: String) = store.update("theme_mode", mode)
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew :app:test --tests "com.orangezest.farkle.data.SettingsRepositoryTest"`
Expected: All 5 tests PASS.

- [ ] **Step 5: Commit**

```bash
git add app/src/
git commit -m "feat(app): add SettingsRepository with Preferences DataStore abstraction"
```

---

## Task 15: Game State Repository (Proto DataStore)

**Files:**
- Create: `app/src/main/proto/game_state.proto`
- Create: `app/src/main/kotlin/com/orangezest/farkle/data/GameStateSerializer.kt`
- Create: `app/src/main/kotlin/com/orangezest/farkle/data/GameStateRepository.kt`
- Create: `app/src/test/kotlin/com/orangezest/farkle/data/GameStateRepositoryTest.kt`

- [ ] **Step 1: Write failing tests**

`app/src/test/kotlin/com/orangezest/farkle/data/GameStateRepositoryTest.kt`:
```kotlin
package com.orangezest.farkle.data

import com.orangezest.farkle.engine.GameUiState
import com.orangezest.farkle.engine.Player
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class GameStateRepositoryTest {

    @Test
    fun `empty datastore returns null`() = runTest {
        val repo = GameStateRepository(testDataStore())
        assertNull(repo.savedGame.first())
    }

    @Test
    fun `save and read round-trips correctly`() = runTest {
        val repo = GameStateRepository(testDataStore())
        val state = GameUiState(players = listOf(Player("Alice")))
        repo.saveGame(state)
        assertEquals(state, repo.savedGame.first())
    }

    @Test
    fun `clearSavedGame returns to null`() = runTest {
        val repo = GameStateRepository(testDataStore())
        val state = GameUiState(players = listOf(Player("Alice")))
        repo.saveGame(state)
        repo.clearSavedGame()
        assertNull(repo.savedGame.first())
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :app:test --tests "com.orangezest.farkle.data.GameStateRepositoryTest"`
Expected: Compilation failure.

- [ ] **Step 3: Create proto schema**

`app/src/main/proto/game_state.proto`:
```protobuf
syntax = "proto3";

option java_package = "com.orangezest.farkle.data.proto";
option java_multiple_files = true;

message SavedGameState {
    int32 schema_version = 1;
    string json_state = 2;
}
```

- [ ] **Step 4: Create serializer and repository**

`app/src/main/kotlin/com/orangezest/farkle/data/GameStateSerializer.kt`:
```kotlin
package com.orangezest.farkle.data

import androidx.datastore.core.Serializer
import com.orangezest.farkle.data.proto.SavedGameState
import java.io.InputStream
import java.io.OutputStream

object GameStateSerializer : Serializer<SavedGameState> {
    override val defaultValue: SavedGameState = SavedGameState.getDefaultInstance()

    override suspend fun readFrom(input: InputStream): SavedGameState =
        SavedGameState.parseFrom(input)

    override suspend fun writeTo(t: SavedGameState, output: OutputStream) =
        t.writeTo(output)
}
```

`app/src/main/kotlin/com/orangezest/farkle/data/GameStateRepository.kt`:
```kotlin
package com.orangezest.farkle.data

import androidx.datastore.core.DataStore
import com.orangezest.farkle.data.proto.SavedGameState
import com.orangezest.farkle.engine.GameUiState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject

class GameStateRepository @Inject constructor(
    private val dataStore: DataStore<SavedGameState>,
) {
    private val json = Json { ignoreUnknownKeys = true }

    val savedGame: Flow<GameUiState?> = dataStore.data.map { saved ->
        if (saved.jsonState.isEmpty()) null
        else try {
            json.decodeFromString<GameUiState>(saved.jsonState)
        } catch (e: Exception) {
            null
        }
    }

    suspend fun saveGame(state: GameUiState) {
        dataStore.updateData { current ->
            current.toBuilder()
                .setSchemaVersion(1)
                .setJsonState(json.encodeToString(state))
                .build()
        }
    }

    suspend fun clearSavedGame() {
        dataStore.updateData { SavedGameState.getDefaultInstance() }
    }
}
```

- [ ] **Step 5: Run tests to verify they pass**

Run: `./gradlew :app:test --tests "com.orangezest.farkle.data.GameStateRepositoryTest"`
Expected: All 3 tests PASS.

- [ ] **Step 6: Commit**

```bash
git add app/src/
git commit -m "feat(app): add GameStateRepository with Proto DataStore for cross-session persistence"
```

---

## Task 16: GameViewModel

**Files:**
- Create: `app/src/main/kotlin/com/orangezest/farkle/viewmodel/GameViewModel.kt`
- Create: `app/src/test/kotlin/com/orangezest/farkle/viewmodel/GameViewModelTest.kt`

- [ ] **Step 1: Write the failing tests**

`app/src/test/kotlin/com/orangezest/farkle/viewmodel/GameViewModelTest.kt`:
```kotlin
package com.orangezest.farkle.viewmodel

import app.cash.turbine.test
import com.orangezest.farkle.engine.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class GameViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private val fakeDice = FakeDiceRoller(listOf(1, 5, 3, 4, 6, 2))

    @BeforeEach
    fun setup() {
        Dispatchers.setMain(testDispatcher)
    }

    @AfterEach
    fun teardown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(
        diceRoller: DiceRoller = fakeDice,
        config: GameConfig = GameConfig.DEFAULT,
    ): GameViewModel {
        val engine = ScoringEngine(config)
        val reducer = GameReducer(engine, diceRoller)
        return GameViewModel(reducer)
    }

    @Test
    fun `initial state is WaitingToRoll after starting game`() = runTest {
        val vm = createViewModel()
        vm.startGame(listOf("Alice", "Bob"))

        vm.uiState.test {
            val state = awaitItem()
            assertIs<TurnPhase.WaitingToRoll>(state.turnPhase)
            assertEquals(2, state.players.size)
            assertEquals("Alice", state.players[0].name)
        }
    }

    @Test
    fun `onEvent RollDice transitions to SelectingDice`() = runTest {
        val vm = createViewModel()
        vm.startGame(listOf("Alice", "Bob"))

        vm.uiState.test {
            awaitItem() // initial
            vm.onEvent(GameEvent.RollDice)
            val state = awaitItem()
            assertIs<TurnPhase.SelectingDice>(state.turnPhase)
        }
    }

    @Test
    fun `full turn sequence - roll select bank`() = runTest {
        val roller = FakeDiceRoller(listOf(1, 1, 1, 5, 5, 2))
        val vm = createViewModel(diceRoller = roller)
        vm.startGame(listOf("Alice", "Bob"))

        vm.uiState.test {
            awaitItem() // initial
            vm.onEvent(GameEvent.RollDice)
            awaitItem() // SelectingDice

            vm.onEvent(GameEvent.ToggleDie(0))
            vm.onEvent(GameEvent.ToggleDie(1))
            vm.onEvent(GameEvent.ToggleDie(2))
            vm.onEvent(GameEvent.ToggleDie(3))
            vm.onEvent(GameEvent.ToggleDie(4))
            // skip intermediate emissions
            cancelAndIgnoreRemainingEvents()
        }

        vm.onEvent(GameEvent.Bank)
        vm.uiState.test {
            val state = awaitItem()
            assertIs<TurnPhase.PassingDevice>(state.turnPhase)
            assertEquals(400, state.players[0].totalScore)
        }
    }

    @Test
    fun `startGame with empty list does not crash`() = runTest {
        val vm = createViewModel()
        vm.startGame(emptyList())
        vm.uiState.test {
            val state = awaitItem()
            assertTrue(state.players.isEmpty())
        }
    }

    @Test
    fun `starting new game resets previous game state`() = runTest {
        val vm = createViewModel()
        vm.startGame(listOf("Alice", "Bob"))
        vm.onEvent(GameEvent.RollDice)

        vm.startGame(listOf("Charlie", "Dave"))
        vm.uiState.test {
            val state = awaitItem()
            assertIs<TurnPhase.WaitingToRoll>(state.turnPhase)
            assertEquals("Charlie", state.players[0].name)
            assertEquals(0, state.players[0].totalScore)
        }
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :app:test --tests "com.orangezest.farkle.viewmodel.GameViewModelTest"`
Expected: Compilation failure.

- [ ] **Step 3: Write minimal implementation**

`app/src/main/kotlin/com/orangezest/farkle/viewmodel/GameViewModel.kt`:
```kotlin
package com.orangezest.farkle.viewmodel

import androidx.lifecycle.ViewModel
import com.orangezest.farkle.engine.GameEvent
import com.orangezest.farkle.engine.GameReducer
import com.orangezest.farkle.engine.GameUiState
import com.orangezest.farkle.engine.Player
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

@HiltViewModel
class GameViewModel @Inject constructor(
    private val reducer: GameReducer,
) : ViewModel() {

    private val _uiState = MutableStateFlow(GameUiState(players = emptyList()))
    val uiState: StateFlow<GameUiState> = _uiState.asStateFlow()

    fun startGame(playerNames: List<String>) {
        _uiState.value = GameUiState(
            players = playerNames.map { Player(name = it) },
        )
    }

    fun onEvent(event: GameEvent) {
        _uiState.value = reducer.reduce(_uiState.value, event)
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew :app:test --tests "com.orangezest.farkle.viewmodel.GameViewModelTest"`
Expected: All 5 tests PASS.

- [ ] **Step 5: Commit**

```bash
git add app/src/
git commit -m "feat(app): add GameViewModel with StateFlow and event dispatch"
```

---

## Task 17: Save/Resume Game

**Files:**
- Modify: `app/src/main/kotlin/com/orangezest/farkle/viewmodel/GameViewModel.kt`
- Create: `app/src/test/kotlin/com/orangezest/farkle/viewmodel/GameViewModelSaveResumeTest.kt`

- [ ] **Step 1: Write failing test**

`app/src/test/kotlin/com/orangezest/farkle/viewmodel/GameViewModelSaveResumeTest.kt`:
```kotlin
package com.orangezest.farkle.viewmodel

import app.cash.turbine.test
import com.orangezest.farkle.data.GameStateRepository
import com.orangezest.farkle.engine.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.*
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue
import kotlin.test.assertFalse

@OptIn(ExperimentalCoroutinesApi::class)
class GameViewModelSaveResumeTest {

    private val testDispatcher = UnconfinedTestDispatcher()

    @BeforeEach
    fun setup() {
        Dispatchers.setMain(testDispatcher)
    }

    @AfterEach
    fun teardown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `hasSavedGame emits true after bank triggers save`() = runTest {
        val repo = GameStateRepository(testDataStore())
        val roller = FakeDiceRoller(listOf(1, 1, 1, 5, 5, 2))
        val engine = ScoringEngine(GameConfig.DEFAULT)
        val reducer = GameReducer(engine, roller)
        val vm = GameViewModel(reducer, repo)

        vm.startGame(listOf("Alice", "Bob"))
        vm.onEvent(GameEvent.RollDice)
        vm.onEvent(GameEvent.ToggleDie(0))
        vm.onEvent(GameEvent.ToggleDie(1))
        vm.onEvent(GameEvent.ToggleDie(2))
        vm.onEvent(GameEvent.ToggleDie(3))
        vm.onEvent(GameEvent.ToggleDie(4))
        vm.onEvent(GameEvent.Bank)

        assertTrue(vm.hasSavedGame.first())
    }

    @Test
    fun `resumeGame loads saved state`() = runTest {
        val repo = GameStateRepository(testDataStore())
        val roller = FakeDiceRoller(listOf(1, 1, 1, 5, 5, 2))
        val engine = ScoringEngine(GameConfig.DEFAULT)
        val reducer = GameReducer(engine, roller)
        val vm = GameViewModel(reducer, repo)

        val savedState = GameUiState(
            players = listOf(Player("Alice", totalScore = 500, isOnBoard = true), Player("Bob")),
            currentPlayerIndex = 1,
        )
        repo.saveGame(savedState)

        vm.resumeGame()
        vm.uiState.test {
            val state = awaitItem()
            assertEquals(500, state.players[0].totalScore)
            assertEquals(1, state.currentPlayerIndex)
        }
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :app:test --tests "com.orangezest.farkle.viewmodel.GameViewModelSaveResumeTest"`
Expected: Compilation failure.

- [ ] **Step 3: Update GameViewModel with save/resume**

Update `app/src/main/kotlin/com/orangezest/farkle/viewmodel/GameViewModel.kt`:
```kotlin
package com.orangezest.farkle.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.orangezest.farkle.data.GameStateRepository
import com.orangezest.farkle.engine.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class GameViewModel @Inject constructor(
    private val reducer: GameReducer,
    private val gameStateRepository: GameStateRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(GameUiState(players = emptyList()))
    val uiState: StateFlow<GameUiState> = _uiState.asStateFlow()

    val hasSavedGame: StateFlow<Boolean> = gameStateRepository.savedGame
        .map { it != null }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    fun startGame(playerNames: List<String>) {
        _uiState.value = GameUiState(
            players = playerNames.map { Player(name = it) },
        )
    }

    fun resumeGame() {
        viewModelScope.launch {
            gameStateRepository.savedGame.first()?.let { saved ->
                _uiState.value = saved
            }
        }
    }

    fun onEvent(event: GameEvent) {
        _uiState.value = reducer.reduce(_uiState.value, event)

        // Save after banking (PassingDevice transition)
        if (_uiState.value.turnPhase == TurnPhase.PassingDevice) {
            viewModelScope.launch {
                gameStateRepository.saveGame(_uiState.value)
            }
        }
    }
}
```

- [ ] **Step 4: Wire hasSavedGame and onResumeGame into nav graph**

Update `FarkleNavGraph.kt` setup route to pass `hasSavedGame` and `onResumeGame` to `SetupScreen`.

- [ ] **Step 5: Run tests to verify they pass**

Run: `./gradlew :app:test --tests "com.orangezest.farkle.viewmodel.GameViewModelSaveResumeTest"`
Expected: All tests PASS.

- [ ] **Step 6: Commit**

```bash
git add app/src/
git commit -m "feat(app): add save/resume game via GameStateRepository in GameViewModel"
```

---

## Task 18: Theme Setup (Material 3)

**Files:**
- Create: `app/src/main/kotlin/com/orangezest/farkle/ui/theme/Color.kt`
- Create: `app/src/main/kotlin/com/orangezest/farkle/ui/theme/Type.kt`
- Create: `app/src/main/kotlin/com/orangezest/farkle/ui/theme/Theme.kt`

- [ ] **Step 1: Create color definitions**

`app/src/main/kotlin/com/orangezest/farkle/ui/theme/Color.kt`:
```kotlin
package com.orangezest.farkle.ui.theme

import androidx.compose.ui.graphics.Color

val OrangeZestPrimary = Color(0xFFFF6D00)
val OrangeZestOnPrimary = Color(0xFFFFFFFF)
val OrangeZestPrimaryContainer = Color(0xFFFFDBCC)
val OrangeZestSecondary = Color(0xFF775A4C)
val OrangeZestBackground = Color(0xFFFFFBFF)
val OrangeZestSurface = Color(0xFFFFFBFF)

val OrangeZestPrimaryDark = Color(0xFFFFB68E)
val OrangeZestOnPrimaryDark = Color(0xFF522300)
val OrangeZestPrimaryContainerDark = Color(0xFF743400)
val OrangeZestSecondaryDark = Color(0xFFE6BEAC)
val OrangeZestBackgroundDark = Color(0xFF201A17)
val OrangeZestSurfaceDark = Color(0xFF201A17)
```

- [ ] **Step 2: Create typography**

`app/src/main/kotlin/com/orangezest/farkle/ui/theme/Type.kt`:
```kotlin
package com.orangezest.farkle.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

val FarkleTypography = Typography(
    headlineLarge = TextStyle(
        fontWeight = FontWeight.Bold,
        fontSize = 32.sp,
        lineHeight = 40.sp,
    ),
    headlineMedium = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 28.sp,
        lineHeight = 36.sp,
    ),
    titleLarge = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 22.sp,
        lineHeight = 28.sp,
    ),
    bodyLarge = TextStyle(
        fontSize = 16.sp,
        lineHeight = 24.sp,
    ),
    labelLarge = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
    ),
)
```

- [ ] **Step 3: Create theme composable**

`app/src/main/kotlin/com/orangezest/farkle/ui/theme/Theme.kt`:
```kotlin
package com.orangezest.farkle.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val LightColorScheme = lightColorScheme(
    primary = OrangeZestPrimary,
    onPrimary = OrangeZestOnPrimary,
    primaryContainer = OrangeZestPrimaryContainer,
    secondary = OrangeZestSecondary,
    background = OrangeZestBackground,
    surface = OrangeZestSurface,
)

private val DarkColorScheme = darkColorScheme(
    primary = OrangeZestPrimaryDark,
    onPrimary = OrangeZestOnPrimaryDark,
    primaryContainer = OrangeZestPrimaryContainerDark,
    secondary = OrangeZestSecondaryDark,
    background = OrangeZestBackgroundDark,
    surface = OrangeZestSurfaceDark,
)

@Composable
fun FarkleTheme(
    themeMode: String = "system",
    content: @Composable () -> Unit,
) {
    val darkTheme = when (themeMode) {
        "dark" -> true
        "light" -> false
        else -> isSystemInDarkTheme()
    }

    val colorScheme = when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context)
            else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = FarkleTypography,
        content = content,
    )
}
```

- [ ] **Step 4: Verify compilation**

Run: `./gradlew :app:compileDebugKotlin`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/kotlin/com/orangezest/farkle/ui/theme/
git commit -m "feat(app): add Material 3 theme with light/dark/system/dynamic color support"
```

---

## Task 19: Navigation and Setup Screen

**Files:**
- Create: `app/src/main/kotlin/com/orangezest/farkle/ui/navigation/FarkleNavGraph.kt`
- Create: `app/src/main/kotlin/com/orangezest/farkle/ui/setup/SetupScreen.kt`
- Modify: `app/src/main/kotlin/com/orangezest/farkle/MainActivity.kt`

- [ ] **Step 1: Create navigation graph**

`app/src/main/kotlin/com/orangezest/farkle/ui/navigation/FarkleNavGraph.kt`:
```kotlin
package com.orangezest.farkle.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.orangezest.farkle.ui.setup.SetupScreen

object Routes {
    const val SETUP = "setup"
    const val GAME = "game"
    const val SETTINGS = "settings"
}

@Composable
fun FarkleNavGraph(
    navController: NavHostController,
    onStartGame: (List<String>) -> Unit,
) {
    NavHost(navController = navController, startDestination = Routes.SETUP) {
        composable(Routes.SETUP) {
            SetupScreen(
                onStartGame = { names ->
                    onStartGame(names)
                    navController.navigate(Routes.GAME) {
                        popUpTo(Routes.SETUP) { inclusive = true }
                    }
                },
                onOpenSettings = {
                    navController.navigate(Routes.SETTINGS)
                },
            )
        }
        composable(Routes.GAME) {
            // Placeholder — built in Task 22
        }
        composable(Routes.SETTINGS) {
            // Placeholder — built in Task 23
        }
    }
}
```

- [ ] **Step 2: Create SetupScreen**

`app/src/main/kotlin/com/orangezest/farkle/ui/setup/SetupScreen.kt`:
```kotlin
package com.orangezest.farkle.ui.setup

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SetupScreen(
    onStartGame: (List<String>) -> Unit,
    onOpenSettings: () -> Unit,
    hasSavedGame: Boolean = false,
    onResumeGame: () -> Unit = {},
) {
    var playerCount by remember { mutableIntStateOf(2) }
    var playerNames by remember { mutableStateOf(List(4) { "" }) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Farkle") },
                actions = {
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text("Players", style = MaterialTheme.typography.titleLarge)

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                (2..4).forEach { count ->
                    FilterChip(
                        selected = playerCount == count,
                        onClick = { playerCount = count },
                        label = { Text("$count") },
                    )
                }
            }

            (0 until playerCount).forEach { index ->
                OutlinedTextField(
                    value = playerNames[index],
                    onValueChange = { name ->
                        playerNames = playerNames.toMutableList().apply { set(index, name) }
                    },
                    label = { Text("Player ${index + 1}") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            if (hasSavedGame) {
                OutlinedButton(
                    onClick = onResumeGame,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Resume Game")
                }
            }

            Button(
                onClick = {
                    val names = (0 until playerCount).map { i ->
                        playerNames[i].ifBlank { "Player ${i + 1}" }
                    }
                    onStartGame(names)
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Start Game")
            }
        }
    }
}
```

- [ ] **Step 3: Wire up MainActivity**

Replace `app/src/main/kotlin/com/orangezest/farkle/MainActivity.kt`:
```kotlin
package com.orangezest.farkle

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.navigation.compose.rememberNavController
import com.orangezest.farkle.ui.navigation.FarkleNavGraph
import com.orangezest.farkle.ui.theme.FarkleTheme
import com.orangezest.farkle.viewmodel.GameViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val gameViewModel: GameViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            FarkleTheme {
                val navController = rememberNavController()

                FarkleNavGraph(
                    navController = navController,
                    onStartGame = { names -> gameViewModel.startGame(names) },
                )
            }
        }
    }
}
```

- [ ] **Step 4: Verify compilation**

Run: `./gradlew :app:compileDebugKotlin`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/kotlin/com/orangezest/farkle/
git commit -m "feat(app): add navigation graph, SetupScreen, and wire up MainActivity"
```

---

## Task 20: Die Composable

**Files:**
- Create: `app/src/main/kotlin/com/orangezest/farkle/ui/components/Die.kt`

- [ ] **Step 1: Create Die composable**

`app/src/main/kotlin/com/orangezest/farkle/ui/components/Die.kt`:
```kotlin
package com.orangezest.farkle.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

enum class DieState {
    DEFAULT,
    SELECTED,
    DISABLED,
}

@Composable
fun Die(
    value: Int,
    state: DieState,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val backgroundColor by animateColorAsState(
        targetValue = when (state) {
            DieState.DEFAULT -> MaterialTheme.colorScheme.surface
            DieState.SELECTED -> MaterialTheme.colorScheme.primaryContainer
            DieState.DISABLED -> MaterialTheme.colorScheme.surfaceVariant
        },
        label = "die_bg",
    )

    val borderColor by animateColorAsState(
        targetValue = when (state) {
            DieState.SELECTED -> MaterialTheme.colorScheme.primary
            else -> MaterialTheme.colorScheme.outline
        },
        label = "die_border",
    )

    val alpha = if (state == DieState.DISABLED) 0.4f else 1f

    Box(
        modifier = modifier
            .size(56.dp)
            .alpha(alpha)
            .clip(RoundedCornerShape(12.dp))
            .background(backgroundColor)
            .border(2.dp, borderColor, RoundedCornerShape(12.dp))
            .then(
                if (state != DieState.DISABLED) Modifier.clickable(onClick = onClick)
                else Modifier
            ),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = value.toString(),
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = when (state) {
                DieState.SELECTED -> MaterialTheme.colorScheme.onPrimaryContainer
                DieState.DISABLED -> MaterialTheme.colorScheme.onSurfaceVariant
                else -> MaterialTheme.colorScheme.onSurface
            },
        )
    }
}
```

- [ ] **Step 2: Verify compilation**

Run: `./gradlew :app:compileDebugKotlin`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: Commit**

```bash
git add app/src/main/kotlin/com/orangezest/farkle/ui/components/
git commit -m "feat(app): add Die composable with selected/disabled/default states"
```

---

## Task 21: Overlays — Farkle, Pass Device, Game Over

**Files:**
- Create: `app/src/main/kotlin/com/orangezest/farkle/ui/game/FarkleOverlay.kt`
- Create: `app/src/main/kotlin/com/orangezest/farkle/ui/game/PassDeviceScreen.kt`
- Create: `app/src/main/kotlin/com/orangezest/farkle/ui/game/GameOverScreen.kt`

- [ ] **Step 1: Create FarkleOverlay**

`app/src/main/kotlin/com/orangezest/farkle/ui/game/FarkleOverlay.kt`:
```kotlin
package com.orangezest.farkle.ui.game

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

@Composable
fun FarkleOverlay(
    lostPoints: Int,
    onDismiss: () -> Unit,
) {
    LaunchedEffect(Unit) {
        delay(2000)
        onDismiss()
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = "FARKLE!",
            fontSize = 48.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.error,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Lost $lostPoints points",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
```

- [ ] **Step 2: Create PassDeviceScreen**

`app/src/main/kotlin/com/orangezest/farkle/ui/game/PassDeviceScreen.kt`:
```kotlin
package com.orangezest.farkle.ui.game

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.orangezest.farkle.engine.TurnPhase

@Composable
fun PassDeviceScreen(
    nextPlayerName: String,
    onReady: () -> Unit,
    offerSteal: TurnPhase.OfferSteal? = null,
    onSteal: () -> Unit = {},
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = "$nextPlayerName's Turn",
            style = MaterialTheme.typography.headlineLarge,
        )

        Spacer(modifier = Modifier.height(32.dp))

        if (offerSteal != null) {
            OutlinedButton(
                onClick = onSteal,
                modifier = Modifier
                    .fillMaxWidth(0.8f)
                    .padding(bottom = 12.dp),
            ) {
                Text("Steal the roll (${offerSteal.previousTotal} pts, ${offerSteal.remainingDice} dice)")
            }

            Button(
                onClick = onReady,
                modifier = Modifier.fillMaxWidth(0.8f),
            ) {
                Text("Start Fresh")
            }
        } else {
            Button(
                onClick = onReady,
                modifier = Modifier.fillMaxWidth(0.8f),
            ) {
                Text("Ready")
            }
        }
    }
}
```

- [ ] **Step 3: Create GameOverScreen**

`app/src/main/kotlin/com/orangezest/farkle/ui/game/GameOverScreen.kt`:
```kotlin
package com.orangezest.farkle.ui.game

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.orangezest.farkle.engine.Player

@Composable
fun GameOverScreen(
    winner: Player,
    players: List<Player>,
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = "${winner.name} Wins!",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text("Final Scores", style = MaterialTheme.typography.titleLarge)
        Spacer(modifier = Modifier.height(8.dp))

        players.sortedByDescending { it.totalScore }.forEach { player ->
            Text(
                text = "${player.name}: ${player.totalScore}",
                style = MaterialTheme.typography.bodyLarge,
            )
        }
    }
}
```

- [ ] **Step 4: Verify compilation**

Run: `./gradlew :app:compileDebugKotlin`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/kotlin/com/orangezest/farkle/ui/game/
git commit -m "feat(app): add FarkleOverlay, PassDeviceScreen, and GameOverScreen"
```

---

## Task 22: Game Screen — Dice Area, Scoreboard, Action Bar

**Files:**
- Create: `app/src/main/kotlin/com/orangezest/farkle/ui/game/GameScreen.kt`
- Create: `app/src/main/kotlin/com/orangezest/farkle/ui/game/Scoreboard.kt`
- Create: `app/src/main/kotlin/com/orangezest/farkle/ui/game/DiceArea.kt`
- Create: `app/src/main/kotlin/com/orangezest/farkle/ui/game/ActionBar.kt`

- [ ] **Step 1: Create Scoreboard**

`app/src/main/kotlin/com/orangezest/farkle/ui/game/Scoreboard.kt`:
```kotlin
package com.orangezest.farkle.ui.game

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.orangezest.farkle.engine.Player

@Composable
fun Scoreboard(
    players: List<Player>,
    currentPlayerIndex: Int,
    minimumToBoard: Int,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
    ) {
        players.forEachIndexed { index, player ->
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(8.dp),
            ) {
                Text(
                    text = player.name,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = if (index == currentPlayerIndex) FontWeight.Bold else FontWeight.Normal,
                    color = if (index == currentPlayerIndex) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
                )
                Text(
                    text = player.totalScore.toString(),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                )
                if (!player.isOnBoard) {
                    Text(
                        text = "Need $minimumToBoard",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}
```

- [ ] **Step 2: Create DiceArea**

`app/src/main/kotlin/com/orangezest/farkle/ui/game/DiceArea.kt`:
```kotlin
package com.orangezest.farkle.ui.game

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.orangezest.farkle.engine.ScoringEngine
import com.orangezest.farkle.engine.TurnPhase
import com.orangezest.farkle.ui.components.Die
import com.orangezest.farkle.ui.components.DieState

@Composable
fun DiceArea(
    phase: TurnPhase.SelectingDice,
    scoringEngine: ScoringEngine,
    onToggleDie: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val scoringOptions = scoringEngine.findScoringOptions(phase.rollResult)
    val scorableFaces = scoringOptions.flatMap { it.diceUsed }.toSet()

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        val rows = phase.rollResult.chunked(3)
        rows.forEachIndexed { rowIndex, row ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                row.forEachIndexed { colIndex, value ->
                    val index = rowIndex * 3 + colIndex
                    val isSelected = index in phase.selectedIndices
                    val canScore = value in scorableFaces

                    val state = when {
                        isSelected -> DieState.SELECTED
                        !canScore -> DieState.DISABLED
                        else -> DieState.DEFAULT
                    }

                    Die(
                        value = value,
                        state = state,
                        onClick = { onToggleDie(index) },
                    )
                }
            }
        }
    }
}
```

- [ ] **Step 3: Create ActionBar**

`app/src/main/kotlin/com/orangezest/farkle/ui/game/ActionBar.kt`:
```kotlin
package com.orangezest.farkle.ui.game

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun ActionBar(
    canRoll: Boolean,
    canBank: Boolean,
    bankAmount: Int,
    onRoll: () -> Unit,
    onBank: () -> Unit,
    onUndo: () -> Unit,
    canUndo: Boolean,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (canUndo) {
            OutlinedButton(onClick = onUndo) {
                Text("Undo")
            }
        }

        Button(
            onClick = onRoll,
            enabled = canRoll,
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFFD32F2F),
            ),
            modifier = Modifier.weight(1f),
        ) {
            Text("Roll", color = Color.White)
        }

        Button(
            onClick = onBank,
            enabled = canBank,
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF388E3C),
            ),
            modifier = Modifier.weight(1f),
        ) {
            Text("Bank (+$bankAmount)", color = Color.White)
        }
    }
}
```

- [ ] **Step 4: Create GameScreen orchestrator**

`app/src/main/kotlin/com/orangezest/farkle/ui/game/GameScreen.kt`:
```kotlin
package com.orangezest.farkle.ui.game

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.unit.dp
import com.orangezest.farkle.engine.*

@Composable
fun GameScreen(
    state: GameUiState,
    scoringEngine: ScoringEngine,
    onEvent: (GameEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val rotationAngle = remember(state.currentPlayerIndex, state.players.size) {
        when (state.players.size) {
            2 -> if (state.currentPlayerIndex == 1) 180f else 0f
            else -> state.currentPlayerIndex * 90f
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
    ) {
        Scoreboard(
            players = state.players,
            currentPlayerIndex = state.currentPlayerIndex,
            minimumToBoard = state.config.minimumToBoard,
        )

        Spacer(modifier = Modifier.height(16.dp))

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .rotate(rotationAngle),
            contentAlignment = Alignment.Center,
        ) {
            when (val phase = state.turnPhase) {
                is TurnPhase.WaitingToRoll -> {
                    Button(onClick = { onEvent(GameEvent.RollDice) }) {
                        Text("Roll Dice")
                    }
                }

                is TurnPhase.SelectingDice -> {
                    val selectedDice = phase.selectedIndices.map { phase.rollResult[it] }
                    val selectionScore = if (selectedDice.isNotEmpty()) {
                        scoringEngine.scoreSelection(phase.rollResult, selectedDice)
                    } else 0
                    val totalIfBanked = state.runningTotal + selectionScore
                    val canBank = phase.selectedIndices.isNotEmpty() &&
                        selectionScore > 0 &&
                        (state.currentPlayer.isOnBoard || totalIfBanked >= state.config.minimumToBoard)

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        if (state.runningTotal > 0) {
                            Text(
                                text = "Running: ${state.runningTotal}",
                                style = MaterialTheme.typography.titleLarge,
                            )
                        }

                        DiceArea(
                            phase = phase,
                            scoringEngine = scoringEngine,
                            onToggleDie = { onEvent(GameEvent.ToggleDie(it)) },
                        )

                        ActionBar(
                            canRoll = phase.selectedIndices.isNotEmpty() && selectionScore > 0,
                            canBank = canBank,
                            bankAmount = totalIfBanked,
                            onRoll = { onEvent(GameEvent.RollDice) },
                            onBank = { onEvent(GameEvent.Bank) },
                            onUndo = { onEvent(GameEvent.Undo) },
                            canUndo = state.stateHistory.isNotEmpty(),
                        )
                    }
                }

                is TurnPhase.Farkled -> {
                    FarkleOverlay(
                        lostPoints = phase.lostPoints,
                        onDismiss = { onEvent(GameEvent.AcknowledgeFarkle) },
                    )
                }

                is TurnPhase.PassingDevice -> {
                    PassDeviceScreen(
                        nextPlayerName = state.players[(state.currentPlayerIndex + 1) % state.players.size].name,
                        onReady = { onEvent(GameEvent.ReadyForTurn) },
                    )
                }

                is TurnPhase.OfferSteal -> {
                    PassDeviceScreen(
                        nextPlayerName = state.currentPlayer.name,
                        offerSteal = phase,
                        onReady = { onEvent(GameEvent.StartFresh) },
                        onSteal = { onEvent(GameEvent.StealRoll(phase.remainingDice, phase.previousTotal)) },
                    )
                }

                is TurnPhase.GameOver -> {
                    GameOverScreen(
                        winner = state.players[phase.winnerIndex],
                        players = state.players,
                    )
                }
            }
        }
    }
}
```

- [ ] **Step 5: Verify compilation**

Run: `./gradlew :app:compileDebugKotlin`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 6: Commit**

```bash
git add app/src/main/kotlin/com/orangezest/farkle/ui/game/
git commit -m "feat(app): add GameScreen with Scoreboard, DiceArea, and ActionBar"
```

---

## Task 23: Settings Screen

**Files:**
- Create: `app/src/main/kotlin/com/orangezest/farkle/ui/settings/SettingsScreen.kt`
- Create: `app/src/main/kotlin/com/orangezest/farkle/viewmodel/SettingsViewModel.kt`
- Create: `app/src/test/kotlin/com/orangezest/farkle/viewmodel/SettingsViewModelTest.kt`

- [ ] **Step 1: Create SettingsViewModel**

`app/src/main/kotlin/com/orangezest/farkle/viewmodel/SettingsViewModel.kt`:
```kotlin
package com.orangezest.farkle.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.orangezest.farkle.data.SettingsRepository
import com.orangezest.farkle.engine.GameConfig
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val repository: SettingsRepository,
) : ViewModel() {

    val gameConfig: StateFlow<GameConfig> = repository.gameConfig
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), GameConfig.DEFAULT)

    val soundEnabled: StateFlow<Boolean> = repository.soundEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val hapticEnabled: StateFlow<Boolean> = repository.hapticEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val themeMode: StateFlow<String> = repository.themeMode
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "system")

    fun updateTargetScore(value: Int) = viewModelScope.launch { repository.updateTargetScore(value) }
    fun updateMinimumToBoard(value: Int) = viewModelScope.launch { repository.updateMinimumToBoard(value) }
    fun updateHotDice(enabled: Boolean) = viewModelScope.launch { repository.updateHotDice(enabled) }
    fun updatePiggybacking(enabled: Boolean) = viewModelScope.launch { repository.updatePiggybacking(enabled) }
    fun updateSoundEnabled(enabled: Boolean) = viewModelScope.launch { repository.updateSoundEnabled(enabled) }
    fun updateHapticEnabled(enabled: Boolean) = viewModelScope.launch { repository.updateHapticEnabled(enabled) }
    fun updateThemeMode(mode: String) = viewModelScope.launch { repository.updateThemeMode(mode) }
}
```

- [ ] **Step 2: Write SettingsViewModel tests**

`app/src/test/kotlin/com/orangezest/farkle/viewmodel/SettingsViewModelTest.kt`:
```kotlin
package com.orangezest.farkle.viewmodel

import com.orangezest.farkle.data.InMemorySettingsStore
import com.orangezest.farkle.data.SettingsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.*
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()

    @BeforeEach
    fun setup() {
        Dispatchers.setMain(testDispatcher)
    }

    @AfterEach
    fun teardown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `updating target score emits new config`() = runTest {
        val store = InMemorySettingsStore()
        val repo = SettingsRepository(store)
        val vm = SettingsViewModel(repo)
        vm.updateTargetScore(5000)
        assertEquals(5000, vm.gameConfig.first().targetScore)
    }

    @Test
    fun `updating piggybacking emits new config`() = runTest {
        val store = InMemorySettingsStore()
        val repo = SettingsRepository(store)
        val vm = SettingsViewModel(repo)
        vm.updatePiggybacking(true)
        assertEquals(true, vm.gameConfig.first().piggybackingEnabled)
    }

    @Test
    fun `updating haptic enabled emits new value`() = runTest {
        val store = InMemorySettingsStore()
        val repo = SettingsRepository(store)
        val vm = SettingsViewModel(repo)
        vm.updateHapticEnabled(false)
        assertEquals(false, vm.hapticEnabled.first())
    }
}
```

- [ ] **Step 3: Create SettingsScreen**

`app/src/main/kotlin/com/orangezest/farkle/ui/settings/SettingsScreen.kt`:
```kotlin
package com.orangezest.farkle.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.orangezest.farkle.engine.GameConfig

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    config: GameConfig,
    soundEnabled: Boolean,
    hapticEnabled: Boolean,
    themeMode: String,
    onUpdateTargetScore: (Int) -> Unit,
    onUpdateMinimumToBoard: (Int) -> Unit,
    onUpdateHotDice: (Boolean) -> Unit,
    onUpdatePiggybacking: (Boolean) -> Unit,
    onUpdateSoundEnabled: (Boolean) -> Unit,
    onUpdateHapticEnabled: (Boolean) -> Unit,
    onUpdateThemeMode: (String) -> Unit,
    onBack: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text("Game Rules", style = MaterialTheme.typography.titleMedium)

            NumberField("Target Score", config.targetScore, onUpdateTargetScore)
            NumberField("Minimum to Get on Board", config.minimumToBoard, onUpdateMinimumToBoard)

            SwitchRow("Hot Dice", config.hotDiceEnabled, onUpdateHotDice)
            SwitchRow("Piggybacking", config.piggybackingEnabled, onUpdatePiggybacking)

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            Text("Feedback", style = MaterialTheme.typography.titleMedium)

            SwitchRow("Haptic Feedback", hapticEnabled, onUpdateHapticEnabled)

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            Text("Appearance", style = MaterialTheme.typography.titleMedium)

            ThemeSelector(themeMode, onUpdateThemeMode)

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun NumberField(label: String, value: Int, onValueChange: (Int) -> Unit) {
    var text by remember(value) { mutableStateOf(value.toString()) }

    OutlinedTextField(
        value = text,
        onValueChange = { newText ->
            text = newText
            newText.toIntOrNull()?.let { onValueChange(it) }
        },
        label = { Text(label) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun SwitchRow(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, style = MaterialTheme.typography.bodyLarge)
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun ThemeSelector(current: String, onSelect: (String) -> Unit) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        listOf("system" to "System", "light" to "Light", "dark" to "Dark").forEach { (value, label) ->
            FilterChip(
                selected = current == value,
                onClick = { onSelect(value) },
                label = { Text(label) },
            )
        }
    }
}
```

- [ ] **Step 4: Wire Settings into nav graph**

Update the settings composable in `FarkleNavGraph.kt`:
```kotlin
        composable(Routes.SETTINGS) {
            val settingsViewModel: SettingsViewModel = hiltViewModel()
            val config by settingsViewModel.gameConfig.collectAsStateWithLifecycle()
            val soundEnabled by settingsViewModel.soundEnabled.collectAsStateWithLifecycle()
            val hapticEnabled by settingsViewModel.hapticEnabled.collectAsStateWithLifecycle()
            val themeMode by settingsViewModel.themeMode.collectAsStateWithLifecycle()

            SettingsScreen(
                config = config,
                soundEnabled = soundEnabled,
                hapticEnabled = hapticEnabled,
                themeMode = themeMode,
                onUpdateTargetScore = settingsViewModel::updateTargetScore,
                onUpdateMinimumToBoard = settingsViewModel::updateMinimumToBoard,
                onUpdateHotDice = settingsViewModel::updateHotDice,
                onUpdatePiggybacking = settingsViewModel::updatePiggybacking,
                onUpdateSoundEnabled = settingsViewModel::updateSoundEnabled,
                onUpdateHapticEnabled = settingsViewModel::updateHapticEnabled,
                onUpdateThemeMode = settingsViewModel::updateThemeMode,
                onBack = { navController.popBackStack() },
            )
        }
```

Add required imports to `FarkleNavGraph.kt`:
```kotlin
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.hilt.navigation.compose.hiltViewModel
import com.orangezest.farkle.ui.settings.SettingsScreen
import com.orangezest.farkle.viewmodel.SettingsViewModel
```

- [ ] **Step 5: Verify compilation and run tests**

Run: `./gradlew :app:compileDebugKotlin`
Run: `./gradlew :app:test --tests "com.orangezest.farkle.viewmodel.SettingsViewModelTest"`
Expected: BUILD SUCCESSFUL, all tests PASS.

- [ ] **Step 6: Commit**

```bash
git add app/src/
git commit -m "feat(app): add SettingsScreen, SettingsViewModel with tests"
```

---

## Task 24: Haptic Feedback

**Files:**
- Create: `app/src/main/kotlin/com/orangezest/farkle/effects/HapticFeedbackPlayer.kt`

> Note: Sound effects deferred to post-v1 — add as separate task once royalty-free assets are sourced.

- [ ] **Step 1: Create HapticFeedbackPlayer**

`app/src/main/kotlin/com/orangezest/farkle/effects/HapticFeedbackPlayer.kt`:
```kotlin
package com.orangezest.farkle.effects

import android.view.HapticFeedbackConstants
import android.view.View
import com.orangezest.farkle.engine.TurnPhase

class HapticFeedbackPlayer(
    private val hapticEnabled: () -> Boolean,
) {

    fun onPhaseChange(previousPhase: TurnPhase?, newPhase: TurnPhase, view: View) {
        when {
            newPhase is TurnPhase.SelectingDice && previousPhase is TurnPhase.WaitingToRoll -> {
                performHaptic(view, HapticFeedbackConstants.KEYBOARD_TAP)
            }
            newPhase is TurnPhase.Farkled -> {
                performHaptic(view, HapticFeedbackConstants.LONG_PRESS)
            }
            newPhase is TurnPhase.PassingDevice && previousPhase is TurnPhase.SelectingDice -> {
                performHaptic(view, HapticFeedbackConstants.CONFIRM)
            }
        }
    }

    fun onDieSelected(view: View) {
        performHaptic(view, HapticFeedbackConstants.KEYBOARD_TAP)
    }

    private fun performHaptic(view: View, feedbackConstant: Int) {
        if (!hapticEnabled()) return
        view.performHapticFeedback(feedbackConstant)
    }
}
```

- [ ] **Step 2: Verify compilation**

Run: `./gradlew :app:compileDebugKotlin`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: Commit**

```bash
git add app/src/main/kotlin/com/orangezest/farkle/effects/
git commit -m "feat(app): add HapticFeedbackPlayer (sound effects deferred to post-v1)"
```

---

## Task 25: Foldable Window Awareness

**Files:**
- Modify: `app/src/main/kotlin/com/orangezest/farkle/ui/game/GameScreen.kt`
- Modify: `app/src/main/kotlin/com/orangezest/farkle/MainActivity.kt`
- Modify: `app/src/main/kotlin/com/orangezest/farkle/ui/navigation/FarkleNavGraph.kt`

- [ ] **Step 1: Add adaptive layout using WindowSizeClass**

Add at the top of the existing `GameScreen.kt` a new wrapper composable:

```kotlin
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass

@OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
@Composable
fun AdaptiveGameScreen(
    state: GameUiState,
    scoringEngine: ScoringEngine,
    onEvent: (GameEvent) -> Unit,
    windowWidthClass: WindowWidthSizeClass,
) {
    if (windowWidthClass == WindowWidthSizeClass.Expanded) {
        Row(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            Scoreboard(
                players = state.players,
                currentPlayerIndex = state.currentPlayerIndex,
                minimumToBoard = state.config.minimumToBoard,
                modifier = Modifier.width(200.dp),
            )
            GameScreen(
                state = state,
                scoringEngine = scoringEngine,
                onEvent = onEvent,
                modifier = Modifier.weight(1f),
            )
        }
    } else {
        GameScreen(
            state = state,
            scoringEngine = scoringEngine,
            onEvent = onEvent,
        )
    }
}
```

- [ ] **Step 2: Wire calculateWindowSizeClass in MainActivity**

Update `MainActivity.kt`:
```kotlin
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass

@OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val gameViewModel: GameViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val windowSizeClass = calculateWindowSizeClass(this)
            FarkleTheme {
                val navController = rememberNavController()

                FarkleNavGraph(
                    navController = navController,
                    onStartGame = { names -> gameViewModel.startGame(names) },
                    gameViewModel = gameViewModel,
                    windowWidthClass = windowSizeClass.widthSizeClass,
                )
            }
        }
    }
}
```

- [ ] **Step 3: Pass windowWidthClass through FarkleNavGraph to game route**

Update `FarkleNavGraph` signature and game route to use `AdaptiveGameScreen`:
```kotlin
@Composable
fun FarkleNavGraph(
    navController: NavHostController,
    onStartGame: (List<String>) -> Unit,
    gameViewModel: GameViewModel,
    windowWidthClass: WindowWidthSizeClass,
) {
    // ...
    composable(Routes.GAME) {
        val state by gameViewModel.uiState.collectAsStateWithLifecycle()
        val config by remember { derivedStateOf { state.config } }
        val scoringEngine = remember(config) { ScoringEngine(config) }

        AdaptiveGameScreen(
            state = state,
            scoringEngine = scoringEngine,
            onEvent = gameViewModel::onEvent,
            windowWidthClass = windowWidthClass,
        )
    }
}
```

- [ ] **Step 4: Verify compilation**

Run: `./gradlew :app:compileDebugKotlin`
Expected: BUILD SUCCESSFUL.

> VERIFY on Samsung foldable: does unfolding in portrait yield Expanded? If not, use `WindowInfoTracker` directly.

- [ ] **Step 5: Commit**

```bash
git add app/src/
git commit -m "feat(app): add adaptive layout for foldable inner/outer screen"
```

---

## Task 26: ProGuard Rules

**Files:**
- Create: `app/proguard-rules.pro`

- [ ] **Step 1: Create ProGuard rules file**

`app/proguard-rules.pro`:
```
# Kotlin Serialization
-keepattributes *Annotation*, InnerClasses
-keepclassmembers class kotlinx.serialization.json.** { *** Companion; }
-keepclasseswithmembers class com.orangezest.farkle.engine.** { kotlinx.serialization.KSerializer serializer(...); }

# Hilt
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }

# DataStore Proto
-keep class com.orangezest.farkle.data.proto.** { *; }
```

- [ ] **Step 2: Verify release build**

Run: `./gradlew :app:assembleRelease`
Expected: BUILD SUCCESSFUL — no missing class errors from ProGuard.

- [ ] **Step 3: Commit**

```bash
git add app/proguard-rules.pro
git commit -m "feat(app): add ProGuard keep rules for Hilt, Kotlin Serialization, and DataStore"
```

---

## Task 27: CI/CD — GitHub Actions Workflows

**Files:**
- Create: `.github/workflows/pr-checks.yml`
- Create: `.github/workflows/release.yml`

- [ ] **Step 1: Create PR checks workflow**

`.github/workflows/pr-checks.yml`:
```yaml
name: PR Checks

on:
  pull_request:
    branches: [main]

jobs:
  build-and-test:
    # Set repository variable ANDROID_RUNNER=arc-android once self-hosted runner is provisioned.
    runs-on: ${{ vars.ANDROID_RUNNER || 'ubuntu-latest' }}
    steps:
      - uses: actions/checkout@v4

      - uses: actions/setup-java@v4
        with:
          distribution: temurin
          java-version: 21

      - name: Setup Gradle
        uses: gradle/actions/setup-gradle@v4

      - name: Install Android SDK (when not on ARC)
        if: ${{ !vars.ANDROID_RUNNER }}
        uses: android-actions/setup-android@v3

      - name: Run game-engine tests
        run: ./gradlew :game-engine:test

      - name: Run app unit tests
        run: ./gradlew :app:testDebugUnitTest

      - name: Lint
        run: ./gradlew :app:lintDebug

      - name: Build debug APK
        run: ./gradlew :app:assembleDebug
```

- [ ] **Step 2: Create release workflow**

`.github/workflows/release.yml`:
```yaml
name: Release Build

on:
  push:
    branches: [main]

jobs:
  build-release:
    runs-on: ${{ vars.ANDROID_RUNNER || 'ubuntu-latest' }}
    steps:
      - uses: actions/checkout@v4

      - uses: actions/setup-java@v4
        with:
          distribution: temurin
          java-version: 21

      - name: Setup Gradle
        uses: gradle/actions/setup-gradle@v4

      - name: Install Android SDK (when not on ARC)
        if: ${{ !vars.ANDROID_RUNNER }}
        uses: android-actions/setup-android@v3

      - name: Run all tests
        run: ./gradlew test

      - name: Build release APK
        run: ./gradlew :app:assembleRelease

      - name: Upload APK
        uses: actions/upload-artifact@v4
        with:
          name: farkle-release
          path: app/build/outputs/apk/release/*.apk
          retention-days: 30
```

- [ ] **Step 3: Commit**

```bash
git add .github/
git commit -m "ci: add PR checks and release build workflows with ubuntu-latest fallback"
```

---

## Task 28: Wire Game Screen into Navigation

**Files:**
- Modify: `app/src/main/kotlin/com/orangezest/farkle/ui/navigation/FarkleNavGraph.kt`

- [ ] **Step 1: Pass gameViewModel from MainActivity (activity-scoped) into FarkleNavGraph**

Update `FarkleNavGraph.kt` to accept `gameViewModel` as a parameter and use it directly in the game route (instead of calling `hiltViewModel()` inside the nav graph, which would create a duplicate instance):

```kotlin
@Composable
fun FarkleNavGraph(
    navController: NavHostController,
    onStartGame: (List<String>) -> Unit,
    gameViewModel: GameViewModel,
    windowWidthClass: WindowWidthSizeClass,
) {
    NavHost(navController = navController, startDestination = Routes.SETUP) {
        composable(Routes.SETUP) {
            // ... SetupScreen
        }
        composable(Routes.GAME) {
            val state by gameViewModel.uiState.collectAsStateWithLifecycle()
            val config by remember { derivedStateOf { state.config } }
            val scoringEngine = remember(config) { ScoringEngine(config) }

            AdaptiveGameScreen(
                state = state,
                scoringEngine = scoringEngine,
                onEvent = gameViewModel::onEvent,
                windowWidthClass = windowWidthClass,
            )
        }
        composable(Routes.SETTINGS) {
            // ... SettingsScreen with hiltViewModel()
        }
    }
}
```

Add required imports:
```kotlin
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.remember
import com.orangezest.farkle.engine.ScoringEngine
import com.orangezest.farkle.ui.game.AdaptiveGameScreen
import com.orangezest.farkle.viewmodel.GameViewModel
```

- [ ] **Step 2: Verify compilation**

Run: `./gradlew :app:compileDebugKotlin`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: Commit**

```bash
git add app/src/main/kotlin/com/orangezest/farkle/ui/navigation/
git commit -m "feat(app): wire GameScreen into navigation with activity-scoped ViewModel"
```

---

## Task 29: End-to-End Smoke Test

**Files:**
- Create: `app/src/test/kotlin/com/orangezest/farkle/SmokeTest.kt`

- [ ] **Step 1: Write a deterministic integration test for full game flow including final round**

`app/src/test/kotlin/com/orangezest/farkle/SmokeTest.kt`:
```kotlin
package com.orangezest.farkle

import com.orangezest.farkle.engine.*
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class SmokeTest {

    @Test
    fun `complete two-player game with final round`() {
        val config = GameConfig.DEFAULT.copy(targetScore = 1000)
        val engine = ScoringEngine(config)

        // Deterministic dice sequence:
        // Each "turn" rolls: 1,1,1,5,5,2 (select indices 0-4 = 400 points)
        // but 400 < 500 minimum, so we need a sequence that gets on board
        // Use: 1,1,1,1,5,2 → select four 1s + one 5 = 1000 + 50 = 1050 (indices 0-4)
        // Actually let's use simpler: three 1s = 300, two 5s = 100, roll remaining → get 1 more
        // Simplest: use rolls that give exactly 500+ per bank

        // Roll 1: [1,1,1,5,5,2] → select 0,1,2,3,4 → score = 300 + 100 = 400
        // That's below minimum. Let's design a better sequence.

        // Use [1,1,1,1,5,6] → select 0,1,2,3,4 → four 1s(1000) + one 5(50) = 1050? No, four 1s = 1000
        // Actually four-of-a-kind for 1s: NOfAKindRule(4) = fourOfAKindPoints = 1000
        // So [1,1,1,1,5,6] select indices 0-3 (four 1s) = 1000 points. That gets on board AND hits target.

        // Better: target=1000, so one good bank wins.
        // Alice rolls [1,1,1,5,5,6], selects 0,1,2,3,4 → three 1s(300) + two 5s(100) = 400. Below 500 minimum.
        // Need hot dice or a bigger roll.

        // Let's use: [1,5,1,5,1,5] → select all 6 → three 1s(300) + three 5s(500) = wait, that's two triplets?
        // No: [1,5,1,5,1,5] sorted = [1,1,1,5,5,5] → TwoTriplets = 2500? Let's check.
        // TwoTripletsRule: size==6, counts.size==2, all counts==3. Yes! = 2500.

        // Simpler approach: use a target of 500 and rolls that always score 500+
        val simpleConfig = GameConfig.DEFAULT.copy(targetScore = 500)
        val simpleEngine = ScoringEngine(simpleConfig)

        // Alice's roll: [1,1,1,5,5,2] → select indices 0,1,2,3,4
        // Score: calculateScore([1,1,1,5,5]) → three 1s = 300, two singles 5 = 100 → total 400
        // Still below 500. Need different dice.

        // Use [1,1,1,5,6,2] select 0,1,2,3 → three 1s(300) + single 5(50) = 350. Nope.
        // Use [1,1,1,1,5,2] select 0,1,2,3 → four 1s = 1000. YES.

        val targetConfig = GameConfig.DEFAULT.copy(targetScore = 1000)
        val targetEngine = ScoringEngine(targetConfig)

        // Alice: rolls [1,1,1,1,5,2], selects indices 0-3 (four 1s = 1000) → banks 1000, hits target
        // Bob: gets final round turn, rolls [5,5,5,5,5,5], selects all (six 5s = 3000) → banks 3000
        // Game ends: Bob has 3000 > Alice has 1000 → Bob wins
        val diceSequence = listOf(
            1, 1, 1, 1, 5, 2, // Alice's roll
            5, 5, 5, 5, 5, 5, // Bob's roll
        )
        val roller = FakeDiceRoller(diceSequence)
        val reducer = GameReducer(targetEngine, roller)

        var state = GameUiState(
            players = listOf(Player("Alice"), Player("Bob")),
            config = targetConfig,
        )

        // Alice rolls
        state = reducer.reduce(state, GameEvent.RollDice)
        assertIs<TurnPhase.SelectingDice>(state.turnPhase)
        val alicePhase = state.turnPhase as TurnPhase.SelectingDice
        assertEquals(listOf(1, 1, 1, 1, 5, 2), alicePhase.rollResult)

        // Alice selects four 1s (indices 0-3)
        state = reducer.reduce(state, GameEvent.ToggleDie(0))
        state = reducer.reduce(state, GameEvent.ToggleDie(1))
        state = reducer.reduce(state, GameEvent.ToggleDie(2))
        state = reducer.reduce(state, GameEvent.ToggleDie(3))

        // Alice banks → 1000 points, triggers final round
        state = reducer.reduce(state, GameEvent.Bank)
        assertIs<TurnPhase.PassingDevice>(state.turnPhase)
        assertEquals(1000, state.players[0].totalScore)
        assertTrue(state.players[0].isOnBoard)
        assertEquals(0, state.finalRoundTriggerIndex) // Alice triggered

        // Pass to Bob
        state = reducer.reduce(state, GameEvent.ReadyForTurn)
        assertEquals(1, state.currentPlayerIndex)
        assertIs<TurnPhase.WaitingToRoll>(state.turnPhase)

        // Bob rolls
        state = reducer.reduce(state, GameEvent.RollDice)
        assertIs<TurnPhase.SelectingDice>(state.turnPhase)
        val bobPhase = state.turnPhase as TurnPhase.SelectingDice
        assertEquals(listOf(5, 5, 5, 5, 5, 5), bobPhase.rollResult)

        // Bob selects all six 5s
        state = reducer.reduce(state, GameEvent.ToggleDie(0))
        state = reducer.reduce(state, GameEvent.ToggleDie(1))
        state = reducer.reduce(state, GameEvent.ToggleDie(2))
        state = reducer.reduce(state, GameEvent.ToggleDie(3))
        state = reducer.reduce(state, GameEvent.ToggleDie(4))
        state = reducer.reduce(state, GameEvent.ToggleDie(5))

        // Bob banks → 3000 points (six of a kind)
        state = reducer.reduce(state, GameEvent.Bank)
        assertIs<TurnPhase.PassingDevice>(state.turnPhase)
        assertEquals(3000, state.players[1].totalScore)

        // Pass device → next would be Alice (trigger player) → game ends
        state = reducer.reduce(state, GameEvent.ReadyForTurn)
        assertIs<TurnPhase.GameOver>(state.turnPhase)

        // Bob wins with higher score (3000 > 1000)
        val gameOver = state.turnPhase as TurnPhase.GameOver
        assertEquals(1, gameOver.winnerIndex) // Bob
    }

    @Test
    fun `game engine scoring is consistent with rules`() {
        val engine = ScoringEngine(GameConfig.DEFAULT)

        assertEquals(true, engine.isFarkle(listOf(2, 3, 4, 6, 6, 4)))
        assertEquals(false, engine.isFarkle(listOf(1, 2, 3, 4, 6, 6)))
        assertEquals(100, engine.scoreSelection(listOf(1, 2, 3, 4, 6, 6), listOf(1)))
        assertEquals(1500, engine.scoreSelection(listOf(1, 2, 3, 4, 5, 6), listOf(1, 2, 3, 4, 5, 6)))
    }
}
```

- [ ] **Step 2: Run all tests**

Run: `./gradlew test`
Expected: All tests across both modules PASS.

- [ ] **Step 3: Commit**

```bash
git add app/src/test/
git commit -m "test: add deterministic end-to-end smoke test with final round verification"
```

---

## Definition of Done (end-to-end verification)

- [ ] Settings changes propagate to scoring engine in next game
- [ ] Save/resume works across app kill
- [ ] Foldable inner screen uses expanded layout
- [ ] Final round triggers correctly; highest score wins
- [ ] ProGuard rules don't break release build

---

## Post-v1

- Sound effects: add once royalty-free assets are sourced (separate task)
- Crash reporting: Timber for dev, consider Firebase Crashlytics for production

---

## Summary

| Task | Module | What it builds |
|------|--------|---------------|
| 1 | Root | Gradle scaffolding, module structure |
| 2 | infra | Gradle validation spike (CI green gate) |
| 3 | game-engine | DiceRoller interface + implementations |
| 4 | game-engine | GameConfig data class |
| 5 | game-engine | ScoringRule interface + singles |
| 6 | game-engine | N-of-a-kind rules (open class) |
| 7 | game-engine | Combination rules (straight, pairs, triplets) |
| 8 | game-engine | ScoringEngine (evaluates rolls, validates selections, priority tests) |
| 9 | game-engine | GameState + GameEvent types (bounds check, @Transient) |
| 10 | game-engine | GameReducer core turn flow |
| 11 | game-engine | Hot dice, final round mechanic, piggybacking |
| 12 | spike | Rotation touch-target spike |
| 13 | app | Hilt DI module (full DataStore + Repository graph) |
| 14 | app | Settings repository |
| 15 | app | Game state persistence (Proto DataStore, full tests) |
| 16 | app | GameViewModel (with edge-case tests) |
| 17 | app | Save/Resume game |
| 18 | app | Material 3 theme |
| 19 | app | Navigation + Setup screen |
| 20 | app | Die composable |
| 21 | app | Overlays (farkle, pass device, game over) |
| 22 | app | Game screen (scoreboard, dice area, action bar) |
| 23 | app | Settings screen + ViewModel (with tests) |
| 24 | app | Haptic feedback |
| 25 | app | Foldable adaptive layout |
| 26 | app | ProGuard rules |
| 27 | infra | GitHub Actions CI/CD (ubuntu-latest fallback) |
| 28 | app | Wire game screen into nav (activity-scoped ViewModel) |
| 29 | app | End-to-end smoke test (deterministic, final round) |

Tasks 1-11 are pure Kotlin with zero Android dependencies — they run instantly on JVM. Tasks 12+ require Android compilation but unit tests still run on JVM. The CI pipeline (Task 27) can be set up at any point after Task 1.
