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
8. Tap **OCR** and verify the placeholder toast/log message appears.
9. Tap **Configure OCR Box** and verify the placeholder toast/log message appears.
10. Tap **Close / Hide Overlay** and verify the panel and bubble disappear.
11. Reopen Ajisai and verify it remains stable and reports the overlay service stopped.
12. Revoke or deny overlay permission and verify starting the overlay is disabled and does not crash.

Stage 4 does not perform OCR, edit OCR boxes, capture continuously, query dictionaries, mine cards, or process audio.
