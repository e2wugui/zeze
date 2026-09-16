package UnitTest.Zeze.Services;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import Zeze.Builtin.LoginQueue.BToken;
import Zeze.Builtin.LoginQueueServer.BSecret;
import Zeze.Config;
import Zeze.Net.Binary;
import Zeze.Services.LoginQueue;
import Zeze.Services.LoginQueueServer;
import Zeze.Util.Task;
import harness.Fast;

/**
 * FND7-20：登录令牌的AES密钥/IV原先用 Zeze.Util.Random（ThreadLocalRandom，非CSPRNG）
 * 生成——种子可预测，配合固定IV的确定性CBC加密，攻击者拿到一次排队登录的密文样本即可
 * 离线穷举伪造令牌绕过排队。修复：改用 SecureRandom 生成（对齐 Token 的做法）。
 * <p>
 * 熵源本身不可从黑盒观测（不可测部分由构造器代码审读保证：nextSecretBinary 使用
 * SecureRandom.nextBytes），本测试钉住可观测的护栏属性：key/IV 各16字节、两个实例
 * 互不相同、非全零/全常数（防退化为常量密钥）、令牌编解码往返不变（格式不受影响）。
 */
@Fast
public class TestFnd720LoginQueueSecretEntropy {
	static {
		Task.tryInitThreadPool();
	}

	private static BSecret.Data newSecret() throws Exception {
		// LoginQueueServer 构造不绑端口（服务未start），可安全实例化；server为私有字段，反射读取
		var lq = new LoginQueue(new Config());
		try {
			var field = LoginQueue.class.getDeclaredField("server");
			field.setAccessible(true);
			return ((LoginQueueServer)field.get(lq)).getSecret();
		} finally {
			lq.stop();
		}
	}

	@Test
	public void testSecretKeyIvShapeAndIndependence() throws Exception {
		var secret1 = newSecret();
		var secret2 = newSecret();

		Assertions.assertEquals(16, secret1.getSecretKey().size(), "AES-128密钥必须16字节");
		Assertions.assertEquals(16, secret1.getSecretIv().size(), "IV必须16字节");
		Assertions.assertNotEquals(secret1.getSecretKey(), secret2.getSecretKey(),
				"两个实例的密钥必须不同（独立生成）");
		Assertions.assertNotEquals(secret1.getSecretIv(), secret2.getSecretIv(),
				"两个实例的IV必须不同（独立生成）");
		Assertions.assertNotEquals(secret1.getSecretKey(), new Binary(new byte[16]),
				"密钥不得为全零（防退化为常量）");
		Assertions.assertNotEquals(secret1.getSecretIv(), new Binary(new byte[16]),
				"IV不得为全零（防退化为常量）");

		// 令牌格式与验证流程不受换熵源影响：编码→解码往返
		var token = new BToken.Data();
		token.setServerId(7);
		token.setLinkServerId(8);
		token.setSerialId(9);
		token.setExpireTime(System.currentTimeMillis() + 60_000);
		var encoded = LoginQueueServer.encodeToken(secret1, token);
		var decoded = LoginQueueServer.decodeToken(secret1, encoded);
		Assertions.assertEquals(token.getServerId(), decoded.getServerId(), "往返serverId");
		Assertions.assertEquals(token.getLinkServerId(), decoded.getLinkServerId(), "往返linkServerId");
		Assertions.assertEquals(token.getSerialId(), decoded.getSerialId(), "往返serialId");
		Assertions.assertEquals(token.getExpireTime(), decoded.getExpireTime(), "往返expireTime");
	}
}
