package UnitTest.Zeze.Net;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicInteger;

import Zeze.Net.AsyncSocket;
import Zeze.Net.Selectors;
import Zeze.Net.Service;
import Zeze.Net.TcpSocket;
import Zeze.Util.Task;
import harness.Fast;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * N3-F5回归：TcpSocket.processReceive的EOF分支未清除readAgain——上一轮满读（readAgain=true）
 * 后紧跟EOF时，非关闭式OnSocketInputClosed覆写（javadoc明文允许）下内层do-while恒真，
 * selector线程忙循环挂死，覆写方"继续发送数据直到主动关闭"的合法用法饿死。
 * 修复：EOF分支补readAgain=false。非关闭式覆写在EOF后发送告别消息，客户端（半关闭）必须收到。
 * 注：满读+EOF同轮到达受TCP投递时序影响（概率性红），多连接迭代提高命中；修复后恒绿。
 */
@Fast
public class TestTcpSocketEofNoBusyLoop {

	// EOF不关闭、暂停接收并发送告别消息的合法覆写（Service.OnSocketInputClosed javadoc允许的形态）
	static final class FarewellOnEofService extends Service {
		final AtomicInteger eofCount = new AtomicInteger();

		FarewellOnEofService(String name) {
			super(name);
		}

		@Override
		public void OnSocketInputClosed(@NotNull AsyncSocket so) throws Exception {
			eofCount.incrementAndGet();
			if (so instanceof TcpSocket tcp)
				tcp.pauseReceive();
			so.Send("bye".getBytes(StandardCharsets.ISO_8859_1), 0, 3);
		}

		@Override
		public boolean OnSocketProcessInputBuffer(@NotNull AsyncSocket so, @NotNull Zeze.Serialize.ByteBuffer input)
				throws Exception {
			input.ReadIndex = input.WriteIndex; // 静默吞掉载荷（本测试只关心EOF语义，不参与协议解码）
			return true;
		}
	}

	@Test
	public void testServerCanSendAfterEofWithNonClosingOverride() throws Exception {
		Task.tryInitThreadPool();
		// 小读缓冲（1024）：客户端发1KB+FIN，满读后紧邻EOF的窗口最大化
		var selectorsConfig = new Selectors.Config();
		selectorsConfig.readBufferSize = 1024;
		var selectors = new Selectors("wt4-eof", 1, selectorsConfig);
		var service = new FarewellOnEofService("test.eof.nobusyloop");
		service.setSelectors(selectors);
		try {
			var listener = (TcpSocket)service.newServerSocket("127.0.0.1", 0, null);
			var local = listener.getLocalInet();
			Assertions.assertNotNull(local);
			int port = local.getPort();

			for (int round = 0; round < 5; round++) {
				try (var sock = new Socket("127.0.0.1", port)) {
					sock.setSoTimeout(10_000);
					var os = sock.getOutputStream();
					os.write(new byte[1024]); // 恰满一个读缓冲：readAgain=true
					os.flush();
					sock.shutdownOutput(); // 紧邻EOF

					InputStream is = sock.getInputStream();
					var buf = new byte[64];
					int n = is.read(buf); // 修复前selector线程忙循环，告别消息永不到达（超时红）
					Assertions.assertEquals(3, n, "EOF后服务端必须仍能发送（round=" + round + "）: n=" + n);
					Assertions.assertEquals("bye", new String(buf, 0, n, StandardCharsets.ISO_8859_1));
				}
			}
			Assertions.assertTrue(service.eofCount.get() >= 5, "每条连接必须观察到EOF");
		} finally {
			service.stop();
			selectors.close();
		}
	}
}
