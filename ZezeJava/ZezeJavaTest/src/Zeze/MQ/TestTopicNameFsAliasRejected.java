package Zeze.MQ;

import harness.Extra;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import Zeze.MQ.Master.Master;
import Zeze.MQ.Master.MasterAgent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * topic 名文件系统命名空间别名的创建拒绝回归（mq-02）：topic 字符串直接用作 Manager
 * home 下的子目录名与 rocksdb 列族名（{home}/{topic}/、{topic}.{partitionId}[.{seg}]），
 * 而大小写不敏感文件系统（Windows NTFS、macOS 默认）上仅大小写/尾随空白不同的两个
 * topic 名在 Master 看是两个不同的 mqTable 键、两套列族，却解析到同一个物理目录——
 * 别名 topic 的 MQFileWithIndex 构造把共享段文件按"幽灵段+未提交尾巴"判定
 * truncate(0)，既有 topic 的全部盘上积压被"创建另一个 topic"这一无预警操作一次性
 * 摧毁；此后两 topic 的追加流以 O_APPEND 指向同一文件交错写入（装载 id 错位停摆，
 * 或 id 恰对齐时跨 topic 串台）。协议不鉴权，无任何机制阻止别名创建。
 * <p>
 * 修复：CreateMQ 入口两重校验——折叠冲突检测（对 mqTable 已有 topic 做
 * 大小写不敏感+尾随空白/点归一的碰撞检测，拒绝别名创建，跨平台一致执行以覆盖
 * "Linux 创建后迁移到大小写不敏感介质"的形态）与结构性危险名校验（控制字符、
 * Windows 保留设备名）。存量 topic 名不受影响：open/订阅/发布路径不重新校验，
 * 新校验只在 CreateMQ 入口生效。
 */
@Extra
public class TestTopicNameFsAliasRejected {
	private static final int masterPort = 26240;
	private static final int proxyPort = 26241;

	@Test
	public void testAliasTopicCreateRejectedAndExistingBacklogKept(@TempDir Path tempDir) throws Exception {
		Zeze.Util.Task.tryInitThreadPool();

		// 折叠函数直测（跨平台一致，不依赖本机 FS 语义）：大小写、尾随空白/点归一；
		// 内部空白与不同名不折叠。
		assertEquals(Master.fsNamespaceKey("foo"), Master.fsNamespaceKey("Foo"));
		assertEquals(Master.fsNamespaceKey("foo"), Master.fsNamespaceKey("FOO"));
		assertEquals(Master.fsNamespaceKey("foo"), Master.fsNamespaceKey("foo "), "尾随空白折叠（Windows同目录）");
		assertEquals(Master.fsNamespaceKey("foo"), Master.fsNamespaceKey("foo."), "尾随点折叠（Windows同目录）");
		assertNotEquals(Master.fsNamespaceKey("foo"), Master.fsNamespaceKey("bar"));
		assertNotEquals(Master.fsNamespaceKey("foo"), Master.fsNamespaceKey("fo o"), "内部空白不折叠");
		// 结构性危险名（新创建校验，存量放行）：保留设备名与控制字符。
		assertNotNull(Master.validateNewTopicName("CON"));
		assertNotNull(Master.validateNewTopicName("com3"));
		assertNotNull(Master.validateNewTopicName("a\u0001b"));
		// 合法名不受影响：ASCII 常规字符与非 ASCII（存量兼容口径，中文 topic 合法）。
		assertNull(Master.validateNewTopicName("topic_1"));
		assertNull(Master.validateNewTopicName("中文主题"));

		var masterHome = tempDir.resolve("mqmaster").toString();
		var managerHome = tempDir.resolve("mqmanager");
		var master = new Zeze.MQ.Master.Main(masterHome, MqNetTestSupport.masterConfig(masterPort));
		var manager = new MQManager(managerHome.toString(), MqNetTestSupport.managerConfig(masterPort, proxyPort));
		var agent = new MasterAgent(MqNetTestSupport.clientConfig(masterPort));
		try {
			master.start();
			manager.start();
			agent.startAndWaitConnectionReady();

			// 既有 topic 带盘上积压（appendMessage 直接落盘，等价已投递未消费的存量消息）。
			agent.createMQ("Foo", 1, null);
			var file = manager.getQueueForTest("Foo").get(0).getFileForTest();
			for (long id = 0; id < 3; ++id)
				file.appendMessage(MqTestSupport.messageOf(id));
			var fooData = managerHome.resolve("Foo").resolve("0.0");
			assertTrue(Files.exists(fooData), "Foo 的段文件必须存在");
			var backlogBytes = fooData.toFile().length();
			assertTrue(backlogBytes > 0, "积压必须已落盘");

			// 大小写别名 topic 创建：必须被拒（eTopicFsAlias），且既有积压完好。
			// bug形态（红，Windows实毁；Linux上为"折叠冲突未拒绝"）：创建成功，
			// Manager 为 "foo" 构造分区时与 "Foo" 共享目录，幽灵段判定+未提交尾巴
			// 恢复把共享段文件 truncate(0)——既有积压被一次性摧毁。
			RuntimeException aliasRejected = null;
			try {
				agent.createMQ("foo", 1, null);
			} catch (RuntimeException e) {
				aliasRejected = e;
			}
			// 先断言积压完好（红：Windows上此刻已被truncate(0)；绿：被拒未触碰）。
			assertEquals(backlogBytes, fooData.toFile().length(),
					"别名topic创建不得摧毁既有topic的盘上积压（共享目录truncate形态）");
			assertNotNull(aliasRejected, "折叠别名 topic 的创建必须被拒绝（否则大小写不敏感FS上共享目录摧毁既有积压）");
			assertTrue(aliasRejected.getMessage().contains("error=" + Master.eTopicFsAlias),
					"必须以 eTopicFsAlias 拒绝: " + aliasRejected.getMessage());
			assertEquals(backlogBytes, fooData.toFile().length(),
					"别名创建被拒后既有 topic 的盘上积压必须完好无损");
			agent.openMQ("Foo"); // 既有 topic 仍可用

			// 尾随空白别名同折叠（Windows "Foo " 与 "Foo" 同目录），同样拒绝。
			assertThrows(RuntimeException.class, () -> agent.createMQ("Foo ", 1, null));

			// 回归保护：不同折叠键的正常创建不受影响。
			agent.createMQ("bar", 1, null);
			agent.openMQ("bar");
		} finally {
			agent.stop();
			manager.stop();
			master.stop();
		}
	}
}
