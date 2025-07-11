# 📡 CellularLab – Advanced iPerf3 Client for Android

[![Abhishek LinkedIn](https://img.shields.io/badge/Abhishek-LinkedIn-blue.svg?style=for-the-badge)](https://www.linkedin.com/in/abhi5h3k/)
[![Abhishek StackOverflow](https://img.shields.io/badge/Abhishek-StackOverflow-orange.svg?style=for-the-badge)](https://stackoverflow.com/users/6870223/abhi?tab=profile)

<img src="https://github.com/user-attachments/assets/11b927ce-dd3e-4dcd-952b-500b35af416d" alt="CellularLab" />


With JNI bindings to native iPerf3, it offers low-level control, intelligent test strategies, and a polished UI — perfect for field diagnostics, automated lab tests, and performance validation on mobile.

---

## 🖼️ UI Preview

<img src="https://github.com/user-attachments/assets/c909b8dd-32d3-4ad9-8612-67249c3dca79" alt="Options" width="80%"/>

> _A fast, native [iPerf3](https://github.com/esnet/iperf) Android client with TCP/UDP/AI log analysis support. Built with JNI + Android Studio._

## 📥 Download APK

Latest signed release: [Download from GitHub Releases →](https://github.com/Abhi5h3k/CellularLab/releases)

> ⚠️ Works on Android 10+ (API 29+).  
> 📁 Logs are saved to your `Downloads/` folder.

---

## 🚀 Features & Capabilities

### ✅ Protocol Support

- 🧪 **TCP**
- 📡 **UDP**
- 📈 **UDP Incremental Ramp-Up**
- 🔄 **Hybrid TCP+UDP**
- 🤖 **Smart Adaptive Ramp-Up**

### ✅ Test Directions

<img src="https://github.com/user-attachments/assets/ea091615-9a5c-4240-bb69-0262166f4e86" alt="Test Direction" width="50%"/>

- 📤 Upload
- 📥 Download (`-R`)
- 🔁 Bidirectional (`--bidir`)

---

## 🔧 Command Mode

Take full control of your testing with **Command Mode** — execute custom iPerf3 commands directly:

- 💻 Enter any `iperf3` command (e.g. `-c 10.0.0.1 -u -b 10M`)
- 🎯 Perfect for advanced users needing custom bandwidth, interval, protocol
- 📊 Full live output visible and saved like regular tests

<img src="https://github.com/user-attachments/assets/6213e256-9d87-4c6b-97c4-a08dea238d1d" alt="Command Mode" width="90%" />

---

## 🤖 Gemini AI Analysis 

Let your assistant do the heavy lifting!

- ✨ Tap "AI Analyze" on any log from **History**
- 📑 Generates a structured **markdown report**:
  - Summary of the test
  - Performance issues (packet loss, jitter, etc.)
  - Recommendations
  - Quality rating (Excellent/Good/Fair/Poor)
- 🪄 Clean, formatted output with headlines, bolds, bullet points
- ⚡ Powered by **Google Gemini Flash**  
- 🔒 Safe — your API key is stored in `local.properties` and not committed

> Great for reports, debugging, and sharing with your network team.

<img src="https://github.com/user-attachments/assets/d35b6a2b-75f8-43f2-97e3-2102e740fee2" alt="Gemini AI" width="50%" />

---

## 🧠 Smart Test Strategies

<img src="https://github.com/user-attachments/assets/5dbbfb7f-d833-40dd-9bd5-ac357f308844" alt="Strategy" width="50%" />

| 🚀 Strategy                | 📝 Description                                                                    |
| -------------------------- | --------------------------------------------------------------------------------- |
| 📈 **Incremental Ramp-Up** | Gradually increases UDP bandwidth to simulate real-time scaling and detect limits |
| 🔄 **Hybrid TCP+UDP**      | Uses TCP to estimate capacity, then runs UDP at that level                        |
| 🧠 **Smart Ramp-Up**       | Increases UDP load only if ≥90% of packets succeed in previous step               |


These are ideal for **automated testing environments** or **dynamic network analysis**.

---

## ⚙️ Additional Test Options

- Set custom durations (e.g., 10s, 60s)
- Configure parallel streams
- Customizable reporting interval
- Enable verbose/debug logging
- Auto-scroll control (double-tap to toggle)
- Wait between iterations

---

## 📁 Output, Logs & History

<img src="https://github.com/user-attachments/assets/a218bded-bfd6-443c-923b-c379f083a7e7" alt="Output" width="70%"/>

- 📉 Real-time iPerf3-style logs (1s interval)
- 📂 Logs saved to `Downloads/` folder
- 🧲 Double-tap log view to toggle auto-scroll
- 📤 Share logs via Mail, WhatsApp, Drive, etc.
- 📄 Clean formatting for easy analysis
- 🤖 AI analysis (v2.0) now supported for detailed summaries!

---

## 🌐 Public iPerf3 Servers

No iPerf server? Use these community-hosted ones:

- 🔗 [iperf.fr](https://iperf.fr/iperf-servers.php)
- 🔗 [R0GGER/public-iperf3-servers](https://github.com/R0GGER/public-iperf3-servers)

> ⚠️ May be unstable or offline depending on maintenance.

---

## 📸 Demo Gallery

### 1. 🚀 First-Time User Guide

New users get an in-app walkthrough of the key screens and controls.

<img src="https://github.com/user-attachments/assets/8086b22c-31ca-43b5-9656-5882324deeb7" alt="Intro Guide" width="30%" />

---

### 2. 🎯 Run a Test

Just set up the parameters (IP, protocol, duration) and tap start.

<img src="https://github.com/user-attachments/assets/251567e1-263b-4751-b5b2-d284336cc0c2" alt="Run Test" width="30%"/>

---

### 3. 📊 History & Log Management

See previous results with clear pass/fail indicators:

- ✅ All tests passed  
- ⚠️ Partial success  
- ❌ Most failed  

You can:
- Tap to open logs
- Share or delete from the UI
- ✨ Analyze with Gemini AI

<img src="https://github.com/user-attachments/assets/9a7efe04-5265-4828-81a0-e15cab9e188a" alt="Result History" width="30%" />

---

### 4. 🤖 AI Assistant

✨ Tap "AI Analyze" on any log from **History**

<img src="https://github.com/user-attachments/assets/39273752-8051-4b61-818d-392a5062a259" alt="NEW AI" width="30%"/>

---

## 🛠️ Development Info

| Component        | Details                              |
|------------------|--------------------------------------|
| **IDE**          | Android Studio `Narwhal 2025.1.1` |
| **NDK Version**  | `28.1.13356709`                      |
| **iPerf Version**| `3.19` (Native C via JNI)            |
| **Min SDK**      | API 29                               |
| **Target SDK**   | API 35                               |
| **ABI Support**  | `armeabi-v7a`, `arm64-v8a`           |
| **Build Types**  | Debug & Signed Release               |

---

## 🛠️ Updating iPerf3 Version

To upgrade to a newer iPerf3 version:

📖 See [`docs/updating-iperf.md`](docs/updating-iperf.md) for detailed steps.

---

## 📚 Articles & Guides

Want to dive deeper into how this app was built or learn how to compile iPerf3 for Android?

- 📖 [How I Accidentally Vibe-Coded an Android iPerf3 App with AI](https://iper3.hashnode.dev/how-i-accidentally-vibe-coded-an-android-iperf3-app-with-ai)
- 🛠️ [Compiling iPerf3 in Android with CMake and JNI – A Practical Guide](https://iper3.hashnode.dev/compiling-iperf3-in-android-with-cmake-and-jni-a-practical-guide)
- 📚 [User Guide](https://hashnode.com/docs/6859436f066f78ab9de2e374/guide/68594370633324afd8ade7b0/version/68594370633324afd8ade7b1/page/6859437da45f4ec3f9f8a0c6)

Follow me on Hashnode for more deep dives:  
🌐 [@AbhishekIN on Hashnode](https://hashnode.com/@AbhishekIN)

---
## 📜 License

This project is open-source under a **custom MIT-style license**:

- ✅ Personal and non-commercial use allowed
- ❌ Uploading to Play Store or commercial use **requires permission**

If you build on this, please give credit with a link to the original repo:

🔗 [https://github.com/Abhi5h3k/CellularLab](https://github.com/Abhi5h3k/CellularLab)
