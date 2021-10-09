package Zeze.Net;

import Zeze.Serialize.*;
import Zeze.*;
import java.io.*;

public final class Encrypt implements Codec {
	private final Codec sink;
	private final ICryptoTransform cipher;
//C# TO JAVA CONVERTER WARNING: Unsigned integer types have no direct equivalent in Java:
//ORIGINAL LINE: private readonly byte[] _iv;
	private final byte[] _iv;
//C# TO JAVA CONVERTER WARNING: Unsigned integer types have no direct equivalent in Java:
//ORIGINAL LINE: private readonly byte[] _in = new byte[16];
	private final byte[] _in = new byte[16];
	//private readonly byte[] _out = new byte[16];
	private int count = 0;

//C# TO JAVA CONVERTER WARNING: Unsigned integer types have no direct equivalent in Java:
//ORIGINAL LINE: public Encrypt(Codec sink, byte[] key)
	public Encrypt(Codec sink, byte[] key) {
		this.sink = sink;
		_iv = Digest.Md5(key);
		AesManaged aes = new AesManaged();
		aes.Mode = CipherMode.ECB;
		cipher = aes.CreateEncryptor(_iv, _iv);
	}

	private void succeed() {
		cipher.TransformBlock(_iv, 0, 16, _iv, 0);
	}

//C# TO JAVA CONVERTER WARNING: Unsigned integer types have no direct equivalent in Java:
//ORIGINAL LINE: public void update(byte c)
	public void update(byte c) {
		if (count < 0) {
			sink.update((byte)((byte)(_iv[count++ + 16] ^= c)));
			return;
		}
		_in[count++] = c;
		if (count < 16) {
			return;
		}
		succeed();
		for (int i = 0; i < 16; i++) {
			_iv[i] ^= _in[i];
		}
		sink.update(_iv, 0, 16);
		count = 0;
	}

//C# TO JAVA CONVERTER WARNING: Unsigned integer types have no direct equivalent in Java:
//ORIGINAL LINE: public void update(byte[] data, int off, int len)
	public void update(byte[] data, int off, int len) {
		int i = off;
		len += off;
		if (count < 0) {
			for (; i < len && count < 0; i++, count++) {
				sink.update((byte)((byte)(_iv[count + 16] ^= data[i])));
			}
		}
		else if (count > 0) {
			for (; i < len && count < 16; i++, count++) {
				_in[count] = data[i];
			}
			if (count < 16) {
				return;
			}
			succeed();
			for (int j = 0; j < 16; j++) {
				_iv[j] ^= _in[j];
			}
			sink.update(_iv, 0, 16);
			count = 0;
		}
//C# TO JAVA CONVERTER WARNING: The right shift operator was not replaced by Java's logical right shift operator since the left operand was not confirmed to be of an unsigned type, but you should review whether the logical right shift operator (>>>) is more appropriate:
		int nblocks = (len - i) >> 4;
		for (int j = 0; j < nblocks; j++) {
			succeed();
			for (int k = 0; k < 16; k++) {
				_iv[k] ^= data[i + j * 16 + k];
			}
			sink.update(_iv, 0, 16);
		}
		for (i += nblocks << 4; i < len; i++) {
			_in[count++] = data[i];
		}
	}

	public void flush() {
		if (count > 0) {
			succeed();
			for (int i = 0; i < count; i++) {
				sink.update((byte)((byte)(_iv[i] ^= _in[i])));
			}
			count -= 16;
		}
		sink.flush();
	}

	public void close() throws IOException {
		cipher.Dispose();
		sink.close();
	}
}