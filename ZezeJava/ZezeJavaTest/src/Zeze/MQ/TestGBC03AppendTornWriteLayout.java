package Zeze.MQ;

import java.io.RandomAccessFile;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import Zeze.Builtin.MQ.BMessage;
import Zeze.Serialize.ByteBuffer;
import Zeze.Util.RocksDatabase;
import harness.Fast;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * FND20 GB-C03 回归：appendMessage 部分写后进程继续运行——追加流（O_APPEND）使下一次追加
 * 以物理尾定位，重发记录接在自己的撕裂前缀之后，段内布局错位；fillMessage 定位环按孤儿头
 * （id/size 与真记录相同）命中后跨界读体，混合字节被 decode 成"成功"的消息静默投递；
 * recoverTornTail 被同 id 垃圾头欺骗（pos += 12+size 落进真记录体内、expectId 到顶退出），
 * truncate 反向截掉真记录尾部。修复：write 失败立即截回上次成功结尾（撕裂悬挂态未解除前
 * 拒绝再追加），"meta.next 是唯一提交点"的恢复语义重新成立。
 * <p>
 * 文件级注入（截断/部分写形态）：短写无法确定性注入 IO 错误，本测试手工构造与 appendMessage
 * 完全同构的撕裂前缀字节与修复跟踪的悬挂状态，验证 recoverTornTail 与 fillMessage 行为：
 * ① 悬挂态下次 append 回滚孤儿前缀、重发记录原地重写，fillMessage 干净读回（旧代码接在
 * 孤儿后，文件长度即判红）；
 * ② 纯撕裂尾的崩溃形态（进程死亡）仍由构造期 recoverTornTail 正确截断自愈（修复不得改变）。
 * <p>
 * 注：tornWritePending/tornRollbackOffset 经反射置位（修复引入；旧基线缺失则按旧布局继续，
 * 由断言判红）。布局约定见 Fnd19MqTestSupport。
 */
@Fast
public class TestGBC03AppendTornWriteLayout {

	/** 与 appendMessage 完全同构的记录字节（Long8 BE id + Int4 LE size + BMessage 体）。 */
	private static byte[] recordBytes(long messageId, BMessage.Data message) {
		var bb = ByteBuffer.Allocate();
		bb.WriteLong8(messageId);
		var sizeOffset = bb.WriteIndex;
		bb.WriteInt4(0);
		message.encode(bb);
		ByteBuffer.intLeHandler.set(bb.Bytes, sizeOffset, bb.WriteIndex - sizeOffset - 4);
		return java.util.Arrays.copyOfRange(bb.Bytes, bb.ReadIndex, bb.ReadIndex + bb.size());
	}

	/**
	 * 索引表裁剪到只含 id=0（与 TestMQFileWithIndexSizeGuards 同款）：定位环确定性地从
	 * 段首扫描，并与 makeIndexPeriod 静态字段的并行改写解耦。
	 */
	private static void scrubIndexToFirstEntryOnly(RocksDatabase database, String indexTable) throws Exception {
		var table = database.getOrAddTable(indexTable);
		var keysToDelete = new ArrayList<byte[]>();
		try (var it = table.iterator()) {
			it.seekToFirst();
			while (it.isValid()) {
				keysToDelete.add(it.key());
				it.next();
			}
		}
		for (var key : keysToDelete)
			if (0 != ByteBuffer.ToLongBE(key, 0))
				table.delete(key);
	}

	/** 反射置撕裂悬挂状态（修复跟踪的"上次成功结尾+悬挂"）；旧基线无此字段返回 false。 */
	private static boolean trySetTornPending(MQFileWithIndex file, long rollbackOffset) {
		try {
			var offset = MQFileWithIndex.class.getDeclaredField("tornRollbackOffset");
			offset.setAccessible(true);
			var pending = MQFileWithIndex.class.getDeclaredField("tornWritePending");
			pending.setAccessible(true);
			offset.setLong(file, rollbackOffset);
			pending.setBoolean(file, true);
			return true;
		} catch (NoSuchFieldException e) {
			return false; // 旧代码（FND20 GB-C03 修复不存在）：按旧布局继续，由断言判红
		} catch (ReflectiveOperationException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * ① 短写+继续运行的布局契约：撕裂前缀（头12B+半个体）悬挂时，重发的同 id 记录必须回滚
	 * 孤儿前缀后原地重写——文件长度==上次成功结尾+整条记录、头部物理位于上次成功结尾、
	 * fillMessage 干净读回该记录（不跨界、不投递混合字节）。旧代码（无回滚）以 channel.size()
	 * 定位=孤儿之后：长度多出撕裂前缀，直接判红。
	 */
	@Test
	public void testTornPrefixRolledBackAndResendRewrittenInPlace(@TempDir Path tempDir) throws Exception {
		var home = tempDir.resolve("db").toString();
		var database = new RocksDatabase(home);
		var file = new MQFileWithIndex(home, database, "topic", 0);
		var dataFile = file.getLastFile();
		try {
			for (long id = 0; id < 5; ++id)
				file.appendMessage(Fnd19MqTestSupport.messageOf(id));
			scrubIndexToFirstEntryOnly(database, "topic.0.0");
			var committedEnd = dataFile.length();

			// 手工注入短写撕裂前缀（POSIX 短写语义：出错前已持久化的记录前缀字节）。
			var full = recordBytes(5, Fnd19MqTestSupport.messageOf(5));
			var tornLen = 12 + Math.max(1, (full.length - 12) / 2);
			try (var raf = new RandomAccessFile(dataFile, "rw")) {
				raf.seek(committedEnd);
				raf.write(full, 0, (int)tornLen);
			}

			// 置修复跟踪的悬挂状态（旧代码无此状态，跳过后按旧布局追加，由长度断言判红）。
			var fixed = trySetTornPending(file, committedEnd);

			file.appendMessage(Fnd19MqTestSupport.messageOf(5)); // 磁盘腾空后生产者重发

			Assertions.assertEquals(committedEnd + full.length, dataFile.length(),
					fixed ? "撕裂前缀必须被回滚，重发记录原地重写（FND20 GB-C03）"
							: "旧代码：重发记录接在孤儿前缀之后，段内物理布局错位（FND20 GB-C03）");
			try (var raf = new RandomAccessFile(dataFile, "r")) {
				raf.seek(committedEnd);
				var head = new byte[12];
				raf.readFully(head);
				var bbHead = ByteBuffer.Wrap(head);
				Assertions.assertEquals(5, bbHead.ReadLong8(), "重发记录头须位于上次成功结尾");
				Assertions.assertEquals(full.length - 12, bbHead.ReadInt4(), "记录体长与编码一致");
			}
			Queue<BMessage.Data> queue = new ConcurrentLinkedQueue<>();
			file.fillMessage(queue, 5, 6);
			Assertions.assertEquals(1, queue.size(), "fill 恰好装载重发的记录");
			Assertions.assertEquals(5, queue.poll().getTimestamp(), "fill 干净读回（不跨界不投递混合字节）");
			Assertions.assertEquals(6, file.getNextMessageId(), "重发记录恰好提交一条");
		} finally {
			file.close();
			database.close();
		}
	}

	/**
	 * ② 崩溃形态回归（修复不得改变）：进程死亡留下纯撕裂前缀（其后无重发记录），重启构造期
	 * recoverTornTail 按未提交尾巴正确截断，位点不动，追加/装载照常干净。
	 */
	@Test
	public void testPureTornTailRecoveryUnchanged(@TempDir Path tempDir) throws Exception {
		var home = tempDir.resolve("db2").toString();
		var topicDir = new java.io.File(home, "topic2");
		//noinspection ResultOfMethodCallIgnored
		topicDir.mkdirs();
		var segmentFile = new java.io.File(topicDir, "0.0");
		long committedEnd;
		long tornLen;
		var database = new RocksDatabase(home);
		var file = new MQFileWithIndex(home, database, "topic2", 0);
		try {
			for (long id = 0; id < 3; ++id)
				file.appendMessage(Fnd19MqTestSupport.messageOf(id));
			committedEnd = segmentFile.length();

			// 崩溃前短写留下的纯撕裂前缀。
			var full = recordBytes(3, Fnd19MqTestSupport.messageOf(3));
			tornLen = 12 + Math.max(1, (full.length - 12) / 2);
			try (var raf = new RandomAccessFile(segmentFile, "rw")) {
				raf.seek(committedEnd);
				raf.write(full, 0, (int)tornLen);
			}
		} finally {
			file.close();
			database.close();
		}
		Assertions.assertEquals(committedEnd + tornLen, segmentFile.length(), "撕裂前缀注入前置");

		// 重启：构造期 recoverTornTail 截断未提交尾巴。
		try (var database2 = new RocksDatabase(home)) {
			var file2 = new MQFileWithIndex(home, database2, "topic2", 0);
			try {
				Assertions.assertEquals(3, file2.getNextMessageId(), "位点不回拨（撕裂记录未提交）");
				Assertions.assertEquals(committedEnd, segmentFile.length(), "recoverTornTail 须截断未提交尾巴");
				file2.appendMessage(Fnd19MqTestSupport.messageOf(3));
				Queue<BMessage.Data> queue = new ConcurrentLinkedQueue<>();
				file2.fillMessage(queue, 3, 4);
				Assertions.assertEquals(1, queue.size());
				Assertions.assertEquals(3, queue.poll().getTimestamp(), "恢复后追加/装载照常干净");
			} finally {
				file2.close();
			}
		}
	}
}
