package Zeze.MQ;

import java.net.SocketAddress;
import Zeze.Builtin.MQ.BMessage;
import Zeze.Builtin.MQ.BSendMessage;
import Zeze.Builtin.MQ.PushMessage;
import Zeze.Net.AsyncSocket;
import Zeze.Net.Protocol;
import Zeze.Net.Service;
import Zeze.Util.TimeThrottle;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * FND19 MQ 单元测试共享工具（对齐 Dbh2/Fnd19GADStubSupport 先例）：消息构造、MQSingle 反射缝
 * 与不走网络的 AsyncSocket 替身。
 * <p>
 * 布局约束：本文件及各单元测试放 src/MQ/ 但声明 package Zeze.MQ——网络测试用 package MQ、
 * 单元测试用 package Zeze.MQ（MQSingle.handlePushResult、MQManager.getQueueForTest 等包内缝
 * 只对后者可见；package MQ 的测试只能反射访问内部字段）。
 */
public final class Fnd19MqTestSupport {
	private Fnd19MqTestSupport() {
	}

	/** BSendMessage 构造（timestamp=id）：驱动 MQSingle.sendMessage 的标准负载。 */
	public static BSendMessage.Data sendMessageOf(long id) {
		var message = new BMessage.Data();
		message.setTimestamp(id);
		var send = new BSendMessage.Data();
		send.setMessage(message);
		return send;
	}

	/** BMessage 构造（timestamp=id）：驱动 MQFileWithIndex.appendMessage 的标准负载。 */
	public static BMessage.Data messageOf(long id) {
		var message = new BMessage.Data();
		message.setTimestamp(id);
		return message;
	}

	/** 反射缝：直置 MQSingle.pendingPushMessage（直驱 handlePushResult 的前置）。 */
	public static void setPending(MQSingle single, PushMessage push) throws Exception {
		var f = MQSingle.class.getDeclaredField("pendingPushMessage");
		f.setAccessible(true);
		f.set(single, push);
	}

	/** 反射读 MQSingle 私有字段（headRetryCount/retryPending 等断言观察点）。 */
	public static Object getField(MQSingle single, String name) throws Exception {
		var f = MQSingle.class.getDeclaredField(name);
		f.setAccessible(true);
		return f.get(single);
	}

	/** 只假装"已发送"的 AsyncSocket 替身：不走网络，Send 总是成功。 */
	public static final class FakeSocket extends AsyncSocket {
		public FakeSocket(Service service) {
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
			return true;
		}

		@Override
		public boolean Send(byte @NotNull [] bytes, int offset, int length) {
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
}
