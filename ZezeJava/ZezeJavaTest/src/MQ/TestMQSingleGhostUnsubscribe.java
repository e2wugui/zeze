package Zeze.MQ;

import java.net.SocketAddress;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import Zeze.Builtin.MQ.PushMessage;
import Zeze.Net.AsyncSocket;
import Zeze.Net.Protocol;
import Zeze.Net.Service;
import Zeze.Util.RocksDatabase;
import Zeze.Util.Task;
import Zeze.Util.TimeThrottle;
import harness.Fast;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static Zeze.MQ.Master.AbstractMaster.eConsumerNotFound;

/**
 * FND2-G2-7 回归：推送应答 eConsumerNotFound（消费端条目已删、Manager 侧订阅残留的
 * 幽灵订阅）时，handlePushResult 必须清掉 Manager 侧订阅并重排，且必须异步执行。
 * <p>
 * 旧代码对非 0 结果码无处理：重推永远 eConsumerNotFound，该分区位点永不推进，
 * 每 5 秒（Rpc 默认超时驱动）一次空转 rpc，直到 Manager 重启。
 * <p>
 * 死锁约束：arrangeConsumer 持 MQPartition 锁后经 partition.bind() 进各 MQSingle 锁；
 * handlePushResult 正持本 MQSingle 锁——同步调用 mqPartition.unsubscribe 反向取
 * MQPartition 锁，与 arrangeConsumer 方向构成 AB-BA 死锁，因此清理必须经任务池异步。
 * 本测试用"另一线程持 MQPartition 锁不放"确定性复现该场景：修复后 handlePushResult
 * 必须立即返回，清理在锁释放后由异步任务完成。
 * <p>
 * 注：文件放 src/MQ/ 但声明 package Zeze.MQ——需要 MQSingle 的包内测试缝
 * （注入 MQFileWithIndex 的构造器与 handlePushResult 回调体）。
 */
@Fast
public class TestMQSingleGhostUnsubscribe {

	/** 只假装"已发送"的 AsyncSocket 替身：rpc 上下文与超时按已发送建立，但不真正走网络。 */
	static final class FakeSocket extends AsyncSocket {
		FakeSocket(Service service) {
			super(service);
		}

		@Override
		public Type getType() {
			return Type.eClient;
		}

		@Override
		public boolean close(@Nullable Throwable ex, boolean gracefully) {
			return true;
		}

		@Override
		public boolean Send(Protocol<?> p) {
			return true;
		}

		@Override
		public boolean Send(byte[] bytes, int offset, int length) {
			return true;
		}

		@Override
		public @Nullable TimeThrottle getTimeThrottle() {
			return null;
		}

		@Override
		public @Nullable SocketAddress getRemoteAddress() {
			return null;
		}

		@Override
		public boolean isClosed() {
			return false;
		}
	}

	/** 记录 unsubscribe 调用的 MQPartition（manager 传 null：unsubscribe/arrangeConsumer 不触达）。 */
	static class RecordingPartition extends MQPartition {
		final List<Long> unsubscribed = new CopyOnWriteArrayList<>();

		RecordingPartition() {
			super(null);
		}

		@Override
		public void unsubscribe(AsyncSocket sender, long sessionId) {
			unsubscribed.add(sessionId);
			super.unsubscribe(sender, sessionId);
		}
	}

	private static void setPending(MQSingle single, PushMessage push) throws Exception {
		var f = MQSingle.class.getDeclaredField("pendingPushMessage");
		f.setAccessible(true);
		f.set(single, push);
	}

	@SuppressWarnings("unchecked")
	private static ConcurrentHashMap<Long, AsyncSocket> subscribesOf(MQPartition partition) throws Exception {
		var f = MQPartition.class.getDeclaredField("subscribes");
		f.setAccessible(true);
		return (ConcurrentHashMap<Long, AsyncSocket>)f.get(partition);
	}

	@Test
	public void testGhostUnsubscribeAsyncWithoutDeadlock(@TempDir Path tempDir) throws Exception {
		Task.tryInitThreadPool();
		var home = tempDir.resolve("db").toString();
		var database = new RocksDatabase(home);
		var file = new MQFileWithIndex(home, database, "topic", 0);
		try {
			var partition = new RecordingPartition();
			var single = new MQSingle(partition, "topic", 0, file);

			// 幽灵订阅残留：Manager 侧 subscribes 含 sessionId=100（消费端条目已删），
			// 分区绑在幽灵会话上（socket 用替身，unsubscribe 不消费 sender）。
			var ghostSocket = new FakeSocket(new Service("TestMQSingleGhostUnsubscribe"));
			partition.subscribe(ghostSocket, 100L);
			single.bind(100L, ghostSocket);
			Assertions.assertTrue(subscribesOf(partition).containsKey(100L));

			// 模拟"推送应答 eConsumerNotFound"：pending 记录的是发送时的会话标识
			// （tryPushMessage 路径由 Argument.setSessionId/getSender 写入，此处等价构造）。
			var push = new PushMessage();
			push.Argument.setTopic("topic");
			push.Argument.setSessionId(100L);
			push.setSender(ghostSocket);
			push.setResultCode(eConsumerNotFound);
			setPending(single, push);

			// 另一线程持 MQPartition 锁不放（模拟 arrangeConsumer 进行中，它随后还会经
			// bind() 进各 MQSingle 锁）：handlePushResult 若同步调用 unsubscribe 将与之互等。
			var partitionLockHeld = new CountDownLatch(1);
			var releasePartitionLock = new CountDownLatch(1);
			var lockHolder = new Thread(() -> {
				partition.lock();
				try {
					partitionLockHeld.countDown();
					releasePartitionLock.await();
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
				} finally {
					partition.unlock();
				}
			});
			lockHolder.setDaemon(true);
			lockHolder.start();
			Assertions.assertTrue(partitionLockHeld.await(10, TimeUnit.SECONDS));

			// ack 回调在独立线程执行：同步反序取 MQPartition 锁（旧代码/同步写法）时永远阻塞，
			// 修复后必须立即返回（清理转异步）。
			var callbackDone = new CountDownLatch(1);
			var callback = new Thread(() -> {
				single.handlePushResult();
				callbackDone.countDown();
			});
			callback.setDaemon(true);
			callback.start();
			Assertions.assertTrue(callbackDone.await(10, TimeUnit.SECONDS),
					"handlePushResult 必须立即返回：持MQSingle锁时同步取MQPartition锁与arrangeConsumer方向构成AB-BA死锁");

			// 释放 MQPartition 锁后，异步清理任务执行：幽灵订阅被移除并触发重排。
			releasePartitionLock.countDown();
			lockHolder.join(10_000);
			var deadline = System.currentTimeMillis() + 10_000;
			while (partition.unsubscribed.isEmpty() && System.currentTimeMillis() < deadline)
				Thread.sleep(10);
			Assertions.assertEquals(List.of(100L), partition.unsubscribed,
					"eConsumerNotFound 必须触发对该会话的unsubscribe（旧代码永不清理，分区无限期停摆+周期性空转rpc）");
			Assertions.assertFalse(subscribesOf(partition).containsKey(100L),
					"Manager侧幽灵订阅必须被移除（随后arrangeConsumer重排）");
		} finally {
			database.close();
			file.close();
		}
	}
}
