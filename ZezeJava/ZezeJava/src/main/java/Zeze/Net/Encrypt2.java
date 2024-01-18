package Zeze.Net;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import Zeze.Util.Json;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

// AES(CFB) Encrypt
public final class Encrypt2 implements Codec {
	static final int BLOCK_SIZE = 16;
	static final @NotNull MethodHandle mhCryptCtor;
	static final @NotNull MethodHandle mhCryptInit;
	static final @NotNull MethodHandle mhCryptEncrypt;

	private final @NotNull Object aesCrypt;
	private final byte[] out = new byte[BLOCK_SIZE];
	private Codec sink;
	private int sinkIndex;
	private int writeIndex;

	static {
		try {
			var clsAESCrypt = Class.forName("com.sun.crypto.provider.AESCrypt");
			var cryptCtor = clsAESCrypt.getDeclaredConstructor();
			var mCryptInit = clsAESCrypt.getDeclaredMethod("init", boolean.class, String.class, byte[].class);
			var mCryptEncrypt = clsAESCrypt.getDeclaredMethod("encryptBlock",
					byte[].class, int.class, byte[].class, int.class);
			Json.setAccessible(cryptCtor);
			Json.setAccessible(mCryptInit);
			Json.setAccessible(mCryptEncrypt);
			var lookup = MethodHandles.lookup();
			mhCryptCtor = lookup.unreflectConstructor(cryptCtor);
			mhCryptInit = lookup.unreflect(mCryptInit);
			mhCryptEncrypt = lookup.unreflect(mCryptEncrypt);
		} catch (ReflectiveOperationException e) {
			throw new ExceptionInInitializerError(e);
		}
	}

	/**
	 * @param sink 如果为null,则需要reset才能开始加密
	 * @param key  长度只支持16,24,32字节. 不能为null
	 * @param iv   长度必须至少BLOCK_SIZE. 如果为null,则需要reset才能开始加密
	 */
	public Encrypt2(@Nullable Codec sink, byte @NotNull [] key, byte @Nullable [] iv) throws CodecException {
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
	 * @param iv   长度必须至少BLOCK_SIZE. 不能为null
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
		out[writeIndex++] ^= c;
		if (writeIndex == BLOCK_SIZE) {
			writeIndex = 0;
			sink.update(out, sinkIndex, BLOCK_SIZE - sinkIndex);
			sinkIndex = 0;
			try {
				mhCryptEncrypt.invoke(aesCrypt, out, 0, out, 0);
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
					out[wi++] ^= data[off++];
					if (wi == BLOCK_SIZE) {
						wi = 0;
						sink.update(out, sinkIndex, BLOCK_SIZE - sinkIndex);
						sinkIndex = 0;
						mhCryptEncrypt.invoke(aesCrypt, out, 0, out, 0);
						break;
					}
				}
			}
			while (off + BLOCK_SIZE <= end) {
				for (int i = 0; i < BLOCK_SIZE; i++)
					out[wi + i] ^= data[off + i];
				off += BLOCK_SIZE;
				sink.update(out, 0, BLOCK_SIZE);
				mhCryptEncrypt.invoke(aesCrypt, out, 0, out, 0);
			}
			while (off < end)
				out[wi++] ^= data[off++];
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
