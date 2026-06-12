# External submodules

Ajisai keeps hoshidicts dependencies as Git submodules. Their source must not be copied into the app package.

Initialize after cloning:

```bash
git submodule update --init --recursive
```

Update the pinned upstream revisions intentionally:

```bash
git submodule update --remote --merge external/hoshidicts
git submodule update --remote --merge external/hoshidicts-kotlin-bridge
```

One nested hoshidicts dependency currently uses an SSH-form GitHub URL. Environments without GitHub SSH access can initialize with a command-scoped HTTPS rewrite:

```bash
git -c url.https://github.com/.insteadOf=git@github.com: submodule update --init --recursive
```
