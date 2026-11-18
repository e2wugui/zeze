package Zeze.Component;

import java.net.SocketAddress;
import java.nio.file.Path;
import Zeze.Builtin.RedoQueue.BTaskId;
import Zeze.Builtin.RedoQueue.RunTask;
import Zeze.Config;
import Zeze.Net.AsyncSocket;
import Zeze.Net.Rpc;
import Zeze.Transaction.Procedure;
import Zeze.Util.Task;
import Zeze.Util.TimeThrottle;
import harness.Fast;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

@Fast
public class TestRedoQueueRestartPending {
	@TempDir
	Path directory;

	@Test
	void ignoresThePreviousRunResponseAfterRestart() throws Exception {
		var config = new Config();
		config.setServiceManager("disable");
		var queue = new RedoQueue(directory.resolve("queue").toString(), config);
		var pending = RedoQueue.class.getDeclaredField("pending");
		pending.setAccessible(true);
		var onResponse = RedoQueue.class.getDeclaredMethod("processRunTaskResult", Rpc.class);
		onResponse.setAccessible(true);
		var previousRun = new RunTask();
		previousRun.Argument.setTaskId(1);
		previousRun.Result.setTaskId(1);

		try {
			queue.start();
			queue.add(1, new BTaskId());
			pending.set(queue, previousRun); // Simulate Send succeeding before the reply is dispatched.
			queue.stop();
			queue.start();

			assertNull(pending.get(queue));
			assertEquals(Procedure.LogicError, onResponse.invoke(queue, previousRun));
		} finally {
			queue.stop();
		}
	}

	@Test
	void lateHandshakeDoesNotBlockSendingOnTheNewConnection() throws Exception {
		Task.tryInitThreadPool();
		var queue = new RedoQueue(directory.resolve("queue").toString(), new Config());
		var oldSocket = new TestSocket(queue);
		var newSocket = new TestSocket(queue);
		try {
			queue.start();
			queue.add(1, new BTaskId());
			oldSocket.close();
			queue.OnSocketClose(oldSocket, null);
			queue.stop();
			queue.start();

			// 握手已派发，队列回调在旧连接关闭且队列重启后才继续。
			queue.OnHandshakeDone(oldSocket);
			queue.OnHandshakeDone(newSocket);

			assertEquals(1, newSocket.sendAttempts, "待发任务应由新连接发送");
			assertEquals(0, oldSocket.sendAttempts, "已关闭的连接不能重新参与发送");
		} finally {
			queue.stop();
		}
	}

	private static final class TestSocket extends AsyncSocket {
		private int sendAttempts;

		TestSocket(RedoQueue queue) {
			super(queue);
		}

		@Override
		public Type getType() {
			return Type.eClient;
		}

		@Override
		public boolean Send(byte[] bytes, int offset, int length) {
			++sendAttempts;
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
		protected void doClose(@Nullable Throwable ex, boolean gracefully) {
		}
	}
}
