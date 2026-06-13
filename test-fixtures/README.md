# Dictionary test fixtures

Do not commit full Yomitan dictionary `.zip` archives to git. Small synthetic fixtures
may be added only when they are intentionally reviewed and are not full dictionaries.

The real JMdict regression test downloads this fixture on demand:

`https://github.com/yomidevs/jmdict-yomitan/releases/latest/download/JMdict_english_with_examples.zip`

It is cached locally under:

`build/e2e/assets/JMdict_english_with_examples.zip`

Run:

```bash
./scripts/e2e-real-jmdict.sh
```

The real JMdict test is intentionally opt-in because it is slower and depends on a connected Android device.
