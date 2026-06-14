# OCR reference fixtures

`vertical-manga-furigana.png` is a manual Google Lens OCR reference crop. Its expected cleaned text is:

```text
わたしの知らない一週間が教室にはあって
```

Normal automated tests do not submit this image to Google Lens. The regression test uses fake Lens response bytes with equivalent normalized line geometry.
