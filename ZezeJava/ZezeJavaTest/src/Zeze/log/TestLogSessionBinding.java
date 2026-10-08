package Zeze.log;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import harness.Fast;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * FND19 GE-D06：会话回执比对（拍板方案A）的纯逻辑直测。
 * 直测两层可写面：
 * <ol>
 * <li>{@link LogSessionBinding#matches}——(会话类型, serverName, logName, 条件指纹)
 *     四元组比对矩阵，这是"客户端漏置 changeSession 不再串数据源/串条件"的判定核心；</li>
 * <li>{@link FileSessionManager} 的 IP 键行为——无端口、IPv6 规范host（GE-C09 回归），
 *     键粒度是方案A记档的"同IP互顶"已知限制的行为基线。</li>
 * </ol>
 * resolve()（建新会话+替换关闭收口）依赖 LogAgent 网络调用，不在直测面，
 * 见 audit-FND19/raw/实现E报告.md 的"未经运行验证"清单。
 */
@Fast
public class TestLogSessionBinding {
	private static final Object SESSION = new Object();
	private static final String COND = "search|-1|-1|1|[error]|";

	/** 单服务器视图绑定：四元组全同才复用；server/logName/视图类型任一不同都必须重建。 */
	@Test
	public void testServerBindingMatches() {
		var binding = LogSessionBinding.server("server1", "app.log", COND, SESSION);
		assertTrue(binding.matches(false, "server1", "app.log", COND), "四元组全同：复用");

		assertFalse(binding.matches(false, "server2", "app.log", COND), "换 serverName：重建（错误数据源防护本体）");
		assertFalse(binding.matches(false, "server1", "other.log", COND), "换 logName：重建（旧文件游标防护）");
		assertFalse(binding.matches(true, "server1", "app.log", COND), "Session→SessionAll 视图切换：重建");
		assertFalse(binding.matches(false, "server2", null, COND), "server 与 logName 都不同：重建");
	}

	/** 全服视图绑定：serverName 无意义不参与比对，只比 logName 与条件指纹。 */
	@Test
	public void testAllViewBindingMatches() {
		var binding = LogSessionBinding.allView("app.log", COND, SESSION);
		assertTrue(binding.matches(true, null, "app.log", COND), "serverName 不参与全服视图比对");
		assertTrue(binding.matches(true, "whatever", "app.log", COND));
		assertFalse(binding.matches(true, null, "other.log", COND), "换 logName：重建");
		assertFalse(binding.matches(false, null, "app.log", COND), "SessionAll→Session 视图切换：重建");
	}

	/**
	 * 条件指纹维度（FND34 zokermanager-02）：数据源三元组不变、查询条件变即重建——
	 * 服务端仅 beginTime 有去重哨兵，words 等条件变更复用旧游标会话会静默漏掉
	 * 游标之前的匹配；同条件续页指纹稳定复用。search/browse 模式前缀不同即不同
	 * 身份（游标消费形态不同，不共享）。
	 */
	@Test
	public void testConditionKeyPartOfMatch() {
		var binding = LogSessionBinding.server("server1", "app.log", COND, SESSION);
		assertTrue(binding.matches(false, "server1", "app.log", COND), "同条件续页：复用");
		assertFalse(binding.matches(false, "server1", "app.log", "search|-1|-1|1|[timeout]|"),
				"换关键词：重建（旧游标对新条件静默漏早段防护本体）");
		assertFalse(binding.matches(false, "server1", "app.log", "search|1000|-1|1|[error]|"),
				"换 beginTime：重建");
		assertFalse(binding.matches(false, "server1", "app.log", "browse|-1|-1|1|[error]|0.5"),
				"search→browse 模式切换：重建");
		assertFalse(binding.matches(false, "server1", "app.log", null),
				"缺指纹（非 HTTP 直构形态）：安全方向重建");
	}

	/** logName 归一：null 与 "" 同一语义（请求侧 null 与绑定侧 null/"" 等价比较）。 */
	@Test
	public void testLogNameNormalization() {
		var nullNamed = LogSessionBinding.server("server1", null, COND, SESSION);
		assertTrue(nullNamed.matches(false, "server1", null, COND));
		assertTrue(nullNamed.matches(false, "server1", "", COND), "null 与 '' 等价");
		assertFalse(nullNamed.matches(false, "server1", "app.log", COND), "空与具体日志文件不同语义：重建");

		var named = LogSessionBinding.server("server1", "", COND, SESSION);
		assertTrue(named.matches(false, "server1", null, COND));
	}

	/** 绑定记录持有会话对象本身（回执与句柄不分家），工厂方法区分两种视图形态；
	 * touched 副本四元组不变仅时间戳前移。 */
	@Test
	public void testBindingHoldsSession() {
		var server = new Object();
		var all = new Object();
		assertSame(server, LogSessionBinding.server("s", "l", COND, server).session());
		assertSame(all, LogSessionBinding.allView("l", COND, all).session());
		assertNotSame(LogSessionBinding.server("s", "l", COND, server).session(),
				LogSessionBinding.allView("l", COND, all).session());

		var binding = LogSessionBinding.server("server1", "app.log", COND, SESSION);
		var touched = binding.touched(System.nanoTime());
		assertSame(binding.session(), touched.session());
		assertTrue(touched.matches(false, "server1", "app.log", COND), "touched 保持四元组可复用");
	}

	/** IP 键行为基线：无端口（同IP不同端口共享会话槽=方案A已知限制）、put 返回被替换旧绑定。 */
	@Test
	public void testIpKeyPortStrippedAndPutReturnsOld() throws Exception {
		SocketAddress port1 = new InetSocketAddress(InetAddress.getByName("127.0.0.3"), 1111);
		SocketAddress port2 = new InetSocketAddress(InetAddress.getByName("127.0.0.3"), 2222);

		var first = LogSessionBinding.server("server1", "app.log", COND, SESSION);
		assertNull(FileSessionManager.put(port1, first), "首次存入无旧绑定");
		assertSame(first, FileSessionManager.get(port2), "同IP不同端口命中同一会话槽（键无端口）");

		var second = LogSessionBinding.server("server2", "app.log", COND, new Object());
		assertSame(first, FileSessionManager.put(port2, second), "put 返回被替换的旧绑定（resolve 的关闭收口依据）");
		assertSame(second, FileSessionManager.get(port1));
	}

	/** IPv6 键规范化（GE-C09 回归）：不同书写形式的回环地址坍缩到同一键。 */
	@Test
	public void testIpv6CanonicalKey() throws Exception {
		SocketAddress v6Full = new InetSocketAddress(InetAddress.getByName("::1"), 1111);
		SocketAddress v6Expanded = new InetSocketAddress(InetAddress.getByName("0:0:0:0:0:0:0:1"), 2222);
		// InetAddress 解析后 hostAddress 一致；FileSessionManager 取规范 host 地址字符串作键，
		// 不再走 InetSocketAddress.toString() 的 "[" 包裹形式（修复前所有IPv6坍缩成同一个键的根因之一）。

		var binding = LogSessionBinding.allView("app.log", COND, SESSION);
		FileSessionManager.put(v6Full, binding);
		assertSame(binding, FileSessionManager.get(v6Expanded), "IPv6 规范形式与全写形式命中同一键");
	}
}
