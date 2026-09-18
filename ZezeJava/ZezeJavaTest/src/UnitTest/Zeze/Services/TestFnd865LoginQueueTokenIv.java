package UnitTest.Zeze.Services;

import java.security.SecureRandom;
import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import Zeze.Builtin.LoginQueue.BToken;
import Zeze.Builtin.LoginQueueServer.BSecret;
import Zeze.Config;
import Zeze.Net.Binary;
import Zeze.Serialize.ByteBuffer;
import Zeze.Services.LoginQueue;
import Zeze.Services.LoginQueueServer;
import Zeze.Util.Task;
import harness.Fast;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * FND8-65 回归：令牌IV随进程固定复用，AES-CBC为确定性加密（IND-CPA不成立；对抗复核
 * 已证伪具体跨令牌泄露通道，本修复属密码学卫生）。
 * 修复：令牌格式改为 IV(16B)||AES-CBC-PKCS5(key,IV,明文)；解码端双试探+语义校验
 * （先新格式后旧格式，均败则拒）——BToken变长编码使纯长度判别在双格式窗口二义。
 * 断言：同一BToken两次编码的IV前缀不同（随机性）、新格式往返、手工构造的旧格式
 * 密文仍可解（迁移兼容）、篡改/垃圾输入被拒。
 */
@Fast
public class TestFnd865LoginQueueTokenIv {
	static {
		Task.tryInitThreadPool();
	}

	private static BSecret.Data newSecret() throws Exception {
		var lq = new LoginQueue(new Config());
		try {
			var field = LoginQueue.class.getDeclaredField("server");
			field.setAccessible(true);
			return ((LoginQueueServer)field.get(lq)).getSecret();
		} finally {
			lq.stop();
		}
	}

	private static BToken.Data newToken() {
		var token = new BToken.Data();
		token.setServerId(7);
		token.setLinkServerId(8);
		token.setSerialId(9);
		token.setExpireTime(System.currentTimeMillis() + 60_000);
		return token;
	}

	/** 手工按旧格式（固定secretIv、无IV前缀）加密——迁移期解码兼容的对象。 */
	private static Binary legacyEncrypt(BSecret.Data secret, BToken.Data token) throws Exception {
		var bb = ByteBuffer.Allocate();
		token.encode(bb);
		var cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
		cipher.init(Cipher.ENCRYPT_MODE,
				new SecretKeySpec(secret.getSecretKey().bytesUnsafe(), "AES"),
				new IvParameterSpec(secret.getSecretIv().bytesUnsafe()));
		return new Binary(cipher.doFinal(bb.Bytes, bb.ReadIndex, bb.size()));
	}

	@Test
	public void testPerTokenRandomIvAndDualFormatDecode() throws Exception {
		var secret = newSecret();
		var token = newToken();

		// 新格式：per-token随机IV前缀——同一BToken两次编码，IV前缀与整体密文都必须不同
		var e1 = LoginQueueServer.encodeToken(secret, token);
		var e2 = LoginQueueServer.encodeToken(secret, token);
		Assertions.assertTrue(e1.size() > 16, "新格式=IV前缀+密文，必然大于一个块");
		var iv1 = new byte[16];
		var iv2 = new byte[16];
		System.arraycopy(e1.bytesUnsafe(), e1.getOffset(), iv1, 0, 16);
		System.arraycopy(e2.bytesUnsafe(), e2.getOffset(), iv2, 0, 16);
		Assertions.assertFalse(java.util.Arrays.equals(iv1, iv2), "IV前缀必须per-token随机");
		Assertions.assertNotEquals(e1, e2, "确定性加密必须消除（同明文两次编码互不相同）");

		// 新格式往返
		var decoded = LoginQueueServer.decodeToken(secret, e1);
		Assertions.assertEquals(token.getServerId(), decoded.getServerId());
		Assertions.assertEquals(token.getLinkServerId(), decoded.getLinkServerId());
		Assertions.assertEquals(token.getSerialId(), decoded.getSerialId());
		Assertions.assertEquals(token.getExpireTime(), decoded.getExpireTime());

		// 旧格式（固定secretIv，迁移期在飞令牌）仍可解：双试探的旧分支
		var legacy = legacyEncrypt(secret, token);
		var decodedLegacy = LoginQueueServer.decodeToken(secret, legacy);
		Assertions.assertEquals(token.getServerId(), decodedLegacy.getServerId(), "旧格式兼容：serverId");
		Assertions.assertEquals(token.getSerialId(), decodedLegacy.getSerialId(), "旧格式兼容：serialId");

		// 垃圾输入（既非新格式也非旧格式）被拒
		var garbage = new byte[24];
		new SecureRandom().nextBytes(garbage);
		Assertions.assertThrows(Exception.class,
				() -> LoginQueueServer.decodeToken(secret, new Binary(garbage)), "双格式均败必须拒绝");
	}
}
