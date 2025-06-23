# 📡 CellularLab – Advanced iPerf3 Client for Android

[![Abhishek LinkedIn](https://img.shields.io/badge/Abhishek-LinkedIn-blue.svg?style=for-the-badge)](https://www.linkedin.com/in/abhi5h3k/)
[![Abhishek StackOverflow](https://img.shields.io/badge/Abhishek-StackOverflow-orange.svg?style=for-the-badge)](https://stackoverflow.com/users/6870223/abhi?tab=profile)


 <img src="https://github.com/user-attachments/assets/c909b8dd-32d3-4ad9-8612-67249c3dca79" alt="Options" width="80%" height="80%"/>


**CellularLab** is a native Android app that serves as a smart frontend for [iPerf3](https://github.com/esnet/iperf), the de facto standard for TCP/UDP performance testing. Built with `JNI` and powered by a native integration of `iperf 3`, it delivers precise, low-level control and accurate results — all from your Android device.

 
 
---

## 📦 About iPerf3

[iPerf3](https://github.com/esnet/iperf) is an open-source command-line tool by ESnet used to:
- Measure TCP and UDP bandwidth
- Diagnose jitter, packet loss, and latency

CellularLab integrates iPerf3 natively to offer these features with an Android-native interface and intelligent test logic.

---

## 🔧 Project Details


<img src="https://github.com/user-attachments/assets/264b5378-1fa7-43a3-98db-fbf13313b47f" alt="Cellular Lab logo" width="20%" height="20%"/>

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

---

## 🧠 Strategy Highlights

 <img src="https://github.com/user-attachments/assets/5dbbfb7f-d833-40dd-9bd5-ac357f308844" alt="Stratergy" width="70%" height="70%"/>

| Strategy         | Description |
|------------------|-------------|
| **Incremental Ramp-Up** | Gradually increases UDP bandwidth to detect network limits and simulate real-time scaling. |
| **Hybrid TCP+UDP** | Uses a TCP test to estimate max capacity, then runs UDP at that level for comparison. |
| **Smart Ramp-Up** | Adaptive logic increases UDP load only if ≥90% success was achieved in the last step. Prevents unstable spikes. |

Ideal for automated or lab-based network performance profiling.

---


### ✅ Test Directions

<img src="https://github.com/user-attachments/assets/ea091615-9a5c-4240-bb69-0262166f4e86" alt="Test Direction" width="70%" height="70%"/>

- 📤 **Upload**
- 📥 **Download (`-R`)**
- 🔁 **Bidirectional (`--bidir`)**

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

<img src="https://github.com/user-attachments/assets/a218bded-bfd6-443c-923b-c379f083a7e7" alt="Output" width="70%" height="70%"/>


- 🔴 Live iPerf3-style logs (per second)
- 🧲 **Double-tap the log area to toggle auto-scroll** (on/off)
  
  <img src="https://github.com/user-attachments/assets/2db46025-3daf-4746-9ecc-98a1875ba07a" alt="Output Dir" width="400" height="800"/>

- 📂 Saved automatically to `Downloads/`
- 🗂️ Share logs via Android UI (e.g. mail, WhatsApp, Drive)
- 📄 Clean, readable formatting

---

### 🌐 Public iPerf3 Servers

You can use public iPerf3 servers listed here to test this app without setting up your own server:

➡️ [R0GGER/public-iperf3-servers](https://github.com/R0GGER/public-iperf3-servers)

**Note**: These are community-maintained and may be unstable or offline at times.

---

## License

This project is open source under a custom MIT-style license.  
You are free to use, modify, and share it for personal and non-commercial purposes.

❗️However, uploading this app or its modified versions to any app store (e.g., Google Play)  
or using it commercially is **not allowed** without written permission.

💡 If you build something based on this, a simple mention or link to the original repo would be appreciated:
[https://github.com/Abhi5h3k/CellularLab](https://github.com/Abhi5h3k/CellularLab)

---

