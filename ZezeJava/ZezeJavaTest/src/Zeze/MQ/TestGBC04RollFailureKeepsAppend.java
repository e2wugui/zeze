package Zeze.MQ;

import java.nio.file.Path;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import Zeze.Builtin.MQ.BMessage;
import Zeze.Util.RocksDatabase;
import harness.Fast;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * FND21 GB-C04 回归：appendMessage 滚段处先 close 旧流再 new FileOutputStream 新流——构造失败
 * （EMFILE/ENOSPC/目录项冲突等）后 lastFileOutputStream 停留在"已关闭"对象上，此后每次追加在
 * getChannel().size() 即抛 ClosedChannelException，分区追加能力到重启前永久丧失（本条消息已
 * 提交，无数据损坏——非对称静默降级，仅运行期可用性损失）。
 * <p>
 * 修复形态：先开后关+资源就绪才发布——getOrAddTable（幂等）→ new 新流（失败则旧流仍开、字段
 * 未动、open 失败原子不留文件）→ 字段替换 → 后关旧流；indexes.put 后移（失败不留无数据文件的
 * 幽灵段）。滚段失败留待条件重合自然重试（size 条件持续成立，modulo 条件在下一个
 * makeIndexPeriod 整除点重合，旧段有限超限）。
 * <p>
 * 确定性注入：滚段目标路径被同名目录占位（FileOutputStream 构造对目录确定抛
 * FileNotFoundException，模拟资源耗尽类失败）。滚段点=append(id99) 提交后 nextMessageId=100
 *（makeIndexPeriod 整除且 fileOffset 已超 trunk）。判别：解除占位后追加——修复代码恢复（判绿），
 * 旧代码每次追加恒抛 ClosedChannelException（判红）。顺带固化恢复后滚段重试成功与 fill 读回。
 * <p>
 * 注：trunkFileSize/makeIndexPeriod 静态字段小值快滚、finally 恢复（TestGBD02SegmentRecycle
 * 先例）。
 */
@Fast
public class TestGBC04RollFailureKeepsAppend {

	@Test
	public void testRollOpenFailureKeepsOldStreamAppendable(@TempDir Path tempDir) throws Exception {
		var home = tempDir.resolve("db").toString();
		try {
			try (var database = new RocksDatabase(home)) {
				var file = new MQFileWithIndex(home, database, "topic", 0);
				file.trunkFileSize = 512;
				file.makeIndexPeriod = 100;
				try {
					// 写满旧段至 id98（fileOffset 远超 trunk；id98 的 next=99 非整除点不滚段）。
					for (long id = 0; id < 99; ++id)
						file.appendMessage(Fnd19MqTestSupport.messageOf(id));
					Assertions.assertEquals(99, file.getNextMessageId());

					// 注入：滚段目标 "0.100" 被同名目录占位——new FileOutputStream 确定失败。
					var blocked = Path.of(home, "topic", "0.100");
					Assertions.assertTrue(blocked.toFile().mkdirs(), "注入：滚段目标被目录占位");

					// append(id99)：本条已提交（write+meta 先于滚段），滚段开流失败上抛
					//（新旧代码一致的契约面——差异在失败后的字段状态）。
					Assertions.assertThrows(RuntimeException.class,
							() -> file.appendMessage(Fnd19MqTestSupport.messageOf(99)),
							"滚段开流失败按契约上抛");
					Assertions.assertEquals(100, file.getNextMessageId(),
							"滚段失败前本条消息已提交（write+meta 先于滚段）");

					// 解除占位（资源耗尽恢复形态）。
					Assertions.assertTrue(blocked.toFile().delete(), "解除滚段目标占位");

					// 【判别点】恢复后的下一次追加：修复代码旧流仍开、正常写入旧段
					//（滚段留待下一个整除点重试）；旧代码字段停留在已关闭流上恒抛
					// ClosedChannelException（FND21 GB-C04：分区追加能力到重启前永久丧失）。
					Assertions.assertDoesNotThrow(() -> file.appendMessage(Fnd19MqTestSupport.messageOf(100)),
							"滚段失败不得锁死追加：旧流必须保持可用");
					Assertions.assertEquals(101, file.getNextMessageId());
					Assertions.assertEquals("0.0", file.getLastFile().getName(),
							"滚段未完成：仍写旧段（有限超限 <makeIndexPeriod 条，非损坏）");

					// 恢复后的滚段重试：append(id199) 提交后 nextMessageId=200 的整除点开新段成功。
					for (long id = 101; id < 200; ++id)
						file.appendMessage(Fnd19MqTestSupport.messageOf(id));
					Assertions.assertEquals("0.200", file.getLastFile().getName(), "下一个整除点滚段成功");
					Assertions.assertTrue(Path.of(home, "topic", "0.200").toFile().isFile(), "新段文件已建");

					// 数据完整性：跨失败窗口的消息可按 id 有序读回（fill 定位走索引锚点）。
					Queue<BMessage.Data> queue = new ConcurrentLinkedQueue<>();
					file.fillMessage(queue, 99, 200);
					Assertions.assertEquals(101, queue.size(), "id99..199 全部可读回（含滚段失败前后两窗口）");
					var expect = 99;
					for (var message : queue)
						Assertions.assertEquals(expect++, message.getTimestamp(), "按 id 有序");
				} finally {
					file.close();
				}
			}
		} finally {
		}
	}
}
