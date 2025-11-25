#include "security.h"
#include "byteorder.h"

namespace limax {

	LIMAX_DLL_EXPORT_API Octets md5digest(const void* input, size_t size)
	{
		MD5 md5;
		md5.update((int8_t*)input, 0, (int32_t)size);
		return Octets(md5.digest(), 16);
	}

	LIMAX_DLL_EXPORT_API Octets sha1digest(const void* input, size_t size)
	{
		SHA1 sha;
		sha.update((int8_t*)input, 0, (int32_t)size);
		return Octets(sha.digest(), 20);
	}

	LIMAX_DLL_EXPORT_API Octets sha256digest(const void* input, size_t size)
	{
		SHA256 sha;
		sha.update((int8_t*)input, 0, (int32_t)size);
		return Octets(sha.digest(), 32);
	}
} // namespace limax {

namespace limax {

	void MD5::reset() {
		ctx.state[0] = 0x67452301;
		ctx.state[1] = 0xefcdab89;
		ctx.state[2] = 0x98badcfe;
		ctx.state[3] = 0x10325476;
		ctx.count = 0;
		ctx.remain = 0;
	}

	MD5::MD5(std::shared_ptr<Codec> _sink) : sink(_sink)
	{
		reset();
	}

	MD5::MD5() : sink(Codec::Null())
	{
		reset();
	}

	inline static uint32_t F(uint32_t x, uint32_t y, uint32_t z) {
		return z ^ (x & (y ^ z));
	}

	inline static uint32_t G(uint32_t x, uint32_t y, uint32_t z) {
		return y ^ (z & (x ^ y));
	}

	inline static uint32_t H(uint32_t x, uint32_t y, uint32_t z) {
		return x ^ y ^ z;
	}

	inline static uint32_t I(uint32_t x, uint32_t y, uint32_t z) {
		return y ^ (x | ~z);
	}

	inline static uint32_t ROTATE_LEFT(uint32_t x, uint32_t n) {
		return (x << n) | (x >> (32 - n));
	}

	inline static void FF(uint32_t& a, uint32_t b, uint32_t c, uint32_t d,
		uint32_t x, uint32_t s, uint32_t ac) {
		a = ROTATE_LEFT(a + F(b, c, d) + x + ac, s) + b;
	}

	inline static void GG(uint32_t& a, uint32_t b, uint32_t c, uint32_t d,
		uint32_t x, uint32_t s, uint32_t ac) {
		a = ROTATE_LEFT(a + G(b, c, d) + x + ac, s) + b;
	}

	inline static void HH(uint32_t& a, uint32_t b, uint32_t c, uint32_t d,
		uint32_t x, uint32_t s, uint32_t ac) {
		a = ROTATE_LEFT(a + H(b, c, d) + x + ac, s) + b;
	}

	inline static void II(uint32_t& a, uint32_t b, uint32_t c, uint32_t d,
		uint32_t x, uint32_t s, uint32_t ac) {
		a = ROTATE_LEFT(a + I(b, c, d) + x + ac, s) + b;
	}

	void MD5::transform(int8_t block[64]) {
		uint32_t x[16];
		uint32_t *p = reinterpret_cast<uint32_t*>(block);
		uint32_t a = ctx.state[0];
		uint32_t b = ctx.state[1];
		uint32_t c = ctx.state[2];
		uint32_t d = ctx.state[3];

		FF(a, b, c, d, x[0] = htole32(p[0]), 7, 0xd76aa478);
		FF(d, a, b, c, x[1] = htole32(p[1]), 12, 0xe8c7b756);
		FF(c, d, a, b, x[2] = htole32(p[2]), 17, 0x242070db);
		FF(b, c, d, a, x[3] = htole32(p[3]), 22, 0xc1bdceee);
		FF(a, b, c, d, x[4] = htole32(p[4]), 7, 0xf57c0faf);
		FF(d, a, b, c, x[5] = htole32(p[5]), 12, 0x4787c62a);
		FF(c, d, a, b, x[6] = htole32(p[6]), 17, 0xa8304613);
		FF(b, c, d, a, x[7] = htole32(p[7]), 22, 0xfd469501);
		FF(a, b, c, d, x[8] = htole32(p[8]), 7, 0x698098d8);
		FF(d, a, b, c, x[9] = htole32(p[9]), 12, 0x8b44f7af);
		FF(c, d, a, b, x[10] = htole32(p[10]), 17, 0xFFFF5bb1);
		FF(b, c, d, a, x[11] = htole32(p[11]), 22, 0x895cd7be);
		FF(a, b, c, d, x[12] = htole32(p[12]), 7, 0x6b901122);
		FF(d, a, b, c, x[13] = htole32(p[13]), 12, 0xfd987193);
		FF(c, d, a, b, x[14] = htole32(p[14]), 17, 0xa679438e);
		FF(b, c, d, a, x[15] = htole32(p[15]), 22, 0x49b40821);

		GG(a, b, c, d, x[1], 5, 0xf61e2562);
		GG(d, a, b, c, x[6], 9, 0xc040b340);
		GG(c, d, a, b, x[11], 14, 0x265e5a51);
		GG(b, c, d, a, x[0], 20, 0xe9b6c7aa);
		GG(a, b, c, d, x[5], 5, 0xd62f105d);
		GG(d, a, b, c, x[10], 9, 0x2441453);
		GG(c, d, a, b, x[15], 14, 0xd8a1e681);
		GG(b, c, d, a, x[4], 20, 0xe7d3fbc8);
		GG(a, b, c, d, x[9], 5, 0x21e1cde6);
		GG(d, a, b, c, x[14], 9, 0xc33707d6);
		GG(c, d, a, b, x[3], 14, 0xf4d50d87);
		GG(b, c, d, a, x[8], 20, 0x455a14ed);
		GG(a, b, c, d, x[13], 5, 0xa9e3e905);
		GG(d, a, b, c, x[2], 9, 0xfcefa3f8);
		GG(c, d, a, b, x[7], 14, 0x676f02d9);
		GG(b, c, d, a, x[12], 20, 0x8d2a4c8a);

		HH(a, b, c, d, x[5], 4, 0xFFfa3942);
		HH(d, a, b, c, x[8], 11, 0x8771f681);
		HH(c, d, a, b, x[11], 16, 0x6d9d6122);
		HH(b, c, d, a, x[14], 23, 0xfde5380c);
		HH(a, b, c, d, x[1], 4, 0xa4beea44);
		HH(d, a, b, c, x[4], 11, 0x4bdecfa9);
		HH(c, d, a, b, x[7], 16, 0xf6bb4b60);
		HH(b, c, d, a, x[10], 23, 0xbebfbc70);
		HH(a, b, c, d, x[13], 4, 0x289b7ec6);
		HH(d, a, b, c, x[0], 11, 0xeaa127fa);
		HH(c, d, a, b, x[3], 16, 0xd4ef3085);
		HH(b, c, d, a, x[6], 23, 0x4881d05);
		HH(a, b, c, d, x[9], 4, 0xd9d4d039);
		HH(d, a, b, c, x[12], 11, 0xe6db99e5);
		HH(c, d, a, b, x[15], 16, 0x1fa27cf8);
		HH(b, c, d, a, x[2], 23, 0xc4ac5665);

		II(a, b, c, d, x[0], 6, 0xf4292244);
		II(d, a, b, c, x[7], 10, 0x432aFF97);
		II(c, d, a, b, x[14], 15, 0xab9423a7);
		II(b, c, d, a, x[5], 21, 0xfc93a039);
		II(a, b, c, d, x[12], 6, 0x655b59c3);
		II(d, a, b, c, x[3], 10, 0x8f0ccc92);
		II(c, d, a, b, x[10], 15, 0xFFeFF47d);
		II(b, c, d, a, x[1], 21, 0x85845dd1);
		II(a, b, c, d, x[8], 6, 0x6fa87e4f);
		II(d, a, b, c, x[15], 10, 0xfe2ce6e0);
		II(c, d, a, b, x[6], 15, 0xa3014314);
		II(b, c, d, a, x[13], 21, 0x4e0811a1);
		II(a, b, c, d, x[4], 6, 0xf7537e82);
		II(d, a, b, c, x[11], 10, 0xbd3af235);
		II(c, d, a, b, x[2], 15, 0x2ad7d2bb);
		II(b, c, d, a, x[9], 21, 0xeb86d391);

		ctx.state[0] += a;
		ctx.state[1] += b;
		ctx.state[2] += c;
		ctx.state[3] += d;
		ctx.count += 512;
	}

	void MD5::fill(int8_t *data, int32_t len) {
		if (ctx.remain) {
			int32_t copy_len = 64 - ctx.remain;
			if (len < copy_len) {
				memcpy(ctx.buffer + ctx.remain, data, len);
				ctx.remain += len;
				return;
			}
			memcpy(ctx.buffer + ctx.remain, data, copy_len);
			transform(ctx.buffer);
			len -= copy_len;
			data += copy_len;
		}
		for (; len >= 64; len -= 64, data += 64) {
			transform(data);
		}
		if ((ctx.remain = len) > 0)
			memcpy(ctx.buffer, data, ctx.remain);
	}

	void MD5::update(int8_t c) {
		fill(&c, 1);
		sink->update(c);
	}

	void MD5::update(int8_t data[], int32_t off, int32_t len) {
		fill(data + off, len);
		sink->update(data, off, len);
	}

	void MD5::flush() {
		sink->flush();
	}

	static int8_t padding[64] = { -128, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
		0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
		0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 };

	const int8_t* MD5::digest() {
		CTX save;
		memcpy(&save, &ctx, sizeof(ctx));
		uint64_t count = htole32((uint32_t)(ctx.count + ctx.remain * 8));
		uint32_t index = ctx.remain & 0x3f;
		uint32_t padlen = (index < 56) ? (56 - index) : (120 - index);
		fill(padding, padlen);
		fill(reinterpret_cast<int8_t*>(&count), sizeof(count));
		ctx.state[0] = htole32(ctx.state[0]);
		ctx.state[1] = htole32(ctx.state[1]);
		ctx.state[2] = htole32(ctx.state[2]);
		ctx.state[3] = htole32(ctx.state[3]);
		memcpy(&result, ctx.state, sizeof(ctx.state));
		memcpy(&ctx, &save, sizeof(ctx));
		return result;
	}

	inline static uint32_t rol1(uint32_t x) {
		return (x << 1) | (x >> 31);
	}

	inline static uint32_t rol5(uint32_t x) {
		return (x << 5) | (x >> 27);
	}

	inline static uint32_t rol30(uint32_t x) {
		return (x << 30) | (x >> 2);
	}

	void SHA1::transform(int8_t block[64]) {
		uint32_t x[16];
		uint32_t *p = reinterpret_cast<uint32_t *>(block);
		uint32_t a = ctx.state[0];
		uint32_t b = ctx.state[1];
		uint32_t c = ctx.state[2];
		uint32_t d = ctx.state[3];
		uint32_t e = ctx.state[4];

#define K1  0x5A827999L
#define K2  0x6ED9EBA1L
#define K3  0x8F1BBCDCL
#define K4  0xCA62C1D6L
#define F1(x,y,z)   (z^(x&(y^z)))
#define F2(x,y,z)   (x^y^z)
#define F3(x,y,z)   ((x&y)|(z&(x|y)))
#define F4(x,y,z)   (x^y^z)
#define M(i) ( x[i&0x0f] = rol1(x[(i-14)&15]^x[(i-8)&15]^x[(i-3)&15]^x[i&15]) )
#define R(a,b,c,d,e,f,k,m)  e += rol5(a)+f(b,c,d)+k+m; b=rol30(b)

		R(a, b, c, d, e, F1, K1, (x[0] = htobe32(p[0])));
		R(e, a, b, c, d, F1, K1, (x[1] = htobe32(p[1])));
		R(d, e, a, b, c, F1, K1, (x[2] = htobe32(p[2])));
		R(c, d, e, a, b, F1, K1, (x[3] = htobe32(p[3])));
		R(b, c, d, e, a, F1, K1, (x[4] = htobe32(p[4])));
		R(a, b, c, d, e, F1, K1, (x[5] = htobe32(p[5])));
		R(e, a, b, c, d, F1, K1, (x[6] = htobe32(p[6])));
		R(d, e, a, b, c, F1, K1, (x[7] = htobe32(p[7])));
		R(c, d, e, a, b, F1, K1, (x[8] = htobe32(p[8])));
		R(b, c, d, e, a, F1, K1, (x[9] = htobe32(p[9])));
		R(a, b, c, d, e, F1, K1, (x[10] = htobe32(p[10])));
		R(e, a, b, c, d, F1, K1, (x[11] = htobe32(p[11])));
		R(d, e, a, b, c, F1, K1, (x[12] = htobe32(p[12])));
		R(c, d, e, a, b, F1, K1, (x[13] = htobe32(p[13])));
		R(b, c, d, e, a, F1, K1, (x[14] = htobe32(p[14])));
		R(a, b, c, d, e, F1, K1, (x[15] = htobe32(p[15])));
		R(e, a, b, c, d, F1, K1, M(16));
		R(d, e, a, b, c, F1, K1, M(17));
		R(c, d, e, a, b, F1, K1, M(18));
		R(b, c, d, e, a, F1, K1, M(19));
		R(a, b, c, d, e, F2, K2, M(20));
		R(e, a, b, c, d, F2, K2, M(21));
		R(d, e, a, b, c, F2, K2, M(22));
		R(c, d, e, a, b, F2, K2, M(23));
		R(b, c, d, e, a, F2, K2, M(24));
		R(a, b, c, d, e, F2, K2, M(25));
		R(e, a, b, c, d, F2, K2, M(26));
		R(d, e, a, b, c, F2, K2, M(27));
		R(c, d, e, a, b, F2, K2, M(28));
		R(b, c, d, e, a, F2, K2, M(29));
		R(a, b, c, d, e, F2, K2, M(30));
		R(e, a, b, c, d, F2, K2, M(31));
		R(d, e, a, b, c, F2, K2, M(32));
		R(c, d, e, a, b, F2, K2, M(33));
		R(b, c, d, e, a, F2, K2, M(34));
		R(a, b, c, d, e, F2, K2, M(35));
		R(e, a, b, c, d, F2, K2, M(36));
		R(d, e, a, b, c, F2, K2, M(37));
		R(c, d, e, a, b, F2, K2, M(38));
		R(b, c, d, e, a, F2, K2, M(39));
		R(a, b, c, d, e, F3, K3, M(40));
		R(e, a, b, c, d, F3, K3, M(41));
		R(d, e, a, b, c, F3, K3, M(42));
		R(c, d, e, a, b, F3, K3, M(43));
		R(b, c, d, e, a, F3, K3, M(44));
		R(a, b, c, d, e, F3, K3, M(45));
		R(e, a, b, c, d, F3, K3, M(46));
		R(d, e, a, b, c, F3, K3, M(47));
		R(c, d, e, a, b, F3, K3, M(48));
		R(b, c, d, e, a, F3, K3, M(49));
		R(a, b, c, d, e, F3, K3, M(50));
		R(e, a, b, c, d, F3, K3, M(51));
		R(d, e, a, b, c, F3, K3, M(52));
		R(c, d, e, a, b, F3, K3, M(53));
		R(b, c, d, e, a, F3, K3, M(54));
		R(a, b, c, d, e, F3, K3, M(55));
		R(e, a, b, c, d, F3, K3, M(56));
		R(d, e, a, b, c, F3, K3, M(57));
		R(c, d, e, a, b, F3, K3, M(58));
		R(b, c, d, e, a, F3, K3, M(59));
		R(a, b, c, d, e, F4, K4, M(60));
		R(e, a, b, c, d, F4, K4, M(61));
		R(d, e, a, b, c, F4, K4, M(62));
		R(c, d, e, a, b, F4, K4, M(63));
		R(b, c, d, e, a, F4, K4, M(64));
		R(a, b, c, d, e, F4, K4, M(65));
		R(e, a, b, c, d, F4, K4, M(66));
		R(d, e, a, b, c, F4, K4, M(67));
		R(c, d, e, a, b, F4, K4, M(68));
		R(b, c, d, e, a, F4, K4, M(69));
		R(a, b, c, d, e, F4, K4, M(70));
		R(e, a, b, c, d, F4, K4, M(71));
		R(d, e, a, b, c, F4, K4, M(72));
		R(c, d, e, a, b, F4, K4, M(73));
		R(b, c, d, e, a, F4, K4, M(74));
		R(a, b, c, d, e, F4, K4, M(75));
		R(e, a, b, c, d, F4, K4, M(76));
		R(d, e, a, b, c, F4, K4, M(77));
		R(c, d, e, a, b, F4, K4, M(78));
		R(b, c, d, e, a, F4, K4, M(79));
#undef K1
#undef K2
#undef K3
#undef K4
#undef F1
#undef F2
#undef F3
#undef F4
#undef M
#undef R
		ctx.state[0] += a;
		ctx.state[1] += b;
		ctx.state[2] += c;
		ctx.state[3] += d;
		ctx.state[4] += e;
		ctx.count += 512;
	}

	void SHA1::fill(int8_t *data, int32_t len) {
		if (ctx.remain) {
			int32_t copy_len = 64 - ctx.remain;
			if (len < copy_len) {
				memcpy(ctx.buffer + ctx.remain, data, len);
				ctx.remain += len;
				return;
			}
			memcpy(ctx.buffer + ctx.remain, data, copy_len);
			transform(ctx.buffer);
			len -= copy_len;
			data += copy_len;
		}
		for (; len >= 64; len -= 64, data += 64) {
			transform(data);
		}
		if ((ctx.remain = len) > 0)
			memcpy(ctx.buffer, data, ctx.remain);
	}

	void SHA1::reset() {
		ctx.state[0] = 0x67452301;
		ctx.state[1] = 0xefcdab89;
		ctx.state[2] = 0x98badcfe;
		ctx.state[3] = 0x10325476;
		ctx.state[4] = 0xc3d2e1f0;
		ctx.count = 0;
		ctx.remain = 0;
	}

	SHA1::SHA1(std::shared_ptr<Codec> _sink) :
		sink(_sink) {
		reset();
	}

	SHA1::SHA1() :
		sink(Codec::Null()) {
		reset();
	}

	void SHA1::update(int8_t c) {
		fill(&c, 1);
		sink->update(c);
	}

	void SHA1::update(int8_t data[], int32_t off, int32_t len) {
		fill(data + off, len);
		sink->update(data, off, len);
	}

	void SHA1::flush() {
		sink->flush();
	}

	const int8_t* SHA1::digest() {
		CTX save;
		memcpy(&save, &ctx, sizeof(ctx));
		uint64_t count = htobe64(ctx.count + ctx.remain * 8);
		uint32_t index = ctx.remain & 0x3f;
		uint32_t padlen = (index < 56) ? (56 - index) : (120 - index);
		fill(padding, padlen);
		fill(reinterpret_cast<int8_t*>(&count), sizeof(count));
		ctx.state[0] = htobe32(ctx.state[0]);
		ctx.state[1] = htobe32(ctx.state[1]);
		ctx.state[2] = htobe32(ctx.state[2]);
		ctx.state[3] = htobe32(ctx.state[3]);
		ctx.state[4] = htobe32(ctx.state[4]);
		memcpy(&result, ctx.state, sizeof(ctx.state));
		memcpy(&ctx, &save, sizeof(ctx));
		return result;
	}

	void SHA256::transform(int8_t block[64]) {
		static const uint32_t K[64] = {
			0x428a2f98, 0x71374491, 0xb5c0fbcf, 0xe9b5dba5, 0x3956c25b,
			0x59f111f1, 0x923f82a4, 0xab1c5ed5, 0xd807aa98, 0x12835b01,
			0x243185be, 0x550c7dc3, 0x72be5d74, 0x80deb1fe, 0x9bdc06a7,
			0xc19bf174, 0xe49b69c1, 0xefbe4786, 0x0fc19dc6, 0x240ca1cc,
			0x2de92c6f, 0x4a7484aa, 0x5cb0a9dc, 0x76f988da, 0x983e5152,
			0xa831c66d, 0xb00327c8, 0xbf597fc7, 0xc6e00bf3, 0xd5a79147,
			0x06ca6351, 0x14292967, 0x27b70a85, 0x2e1b2138, 0x4d2c6dfc,
			0x53380d13, 0x650a7354, 0x766a0abb, 0x81c2c92e, 0x92722c85,
			0xa2bfe8a1, 0xa81a664b, 0xc24b8b70, 0xc76c51a3, 0xd192e819,
			0xd6990624, 0xf40e3585, 0x106aa070, 0x19a4c116, 0x1e376c08,
			0x2748774c, 0x34b0bcb5, 0x391c0cb3, 0x4ed8aa4a, 0x5b9cca4f,
			0x682e6ff3, 0x748f82ee, 0x78a5636f, 0x84c87814, 0x8cc70208,
			0x90befffa, 0xa4506ceb, 0xbef9a3f7, 0xc67178f2
		};
		uint32_t x[64];
		uint32_t *p = reinterpret_cast<uint32_t *>(block);
		uint32_t  a = ctx.state[0];
		uint32_t  b = ctx.state[1];
		uint32_t  c = ctx.state[2];
		uint32_t  d = ctx.state[3];
		uint32_t  e = ctx.state[4];
		uint32_t  f = ctx.state[5];
		uint32_t  g = ctx.state[6];
		uint32_t  h = ctx.state[7];

#define SHA256_SHR(bits,word)	((word) >> (bits))
#define SHA256_ROTL(bits,word)	(((word) << (bits)) | ((word) >> (32-(bits))))
#define SHA256_ROTR(bits,word)	(((word) >> (bits)) | ((word) << (32-(bits))))
#define SHA256_SIGMA0(word)		(SHA256_ROTR( 2,word) ^ SHA256_ROTR(13,word) ^ SHA256_ROTR(22,word))
#define SHA256_SIGMA1(word)		(SHA256_ROTR( 6,word) ^ SHA256_ROTR(11,word) ^ SHA256_ROTR(25,word))
#define SHA256_sigma0(word)		(SHA256_ROTR( 7,word) ^ SHA256_ROTR(18,word) ^ SHA256_SHR( 3,word))
#define SHA256_sigma1(word)		(SHA256_ROTR(17,word) ^ SHA256_ROTR(19,word) ^ SHA256_SHR(10,word))
#define SHA_Ch(x,y,z)			(((x) & (y)) ^ ((~(x)) & (z)))
#define SHA_Maj(x,y,z)			(((x) & (y)) ^ ((x) & (z)) ^ ((y) & (z)))

		for (int i = 0; i < 16; i++)
			x[i] = htobe32(p[i]);
		for (int i = 16; i < 64; i++)
			x[i] = SHA256_sigma1(x[i - 2]) + x[i - 7] + SHA256_sigma0(x[i - 15]) + x[i - 16];
		for (int i = 0; i < 64; i++) {
			uint32_t t1 = h + SHA256_SIGMA1(e) + SHA_Ch(e, f, g) + K[i] + x[i];
			uint32_t t2 = SHA256_SIGMA0(a) + SHA_Maj(a, b, c);
			h = g;
			g = f;
			f = e;
			e = d + t1;
			d = c;
			c = b;
			b = a;
			a = t1 + t2;
		}

#undef SHA256_SHR
#undef SHA256_ROTL
#undef SHA256_ROTR
#undef SHA256_SIGMA0
#undef SHA256_SIGMA1
#undef SHA256_sigma0
#undef SHA256_sigma1
#undef SHA_Ch
#undef SHA_Maj

		ctx.state[0] += a;
		ctx.state[1] += b;
		ctx.state[2] += c;
		ctx.state[3] += d;
		ctx.state[4] += e;
		ctx.state[5] += f;
		ctx.state[6] += g;
		ctx.state[7] += h;
		ctx.count += 512;
	}

	void SHA256::fill(int8_t *data, int32_t len) {
		if (ctx.remain) {
			int32_t copy_len = 64 - ctx.remain;
			if (len < copy_len) {
				memcpy(ctx.buffer + ctx.remain, data, len);
				ctx.remain += len;
				return;
			}
			memcpy(ctx.buffer + ctx.remain, data, copy_len);
			transform(ctx.buffer);
			len -= copy_len;
			data += copy_len;
		}
		for (; len >= 64; len -= 64, data += 64) {
			transform(data);
		}
		if ((ctx.remain = len) > 0)
			memcpy(ctx.buffer, data, ctx.remain);
	}

	void SHA256::reset() {
		ctx.state[0] = 0x6a09e667;
		ctx.state[1] = 0xbb67ae85;
		ctx.state[2] = 0x3c6ef372;
		ctx.state[3] = 0xa54ff53a;
		ctx.state[4] = 0x510e527f;
		ctx.state[5] = 0x9b05688c;
		ctx.state[6] = 0x1f83d9ab;
		ctx.state[7] = 0x5be0cd19;
		ctx.count = 0;
		ctx.remain = 0;
	}

	SHA256::SHA256(std::shared_ptr<Codec> _sink) :
		sink(_sink) {
		reset();
	}

	SHA256::SHA256() :
		sink(Codec::Null()) {
		reset();
	}

	void SHA256::update(int8_t c) {
		fill(&c, 1);
		sink->update(c);
	}

	void SHA256::update(int8_t data[], int32_t off, int32_t len) {
		fill(data + off, len);
		sink->update(data, off, len);
	}

	void SHA256::flush() {
		sink->flush();
	}

	const int8_t* SHA256::digest() {
		CTX save;
		memcpy(&save, &ctx, sizeof(ctx));
		uint64_t count = htobe64(ctx.count + ctx.remain * 8);
		uint32_t index = ctx.remain & 0x3f;
		uint32_t padlen = (index < 56) ? (56 - index) : (120 - index);
		fill(padding, padlen);
		fill(reinterpret_cast<int8_t*>(&count), sizeof(count));
		ctx.state[0] = htobe32(ctx.state[0]);
		ctx.state[1] = htobe32(ctx.state[1]);
		ctx.state[2] = htobe32(ctx.state[2]);
		ctx.state[3] = htobe32(ctx.state[3]);
		ctx.state[4] = htobe32(ctx.state[4]);
		ctx.state[5] = htobe32(ctx.state[5]);
		ctx.state[6] = htobe32(ctx.state[6]);
		ctx.state[7] = htobe32(ctx.state[7]);
		memcpy(&result, ctx.state, sizeof(ctx.state));
		memcpy(&ctx, &save, sizeof(ctx));
		return result;
	}

	void HmacMD5::reset(int8_t key[], int32_t off, int32_t len)
	{
		int8_t k_ipad[64];
		if (len > 64) {
			out.update(key, off, len);
			memcpy(k_ipad, out.digest(), len = 16);
		}
		else {
			memcpy(k_ipad, key + off, len);
		}
		memcpy(k_opad, k_ipad, 64);

		for (int32_t i = 0; i < len; i++) {
			k_ipad[i] ^= 0x36;
			k_opad[i] ^= 0x5c;
		}
		memset(k_ipad + len, 0x36, 64 - len);
		memset(k_opad + len, 0x5c, 64 - len);
		md.update(k_ipad, 0, sizeof(k_ipad));
	}

	HmacMD5::HmacMD5(std::shared_ptr<Codec> _sink, int8_t key[], int32_t off, int32_t len) :
		sink(_sink), md(Codec::Null()), out(Codec::Null()) {
		reset(key, off, len);
	}

	HmacMD5::HmacMD5(int8_t key[], int32_t off, int32_t len) :
		sink(Codec::Null()), md(Codec::Null()), out(Codec::Null()) {
		reset(key, off, len);
	}

	void HmacMD5::update(int8_t c) {
		md.update(c);
		sink->update(c);
	}

	void HmacMD5::update(int8_t data[], int32_t off, int32_t len) {
		md.update(data, off, len);
		sink->update(data, off, len);
	}

	void HmacMD5::flush() {
		md.flush();
		sink->flush();
	}

	const int8_t* HmacMD5::digest() {
		out.reset();
		out.update(k_opad, 0, sizeof(k_opad));
		out.update(const_cast<int8_t*>(md.digest()), 0, 16);
		return out.digest();
	}

	void HmacSHA1::reset(int8_t key[], int32_t off, int32_t len)
	{
		int8_t k_ipad[64];
		if (len > 64) {
			out.update(key, off, len);
			memcpy(k_ipad, out.digest(), len = 16);
		}
		else {
			memcpy(k_ipad, key + off, len);
		}
		memcpy(k_opad, k_ipad, 64);

		for (int32_t i = 0; i < len; i++) {
			k_ipad[i] ^= 0x36;
			k_opad[i] ^= 0x5c;
		}
		memset(k_ipad + len, 0x36, 64 - len);
		memset(k_opad + len, 0x5c, 64 - len);
		md.update(k_ipad, 0, sizeof(k_ipad));
	}

	HmacSHA1::HmacSHA1(std::shared_ptr<Codec> _sink, int8_t key[], int32_t off, int32_t len) :
		sink(_sink), md(Codec::Null()), out(Codec::Null()) {
		reset(key, off, len);
	}

	HmacSHA1::HmacSHA1(int8_t key[], int32_t off, int32_t len) :
		sink(Codec::Null()), md(Codec::Null()), out(Codec::Null()) {
		reset(key, off, len);
	}

	void HmacSHA1::update(int8_t c) {
		md.update(c);
		sink->update(c);
	}

	void HmacSHA1::update(int8_t data[], int32_t off, int32_t len) {
		md.update(data, off, len);
		sink->update(data, off, len);
	}

	void HmacSHA1::flush() {
		md.flush();
		sink->flush();
	}

	const int8_t* HmacSHA1::digest() {
		out.reset();
		out.update(k_opad, 0, sizeof(k_opad));
		out.update(const_cast<int8_t*>(md.digest()), 0, 20);
		return out.digest();
	}

	void HmacSHA256::reset(int8_t key[], int32_t off, int32_t len)
	{
		int8_t k_ipad[64];

		if (len > 64) {
			out.update(key, off, len);
			memcpy(k_ipad, out.digest(), len = 16);
		}
		else {
			memcpy(k_ipad, key + off, len);
		}
		memcpy(k_opad, k_ipad, 64);

		for (int32_t i = 0; i < len; i++) {
			k_ipad[i] ^= 0x36;
			k_opad[i] ^= 0x5c;
		}
		memset(k_ipad + len, 0x36, 64 - len);
		memset(k_opad + len, 0x5c, 64 - len);
		md.update(k_ipad, 0, sizeof(k_ipad));
	}

	HmacSHA256::HmacSHA256(std::shared_ptr<Codec> _sink, int8_t key[], int32_t off, int32_t len) :
		sink(_sink), md(Codec::Null()), out(Codec::Null()) {
		reset(key, off, len);
	}

	HmacSHA256::HmacSHA256(int8_t key[], int32_t off, int32_t len) :
		sink(Codec::Null()), md(Codec::Null()), out(Codec::Null()) {
		reset(key, off, len);
	}

	void HmacSHA256::update(int8_t c) {
		md.update(c);
		sink->update(c);
	}

	void HmacSHA256::update(int8_t data[], int32_t off, int32_t len) {
		md.update(data, off, len);
		sink->update(data, off, len);
	}

	void HmacSHA256::flush() {
		md.flush();
		sink->flush();
	}

	const int8_t* HmacSHA256::digest() {
		out.reset();
		out.update(k_opad, 0, sizeof(k_opad));
		out.update(const_cast<int8_t*>(md.digest()), 0, 32);
		return out.digest();
	}

	static uint32_t crc32table[] = { 0x00000000, 0x77073096, 0xee0e612c, 0x990951ba,
		0x076dc419, 0x706af48f, 0xe963a535, 0x9e6495a3, 0x0edb8832, 0x79dcb8a4,
		0xe0d5e91e, 0x97d2d988, 0x09b64c2b, 0x7eb17cbd, 0xe7b82d07, 0x90bf1d91,
		0x1db71064, 0x6ab020f2, 0xf3b97148, 0x84be41de, 0x1adad47d, 0x6ddde4eb,
		0xf4d4b551, 0x83d385c7, 0x136c9856, 0x646ba8c0, 0xfd62f97a, 0x8a65c9ec,
		0x14015c4f, 0x63066cd9, 0xfa0f3d63, 0x8d080df5, 0x3b6e20c8, 0x4c69105e,
		0xd56041e4, 0xa2677172, 0x3c03e4d1, 0x4b04d447, 0xd20d85fd, 0xa50ab56b,
		0x35b5a8fa, 0x42b2986c, 0xdbbbc9d6, 0xacbcf940, 0x32d86ce3, 0x45df5c75,
		0xdcd60dcf, 0xabd13d59, 0x26d930ac, 0x51de003a, 0xc8d75180, 0xbfd06116,
		0x21b4f4b5, 0x56b3c423, 0xcfba9599, 0xb8bda50f, 0x2802b89e, 0x5f058808,
		0xc60cd9b2, 0xb10be924, 0x2f6f7c87, 0x58684c11, 0xc1611dab, 0xb6662d3d,
		0x76dc4190, 0x01db7106, 0x98d220bc, 0xefd5102a, 0x71b18589, 0x06b6b51f,
		0x9fbfe4a5, 0xe8b8d433, 0x7807c9a2, 0x0f00f934, 0x9609a88e, 0xe10e9818,
		0x7f6a0dbb, 0x086d3d2d, 0x91646c97, 0xe6635c01, 0x6b6b51f4, 0x1c6c6162,
		0x856530d8, 0xf262004e, 0x6c0695ed, 0x1b01a57b, 0x8208f4c1, 0xf50fc457,
		0x65b0d9c6, 0x12b7e950, 0x8bbeb8ea, 0xfcb9887c, 0x62dd1ddf, 0x15da2d49,
		0x8cd37cf3, 0xfbd44c65, 0x4db26158, 0x3ab551ce, 0xa3bc0074, 0xd4bb30e2,
		0x4adfa541, 0x3dd895d7, 0xa4d1c46d, 0xd3d6f4fb, 0x4369e96a, 0x346ed9fc,
		0xad678846, 0xda60b8d0, 0x44042d73, 0x33031de5, 0xaa0a4c5f, 0xdd0d7cc9,
		0x5005713c, 0x270241aa, 0xbe0b1010, 0xc90c2086, 0x5768b525, 0x206f85b3,
		0xb966d409, 0xce61e49f, 0x5edef90e, 0x29d9c998, 0xb0d09822, 0xc7d7a8b4,
		0x59b33d17, 0x2eb40d81, 0xb7bd5c3b, 0xc0ba6cad, 0xedb88320, 0x9abfb3b6,
		0x03b6e20c, 0x74b1d29a, 0xead54739, 0x9dd277af, 0x04db2615, 0x73dc1683,
		0xe3630b12, 0x94643b84, 0x0d6d6a3e, 0x7a6a5aa8, 0xe40ecf0b, 0x9309ff9d,
		0x0a00ae27, 0x7d079eb1, 0xf00f9344, 0x8708a3d2, 0x1e01f268, 0x6906c2fe,
		0xf762575d, 0x806567cb, 0x196c3671, 0x6e6b06e7, 0xfed41b76, 0x89d32be0,
		0x10da7a5a, 0x67dd4acc, 0xf9b9df6f, 0x8ebeeff9, 0x17b7be43, 0x60b08ed5,
		0xd6d6a3e8, 0xa1d1937e, 0x38d8c2c4, 0x4fdff252, 0xd1bb67f1, 0xa6bc5767,
		0x3fb506dd, 0x48b2364b, 0xd80d2bda, 0xaf0a1b4c, 0x36034af6, 0x41047a60,
		0xdf60efc3, 0xa867df55, 0x316e8eef, 0x4669be79, 0xcb61b38c, 0xbc66831a,
		0x256fd2a0, 0x5268e236, 0xcc0c7795, 0xbb0b4703, 0x220216b9, 0x5505262f,
		0xc5ba3bbe, 0xb2bd0b28, 0x2bb45a92, 0x5cb36a04, 0xc2d7ffa7, 0xb5d0cf31,
		0x2cd99e8b, 0x5bdeae1d, 0x9b64c2b0, 0xec63f226, 0x756aa39c, 0x026d930a,
		0x9c0906a9, 0xeb0e363f, 0x72076785, 0x05005713, 0x95bf4a82, 0xe2b87a14,
		0x7bb12bae, 0x0cb61b38, 0x92d28e9b, 0xe5d5be0d, 0x7cdcefb7, 0x0bdbdf21,
		0x86d3d2d4, 0xf1d4e242, 0x68ddb3f8, 0x1fda836e, 0x81be16cd, 0xf6b9265b,
		0x6fb077e1, 0x18b74777, 0x88085ae6, 0xff0f6a70, 0x66063bca, 0x11010b5c,
		0x8f659eff, 0xf862ae69, 0x616bffd3, 0x166ccf45, 0xa00ae278, 0xd70dd2ee,
		0x4e048354, 0x3903b3c2, 0xa7672661, 0xd06016f7, 0x4969474d, 0x3e6e77db,
		0xaed16a4a, 0xd9d65adc, 0x40df0b66, 0x37d83bf0, 0xa9bcae53, 0xdebb9ec5,
		0x47b2cf7f, 0x30b5ffe9, 0xbdbdf21c, 0xcabac28a, 0x53b39330, 0x24b4a3a6,
		0xbad03605, 0xcdd70693, 0x54de5729, 0x23d967bf, 0xb3667a2e, 0xc4614ab8,
		0x5d681b02, 0x2a6f2b94, 0xb40bbe37, 0xc30c8ea1, 0x5a05df1b, 0x2d02ef8d };

	CRC32::CRC32(std::shared_ptr<Codec> _sink) :
		sink(_sink), crc(0xffffffff) {
	}

	void CRC32::update(int8_t c) {
		crc = crc32table[static_cast<uint8_t>(crc ^ c)] ^ (crc >> 8);
		sink->update(c);
	}

	void CRC32::update(int8_t data[], int32_t off, int32_t len) {
		int8_t *p = data + off;
		for (int32_t i = 0; i < len; i++)
			crc = crc32table[static_cast<uint8_t>(crc ^ p[i])] ^ (crc >> 8);
		sink->update(data, off, len);
	}

	void CRC32::flush() {
		sink->flush();
	}

	int64_t CRC32::getValue() {
		return (int32_t)(~crc);
	}

	static uint32_t ENC0[256] = { 0xc66363a5, 0xf87c7c84, 0xee777799, 0xf67b7b8d,
		0xfff2f20d, 0xd66b6bbd, 0xde6f6fb1, 0x91c5c554, 0x60303050, 0x02010103,
		0xce6767a9, 0x562b2b7d, 0xe7fefe19, 0xb5d7d762, 0x4dababe6, 0xec76769a,
		0x8fcaca45, 0x1f82829d, 0x89c9c940, 0xfa7d7d87, 0xeffafa15, 0xb25959eb,
		0x8e4747c9, 0xfbf0f00b, 0x41adadec, 0xb3d4d467, 0x5fa2a2fd, 0x45afafea,
		0x239c9cbf, 0x53a4a4f7, 0xe4727296, 0x9bc0c05b, 0x75b7b7c2, 0xe1fdfd1c,
		0x3d9393ae, 0x4c26266a, 0x6c36365a, 0x7e3f3f41, 0xf5f7f702, 0x83cccc4f,
		0x6834345c, 0x51a5a5f4, 0xd1e5e534, 0xf9f1f108, 0xe2717193, 0xabd8d873,
		0x62313153, 0x2a15153f, 0x0804040c, 0x95c7c752, 0x46232365, 0x9dc3c35e,
		0x30181828, 0x379696a1, 0x0a05050f, 0x2f9a9ab5, 0x0e070709, 0x24121236,
		0x1b80809b, 0xdfe2e23d, 0xcdebeb26, 0x4e272769, 0x7fb2b2cd, 0xea75759f,
		0x1209091b, 0x1d83839e, 0x582c2c74, 0x341a1a2e, 0x361b1b2d, 0xdc6e6eb2,
		0xb45a5aee, 0x5ba0a0fb, 0xa45252f6, 0x763b3b4d, 0xb7d6d661, 0x7db3b3ce,
		0x5229297b, 0xdde3e33e, 0x5e2f2f71, 0x13848497, 0xa65353f5, 0xb9d1d168,
		0x00000000, 0xc1eded2c, 0x40202060, 0xe3fcfc1f, 0x79b1b1c8, 0xb65b5bed,
		0xd46a6abe, 0x8dcbcb46, 0x67bebed9, 0x7239394b, 0x944a4ade, 0x984c4cd4,
		0xb05858e8, 0x85cfcf4a, 0xbbd0d06b, 0xc5efef2a, 0x4faaaae5, 0xedfbfb16,
		0x864343c5, 0x9a4d4dd7, 0x66333355, 0x11858594, 0x8a4545cf, 0xe9f9f910,
		0x04020206, 0xfe7f7f81, 0xa05050f0, 0x783c3c44, 0x259f9fba, 0x4ba8a8e3,
		0xa25151f3, 0x5da3a3fe, 0x804040c0, 0x058f8f8a, 0x3f9292ad, 0x219d9dbc,
		0x70383848, 0xf1f5f504, 0x63bcbcdf, 0x77b6b6c1, 0xafdada75, 0x42212163,
		0x20101030, 0xe5ffff1a, 0xfdf3f30e, 0xbfd2d26d, 0x81cdcd4c, 0x180c0c14,
		0x26131335, 0xc3ecec2f, 0xbe5f5fe1, 0x359797a2, 0x884444cc, 0x2e171739,
		0x93c4c457, 0x55a7a7f2, 0xfc7e7e82, 0x7a3d3d47, 0xc86464ac, 0xba5d5de7,
		0x3219192b, 0xe6737395, 0xc06060a0, 0x19818198, 0x9e4f4fd1, 0xa3dcdc7f,
		0x44222266, 0x542a2a7e, 0x3b9090ab, 0x0b888883, 0x8c4646ca, 0xc7eeee29,
		0x6bb8b8d3, 0x2814143c, 0xa7dede79, 0xbc5e5ee2, 0x160b0b1d, 0xaddbdb76,
		0xdbe0e03b, 0x64323256, 0x743a3a4e, 0x140a0a1e, 0x924949db, 0x0c06060a,
		0x4824246c, 0xb85c5ce4, 0x9fc2c25d, 0xbdd3d36e, 0x43acacef, 0xc46262a6,
		0x399191a8, 0x319595a4, 0xd3e4e437, 0xf279798b, 0xd5e7e732, 0x8bc8c843,
		0x6e373759, 0xda6d6db7, 0x018d8d8c, 0xb1d5d564, 0x9c4e4ed2, 0x49a9a9e0,
		0xd86c6cb4, 0xac5656fa, 0xf3f4f407, 0xcfeaea25, 0xca6565af, 0xf47a7a8e,
		0x47aeaee9, 0x10080818, 0x6fbabad5, 0xf0787888, 0x4a25256f, 0x5c2e2e72,
		0x381c1c24, 0x57a6a6f1, 0x73b4b4c7, 0x97c6c651, 0xcbe8e823, 0xa1dddd7c,
		0xe874749c, 0x3e1f1f21, 0x964b4bdd, 0x61bdbddc, 0x0d8b8b86, 0x0f8a8a85,
		0xe0707090, 0x7c3e3e42, 0x71b5b5c4, 0xcc6666aa, 0x904848d8, 0x06030305,
		0xf7f6f601, 0x1c0e0e12, 0xc26161a3, 0x6a35355f, 0xae5757f9, 0x69b9b9d0,
		0x17868691, 0x99c1c158, 0x3a1d1d27, 0x279e9eb9, 0xd9e1e138, 0xebf8f813,
		0x2b9898b3, 0x22111133, 0xd26969bb, 0xa9d9d970, 0x078e8e89, 0x339494a7,
		0x2d9b9bb6, 0x3c1e1e22, 0x15878792, 0xc9e9e920, 0x87cece49, 0xaa5555ff,
		0x50282878, 0xa5dfdf7a, 0x038c8c8f, 0x59a1a1f8, 0x09898980, 0x1a0d0d17,
		0x65bfbfda, 0xd7e6e631, 0x844242c6, 0xd06868b8, 0x824141c3, 0x299999b0,
		0x5a2d2d77, 0x1e0f0f11, 0x7bb0b0cb, 0xa85454fc, 0x6dbbbbd6, 0x2c16163a };
	static uint32_t ENC1[] = { 0xa5c66363, 0x84f87c7c, 0x99ee7777, 0x8df67b7b,
		0x0dfff2f2, 0xbdd66b6b, 0xb1de6f6f, 0x5491c5c5, 0x50603030, 0x03020101,
		0xa9ce6767, 0x7d562b2b, 0x19e7fefe, 0x62b5d7d7, 0xe64dabab, 0x9aec7676,
		0x458fcaca, 0x9d1f8282, 0x4089c9c9, 0x87fa7d7d, 0x15effafa, 0xebb25959,
		0xc98e4747, 0x0bfbf0f0, 0xec41adad, 0x67b3d4d4, 0xfd5fa2a2, 0xea45afaf,
		0xbf239c9c, 0xf753a4a4, 0x96e47272, 0x5b9bc0c0, 0xc275b7b7, 0x1ce1fdfd,
		0xae3d9393, 0x6a4c2626, 0x5a6c3636, 0x417e3f3f, 0x02f5f7f7, 0x4f83cccc,
		0x5c683434, 0xf451a5a5, 0x34d1e5e5, 0x08f9f1f1, 0x93e27171, 0x73abd8d8,
		0x53623131, 0x3f2a1515, 0x0c080404, 0x5295c7c7, 0x65462323, 0x5e9dc3c3,
		0x28301818, 0xa1379696, 0x0f0a0505, 0xb52f9a9a, 0x090e0707, 0x36241212,
		0x9b1b8080, 0x3ddfe2e2, 0x26cdebeb, 0x694e2727, 0xcd7fb2b2, 0x9fea7575,
		0x1b120909, 0x9e1d8383, 0x74582c2c, 0x2e341a1a, 0x2d361b1b, 0xb2dc6e6e,
		0xeeb45a5a, 0xfb5ba0a0, 0xf6a45252, 0x4d763b3b, 0x61b7d6d6, 0xce7db3b3,
		0x7b522929, 0x3edde3e3, 0x715e2f2f, 0x97138484, 0xf5a65353, 0x68b9d1d1,
		0x00000000, 0x2cc1eded, 0x60402020, 0x1fe3fcfc, 0xc879b1b1, 0xedb65b5b,
		0xbed46a6a, 0x468dcbcb, 0xd967bebe, 0x4b723939, 0xde944a4a, 0xd4984c4c,
		0xe8b05858, 0x4a85cfcf, 0x6bbbd0d0, 0x2ac5efef, 0xe54faaaa, 0x16edfbfb,
		0xc5864343, 0xd79a4d4d, 0x55663333, 0x94118585, 0xcf8a4545, 0x10e9f9f9,
		0x06040202, 0x81fe7f7f, 0xf0a05050, 0x44783c3c, 0xba259f9f, 0xe34ba8a8,
		0xf3a25151, 0xfe5da3a3, 0xc0804040, 0x8a058f8f, 0xad3f9292, 0xbc219d9d,
		0x48703838, 0x04f1f5f5, 0xdf63bcbc, 0xc177b6b6, 0x75afdada, 0x63422121,
		0x30201010, 0x1ae5ffff, 0x0efdf3f3, 0x6dbfd2d2, 0x4c81cdcd, 0x14180c0c,
		0x35261313, 0x2fc3ecec, 0xe1be5f5f, 0xa2359797, 0xcc884444, 0x392e1717,
		0x5793c4c4, 0xf255a7a7, 0x82fc7e7e, 0x477a3d3d, 0xacc86464, 0xe7ba5d5d,
		0x2b321919, 0x95e67373, 0xa0c06060, 0x98198181, 0xd19e4f4f, 0x7fa3dcdc,
		0x66442222, 0x7e542a2a, 0xab3b9090, 0x830b8888, 0xca8c4646, 0x29c7eeee,
		0xd36bb8b8, 0x3c281414, 0x79a7dede, 0xe2bc5e5e, 0x1d160b0b, 0x76addbdb,
		0x3bdbe0e0, 0x56643232, 0x4e743a3a, 0x1e140a0a, 0xdb924949, 0x0a0c0606,
		0x6c482424, 0xe4b85c5c, 0x5d9fc2c2, 0x6ebdd3d3, 0xef43acac, 0xa6c46262,
		0xa8399191, 0xa4319595, 0x37d3e4e4, 0x8bf27979, 0x32d5e7e7, 0x438bc8c8,
		0x596e3737, 0xb7da6d6d, 0x8c018d8d, 0x64b1d5d5, 0xd29c4e4e, 0xe049a9a9,
		0xb4d86c6c, 0xfaac5656, 0x07f3f4f4, 0x25cfeaea, 0xafca6565, 0x8ef47a7a,
		0xe947aeae, 0x18100808, 0xd56fbaba, 0x88f07878, 0x6f4a2525, 0x725c2e2e,
		0x24381c1c, 0xf157a6a6, 0xc773b4b4, 0x5197c6c6, 0x23cbe8e8, 0x7ca1dddd,
		0x9ce87474, 0x213e1f1f, 0xdd964b4b, 0xdc61bdbd, 0x860d8b8b, 0x850f8a8a,
		0x90e07070, 0x427c3e3e, 0xc471b5b5, 0xaacc6666, 0xd8904848, 0x05060303,
		0x01f7f6f6, 0x121c0e0e, 0xa3c26161, 0x5f6a3535, 0xf9ae5757, 0xd069b9b9,
		0x91178686, 0x5899c1c1, 0x273a1d1d, 0xb9279e9e, 0x38d9e1e1, 0x13ebf8f8,
		0xb32b9898, 0x33221111, 0xbbd26969, 0x70a9d9d9, 0x89078e8e, 0xa7339494,
		0xb62d9b9b, 0x223c1e1e, 0x92158787, 0x20c9e9e9, 0x4987cece, 0xffaa5555,
		0x78502828, 0x7aa5dfdf, 0x8f038c8c, 0xf859a1a1, 0x80098989, 0x171a0d0d,
		0xda65bfbf, 0x31d7e6e6, 0xc6844242, 0xb8d06868, 0xc3824141, 0xb0299999,
		0x775a2d2d, 0x111e0f0f, 0xcb7bb0b0, 0xfca85454, 0xd66dbbbb, 0x3a2c1616 };
	static uint32_t ENC2[] = { 0x63a5c663, 0x7c84f87c, 0x7799ee77, 0x7b8df67b,
		0xf20dfff2, 0x6bbdd66b, 0x6fb1de6f, 0xc55491c5, 0x30506030, 0x01030201,
		0x67a9ce67, 0x2b7d562b, 0xfe19e7fe, 0xd762b5d7, 0xabe64dab, 0x769aec76,
		0xca458fca, 0x829d1f82, 0xc94089c9, 0x7d87fa7d, 0xfa15effa, 0x59ebb259,
		0x47c98e47, 0xf00bfbf0, 0xadec41ad, 0xd467b3d4, 0xa2fd5fa2, 0xafea45af,
		0x9cbf239c, 0xa4f753a4, 0x7296e472, 0xc05b9bc0, 0xb7c275b7, 0xfd1ce1fd,
		0x93ae3d93, 0x266a4c26, 0x365a6c36, 0x3f417e3f, 0xf702f5f7, 0xcc4f83cc,
		0x345c6834, 0xa5f451a5, 0xe534d1e5, 0xf108f9f1, 0x7193e271, 0xd873abd8,
		0x31536231, 0x153f2a15, 0x040c0804, 0xc75295c7, 0x23654623, 0xc35e9dc3,
		0x18283018, 0x96a13796, 0x050f0a05, 0x9ab52f9a, 0x07090e07, 0x12362412,
		0x809b1b80, 0xe23ddfe2, 0xeb26cdeb, 0x27694e27, 0xb2cd7fb2, 0x759fea75,
		0x091b1209, 0x839e1d83, 0x2c74582c, 0x1a2e341a, 0x1b2d361b, 0x6eb2dc6e,
		0x5aeeb45a, 0xa0fb5ba0, 0x52f6a452, 0x3b4d763b, 0xd661b7d6, 0xb3ce7db3,
		0x297b5229, 0xe33edde3, 0x2f715e2f, 0x84971384, 0x53f5a653, 0xd168b9d1,
		0x00000000, 0xed2cc1ed, 0x20604020, 0xfc1fe3fc, 0xb1c879b1, 0x5bedb65b,
		0x6abed46a, 0xcb468dcb, 0xbed967be, 0x394b7239, 0x4ade944a, 0x4cd4984c,
		0x58e8b058, 0xcf4a85cf, 0xd06bbbd0, 0xef2ac5ef, 0xaae54faa, 0xfb16edfb,
		0x43c58643, 0x4dd79a4d, 0x33556633, 0x85941185, 0x45cf8a45, 0xf910e9f9,
		0x02060402, 0x7f81fe7f, 0x50f0a050, 0x3c44783c, 0x9fba259f, 0xa8e34ba8,
		0x51f3a251, 0xa3fe5da3, 0x40c08040, 0x8f8a058f, 0x92ad3f92, 0x9dbc219d,
		0x38487038, 0xf504f1f5, 0xbcdf63bc, 0xb6c177b6, 0xda75afda, 0x21634221,
		0x10302010, 0xff1ae5ff, 0xf30efdf3, 0xd26dbfd2, 0xcd4c81cd, 0x0c14180c,
		0x13352613, 0xec2fc3ec, 0x5fe1be5f, 0x97a23597, 0x44cc8844, 0x17392e17,
		0xc45793c4, 0xa7f255a7, 0x7e82fc7e, 0x3d477a3d, 0x64acc864, 0x5de7ba5d,
		0x192b3219, 0x7395e673, 0x60a0c060, 0x81981981, 0x4fd19e4f, 0xdc7fa3dc,
		0x22664422, 0x2a7e542a, 0x90ab3b90, 0x88830b88, 0x46ca8c46, 0xee29c7ee,
		0xb8d36bb8, 0x143c2814, 0xde79a7de, 0x5ee2bc5e, 0x0b1d160b, 0xdb76addb,
		0xe03bdbe0, 0x32566432, 0x3a4e743a, 0x0a1e140a, 0x49db9249, 0x060a0c06,
		0x246c4824, 0x5ce4b85c, 0xc25d9fc2, 0xd36ebdd3, 0xacef43ac, 0x62a6c462,
		0x91a83991, 0x95a43195, 0xe437d3e4, 0x798bf279, 0xe732d5e7, 0xc8438bc8,
		0x37596e37, 0x6db7da6d, 0x8d8c018d, 0xd564b1d5, 0x4ed29c4e, 0xa9e049a9,
		0x6cb4d86c, 0x56faac56, 0xf407f3f4, 0xea25cfea, 0x65afca65, 0x7a8ef47a,
		0xaee947ae, 0x08181008, 0xbad56fba, 0x7888f078, 0x256f4a25, 0x2e725c2e,
		0x1c24381c, 0xa6f157a6, 0xb4c773b4, 0xc65197c6, 0xe823cbe8, 0xdd7ca1dd,
		0x749ce874, 0x1f213e1f, 0x4bdd964b, 0xbddc61bd, 0x8b860d8b, 0x8a850f8a,
		0x7090e070, 0x3e427c3e, 0xb5c471b5, 0x66aacc66, 0x48d89048, 0x03050603,
		0xf601f7f6, 0x0e121c0e, 0x61a3c261, 0x355f6a35, 0x57f9ae57, 0xb9d069b9,
		0x86911786, 0xc15899c1, 0x1d273a1d, 0x9eb9279e, 0xe138d9e1, 0xf813ebf8,
		0x98b32b98, 0x11332211, 0x69bbd269, 0xd970a9d9, 0x8e89078e, 0x94a73394,
		0x9bb62d9b, 0x1e223c1e, 0x87921587, 0xe920c9e9, 0xce4987ce, 0x55ffaa55,
		0x28785028, 0xdf7aa5df, 0x8c8f038c, 0xa1f859a1, 0x89800989, 0x0d171a0d,
		0xbfda65bf, 0xe631d7e6, 0x42c68442, 0x68b8d068, 0x41c38241, 0x99b02999,
		0x2d775a2d, 0x0f111e0f, 0xb0cb7bb0, 0x54fca854, 0xbbd66dbb, 0x163a2c16 };
	static uint32_t ENC3[] = { 0x6363a5c6, 0x7c7c84f8, 0x777799ee, 0x7b7b8df6,
		0xf2f20dff, 0x6b6bbdd6, 0x6f6fb1de, 0xc5c55491, 0x30305060, 0x01010302,
		0x6767a9ce, 0x2b2b7d56, 0xfefe19e7, 0xd7d762b5, 0xababe64d, 0x76769aec,
		0xcaca458f, 0x82829d1f, 0xc9c94089, 0x7d7d87fa, 0xfafa15ef, 0x5959ebb2,
		0x4747c98e, 0xf0f00bfb, 0xadadec41, 0xd4d467b3, 0xa2a2fd5f, 0xafafea45,
		0x9c9cbf23, 0xa4a4f753, 0x727296e4, 0xc0c05b9b, 0xb7b7c275, 0xfdfd1ce1,
		0x9393ae3d, 0x26266a4c, 0x36365a6c, 0x3f3f417e, 0xf7f702f5, 0xcccc4f83,
		0x34345c68, 0xa5a5f451, 0xe5e534d1, 0xf1f108f9, 0x717193e2, 0xd8d873ab,
		0x31315362, 0x15153f2a, 0x04040c08, 0xc7c75295, 0x23236546, 0xc3c35e9d,
		0x18182830, 0x9696a137, 0x05050f0a, 0x9a9ab52f, 0x0707090e, 0x12123624,
		0x80809b1b, 0xe2e23ddf, 0xebeb26cd, 0x2727694e, 0xb2b2cd7f, 0x75759fea,
		0x09091b12, 0x83839e1d, 0x2c2c7458, 0x1a1a2e34, 0x1b1b2d36, 0x6e6eb2dc,
		0x5a5aeeb4, 0xa0a0fb5b, 0x5252f6a4, 0x3b3b4d76, 0xd6d661b7, 0xb3b3ce7d,
		0x29297b52, 0xe3e33edd, 0x2f2f715e, 0x84849713, 0x5353f5a6, 0xd1d168b9,
		0x00000000, 0xeded2cc1, 0x20206040, 0xfcfc1fe3, 0xb1b1c879, 0x5b5bedb6,
		0x6a6abed4, 0xcbcb468d, 0xbebed967, 0x39394b72, 0x4a4ade94, 0x4c4cd498,
		0x5858e8b0, 0xcfcf4a85, 0xd0d06bbb, 0xefef2ac5, 0xaaaae54f, 0xfbfb16ed,
		0x4343c586, 0x4d4dd79a, 0x33335566, 0x85859411, 0x4545cf8a, 0xf9f910e9,
		0x02020604, 0x7f7f81fe, 0x5050f0a0, 0x3c3c4478, 0x9f9fba25, 0xa8a8e34b,
		0x5151f3a2, 0xa3a3fe5d, 0x4040c080, 0x8f8f8a05, 0x9292ad3f, 0x9d9dbc21,
		0x38384870, 0xf5f504f1, 0xbcbcdf63, 0xb6b6c177, 0xdada75af, 0x21216342,
		0x10103020, 0xffff1ae5, 0xf3f30efd, 0xd2d26dbf, 0xcdcd4c81, 0x0c0c1418,
		0x13133526, 0xecec2fc3, 0x5f5fe1be, 0x9797a235, 0x4444cc88, 0x1717392e,
		0xc4c45793, 0xa7a7f255, 0x7e7e82fc, 0x3d3d477a, 0x6464acc8, 0x5d5de7ba,
		0x19192b32, 0x737395e6, 0x6060a0c0, 0x81819819, 0x4f4fd19e, 0xdcdc7fa3,
		0x22226644, 0x2a2a7e54, 0x9090ab3b, 0x8888830b, 0x4646ca8c, 0xeeee29c7,
		0xb8b8d36b, 0x14143c28, 0xdede79a7, 0x5e5ee2bc, 0x0b0b1d16, 0xdbdb76ad,
		0xe0e03bdb, 0x32325664, 0x3a3a4e74, 0x0a0a1e14, 0x4949db92, 0x06060a0c,
		0x24246c48, 0x5c5ce4b8, 0xc2c25d9f, 0xd3d36ebd, 0xacacef43, 0x6262a6c4,
		0x9191a839, 0x9595a431, 0xe4e437d3, 0x79798bf2, 0xe7e732d5, 0xc8c8438b,
		0x3737596e, 0x6d6db7da, 0x8d8d8c01, 0xd5d564b1, 0x4e4ed29c, 0xa9a9e049,
		0x6c6cb4d8, 0x5656faac, 0xf4f407f3, 0xeaea25cf, 0x6565afca, 0x7a7a8ef4,
		0xaeaee947, 0x08081810, 0xbabad56f, 0x787888f0, 0x25256f4a, 0x2e2e725c,
		0x1c1c2438, 0xa6a6f157, 0xb4b4c773, 0xc6c65197, 0xe8e823cb, 0xdddd7ca1,
		0x74749ce8, 0x1f1f213e, 0x4b4bdd96, 0xbdbddc61, 0x8b8b860d, 0x8a8a850f,
		0x707090e0, 0x3e3e427c, 0xb5b5c471, 0x6666aacc, 0x4848d890, 0x03030506,
		0xf6f601f7, 0x0e0e121c, 0x6161a3c2, 0x35355f6a, 0x5757f9ae, 0xb9b9d069,
		0x86869117, 0xc1c15899, 0x1d1d273a, 0x9e9eb927, 0xe1e138d9, 0xf8f813eb,
		0x9898b32b, 0x11113322, 0x6969bbd2, 0xd9d970a9, 0x8e8e8907, 0x9494a733,
		0x9b9bb62d, 0x1e1e223c, 0x87879215, 0xe9e920c9, 0xcece4987, 0x5555ffaa,
		0x28287850, 0xdfdf7aa5, 0x8c8c8f03, 0xa1a1f859, 0x89898009, 0x0d0d171a,
		0xbfbfda65, 0xe6e631d7, 0x4242c684, 0x6868b8d0, 0x4141c382, 0x9999b029,
		0x2d2d775a, 0x0f0f111e, 0xb0b0cb7b, 0x5454fca8, 0xbbbbd66d, 0x16163a2c };

	static uint32_t rcon[] =
	{ 0x01000000, 0x02000000, 0x04000000, 0x08000000, 0x10000000,
	0x20000000, 0x40000000, 0x80000000, 0x1B000000, 0x36000000, };

	inline static void __init_enc_key(uint32_t ctx[44], int8_t key[16]) {
		uint32_t *p = reinterpret_cast<uint32_t *>(key);
		ctx[0] = be32toh(p[0]);
		ctx[1] = be32toh(p[1]);
		ctx[2] = be32toh(p[2]);
		ctx[3] = be32toh(p[3]);
		for (int32_t i = 0; i < 10; i++) {
			uint32_t t = ctx[i * 4 + 3];
			ctx[i * 4 + 4] = ctx[i * 4] ^ (ENC2[(t >> 16) & 0xff] & 0xff000000)
				^ (ENC3[(t >> 8) & 0xff] & 0xff0000) ^ (ENC0[t & 0xff] & 0xff00)
				^ (ENC1[(t >> 24)] & 0xff);
			ctx[i * 4 + 4] ^= rcon[i];
			ctx[i * 4 + 5] = ctx[i * 4 + 1] ^ ctx[i * 4 + 4];
			ctx[i * 4 + 6] = ctx[i * 4 + 2] ^ ctx[i * 4 + 5];
			ctx[i * 4 + 7] = ctx[i * 4 + 3] ^ ctx[i * 4 + 6];
		}
	}
	inline static void __encrypt(uint32_t ctx[44], uint32_t out[4],
		uint32_t in[4]) {
		uint32_t s0, s1, s2, s3, t0, t1, t2, t3;
		s0 = be32toh(in[0]) ^ ctx[0];
		s1 = be32toh(in[1]) ^ ctx[1];
		s2 = be32toh(in[2]) ^ ctx[2];
		s3 = be32toh(in[3]) ^ ctx[3];
		t0 = ENC0[s0 >> 24] ^ ENC1[(s1 >> 16) & 0xff] ^ ENC2[(s2 >> 8) & 0xff]
			^ ENC3[s3 & 0xff] ^ ctx[4];
		t1 = ENC0[s1 >> 24] ^ ENC1[(s2 >> 16) & 0xff] ^ ENC2[(s3 >> 8) & 0xff]
			^ ENC3[s0 & 0xff] ^ ctx[5];
		t2 = ENC0[s2 >> 24] ^ ENC1[(s3 >> 16) & 0xff] ^ ENC2[(s0 >> 8) & 0xff]
			^ ENC3[s1 & 0xff] ^ ctx[6];
		t3 = ENC0[s3 >> 24] ^ ENC1[(s0 >> 16) & 0xff] ^ ENC2[(s1 >> 8) & 0xff]
			^ ENC3[s2 & 0xff] ^ ctx[7];
		s0 = ENC0[t0 >> 24] ^ ENC1[(t1 >> 16) & 0xff] ^ ENC2[(t2 >> 8) & 0xff]
			^ ENC3[t3 & 0xff] ^ ctx[8];
		s1 = ENC0[t1 >> 24] ^ ENC1[(t2 >> 16) & 0xff] ^ ENC2[(t3 >> 8) & 0xff]
			^ ENC3[t0 & 0xff] ^ ctx[9];
		s2 = ENC0[t2 >> 24] ^ ENC1[(t3 >> 16) & 0xff] ^ ENC2[(t0 >> 8) & 0xff]
			^ ENC3[t1 & 0xff] ^ ctx[10];
		s3 = ENC0[t3 >> 24] ^ ENC1[(t0 >> 16) & 0xff] ^ ENC2[(t1 >> 8) & 0xff]
			^ ENC3[t2 & 0xff] ^ ctx[11];
		t0 = ENC0[s0 >> 24] ^ ENC1[(s1 >> 16) & 0xff] ^ ENC2[(s2 >> 8) & 0xff]
			^ ENC3[s3 & 0xff] ^ ctx[12];
		t1 = ENC0[s1 >> 24] ^ ENC1[(s2 >> 16) & 0xff] ^ ENC2[(s3 >> 8) & 0xff]
			^ ENC3[s0 & 0xff] ^ ctx[13];
		t2 = ENC0[s2 >> 24] ^ ENC1[(s3 >> 16) & 0xff] ^ ENC2[(s0 >> 8) & 0xff]
			^ ENC3[s1 & 0xff] ^ ctx[14];
		t3 = ENC0[s3 >> 24] ^ ENC1[(s0 >> 16) & 0xff] ^ ENC2[(s1 >> 8) & 0xff]
			^ ENC3[s2 & 0xff] ^ ctx[15];
		s0 = ENC0[t0 >> 24] ^ ENC1[(t1 >> 16) & 0xff] ^ ENC2[(t2 >> 8) & 0xff]
			^ ENC3[t3 & 0xff] ^ ctx[16];
		s1 = ENC0[t1 >> 24] ^ ENC1[(t2 >> 16) & 0xff] ^ ENC2[(t3 >> 8) & 0xff]
			^ ENC3[t0 & 0xff] ^ ctx[17];
		s2 = ENC0[t2 >> 24] ^ ENC1[(t3 >> 16) & 0xff] ^ ENC2[(t0 >> 8) & 0xff]
			^ ENC3[t1 & 0xff] ^ ctx[18];
		s3 = ENC0[t3 >> 24] ^ ENC1[(t0 >> 16) & 0xff] ^ ENC2[(t1 >> 8) & 0xff]
			^ ENC3[t2 & 0xff] ^ ctx[19];
		t0 = ENC0[s0 >> 24] ^ ENC1[(s1 >> 16) & 0xff] ^ ENC2[(s2 >> 8) & 0xff]
			^ ENC3[s3 & 0xff] ^ ctx[20];
		t1 = ENC0[s1 >> 24] ^ ENC1[(s2 >> 16) & 0xff] ^ ENC2[(s3 >> 8) & 0xff]
			^ ENC3[s0 & 0xff] ^ ctx[21];
		t2 = ENC0[s2 >> 24] ^ ENC1[(s3 >> 16) & 0xff] ^ ENC2[(s0 >> 8) & 0xff]
			^ ENC3[s1 & 0xff] ^ ctx[22];
		t3 = ENC0[s3 >> 24] ^ ENC1[(s0 >> 16) & 0xff] ^ ENC2[(s1 >> 8) & 0xff]
			^ ENC3[s2 & 0xff] ^ ctx[23];
		s0 = ENC0[t0 >> 24] ^ ENC1[(t1 >> 16) & 0xff] ^ ENC2[(t2 >> 8) & 0xff]
			^ ENC3[t3 & 0xff] ^ ctx[24];
		s1 = ENC0[t1 >> 24] ^ ENC1[(t2 >> 16) & 0xff] ^ ENC2[(t3 >> 8) & 0xff]
			^ ENC3[t0 & 0xff] ^ ctx[25];
		s2 = ENC0[t2 >> 24] ^ ENC1[(t3 >> 16) & 0xff] ^ ENC2[(t0 >> 8) & 0xff]
			^ ENC3[t1 & 0xff] ^ ctx[26];
		s3 = ENC0[t3 >> 24] ^ ENC1[(t0 >> 16) & 0xff] ^ ENC2[(t1 >> 8) & 0xff]
			^ ENC3[t2 & 0xff] ^ ctx[27];
		t0 = ENC0[s0 >> 24] ^ ENC1[(s1 >> 16) & 0xff] ^ ENC2[(s2 >> 8) & 0xff]
			^ ENC3[s3 & 0xff] ^ ctx[28];
		t1 = ENC0[s1 >> 24] ^ ENC1[(s2 >> 16) & 0xff] ^ ENC2[(s3 >> 8) & 0xff]
			^ ENC3[s0 & 0xff] ^ ctx[29];
		t2 = ENC0[s2 >> 24] ^ ENC1[(s3 >> 16) & 0xff] ^ ENC2[(s0 >> 8) & 0xff]
			^ ENC3[s1 & 0xff] ^ ctx[30];
		t3 = ENC0[s3 >> 24] ^ ENC1[(s0 >> 16) & 0xff] ^ ENC2[(s1 >> 8) & 0xff]
			^ ENC3[s2 & 0xff] ^ ctx[31];
		s0 = ENC0[t0 >> 24] ^ ENC1[(t1 >> 16) & 0xff] ^ ENC2[(t2 >> 8) & 0xff]
			^ ENC3[t3 & 0xff] ^ ctx[32];
		s1 = ENC0[t1 >> 24] ^ ENC1[(t2 >> 16) & 0xff] ^ ENC2[(t3 >> 8) & 0xff]
			^ ENC3[t0 & 0xff] ^ ctx[33];
		s2 = ENC0[t2 >> 24] ^ ENC1[(t3 >> 16) & 0xff] ^ ENC2[(t0 >> 8) & 0xff]
			^ ENC3[t1 & 0xff] ^ ctx[34];
		s3 = ENC0[t3 >> 24] ^ ENC1[(t0 >> 16) & 0xff] ^ ENC2[(t1 >> 8) & 0xff]
			^ ENC3[t2 & 0xff] ^ ctx[35];
		t0 = ENC0[s0 >> 24] ^ ENC1[(s1 >> 16) & 0xff] ^ ENC2[(s2 >> 8) & 0xff]
			^ ENC3[s3 & 0xff] ^ ctx[36];
		t1 = ENC0[s1 >> 24] ^ ENC1[(s2 >> 16) & 0xff] ^ ENC2[(s3 >> 8) & 0xff]
			^ ENC3[s0 & 0xff] ^ ctx[37];
		t2 = ENC0[s2 >> 24] ^ ENC1[(s3 >> 16) & 0xff] ^ ENC2[(s0 >> 8) & 0xff]
			^ ENC3[s1 & 0xff] ^ ctx[38];
		t3 = ENC0[s3 >> 24] ^ ENC1[(s0 >> 16) & 0xff] ^ ENC2[(s1 >> 8) & 0xff]
			^ ENC3[s2 & 0xff] ^ ctx[39];
		out[0] = htobe32(
			(ENC2[(t0 >> 24)] & 0xff000000)
			^ (ENC3[(t1 >> 16) & 0xff] & 0x00ff0000)
			^ (ENC0[(t2 >> 8) & 0xff] & 0x0000ff00)
			^ (ENC1[(t3)& 0xff] & 0x000000ff) ^ ctx[40]);
		out[1] = htobe32(
			(ENC2[(t1 >> 24)] & 0xff000000)
			^ (ENC3[(t2 >> 16) & 0xff] & 0x00ff0000)
			^ (ENC0[(t3 >> 8) & 0xff] & 0x0000ff00)
			^ (ENC1[(t0)& 0xff] & 0x000000ff) ^ ctx[41]);
		out[2] = htobe32(
			(ENC2[(t2 >> 24)] & 0xff000000)
			^ (ENC3[(t3 >> 16) & 0xff] & 0x00ff0000)
			^ (ENC0[(t0 >> 8) & 0xff] & 0x0000ff00)
			^ (ENC1[(t1)& 0xff] & 0x000000ff) ^ ctx[42]);
		out[3] = htobe32(
			(ENC2[(t3 >> 24)] & 0xff000000)
			^ (ENC3[(t0 >> 16) & 0xff] & 0x00ff0000)
			^ (ENC0[(t1 >> 8) & 0xff] & 0x0000ff00)
			^ (ENC1[(t2)& 0xff] & 0x000000ff) ^ ctx[43]);
	}

#ifdef LIMAX_TRY_USE_CPU_AES

} // namespace limax {

#ifdef __GNUC__
#include <x86intrin.h>  // 替代 intrin.h
#include <wmmintrin.h>  // AES 指令集
#else
#include <wmmintrin.h>
#include <intrin.h>
#endif

namespace limax {

	inline __m128i AES_128_ASSIST(__m128i temp1, __m128i temp2) {
		__m128i temp3;
		temp2 = _mm_shuffle_epi32(temp2, 0xff);
		temp3 = _mm_slli_si128(temp1, 0x4);
		temp1 = _mm_xor_si128(temp1, temp3);
		temp3 = _mm_slli_si128(temp3, 0x4);
		temp1 = _mm_xor_si128(temp1, temp3);
		temp3 = _mm_slli_si128(temp3, 0x4);
		temp1 = _mm_xor_si128(temp1, temp3);
		temp1 = _mm_xor_si128(temp1, temp2);
		return temp1;
	}

	inline static void aes_init_enc_key(uint32_t ctx[44], int8_t key[16]) {
		__m128i temp1, temp2;
		__m128i *Key_Schedule = (__m128i *) ctx;
		temp1 = _mm_loadu_si128((__m128i *) key);
		Key_Schedule[0] = temp1;
		temp2 = _mm_aeskeygenassist_si128(temp1, 0x1);
		temp1 = AES_128_ASSIST(temp1, temp2);
		Key_Schedule[1] = temp1;
		temp2 = _mm_aeskeygenassist_si128(temp1, 0x2);
		temp1 = AES_128_ASSIST(temp1, temp2);
		Key_Schedule[2] = temp1;
		temp2 = _mm_aeskeygenassist_si128(temp1, 0x4);
		temp1 = AES_128_ASSIST(temp1, temp2);
		Key_Schedule[3] = temp1;
		temp2 = _mm_aeskeygenassist_si128(temp1, 0x8);
		temp1 = AES_128_ASSIST(temp1, temp2);
		Key_Schedule[4] = temp1;
		temp2 = _mm_aeskeygenassist_si128(temp1, 0x10);
		temp1 = AES_128_ASSIST(temp1, temp2);
		Key_Schedule[5] = temp1;
		temp2 = _mm_aeskeygenassist_si128(temp1, 0x20);
		temp1 = AES_128_ASSIST(temp1, temp2);
		Key_Schedule[6] = temp1;
		temp2 = _mm_aeskeygenassist_si128(temp1, 0x40);
		temp1 = AES_128_ASSIST(temp1, temp2);
		Key_Schedule[7] = temp1;
		temp2 = _mm_aeskeygenassist_si128(temp1, 0x80);
		temp1 = AES_128_ASSIST(temp1, temp2);
		Key_Schedule[8] = temp1;
		temp2 = _mm_aeskeygenassist_si128(temp1, 0x1b);
		temp1 = AES_128_ASSIST(temp1, temp2);
		Key_Schedule[9] = temp1;
		temp2 = _mm_aeskeygenassist_si128(temp1, 0x36);
		temp1 = AES_128_ASSIST(temp1, temp2);
		Key_Schedule[10] = temp1;
	}

	inline static void aes_encrypt(uint32_t ctx[44], uint32_t out[4], uint32_t in[4]) {
		__m128i tmp;
		tmp = _mm_loadu_si128(((__m128i *) in));
		tmp = _mm_xor_si128(tmp, ((__m128i *) ctx)[0]);
		tmp = _mm_aesenc_si128(tmp, ((__m128i *) ctx)[1]);
		tmp = _mm_aesenc_si128(tmp, ((__m128i *) ctx)[2]);
		tmp = _mm_aesenc_si128(tmp, ((__m128i *) ctx)[3]);
		tmp = _mm_aesenc_si128(tmp, ((__m128i *) ctx)[4]);
		tmp = _mm_aesenc_si128(tmp, ((__m128i *) ctx)[5]);
		tmp = _mm_aesenc_si128(tmp, ((__m128i *) ctx)[6]);
		tmp = _mm_aesenc_si128(tmp, ((__m128i *) ctx)[7]);
		tmp = _mm_aesenc_si128(tmp, ((__m128i *) ctx)[8]);
		tmp = _mm_aesenc_si128(tmp, ((__m128i *) ctx)[9]);
		tmp = _mm_aesenclast_si128(tmp, ((__m128i *) ctx)[10]);
		_mm_storeu_si128(((__m128i *) out), tmp);
	}

} // namespace limax {

#ifdef LIMAX_OS_UNIX_FAMILY
#include <cpuid.h>
#endif

namespace limax {

	static int Check_CPU_support_AES()
	{
#ifdef LIMAX_OS_WINDOWS
		int a[4];
		__cpuid(a, 1);
		return a[2] & 0x2000000;
#endif

#ifdef LIMAX_OS_LINUX
		unsigned int a, b, c, d;
		__cpuid( 1, a, b, c, d);
		return bit_AES & c;
#endif

#ifdef LIMAX_OS_APPLE_FAMILY
		uint32_t a, b, c, d;
		__cpuid( 1, a, b, c, d);
		return c & bit_AESNI;
#endif
	}

	static struct AES_Function
	{
		void(*finitkey)(uint32_t*, int8_t*);
		void(*fencrypt)(uint32_t*, uint32_t*, uint32_t*);

		AES_Function() {
			if (Check_CPU_support_AES())
			{
#ifdef LIMAX_DEBUG
				printf("use cpu aes\n");
#endif
				finitkey = aes_init_enc_key;
				fencrypt = aes_encrypt;
			}
			else
			{
#ifdef LIMAX_DEBUG
				printf("no cpu aes\n");
#endif
				finitkey = __init_enc_key;
				fencrypt = __encrypt;
			}
		}
	} __dummy_aes_function__;

#else // #ifdef LIMAX_TRY_USE_CPU_AES


	static struct AES_Function
	{
		void(*finitkey)(uint32_t*, int8_t*);
		void(*fencrypt)(uint32_t*, uint32_t*, uint32_t*);

		AES_Function() {
#ifdef LIMAX_DEBUG
			printf("soft aes only\n");
#endif
			finitkey = __init_enc_key;
			fencrypt = __encrypt;
		}
	} __dummy_aes_function__;

#endif // #else // #ifdef LIMAX_TRY_USE_CPU_AES

	inline static void init_enc_key(uint32_t ctx[44], int8_t key[16]) {
		__dummy_aes_function__.finitkey(ctx, key);
	}

	inline static void encrypt(uint32_t ctx[44], uint32_t out[4], uint32_t in[4]) {
		__dummy_aes_function__.fencrypt(ctx, out, in);
	}

	Encrypt::Encrypt(std::shared_ptr<Codec> _sink, int8_t key[], int32_t len) :
		sink(_sink), count(0) {
		MD5 md5;
		md5.update(key, 0, len);
		int8_t* md5key = (int8_t*)md5.digest();
		init_enc_key(ctx, md5key);
		memcpy(iv, md5key, sizeof(iv));
	}

	void Encrypt::update(int8_t c) {
		int8_t *piv = reinterpret_cast<int8_t *>(iv);
		if (count < 0) {
			sink->update(piv[count++ + 16] ^= c);
			return;
		}
		in[count++] = c;
		if (count < 16)
			return;
		uint32_t *pin = reinterpret_cast<uint32_t *>(in);
		encrypt(ctx, iv, iv);
		iv[0] ^= pin[0];
		iv[1] ^= pin[1];
		iv[2] ^= pin[2];
		iv[3] ^= pin[3];
		sink->update(piv, 0, sizeof(iv));
		count = 0;
	}

	void Encrypt::update(int8_t data[], int32_t off, int32_t len) {
		int8_t *p = data + off;
		int8_t *piv = reinterpret_cast<int8_t *>(iv);
		int32_t i = 0;
		if (count < 0) {
			for (; i < len && count < 0; i++, count++) {
				sink->update(piv[count + 16] ^= p[i]);
			}
		}
		else if (count > 0) {
			for (; i < len && count < 16; i++, count++)
				in[count] = p[i];
			if (count < 16)
				return;
			uint32_t *pin = reinterpret_cast<uint32_t *>(in);
			encrypt(ctx, iv, iv);
			iv[0] ^= pin[0];
			iv[1] ^= pin[1];
			iv[2] ^= pin[2];
			iv[3] ^= pin[3];
			sink->update(piv, 0, sizeof(iv));
			count = 0;
		}
		int32_t nblocks = (len - i) >> 4;
		uint32_t *q = reinterpret_cast<uint32_t *>(p + i);
		for (int32_t j = 0; j < nblocks; j++) {
			encrypt(ctx, iv, iv);
			iv[0] ^= q[j * 4];
			iv[1] ^= q[j * 4 + 1];
			iv[2] ^= q[j * 4 + 2];
			iv[3] ^= q[j * 4 + 3];
			sink->update(piv, 0, sizeof(iv));
		}
		for (i += nblocks << 4; i < len; i++) {
			in[count++] = p[i];
		}
	}

	void Encrypt::flush() {
		if (count > 0) {
			encrypt(ctx, iv, iv);
			int8_t *piv = reinterpret_cast<int8_t *>(iv);
			for (int32_t i = 0; i < count; i++)
				sink->update(piv[i] ^= in[i]);
			count -= 16;
		}
		sink->flush();
	}

	Decrypt::Decrypt(std::shared_ptr<Codec> _sink, int8_t key[], int32_t len) :
		sink(_sink), count(0) {
		MD5 md5;
		md5.update(key, 0, len);
		int8_t* md5key = (int8_t*)md5.digest();
		init_enc_key(ctx, md5key);
		memcpy(iv, md5key, sizeof(iv));
	}

	void Decrypt::update(int8_t c) {
		int8_t *piv = reinterpret_cast<int8_t *>(iv);
		if (count < 0) {
			sink->update(piv[count + 16] ^ c);
			piv[count++ + 16] = c;
			return;
		}
		in[count++] = c;
		if (count < 16)
			return;
		encrypt(ctx, iv, iv);
		uint32_t *pin = reinterpret_cast<uint32_t *>(in);
		uint32_t out[4];
		out[0] = iv[0] ^ pin[0];
		out[1] = iv[1] ^ pin[1];
		out[2] = iv[2] ^ pin[2];
		out[3] = iv[3] ^ pin[3];
		iv[0] = pin[0];
		iv[1] = pin[1];
		iv[2] = pin[2];
		iv[3] = pin[3];
		sink->update(reinterpret_cast<int8_t *>(out), 0, sizeof(out));
		count = 0;
	}

	void Decrypt::update(int8_t data[], int32_t off, int32_t len) {
		int8_t *p = data + off;
		int8_t *piv = reinterpret_cast<int8_t *>(iv);
		int32_t i = 0;
		uint32_t out[4];
		if (count < 0) {
			for (; i < len && count < 0; i++, count++) {
				sink->update(piv[count + 16] ^ p[i]);
				piv[count + 16] = p[i];
			}
		}
		else if (count > 0) {
			for (; i < len && count < 16; i++, count++)
				in[count] = p[i];
			if (count < 16)
				return;
			encrypt(ctx, iv, iv);
			uint32_t *pin = reinterpret_cast<uint32_t *>(in);
			out[0] = iv[0] ^ pin[0];
			out[1] = iv[1] ^ pin[1];
			out[2] = iv[2] ^ pin[2];
			out[3] = iv[3] ^ pin[3];
			iv[0] = pin[0];
			iv[1] = pin[1];
			iv[2] = pin[2];
			iv[3] = pin[3];
			sink->update(reinterpret_cast<int8_t *>(out), 0, sizeof(out));
			count = 0;
		}
		int32_t nblocks = (len - i) >> 4;
		uint32_t *q = reinterpret_cast<uint32_t *>(p + i);
		for (int32_t j = 0; j < nblocks; j++) {
			encrypt(ctx, iv, iv);
			out[0] = iv[0] ^ q[j * 4];
			out[1] = iv[1] ^ q[j * 4 + 1];
			out[2] = iv[2] ^ q[j * 4 + 2];
			out[3] = iv[3] ^ q[j * 4 + 3];
			iv[0] = q[j * 4];
			iv[1] = q[j * 4 + 1];
			iv[2] = q[j * 4 + 2];
			iv[3] = q[j * 4 + 3];
			sink->update(reinterpret_cast<int8_t *>(out), 0, sizeof(out));
		}
		for (i += nblocks << 4; i < len; i++) {
			in[count++] = p[i];
		}
	}

	void Decrypt::flush() {
		if (count > 0) {
			encrypt(ctx, iv, iv);
			int8_t *piv = reinterpret_cast<int8_t *>(iv);
			for (int32_t i = 0; i < count; i++) {
				sink->update(piv[i] ^ in[i]);
				piv[i] = in[i];
			}
			count -= 16;
		}
		sink->flush();
	}
} // namespace limax {

