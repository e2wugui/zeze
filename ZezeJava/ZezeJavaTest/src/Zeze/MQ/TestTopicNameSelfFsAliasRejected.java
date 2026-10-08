package Zeze.MQ;

import harness.Extra;
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
 * topic 名"自身折叠后改变"与跨平台保留字符的创建拒绝回归（mq-01）：FND33 的折叠冲突
 * 检测只比对"新建名 vs 既有名"，对无碰撞对象的全新名字放行了两种名字-目录分叉形态——
 * (1) 尾随空白（"t "）：win32 上 CreateDirectory 对尾随空白的行为分版本两种结局，皆不可
 * 接受——剥离型（fsNamespaceKey 注释引用的经典规范，尾随点在现行构建上实测仍剥离：
 * "mix. ." 实建目录 "mix"）实建无空白目录，运行期经同一路径解析一切正常，Manager 重启
 * 后 loadMQ 按磁盘目录名装载出另一个 topic 名：meta/索引列族按 "t.0" 查找而真名是
 * "t .0"，全部命中"幽灵段+位点全零"形态——单段被 recoverTornTail 判"未提交尾巴"
 * truncate(0)、多段被 completeInterruptedDeletion 整体 file.delete()，该 topic 全部落盘
 * 消息被本进程自己的恢复逻辑物理摧毁；拒绝型（现行构建对尾随空白实测）mkdirs 失败，
 * Manager 侧构造抛出，Master 回 eCreatePartition 部分失败（误导性的"manual cleanup
 * required"，混合集群下 Linux Manager 成功、Windows Manager 失败即留真残留）。
 * (2) 含 Win32 非法文件名字符（a:b 等 &lt;&gt;:"|?*）：Linux 上创建成功（合法 ext4 名），
 * home 迁移 Windows 后 loadMQ 的 topicDir.mkdirs() 失败、FileOutputStream 抛
 * FileNotFoundException——Manager 进程无法启动，须人工清理。
 * <p>
 * 修复：validateNewTopicName（仅 CreateMQ 入口，存量零影响）补两条结构性拒绝——
 * 名字自身经尾随空白/点归一后改变即拒（折叠碰撞抓不到的"自别名"）；含跨平台保留字符
 * &lt;&gt;:"|?* 即拒（黑名单口径，非 ASCII 中文名按 FND33 声明继续合法）。Manager 侧
 * MQFileWithIndex 构造对 topicDir 不可用明确报错（防御纵深，替代指向不明的
 * FileNotFoundException）。
 */
@Extra
public class TestTopicNameSelfFsAliasRejected {
	private static final int masterPort = 26242;
	private static final int proxyPort = 26243;

	@Test
	public void testSelfFoldDivergentAndReservedCharsRejected(@TempDir Path tempDir) throws Exception {
		Zeze.Util.Task.tryInitThreadPool();

		// 直测校验缺口（修复前红：两形态均返回 null=放行）：
		// 自身折叠改变=名字在 Win32 剥离规则下物理目录名必然 != topic 名的形态。
		assertNotNull(Master.validateNewTopicName("t "), "尾随空白：折叠后改变（Win32目录名分叉）");
		assertNotNull(Master.validateNewTopicName("t\t"), "尾随制表符：同形态");
		assertNotNull(Master.validateNewTopicName("t. . "), "尾随点/空白混合：同形态");
		assertNotNull(Master.validateNewTopicName(" "), "纯空白名");
		// 跨平台保留字符（Win32 文件名非法集）：Linux 合法创建、迁移 Windows 后启动死结。
		for (var c : new char[]{'<', '>', ':', '"', '|', '?', '*'})
			assertNotNull(Master.validateNewTopicName("a" + c + "b"), "保留字符 " + c);
		assertNotNull(Master.validateNewTopicName("a:b"), "冒号形态（迁移后启动死结）");
		// 合法名不受影响（存量兼容口径不变）：ASCII 常规、内部空白、非 ASCII（中文）。
		assertNull(Master.validateNewTopicName("topic_1"));
		assertNull(Master.validateNewTopicName("my topic"));
		assertNull(Master.validateNewTopicName("中文主题"));
		// 大小写混合名仍合法：Win32 创建目录保留给定大小写（仅查找不敏感），自不分叉——
		// 自折叠检查只拒尾随空白/点形态，不得波及（TestTopicNameFsAliasRejected 的 Foo 依赖）。
		assertNull(Master.validateNewTopicName("Foo"));

		var masterHome = tempDir.resolve("mqmaster").toString();
		var managerHome = tempDir.resolve("mqmanager");
		var master = new Zeze.MQ.Master.Main(masterHome, MqNetTestSupport.masterConfig(masterPort));
		var manager = new MQManager(managerHome.toString(), MqNetTestSupport.managerConfig(masterPort, proxyPort));
		var agent = new MasterAgent(MqNetTestSupport.clientConfig(masterPort));
		try {
			master.start();
			manager.start();
			agent.startAndWaitConnectionReady();

			// bug形态（红，win32实毁链的头）：createMQ("t ") 现状放行——实际目录已分叉为 "t"，
			// 只待一次重启即触发装载恢复摧毁全部落盘数据（见类注释）。修复后必须拒绝。
			var trailingSpaceRejected = assertThrows(RuntimeException.class, () -> agent.createMQ("t ", 1, null),
					"尾随空白 topic 的创建必须被拒绝（否则 Win32 目录名分叉，重启装载摧毁全部落盘数据）");
			assertTrue(trailingSpaceRejected.getMessage().contains("error=" + Master.eTopicHasReserveChar),
					"必须以 eTopicHasReserveChar 拒绝: " + trailingSpaceRejected.getMessage());
			// 拒绝即未触碰磁盘：分叉目录（Win32 实际目录名 "t"）不得存在。
			assertTrue(!Files.exists(managerHome.resolve("t")),
					"被拒的尾随空白 topic 不得在 Manager home 留下分叉目录");

			// 保留字符名同样拒绝（修复前 win32 上为 eCreatePartition 部分失败误导、Linux 上放行）。
			var reservedRejected = assertThrows(RuntimeException.class, () -> agent.createMQ("a:b", 1, null),
					"含 Win32 保留字符的 topic 名必须被拒绝（否则跨平台迁移后 Manager 启动死结）");
			assertTrue(reservedRejected.getMessage().contains("error=" + Master.eTopicHasReserveChar),
					"必须以 eTopicHasReserveChar 拒绝: " + reservedRejected.getMessage());
			assertTrue(!Files.exists(managerHome.resolve("a:b")),
					"被拒的保留字符 topic 不得在 Manager home 留下目录");

			// 回归保护：合法名（含内部空白、大小写混合）创建/打开不受影响。
			agent.createMQ("normalTopic", 1, null);
			agent.openMQ("normalTopic");
			agent.createMQ("my topic", 1, null);
			agent.openMQ("my topic");
			agent.createMQ("Foo", 1, null);
			agent.openMQ("Foo");
		} finally {
			agent.stop();
			manager.stop();
			master.stop();
		}
	}
}
