package Zeze.Net;

import Zeze.Serialize.*;
import Zeze.*;
import java.io.*;

// RFC2118
public final class Compress implements Codec {
	private final Codec sink;

	private int pos = 0;
	private int rem = 0;
//C# TO JAVA CONVERTER WARNING: Unsigned integer types have no direct equivalent in Java:
//ORIGINAL LINE: private byte[] dict = new byte[8192];
	private byte[] dict = new byte[8192];
	private short[] hash = new short[65536];
	private int idx = 0;
	private int match_idx;
	private int match_off = -1;
	private int match_len;
	private boolean flushed = true;

	public Compress(Codec sink) {
		this.sink = sink;
		for (int i = 0; i < hash.length; i++) {
			hash[i] = -1;
		}
	}
	private void putBits(int val, int nbits) {
		pos += nbits;
		rem |= val << (32 - pos);
		while (pos > 7) {
//C# TO JAVA CONVERTER WARNING: The right shift operator was not replaced by Java's logical right shift operator since the left operand was not confirmed to be of an unsigned type, but you should review whether the logical right shift operator (>>>) is more appropriate:
//C# TO JAVA CONVERTER WARNING: Unsigned integer types have no direct equivalent in Java:
//ORIGINAL LINE: sink.update((byte)(rem >> 24));
			sink.update((byte)(rem >> 24));
			pos -= 8;
			rem <<= 8;
		}
	}
//C# TO JAVA CONVERTER WARNING: Unsigned integer types have no direct equivalent in Java:
//ORIGINAL LINE: private void putLiteral(byte c)
	private void putLiteral(byte c) {
		if ((c & 0x80) == 0) {
			putBits(c, 8);
		}
		else {
			putBits(c & 0x7f | 0x100, 9);
		}
	}
	private void putTuple(int off, int len) {
		if (off < 64) {
			putBits(0x3c0 | off, 10);
		}
		else if (off < 320) {
			putBits(0xe00 | (off - 64), 12);
		}
		else {
			putBits(0xc000 | (off - 320), 16);
		}
		if (len < 4) {
			putBits(0, 1);
		}
		else if (len < 8) {
			putBits(0x08 | (len & 0x03), 4);
		}
		else if (len < 16) {
			putBits(0x30 | (len & 0x07), 6);
		}
		else if (len < 32) {
			putBits(0xe0 | (len & 0x0f), 8);
		}
		else if (len < 64) {
			putBits(0x3c0 | (len & 0x1f), 10);
		}
		else if (len < 128) {
			putBits(0xf80 | (len & 0x3f), 12);
		}
		else if (len < 256) {
			putBits(0x3f00 | (len & 0x7f), 14);
		}
		else if (len < 512) {
			putBits(0xfe00 | (len & 0xff), 16);
		}
		else if (len < 1024) {
			putBits(0x3fc00 | (len & 0x1ff), 18);
		}
		else if (len < 2048) {
			putBits(0xff800 | (len & 0x3ff), 20);
		}
		else if (len < 4096) {
			putBits(0x3ff000 | (len & 0x7ff), 22);
		}
		else if (len < 8192) {
			putBits(0xffe000 | (len & 0xfff), 24);
		}
	}

	private void _flush() {
		if (match_off > 0) {
			if (match_len == 2) {
				putLiteral(dict[match_idx - 2]);
				putLiteral(dict[match_idx - 1]);
			}
			else {
				putTuple(match_off, match_len);
			}
			match_off = -1;
		}
		else {
			putLiteral(dict[idx - 1]);
		}
		flushed = true;
	}

//C# TO JAVA CONVERTER WARNING: Unsigned integer types have no direct equivalent in Java:
//ORIGINAL LINE: public void update(byte c)
	public void update(byte c) {
		if (idx == dict.length) {
			if (!flushed) {
				_flush();
			}
			for (int i = 0; i < hash.length; i++) {
				hash[i] = -1;
			}
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
			}
			else {
				if (match_len == 2) {
					putLiteral(dict[match_idx - 2]);
					putLiteral(dict[match_idx - 1]);
				}
				else {
					putTuple(match_off, match_len);
				}
				match_off = -1;
			}
		}
		else {
			if (tmp != -1) {
				match_idx = tmp;
				match_off = idx - tmp;
				match_len = 2;
			}
			else {
				putLiteral(dict[idx - 2]);
			}
		}
	}

//C# TO JAVA CONVERTER WARNING: Unsigned integer types have no direct equivalent in Java:
//ORIGINAL LINE: public void update(byte[] data, int off, int len)
	public void update(byte[] data, int off, int len) {
		len += off;
		for (int i = off; i < len; i++) {
			update(data[i]);
		}
	}

	public void flush() {
		if (!flushed) {
			_flush();
			if (pos > 0) {
				putBits(0x3c0, 10);
			}
		}
		sink.flush();
	}

	public void close() throws IOException {
		sink.close();
	}
}