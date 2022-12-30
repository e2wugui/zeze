package Zeze.Net;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import Zeze.Util.Json;

public class Encrypt2 implements Codec {
	private static final Constructor<?> aesCryptCtor;
	private static final Method mAesCryptInit;
	private static final MethodHandle mhEncrypt;

	private final Codec sink;
	private final Object aesCrypt;
	private final byte[] iv;
	private final byte[] in = new byte[16];
	private int count;

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
		iv = Digest.md5(key);
		try {
			aesCrypt = aesCryptCtor.newInstance();
			mAesCryptInit.invoke(aesCrypt, false, "AES", iv);
		} catch (ReflectiveOperationException e) {
			throw new CodecException(e);
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
		try {
			mhEncrypt.invoke(aesCrypt, iv, 0, iv, 0);
		} catch (Throwable e) {
			throw new CodecException(e);
		}
		for (int i = 0; i < 16; i++)
			iv[i] ^= in[i];
		sink.update(iv, 0, 16);
		count = 0;
	}

	@Override
	public void update(byte[] data, int off, int len) throws CodecException {
		try {
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
				mhEncrypt.invoke(aesCrypt, iv, 0, iv, 0);
				for (int j = 0; j < 16; j++)
					iv[j] ^= in[j];
				sink.update(iv, 0, 16);
				count = 0;
			}
			int nBlocks = (len - i) >> 4;
			for (int j = 0; j < nBlocks; j++) {
				mhEncrypt.invoke(aesCrypt, iv, 0, iv, 0);
				for (int k = 0; k < 16; k++)
					iv[k] ^= data[i + j * 16 + k];
				sink.update(iv, 0, 16);
			}
			for (i += nBlocks << 4; i < len; i++)
				in[count++] = data[i];
		} catch (Throwable e) {
			throw new CodecException(e);
		}
	}

	@Override
	public void flush() throws CodecException {
		if (count > 0) {
			try {
				mhEncrypt.invoke(aesCrypt, iv, 0, iv, 0);
			} catch (Throwable e) {
				throw new CodecException(e);
			}
			for (int i = 0; i < count; i++)
				sink.update(iv[i] ^= in[i]);
			count -= 16;
		}
		sink.flush();
	}
}
