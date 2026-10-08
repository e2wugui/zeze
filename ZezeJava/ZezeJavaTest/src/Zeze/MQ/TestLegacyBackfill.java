package Zeze.MQ.Master;

import harness.Extra;
import java.nio.file.Path;
import Zeze.Builtin.MQ.Master.BMQServer;
import Zeze.Builtin.MQ.Master.BMQServers;
import Zeze.Config;
import harness.Fast;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * FND19 GB-D05 存量兼容回归（@Fast 直构 Master）：升级前落库的 mqTable 条目无 ManagerId
 * （decode 缺省 0=未知身份），Register 联动重写必须保留 host:port 匹配的旧路径兜底——
 * 按地址匹配命中后改写新地址并回填 ManagerId；ManagerId 不匹配且非 0 的条目不得误改。
 * <p>
 * 注：文件放 src/MQ/ 但声明 package Zeze.MQ.Master（访问 Master 的包内测试缝
 * rewriteRoutes/getServers/putMqServers，TestSegmentRecycle 先例）。
 */
@Fast
@Extra
public class TestLegacyBackfill {

	private static BMQServer serverOf(String host, int port, int partitionIndex, String topic, long managerId) {
		return new BMQServer(host, port, partitionIndex, topic, managerId);
	}

	private static void seed(Master master, String topic, BMQServer... entries) throws Exception {
		var servers = new BMQServers();
		servers.getInfo().setTopic(topic);
		servers.getInfo().setPartition(entries.length);
		for (var e : entries)
			servers.getServers().add(e);
		master.putMqServers(topic, servers);
	}

	@Test
	public void testLegacyAddressMatchBackfillsManagerId(@TempDir Path tempDir) throws Exception {
		var master = new Master(tempDir.resolve("master").toString(), new Config());
		try {
			// 存量形态：ManagerId=0，老地址 10.0.0.1:20000。
			seed(master, "legacy", serverOf("10.0.0.1", 20000, 0, "legacy", 0));

			// 注册：managerId=42 携新地址 10.0.0.2:21000；被替换旧注册条目=老地址。
			master.rewriteRoutes(new BMQServer.Data("10.0.0.2", 21000, 1, "", 42),
					new BMQServer.Data("10.0.0.1", 20000, 1, "", 0));

			var rewritten = master.getServers("legacy").getServers().get(0);
			Assertions.assertEquals("10.0.0.2", rewritten.getHost(), "存量条目按旧地址匹配，路由重写为新地址");
			Assertions.assertEquals(21000, rewritten.getPort());
			Assertions.assertEquals(42, rewritten.getManagerId(), "匹配后回填 ManagerId（此后走 id 主路径）");

			// 同址重注册形态（replacedOldInfo==null）：按注册地址匹配同样回填。
			seed(master, "sameAddr", serverOf("10.0.0.3", 30000, 0, "sameAddr", 0));
			master.rewriteRoutes(new BMQServer.Data("10.0.0.3", 30000, 1, "", 43), null);
			Assertions.assertEquals(43, master.getServers("sameAddr").getServers().get(0).getManagerId(),
					"同址重注册按地址匹配回填");

			// 身份不匹配的条目不得误改：别的 manager（id=99）承载的条目对注册 id=42 不动。
			seed(master, "other", serverOf("10.9.9.9", 40000, 0, "other", 99));
			master.rewriteRoutes(new BMQServer.Data("10.0.0.2", 21000, 1, "", 42), null);
			var untouched = master.getServers("other").getServers().get(0);
			Assertions.assertEquals("10.9.9.9", untouched.getHost(), "非本 manager 的条目不受联动重写影响");
			Assertions.assertEquals(99, untouched.getManagerId());
		} finally {
			master.close();
		}
	}
}
