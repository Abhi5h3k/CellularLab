# 📡 CellularLab – Advanced iPerf3 Client for Android

[![Abhishek LinkedIn](https://img.shields.io/badge/Abhishek-LinkedIn-blue.svg?style=for-the-badge)](https://www.linkedin.com/in/abhi5h3k/)
[![Abhishek StackOverflow](https://img.shields.io/badge/Abhishek-StackOverflow-orange.svg?style=for-the-badge)](https://stackoverflow.com/users/6870223/abhi?tab=profile)

**CellularLab** is a powerful Android app that acts as a native frontend for [iPerf3](https://github.com/esnet/iperf), the gold standard for TCP/UDP performance testing.

With JNI bindings to native iPerf3, it offers low-level control, intelligent test strategies, and a polished UI — perfect for field diagnostics, automated lab tests, and performance validation on mobile.

---

## 🖼️ UI Preview

<img src="https://github.com/user-attachments/assets/c909b8dd-32d3-4ad9-8612-67249c3dca79" alt="Options" width="80%" height="80%"/>

---

## 🚀 Features & Capabilities

### ✅ Protocol Support

- 🧪 **TCP**
- 📡 **UDP**
- 📈 **UDP Incremental Ramp-Up**
- 🔄 **Hybrid TCP+UDP**
- 🤖 **Smart Adaptive Ramp-Up**

### ✅ Test Directions

<img src="https://github.com/user-attachments/assets/ea091615-9a5c-4240-bb69-0262166f4e86" alt="Test Direction" width="50%" height="50%"/>

- 📤 Upload
- 📥 Download (`-R`)
- 🔁 Bidirectional (`--bidir`)

---

## 🧠 Smart Test Strategies

<img src="https://github.com/user-attachments/assets/5dbbfb7f-d833-40dd-9bd5-ac357f308844" alt="Strategy" width="50%" height="50%"/>

| Strategy             | Description |
|----------------------|-------------|
| **Incremental Ramp-Up** | Gradually increases UDP bandwidth to simulate real-time scaling and detect limits |
| **Hybrid TCP+UDP**   | Uses TCP to estimate capacity, then runs UDP at that level |
| **Smart Ramp-Up**    | Increases UDP load only if ≥90% of packets succeed in previous step |

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

<img src="https://github.com/user-attachments/assets/a218bded-bfd6-443c-923b-c379f083a7e7" alt="Output" width="70%" height="70%"/>

- 📉 Real-time iPerf3-style logs (1s interval)
- 📂 Logs saved to `Downloads/` folder
- 🧲 Double-tap log view to toggle auto-scroll
- 📤 Share logs via Mail, WhatsApp, Drive, etc.
- 📄 Clean formatting for easy analysis

<img src="https://github.com/user-attachments/assets/2db46025-3daf-4746-9ecc-98a1875ba07a" alt="Output Dir" width="400" height="800"/>

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

<img src="https://github.com/user-attachments/assets/8086b22c-31ca-43b5-9656-5882324deeb7" alt="Intro Guide" width="30%" height="30%"/>

---

### 2. 🎯 Run a Test

Just set up the parameters (IP, protocol, duration) and tap start.

<img src="https://github.com/user-attachments/assets/251567e1-263b-4751-b5b2-d284336cc0c2" alt="Run Test" width="30%" height="30%"/>

---

### 3. 📊 History & Log Management

See previous results with clear pass/fail indicators:

- ✅ All tests passed  
- ⚠️ Partial success  
- ❌ Most failed  

You can:
- Tap to open logs
- Share or delete from the UI

<img src="https://github.com/user-attachments/assets/9a7efe04-5265-4828-81a0-e15cab9e188a" alt="Result History" width="30%" height="30%"/>

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

## 📜 License

This project is open-source under a **custom MIT-style license**:

- ✅ Personal and non-commercial use allowed
- ❌ Uploading to Play Store or commercial use **requires permission**

If you build on this, please give credit with a link to the original repo:

🔗 [https://github.com/Abhi5h3k/CellularLab](https://github.com/Abhi5h3k/CellularLab)

---
