package Zeze.Net;


// RFC2118
public final class Decompress implements Codec {
	private final Codec sink;
	private int rem = 0;
	private int pos = 0;
	private int off = -1;
	private int len;
	private final byte hist[] = new byte[8192 * 3];
	private int hpos = 0;

	public static class UncompressException extends CodecException {
		private static final long serialVersionUID = -3837317279182358389L;
	}

	public Decompress(Codec sink) {
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
			throw new UncompressException();
		copy(hpos, hpos - off, len);
		sink.update(hist, hpos, len);
		hpos += len;
		drain();
	}

	private int bitCompute() {
		long val = (rem << (32 - pos)) & 0xffffffffl;
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
		} else {
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

	private void process() throws CodecException {
		long val = (rem << (32 - pos)) & 0xffffffffl;
		if (off < 0) {
			if (val < 0x80000000l) {
				output((byte) (val >> 24));
				pos -= 8;
			} else if (val < 0xc0000000l) {
				output((byte) ((val >> 23) | 0x80));
				pos -= 9;
			} else if (val < 0xe0000000l) {
				off = (int) (((val >> 16) & 0x1fff) + 320);
				pos -= 16;
			} else if (val < 0xf0000000l) {
				off = (int) (((val >> 20) & 0xff) + 64);
				pos -= 12;
			} else {
				off = (int) ((val >> 22) & 0x3f);
				pos -= 10;
				if (off == 0)
					off = -1;
			}
		} else {
			if (val < 0x80000000l) {
				len = 3;
				pos -= 1;
			} else if (val < 0xc0000000l) {
				len = (int) (4 | ((val >> 28) & 3));
				pos -= 4;
			} else if (val < 0xe0000000l) {
				len = (int) (8 | ((val >> 26) & 7));
				pos -= 6;
			} else if (val < 0xf0000000l) {
				len = (int) (16 | ((val >> 24) & 15));
				pos -= 8;
			} else if (val < 0xf8000000l) {
				len = (int) (32 | ((val >> 22) & 31));
				pos -= 10;
			} else if (val < 0xfc000000l) {
				len = (int) (64 | ((val >> 20) & 63));
				pos -= 12;
			} else if (val < 0xfe000000l) {
				len = (int) (128 | ((val >> 18) & 127));
				pos -= 14;
			} else if (val < 0xff000000l) {
				len = (int) (256 | ((val >> 16) & 255));
				pos -= 16;
			} else if (val < 0xff800000l) {
				len = (int) (512 | ((val >> 14) & 511));
				pos -= 18;
			} else if (val < 0xffc00000l) {
				len = (int) (1024 | ((val >> 12) & 1023));
				pos -= 20;
			} else if (val < 0xffe00000l) {
				len = (int) (2048 | ((val >> 10) & 2047));
				pos -= 22;
			} else if (val < 0xfff00000l) {
				len = (int) (4096 | ((val >> 8) & 4095));
				pos -= 24;
			} else
				throw new UncompressException();
			output(off, len);
			off = -1;
		}
	}

	@Override
	public void update(byte c) throws CodecException {
		pos += 8;
		rem = (rem << 8) | (c & 0xff);
		while (pos > 24)
			process();
	}

	@Override
	public void update(byte[] data, int off, int len) throws CodecException {
		len += off;
		for (int i = off; i < len; i++)
			update(data[i]);
	}

	@Override
	public void flush() throws CodecException {
		while (pos >= bitCompute())
			process();
		sink.flush();
	}
}