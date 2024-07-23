package Zeze.Net;

import java.util.Arrays;
import org.jetbrains.annotations.NotNull;

// RFC2118
public class Compress implements Codec {
	private final @NotNull Codec sink;
	private int pos;
	private int rem;
	private final byte[] dict = new byte[8192];
	private final short[] hash = new short[65536];
	private int idx;
	private int match_idx;
	private int match_off = -1;
	private int match_len;
	private boolean flushed = true;

	public Compress(@NotNull Codec sink) {
		this.sink = sink;
		Arrays.fill(hash, (short)-1);
	}

	protected int getPos() {
		return pos;
	}

	protected void putBits(int val, int nbits) throws CodecException {
		pos += nbits;
		rem |= val << (32 - pos);
		while (pos > 7) {
			sink.update((byte)(rem >> 24));
			pos -= 8;
			rem <<= 8;
		}
	}

	private void putLiteral(byte c) throws CodecException {
		if (c >= 0)
			putBits(c, 8); // 0xxx xxxx
		else
			putBits(c & 0x7f | 0x100, 9); // 1 0xxx xxxx
	}

	private void putTuple(int off, int len) throws CodecException {
		if (off < 64)
			putBits(0x3c0 | off, 10); // 11 11xx xxxx
		else if (off < 320)
			putBits(0xe00 | (off - 64), 12); // 1110 xxxx xxxx
		else
			putBits(0xc000 | (off - 320), 16); // 110x xxxx xxxx xxxx
		if (len < 4) // len == 3
			putBits(0, 1); // 0
		else if (len < 8)
			putBits(0x08 | (len & 0x03), 4); // 10xx
		else if (len < 16)
			putBits(0x30 | (len & 0x07), 6); // 11 0xxx
		else if (len < 32)
			putBits(0xe0 | (len & 0x0f), 8); // 1110 xxxx
		else if (len < 64)
			putBits(0x3c0 | (len & 0x1f), 10); // 11 110x xxxx
		else if (len < 128)
			putBits(0xf80 | (len & 0x3f), 12); // 1111 10xx xxxx
		else if (len < 256)
			putBits(0x3f00 | (len & 0x7f), 14); // 11 1111 0xxx xxxx
		else if (len < 512)
			putBits(0xfe00 | (len & 0xff), 16); // 1111 1110 xxxx xxxx
		else if (len < 1024)
			putBits(0x3fc00 | (len & 0x1ff), 18); // 11 1111 110x xxxx xxxx
		else if (len < 2048)
			putBits(0xff800 | (len & 0x3ff), 20); // 1111 1111 10xx xxxx xxxx
		else if (len < 4096)
			putBits(0x3ff000 | (len & 0x7ff), 22); // 11 1111 1111 0xxx xxxx xxxx
		else if (len < 8192)
			putBits(0xffe000 | (len & 0xfff), 24); // 1111 1111 1110 xxxx xxxx xxxx
	}

	private void _flush() throws CodecException {
		if (match_off > 0) {
			if (match_len == 2) {
				putLiteral(dict[match_idx - 2]);
				putLiteral(dict[match_idx - 1]);
			} else
				putTuple(match_off, match_len);
			match_off = -1;
		} else
			putLiteral(dict[idx - 1]);
		flushed = true;
	}

	@Override
	public void update(byte c) throws CodecException {
		if (idx == dict.length) {
			if (!flushed)
				_flush();
			Arrays.fill(hash, (short)-1);
			idx = 0;
		}
		dict[idx++] = c;
		if (flushed) {
			flushed = false;
			return;
		}
		int key = ((c & 0xff) << 8) | dict[idx - 2] & 0xff;
		int tmp = hash[key];
		hash[key] = (short)idx;
		if (match_off > 0) {
			if (dict[match_idx] == c) {
				match_idx++;
				match_len++;
			} else {
				if (match_len == 2) {
					putLiteral(dict[match_idx - 2]);
					putLiteral(dict[match_idx - 1]);
				} else
					putTuple(match_off, match_len);
				match_off = -1;
			}
		} else {
			if (tmp != -1) {
				match_idx = tmp;
				match_off = idx - tmp;
				match_len = 2;
			} else
				putLiteral(dict[idx - 2]);
		}
	}

	@Override
	public void flush() throws CodecException {
		if (!flushed) {
			_flush();
			if (pos > 0)
				putBits(0x3c0, 10); // 11 1100 0000
		}
		sink.flush();
	}
}
