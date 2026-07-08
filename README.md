# Cloud Journal — Android Append-Only Journal & Cloud Backup

An Android journaling application built with **Clean Architecture**, **Jetpack Compose**, **Room**, **WorkManager**, and **Google Drive REST API** integration. Entries append to unified daily logs locally and sync to Google Drive in the background.

## Architecture

```
                      +-----------------------+
                      |    Jetpack Compose    |
                      |   UI (Views/Screens)  |
                      +-----------+-----------+
                                  |
                      +-----------v-----------+
                      |       ViewModel       |
                      +-----------+-----------+
                                  |
                      +-----------v-----------+
                      |   JournalRepository   |
                      +-----+-----------+-----+
                            |           |
            +---------------+           +---------------+
            |                                           |
+-----------v-----------+                   +-----------v-----------+
|     Room Database     |                   |  Google Drive API     |
|    (Local Storage)    |                   |   (Remote Backup)     |
+-----------------------+                   +-----------------------+
```

## Features

- **Append-only daily log model** — text entries append to a single `consolidatedText` field per calendar day (`YYYY-MM-DD`)
- **Voice notes** — `.m4a` recordings stored in app-private storage with playback controls
- **Automatic cloud backup** — every local write enqueues a WorkManager job with network constraints
- **Optimistic concurrency** — local `lastModifiedLong` is the source of truth; cloud files are force-overwritten from Room
- **Offline resilience** — local saves succeed immediately; uploads defer until connectivity returns
- **Orphan cleanup** — missing audio files trigger metadata row deletion; failed uploads retry with `googleDriveFileId = null`

## Project Structure

```
app/src/main/java/com/journal/app/
├── data/
│   ├── local/          # Room entities, DAO, database
│   ├── remote/           # GoogleDriveClient, BackupWorker, BackupScheduler
│   └── repository/       # JournalRepositoryImpl
├── domain/
│   ├── model/            # JournalDay, VoiceEntry
│   └── repository/       # JournalRepository interface
├── audio/                # AudioRecorder, AudioPlayer
└── presentation/         # MainActivity, JournalViewModel, HomeScreen
```

## Setup

### Prerequisites

- Android Studio Ladybug or newer
- JDK 17
- A Google Cloud project with OAuth 2.0 credentials (Android client)

### Google Sign-In Configuration

1. Create an OAuth 2.0 **Android** client in [Google Cloud Console](https://console.cloud.google.com/)
2. Add your app's SHA-1 fingerprint and package name `com.journal.app`
3. Enable the **Google Drive API** for the project
4. The app requests `DriveScopes.DRIVE_FILE` at sign-in

### Build & Run

```bash
./gradlew assembleDebug
```

Install the generated APK on a device or emulator, sign in with Google, and start journaling.

### Continuous Integration

GitHub Actions builds the debug APK on every push to `main` and on pull requests targeting `main`. Workflow: [`.github/workflows/android-ci.yml`](.github/workflows/android-ci.yml).

- **Actions tab** — view build status and logs
- **Pull requests** — CI checks appear on the PR checks panel
- **Artifacts** — successful runs upload `app-debug-apk` (retained 14 days); download from the workflow run summary

## Cloud Backup Layout

All files are stored in a Drive folder named **My Android Cloud Journal**:

| Local Data | Remote File |
|---|---|
| `JournalDayEntity` for `2026-07-08` | `journal_2026-07-08.txt` |
| Voice entry `uuid` on `2026-07-08` | `voice_2026-07-08_uuid.m4a` |

## Edge Case Handling

| Scenario | Guardrail |
|---|---|
| Concurrent writes while sync is in-flight | WorkManager uses `APPEND_OR_REPLACE`; worker compares `localLastModified` vs remote `modifiedTime` and always writes local consolidated text |
| Offline saves | `NetworkType.CONNECTED` constraint queues work until internet is available |
| Missing audio files | Worker deletes orphan `VoiceEntryEntity` rows |
| Failed audio upload | `googleDriveFileId` stays `null`; worker retries on next run |

## Tech Stack

- Kotlin 2.0 · Jetpack Compose · Material 3
- Room 2.6 · Coroutines · Flow
- WorkManager 2.10
- Google Play Services Auth · Google Drive API v3
