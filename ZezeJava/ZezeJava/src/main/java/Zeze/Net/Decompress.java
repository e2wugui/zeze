package Zeze.Net;

import org.jetbrains.annotations.NotNull;

// RFC2118
public class Decompress implements Codec {
	protected final @NotNull Codec sink;
	protected int rem;
	protected int pos;
	protected int off = -1;
	private final byte[] hist = new byte[8192 * 3];
	private int hpos;

	public static class DecompressException extends CodecException {
	}

	public Decompress(@NotNull Codec sink) {
		this.sink = sink;
	}

	private void drain() {
		if (hpos >= 8192 * 2) {
			System.arraycopy(hist, hpos - 8192, hist, 0, 8192);
			hpos = 8192;
		}
	}

	private void copy(int dstPos, int srcPos, int length) {
		for (int i = 0; i < length; i++)
			hist[dstPos++] = hist[srcPos++];
	}

	private void output(byte c) throws CodecException {
		sink.update(hist[hpos++] = c);
		drain();
	}

	private void output(int off, int len) throws CodecException {
		if (hpos < off)
			throw new DecompressException();
		copy(hpos, hpos - off, len);
		sink.update(hist, hpos, len);
		hpos += len;
		drain();
	}

	private int bitCompute() {
		long val = ((long)rem << (32 - pos)) & 0xffff_ffffL;
		if (off < 0) {
			if (val < 0x8000_0000L)
				return 8;
			if (val < 0xc000_0000L)
				return 9;
			if (val < 0xe000_0000L)
				return 16;
			if (val < 0xf000_0000L)
				return 12;
			return 10;
		}
		if (val < 0x8000_0000L)
			return 1;
		if (val < 0xc000_0000L)
			return 4;
		if (val < 0xe000_0000L)
			return 6;
		if (val < 0xf000_0000L)
			return 8;
		if (val < 0xf800_0000L)
			return 10;
		if (val < 0xfc00_0000L)
			return 12;
		if (val < 0xfe00_0000L)
			return 14;
		if (val < 0xff00_0000L)
			return 16;
		if (val < 0xff80_0000L)
			return 18;
		if (val < 0xffc0_0000L)
			return 20;
		if (val < 0xffe0_0000L)
			return 22;
		if (val < 0xfff0_0000L)
			return 24;
		return 32;
	}

	private void process() throws CodecException {
		long val = ((long)rem << (32 - pos)) & 0xffff_ffffL;
		if (off < 0) {
			if (val < 0x8000_0000L) { // 0xxx xxxx
				output((byte)(val >> 24));
				pos -= 8;
			} else if (val < 0xc000_0000L) { // 1 0xxx xxxx
				output((byte)((val >> 23) | 0x80));
				pos -= 9;
			} else if (val < 0xe000_0000L) { // 110x xxxx xxxx xxxx
				off = (((int)val >> 16) & 0x1fff) + 320;
				pos -= 16;
			} else if (val < 0xf000_0000L) { // 1110 xxxx xxxx
				off = (((int)val >> 20) & 0xff) + 64;
				pos -= 12;
			} else { // 11 11xx xxxx
				off = ((int)val >> 22) & 0x3f;
				pos -= 10;
				if (off == 0)
					off = -1;
			}
		} else {
			int len;
			if (val < 0x8000_0000L) { // 0
				len = 3;
				pos -= 1;
			} else if (val < 0xc000_0000L) { // 10xx
				len = 0x4 | (((int)val >> 28) & 0x3);
				pos -= 4;
			} else if (val < 0xe000_0000L) { // 11 0xxx
				len = 0x8 | (((int)val >> 26) & 0x7);
				pos -= 6;
			} else if (val < 0xf000_0000L) { // 1110 xxxx
				len = 0x10 | (((int)val >> 24) & 0xf);
				pos -= 8;
			} else if (val < 0xf800_0000L) { // 11 110x xxxx
				len = 0x20 | (((int)val >> 22) & 0x1f);
				pos -= 10;
			} else if (val < 0xfc00_0000L) { // 1111 10xx xxxx
				len = 0x40 | (((int)val >> 20) & 0x3f);
				pos -= 12;
			} else if (val < 0xfe00_0000L) { // 11 1111 0xxx xxxx
				len = 0x80 | (((int)val >> 18) & 0x7f);
				pos -= 14;
			} else if (val < 0xff00_0000L) { // 1111 1110 xxxx xxxx
				len = 0x100 | (((int)val >> 16) & 0xff);
				pos -= 16;
			} else if (val < 0xff80_0000L) { // 11 1111 110x xxxx xxxx
				len = 0x200 | (((int)val >> 14) & 0x1ff);
				pos -= 18;
			} else if (val < 0xffc0_0000L) { // 1111 1111 10xx xxxx xxxx
				len = 0x400 | (((int)val >> 12) & 0x3ff);
				pos -= 20;
			} else if (val < 0xffe0_0000L) { // 11 1111 1111 0xxx xxxx xxxx
				len = 0x800 | (((int)val >> 10) & 0x7ff);
				pos -= 22;
			} else if (val < 0xfff0_0000L) { // 1111 1111 1110 xxxx xxxx xxxx
				len = 0x1000 | (((int)val >> 8) & 0xfff);
				pos -= 24;
			} else
				throw new DecompressException();
			output(off, len);
			off = -1;
		}
	}

	@Override
	public void update(byte c) throws CodecException {
		pos += 8;
		rem = (rem << 8) | (c & 0xff);
		while (pos >= 24) // pos=[24,31]
			process(); // pos-=[1,24]
	}

	@Override
	public void flush() throws CodecException {
		while (pos >= bitCompute()) // pos=[0,23], bitCompute()=[1,24]
			process();
		sink.flush();
	}
}
