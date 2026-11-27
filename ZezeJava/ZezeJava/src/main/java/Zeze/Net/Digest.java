package Zeze.Net;

import java.security.MessageDigest;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import Zeze.Util.Task;

/**
 * 摘要工具（MD5/HmacMD5）。
 */
public final class Digest {
	// 不做 ThreadLocal 缓存：握手/建 codec 路径频次低，避免虚拟线程下实例驻留随线程数膨胀。
	public static byte [] md5(byte [] message) {
		return md5(message, 0, message.length);
	}

	public static byte [] md5(byte [] message, int offset, int len) {
		try {
			var md5 = MessageDigest.getInstance("MD5");
			md5.update(message, offset, len);
			return md5.digest();
		} catch (Exception e) {
			throw Task.forceThrow(e);
		}
	}

	public static byte [] hmacMd5(byte [] key, byte [] data, int offset, int length) {
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
