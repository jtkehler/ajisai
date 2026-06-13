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
