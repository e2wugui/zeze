package Zeze.Net;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import static Zeze.Net.Encrypt2.BLOCK_SIZE;
import static Zeze.Net.Encrypt2.mhCryptCtor;
import static Zeze.Net.Encrypt2.mhCryptEncrypt;
import static Zeze.Net.Encrypt2.mhCryptInit;

// AES(CFB) Decrypt
public final class Decrypt2 implements Codec {
	private final @NotNull Object aesCrypt;
	private final byte[] in = new byte[BLOCK_SIZE];
	private final byte[] out = new byte[BLOCK_SIZE];
	private Codec sink;
	private int sinkIndex;
	private int writeIndex;

	/**
	 * @param sink 如果为null,则需要reset才能开始解密
	 * @param key  长度只支持16,24,32字节. 不能为null
	 * @param iv   长度必须至少Encrypt2.BLOCK_SIZE. 如果为null,则需要reset才能开始解密
	 */
	public Decrypt2(@Nullable Codec sink, byte @NotNull [] key, byte @Nullable [] iv) throws CodecException {
		this.sink = sink;
		try {
			aesCrypt = mhCryptCtor.invoke();
			mhCryptInit.invoke(aesCrypt, false, "AES", key);
			if (iv != null) {
				System.arraycopy(iv, 0, out, 0, BLOCK_SIZE);
				mhCryptEncrypt.invoke(aesCrypt, out, 0, out, 0);
			}
		} catch (Error e) {
			throw e;
		} catch (Throwable e) { // MethodHandle.invoke
			throw new CodecException(e);
		}
	}

	/**
	 * @param sink 不能为null
	 * @param iv   长度必须至少Encrypt2.BLOCK_SIZE. 不能为null
	 */
	public void reset(@NotNull Codec sink, byte @NotNull [] iv) {
		System.arraycopy(iv, 0, out, 0, BLOCK_SIZE);
		this.sink = sink;
		sinkIndex = 0;
		writeIndex = 0;
		try {
			mhCryptEncrypt.invoke(aesCrypt, out, 0, out, 0);
		} catch (Error e) {
			throw e;
		} catch (Throwable e) { // MethodHandle.invoke
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
				mhCryptEncrypt.invoke(aesCrypt, in, 0, out, 0);
			} catch (Error e) {
				throw e;
			} catch (Throwable e) { // MethodHandle.invoke
				throw new CodecException(e);
			}
		}
	}

	@Override
	public void update(byte @NotNull [] data, int off, int len) throws CodecException {
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
						mhCryptEncrypt.invoke(aesCrypt, in, 0, out, 0);
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
				mhCryptEncrypt.invoke(aesCrypt, in, 0, out, 0);
			}
			while (off < end) {
				var c = data[off++];
				in[wi] = c;
				out[wi++] ^= c;
			}
			writeIndex = wi;
		} catch (Error e) {
			throw e;
		} catch (Throwable e) { // MethodHandle.invoke
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
