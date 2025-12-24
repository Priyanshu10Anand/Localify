# 🎵 Localify

**Localify** is a premium, high-performance local music player built with modern Android standards. It focuses on a fluid, immersive user experience with a minimalist "glassmorphism" aesthetic.

---

## ✨ Features

- **🎨 Dynamic Theming**: The app UI automatically adapts its color palette to match the artwork of the currently playing song using the **Palette API**.
- **🌫️ Immersive Glass UI**: A beautiful "frosted glass" interface with deep Gaussian blurs and real-time crossfading backgrounds.
- **⚡ Performance Optimized**:
    - **Room Database Cache**: Instant app startup and library loading.
    - **Asynchronous Processing**: All MediaStore scans and bitmapping run on background IO threads to ensure 60fps UI performance.
    - **Media3 (ExoPlayer)**: Robust, industry-standard audio engine for gapless playback.
- **👆 Fluid Gestures**:
    - **Swipe-to-Skip**: Toss the album art left or right to change tracks with physical scaling feedback.
    - **Interactive Queue**: Smoothly slide up the queue from the bottom of the player.
- **🔍 Smart Search**: Real-time song filtering with debounced logic for lag-free searching in large libraries.
- **🔄 Modern Controls**: Morphing play/pause button that changes shape between a circle and a rounded square.

---

## 🛠️ Tech Stack

- **UI**: Jetpack Compose (100%)
- **Audio Engine**: Android Media3 (ExoPlayer & MediaSession)
- **Database**: Room (Local Cache)
- **Image Loading**: Coil
- **Theming**: Android Palette API
- **Concurrency**: Kotlin Coroutines & Flow
- **Architecture**: MVVM (Model-View-ViewModel)

---

## 📸 Screenshots

| Home Library | Now Playing | Interactive Queue |
| :---: | :---: | :---: |
| _Coming Soon_ | _Coming Soon_ | _Coming Soon_ |

---

## 🚀 Getting Started

1. **Clone the repository**:
   ```bash
   git clone https://github.com/yourusername/localify.git
   ```
2. **Open in Android Studio**:
   Make sure you have the latest Arctic Fox or newer.
3. **Run**:
   Ensure you grant the "Music & Audio" permission on your device to see your local files.

---

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

---

Developed with ❤️ by **Priyanshu**
