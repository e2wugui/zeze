package Zeze.Net;

import Zeze.Serialize.*;
import Zeze.*;
import java.io.*;

// RFC2118
public final class Decompress implements Codec {
	private Codec sink;

	private int rem = 0;
	private int pos = 0;
	private int off = -1;
	private int len;
//C# TO JAVA CONVERTER WARNING: Unsigned integer types have no direct equivalent in Java:
//ORIGINAL LINE: private byte[] hist = new byte[8192 * 3];
	private byte[] hist = new byte[8192 * 3];
	private int hpos = 0;
	public final static class UncompressException extends RuntimeException {
	}

	public Decompress(Codec sink) {
		this.sink = sink;
	}

	private void drain() {
		if (hpos >= 8192 * 2) {
			Buffer.BlockCopy(hist, hpos - 8192, hist, 0, 8192);
			hpos = 8192;
		}
	}

	private void copy(int dstPos, int srcPos, int length) {
		for (int i = 0; i < length; i++) {
			hist[dstPos++] = hist[srcPos++];
		}
	}

//C# TO JAVA CONVERTER WARNING: Unsigned integer types have no direct equivalent in Java:
//ORIGINAL LINE: private void output(byte c)
	private void output(byte c) {
		sink.update(hist[hpos++] = c);
		drain();
	}
	private void output(int off, int len) {
		if (hpos < off) {
			throw new UncompressException();
		}
		copy(hpos, hpos - off, len);
		sink.update(hist, hpos, len);
		hpos += len;
		drain();
	}

	private int bitCompute() {
		long val = (rem << (32 - pos)) & 0xffffffffL;
		if (off < 0) {
			if (val < 0x80000000L) {
				return 8;
			}
			else if (val < 0xc0000000L) {
				return 9;
			}
			else if (val < 0xe0000000L) {
				return 16;
			}
			else if (val < 0xf0000000L) {
				return 12;
			}
			else {
				return 10;
			}
		}
		else {
			if (val < 0x80000000L) {
				return 1;
			}
			else if (val < 0xc0000000L) {
				return 4;
			}
			else if (val < 0xe0000000L) {
				return 6;
			}
			else if (val < 0xf0000000L) {
				return 8;
			}
			else if (val < 0xf8000000L) {
				return 10;
			}
			else if (val < 0xfc000000L) {
				return 12;
			}
			else if (val < 0xfe000000L) {
				return 14;
			}
			else if (val < 0xff000000L) {
				return 16;
			}
			else if (val < 0xff800000L) {
				return 18;
			}
			else if (val < 0xffc00000L) {
				return 20;
			}
			else if (val < 0xffe00000L) {
				return 22;
			}
			else if (val < 0xfff00000L) {
				return 24;
			}
			else {
				return 32;
			}
		}
	}
	private void process() {
		long val = (rem << (32 - pos)) & 0xffffffffL;
		if (off < 0) {
			if (val < 0x80000000L) {
//C# TO JAVA CONVERTER WARNING: The right shift operator was not replaced by Java's logical right shift operator since the left operand was not confirmed to be of an unsigned type, but you should review whether the logical right shift operator (>>>) is more appropriate:
//C# TO JAVA CONVERTER WARNING: Unsigned integer types have no direct equivalent in Java:
//ORIGINAL LINE: output((byte)(val >> 24));
				output((byte)(val >> 24));
				pos -= 8;
			}
			else if (val < 0xc0000000L) {
//C# TO JAVA CONVERTER WARNING: The right shift operator was not replaced by Java's logical right shift operator since the left operand was not confirmed to be of an unsigned type, but you should review whether the logical right shift operator (>>>) is more appropriate:
//C# TO JAVA CONVERTER WARNING: Unsigned integer types have no direct equivalent in Java:
//ORIGINAL LINE: output((byte)((val >> 23) | 0x80));
				output((byte)((val >> 23) | 0x80));
				pos -= 9;
			}
			else if (val < 0xe0000000L) {
//C# TO JAVA CONVERTER WARNING: The right shift operator was not replaced by Java's logical right shift operator since the left operand was not confirmed to be of an unsigned type, but you should review whether the logical right shift operator (>>>) is more appropriate:
				off = (int)(((val >> 16) & 0x1fff) + 320);
				pos -= 16;
			}
			else if (val < 0xf0000000L) {
//C# TO JAVA CONVERTER WARNING: The right shift operator was not replaced by Java's logical right shift operator since the left operand was not confirmed to be of an unsigned type, but you should review whether the logical right shift operator (>>>) is more appropriate:
				off = (int)(((val >> 20) & 0xff) + 64);
				pos -= 12;
			}
			else {
//C# TO JAVA CONVERTER WARNING: The right shift operator was not replaced by Java's logical right shift operator since the left operand was not confirmed to be of an unsigned type, but you should review whether the logical right shift operator (>>>) is more appropriate:
				off = (int)((val >> 22) & 0x3f);
				pos -= 10;
				if (off == 0) {
					off = -1;
				}
			}
		}
		else {
			if (val < 0x80000000L) {
				len = 3;
				pos -= 1;
			}
			else if (val < 0xc0000000L) {
//C# TO JAVA CONVERTER WARNING: The right shift operator was not replaced by Java's logical right shift operator since the left operand was not confirmed to be of an unsigned type, but you should review whether the logical right shift operator (>>>) is more appropriate:
				len = (int)(4 | ((val >> 28) & 3));
				pos -= 4;
			}
			else if (val < 0xe0000000L) {
//C# TO JAVA CONVERTER WARNING: The right shift operator was not replaced by Java's logical right shift operator since the left operand was not confirmed to be of an unsigned type, but you should review whether the logical right shift operator (>>>) is more appropriate:
				len = (int)(8 | ((val >> 26) & 7));
				pos -= 6;
			}
			else if (val < 0xf0000000L) {
//C# TO JAVA CONVERTER WARNING: The right shift operator was not replaced by Java's logical right shift operator since the left operand was not confirmed to be of an unsigned type, but you should review whether the logical right shift operator (>>>) is more appropriate:
				len = (int)(16 | ((val >> 24) & 15));
				pos -= 8;
			}
			else if (val < 0xf8000000L) {
//C# TO JAVA CONVERTER WARNING: The right shift operator was not replaced by Java's logical right shift operator since the left operand was not confirmed to be of an unsigned type, but you should review whether the logical right shift operator (>>>) is more appropriate:
				len = (int)(32 | ((val >> 22) & 31));
				pos -= 10;
			}
			else if (val < 0xfc000000L) {
//C# TO JAVA CONVERTER WARNING: The right shift operator was not replaced by Java's logical right shift operator since the left operand was not confirmed to be of an unsigned type, but you should review whether the logical right shift operator (>>>) is more appropriate:
				len = (int)(64 | ((val >> 20) & 63));
				pos -= 12;
			}
			else if (val < 0xfe000000L) {
//C# TO JAVA CONVERTER WARNING: The right shift operator was not replaced by Java's logical right shift operator since the left operand was not confirmed to be of an unsigned type, but you should review whether the logical right shift operator (>>>) is more appropriate:
				len = (int)(128 | ((val >> 18) & 127));
				pos -= 14;
			}
			else if (val < 0xff000000L) {
//C# TO JAVA CONVERTER WARNING: The right shift operator was not replaced by Java's logical right shift operator since the left operand was not confirmed to be of an unsigned type, but you should review whether the logical right shift operator (>>>) is more appropriate:
				len = (int)(256 | ((val >> 16) & 255));
				pos -= 16;
			}
			else if (val < 0xff800000L) {
//C# TO JAVA CONVERTER WARNING: The right shift operator was not replaced by Java's logical right shift operator since the left operand was not confirmed to be of an unsigned type, but you should review whether the logical right shift operator (>>>) is more appropriate:
				len = (int)(512 | ((val >> 14) & 511));
				pos -= 18;
			}
			else if (val < 0xffc00000L) {
//C# TO JAVA CONVERTER WARNING: The right shift operator was not replaced by Java's logical right shift operator since the left operand was not confirmed to be of an unsigned type, but you should review whether the logical right shift operator (>>>) is more appropriate:
				len = (int)(1024 | ((val >> 12) & 1023));
				pos -= 20;
			}
			else if (val < 0xffe00000L) {
//C# TO JAVA CONVERTER WARNING: The right shift operator was not replaced by Java's logical right shift operator since the left operand was not confirmed to be of an unsigned type, but you should review whether the logical right shift operator (>>>) is more appropriate:
				len = (int)(2048 | ((val >> 10) & 2047));
				pos -= 22;
			}
			else if (val < 0xfff00000L) {
//C# TO JAVA CONVERTER WARNING: The right shift operator was not replaced by Java's logical right shift operator since the left operand was not confirmed to be of an unsigned type, but you should review whether the logical right shift operator (>>>) is more appropriate:
				len = (int)(4096 | ((val >> 8) & 4095));
				pos -= 24;
			}
			else {
				throw new UncompressException();
			}
			output(off, len);
			off = -1;
		}
	}
//C# TO JAVA CONVERTER WARNING: Unsigned integer types have no direct equivalent in Java:
//ORIGINAL LINE: public void update(byte c)
	public void update(byte c) {
		pos += 8;
		rem = (rem << 8) | (c & 0xff);
		while (pos > 24) {
			process();
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
		while (pos >= bitCompute()) {
			process();
		}
		sink.flush();
	}

	public void close() throws IOException {
		sink.close();
	}
}