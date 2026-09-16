package UnitTest.Zeze.Services;

import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;
import Zeze.Config;
import Zeze.Net.AsyncSocket;
import Zeze.Net.TcpSocket;
import Zeze.Util.CommandConsoleService;
import Zeze.Util.Task;
import harness.Fast;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * R2-U2回归（backlog U2②）：CommandConsoleService.OnSocketProcessInputBuffer在
 * cc==null（连接accept时setCommandConsole尚未调用，或被显式置null）时无条件
 * 整块消费输入并静默丢弃——客户端敲任何命令都无响应也无断开，不可观测。
 * 修复：cc==null时抛IllegalStateException拒收，连接关闭并留痕，客户端重连
 * 即得已就绪的控制台。
 * 修复前红：服务端吞输入不断开，客户端read阻塞至SoTimeout抛SocketTimeoutException；
 * 修复后绿：服务端立即断开，客户端read得EOF，且关闭原因是明确的服务端异常。
 */
@Fast
public class TestCommandConsoleNotReadyClose {

	/** 捕获OnSocketClose原因的CommandConsoleService。 */
	private static final class CloseReasonService extends CommandConsoleService {
		final AtomicReference<Throwable> closeReason = new AtomicReference<>();

		CloseReasonService(String name, Config config) {
			super(name, config);
		}

		@Override
		public void OnSocketClose(@NotNull AsyncSocket so, @Nullable Throwable e) throws Exception {
			closeReason.compareAndSet(null, e);
			super.OnSocketClose(so, e);
		}
	}

	@Test
	public void testInputRejectedWhenConsoleNotSet() throws Exception {
		Task.tryInitThreadPool();
		var server = new CloseReasonService("TestCcNotReady", new Config()); // 故意不 setCommandConsole
		try {
			var listener = (TcpSocket)server.newServerSocket(new InetSocketAddress("127.0.0.1", 0), null);
			var local = listener.getLocalInet();
			Assertions.assertNotNull(local);
			int port = local.getPort();

			try (var raw = new Socket("127.0.0.1", port)) {
				raw.setSoTimeout(3_000);
				raw.getOutputStream().write("whatever\n".getBytes(StandardCharsets.UTF_8));
				raw.getOutputStream().flush();
				// 修复前：输入被静默吞掉，read阻塞到SoTimeout抛SocketTimeoutException（红）
				Assertions.assertEquals(-1, raw.getInputStream().read(),
						"未就绪控制台必须拒收并断开，而不是静默吞输入");
			}
			// 关闭原因必须是明确的服务端异常（IllegalStateException），不是静默/协议错误
			var deadline = System.currentTimeMillis() + 10_000;
			while (server.closeReason.get() == null) {
				if (System.currentTimeMillis() > deadline)
					Assertions.fail("timeout waiting for server OnSocketClose");
				//noinspection BusyWait
				Thread.sleep(1);
			}
			Assertions.assertInstanceOf(IllegalStateException.class, server.closeReason.get(),
					() -> "关闭原因应为IllegalStateException，实际: " + server.closeReason.get());
		} finally {
			server.stop();
		}
	}
}
