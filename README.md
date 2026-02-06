# ReShare

Android app that receives shared documents, converts them via Pandoc, and reshares to other targets.

## User Guide

See [docs/USER_GUIDE.md](docs/USER_GUIDE.md) for installation and usage instructions.

## Architecture

### Share Intent Flow
1. `MainActivity` receives `ACTION_SEND` intent with document/text
2. `FormatDetector` identifies input format (MIME type, extension, content sniffing)
3. `FormatPickerDialog` presents output format options
4. `PandocConverter` or `PdfConverter` performs conversion
5. `ShareHandler` reshares converted file via `FileProvider`

### Key Components
- `MainActivity` - Share intent receiver, orchestrates conversion flow
- `FormatDetector` - Detects input format from MIME type, extension, or magic bytes
- `FormatPickerDialog` - Bottom sheet for selecting output format
- `PandocConverter` - Executes bundled Pandoc binary for document conversion
- `PdfConverter` - HTML to PDF via WebView/PrintManager
- `ShareHandler` - Reshares converted files or text content
- `ProgressNotifier` - Shows progress/error notifications
- `CacheManager` - Cleans up old converted files

### Supported Formats
- **Input**: Plain text, Markdown, HTML, DOCX, ODT, EPUB
- **Output**: PDF, DOCX, HTML, Markdown, Plain text

### Pandoc Integration
- Pandoc binary bundled as native library (`libpandoc.so`)
- Dependencies from Termux packages (zlib, libiconv, libgmp, lua54, libffi)
- Gradle task `downloadPandoc` fetches binaries for aarch64 and x86_64

## Build

### Prerequisites
- Android Studio Hedgehog (2023.1.1) or later
- JDK 17
- Android SDK 34

### Setup
```bash
# Download Pandoc binaries (required before first build)
./gradlew downloadPandoc

# Build debug APK
./gradlew assembleDebug

# Install on device
./gradlew installDebug
```

## Testing

### Unit Tests
```bash
./gradlew test
```
- `PandocCommandTest` - Command building, error mapping
- `FormatDetectorTest` - Format detection logic
- `CacheManagerTest` - Cache cleanup

### Instrumented Tests
```bash
./gradlew connectedAndroidTest
```
- `PandocExecutionTest` - Pandoc binary execution on device
- `TextConversionTest` - Text format conversions (txt, md, html)
- `BinaryConversionTest` - Binary format conversions (docx, odt, epub)

## Contributing

### Project Structure
```
app/
  src/
    main/kotlin/com/reshare/
      MainActivity.kt           # Share intent entry point
      ReShareApplication.kt     # Application class
      converter/                # Conversion logic
      notification/             # Progress/error notifications
      share/                    # Reshare handling
      ui/                       # Dialogs and activities
      util/                     # Utilities (cache management)
    test/                       # Unit tests (JVM)
    androidTest/                # Instrumented tests (device)
```

### Commit Guidelines
- Keep commits minimal and focused
- Ensure code compiles before committing
- Run tests: `./gradlew test connectedAndroidTest`

### Pull Request Process
1. Create feature branch from `main`
2. Implement changes with tests
3. Ensure all tests pass
4. Submit PR with clear description
