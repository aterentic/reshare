# ReShare User Guide

ReShare is an Android app that receives shared documents, converts them between formats using Pandoc, and reshares the result to other apps or storage.

## Installation

### Prerequisites

- Android 8.0 (Oreo) or later
- ARM64 or x86_64 device

### Sideloading the APK

1. Download the ReShare APK file to your device.
2. Open **Settings > Apps > Special app access > Install unknown apps** and enable it for your file manager or browser.
3. Tap the downloaded APK to install.
4. When prompted by Play Protect, tap **Install anyway** (the app is not on the Play Store).

## Usage

### Sharing a document to ReShare

1. Open any app that has a document or text you want to convert (Files, email, browser, note app, etc.).
2. Tap the **Share** button and select **ReShare** from the share sheet.
3. ReShare detects the input format automatically from the file type, extension, or content.

### Selecting an output format

After sharing a document, a format picker appears as a bottom sheet with icon buttons for each available output format. Tap the format you want.

Favorite formats appear first. Long-press any format to toggle it as a favorite.

### Document templates

When converting to HTML or PDF, you can choose a template that controls the visual style of the output:

- **Default** -- Standard Pandoc output with no extra styling.
- **Clean** -- Minimal, readable styling.
- **Academic** -- Formal layout suitable for papers and reports.

### Text preview

When converting to a text-based format (Markdown, Plain text, HTML, LaTeX), ReShare shows a preview of the converted text. You can select a portion of the text before sharing, or share the full result.

### Resharing the result

After conversion completes, the standard Android share sheet opens so you can send the converted file to any app: email, messaging, cloud storage, etc.

Text-based formats (Markdown, Plain text, HTML, LaTeX) can be shared as inline text instead of as a file attachment. This behavior is controlled by the **Share text formats as text** toggle in Settings.

### Save to storage

Instead of resharing to another app, you can save the converted file directly to your device or cloud storage using the **Save to...** option, which uses Android's Storage Access Framework (SAF). This lets you pick any folder, including connected cloud storage providers.

### Batch conversion

You can share multiple files at once. ReShare processes each file with the selected output format and presents the results together for resharing.

## Supported Formats

### Input/output format matrix

| Input \ Output | PDF | DOCX | HTML | Markdown | Plain text | LaTeX |
|----------------|-----|------|------|----------|------------|-------|
| Plain text     | Yes | Yes  | Yes  | Yes      | --         | Yes   |
| Markdown       | Yes | Yes  | Yes  | --       | Yes        | Yes   |
| Org            | Yes | Yes  | Yes  | Yes      | Yes        | Yes   |
| HTML           | Yes | Yes  | --   | Yes      | Yes        | Yes   |
| DOCX           | Yes | --   | Yes  | Yes      | Yes        | Yes   |
| ODT            | Yes | Yes  | Yes  | Yes      | Yes        | Yes   |
| EPUB           | Yes | Yes  | Yes  | Yes      | Yes        | Yes   |
| LaTeX          | Yes | Yes  | Yes  | Yes      | Yes        | --    |

Cells marked "--" are same-format conversions and are not offered.

PDF output is produced via HTML rendered through Android's WebView and PrintManager, not directly by Pandoc.

## Settings

### Notification permissions

ReShare shows notifications for conversion progress and errors. On Android 13+, grant the notification permission when prompted, or enable it in **Settings > Apps > ReShare > Notifications**.

### Format matrix

The format matrix setting lets you control which output formats appear in the picker for each input format. Disable combinations you never use to keep the picker uncluttered.

### Share text formats as text

When enabled, text-based output formats (Markdown, Plain text, HTML, LaTeX) are shared as inline text content rather than as file attachments. Useful for pasting converted content directly into messaging or note apps.

### Favorite output formats

Long-press any format in the picker to mark it as a favorite. Favorites are shown first in the format picker for faster access.

## Conversion History

ReShare keeps a history of recent conversions. You can revisit past results to reshare them without converting again. Old files are cleaned up automatically by the cache manager.

## Automation API

ReShare exposes an intent-based API for automation tools like Tasker, Automate, or other apps.

### Intent details

- **Action**: `com.reshare.ACTION_CONVERT`
- **Permission**: `com.reshare.permission.CONVERT` (must be declared by the calling app)

### Required extras

| Extra           | Type   | Description                                      |
|-----------------|--------|--------------------------------------------------|
| `INPUT_URI`     | Uri    | Content URI of the document to convert            |
| `OUTPUT_FORMAT` | String | Target format: `pdf`, `docx`, `html`, `markdown`, `plain`, `latex` |

### Optional extras

| Extra          | Type   | Description                                           |
|----------------|--------|-------------------------------------------------------|
| `INPUT_FORMAT` | String | Override auto-detection: `markdown`, `org`, `html`, `docx`, `odt`, `epub`, `latex` |

### Result

On success, the result intent contains:

- `OUTPUT_URI` (Uri) -- Content URI of the converted file.

On failure:

- `ERROR` (String) -- Description of what went wrong.

### Example: Tasker setup

1. Create a new **Task** in Tasker.
2. Add an **App > Send Intent** action with:
   - **Action**: `com.reshare.ACTION_CONVERT`
   - **Extra**: `INPUT_URI:<uri of your file>`
   - **Extra**: `OUTPUT_FORMAT:pdf`
   - **Target**: Activity
3. Add a second action to handle the result using `%result_uri`.

## Troubleshooting

### Large files

Files over 10 MB are rejected. Split large documents or reduce embedded image sizes before sharing.

### Unsupported formats

If ReShare does not appear in the share sheet, the file type is not in the supported input list. Convert the file to a supported format first (e.g., save as DOCX from your editor).

### Conversion timeouts

Pandoc conversions time out after 30 seconds. This can happen with very complex documents containing many tables, footnotes, or embedded content. Simplify the document and try again.

### Conversion errors

If a conversion fails, check the error notification for details. Common causes:

- Malformed input files (corrupted DOCX, invalid HTML).
- Password-protected documents (not supported).
- Missing fonts for PDF output (use the Clean or Default template).

### Notifications not appearing

On Android 13+, ensure notification permission is granted in **Settings > Apps > ReShare > Notifications**.
