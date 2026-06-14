# PLANS.md

# Ajisai — Japanese OCR Overlay Sentence Miner

## Project goal

Build an Android overlay app for playing Japanese games and visual novels. The app should let the user OCR on-screen Japanese text, perform dictionary lookups, and mine cards to AnkiDroid with sentence text, dictionary information, screenshot, and sentence audio.

The app must be lightweight enough to run on top of performance-heavy games.

## Current baseline

This repository has been restarted from a clean Android Studio-generated project.

Current baseline assumptions:

- Android Studio-generated Gradle structure should be preserved.
- Gradle uses Kotlin DSL and a version catalog where practical.
- The app package/namespace is `com.jtkehler.ajisai`.
- Minimum SDK is API 29 because app/game audio capture requires `AudioPlaybackCaptureConfiguration`.
- Target/compile SDK should remain on the Android Studio/latest installed baseline unless a stage requires changing it.
- WSL is used for Codex build/test verification.
- Windows Android Studio/device testing is used for manual Android behavior.

Before feature work begins, Stage 0 must restore the app shell, Kotlin setup, project docs, and placeholder architecture boundaries.

## MVP principles

1. Event-driven processing only.

   - Do not run OCR continuously.
   - Do not run VAD continuously.
   - Do not run Whisper continuously.
   - Expensive work should happen only after explicit user actions.

2. Google Lens OCR first.

   - Do not use ML Kit for OCR in MVP.
   - Implement Google Lens OCR behind an `OcrEngine` interface.
   - Use Chimahon, OwOCR, Chromium Lens-style projects, and similar Japanese-learning apps as references.
   - Keep Lens-specific request/response handling isolated.

3. Static OCR box first.

   - Do not require cropping on every OCR trigger.
   - MVP uses one persistent OCR box.
   - OCR box is configured from the overlay.
   - Store coordinates as normalized screen coordinates.
   - Data model should support multiple OCR boxes later.

4. hoshidicts for dictionary lookup.

   - Keep `hoshidicts` and `hoshidicts-kotlin-bridge` as Git submodules.
   - Do not vendor/copy their source into app packages.
   - Settings must include dictionary import.
   - Import Yomitan-format zip dictionaries.
   - Support term, frequency, and pitch dictionaries where possible.

5. Direct AnkiDroid export for MVP.

   - MVP uses direct AnkiDroid API only.
   - Do not implement AnkiconnectAndroid in MVP.
   - Do not implement AnkiConnect HTTP in MVP.
   - Do not implement TSV/offline fallback in MVP.
   - Keep exporter abstraction so AnkiconnectAndroid/AnkiConnect can be added later.

6. Lapis note fields by default.

   - Default model/profile: `Lapis`.
   - Default card type: ClickCard.
   - Set `IsClickCard = "x"`.
   - Leave `IsWordAndSentenceCard`, `IsSentenceCard`, and `IsAudioCard` empty.
   - Allow field mapping configuration in settings.

7. Audio ring buffer with VAD on Mine.

   - Maintain a continuous timestamped audio ring buffer.
   - Each OCR session stores the screenshot/audio timestamp.
   - On Mine, extract candidate audio using the OCR timestamp.
   - Run VAD only after Mine, not on every hotkey/OCR trigger.
   - Future setting may preserve or pre-trim candidate audio on OCR trigger.

8. Test features as they are added.

   - Prefer Espresso for in-app UI and E2E flows.
   - Use UI Automator only when needed for Android system UI or cross-app flows.
   - Let implementation choose the test structure.
   - Do not require live Google Lens, real MediaProjection, or real AnkiDroid in normal CI tests unless explicitly marked as manual/device tests.

9. Dark Material Design UI.

   - Use Material Design components and interaction patterns for app and overlay UI.
   - Use a dark theme by default across the main app, settings, and overlay surfaces.
   - Use purple and blue accent colors inspired by the colors of `紫陽花` (hydrangea).
   - Maintain accessible contrast, readable Japanese text, and clear touch targets.
   - Keep overlay surfaces compact and visually unobtrusive over games.
   - Minimalist, black and white overlay.

## Non-goals for MVP

- ML Kit OCR.
- Continuous OCR.
- Continuous speech recognition.
- Continuous Silero VAD.
- Whisper alignment.
- Multi-OCR-box auto-selection.
- Gamepad support.
- AnkiconnectAndroid support.
- Desktop/mobile AnkiConnect HTTP support.
- TSV/offline export fallback.
- Full video recording.

## External references

Use these projects as implementation references:

- Chimahon:
  - Android Japanese immersion reader/mining workflow.
  - OCR + dictionary + Anki mining reference.
  - Direct Android-first UX reference.

- Hoshi Reader Android:
  - hoshidicts integration reference.
  - Lapis-compatible Anki mining reference.
  - AnkiDroid/AnkiConnect architecture reference.

- hoshidicts:
  - Dictionary backend.
  - Yomitan dictionary import.
  - Term, frequency, and pitch dictionary support.

- hoshidicts-kotlin-bridge:
  - Kotlin/Android bridge for hoshidicts.

- OwOCR / Chromium Lens-style OCR projects:
  - Google Lens OCR behavior reference.

## Build and environment expectations

- Preserve Android Studio-generated Gradle structure.
- Prefer adding dependencies to `gradle/libs.versions.toml`.
- Do not rewrite Gradle files broadly.
- Make the smallest Gradle changes needed for the current stage.
- Use WSL for:
  - `./gradlew clean`
  - `./gradlew test`
  - `./gradlew assembleDebug`
- Use Windows Android Studio/device for:
  - manual app launch
  - MediaProjection permission testing
  - overlay permission testing
  - real Google Lens OCR testing
  - real AnkiDroid export testing
  - game performance checks

Recommended verification after each stage:

```bash
./gradlew clean test assembleDebug
```

Run connected/device tests only when a device/emulator is available and the stage needs them:

```bash
./gradlew connectedAndroidTest
```

## Repository layout

Suggested package/module layout:

```text
app/
  src/main/...

external/
  hoshidicts/
  hoshidicts-kotlin-bridge/

capture/
  MediaProjectionController
  ScreenFrameSource
  AppAudioRecorder
  AudioRingBuffer
  AudioActivityIndex

overlay/
  OverlayService
  BubbleController
  OcrBoxEditor
  OcrResultPanel
  DictionaryPopup
  MineCardPanel

input/
  OverlayTriggerSource
  OverlayTriggerRouter
  FloatingBubbleTriggerSource
  AccessibilityVolumeTriggerSource
  GamepadTriggerSource placeholder/future

ocr/
  OcrEngine
  GoogleLensOcrEngine
  LensTransport
  LensRequestBuilder
  LensResponseParser
  OcrImagePreprocessor
  OcrTextPostProcessor
  OcrDebugLogger

dictionary/
  HoshiDictionaryRepository
  HoshiDictionaryImporter
  LookupService
  ImportedDictionaryStore
  DictionaryPriorityManager

anki/
  AnkiExporter
  AnkiDroidExporter
  AnkiDroidApiClient
  AnkiDroidSetupValidator
  AnkiDroidMediaWriter
  AnkiDroidDuplicateChecker
  LapisFieldMapper

audio/
  VadSegmenter
  EnergyVadSegmenter optional
  SileroVadSegmenter
  AudioClipExporter

mining/
  MiningCandidate
  OcrSession
  MiningRepository
  MiningCoordinator

settings/
  SettingsScreen
  DictionarySettingsScreen
  OcrSettingsScreen
  AnkiSettingsScreen
  AudioSettingsScreen
```

The exact physical Gradle module layout may evolve. Keep the architecture boundaries even if implemented as packages inside one app module at first.

## Submodules

Add hoshidicts dependencies as submodules:

```bash
git submodule add https://github.com/Manhhao/hoshidicts.git external/hoshidicts
git submodule add https://github.com/Manhhao/hoshidicts-kotlin-bridge.git external/hoshidicts-kotlin-bridge
git submodule update --init --recursive
```

Developer update commands:

```bash
git submodule update --remote --merge external/hoshidicts
git submodule update --remote --merge external/hoshidicts-kotlin-bridge
git submodule update --remote --merge
```

Do not directly edit submodule source unless the task explicitly asks for it.

## Settings

Settings must include:

### Dictionaries

- Import Yomitan dictionary zip.
- List imported dictionaries.
- Show dictionary title, type, enabled state, and priority.
- Enable/disable dictionaries.
- Reorder priority.
- Delete dictionaries.
- Show import progress.
- Show import errors.
- Support term, frequency, and pitch dictionaries as first-class types when hoshidicts supports them.

### OCR

- Configure static OCR box.
- Select active OCR box.
- Future: multiple OCR boxes.
- Future: auto-select box with most useful text.
- Debug: save crop sent to OCR.
- Debug: save raw OCR response.

### Anki

- Direct AnkiDroid setup.
- Select deck.
- Select model, default `Lapis`.
- Validate fields.
- Configure field mapping.
- Configure duplicate mode.
- Default card type: ClickCard.

### Audio

- Ring buffer duration.
- Audio capture enabled/disabled.
- Future: preservation mode.
- Future: pre-trim on OCR trigger.
- VAD settings after VAD is implemented.

### Appearance / UI

- Dark theme default.
- Hydrangea-inspired purple/blue accents.
- Accessible contrast settings if custom themes are added later.
- Overlay compactness/readability options if needed later.

## UI direction

Use Material Design as the main interaction language.

Main app/settings:

- Dark theme by default.
- Hydrangea-inspired purple and blue accent colors.
- Clear navigation hierarchy.
- Large enough touch targets.
- Good Japanese text rendering.
- Avoid overly decorative UI.

Overlay:

- Minimalist black and white by default.
- Compact and unobtrusive over games.
- Use accent colors sparingly for selected state, OCR box edges, and primary actions.
- Avoid large opaque surfaces over game content.
- Keep lookup/mining panels readable but collapsible.
- Prioritize fast interaction over visual complexity.

## OCR box model

MVP exposes one OCR box, but the data model should support a list.

```kotlin
data class OcrBoxProfile(
    val id: String,
    val name: String,
    val normalizedRect: NormalizedRect,
    val enabled: Boolean = true,
    val priority: Int = 0,
    val lastUsedAtMs: Long? = null
)

data class NormalizedRect(
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float
)
```

On OCR trigger:

```text
capture screenshot
→ load active OCR box
→ convert normalized rect to bitmap crop rect
→ crop
→ send crop to Google Lens OCR
→ show editable OCR result
```

Future multi-box behavior:

```text
capture one screenshot
→ crop each enabled OCR box
→ OCR each crop
→ score results
→ select box with most useful Japanese text
```

Future score may consider:

- Japanese character count.
- Line count.
- Kana/kanji ratio.
- Repeated text penalty.
- Punctuation/UI junk penalty.
- Manual priority.
- Most recently successful box.

## OCR provider

Use this interface shape or equivalent:

```kotlin
interface OcrEngine {
    suspend fun recognize(
        image: Bitmap,
        region: Rect? = null,
        options: OcrOptions = OcrOptions()
    ): OcrResult
}
```

Separate responsibilities:

- `GoogleLensOcrEngine`: high-level provider.
- `LensTransport`: network/request transport.
- `LensRequestBuilder`: request encoding.
- `LensResponseParser`: raw response parsing.
- `OcrImagePreprocessor`: crop/upscale/compress.
- `OcrTextPostProcessor`: Japanese cleanup.
- `OcrDebugLogger`: optional debug crop/payload saving.

Do not allow Lens internals to leak into overlay, mining, or lookup code.

## Dictionary import and lookup

Dictionary import flow:

```text
Settings → Dictionaries → Import
→ Android document picker selects zip
→ copy zip to app cache/imports
→ background import through hoshidicts
→ save metadata
→ enable dictionary by default
→ refresh dictionary list
```

Lookup behavior:

```text
user taps OCR sentence at character offset
→ lookup substrings from that offset
→ hoshidicts handles deinflection/frequency/pitch where available
→ display candidates
```

Only enabled dictionaries should be used, in priority order.

## AnkiDroid MVP export

MVP uses direct AnkiDroid API only.

Do not implement:

- AnkiconnectAndroid.
- Desktop/mobile AnkiConnect HTTP.
- TSV fallback.
- Offline queue.

Add future abstraction only:

```kotlin
interface AnkiExporter {
    suspend fun validateSetup(): AnkiSetupResult
    suspend fun getDecks(): List<AnkiDeck>
    suspend fun getModels(): List<AnkiModel>
    suspend fun getFields(modelId: Long): List<String>
    suspend fun canAdd(candidate: MiningCandidate): CanAddResult
    suspend fun add(candidate: MiningCandidate): AddNoteResult
}
```

MVP binding:

```text
AnkiExporter = AnkiDroidExporter
```

Setup validation:

1. Check AnkiDroid installed.
2. Check/request AnkiDroid API permission.
3. Verify AnkiDroid API enabled.
4. Fetch deck list.
5. Fetch model list.
6. Default to model named `Lapis`.
7. Fetch field list.
8. Validate required fields or configured mappings.
9. Enable mining only after setup succeeds.

Card creation:

```text
Build MiningCandidate
→ export screenshot/audio to cache
→ share media through FileProvider
→ add media through AnkiDroid API
→ use returned media field values
→ map candidate to Lapis fields
→ duplicate check
→ add note
→ show result
```

## Lapis default fields

Default model/profile: `Lapis`.

Default card type: ClickCard.

Default card selector fields:

```text
IsWordAndSentenceCard = ""
IsClickCard = "x"
IsSentenceCard = ""
IsAudioCard = ""
```

Default fields to support:

```text
Expression
ExpressionFurigana
ExpressionReading
ExpressionAudio
SelectionText
MainDefinition
DefinitionPicture
Sentence
SentenceFurigana
SentenceAudio
Picture
Glossary
Hint
IsWordAndSentenceCard
IsClickCard
IsSentenceCard
IsAudioCard
PitchPosition
PitchCategories
Frequency
FreqSort
MiscInfo
```

Recommended defaults:

```text
Expression              ← selected target expression
ExpressionFurigana      ← empty or generated later
ExpressionReading       ← dictionary reading
ExpressionAudio         ← optional, empty in MVP unless available
SelectionText           ← selected definition text
MainDefinition          ← primary definition HTML
DefinitionPicture       ← empty
Sentence                ← OCR sentence, target bolded where possible
SentenceFurigana        ← empty by default
SentenceAudio           ← AnkiDroid media field value
Picture                 ← AnkiDroid media field value
Glossary                ← full glossary HTML
Hint                    ← empty/user configurable
IsWordAndSentenceCard   ← empty
IsClickCard             ← x
IsSentenceCard          ← empty
IsAudioCard             ← empty
PitchPosition           ← pitch position if available
PitchCategories         ← pitch category labels if available
Frequency               ← frequency HTML if available
FreqSort                ← sortable frequency value if available
MiscInfo                ← source app/timestamp/OCR debug info
```

## Audio plan

Use:

- `MediaProjection` + `AudioPlaybackCaptureConfiguration` + `AudioRecord`.
- Fixed-size timestamped PCM ring buffer.
- Default ring buffer duration: 60 seconds, configurable later.
- Store OCR timestamp on each `OcrSession`.

MVP/default behavior:

```text
OCR trigger:
  capture screenshot
  store OCR timestamp
  run OCR
  do not run VAD

Mine:
  use OCR timestamp
  read candidate audio window from ring buffer
  run VAD on that short window
  export trimmed audio
  add audio to AnkiDroid
```

Candidate window default:

```text
start = OCR timestamp - 12 seconds
end   = OCR timestamp + 2 seconds
```

VAD behavior:

- VAD runs only after Mine.
- VAD does not run on every hotkey/OCR trigger.
- Fallback to raw candidate window if VAD fails.
- Add small padding before and after detected speech.

Future audio options:

```text
Audio preservation:
- Ring buffer only
- Save candidate audio on OCR trigger
- Pre-trim candidate audio on OCR trigger
```

## Input triggers

Use a trigger abstraction. Do not hardcode overlay behavior directly to volume keys.

MVP:

- Floating overlay bubble.
- Optional accessibility volume-button hotkey if implemented early.

Future:

- Quick Settings tile.
- Gamepad support.

Future gamepad actions:

- Toggle overlay.
- Run OCR.
- Previous OCR box.
- Next OCR box.
- Mine current sentence.
- Close overlay.

## Testing requirements

Implement automated tests as features are added.

Guidelines:

- Prefer Espresso for in-app UI and E2E flows.
- Use UI Automator only when needed for Android system UI or cross-app flows.
- Implementation may choose the testing structure, helpers, fakes, fixtures, and architecture.
- Each feature milestone should include meaningful tests for happy path and key error states.
- Do not require live Google Lens, real MediaProjection, or real AnkiDroid in normal CI tests unless explicitly marked as manual/device tests.
- Add manual test notes for flows that are hard to automate reliably:
  - Overlay permission.
  - MediaProjection permission.
  - Real Google Lens OCR.
  - Real AnkiDroid export.
  - Performance over real games.

Important tests to include over time:

- App launches/settings opens.
- Dictionary import screen works with fake importer.
- Static OCR box persists normalized coordinates.
- OCR trigger uses saved OCR box.
- Fake OCR result can be edited.
- Tapping OCR text performs dictionary lookup.
- Default Lapis mapping uses `IsClickCard = "x"`.
- Mining blocked until AnkiDroid setup succeeds.
- VAD does not run on hotkey/OCR trigger.
- VAD runs only after Mine using the OCR session timestamp.
- Dark theme and overlay contrast/readability do not regress.

## Performance constraints

Always-on work should be minimal.

Allowed always-on:

- AudioRecord writing to ring buffer.
- Lightweight audio activity metadata if needed.
- Minimal overlay bubble.
- Capture service state.

Avoid:

- Continuous OCR.
- Continuous VAD.
- Continuous Whisper.
- Continuous full-frame processing.
- Continuous screenshot saving.
- Repeated dictionary DB initialization.
- Heavy overlay recomposition while game is active.

Run expensive tasks only after explicit actions:

```text
Hotkey/OCR:
  screenshot crop
  Google Lens OCR

Mine:
  audio extraction
  VAD
  media encoding
  AnkiDroid export
```

## Implementation stages

### Stage 0A — Restore project docs

- Add `PLANS.md`.
- Add `AGENTS.md`.
- Ensure both describe the Android Studio-generated baseline.
- No app feature implementation yet.

### Stage 0B — Kotlin app shell (complete)

- [x] Add Kotlin Android plugin through version catalog.
- [x] Apply Kotlin Android plugin to app module.
- [x] Set Java/Kotlin compile target to 17.
- [x] Create Kotlin `MainActivity`.
- [x] Declare launcher activity in manifest.
- [x] Create a simple main screen with app title and Settings button.
- [x] Create settings screen/activity.
- [x] Add smoke tests for app launch/settings entry.
- [x] Run `./gradlew clean test assembleDebug`.

### Stage 0C — Placeholder architecture boundaries (complete)

- [x] Add minimal packages/interfaces for:
  - capture
  - overlay
  - input
  - ocr
  - dictionary
  - anki
  - audio
  - mining
  - settings
- [x] Keep implementations empty/minimal.
- [x] Do not implement feature logic.
- [x] Run `./gradlew clean test assembleDebug`.

### Stage 1 — Submodules and dictionary settings shell (complete)

- [x] Add hoshidicts submodules.
- [x] Settings → Dictionaries shell.
- [x] Import button placeholder.
- [x] Imported dictionary models.
- [x] Repository interfaces and in-memory fakes.
- [x] Tests.
- [x] Run `./gradlew clean test assembleDebug`.

### Stage 2 — Dictionary import (complete)

- [x] Yomitan zip import through hoshidicts.
- [x] WorkManager import with visible progress states.
- [x] Persisted imported dictionary metadata and list.
- [x] Enable/disable.
- [x] Priority and reorder controls.
- [x] Delete.
- [x] Tests with fakes/fixtures.
- [x] Physical-device E2E scripts for normal connected tests and opt-in real JMdict import/restart regression.
- [x] Verify imported dictionary settings render after force-stop and cold reopen.
- [x] Clear completed import work observation IDs so Dictionaries does not re-observe stale work.
- [x] Run `./gradlew clean test assembleDebug`.

### Stage 3 — MediaProjection screenshot capture

- [x] Screen-capture permission flow.
- [x] Foreground capture service.
- [x] On-demand ImageReader screenshot capture.
- [x] Debug screenshot UI and preview.
- [x] Error states.
- [x] Tests with fakes.
- [x] Physical-device verification of the real MediaProjection prompt, notification, screenshot preview, and stop flow.

### Stage 4 — Overlay bubble and trigger abstraction

- [x] OverlayTriggerSource and action router boundaries.
- [x] Lightweight movable floating bubble with persisted position.
- [x] Overlay permission handling and visible app state.
- [x] Compact overlay panel with placeholder OCR, configure, and close actions.
- [x] Overlay open/close service flow kept separate from screen capture.
- [x] Future trigger source TODO placeholders without implementations.
- [x] Unit and instrumentation tests using fakes.
- [x] Physical-device verification of permission grant/denial, drag/tap, panel actions, and close cleanup.

### Stage 5 — Static OCR box

- [x] One persistent OCR box.
- [x] Persist normalized coordinates across restarts and resolution changes.
- [x] Overlay box editor with move, edge/corner resize, Save, and Cancel.
- [x] Crop captured screenshots via the saved box.
- [x] Multi-box-ready profile and repository data model.
- [x] Unit and device tests for geometry, persistence, routing, and bitmap crop content.
- [x] Stage 5 manual test notes.

### Stage 6 — Google Lens OCR

- [x] Extend the provider-neutral `OcrEngine` boundary and typed OCR errors/results.
- [x] Add `GoogleLensOcrEngine` behind isolated Lens config, request, transport, parser, image preprocessing, text postprocessing, and debug logging components.
- [x] Coordinate one fresh on-demand screenshot, saved OCR-box crop, timeout, OCR execution, and captured-frame timestamp without using stale `latestFrame`.
- [x] Replace the Stage 5 debug crop action with Capturing/Recognizing/success/error overlay states.
- [x] Show OCR progress, result, and error states in the overlay (the initial editable/retry panel was replaced by the Stage 6.5 read-only HUD).
- [x] Save the OCR crop and raw Lens response only when debug artifact saving is enabled.
- [x] Add fake OCR/Lens transport tests, protocol/preprocessing/postprocessing tests, runner tests, overlay routing/UI tests, and Stage 6 manual checks.
- [x] Run `./gradlew clean test assembleDebug`, `./scripts/e2e.sh`, and `./scripts/e2e-real-jmdict.sh`.

### Stage 6.5 — Overlay HUD toggle and OCR cleanup

- [x] Make the persistent floating bubble toggle a collapsed/shown game HUD instead of opening the menu-first OCR panel.
- [x] Show compact top-corner OCR-box/settings/hide controls and a read-only bottom OCR text panel only while the HUD is shown.
- [x] Start exactly one fresh OCR attempt when the HUD is toggled on; cancel and clear transient OCR state when it is toggled off.
- [x] Keep overlay windows non-focusable/non-touch-modal where practical so no result keyboard appears and game touches outside Ajisai surfaces pass through.
- [x] Clear stale `ACTIVE` capture errors synchronously before sending a new one-shot capture request.
- [x] Preserve provider-neutral OCR line geometry and conservatively remove small aligned horizontal furigana lines during Japanese OCR postprocessing.
- [x] Add HUD toggle/cancellation, read-only result UI, stale capture error, Lens geometry, and furigana filtering tests.
- [x] Update Stage 6.5 manual checks and preserve Stage 3-6 behavior without adding lookup, mining, audio, VAD, or continuous OCR.
- [x] Run `./gradlew clean test assembleDebug`, `./scripts/e2e.sh`, and `./scripts/e2e-real-jmdict.sh`.

### Stage 7 — hoshidicts lookup

- Tap-to-lookup from OCR sentence.
- Enabled dictionaries in priority order.
- Definitions, reading, frequency, pitch.
- Tests.

### Stage 8 — Mining candidate and Lapis mapper

- MiningCandidate.
- LapisFieldMapper.
- Default ClickCard mapping.
- Configurable field mapping.
- Tests.

### Stage 9 — Direct AnkiDroid setup

- AnkiDroid API integration.
- Permission.
- Deck/model/field discovery.
- Lapis validation.
- Mining disabled until ready.
- Tests with fake API.

### Stage 10 — Screenshot-only AnkiDroid export

- Screenshot media export.
- FileProvider.
- Add media to AnkiDroid.
- Duplicate check.
- Add note.
- Success/error/duplicate states.
- Tests.

### Stage 11 — Audio ring buffer

- AudioPlaybackCapture.
- AudioRecord.
- Timestamped PCM ring buffer.
- OCR session timestamp.
- Tests.

### Stage 12 — Sentence audio export

- Extract audio window on Mine.
- Use OCR timestamp.
- Add sentence audio to AnkiDroid.
- Tests verifying no VAD on OCR trigger.

### Stage 13 — VAD on Mine

- VadSegmenter interface.
- Energy/Silero implementation.
- Run only on Mine.
- Fallback behavior.
- Tests.

### Stage 14 — Polish and future hooks

- Future AnkiConnect/AnkiconnectAndroid abstraction placeholder.
- Future gamepad trigger placeholder.
- Future multi-OCR-box scoring placeholder.
- Manual test notes.
- Documentation update.
- Keep tests passing.

## Future features

- Multi-OCR-box support.
- Auto-select OCR box with most useful text.
- Gamepad overlay toggle and controller bindings.
- Quick Settings tile.
- AnkiconnectAndroid exporter.
- AnkiConnect-compatible HTTP exporter.
- Whisper alignment.
- Candidate audio preservation on OCR trigger.
- Pre-trim audio on OCR trigger.
- More advanced sentence/word alignment.
- Pitch/frequency display polish.
- Word audio support.
