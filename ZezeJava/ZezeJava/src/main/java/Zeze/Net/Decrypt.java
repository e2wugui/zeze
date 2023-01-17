package Zeze.Net;

import java.nio.ByteBuffer;
import javax.crypto.Cipher;
import javax.crypto.ShortBufferException;
import javax.crypto.spec.SecretKeySpec;

public class Decrypt implements Codec {
	private final Codec sink;
	private final Cipher cipher;
	private final ByteBuffer ivr;
	private final ByteBuffer ivw;
	private final byte[] iv;
	private final byte[] in;
	private final byte[] out;
	private int count = 0;

	public Decrypt(Codec sink, byte[] key) throws CodecException {
		this.sink = sink;
		iv = new byte[16];
		in = new byte[16];
		out = new byte[16];
		System.arraycopy(Digest.md5(key), 0, iv, 0, 16);
		ivr = ByteBuffer.wrap(iv);
		ivw = ByteBuffer.wrap(iv);
		try {
			cipher = Cipher.getInstance("AES/ECB/NoPadding");
			cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(iv, "AES"));
		} catch (Exception e) {
			throw new CodecException(e);
		}
	}

	private void succeed() {
		try {
			cipher.update(ivr, ivw);
			ivr.clear();
			ivw.clear();
		} catch (ShortBufferException e) {
			// skip
		}
	}

	@Override
	public void update(byte c) throws CodecException {
		if (count < 0) {
			sink.update((byte)(iv[count + 16] ^ c));
			iv[count++ + 16] = c;
			return;
		}
		in[count++] = c;
		if (count < 16)
			return;
		succeed();
		for (int i = 0; i < 16; i++) {
			out[i] = (byte)(iv[i] ^ in[i]);
			iv[i] = in[i];
		}
		sink.update(out, 0, 16);
		count = 0;
	}

	@Override
	public void update(byte[] data, int off, int len) throws CodecException {
		int i = off;
		len += off;
		if (count < 0) {
			for (; i < len && count < 0; i++, count++) {
				sink.update((byte)(iv[count + 16] ^ data[i]));
				iv[count + 16] = data[i];
			}
		} else if (count > 0) {
			for (; i < len && count < 16; i++, count++)
				in[count] = data[i];
			if (count < 16)
				return;
			succeed();
			for (int j = 0; j < 16; j++) {
				out[j] = (byte)(iv[j] ^ in[j]);
				iv[j] = in[j];
			}
			sink.update(out, 0, 16);
			count = 0;
		}
		int nblocks = (len - i) >> 4;
		for (int j = 0; j < nblocks; j++) {
			succeed();
			for (int k = 0; k < 16; k++) {
				byte c = data[i + j * 16 + k];
				out[k] = (byte)(iv[k] ^ c);
				iv[k] = c;
			}
			sink.update(out, 0, 16);
		}
		for (i += nblocks << 4; i < len; i++)
			in[count++] = data[i];
	}

	@Override
	public void flush() throws CodecException {
		if (count > 0) {
			succeed();
			for (int i = 0; i < count; i++) {
				sink.update((byte)(iv[i] ^ in[i]));
				iv[i] = in[i];
			}
			count -= 16;
		}
		sink.flush();
	}
}
