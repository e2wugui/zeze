package Zeze.Net;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import Zeze.Net.AsyncSocket;
import Zeze.Net.Service;
import Zeze.Net.TcpSocket;
import Zeze.Util.Task;
import harness.Fast;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * 回归（僵尸socketMap条目）：连接成功回调跨骑完整close链时，迟到登记的OnSocketClose
 * "恰好一次"已消费、入表后无人核销。修复：置死与登记在AsyncSocket互斥，closed登记被拒
 * （三个入口TcpSocket连接成功/WebsocketClient.onOpen/accept同享）。
 */
@Fast
public class TestServiceAddSocketClosedRejected {

	/** addSocket为protected：测试桥。 */
	private static final class BridgeService extends Service {
		BridgeService(String name) {
			super(name);
		}

		boolean addSocketForTest(AsyncSocket so) {
			return addSocket(so);
		}
	}

	@Test
	public void testClosedSocketRejected() throws Exception {
		Task.tryInitThreadPool();
		var service = new BridgeService("test.addsocket.closed");
		// 开放条目对照必须连真监听：连拒绝端口(如127.0.0.1:1)时OS毫秒级回Connection refused，
		// selector的拒连close与测试体赛跑——快则addSocket按设计拒掉已死socket(:46假红)，
		// 慢则markClosed被selector先赢、测试的close成no-op，断言插在selector的
		// markClosed→OnSocketClose.remove窗口里(:49假红)。2026-10-09批r8/r20两形态。
		// 内核backlog完成TCP握手，client端保持ESTABLISHED直到测试主动close，竞态根除。
		try (var listener = new ServerSocket(0, 1, InetAddress.getLoopbackAddress())) {
			// closed条目：登记被拒，按id与计数均不可见
			var closed = (TcpSocket)service.newClientSocket("127.0.0.1", 1, null, null);
			closed.close(new IOException("simulate straddled close"));
			Assertions.assertTrue(closed.isClosed());
			Assertions.assertFalse(service.addSocketForTest(closed), "closed socket不得登记入表");
			Assertions.assertNull(service.GetSocket(closed.getSessionId()), "表中不得存在closed条目");
			Assertions.assertEquals(0, service.getSocketCount());

			// 开放条目对照：入表成功，移除仍由close链的remove负责（锁不越权）
			var open = (TcpSocket)service.newClientSocket("127.0.0.1", listener.getLocalPort(), null, null);
			Assertions.assertTrue(service.addSocketForTest(open));
			Assertions.assertEquals(1, service.getSocketCount());
			open.close(new IOException("normal close"));
			Assertions.assertNull(service.GetSocket(open.getSessionId()), "正常移除仍由close链负责");
			Assertions.assertEquals(0, service.getSocketCount());
		} finally {
			service.stop();
		}
	}
}
