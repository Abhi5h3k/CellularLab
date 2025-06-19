// Add this to iperf.h or create iperf_fd_safe.h
#ifndef IPERF_FD_SAFE_H
#define IPERF_FD_SAFE_H

#include <sys/select.h>
#include <errno.h>

// Safe FD_SET wrapper that checks bounds
#define SAFE_FD_SET(fd, set) do { \
    if ((fd) >= 0 && (fd) < FD_SETSIZE) { \
        FD_SET((fd), (set)); \
    } else { \
        errno = EINVAL; \
        return -1; \
    } \
} while(0)

// Safe FD_CLR wrapper
#define SAFE_FD_CLR(fd, set) do { \
    if ((fd) >= 0 && (fd) < FD_SETSIZE) { \
        FD_CLR((fd), (set)); \
    } \
} while(0)

// Safe FD_ISSET wrapper
#define SAFE_FD_ISSET(fd, set) \
    (((fd) >= 0 && (fd) < FD_SETSIZE) ? FD_ISSET((fd), (set)) : 0)

#endif