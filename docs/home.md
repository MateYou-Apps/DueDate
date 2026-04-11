<p align="center">
  <strong>Smart Bill Tracking. 100% Offline.</strong><br>
  Automated bill detection from SMS with a modern Material You interface.
</p>

<br>


<p align="center">
  <a href="https://github.com/MateYou-Apps/DueDate/releases/latest/download/DueDate.apk" style="display: inline-flex; align-items: center; gap: 10px; padding: 12px 32px; background-color: #AAEEAA; color: #002106; border-radius: 30px; text-decoration: none; font-weight: 700; margin: 20px 0; box-shadow: 0 4px 12px rgba(0,0,0,0.1);">
    <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"/><polyline points="7 10 12 15 17 10"/><line x1="12" y1="15" x2="12" y2="3"/></svg>
    Download Latest APK
  </a>
</p>

<br>

<p align="center">
  <img src="assets/screenshots/0.png" width="23%" style="border-radius:12px; margin: 1px;">
  <img src="assets/screenshots/3.png" width="23%" style="border-radius:12px; margin: 1px;">
  <img src="assets/screenshots/4.png" width="23%" style="border-radius:12px; margin: 1px;">
  <img src="assets/screenshots/5.png" width="23%" style="border-radius:12px; margin: 1px;">
  <img src="assets/screenshots/6.png" width="23%" style="border-radius:12px; margin: 1px;">
  <img src="assets/screenshots/7.png" width="23%" style="border-radius:12px; margin: 1px;">
  <img src="assets/screenshots/8.png" width="23%" style="border-radius:12px; margin: 1px;">
  <img src="assets/screenshots/9.png" width="23%" style="border-radius:12px; margin: 1px;">
</p>

## Core Features

DueDate is designed to simplify your financial life by automating credit card bill tracking without compromising your privacy.

### ⚡ Smart Automation
- **Auto-Detection:** Effortlessly identifies credit card bills from bank SMS alerts with **global currency support**.
- **Custom Templates:** Unsupported bank sms? Create your own parsing rules using an intuitive visual configuration tool.
- **Partial Payments:** Track partial payments on your bills. Log them without moving the bill to 'Paid' until it's fully settled.
- **Smart Status:** Instantly see which bills are due, late, or paid at a glance with color-coded indicators.

### 📊 Visualize & Remind
- **Concise Calendar:** A beautiful, integrated calendar view to see your upcoming financial commitments for any month.
- **Detailed History:** Monitor spending habits with statement history and interactive spending graphs.
- **Enhanced Reminders:** Set custom notification schedules (5 days, 1 day, and same-day alerts) to ensure you never pay a late fee again.
- **Interactive Widgets:** Keep track of your most urgent bills directly from your home screen.

### 🛡️ Private & Reliable
- **100% Offline:** Zero internet permissions. Your data is parsed and stored locally - never leaving your device.
- **Biometric Security:** Protect your sensitive financial information with an optional app lock using Fingerprint.
- **Material You:** Fully supports dynamic theming. The app adapts to your wallpaper for a personalized aesthetic.
- **Portable Backups:** Export and import backups of your bills, banks, and custom configurations.

<br>

## Tech Stack

Built with modern Android standards for performance and longevity.

### Architecture & UI
- **Language:** [Kotlin](https://kotlinlang.org/)
- **UI Toolkit:** [Jetpack Compose](https://developer.android.com/jetpack/compose) (Material 3)
- **Architecture:** MVVM (Model-View-ViewModel)
- **Background Tasks:** [WorkManager](https://developer.android.com/topic/libraries/architecture/workmanager) for reliable notifications.

### Data & Integration
- **Local Database:** [Room](https://developer.android.com/training/data-storage/room) (SQLite)
- **Home Screen Widgets:** [Jetpack Glance](https://developer.android.com/jetpack/compose/glance)
- **SVG Rendering:** [AndroidSVG](https://bigbadaboom.github.io/androidsvg/)
- **Serialization:** [Gson](https://github.com/google/gson)

## Building Locally
To build DueDate on your machine:

### 1. Prerequisites
- **Android Studio:** Ladybug (2024.2.1) or newer.
- **JDK:** Java 21 toolchain.

### 2. Clone the Repository
```bash
git clone https://github.com/MateYou-Apps/DueDate.git
cd DueDate
```

### 3. Build & Run
1. Open the project in Android Studio.
2. Sync Gradle and click **Run** to deploy to your device.

## License
Licensed under the [GNU General Public License v3.0](LICENSE).
