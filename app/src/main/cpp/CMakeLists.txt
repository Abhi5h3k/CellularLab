cmake_minimum_required(VERSION 3.4.1)
set(IPERF_SOURCE_DIR ${CMAKE_SOURCE_DIR}/iperf)
project("cellularlab")
add_definitions(-DHAVE_PTHREAD)

if (ANDROID)
    add_definitions(-D__ANDROID__)
endif()

add_library(cellularlab SHARED
        # Add your pthread workaround files
        ${IPERF_SOURCE_DIR}/iperf_pthread.c
        ${IPERF_SOURCE_DIR}/iperf_pthread.h
        # Add JNI wrapper
        ${IPERF_SOURCE_DIR}/iperf_jni.c
        # Provides a relative path to your source file(s).
        ${IPERF_SOURCE_DIR}/main.c
        ${IPERF_SOURCE_DIR}/iperf_api.c
        ${IPERF_SOURCE_DIR}/iperf_auth.c
        ${IPERF_SOURCE_DIR}/iperf_client_api.c
        ${IPERF_SOURCE_DIR}/iperf_error.c
        ${IPERF_SOURCE_DIR}/iperf_locale.c
        ${IPERF_SOURCE_DIR}/iperf_sctp.c
        ${IPERF_SOURCE_DIR}/iperf_server_api.c
        ${IPERF_SOURCE_DIR}/iperf_tcp.c
        ${IPERF_SOURCE_DIR}/iperf_time.c
        ${IPERF_SOURCE_DIR}/iperf_udp.c
        ${IPERF_SOURCE_DIR}/iperf_util.c
        ${IPERF_SOURCE_DIR}/net.c
        ${IPERF_SOURCE_DIR}/tcp_info.c
        ${IPERF_SOURCE_DIR}/timer.c
        ${IPERF_SOURCE_DIR}/units.c
        ${IPERF_SOURCE_DIR}/dscp.c
        ${IPERF_SOURCE_DIR}/cjson.c
)

target_include_directories(cellularlab PRIVATE ${CMAKE_SOURCE_DIR})

find_package(Threads REQUIRED)

target_link_libraries(cellularlab
        Threads::Threads
        log
        android
)
