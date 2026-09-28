package Zeze.MQ;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import Zeze.Config;
import harness.Fast;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * FND19 GB-D01 Manager 侧删除路径回归（@Fast，未 start 的 MQManager 直驱——无网络，
 * ProcessDeletePartitionRequest 的处理体即被测入口 deletePartition）。
 * <p>
 * 口径：活分区先摘除（close+从 queues 摘）再清存储；段文件/索引列族/meta 列族全清
 *（无视水位线强制回收，GB-D02 三步形态作用于全部段）；topic 目录空则一并删除；
 * 不在活集合的分区（目录在而 queues 无——构造失败/外部残留形态）按目录扫描同样清掉；
 * 删除后同 topic 重建可用。
 * <p>
 * 注：访问 createPartition/deletePartition/getQueueForTest 包内缝（布局约定见 Fnd19MqTestSupport）。
 */
@Fast
public class TestGBD01ManagerDelete {

	@Test
	public void testDeleteLiveAndDeadPartitions(@TempDir Path tempDir) throws Exception {
		var home = tempDir.resolve("manager").toString();
		var manager = new MQManager(home, new Config());
		try {
			var topicDir = Path.of(home, "ghost");
			// 活分区 + 数据。
			manager.createPartition("ghost", new HashSet<>(java.util.List.of(0, 1)));
			var queue = manager.getQueueForTest("ghost");
			Assertions.assertNotNull(queue);
			queue.get(0).sendMessage(Fnd19MqTestSupport.sendMessageOf(1));
			queue.get(0).sendMessage(Fnd19MqTestSupport.sendMessageOf(2));
			queue.get(1).sendMessage(Fnd19MqTestSupport.sendMessageOf(3));
			Assertions.assertEquals(2, manager.queueCount());
			Assertions.assertTrue(Files.exists(topicDir.resolve("0.0")), "分区数据文件存在（删除前置）");
			Assertions.assertNotNull(manager.getRocksDatabase().getTable("ghost.0"), "meta 列族存在（删除前置）");
			Assertions.assertNotNull(manager.getRocksDatabase().getTable("ghost.0.0"), "索引列族存在（删除前置）");

			// 删除（Master 对账裁决的 Manager 处理体）：活分区先摘，存储全清。
			manager.deletePartition("ghost", new HashSet<>(java.util.List.of(0, 1)));
			Assertions.assertEquals(0, manager.queueCount(), "活分区从 queues 摘除");
			Assertions.assertFalse(topicDir.toFile().exists(), "topic 目录全清（含段文件）");
			Assertions.assertNull(manager.getRocksDatabase().getTable("ghost.0"), "meta 列族已 drop");
			Assertions.assertNull(manager.getRocksDatabase().getTable("ghost.0.0"), "索引列族已 drop");
			Assertions.assertNull(manager.getRocksDatabase().getTable("ghost.1"), "分区1 meta 列族已 drop");

			// 不在活集合的分区（目录在而 queues 无——外部残留/构造失败形态）：按目录扫描同样清掉。
			Files.createDirectories(Path.of(home, "ghost2"));
			Files.writeString(Path.of(home, "ghost2", "5.123"), "segment-leftover");
			manager.deletePartition("ghost2", new HashSet<>(java.util.List.of(5)));
			Assertions.assertFalse(Path.of(home, "ghost2").toFile().exists(), "死分区残留同样回收");

			// 删除后同 topic 重建可用（表/文件/位点全新）。
			manager.createPartition("ghost", new HashSet<>(java.util.List.of(0)));
			var rebuilt = manager.getQueueForTest("ghost").get(0);
			Assertions.assertEquals(0, rebuilt.getFileForTest().getFirstMessageId(), "重建后位点归零");
			rebuilt.sendMessage(Fnd19MqTestSupport.sendMessageOf(9)); // 无异常：append/索引/meta 全新
			Assertions.assertEquals(1, rebuilt.getFileForTest().getNextMessageId());
		} finally {
			manager.stop();
		}
	}
}
