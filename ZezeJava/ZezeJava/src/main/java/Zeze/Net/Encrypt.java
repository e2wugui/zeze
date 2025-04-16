package Zeze.Net;

import java.nio.ByteBuffer;
import javax.crypto.Cipher;
import javax.crypto.ShortBufferException;
import javax.crypto.spec.SecretKeySpec;
import org.jetbrains.annotations.NotNull;

public class Encrypt implements Codec {
	private final @NotNull Codec sink;
	private final @NotNull Cipher cipher;
	private final @NotNull ByteBuffer ivr;
	private final @NotNull ByteBuffer ivw;
	private final byte @NotNull [] iv;
	private final byte @NotNull [] in;
	private int count = 0;

	public Encrypt(@NotNull Codec sink, byte @NotNull [] key) throws CodecException {
		this.sink = sink;
		iv = new byte[16];
		in = new byte[16];
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
			sink.update(iv[count++ + 16] ^= c);
			return;
		}
		in[count++] = c;
		if (count < 16)
			return;
		succeed();
		for (int i = 0; i < 16; i++)
			iv[i] ^= in[i];
		sink.update(iv, 0, 16);
		count = 0;
	}

	@Override
	public void update(byte @NotNull [] data, int off, int len) throws CodecException {
		int i = off;
		len += off;
		if (count < 0) {
			for (; i < len && count < 0; i++, count++)
				sink.update(iv[count + 16] ^= data[i]);
		} else if (count > 0) {
			for (; i < len && count < 16; i++, count++)
				in[count] = data[i];
			if (count < 16)
				return;
			succeed();
			for (int j = 0; j < 16; j++)
				iv[j] ^= in[j];
			sink.update(iv, 0, 16);
			count = 0;
		}
		int nBlocks = (len - i) >> 4;
		for (int j = 0; j < nBlocks; j++) {
			succeed();
			for (int k = 0; k < 16; k++)
				iv[k] ^= data[i + j * 16 + k];
			sink.update(iv, 0, 16);
		}
		for (i += nBlocks << 4; i < len; i++)
			in[count++] = data[i];
	}

	@Override
	public void flush() throws CodecException {
		if (count > 0) {
			succeed();
			for (int i = 0; i < count; i++)
				sink.update(iv[i] ^= in[i]);
			count -= 16;
		}
		sink.flush();
	}
}
