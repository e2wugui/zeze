package Zeze.Net;

import java.security.MessageDigest;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import Zeze.Serialize.ByteBuffer;
import Zeze.Util.Task;
import org.jetbrains.annotations.NotNull;

public final class Digest {
	public static byte @NotNull [] md5(byte @NotNull [] message) {
		return md5(message, 0, message.length);
	}

	public static byte @NotNull [] md5(byte[] message, int offset, int len) {
		try {
			var md5 = MessageDigest.getInstance("MD5");
			md5.update(message, offset, len);
			return md5.digest();
		} catch (Exception e) {
			Task.forceThrow(e);
			//noinspection UnreachableCode
			return ByteBuffer.Empty; // never run here
		}
	}

	public static byte @NotNull [] hmacMd5(byte[] key, byte[] data, int offset, int length) {
		try {
			var mac = Mac.getInstance("HmacMD5");
			mac.init(new SecretKeySpec(key, 0, key.length, "HmacMD5"));
			mac.update(data, offset, length);
			return mac.doFinal();
		} catch (Exception e) {
			Task.forceThrow(e);
			//noinspection UnreachableCode
			return ByteBuffer.Empty; // never run here
		}
	}

	private Digest() {
	}
}
