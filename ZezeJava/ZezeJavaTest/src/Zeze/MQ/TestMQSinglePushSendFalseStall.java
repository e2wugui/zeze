package Zeze.MQ;

import java.net.SocketAddress;
import java.nio.file.Path;
import java.util.Queue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import Zeze.Builtin.MQ.BMessage;
import Zeze.Builtin.MQ.BSendMessage;
import Zeze.Config;
import Zeze.Net.AsyncSocket;
import Zeze.Net.Protocol;
import Zeze.Net.Service;
import Zeze.Util.Task;
import Zeze.Util.TimeThrottle;
import harness.Fast;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * FND30 mq-01 回归：PushMessage 因消费者 socket 输出缓冲溢出被网络层静默丢弃
 * （TcpSocket.Send 拒写但不关连接，Rpc.Send 返回 false 且应答/超时回调永不触发）时，
 * tryPushMessage 的 false 分支此前只清 pendingPushMessage 不排任何重试——分区投递
 * 失去全部驱动事件源（无在飞 rpc、无退避排期、订阅未变、盘上无积压时无后台回填），
 * 安静 topic 永久停摆，退避与 PushRetryMax 死信兜底整体被绕过。
 * <p>
 * 修复：false 分支统一并入 onPushFailure 失败处置（计数+指数退避，达上限按配置
 * 转死信/丢弃推进队头），对齐超时失败路径。
 * <p>
 * 用恒 false 的假 socket 驱动真实 tryPushMessage 路径（Rpc 上下文按真实语义建立又
 * 在 Send 尾部被双参 remove 回收——与溢出丢弃形态逐点同构），退避调度器注入同步
 * 执行（既有测试缝，见 MQSingle.retryScheduler 注释），确定性走完
 * 失败→退避→重投→…→PushRetryMax 上限，断言消息按策略出队、退避确有排期。
 */
@Fast
public class TestMQSinglePushSendFalseStall {

	/** 恒拒绝发送的 AsyncSocket 替身：模拟输出缓冲溢出丢弃（连接健康但拒写）。 */
	static final class OverflowDropSocket extends AsyncSocket {
		OverflowDropSocket(Service service) {
			super(service);
		}

		@Override
		public Type getType() {
			return Type.eClient;
		}

		@Override
		protected void doClose(@Nullable Throwable ex, boolean gracefully) {
		}

		@Override
		public boolean Send(@NotNull Protocol<?> p) {
			return false; // 溢出丢弃形态：拒写、不关连接、无任何回调
		}

		@Override
		public boolean Send(byte @NotNull [] bytes, int offset, int length) {
			return false;
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
			return false; // 连接保持健康是本形态区别于"连接失效"的关键
		}
	}

	private static BSendMessage.Data sendMessageOf(long id) {
		var message = new BMessage.Data();
		message.setTimestamp(id);
		var send = new BSendMessage.Data();
		send.setMessage(message);
		return send;
	}

	@Test
	public void testSendFalseEntersFailureHandling(@TempDir Path tempDir) throws Exception {
		Task.tryInitThreadPool();
		// 仅构造不 start：不占端口、不连 Master（与 TestMQSinglePushRpcTimeout 同形态）。
		var manager = new MQManager(tempDir.resolve("manager").toString(), new Config());
		try {
			var config = manager.getMqConfig();
			config.setRpcTimeout(60000); // 排除超时回调路径干扰（本形态回调永不触发）
			config.setPushRetryMax(3);
			config.setPushRetryBackoffBaseMs(1);
			config.setPushRetryBackoffCapMs(1);
			// 死信档走丢弃：终态断言只关心队头推进，不引入 DLQ 存储细节。
			config.setPushDeadLetterPolicy("discard");
			var partition = new MQPartition(manager);
			var single = new MQSingle(partition, "topic", 0);
			try {
				var scheduled = new AtomicInteger();
				// 同步执行的退避调度器：失败→退避→重投在同一线程内确定性走完（既有测试缝）。
				single.retryScheduler = (delayMs, action) -> {
					scheduled.incrementAndGet();
					action.run();
					return CompletableFuture.completedFuture(null);
				};
				var socket = new OverflowDropSocket(new Service("TestMQSinglePushSendFalseStall"));
				single.bind(77L, socket);
				// 空分区首条消息：直入内存队列 → tryPushMessage → Send false（溢出丢弃形态）。
				single.sendMessage(sendMessageOf(1));

				var queueField = MQSingle.class.getDeclaredField("messageQueue");
				queueField.setAccessible(true);
				var queue = (Queue<?>)queueField.get(single);
				Assertions.assertTrue(queue.isEmpty(),
						"Send-false 必须并入失败处置：持续失败达 PushRetryMax 后按策略出队，不得停摆");
				Assertions.assertTrue(scheduled.get() >= 1,
						"Send-false 必须排退避重推（修复前只清 pending，分区失去全部事件源）");
			} finally {
				single.close();
			}
		} finally {
			manager.stop();
		}
	}
}
