# Compressor — Archive Fork (1.6.0)

Personal fork of [JoshAtticus/Compressor](https://github.com/JoshAtticus/Compressor)
focused on phone-archive workflows. Upstream Compressor is a great single-file
compress-and-share tool. This fork adds three things needed for **bulk archival
of Pixel videos**:

1. **Metadata preservation** — `DATE_TAKEN`, GPS (`udta/©xyz`), rotation, and HDR
   pipeline are carried from source to compressed copy. Google Photos shows the
   correct date and location on the archive copy.
2. **Batch compression** — pick multiple videos in one go, set a preset once,
   zero per-file interaction.
3. **Optional in-place overwrite** — toggle "Replace originals"; after a
   verified save, the originals are passed to a single Android system
   confirmation dialog that deletes them.

## What changed vs. upstream

| Area | Upstream | Fork |
|------|----------|------|
| Video picker | `PickVisualMedia` (1 file) | `PickMultipleVisualMedia` (≤30) |
| `DATE_TAKEN` | not written | written to MediaStore + patched into mvhd |
| GPS | stripped | read with `ACCESS_MEDIA_LOCATION`, written to `udta/©xyz` |
| Save flow | manual per file | auto-save + auto-advance in batch mode |
| Originals | untouched | optional `MediaStore.createDeleteRequest` at batch end |
| `targetSdk` | 37 (preview) | 36 (stable) |

All metadata patching lives in
`app/src/main/java/compress/joshattic/us/Mp4MetadataPatcher.kt` — a hand-rolled
ISO BMFF box editor that appends `©xyz` to `moov/udta` and rewrites
`mvhd`/`tkhd` `creation_time`. No new dependencies.

## Installing the APK on your Pixel 10 via USB-C

You already have the cable, so this is the fastest path — no GitHub round-trip
needed.

1. **On the Mac**, the build dropped a release APK here:

   ```
   app/build/outputs/apk/release/app-release.apk
   ```

   (If you only want to test, the debug APK at
   `app/build/outputs/apk/debug/app-debug.apk` works too. Use the **release**
   APK for the actual archive workflow — it's a separate app ID? No, same ID,
   just less debug overhead.)

2. **Plug the Pixel into the Mac via USB-C.** When the phone asks for the
   transfer mode, choose **File Transfer (MTP)** — not "Charge only".

3. On the Mac, open Finder → **Locations** → your Pixel should appear as a
   drive. Open it, navigate to `Internal storage/Download/`, and drop the APK
   in. (macOS Sonoma+ needs the *Android File Transfer* app — install it from
   <https://www.android.com/filetransfer/> if Finder doesn't see the phone.)

4. **On the phone**: open Files / Files by Google → Downloads → tap
   `app-release.apk`. Android will warn that installing from this source isn't
   allowed; tap *Settings* → enable *Allow from this source* → back, then
   **Install**. (After installing once, you can disable the toggle again.)

5. The first time you compress a video with GPS, the app will prompt for
   *Photos and videos / Allow access to all photos*. Grant it — that's what
   `ACCESS_MEDIA_LOCATION` needs to read the un-redacted GPS.

## Installing & updating via Obtainium

[Obtainium](https://github.com/ImranR98/Obtainium) tracks GitHub Releases and
side-loads the latest APK whenever you trigger an update. Set up once, then
future releases land on the phone without USB.

### One-time setup

1. **Push this fork** to a GitHub repo of your choice. For example:

   ```bash
   git remote remove origin
   git remote add origin git@github.com:YOUR_USER/Compressor-archive.git
   git add -A
   git commit -m "Archive fork: batch + metadata + in-place"
   git push -u origin main
   ```

2. **Tag a release.** The CI workflow at
   `.github/workflows/release.yml` is configured to fire on any `v*` tag and
   attach a signed APK to a GitHub Release.

   ```bash
   git tag v1.6.0
   git push origin v1.6.0
   ```

   Within ~3–5 minutes the Actions run will produce a Release at
   `https://github.com/YOUR_USER/Compressor-archive/releases/tag/v1.6.0` with
   `Compressor-archive-v1.6.0.apk` attached.

### Adding to Obtainium

1. Open Obtainium on the Pixel → **+** (Add app).
2. Paste the **repo URL**, not a release URL:

   ```
   https://github.com/YOUR_USER/Compressor-archive
   ```

3. Obtainium auto-detects the source as **GitHub** and shows the latest
   release. Leave the defaults — Obtainium will match the APK by `.apk`
   extension on the latest release asset.
4. Tap **Add**. Obtainium installs the APK (you'll see the same Android
   "Install unknown app" prompt the first time).
5. From now on, push a new `vX.Y.Z` tag → wait for the workflow → open
   Obtainium and tap *Check for updates* to pull the new build.

### Obtainium configuration that actually matters

The default GitHub source works because of three things this fork sets up:

- **APK is attached to a Release**, not just sitting in `dist/`. Obtainium's
  GitHub source reads release assets, not repo files.
- **Tag matches `v*` semver pattern**, so Obtainium's version comparison
  (`v1.6.0 > v1.5.7`) sorts correctly. Don't tag `1.6` or `latest`.
- **APK is signed with a stable keystore** (the debug keystore — the same one
  every machine that runs the workflow uses, because Android Gradle Plugin
  reuses it). If you swap to a different keystore mid-stream, Android refuses
  the upgrade and Obtainium will report "install failed" — you'd need to
  uninstall first.

If you want production-grade signing later, generate a keystore with
`keytool -genkeypair`, store it as a base64 GitHub secret, and adapt
`release.yml` to decode it before `assembleRelease`. Out of scope for the
archive workflow.

## Build locally

```bash
brew install openjdk@21 android-commandlinetools
export JAVA_HOME=/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home
export ANDROID_HOME=/opt/homebrew/share/android-commandlinetools
export PATH=$JAVA_HOME/bin:$ANDROID_HOME/cmdline-tools/latest/bin:$ANDROID_HOME/platform-tools:$PATH
yes | sdkmanager --licenses
sdkmanager "platform-tools" "platforms;android-36" "build-tools;36.0.0"
echo "sdk.dir=$ANDROID_HOME" > local.properties
./gradlew assembleRelease
```

APK lands at `app/build/outputs/apk/release/app-release.apk`.

## Known limitations

- **HDR pipeline on Pixel 10 H264/H265**: upstream keeps the
  `HDR_MODE_TONE_MAP_HDR_TO_SDR_USING_OPEN_GL` workaround. We did not touch it
  — to preserve true 10-bit HDR, set the codec to AV1 in the Video tab; the
  workaround branch in `CompressorViewModel.startCompression` doesn't activate
  for AV1.
- **GPS write requires moov-after-mdat layout**. Media3 1.5.0 produces this by
  default. `Mp4MetadataPatcher` falls back to date-only if a future version
  flips to moov-first (it detects this and bails on the GPS append rather than
  corrupt mdat offsets).
- **In-place delete** uses `MediaStore.createDeleteRequest` (API 30+). On
  Android 10 it silently no-ops — fine for a Pixel 10, but worth knowing if
  this ever runs on older devices.
