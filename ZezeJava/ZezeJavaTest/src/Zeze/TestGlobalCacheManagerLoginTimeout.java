package Zeze;

import harness.Fast;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * GCM Login/ReLogin 一直使用 RPC 默认 5s 超时且不可配：GCM 端 processLogin
 * 应答前逐 key release 无界，实例多时应答超 5s，客户端超时重连风暴而服务端
 * 还在处理。
 *
 * 修复：Config 新增 GlobalCacheManagerLoginTimeout（毫秒，默认 5000 维持现状），
 * GlobalClient 的 Login/ReLogin 显式携带该超时。
 */
@Fast
public class TestGlobalCacheManagerLoginTimeout {

	@Test
	public void testDefaultAndConfigurable() {
		var conf = new Config();
		Assertions.assertEquals(5000, conf.getGlobalCacheManagerLoginTimeout(),
				"默认维持RPC全局默认5s，行为不变");
		conf.setGlobalCacheManagerLoginTimeout(60_000);
		Assertions.assertEquals(60_000, conf.getGlobalCacheManagerLoginTimeout());
	}
}
