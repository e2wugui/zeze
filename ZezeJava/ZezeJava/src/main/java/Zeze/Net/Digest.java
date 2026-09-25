package Zeze.Net;

import java.security.MessageDigest;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import Zeze.Util.Task;
import org.jetbrains.annotations.NotNull;

public final class Digest {
	// MessageDigest/Mac 非线程安全且 getInstance 有同步开销，按线程缓存复用。
	// digest()/doFinal() 完成后自动复位，可跨调用安全复用；Mac 的 key 经每次 init 重设。
	private static final @NotNull ThreadLocal<MessageDigest> md5Local = ThreadLocal.withInitial(() -> {
		try {
			return MessageDigest.getInstance("MD5");
		} catch (Exception e) {
			throw Task.forceThrow(e);
		}
	});

	private static final @NotNull ThreadLocal<Mac> hmacMd5Local = ThreadLocal.withInitial(() -> {
		try {
			return Mac.getInstance("HmacMD5");
		} catch (Exception e) {
			throw Task.forceThrow(e);
		}
	});

	public static byte @NotNull [] md5(byte @NotNull [] message) {
		return md5(message, 0, message.length);
	}

	public static byte @NotNull [] md5(byte @NotNull [] message, int offset, int len) {
		try {
			var md5 = md5Local.get();
			md5.update(message, offset, len);
			return md5.digest();
		} catch (Exception e) {
			throw Task.forceThrow(e);
		}
	}

	public static byte @NotNull [] hmacMd5(byte @NotNull [] key, byte @NotNull [] data, int offset, int length) {
		try {
			var mac = hmacMd5Local.get();
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
