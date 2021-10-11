package Zeze.Net;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.security.MessageDigest;

public final class Digest {
	public static byte[] Md5(byte[] message) {
		return Md5(message, 0, message.length);
	}
	
	public static byte[] Md5(byte[] message, int offset, int len) {
		try {
			var md5 = MessageDigest.getInstance("MD5");
			md5.update(message, offset, len);
			return md5.digest();
		} catch (Throwable ex) {
			throw new RuntimeException(ex);
		}
	}

	// TODO 需要确认一下写法对不对。
	public static byte[] HmacMd5(byte[] key, byte[] data, int offset, int length) {
		try {
			var mac = Mac.getInstance("HmacMD5");
			mac.init(new SecretKeySpec(key, 0, key.length, "HmacMD5"));
			mac.update(data, offset, length);
			return mac.doFinal();
		} catch (Throwable ex) {
			throw new RuntimeException(ex);
		}
	}
}