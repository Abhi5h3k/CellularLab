## 🛠️ How to Update iPerf3 Source in This Project

To upgrade to a newer iPerf3 version in this app, follow these steps:

1. **Download Latest iPerf3 Source** 
 
   - Visit: [https://github.com/esnet/iperf/releases](https://github.com/esnet/iperf/releases)  
   - Download the latest source archive (e.g. `iperf-3.20.zip` or `iperf-3.20.tar.gz`)

2. **Extract and Copy Files**  

   - Extract the archive  
   - Copy all `.c` and `.h` files from the `src/` directory  
   - Paste them into a new versioned folder in your project, for example:  

     ```
     app/src/main/cpp/iperf/iperf-3.20/
     ```

3. **Update Build Configuration**  

   - Edit `CMakeLists.txt`  
   - Update the iPerf source directory path:

     ```cmake
     set(IPERF_SRC_DIR ${CMAKE_SOURCE_DIR}/iperf/iperf-3.20)
     ```

   - Update the version number:

     ![PACKAGE_VERSION](https://github.com/user-attachments/assets/97972f81-d803-4ea6-9512-00c89438c3cf)


     ```cmake
     set(PACKAGE_VERSION "3.20")
     ```

4. **Clean and Rebuild the Project**  

   - In Android Studio, go to: **Build > Clean Project**  
   - Or run the following command:

     ```
     ./gradlew clean
     ```

   - This will regenerate `iperf_config.h` and `version.h` automatically using the updated version.

5. ✅ **Done!** You're now using the latest iPerf3 version natively in your Android app.

---
