# Changelog

## Unreleased

### Added
- **URL input** — shared text containing a URL is now fetched and converted instead of treated as plain text
- **Image input** — shared images (PNG, JPEG, WebP, GIF) are converted to PDF via WebView rendering
- **Twitter/X support** — tweet URLs are fetched via syndication API, with fallback to a minimal HTML document
- **Instagram support** — Instagram URLs are scraped for OG meta tags (title, description, image)
- INTERNET permission for URL fetching
- Image MIME type intent filters for SEND and SEND_MULTIPLE

## 1.0

### Added
- Document conversion via bundled Pandoc binary
- Input formats: Plain text, Markdown, Org, HTML, DOCX, ODT, EPUB, LaTeX, PDF
- Output formats: PDF, DOCX, HTML, Markdown, Plain text, LaTeX
- PDF input via Poppler (pdftohtml) pipeline
- Share intent receiver for single and batch files
- Format picker with icon grid and favorite formats
- Document templates and CSS styling
- Text preview with selection before sharing
- Save to storage via SAF
- Conversion history with file management
- Format matrix settings (enable/disable input and output formats)
- Progress notifications during conversion
- Automation intent API for programmatic conversion
- Notification permission handling for Android 13+
- Cache cleanup for old converted files
- Material 3 UI with dynamic color support
- CI/CD workflows with release signing
- Play Store and F-Droid listing materials
