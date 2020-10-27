#pragma once

#include "osdefine.h"

#ifdef LIMAX_OS_WINDOWS

namespace limax {
	namespace helper {

		inline uint16_t Make16bits(uint8_t l, uint8_t h)
		{
			return (uint16_t)((uint8_t)l) | (((uint16_t)(uint8_t)h) << 8);
		}
		inline uint8_t Lo8bits(uint16_t v)
		{
			return (uint8_t)(v & 0xff);
		}
		inline uint8_t Hi8bits(uint16_t v)
		{
			return (uint8_t)(v >> 8);
		}

		inline uint32_t Make32bits(uint16_t l, uint16_t h)
		{
			return (uint32_t)((uint16_t)(l & 0xffff)) | (((uint32_t)((uint16_t)(h & 0xffff))) << 16);
		}
		inline uint16_t Lo16bits(uint32_t v)
		{
			return (uint16_t)(v & 0xffff);
		}
		inline uint16_t Hi16bits(uint32_t v)
		{
			return (uint16_t)(v >> 16);
		}

		inline uint64_t Make64bits(uint32_t l, uint32_t h)
		{
			return (uint64_t)((uint32_t)(l & 0xffffffff)) | (((uint64_t)((uint32_t)(h & 0xffffffff))) << 32);
		}
		inline uint32_t Lo32bits(uint64_t v)
		{
			return (uint32_t)(v & 0xffffffff);
		}
		inline uint32_t Hi32bits(uint64_t v)
		{
			return (uint32_t)(v >> 32);
		}

	} // namespace helper {

	inline uint16_t htole16(uint16_t v)
	{
		return v;
	}
	inline uint16_t htobe16(uint16_t v)
	{
		uint8_t lo = helper::Lo8bits(v);
		uint8_t hi = helper::Hi8bits(v);
		return helper::Make16bits(hi, lo);
	}
	inline uint16_t le16toh(uint16_t v)
	{
		return v;
	}
	inline uint16_t be16toh(uint16_t v)
	{
		return htobe16(v);
	}

	inline uint32_t htole32(uint32_t v)
	{
		return v;
	}
	inline uint32_t htobe32(uint32_t v)
	{
		uint16_t lo = helper::Lo16bits(v);
		uint16_t hi = helper::Hi16bits(v);
		return helper::Make32bits(htobe16(hi), htobe16(lo));
	}
	inline uint32_t le32toh(uint32_t v)
	{
		return v;
	}
	inline uint32_t be32toh(uint32_t v)
	{
		return htobe32(v);
	}

	inline uint64_t htole64(uint64_t v)
	{
		return v;
	}
	inline uint64_t htobe64(uint64_t v)
	{
		uint32_t lo = helper::Lo32bits(v);
		uint32_t hi = helper::Hi32bits(v);
		return helper::Make64bits(htobe32(hi), htobe32(lo));
	}
	inline uint64_t le64toh(uint64_t v)
	{
		return v;
	}
	inline uint64_t be64toh(uint64_t v)
	{
		return htobe64(v);
	}

} // namespace limax {

#endif // #ifdef LIMAX_OS_WINDOWS

//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

#ifdef LIMAX_OS_LINUX

#include <endian.h>

#endif // #ifdef LIMAX_OS_LINUX

//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

#ifdef LIMAX_OS_ANDROID

#include <endian.h>

namespace limax {

#ifndef be16toh
	inline uint16_t be16toh(uint16_t x)
	{
		return betoh16(x);
	}
#endif

#ifndef be32toh
	inline uint32_t be32toh(uint32_t x)
	{
		return betoh32(x);
	}
#endif

#ifndef be64toh
	inline uint64_t be64toh(uint64_t x)
	{
		return betoh64(x);
	}
#endif

} // namespace limax {

#endif //#ifdef LIMAX_OS_ANDROID

//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

#ifdef LIMAX_OS_APPLE_FAMILY

#include <libkern/OSByteOrder.h>

namespace limax {

	inline uint16_t htobe16(uint16_t x)
	{
		return OSSwapHostToBigInt16(x);
	}
	inline uint16_t htole16(uint16_t x)
	{
		return OSSwapHostToLittleInt16(x);
	}
	inline uint16_t be16toh(uint16_t x)
	{
		return OSSwapBigToHostInt16(x);
	}
	inline uint16_t le16toh(uint16_t x)
	{
		return OSSwapLittleToHostInt16(x);
	}

	inline uint32_t htobe32(uint32_t x)
	{
		return OSSwapHostToBigInt32(x);
	}
	inline uint32_t htole32(uint32_t x)
	{
		return OSSwapHostToLittleInt32(x);
	}
	inline uint32_t be32toh(uint32_t x)
	{
		return OSSwapBigToHostInt32(x);
	}
	inline uint32_t le32toh(uint32_t x)
	{
		return OSSwapLittleToHostInt32(x);
	}

	inline uint64_t htobe64(uint64_t x)
	{
		return OSSwapHostToBigInt64(x);
	}
	inline uint64_t htole64(uint64_t x)
	{
		return OSSwapHostToLittleInt64(x);
	}
	inline uint64_t be64toh(uint64_t x)
	{
		return OSSwapBigToHostInt64(x);
	}
	inline uint64_t le64toh(uint64_t x)
	{
		return OSSwapLittleToHostInt64(x);
	}

} // namespace limax {

#endif // #ifdef LIMAX_OS_APPLE_FAMILY

