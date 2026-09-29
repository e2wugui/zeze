package Zeze.Dbh2;

import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Element;
import harness.Fast;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Dbh2Config 2PC超时围栏安全余量校验（FND29 dbh2-03止血步）：
 * 桶侧undo定时（BucketMaxTime）相对协调者prepare时限（PrepareMaxTime）的余量是
 * "跨机墙钟一致"前提下的安全边界——低于NTP最大步进预算量级（60s）时，桶时钟前跳越余量
 * 即在协调者仍合法的prepare窗口内undo已决定提交的事务（客户端确认成功而数据灭失）。
 * 低余量配置必须fail-fast（带字段说明），不得静默修正掩盖误配。
 * 断根方案（raft index fencing取代墙钟）为独立裁定，留池。
 */
@Fast
public class TestDbh2ConfigBucketUndoMargin {

	private static Element conf(String... attrs) throws Exception {
		var doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
		var e = doc.createElement("Dbh2Config");
		for (var i = 0; i < attrs.length; i += 2)
			e.setAttribute(attrs[i], attrs[i + 1]);
		return e;
	}

	@Test
	public void testLowMarginRejected() {
		var ex = Assertions.assertThrows(RuntimeException.class,
				() -> new Dbh2Config().parse(conf("PrepareMaxTime", "9000", "BucketMaxTime", "10000")),
				"低余量（<60s）必须fail-fast，不得静默抬高掩盖误配");
		Assertions.assertTrue(ex.getMessage().contains("BucketMaxTime") && ex.getMessage().contains("PrepareMaxTime"),
				"错误信息必须带字段与要求值说明: " + ex.getMessage());
	}

	@Test
	public void testExactMarginAccepted() throws Exception {
		var cfg = new Dbh2Config();
		// RpcTimeout必须一并配置：缺省时prepareMaxTime会被rpcTimeout+1000下界自动抬高。
		cfg.parse(conf("RpcTimeout", "1000", "PrepareMaxTime", "9000",
				"BucketMaxTime", String.valueOf(9000 + Dbh2Config.MinBucketUndoMarginMs)));
		Assertions.assertEquals(9000, cfg.getPrepareMaxTime());
		Assertions.assertEquals(9000 + Dbh2Config.MinBucketUndoMarginMs, cfg.getBucketMaxTime());
	}

	@Test
	public void testDefaultsSatisfyMargin() throws Exception {
		var cfg = new Dbh2Config();
		cfg.parse(conf()); // 显式解析空节点（自定义节存在但未配置时间参数）不得拒绝默认值
		Assertions.assertTrue(cfg.getBucketMaxTime() - cfg.getPrepareMaxTime() >= Dbh2Config.MinBucketUndoMarginMs,
				"默认配置必须自带安全余量（默认即合规）");
	}
}
