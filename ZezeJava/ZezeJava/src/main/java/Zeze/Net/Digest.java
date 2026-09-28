package Zeze.Net;

import java.security.MessageDigest;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import Zeze.Util.Task;
import org.jetbrains.annotations.NotNull;

/**
 * 摘要工具（MD5/HmacMD5）。
 */
public final class Digest {
	// 刻意不缓存：调用点全在每连接握手/建codec路径，getInstance 开销无关紧要；
	// ThreadLocal 缓存在虚拟线程下实例驻留随线程数无界放大（每线程一 MD5 + 一 Mac）。
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
