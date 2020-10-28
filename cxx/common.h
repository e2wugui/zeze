#pragma once

//#include "../include/osbase.h"
//#include "../include/netbase.h"
//#include "netinterface.h"
#include "osdefine.h"

//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

#ifdef LIMAX_OS_WINDOWS
#include <stdlib.h> 
#include <crtdbg.h> 

#undef max
#undef min
#include <xutility>

#endif // #ifdef LIMAX_OS_WINDOWS

//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

#ifdef LIMAX_OS_LINUX
#include <sys/time.h>
#include <netinet/tcp.h>
#include <signal.h>
#include <fcntl.h>
#include <limits.h>
#include <stddef.h>
#endif // #ifdef LIMAX_OS_LINUX

//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

#ifdef LIMAX_OS_ANDROID
#include <errno.h>
#include <fcntl.h>
#include <sys/epoll.h>
#include <arpa/inet.h>
#include <netinet/tcp.h>
#endif // #ifdef LIMAX_OS_ANDROID

//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

#ifdef LIMAX_OS_APPLE_FAMILY
#include <arpa/inet.h>
#include <netinet/tcp.h>
#include <sys/time.h>
#include <fcntl.h>
#include <signal.h>
#include <libkern/OSAtomic.h>
#endif // #ifdef LIMAX_OS_APPLE_FAMILY

//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

#include <assert.h>
#include <time.h>
#include <string.h>

#include <sstream>
#include <iostream>
#include <algorithm>

#define LIMAX_DLL_EXPORT_API

//#include "byteorder.h"
//#include "osstr.h"
