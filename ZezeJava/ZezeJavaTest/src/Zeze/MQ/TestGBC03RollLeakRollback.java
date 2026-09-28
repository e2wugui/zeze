package Zeze.MQ;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import Zeze.Builtin.MQ.BMessage;
import Zeze.Util.RocksDatabase;
import harness.Fast;
import org.junit.jupiter.api.parallel.ResourceLock;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * FND22 GB-C03 回归（fix-the-fix 的修法边界缺口）：FND21 GB-C04 滚段改 getOrAddTable → new 流
 * →字段替换→关旧流，对文件面的"失败无外渗"论证成立，但对列族面不成立——getOrAddTable 的
 * createColumnFamily 是即刻持久化的外部副作用，open 失败（EMFILE/ENOSPC/目录占位等）时列族
 * 残留：在 rocksdb 中存在、在 tableMap 中注册、不在 indexes 中、无数据文件。泄漏不可回收且
 * 可累积：每次失败尝试的 base 名不同（nextMessageId 递增），EMFILE 持续期间每 makeIndexPeriod
 * 条消息泄漏一个空列族；deletePartitionStorage 按文件名反推列族名来 drop——无文件则永不被清，
 * 重启扫描同样不注册（无文件），成为 rocksdb 元数据里的永久孤儿。
 * <p>
 * 修复形态（案卷处置建议）：滚段分支对 open 包 try/catch，失败时用 getOrAddTable(name, isNew)
 * 的 isNew 出参判定"本调用新建"，仅对新建者 database.dropTable(name) 回滚后原样上抛——重试
 * 路径 base 已换新名，幂等先行不受影响；备选"文件先行"会复活 GB-C04 要防的无列族幽灵段，不取。
 * <p>
 * 判别（确定性注入沿用 TestFnd21GBC04：滚段目标被同名目录占位，open 确定失败）：
 * ① 内存面：失败后 tableMap 不得残留本次新建的 "topic.0.100"（旧代码判红：已注册）；
 * ② 磁盘面：关库后 rocksdb 列族清单不得含 "topic.0.100"（真泄漏面，旧代码判红：永久孤儿）；
 * ③ 恢复链（双绿守卫）：解除占位后追加，下一个整除点（base=200）滚段重试成功、fill 跨失败
 * 窗口按 id 有序读回——回滚不破坏 GB-C04 的旧流可用语义。
 * <p>
 * 注：trunkFileSize/makeIndexPeriod 静态字段小值快滚、finally 恢复（TestGBD02SegmentRecycle
 * 先例，08bd9cbe8 教训）。
 */
@Fast
@ResourceLock("mq-file-statics") // MQFileWithIndex静态字段(trunkFileSize/makeIndexPeriod)操纵的测试类互斥（FND22门禁插曲：并行改写使滚段点漂移注入失灵）
public class TestGBC03RollLeakRollback {

	@Test
	public void testRollOpenFailureRollsBackNewColumnFamily(@TempDir Path tempDir) throws Exception {
		var home = tempDir.resolve("db").toString();
		var oldTrunkFileSize = MQFileWithIndex.trunkFileSize;
		var oldMakeIndexPeriod = MQFileWithIndex.makeIndexPeriod;
		MQFileWithIndex.trunkFileSize = 512; // 小段快滚
		MQFileWithIndex.makeIndexPeriod = 100; // 滚段点=nextMessageId=100（整除栅格，与 GB-C04 测试一致）
		try {
			try (var database = new RocksDatabase(home)) {
				var file = new MQFileWithIndex(home, database, "topic", 0);
				try {
					// 写满旧段至 id98（fileOffset 远超 trunk；id98 的 next=99 非整除点不滚段）。
					for (long id = 0; id < 99; ++id)
						file.appendMessage(Fnd19MqTestSupport.messageOf(id));
					Assertions.assertEquals(99, file.getNextMessageId());

					// 注入：滚段目标 "0.100" 被同名目录占位——new FileOutputStream 确定失败
					//（模拟 EMFILE/ENOSPC 类资源耗尽；getOrAddTable 已先行成功）。
					var blocked = Path.of(home, "topic", "0.100");
					Assertions.assertTrue(blocked.toFile().mkdirs(), "注入：滚段目标被目录占位");

					// append(id99)：本条已提交（write+meta 先于滚段），滚段开流失败上抛
					//（GB-C04 契约面不变）。
					Assertions.assertThrows(RuntimeException.class,
							() -> file.appendMessage(Fnd19MqTestSupport.messageOf(99)),
							"滚段开流失败按契约上抛");
					Assertions.assertEquals(100, file.getNextMessageId(),
							"滚段失败前本条消息已提交（write+meta 先于滚段）");

					// 【判别点①】内存面：本次新建的列族必须回滚除名——旧代码残留在 tableMap
					//（无数据文件、不在 indexes 的注册孤儿）。
					Assertions.assertNull(database.getTable("topic.0.100"),
							"滚段 open 失败必须回滚本次新建的列族（FND22 GB-C03：孤儿列族"
									+ "——磁盘真相上报/删除清理/重启扫描三面皆不可见，永不可回收）");

					// 【判别点③-前半】恢复链：解除占位，追加至下一个整除点——滚段重试成功
					//（重试 base 已换新名=200，证明回滚不影响幂等先行的重试路径）。
					Assertions.assertTrue(blocked.toFile().delete(), "解除滚段目标占位");
					for (long id = 100; id < 200; ++id)
						file.appendMessage(Fnd19MqTestSupport.messageOf(id));
					Assertions.assertEquals("0.200", file.getLastFile().getName(),
							"下一个整除点滚段重试成功（GB-C04 语义不回归）");
					Assertions.assertTrue(Path.of(home, "topic", "0.200").toFile().isFile(), "新段文件已建");

					// 【判别点③-后半】数据完整性：跨失败窗口的消息可按 id 有序读回。
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

			// 【判别点②】磁盘面（真泄漏面）：关库后 rocksdb 列族清单——失败尝试的 "topic.0.100"
			// 不得残留（旧代码判红：重启后依旧存在的永久元数据孤儿）；重试成功的 "topic.0.200" 在
			//（健全性锚点，证明清单读取有效）。
			var cfNames = new HashSet<String>();
			for (var descriptor : RocksDatabase.getCfDescriptors(home))
				cfNames.add(new String(descriptor.getName(), StandardCharsets.UTF_8));
			Assertions.assertTrue(cfNames.contains("topic.0.200"), "健全性锚点：重试成功的段列族在");
			Assertions.assertTrue(cfNames.contains("topic.0.0"), "健全性锚点：旧段列族在");
			Assertions.assertFalse(cfNames.contains("topic.0.100"),
					"失败尝试的列族不得残留在 rocksdb 元数据（FND22 GB-C03：EMFILE 持续期每"
							+ " makeIndexPeriod 条泄漏一个、无上界累积）");
		} finally {
			// 静态字段恢复（类级并行下残留值改写他测锚点，08bd9cbe8 教训）
			MQFileWithIndex.trunkFileSize = oldTrunkFileSize;
			MQFileWithIndex.makeIndexPeriod = oldMakeIndexPeriod;
		}
	}
}
