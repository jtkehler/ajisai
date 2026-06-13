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
10. With the Stage 3 screenshot available, tap **OCR** in the overlay panel and verify Ajisai reports only a debug crop size and does not run OCR.
11. In the main app, verify **Preview saved box crop** shows the expected screenshot region and **Reset OCR box** restores the default normalized coordinates.
12. Verify the Stage 3 capture start/screenshot/stop flow and the Stage 4 bubble drag/tap/close flow still work.

Stage 5 performs only coordinate editing and bitmap cropping. It does not call Google Lens, show OCR text, query dictionaries, mine cards, or process audio/VAD.
