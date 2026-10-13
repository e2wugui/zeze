package Zeze.Services.Log4jQuery;

import harness.Extra;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import Zeze.Config;
import Zeze.Services.LogAgent;
import harness.Fast;

/**
 * Session 对未注册 serverName 的显式失败直测（对齐 LogAgent.query 的
 * "unknown log server" 判空先例——同一张动态服务器表，query 修复时 Session 漏改，
 * FND29/FND30 两轮审计均列为跨域线索）：构造/search/browse/close 裸解引用
 * __getLogServer 的返回值，未命中（构造期未知名/运行期被 SM 摘除）返回 null →
 * NPE 无信息且调用方不可自愈。修复后抛带名字的 IllegalArgumentException。
 */
@Fast
@Extra
public class TestSessionUnknownServerName {

	@Test
	public void testUnknownServerNameThrowsIllegalArgumentWithName() throws Exception {
		var conf = new Config().loadAndParse();
		conf.setServiceManager("disable"); // 不依赖部署配置的ServiceManager（无SM时注册表为空）
		var agent = new LogAgent(conf);

		var ex = assertThrows(IllegalArgumentException.class,
				() -> new Session(agent, "no-such-log-server", "zeze"),
				"未注册的服务器名必须显式失败（未知名的会话构造本就无成功可能）");
		assertTrue(ex.getMessage().contains("no-such-log-server"),
				"异常信息必须携带服务器名便于诊断: " + ex.getMessage());
	}
}
