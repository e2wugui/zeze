package Zeze.MQ;

import java.lang.reflect.InvocationTargetException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;
import Zeze.Config;
import harness.Fast;
import org.junit.jupiter.api.parallel.ResourceLock;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * FND20 GB-D01 回归：loadMonitor 中段回收与 Master 可达性的解耦（拍板 A：回收前置）。
 * <p>
 * 修复前：loadMonitor body 是"两个远端 rpc + 一个本地批量操作"的串行链，纯本地的段回收排最后。
 * Master 不可达时 reportLoad 的 SendForWait(null) 即抛（GetSocket() 返回 null → Send 失败 →
 * future.setException → await 抛），DaemonTimer 对 body 异常"记日志，链继续"——Master 降级的
 * 整个期间每轮回收整体跳过（120s一轮），GB-D02 的磁盘有界承诺退回"历史消息总量无上界"形态，
 * 而降级期恰是积压最大、最需要回收的时候。
 * <p>
 * 修复后不变式：loadMonitor 到达段回收步骤之前不执行任何远端调用——回收的可用性只依赖本地
 * 资源（rocksdb+文件系统）与配置开关；上报失败仍向上抛（损失≤一个周期）。
 * <p>
 * 直驱 DaemonTimer 的同一 body（loadMonitor 私有，反射调用；新旧基线皆有该方法，红绿由
 * 段文件断言区分）。Master 不可达形态=manager 未 start（无任何 master 连接）。段前提与
 * 断言形态对齐 TestFnd19GBD02SegmentRecycle（trunkFileSize/makeIndexPeriod 静态小值快滚、
 * finally 恢复；布局约定见 Fnd19MqTestSupport）。
 */
@Fast
@ResourceLock("mq-file-statics") // MQFileWithIndex静态字段(trunkFileSize/makeIndexPeriod)操纵的测试类互斥（FND22门禁插曲：并行改写使滚段点漂移注入失灵）
public class TestFnd20GBD01RecycleBeforeRemoteReport {

	/** 反射直驱 loadMonitor（loadMonitorTimer 周期体的同一入口）。 */
	private static void runLoadMonitor(MQManager manager) throws Exception {
		var m = MQManager.class.getDeclaredMethod("loadMonitor");
		m.setAccessible(true);
		m.invoke(manager);
	}

	/** topic 目录下按"分区号.段基"命名的段基列表（升序）。 */
	private static List<Long> segmentBases(Path topicDir) throws Exception {
		try (var stream = Files.list(topicDir)) {
			return stream.map(p -> p.getFileName().toString())
					.filter(n -> n.matches("\\d+\\.\\d+"))
					.map(n -> Long.parseLong(n.split("\\.")[1]))
					.sorted()
					.collect(Collectors.toList());
		}
	}

	@Test
	public void testRecycleRunsDespiteRemoteReportFailure(@TempDir Path tempDir) throws Exception {
		var home = tempDir.resolve("manager").toString();
		var topicDir = Path.of(home, "t");
		var oldTrunkFileSize = MQFileWithIndex.trunkFileSize;
		MQFileWithIndex.trunkFileSize = 1024; // 小段快滚
		// 不动 makeIndexPeriod：默认 100 下滚段发生在整百 id（段基 0,100,200,...），段文件照常多段；
		// 避免加入 TestMQFileWithIndexTornTail 注释所记录的"并行测试改写该静态"干扰面
		//（makeIndexPeriod=1 会改变其撕裂恢复的锚点选择）。
		try {
			var manager = new MQManager(home, new Config());
			try {
				manager.getMqConfig().setSegmentRecycleDelayMs(0); // 软删除窗口立即到期（默认60s窗口同一逻辑）
				manager.createPartition("t", new HashSet<>(List.of(0)));
				var single = manager.getQueueForTest("t").get(0);
				var file = single.getFileForTest();
				for (long id = 0; id < 350; ++id)
					single.sendMessage(Fnd19MqTestSupport.sendMessageOf(id)); // bindSocket=null：只装载不推送
				var bases = segmentBases(topicDir);
				Assertions.assertTrue(bases.size() >= 3, "多段前提不成立，实际段数=" + bases.size());

				// 首段全部确认（水位推过段尾 bases[1]，tryRecycle 的整段回收条件成立）。
				while (file.getFirstMessageId() < bases.get(1))
					file.increaseFirstMessageId();

				// Master 不可达（manager 未 start：GetSocket()==null → SendForWait 即抛）：
				// 上报失败必须原样上抛（既有语义不变），但本轮段回收不得被连坐跳过。
				Assertions.assertThrows(InvocationTargetException.class, () -> runLoadMonitor(manager),
						"上报失败必须上抛（DaemonTimer 记日志续约，损失≤一个周期）");
				Assertions.assertEquals(bases.subList(1, bases.size()), segmentBases(topicDir),
						"远端上报失败不得连坐跳过本轮段回收（FND20 GB-D01：回收前置，"
								+ "Master 降级期磁盘有界承诺同样成立）");
			} finally {
				manager.stop(); // 未 start 的 stop 安全（TestFnd19GBD06 同款依据）
			}
		} finally {
			MQFileWithIndex.trunkFileSize = oldTrunkFileSize;
		}
	}
}
