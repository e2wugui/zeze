package Zeze.Net;

import java.security.MessageDigest;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import Zeze.Util.Task;
import org.jetbrains.annotations.NotNull;

public final class Digest {
	public static byte @NotNull [] md5(byte @NotNull [] message) {
		return md5(message, 0, message.length);
	}

	public static byte @NotNull [] md5(byte @NotNull [] message, int offset, int len) {
		try {
			var md5 = MessageDigest.getInstance("MD5");
			md5.update(message, offset, len);
			return md5.digest();
		} catch (Exception e) {
			throw Task.forceThrow(e);
		}
	}

	public static byte @NotNull [] hmacMd5(byte @NotNull [] key, byte @NotNull [] data, int offset, int length) {
		try {
			var mac = Mac.getInstance("HmacMD5");
			mac.init(new SecretKeySpec(key, 0, key.length, "HmacMD5"));
			mac.update(data, offset, length);
			return mac.doFinal();
		} catch (Exception e) {
			throw Task.forceThrow(e);
		}
	}

	private Digest() {
	}
}
