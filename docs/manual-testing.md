# Manual testing

## Stage 3 screen capture

Use a physical Android device because normal automated tests use capture fakes and do not grant real MediaProjection permission.

1. Launch Ajisai.
2. Tap **Start capture**.
3. On Android 13+, allow notifications when prompted so the foreground notification is visible.
4. Accept Android's screen-capture permission prompt.
5. Verify the Ajisai screen-capture foreground notification appears.
6. Tap **Capture screenshot**.
7. Verify a current screenshot preview appears on the main screen.
8. Tap **Stop capture**.
9. Verify the notification disappears and further capture is disabled.
10. Tap **Start capture** again, deny the screen-capture permission prompt, and verify Ajisai shows a permission-denied message without crashing or starting capture.

This stage captures only on demand. It does not add an overlay, OCR, audio capture, VAD, or Anki export.

## Stage 4 floating overlay

1. Launch Ajisai.
2. Tap **Grant overlay permission** and allow Ajisai to display over other apps.
3. Return to Ajisai and verify the screen says overlay permission is granted.
4. Tap **Start overlay** and verify the service state changes to running.
5. Open another app and verify the small Ajisai bubble remains visible.
6. Drag the bubble and verify it follows the gesture without opening the panel.
7. Tap the bubble and verify the compact panel opens or closes.
8. Tap **Close / Hide Overlay** and verify the panel and bubble disappear.
9. Reopen Ajisai and verify it remains stable and reports the overlay service stopped.
10. Revoke or deny overlay permission and verify starting the overlay is disabled and does not crash.

Stage 4 does not capture continuously, query dictionaries, mine cards, or process audio.

## Stage 5 static OCR box

Use a physical Android device because the editor is a real application overlay and normal unit tests use geometry/repository fakes.

1. Start screen capture and request one screenshot so a debug frame exists.
2. Start the overlay, tap the bubble, and tap **Configure OCR Box**.
3. Verify a dimmed full-screen editor appears with one outlined rectangle, eight resize handles, and Save/Cancel controls.
4. Drag inside the rectangle and verify it moves while remaining inside the screen.
5. Drag corner and edge handles and verify the rectangle resizes without becoming inverted or too small.
6. Tap **Save**, reopen **Configure OCR Box**, and verify the saved rectangle is restored.
7. Stop and restart the overlay, reopen the editor, and verify the rectangle still persists.
8. Rotate the device or otherwise change resolution, reopen the editor, and verify the normalized position and size remain reasonable.
9. Move or resize the rectangle, tap **Cancel**, reopen the editor, and verify the previously saved rectangle is unchanged.
10. In the main app, verify **Preview saved box crop** shows the expected screenshot region and **Reset OCR box** restores the default normalized coordinates.
11. Verify the Stage 3 capture start/screenshot/stop flow and the Stage 4 bubble drag/tap/close flow still work.

Stage 5 covers coordinate editing and bitmap cropping. The overlay **OCR** action is exercised by the Stage 6 checklist below.

## Stage 6 Google Lens OCR

Use a physical Android device with network access. Normal automated tests use fake OCR engines and transports and do not call live Google Lens.

1. Open a game, visual novel, browser page, or test image with visible Japanese text.
2. Launch Ajisai, start screen capture, accept the MediaProjection prompt, and verify capture becomes active.
3. Start the overlay, tap the bubble, choose **Configure OCR Box**, place the box over the Japanese text, and save it.
4. Tap the bubble, then tap **OCR**. Verify the result panel first shows **Capturing fresh screenshot…** and then **Recognizing text…**.
5. Change the text visible under the OCR box immediately before tapping **OCR** and verify the result reflects the new screen content rather than the previous debug screenshot. This confirms one fresh screenshot was requested.
6. Verify text outside the saved OCR box is not included when the crop contains only the intended Japanese text.
7. Verify the success panel contains editable OCR text. Insert, delete, and replace Japanese characters and confirm the edited text remains visible.
8. Tap **Retry** and verify one new capture/OCR run occurs and the loading states appear again. Leave the screen unchanged afterward and verify later frame updates do not trigger OCR continuously.
9. Tap **Copy** and verify the edited OCR text is placed on the clipboard.
10. Tap **Clear / Close** and verify the result panel closes while the compact bubble remains available.
11. Stop screen capture, tap **OCR**, and verify the panel reports that screen capture must be started instead of using a stale frame.
12. Disable network access or otherwise force a Lens request failure if practical, tap **Retry**, and verify a network or Lens error appears without crashing. Restore network access and retry successfully.
13. Verify **Configure OCR Box**, **Close / Hide Overlay**, bubble drag/tap, and compact panel behavior still work.
14. Verify the Stage 3 capture preview/stop flow and Stage 5 OCR box persistence/crop preview still work.
15. Verify OCR does not show dictionary definitions, mine a card, access AnkiDroid, record/export audio, or run VAD.

Optional debug artifact check: enable OCR debug saving through the test/developer configuration, run OCR, and verify the encoded crop and raw Lens response are written under the app's `files/ocr-debug/` directory. With debug saving disabled, verify those files are not created.
