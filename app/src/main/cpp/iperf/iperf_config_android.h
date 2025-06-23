/* Custom iperf_config.h for Android (JNI build) */
/* Safe minimal config that works with Android NDK */

// System/Time features
#define HAVE_CLOCK_GETTIME 1              // Used for precise timekeeping
#undef HAVE_CLOCK_NANOSLEEP              // Not available in Android NDK
#undef HAVE_CPUSET_SETAFFINITY           // Not used; Linux-specific
#define HAVE_CPU_AFFINITY 1              // Enables CPU affinity support (via sched)

// POSIX / System Functions
#undef HAVE_DAEMON                       // Not available in Android (used for backgrounding)
#undef HAVE_DONT_FRAGMENT                // Linux-specific socket option
#define HAVE_DLFCN_H 1                   // Required for dynamic linking (e.g., dlsym)
#define HAVE_ENDIAN_H 1                  // Provides byte-order functions
#undef HAVE_FLOWLABEL                    // IPv6 flow label support (not needed)
#define HAVE_GETLINE 1                   // Used to read lines from config/stdin
#define HAVE_INTTYPES_H 1                // Required for int64_t, uint32_t, etc.
#undef HAVE_IPPROTO_MPTCP                // MPTCP not supported in Android
#undef HAVE_IP_DONTFRAG                  // Linux socket option
#undef HAVE_IP_DONTFRAGMENT              // Linux socket option
#undef HAVE_IP_MTU_DISCOVER              // Linux-specific feature
#undef HAVE_LINUX_TCP_H                  // Linux-only TCP options
#undef HAVE_MSG_TRUNC                    // Message truncation support (recv)
#define HAVE_NANOSLEEP 1                 // For sub-second sleep

// Networking Protocol Support
#undef HAVE_NETINET_SCTP_H              // SCTP not supported on Android
#define HAVE_POLL_H 1                   // Polling support for I/O
#define HAVE_PTHREAD 1                  // Android supports POSIX threads
#undef HAVE_PTHREAD_PRIO_INHERIT        // Not always present in Android NDK
#undef HAVE_SCHED_SETAFFINITY           // Optional; for setting thread affinity
#undef HAVE_SCTP_H                      // No SCTP protocol on Android
#undef HAVE_SENDFILE                    // `sendfile()` not supported in NDK
#undef HAVE_SETPROCESSAFFINITYMASK      // Windows-only
#undef HAVE_SO_BINDTODEVICE             // Not supported in Android user space
#define HAVE_SO_MAX_PACING_RATE 1       // Controls pacing rate (useful on Android ≥ Q)
#undef HAVE_SSL                         // Requires OpenSSL — disabled by default
#undef HAVE_STDATOMIC_H                // Android NDK might lack full support
#define HAVE_STDINT_H 1                 // Standard integer types
#define HAVE_STDIO_H 1                  // Standard C I/O
#define HAVE_STDLIB_H 1                 // Memory, process, conversions
#define HAVE_STRINGS_H 1                // `bzero`, `strcasecmp`, etc.
#define HAVE_STRING_H 1                 // String functions like `strcmp`

// SCTP-specific types
#undef HAVE_STRUCT_SCTP_ASSOC_VALUE    // Not relevant for Android

// More system headers
#undef HAVE_SYS_ENDIAN_H               // BSD-specific
#define HAVE_SYS_SOCKET_H 1            // Basic networking
#define HAVE_SYS_STAT_H 1              // File stats
#define HAVE_SYS_TYPES_H 1             // System data types

// TCP options
#define HAVE_TCP_CONGESTION 1          // TCP congestion control (Android ≥ Q)
#undef HAVE_TCP_INFO_SND_WND          // Rare kernel struct field
#undef HAVE_TCP_KEEPALIVE             // Keepalive options (define if used)
#undef HAVE_TCP_USER_TIMEOUT          // Not usually supported in Android

// UNIX header
#define HAVE_UNISTD_H 1                // Standard POSIX APIs (close, read, etc.)

// Required for libtool-style builds
#define LT_OBJDIR ".libs/"

// Package metadata (update version as needed)
#define PACKAGE "iperf"
#define PACKAGE_BUGREPORT "https://github.com/esnet/iperf"
#define PACKAGE_NAME "iperf"
#define PACKAGE_STRING "iperf 3.19"
#define PACKAGE_TARNAME "iperf"
#define PACKAGE_URL "https://software.es.net/iperf/"
#define PACKAGE_VERSION "3.19"
#define VERSION "3.19"
// ──────────────────────────────
// 🔁 SECTION TO UPDATE ON IPERF VERSION UPGRADE
// (Update version strings when pulling new iperf source)
// ──────────────────────────────
#define PACKAGE_STRING "iperf @PACKAGE_VERSION@"     // ⬅️ Auto Update
#define PACKAGE_VERSION "@PACKAGE_VERSION@"          // ⬅️ Auto Update
#define VERSION "@PACKAGE_VERSION@"                  // ⬅️ Auto Update

#define STDC_HEADERS 1                // Define if ANSI C headers are available

/* #undef const */                    // Do not touch — reserved for legacy platforms
