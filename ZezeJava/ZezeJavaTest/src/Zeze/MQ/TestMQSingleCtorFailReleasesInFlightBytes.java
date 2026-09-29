package Zeze.MQ;

import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Queue;
import Zeze.Builtin.MQ.BMessage;
import Zeze.Net.Binary;
import Zeze.Util.RocksDatabase;
import harness.Fast;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * MQSingle 构造失败路径的记账归还回归：构造期装载 pullMessage(true) 中途抛错（记录头
 * 错位/索引缺失/IO 错形态）时，已装载消息经 admitFillBytes 入账（分区 queueBytes +
 * Manager 级共享 totalInFlightBytes），而实例不发布——ack/死信出账与 close() 终态释放
 * 均不可达。不归还则全局在飞预算被每次失败构造永久虚占（Master 对 CreatePartition 的
 * 重试可累积至耗尽，全分区退化为队头单条推进且无根因日志，仅重启可恢复）。
 * <p>
 * 注：文件放 src/MQ/ 但声明 package Zeze.MQ——需要访问 MQSingle/MQManager 的包内测试缝
 * （注入 MQFileWithIndex 的构造器、getQueueForTest、totalInFlightBytes），
 * 与 TestMQSingleDirectEnqueue 先例一致。分区级 queueBytes 随实例死亡不可读，
 * 全局 totalInFlightBytes 是其精确镜像（同点同额入/出账），作为断言观察点。
 */
@Fast
public class TestMQSingleCtorFailReleasesInFlightBytes {

	/**
	 * 装载前两条后注入记录错位形态的抛错（等价 fillMessage 读到第 3 条记录头 id 错位/
	 * readFully IO 错的确定性故障），走 MQSingle 构造 catch 的真实失败路径。
	 */
	static class CorruptMidFillFile extends MQFileWithIndex {
		CorruptMidFillFile(String home, RocksDatabase database) throws Exception {
			super(home, database, "topic", 0);
		}

		@Override
		public long fillMessage(Queue<BMessage.Data> messageQueue, long headMessageId, long endMessageId,
								MQFileWithIndex.FillBudget budget) {
			super.fillMessage(messageQueue, headMessageId, Math.min(endMessageId, headMessageId + 2), budget);
			throw new RuntimeException("injected record corruption after messageId=" + (headMessageId + 1));
		}
	}

	private static BMessage.Data messageOf(long id) {
		var message = new BMessage.Data();
		message.setTimestamp(id);
		message.setBody(new Binary(new byte[1024]));
		return message;
	}

	@Test
	public void testFailedCtorReturnsTotalInFlightBudget(@TempDir Path tempDir) throws Exception {
		var home = tempDir.resolve("db").toString();
		var manager = new MQManager(home, new Zeze.Config());
		try {
			// 盘上造 5 条积压（绕开内存队列直接落盘；Manager 级共享计数从 0 起账）。
			manager.createPartition("topic", new HashSet<>(List.of(0)));
			var file = manager.getQueueForTest("topic").get(0).getFileForTest();
			for (long id = 1; id <= 5; ++id)
				file.appendMessage(messageOf(id));
			manager.getQueueForTest("topic").removePartition(0); // 摘除（close 归还其记账=0）

			// 构造失败 ×2（Master 重试/同 topic 反复重建的形态）：每次装载 2 条后抛错。
			// 修复前各泄漏 2 条的在飞字节（实例丢弃、无出账方）并累积；修复后随实例死亡
			// 即时归还（红态实测两次共虚占 4120 字节）。
			Assertions.assertThrows(RuntimeException.class, () -> new MQSingle(new MQPartition(manager),
					"topic", 0, new CorruptMidFillFile(home, manager.getRocksDatabase())));
			Assertions.assertThrows(RuntimeException.class, () -> new MQSingle(new MQPartition(manager),
					"topic", 0, new CorruptMidFillFile(home, manager.getRocksDatabase())));

			Assertions.assertEquals(0L, manager.totalInFlightBytes.get(),
					"构造失败=实例不发布，已入账的在飞字节必须归还（否则全局预算被失败构造永久虚占）");
		} finally {
			manager.stop();
		}
	}
}
