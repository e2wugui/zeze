#pragma once

#ifdef _MSC_VER

#define LIMAX_OS_WINDOWS

#elif defined( ANDROID)

#define LIMAX_OS_UNIX_FAMILY
#define LIMAX_OS_ANDROID
#define LIMAX_OS_MOBILE

#ifndef NDEBUG
#define LIMAX_DEBUG
#endif 

#elif defined( __APPLE__)

#include <TargetConditionals.h>

#define LIMAX_OS_UNIX_FAMILY
#define LIMAX_OS_APPLE_FAMILY

#if TARGET_IPHONE_SIMULATOR
#define LIMAX_OS_IPHONE_SIMULATOR
#define LIMAX_OS_MOBILE
#elif TARGET_OS_IPHONE
#define LIMAX_OS_IPHONE
#define LIMAX_OS_MOBILE
#elif TARGET_OS_MAC
#define LIMAX_OS_MAC
#endif

#elif defined( __linux__ )

#define LIMAX_OS_UNIX_FAMILY
#define LIMAX_OS_LINUX

#endif

//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

#ifdef LIMAX_OS_WINDOWS
#ifndef _WIN32_WINNT
#define _WIN32_WINNT	_WIN32_WINNT_VISTA
#endif

#ifdef _DEBUG
#define LIMAX_DEBUG
#ifndef LIMAX_VS_DEBUG_NO_MEMORY_LEAKS_DETECT
#define LIMAX_VS_DEBUG_MEMORY_LEAKS_DETECT
#define _CRTDBG_MAP_ALLOC 
#endif
#endif

#include <ws2tcpip.h>


#ifdef _M_X64
#	define LIMAX_PLAT_X86_64
#	define LIMAX_PLAT_64
#elif defined( _M_IX86)
#	define LIMAX_PLAT_I686
#	define LIMAX_PLAT_32
#else
#	pragma warning("unknow cpu type")
#endif

#define LIMAX_PLAT_INTEL

#endif // #ifdef LIMAX_OS_WINDOWS

//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

#ifdef LIMAX_OS_UNIX_FAMILY

#include <unistd.h>
#include <sys/socket.h>
#include <arpa/inet.h>
#include <errno.h>

#if __i386__
#	define LIMAX_PLAT_32
#	define LIMAX_PLAT_I686
#	define LIMAX_PLAT_INTEL
#elif __x86_64__
#	define LIMAX_PLAT_64
#	define LIMAX_PLAT_X86_64
#	define LIMAX_PLAT_INTEL
#elif __arm64__
#	define LIMAX_PLAT_64
#	define LIMAX_PLAT_ARM
#elif __aarch64__
#	define LIMAX_PLAT_64
#	define LIMAX_PLAT_ARM
#elif __mips64
#	define LIMAX_PLAT_64
#	define LIMAX_PLAT_MIPS
#elif __mips__
#	define LIMAX_PLAT_32
#	define LIMAX_PLAT_MIPS
#elif __arm__
#	define LIMAX_PLAT_32
#	define LIMAX_PLAT_ARM
#else
#	pragma warning("unknow cpu type")
#endif

#endif // #ifdef LIMAX_OS_UNIX_FAMILY

//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

#ifdef LIMAX_OS_APPLE_FAMILY

#include <pthread.h>

#endif // #ifdef LIMAX_OS_APPLE_FAMILY

//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

#if defined( LIMAX_OS_WINDOWS) && defined( LIMAX_PLAT_X86_64)
#define SUGGEST_TRY_USE_CPU_AES
#endif // #ifdef LIMAX_OS_WINDOWS

#if defined( LIMAX_OS_APPLE_FAMILY) && defined(LIMAX_PLAT_INTEL)
#define SUGGEST_TRY_USE_CPU_AES
#endif // #ifdef LIMAX_OS_MAC

#ifdef LIMAX_OS_LINUX
#define SUGGEST_TRY_USE_CPU_AES
#endif //#ifdef LIMAX_OS_LINUX

#if defined( SUGGEST_TRY_USE_CPU_AES ) && !defined( LIMAX_NOT_USE_CPU_AES ) && !defined( LIMAX_TRY_USE_CPU_AES)
#	define LIMAX_TRY_USE_CPU_AES
#endif

#ifdef LIMAX_TRY_USE_CPU_AES
#	ifdef LIMAX_OS_WINDOWS
#		define LIMAX_ALIGN( x )  __declspec( align( x ) )
#	else
#		define LIMAX_ALIGN( x ) alignas( x)
#	endif
#else
#	define LIMAX_ALIGN( x )
#endif // #ifndef LIMAX_TRY_USE_CPU_AES
