package Zeze.Net;

import java.security.MessageDigest;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import Zeze.Util.Task;

public final class Digest {
	public static byte[] md5(byte[] message) {
		return md5(message, 0, message.length);
	}

	public static byte[] md5(byte[] message, int offset, int len) {
		try {
			var md5 = MessageDigest.getInstance("MD5");
			md5.update(message, offset, len);
			return md5.digest();
		} catch (Exception e) {
			Task.forceThrow(e);
			return null; // never run here
		}
	}

	public static byte[] hmacMd5(byte[] key, byte[] data, int offset, int length) {
		try {
			var mac = Mac.getInstance("HmacMD5");
			mac.init(new SecretKeySpec(key, 0, key.length, "HmacMD5"));
			mac.update(data, offset, length);
			return mac.doFinal();
		} catch (Exception e) {
			Task.forceThrow(e);
			return null; // never run here
		}
	}

	private Digest() {
	}
}
