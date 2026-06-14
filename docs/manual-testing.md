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
7. Tap the bubble and verify the compact HUD appears; tap it again and verify the HUD collapses while the bubble remains.
8. Use the main app's **Stop overlay** action and verify the HUD and bubble disappear.
9. Reopen Ajisai and verify it remains stable and reports the overlay service stopped.
10. Revoke or deny overlay permission and verify starting the overlay is disabled and does not crash.

Stage 4 does not capture continuously, query dictionaries, mine cards, or process audio.

## Stage 5 static OCR box

Use a physical Android device because the editor is a real application overlay and normal unit tests use geometry/repository fakes.

1. Start screen capture and request one screenshot so a debug frame exists.
2. Start the overlay, tap the bubble to show the HUD, and tap the compact **Box** control.
3. Verify a dimmed full-screen editor appears with one outlined rectangle, eight resize handles, and Save/Cancel controls.
4. Drag inside the rectangle and verify it moves while remaining inside the screen.
5. Drag corner and edge handles and verify the rectangle resizes without becoming inverted or too small.
6. Tap **Save**, reopen **Configure OCR Box**, and verify the saved rectangle is restored.
7. Stop and restart the overlay, reopen the editor, and verify the rectangle still persists.
8. Rotate the device or otherwise change resolution, reopen the editor, and verify the normalized position and size remain reasonable.
9. Move or resize the rectangle, tap **Cancel**, reopen the editor, and verify the previously saved rectangle is unchanged.
10. In the main app, verify **Preview saved box crop** shows the expected screenshot region and **Reset OCR box** restores the default normalized coordinates.
11. Verify the Stage 3 capture start/screenshot/stop flow and the Stage 4 bubble drag/tap/close flow still work.

Stage 5 covers coordinate editing and bitmap cropping. The toggle-triggered OCR flow is exercised by the Stage 6.5 checklist below.

## Stage 6.5 overlay HUD and Google Lens OCR

Use a physical Android device with network access. Normal automated tests use fake OCR engines and transports and do not call live Google Lens.

1. Open a game, visual novel, browser page, or test image with visible Japanese text.
2. Launch Ajisai, start screen capture, accept the MediaProjection prompt, and verify capture becomes active.
3. Start the overlay and verify the collapsed state shows only the draggable bubble. Verify game touches outside the bubble pass through normally.
4. Tap the bubble to show the HUD. Verify compact top-corner **Box**, **Set**, and **Hide** controls and the bottom OCR panel appear, and exactly one OCR attempt starts immediately.
5. Tap **Box**, place the OCR box over the Japanese text, and save it. Verify the normal HUD returns and starts one fresh OCR attempt.
6. Verify the bottom panel first shows **Capturing fresh screenshot…** and then **Recognizing text…** without opening the soft keyboard.
7. Change the text visible under the OCR box immediately before toggling the HUD on and verify the result reflects the new screen content rather than the previous screenshot. This confirms one fresh screenshot was requested.
8. Verify text outside the saved OCR box is not included when the crop contains only the intended Japanese text.
9. Verify the success panel shows readable, read-only Japanese OCR text and has no Retry button or editable result field.
10. Verify touches outside the bottom panel and quick controls continue to reach the game.
11. Toggle the HUD off during **Capturing** or **Recognizing**. Verify the OCR job is canceled, the bottom panel and quick controls disappear, and only the bubble remains.
12. Toggle the HUD on again and verify one new OCR attempt starts. Leave the screen unchanged afterward and verify later frame updates do not trigger continuous OCR.
13. Stop screen capture, toggle the HUD on, and verify the panel reports that capture must be started instead of using a stale frame.
14. Restart capture after a prior capture failure, toggle the HUD off and on, and verify the next fresh OCR attempt is allowed instead of immediately repeating the stale error.
15. Disable network access or otherwise force a Lens request failure if practical. Verify a compact network or Lens error appears without crashing, then restore network access and toggle the HUD off/on to retry successfully.
16. Use `test-fixtures/ocr/vertical-manga-furigana.png` or comparable furigana-heavy game text and verify small kana readings are not duplicated into the displayed sentence. The fixture should read `わたしの知らない一週間が教室にはあって`.
17. Test normal kana-only dialogue and verify the furigana filter does not delete it.
18. Verify **Box** still opens the OCR box editor and returns to the normal HUD state after Save or Cancel. Verify **Set** opens Ajisai settings and **Hide** collapses the HUD without stopping the overlay service.
19. Verify the Stage 3 capture preview/stop flow, Stage 4 bubble drag/toggle/service stop flow, and Stage 5 OCR box persistence/crop preview still work.
20. Verify OCR does not show dictionary definitions, mine a card, access AnkiDroid, record/export audio, or run VAD.

Optional debug artifact check: enable OCR debug saving through the test/developer configuration, run OCR, and verify the encoded crop and raw Lens response are written under the app's `files/ocr-debug/` directory. With debug saving disabled, verify those files are not created.
