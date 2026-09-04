package Zeze.MQ;

import java.net.SocketAddress;
import java.nio.file.Path;
import Zeze.Builtin.MQ.BMessage;
import Zeze.Builtin.MQ.BSendMessage;
import Zeze.Builtin.MQ.PushMessage;
import Zeze.Config;
import Zeze.Net.AsyncSocket;
import Zeze.Net.Protocol;
import Zeze.Net.Rpc;
import Zeze.Net.Service;
import Zeze.Util.RocksDatabase;
import Zeze.Util.Task;
import Zeze.Util.TimeThrottle;
import harness.Fast;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * FND2-G2-5 回归：MQConfig.RpcTimeout 此前从不作用于 MQ 数据面——tryPushMessage 推送
 * 不传超时参数，固定按 Rpc 字段默认 5000ms；配置只流入 Raft ProxyServer 的代理路径
 * （MQManager 从不 addRaft，不可达）。修复：Send 显式传
 * {@code mqPartition.getManager().getMqConfig().getRpcTimeout()}。
 * <p>
 * 用假 socket（只假装"已发送"，rpc 上下文与超时按已发送建立）驱动 tryPushMessage 的
 * 真实路径，断言 pending rpc 的 timeout 字段等于 MQConfig 配置值（修复前恒为 5000，
 * 无论 MQConfig 配置多少）。MQManager 仅构造不 start（MQSingle 的持有链
 * partition→manager→mqConfig 在构造期即完整，推送时才读取）。
 * <p>
 * 注：文件放 src/MQ/ 但声明 package Zeze.MQ（与 TestMQSingle* 先例一致）；
 * timeout 值取 65432，区别于 MQConfig 默认 20000 与 Rpc 默认 5000，防碰巧相等。
 */
@Fast
public class TestMQSinglePushRpcTimeout {

	/** 只假装"已发送"的 AsyncSocket 替身：不走网络，Send 总是成功。 */
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

	private static BSendMessage.Data sendMessageOf(long id) {
		var message = new BMessage.Data();
		message.setTimestamp(id);
		var send = new BSendMessage.Data();
		send.setMessage(message);
		return send;
	}

	@Test
	public void testPushUsesConfiguredRpcTimeout(@TempDir Path tempDir) throws Exception {
		Task.tryInitThreadPool();
		// 仅构造不 start：不占端口、不连 Master（startAndWaitConnectionReady 会阻塞等 Master）。
		var manager = new MQManager(tempDir.resolve("manager").toString(), new Config());
		try {
			manager.getMqConfig().setRpcTimeout(65432);
			var partition = new MQPartition(manager);
			var single = new MQSingle(partition, "topic", 0);
			try {
				var socket = new FakeSocket(new Service("TestMQSinglePushRpcTimeout"));
				single.bind(77L, socket);
				// 空分区首条消息：直入内存队列 → tryPushMessage → Send(socket, cb, rpcTimeout)。
				single.sendMessage(sendMessageOf(1));

				var pendingField = MQSingle.class.getDeclaredField("pendingPushMessage");
				pendingField.setAccessible(true);
				var pending = (PushMessage)pendingField.get(single);
				Assertions.assertNotNull(pending, "推送应已发出（假socket总成功）");

				// Rpc.Send(so, handle, millisecondsTimeout) 会把超时写入 rpc 实例的 timeout 字段。
				var timeoutField = Rpc.class.getDeclaredField("timeout");
				timeoutField.setAccessible(true);
				Assertions.assertEquals(65432, timeoutField.getInt(pending),
						"推送超时必须取 MQConfig.RpcTimeout（修复前恒为 Rpc 默认 5000，配置不生效）");
			} finally {
				single.close(); // 关闭 MQFileWithIndex 的文件流
			}
		} finally {
			// MQManager 未 start，不能走 stop()：反射关闭其 rocksdb，保证临时目录可清理。
			var rocksField = MQManager.class.getDeclaredField("rocksDatabase");
			rocksField.setAccessible(true);
			((RocksDatabase)rocksField.get(manager)).close();
		}
	}
}
