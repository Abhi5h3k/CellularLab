# 📡 CellularLab – Advanced iPerf3 Client for Android

[![Abhishek LinkedIn](https://img.shields.io/badge/Abhishek-LinkedIn-blue.svg?style=for-the-badge)](https://www.linkedin.com/in/abhi5h3k/)
[![Abhishek StackOverflow](https://img.shields.io/badge/Abhishek-StackOverflow-orange.svg?style=for-the-badge)](https://stackoverflow.com/users/6870223/abhi?tab=profile)

![Options](https://github.com/user-attachments/assets/15cf417a-8118-48a8-bd9e-9bbccd0d2f39)


**CellularLab** is a native Android app that serves as a smart frontend for [iPerf3](https://github.com/esnet/iperf), the de facto standard for TCP/UDP performance testing. Built with `JNI` and powered by a native integration of `iperf 3`, it delivers precise, low-level control and accurate results — all from your Android device.

---

## 📦 About iPerf3

[iPerf3](https://github.com/esnet/iperf) is an open-source command-line tool by ESnet used to:
- Measure TCP and UDP bandwidth
- Diagnose jitter, packet loss, and latency

CellularLab integrates iPerf3 natively to offer these features with an Android-native interface and intelligent test logic.

---

## 🔧 Project Details

| Key Component | Details |
|---------------|---------|
| **IDE** | Android Studio `Meerkat | 2024.3.2 Patch 1` |
| **NDK Version** | `28.1.13356709` |
| **iPerf Version** | `3.19` (Native C integration via JNI) |
| **Min SDK** | API 29 |
| **Target SDK** | API 35 |
| **ABI Support** | `armeabi-v7a`, `arm64-v8a` |
| **Build Types** | Debug & Signed Release |

---

## 🚀 Features & Capabilities

### ✅ Protocol Support
- 🧪 **TCP**
- 📡 **UDP**
- 📈 **UDP Incremental Ramp-Up**
- 🔄 **Hybrid TCP+UDP Strategy**
- 🤖 **Smart Ramp-Up Strategy**

### ✅ Test Directions
- 📤 **Upload**
- 📥 **Download (`-R`)**
- 🔁 **Bidirectional (`--bidir`)**

---

## 🧠 Strategy Highlights

| Strategy         | Description |
|------------------|-------------|
| **Incremental Ramp-Up** | Gradually increases UDP bandwidth to detect network limits and simulate real-time scaling. |
| **Hybrid TCP+UDP** | Uses a TCP test to estimate max capacity, then runs UDP at that level for comparison. |
| **Smart Ramp-Up** | Adaptive logic increases UDP load only if ≥90% success was achieved in the last step. Prevents unstable spikes. |

Ideal for automated or lab-based network performance profiling.

---

## 🧪 Additional Test Options

- Set custom test durations (e.g. 10s, 60s)
- Choose number of parallel streams
- Configure report interval (e.g. 1s)
- Enable debug/verbose output
- Auto-scroll logs
- Wait between iterations

---

## 📁 Output

![Auto scroll](https://github.com/user-attachments/assets/a218bded-bfd6-443c-923b-c379f083a7e7)


- 🔴 Live iPerf3-style logs (per second)
- 🧲 **Double-tap the log area to toggle auto-scroll** (on/off)
  
  <img src="https://github.com/user-attachments/assets/2db46025-3daf-4746-9ecc-98a1875ba07a" alt="Output Dir" width="400" height="800"/>

- 📂 Saved automatically to `Downloads/`
- 🗂️ Share logs via Android UI (e.g. mail, WhatsApp, Drive)
- 📄 Clean, readable formatting

---
