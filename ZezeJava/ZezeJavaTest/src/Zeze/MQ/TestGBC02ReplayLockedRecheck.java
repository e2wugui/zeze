package Zeze.MQ;

import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReentrantLock;
import Zeze.Builtin.MQ.BMessage;
import Zeze.Config;
import Zeze.Serialize.ByteBuffer;
import Zeze.Util.Task;
import harness.Fast;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.parallel.ResourceLock;

/**
 * FND22 GB-C02 回归（fix-the-fix）：FND21 GB-C05 给 replayDeadLetter 加的首尾两道 stopped 闸
 * 都是无锁裸读——"复查 stopped==false 之后、dlq.get/dlq.delete 执行前"两条语句之间，stop 可
 * 完整走完（proxyServer.stop、N 个分区 close 排空、rocksDatabase.close），其后的触库是对已关库
 * 的悬垂句柄 native 调用（close 契约）。GB-C05 自称收口的窗口没有闭合。
 * <p>
 * 修复形态（案卷处置建议）：对齐同文件同族管理面入口（createPartition/deletePartition）——
 * 整个方法体（getTable/get/sendMessage/复核/delete）包进 managementLock，锁内复查 stopped。
 * 正确性：stopped 在 stop 取 managementLock 之前置位，持锁后复查必见终态；stop 关库前的
 * tryLock(25s) 有界等待在飞 replay 出锁后再关库（超预算 ε 同既有口径）。
 * <p>
 * 判别（首部窗口的时序终点）：占位 managementLock 使 replay 停靠在取锁处（入口闸已读 false），
 * 随后模拟 stop 完整走完（置位+关库）再放行——修复代码锁内复查拒绝（IllegalStateException、
 * 未触任何库）；旧代码无锁直行、早已完整返回（判红：无异常且消息已重放追加）。
 * <p>
 * 尾部窗口（sendMessage 后置位）由 TestGBC05ReplayDeadLetterStopGate ③（替身分区）持续
 * 守卫——复查保留在锁内，该测试不回归即尾部闸仍生效。
 */
@ResourceLock("mq-file-statics") // 旁观者READ：与改写trunkFileSize/makeIndexPeriod的类互斥——静态被并行改小期间本类append会滚出无索引段，fillMessage seekForPrev落空即messageIndexNotFound假红（2026-10-04 test40批r5实证）；旁观者彼此READ可并行
@Fast
public class TestGBC02ReplayLockedRecheck {

	/** 死信值编码（与 MQSingle.tryDeadLetter 同构：BMessage 编码 + 8 字节 BE 时间戳尾缀）。 */
	private static byte[] dlqValue(BMessage.Data message) {
		var bb = ByteBuffer.Allocate();
		message.encode(bb);
		var stamped = java.util.Arrays.copyOfRange(bb.Bytes, bb.ReadIndex, bb.ReadIndex + bb.size() + 8);
		ByteBuffer.longBeHandler.set(stamped, stamped.length - 8, System.currentTimeMillis());
		return stamped;
	}

	private static void putDeadLetter(MQManager manager, String topic, int partitionIndex, long messageId)
			throws Exception {
		var message = Fnd19MqTestSupport.messageOf(messageId);
		var key = MQManager.dlqKey(topic, partitionIndex, messageId);
		var value = dlqValue(message);
		manager.getDlqTable().put(key, 0, key.length, value, 0, value.length);
	}

	private static void setStopped(MQManager manager, boolean value) throws Exception {
		var f = MQManager.class.getDeclaredField("stopped");
		f.setAccessible(true);
		f.setBoolean(manager, value);
	}

	@SuppressWarnings("unchecked")
	private static ReentrantLock managementLockOf(MQManager manager) throws Exception {
		var f = MQManager.class.getDeclaredField("managementLock");
		f.setAccessible(true);
		return (ReentrantLock)f.get(manager);
	}

	/**
	 * 首部窗口（GB-C02 机制点1）：入口闸读到 false 后、dlq.get 执行前，stop 完整走完关库。
	 * 修复代码停靠 managementLock.lock()（在飞管理 handler 排队形态），stop 的关库在锁排空
	 * 之后——放行时锁内复查必见 stopped 终态，先于任何触库步拒绝。
	 */
	@Test
	public void testStopCompletedBetweenGateAndBodyRejectedUnderLock(@TempDir Path tempDir) throws Exception {
		Task.tryInitThreadPool();
		var manager = new MQManager(tempDir.resolve("manager").toString(), new Config());
		try {
			manager.createPartition("t", new HashSet<>(List.of(0)));
			var single = manager.getQueueForTest("t").get(0);
			putDeadLetter(manager, "t", 0, 7);

			var managementLock = managementLockOf(manager);
			managementLock.lock(); // 占位：模拟在飞管理 handler 持锁，replay 将停靠在取锁处

			var failure = new AtomicReference<Throwable>();
			var replayer = new Thread(() -> {
				try {
					manager.replayDeadLetter("t", 0, 7);
				} catch (Throwable e) {
					failure.set(e);
				}
			}, "fnd22-gbc02-replayer");
			replayer.start();

			// 停靠判定：修复代码停驻 managementLock.lock()（WAITING）；旧代码无锁直行、毫秒内终结。
			var deadline = System.currentTimeMillis() + 5_000;
			while (replayer.isAlive() && System.currentTimeMillis() < deadline
					&& replayer.getState() != Thread.State.WAITING)
				Thread.sleep(10);

			// 模拟 stop 在"入口闸读 false 之后、方法体执行前"完整走完：最前置位 + 终末关库
			//（真实 stop 的关库在 managementLock 有界排空之后——此处由占位锁等价保证时序）。
			setStopped(manager, true);
			manager.getRocksDatabase().close();

			managementLock.unlock(); // 放行：修复代码的锁内复查在取锁后立即执行
			replayer.join(30_000);

			Assertions.assertFalse(replayer.isAlive(), "replay 必须已终结（拒绝路径不悬挂）");
			Assertions.assertNotNull(failure.get(),
					"首部窗口必须被拒绝（FND22 GB-C02：旧代码无锁裸读穿过 stop 全时长后照常触库返回）");
			Assertions.assertTrue(failure.get() instanceof IllegalStateException,
					"锁内复查的拒绝语义（旧代码判红形态：无异常正常返回，或库已关时"
							+ " IllegalArgumentException 掩盖真语义）");
			Assertions.assertTrue(failure.get().getMessage().contains("stopped"), "拒绝语义可辨识");
			Assertions.assertEquals(0, single.getFileForTest().getNextMessageId(),
					"锁内复查先于任何触库步——消息未被重放追加（旧代码已追加，判红点之一）");
		} finally {
			// stopped 已置位、库已手动关闭：仍走完整 stop 关闭分区文件流与 rocksdb
			//（rocksdb close 幂等），保证 Windows 临时目录清理。
			manager.stop();
		}
	}
}
