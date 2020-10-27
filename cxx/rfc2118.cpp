#include "common.h"

namespace limax {

	void RFC2118Encode::putBits(int32_t val, int32_t nbits) {
		pos += nbits;
		rem |= val << (32 - pos);
		while (pos > 7) {
			sink->update(rem >> 24);
			pos -= 8;
			rem <<= 8;
		}
	}

	void RFC2118Encode::putLiteral(int8_t c) {
		if (c >= 0)
			putBits(c, 8);
		else
			putBits((c & 0x7f) | 0x100, 9);
	}

	void RFC2118Encode::putTuple(int32_t off, int32_t len) {
		if (off < 64)
			putBits(0x3c0 | off, 10);
		else if (off < 320)
			putBits(0xe00 | (off - 64), 12);
		else
			putBits(0xc000 | (off - 320), 16);
		if (len < 4)
			putBits(0, 1);
		else if (len < 8)
			putBits(0x08 | (len & 0x03), 4);
		else if (len < 16)
			putBits(0x30 | (len & 0x07), 6);
		else if (len < 32)
			putBits(0xe0 | (len & 0x0f), 8);
		else if (len < 64)
			putBits(0x3c0 | (len & 0x1f), 10);
		else if (len < 128)
			putBits(0xf80 | (len & 0x3f), 12);
		else if (len < 256)
			putBits(0x3f00 | (len & 0x7f), 14);
		else if (len < 512)
			putBits(0xfe00 | (len & 0xff), 16);
		else if (len < 1024)
			putBits(0x3fc00 | (len & 0x1ff), 18);
		else if (len < 2048)
			putBits(0xff800 | (len & 0x3ff), 20);
		else if (len < 4096)
			putBits(0x3ff000 | (len & 0x7ff), 22);
		else if (len < 8192)
			putBits(0xffe000 | (len & 0xfff), 24);
	}

	void RFC2118Encode::_flush() {
		if (match_off > 0) {
			if (match_len == 2) {
				putLiteral(dict[match_idx - 2]);
				putLiteral(dict[match_idx - 1]);
			}
			else
				putTuple(match_off, match_len);
			match_off = -1;
		}
		else
			putLiteral(dict[idx - 1]);
		flushed = true;
	}

	RFC2118Encode::RFC2118Encode(std::shared_ptr<Codec> _sink) :
		sink(_sink), pos(0), rem(0), idx(0), match_off(-1), flushed(true) {
		memset(hash, 0xff, sizeof(hash));
	}

	void RFC2118Encode::update(int8_t c) {
		if (idx == sizeof(dict) / sizeof(dict[0])) {
			if (!flushed)
				_flush();
			memset(hash, 0xff, sizeof(hash));
			idx = 0;
		}
		dict[idx++] = c;
		if (flushed) {
			flushed = false;
			return;
		}
		int32_t key = ((c & 0xff) << 8) | (dict[idx - 2] & 0xff);
		int32_t tmp = hash[key];
		hash[key] = idx;
		if (match_off > 0) {
			if (dict[match_idx] == c) {
				match_idx++;
				match_len++;
			}
			else {
				if (match_len == 2) {
					putLiteral(dict[match_idx - 2]);
					putLiteral(dict[match_idx - 1]);
				}
				else
					putTuple(match_off, match_len);
				match_off = -1;
			}
		}
		else {
			if (tmp != -1) {
				match_idx = tmp;
				match_off = idx - tmp;
				match_len = 2;
			}
			else
				putLiteral(dict[idx - 2]);
		}
	}

	void RFC2118Encode::update(int8_t data[], int32_t off, int32_t len) {
		int8_t *p = data + off;
		for (int32_t i = 0; i < len; i++)
			update(p[i]);
	}

	void RFC2118Encode::flush() {
		if (!flushed) {
			_flush();
			if (pos > 0)
				putBits(0x3c0, 10);
		}
		sink->flush();
	}

	void RFC2118Decode::drain() {
		if (hpos >= 8192 * 2) {
			memmove(hist, hist + (hpos - 8192), 8192);
			hpos = 8192;
		}
	}

	void RFC2118Decode::copy(int32_t dstPos, int32_t srcPos, int32_t length) {
		for (int i = 0; i < length; i++)
			hist[dstPos++] = hist[srcPos++];
	}

	void RFC2118Decode::output(int8_t c) {
		sink->update(hist[hpos++] = c);
		drain();
	}

	void RFC2118Decode::output(int32_t off, int32_t len) {
		if (hpos < off)
			throw UncompressException();
		copy(hpos, hpos - off, len);
		sink->update(hist, hpos, len);
		hpos += len;
		drain();
	}

	int32_t RFC2118Decode::bitCompute() {
		uint32_t val = static_cast<uint32_t>(rem << (32 - pos));
		if (off < 0) {
			if (val < 0x80000000l)
				return 8;
			else if (val < 0xc0000000l)
				return 9;
			else if (val < 0xe0000000l)
				return 16;
			else if (val < 0xf0000000l)
				return 12;
			else
				return 10;
		}
		else {
			if (val < 0x80000000l)
				return 1;
			else if (val < 0xc0000000l)
				return 4;
			else if (val < 0xe0000000l)
				return 6;
			else if (val < 0xf0000000l)
				return 8;
			else if (val < 0xf8000000l)
				return 10;
			else if (val < 0xfc000000l)
				return 12;
			else if (val < 0xfe000000l)
				return 14;
			else if (val < 0xff000000l)
				return 16;
			else if (val < 0xff800000l)
				return 18;
			else if (val < 0xffc00000l)
				return 20;
			else if (val < 0xffe00000l)
				return 22;
			else if (val < 0xfff00000l)
				return 24;
			else
				return 32;
		}
	}

	void RFC2118Decode::process() {
		uint32_t val = static_cast<uint32_t>(rem << (32 - pos));
		if (off < 0) {
			if (val < 0x80000000l) {
				output(val >> 24);
				pos -= 8;
			}
			else if (val < 0xc0000000l) {
				output((val >> 23) | 0x80);
				pos -= 9;
			}
			else if (val < 0xe0000000l) {
				off = ((val >> 16) & 0x1fff) + 320;
				pos -= 16;
			}
			else if (val < 0xf0000000l) {
				off = ((val >> 20) & 0xff) + 64;
				pos -= 12;
			}
			else {
				off = (val >> 22) & 0x3f;
				pos -= 10;
				if (off == 0)
					off = -1;
			}
		}
		else {
			if (val < 0x80000000l) {
				len = 3;
				pos -= 1;
			}
			else if (val < 0xc0000000l) {
				len = 4 | ((val >> 28) & 3);
				pos -= 4;
			}
			else if (val < 0xe0000000l) {
				len = 8 | ((val >> 26) & 7);
				pos -= 6;
			}
			else if (val < 0xf0000000l) {
				len = 16 | ((val >> 24) & 15);
				pos -= 8;
			}
			else if (val < 0xf8000000l) {
				len = 32 | ((val >> 22) & 31);
				pos -= 10;
			}
			else if (val < 0xfc000000l) {
				len = 64 | ((val >> 20) & 63);
				pos -= 12;
			}
			else if (val < 0xfe000000l) {
				len = 128 | ((val >> 18) & 127);
				pos -= 14;
			}
			else if (val < 0xff000000l) {
				len = 256 | ((val >> 16) & 255);
				pos -= 16;
			}
			else if (val < 0xff800000l) {
				len = 512 | ((val >> 14) & 511);
				pos -= 18;
			}
			else if (val < 0xffc00000l) {
				len = 1024 | ((val >> 12) & 1023);
				pos -= 20;
			}
			else if (val < 0xffe00000l) {
				len = 2048 | ((val >> 10) & 2047);
				pos -= 22;
			}
			else if (val < 0xfff00000l) {
				len = 4096 | ((val >> 8) & 4095);
				pos -= 24;
			}
			else
				throw UncompressException();
			output(off, len);
			off = -1;
		}
	}

	RFC2118Decode::RFC2118Decode(std::shared_ptr<Codec> _sink) :
		sink(_sink), rem(0), pos(0), off(-1), hpos(0) {
	}

	void RFC2118Decode::update(int8_t c) {
		pos += 8;
		rem = (rem << 8) | (c & 0xff);
		while (pos > 24)
			process();
	}

	void RFC2118Decode::update(int8_t data[], int32_t off, int32_t len) {
		int8_t *p = data + off;
		for (int32_t i = 0; i < len; i++)
			update(p[i]);
	}

	void RFC2118Decode::flush() {
		while (pos >= bitCompute())
			process();
		sink->flush();
	}

} // namespace limax {


