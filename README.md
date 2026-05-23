# ⚡ SmartPlayer

A modern, dark-themed desktop video player built with **Java 17**, **JavaFX 21**, and **VLCJ**. Think of it as a lightweight, smart VLC/Netflix-style local media player that recursively scans folders, intelligently sorts videos, and plays them in sequence.

![Java](https://img.shields.io/badge/Java-17-blue)
![JavaFX](https://img.shields.io/badge/JavaFX-21-green)
![VLCJ](https://img.shields.io/badge/VLCJ-4.8.3-orange)

---

## ✨ Features

### 📂 Smart Folder Scanning
- Select any folder and recursively discover all video files
- Supports: `.mp4`, `.mkv`, `.avi`, `.mov`, `.wmv`, `.flv`, `.webm`, `.m4v`, `.ts`, `.vob`, `.3gp`, `.mpg`, `.mpeg`
- Drag-and-drop folder support

### 🧠 Intelligent Video Sorting
- Automatically detects and sorts videos by sequence:
  - `S01E01`, `S02E03` (Season/Episode)
  - `Episode 01`, `EP02`
  - `Part 1`, `Part 2`
  - `Chapter 3`
  - Numeric suffixes as fallback
- Groups videos by series name

### ▶️ Full Playback Controls
- Play / Pause / Stop
- Skip ±10 seconds
- Seek bar with drag support
- Volume slider + mute toggle
- Brightness control
- Playback speed: 0.25x, 0.5x, 0.75x, 1x, 1.25x, 1.5x, 2x
- Fullscreen mode with auto-hiding controls
- Current time / Total duration / Remaining time

### 🔁 Auto-Next Playback
- Automatically plays the next video when the current one finishes
- Sequential playback through the sorted playlist

### 📝 Subtitle Support
- Auto-detects `.srt`, `.sub`, `.ass`, `.ssa`, `.vtt` files
- Toggle subtitles on/off with button or keyboard

### 💾 Resume Playback
- Saves playback position every 5 seconds
- Resumes from last position when reopening
- Persists volume, speed, and last folder

### 🕐 Watch History
- Tracks recently played videos (last 50)
- Quick access from sidebar

### ⌨️ Keyboard Shortcuts

| Key | Action |
|-----|--------|
| `Space` | Play / Pause |
| `←` | Skip back 10s |
| `→` | Skip forward 10s |
| `↑` | Volume up |
| `↓` | Volume down |
| `F` | Toggle fullscreen |
| `M` | Toggle mute |
| `S` | Toggle subtitles |
| `N` | Next video |
| `P` | Previous video |
| `Esc` | Exit fullscreen |

---

## 📋 Prerequisites

### 1. Java 17+
Download and install JDK 17 or later:
- [Oracle JDK 17](https://www.oracle.com/java/technologies/javase/jdk17-archive-downloads.html)
- [Adoptium (Eclipse Temurin) JDK 17](https://adoptium.net/)

### 2. VLC Media Player (64-bit)
**VLCJ requires VLC to be installed on your system.**
- Download [VLC 3.x (64-bit)](https://www.videolan.org/vlc/) for Windows
- Install to the default location (`C:\Program Files\VideoLAN\VLC`)
- **Important:** The VLC architecture (64-bit) must match your JDK architecture

### 3. Maven 3.9+
- Download [Maven](https://maven.apache.org/download.cgi)
- Or use the Maven wrapper if provided

---

## 🚀 Build & Run

### Build the project
```bash
mvn clean compile
```

### Run the application
```bash
mvn javafx:run
```

### Package as executable JAR
```bash
mvn clean package
java -jar target/smartplayer-1.0.0.jar
```

---

## 📁 Project Structure

```
src/main/java/com/smartplayer/
├── App.java                    # Main application + wiring
├── Launcher.java               # Fat JAR entry point
├── models/
│   ├── VideoFile.java          # Video file data model
│   ├── Playlist.java           # Observable playlist
│   └── PlaybackState.java      # Persistence state
├── scanner/
│   ├── FolderScanner.java      # Recursive folder scanner
│   └── VideoSorter.java       # Regex-based smart sorting
├── player/
│   ├── MediaPlayerManager.java # VLCJ player lifecycle
│   └── SubtitleManager.java    # Subtitle detection/toggle
├── ui/
│   ├── MainView.java           # Root layout + drag-drop
│   ├── SidebarView.java        # Folder & playlist sidebar
│   ├── PlayerView.java         # Video rendering surface
│   ├── ControlBarView.java     # Bottom media controls
│   └── FullscreenHandler.java  # Fullscreen + auto-hide
└── utils/
    ├── TimeFormatter.java      # Duration formatting
    ├── PersistenceManager.java # JSON state persistence
    └── KeyboardShortcuts.java  # Global key bindings

src/main/resources/com/smartplayer/
└── styles/
    └── dark-theme.css          # Modern dark theme
```

---

## 🎨 UI Design

- **Dark theme** with vibrant coral accent (`#e94560`)
- **Left sidebar** with folder selector, playlist, and recent history
- **Bottom controls** styled like YouTube/VLC
- **Glassmorphism** effects on controls
- **Auto-hiding controls** in fullscreen mode
- **Custom scrollbars** and styled sliders

---

## 🔧 Troubleshooting

### "Unable to load native library"
- Ensure VLC 64-bit is installed
- Ensure your JDK is also 64-bit
- VLC must be in `PATH` or installed in the default location

### "No video output"
- Check that the video codec is supported by VLC
- Try a different video file format

### Application won't start from JAR
- Use `java -jar target/smartplayer-1.0.0.jar`
- Ensure Java 17+ is on your `PATH`

---

## 📄 License

This project is for educational and personal use.
