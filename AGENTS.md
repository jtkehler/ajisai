# AGENTS.md

# Agent Instructions

## Project summary

This project is an Android Japanese OCR overlay and sentence-mining app for games and visual novels.

Core workflow:

```text
overlay trigger
→ capture screenshot
→ crop static OCR box
→ Google Lens OCR
→ show editable Japanese sentence
→ dictionary lookup with hoshidicts
→ mine to AnkiDroid using Lapis fields
→ include screenshot and sentence audio
```

## Read first

Before making changes, read:

1. `PLANS.md`
2. This `AGENTS.md`
3. Relevant existing code/tests

Follow the current plan unless the user explicitly changes it.

## Current repository baseline

This repository was restarted from a clean Android Studio-generated project.

Important baseline rules:

- Preserve Android Studio-generated Gradle structure.
- Prefer adding dependencies through `gradle/libs.versions.toml`.
- Do not rewrite Gradle files broadly.
- Make the smallest Gradle changes needed for the current stage.
- WSL is used for Gradle build/test verification.
- Windows Android Studio/device is used for manual Android testing.
- Do not assume an emulator or physical device is available from WSL.

After Gradle changes, run:

```bash
./gradlew clean test assembleDebug
```

## Hard MVP decisions

Do not change these without explicit user instruction:

- Use Google Lens OCR first.
- Do not use ML Kit OCR.
- Use direct AnkiDroid API for MVP.
- Do not implement AnkiconnectAndroid in MVP.
- Do not implement AnkiConnect HTTP in MVP.
- Do not implement TSV/offline fallback in MVP.
- Use hoshidicts and hoshidicts-kotlin-bridge as Git submodules.
- Do not vendor/copy hoshidicts source into app packages.
- Use Lapis fields by default.
- Default card type is ClickCard.
- Set `IsClickCard = "x"` by default.
- Leave other Lapis card selector fields empty by default.
- Use one static OCR box for MVP.
- Store OCR box coordinates as normalized coordinates.
- VAD runs only after Mine, not on every OCR trigger.
- Mining audio extraction uses the OCR session timestamp, not current time.
- Use Material Design components/patterns where practical.
- Use a dark theme by default.
- Use purple/blue accents inspired by `紫陽花`.
- Keep overlay UI compact, minimalist, black/white, and unobtrusive over games.

## Architecture expectations

Keep implementation boundaries clean.

Use interfaces around unstable or replaceable systems:

- OCR provider.
- Lens transport/parser.
- Dictionary repository.
- Anki exporter.
- Audio recorder.
- VAD segmenter.
- Screen frame source.
- Overlay trigger source.

Do not let implementation details leak across boundaries. For example:

- Overlay code should not depend on Lens request formats.
- Mining code should not depend directly on hoshidicts internals.
- Lookup code should not know about AnkiDroid APIs.
- Anki export should receive a `MiningCandidate` and mapped fields, not raw overlay state.

## Performance rules

This app runs over games. Keep always-on work minimal.

Allowed always-on:

- Minimal overlay bubble.
- Capture service state.
- Audio ring buffer when audio capture is enabled.

Avoid always-on:

- OCR.
- VAD.
- Whisper.
- Full-frame processing.
- Repeated dictionary initialization.
- Heavy UI recomposition.
- Repeated allocation in capture/audio loops.

Expensive work should be user-triggered:

- OCR runs only after OCR trigger.
- VAD runs only after Mine.
- Media encoding runs only during mining.
- Anki export runs only during mining.

## UI rules

Use a dark Material Design UI by default.

Main app/settings:

- Use Material components and interaction patterns where practical.
- Dark theme by default.
- Purple and blue accents inspired by hydrangea colors.
- Maintain accessible contrast.
- Keep Japanese text readable.
- Use clear touch targets.
- Prefer simple layouts over decorative UI.

Overlay:

- Minimalist black and white by default.
- Compact and visually unobtrusive over games.
- Avoid large opaque panels unless explicitly needed.
- Use accent colors sparingly for selected state, OCR box outlines, and primary actions.
- Prioritize speed, readability, and low visual noise.

## OCR rules

- Do not add ML Kit unless explicitly requested.
- Google Lens OCR is the primary OCR provider.
- Keep Lens integration behind `OcrEngine`.
- Add debug hooks for saving OCR crop and raw response.
- Normal tests should use fake OCR/Lens transport.
- Do not require live Google Lens for CI.

## Dictionary rules

- hoshidicts and hoshidicts-kotlin-bridge must remain submodules under `external/`.
- Do not edit submodule source unless explicitly requested.
- Dictionary import belongs in Settings.
- Import Yomitan zip dictionaries.
- Support enabled/disabled dictionaries and priority order.
- Lookup should use enabled dictionaries in priority order.

## Anki rules

MVP uses direct AnkiDroid only.

Do not implement during MVP:

- AnkiconnectAndroid exporter.
- AnkiConnect HTTP exporter.
- TSV exporter.
- Offline note queue.

Keep `AnkiExporter` abstract so alternate backends can be added later.

Default Lapis card selector fields:

```text
IsWordAndSentenceCard = ""
IsClickCard = "x"
IsSentenceCard = ""
IsAudioCard = ""
```

`SentenceFurigana` should be empty by default.

## Audio rules

- Use timestamped audio ring buffer.
- Each `OcrSession` must store the screenshot/audio timestamp.
- On Mine, use the OCR timestamp to extract candidate audio.
- Do not use current time for sentence audio extraction unless explicitly intended.
- Do not run VAD on OCR trigger in MVP.
- VAD runs only after Mine.
- Fallback gracefully if audio capture is unavailable or VAD fails.

## Testing expectations

Implement tests as features are added.

- Prefer Espresso for in-app UI/E2E flows.
- Use UI Automator only when needed for system UI or cross-app flows.
- Choose the test architecture that best fits the implementation.
- Add meaningful tests for happy paths and key error states.
- Normal CI tests should not require:
  - live Google Lens
  - real MediaProjection
  - real AnkiDroid
  - real overlay permission
- Add manual test notes for flows that are hard to automate reliably.

Important behaviors to test over time:

- Static OCR box persists normalized coordinates.
- OCR trigger uses saved OCR box.
- Fake OCR result can be edited.
- Lookup works from tapped OCR text.
- Lapis default mapping uses ClickCard.
- Mining blocked until AnkiDroid setup succeeds.
- VAD does not run on OCR trigger.
- VAD runs after Mine using OCR session timestamp.
- Dark UI remains readable and accessible.
- Overlay remains compact and unobtrusive.

## Submodule handling

Use:

```bash
git submodule update --init --recursive
```

Do not replace submodules with copied source.

Do not make broad changes inside `external/` unless the user asks.

## Code style

- Prefer Kotlin.
- Keep services small and testable.
- Prefer explicit interfaces for platform-heavy components.
- Keep platform APIs wrapped so tests can use fakes.
- Avoid large unrelated refactors.
- Keep each stage buildable.
- Add or update tests with each feature.
- Update `PLANS.md` when implementation status or project decisions change.

## When uncertain

Make the smallest implementation that satisfies the current stage while preserving the planned architecture.

Do not add future features early unless the prompt asks for them.

If desired packages or tools are not installed, pause and allow the user to install them rather than looking for a workaround.

Prefer TODOs and interfaces over speculative implementation for future items like:

- AnkiconnectAndroid.
- Gamepad support.
- Multi-OCR-box auto-selection.
- Whisper alignment.
- Pre-trim audio on OCR trigger.
