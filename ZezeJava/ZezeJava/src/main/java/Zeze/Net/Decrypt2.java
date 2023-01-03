package Zeze.Net;

import static Zeze.Net.Encrypt2.BLOCK_SIZE;
import static Zeze.Net.Encrypt2.aesCryptCtor;
import static Zeze.Net.Encrypt2.mAesCryptInit;
import static Zeze.Net.Encrypt2.mhEncrypt;

public final class Decrypt2 implements Codec {
	private final Codec sink;
	private final Object aesCrypt;
	private final byte[] in = new byte[BLOCK_SIZE];
	private final byte[] out;
	private int sinkIndex;
	private int writeIndex;

	public Decrypt2(Codec sink, byte[] key) throws CodecException {
		this.sink = sink;
		out = Digest.md5(key);
		try {
			aesCrypt = aesCryptCtor.newInstance();
			mAesCryptInit.invoke(aesCrypt, false, "AES", out);
			mhEncrypt.invoke(aesCrypt, out, 0, out, 0);
		} catch (Throwable e) {
			throw new CodecException(e);
		}
	}

	@Override
	public void update(byte c) throws CodecException {
		in[writeIndex] = c;
		out[writeIndex++] ^= c;
		if (writeIndex == BLOCK_SIZE) {
			writeIndex = 0;
			sink.update(out, sinkIndex, BLOCK_SIZE - sinkIndex);
			sinkIndex = 0;
			try {
				mhEncrypt.invoke(aesCrypt, in, 0, out, 0);
			} catch (Throwable e) {
				throw new CodecException(e);
			}
		}
	}

	@Override
	public void update(byte[] data, int off, int len) throws CodecException {
		try {
			int wi = writeIndex, end = off + len;
			if (wi > 0) {
				while (off < end) {
					var c = data[off++];
					in[wi] = c;
					out[wi++] ^= c;
					if (wi == BLOCK_SIZE) {
						wi = 0;
						sink.update(out, sinkIndex, BLOCK_SIZE - sinkIndex);
						sinkIndex = 0;
						mhEncrypt.invoke(aesCrypt, in, 0, out, 0);
						break;
					}
				}
			}
			while (off + BLOCK_SIZE <= end) {
				for (int i = 0; i < BLOCK_SIZE; i++) {
					var c = data[off + i];
					in[wi + i] = c;
					out[wi + i] ^= c;
				}
				off += BLOCK_SIZE;
				sink.update(out, 0, BLOCK_SIZE);
				mhEncrypt.invoke(aesCrypt, in, 0, out, 0);
			}
			while (off < end) {
				var c = data[off++];
				in[wi] = c;
				out[wi++] ^= c;
			}
			writeIndex = wi;
		} catch (Throwable e) {
			throw new CodecException(e);
		}
	}

	@Override
	public void flush() throws CodecException {
		if (sinkIndex < writeIndex) {
			sink.update(out, sinkIndex, writeIndex - sinkIndex);
			sinkIndex = writeIndex;
		}
		sink.flush();
	}
}
