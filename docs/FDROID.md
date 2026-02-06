# F-Droid Submission

## Overview

ReShare is a document converter Android app that bundles Pandoc as a native binary. This document covers the F-Droid inclusion process and build considerations.

## License

GPL-3.0-or-later. LICENSE file is at the project root.

## Dependencies

All runtime dependencies are FOSS:

| Dependency | License |
|---|---|
| androidx.core:core-ktx | Apache 2.0 |
| androidx.appcompat:appcompat | Apache 2.0 |
| com.google.android.material:material | Apache 2.0 |
| androidx.constraintlayout:constraintlayout | Apache 2.0 |
| org.jetbrains.kotlinx:kotlinx-coroutines-android | Apache 2.0 |
| androidx.lifecycle:lifecycle-runtime-ktx | Apache 2.0 |

No Google Play Services or proprietary SDKs are used.

## Pandoc Binary

Pandoc and its shared library dependencies are downloaded at build time from the Termux APT repository (`packages.termux.dev`). This is a FOSS source -- Termux packages are built from source and distributed under their original licenses (Pandoc is GPL-2.0-or-later).

The `downloadPandoc` Gradle task handles this:
- Downloads `.deb` packages for aarch64 and x86_64
- Extracts native binaries (pandoc, zlib, libiconv, libgmp, lua54, libffi)
- Places them in `app/src/main/jniLibs/` for bundling

### Packages downloaded

| Package | Source | License |
|---|---|---|
| pandoc | Termux | GPL-2.0-or-later |
| zlib | Termux | zlib license |
| libiconv | Termux | LGPL-2.1-or-later |
| libgmp | Termux | LGPL-3.0-or-later / GPL-2.0-or-later |
| lua54 | Termux | MIT |
| libffi | Termux | MIT |

## F-Droid Build Recipe

The F-Droid build server needs internet access during the `downloadPandoc` task. A suggested `.yml` recipe:

```yaml
Categories:
  - Reading
  - Writing
License: GPL-3.0-or-later
SourceCode: https://github.com/<owner>/reshare
IssueTracker: https://github.com/<owner>/reshare/issues

AutoName: ReShare

RepoType: git
Repo: https://github.com/<owner>/reshare.git

Builds:
  - versionName: '1.0'
    versionCode: 1
    commit: v1.0
    subdir: app
    gradle:
      - yes
    prebuild:
      - cd .. && gradle downloadPandoc
    sudo:
      - apt-get install -y cmake
    scandelete:
      - app/src/main/jniLibs/

AutoUpdateMode: Version
UpdateCheckMode: Tags
CurrentVersion: '1.0'
CurrentVersionCode: 1
```

### Build notes

- `prebuild` runs `downloadPandoc` to fetch Pandoc binaries from Termux before the main Gradle build.
- `sudo` may be needed if native tooling (cmake, etc.) is required. Test on the F-Droid build environment to confirm.
- `scandelete` removes pre-existing jniLibs so F-Droid can verify they are rebuilt from the downloaded packages.
- The build requires network access for the Termux APT downloads. F-Droid typically allows this in `prebuild` steps.

## Metadata

Fastlane metadata is at `fastlane/metadata/android/en-US/`:
- `title.txt` -- app name
- `short_description.txt` -- one-line summary
- `full_description.txt` -- full listing description
- `changelogs/1.txt` -- initial release notes
- `images/` -- placeholder directory for icon, feature graphic, and screenshots

## Submission Steps

1. Ensure the repository is public on a supported forge (GitHub, GitLab, Codeberg, etc.)
2. Tag a release (e.g., `v1.0`)
3. Open a merge request on https://gitlab.com/fdroid/fdroiddata adding the build recipe above
4. F-Droid maintainers will review the recipe, test the build, and merge
5. The app appears in the F-Droid repository after the next index update cycle

## References

- https://f-droid.org/docs/Submitting_to_F-Droid_Quick_Start_Guide/
- https://f-droid.org/docs/All_About_Descriptions_Graphics_and_Screenshots/
- https://f-droid.org/docs/Build_Metadata_Reference/
- https://gitlab.com/fdroid/fdroiddata
