package UnitTest.Zeze.Net;

import java.io.IOException;
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
		try {
			// closed条目：登记被拒，按id与计数均不可见
			var closed = (TcpSocket)service.newClientSocket("127.0.0.1", 1, null, null);
			closed.close(new IOException("simulate straddled close"));
			Assertions.assertTrue(closed.isClosed());
			Assertions.assertFalse(service.addSocketForTest(closed), "closed socket不得登记入表");
			Assertions.assertNull(service.GetSocket(closed.getSessionId()), "表中不得存在closed条目");
			Assertions.assertEquals(0, service.getSocketCount());

			// 开放条目对照：入表成功，移除仍由close链的remove负责（锁不越权）
			var open = (TcpSocket)service.newClientSocket("127.0.0.1", 1, null, null);
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
