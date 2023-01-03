package Zeze.Net;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import Zeze.Util.Json;

public final class Encrypt2 implements Codec {
	static final int BLOCK_SIZE = 16;
	static final Constructor<?> aesCryptCtor;
	static final Method mAesCryptInit;
	static final MethodHandle mhEncrypt;

	private final Codec sink;
	private final Object aesCrypt;
	private final byte[] out;
	private int sinkIndex;
	private int writeIndex;

	static {
		try {
			var clsAESCrypt = Class.forName("com.sun.crypto.provider.AESCrypt");
			aesCryptCtor = clsAESCrypt.getDeclaredConstructor();
			mAesCryptInit = clsAESCrypt.getDeclaredMethod("init", boolean.class, String.class, byte[].class);
			var methodEncrypt = clsAESCrypt.getDeclaredMethod("encryptBlock",
					byte[].class, int.class, byte[].class, int.class);
			Json.setAccessible(aesCryptCtor);
			Json.setAccessible(mAesCryptInit);
			Json.setAccessible(methodEncrypt);
			mhEncrypt = MethodHandles.lookup().unreflect(methodEncrypt);
		} catch (ReflectiveOperationException e) {
			throw new RuntimeException(e);
		}
	}

	public Encrypt2(Codec sink, byte[] key) throws CodecException {
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
		out[writeIndex++] ^= c;
		if (writeIndex == BLOCK_SIZE) {
			writeIndex = 0;
			sink.update(out, sinkIndex, BLOCK_SIZE - sinkIndex);
			sinkIndex = 0;
			try {
				mhEncrypt.invoke(aesCrypt, out, 0, out, 0);
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
					out[wi++] ^= data[off++];
					if (wi == BLOCK_SIZE) {
						wi = 0;
						sink.update(out, sinkIndex, BLOCK_SIZE - sinkIndex);
						sinkIndex = 0;
						mhEncrypt.invoke(aesCrypt, out, 0, out, 0);
						break;
					}
				}
			}
			while (off + BLOCK_SIZE <= end) {
				for (int i = 0; i < BLOCK_SIZE; i++)
					out[wi + i] ^= data[off + i];
				off += BLOCK_SIZE;
				sink.update(out, 0, BLOCK_SIZE);
				mhEncrypt.invoke(aesCrypt, out, 0, out, 0);
			}
			while (off < end)
				out[wi++] ^= data[off++];
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
