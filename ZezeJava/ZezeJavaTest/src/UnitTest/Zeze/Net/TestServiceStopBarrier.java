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
 * XA1-F1/N2-F3回归：stop()关闭循环与迟到连接登记竞态。stop()最前置位stopped屏障，
 * addSocket在putIfAbsent成功后复查，命中即自查自关——停机后迟到的accept/connect登记
 * 不再泄漏活连接（继续被派发/被统计）。start()复位屏障，支持stop→start重启。
 */
@Fast
public class TestServiceStopBarrier {

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
	public void testStoppedServiceRejectsLateAddSocket() throws Exception {
		Task.tryInitThreadPool();
		var service = new BridgeService("test.stopbarrier");
		try {
			service.stop(); // 未start也可stop（置屏障+清扫空表）

			// 停机后的迟到登记（模拟accept竞态：连接本身open，但服务已停）：必须被拒绝并关闭
			var late = (TcpSocket)service.newClientSocket("127.0.0.1", 1, null, null);
			Assertions.assertFalse(late.isClosed(), "前提：迟到连接自身尚未关闭");
			Assertions.assertFalse(service.addSocketForTest(late), "已停服务不得登记迟到连接");
			Assertions.assertTrue(late.isClosed(), "被拒的迟到连接必须自查自关（serviceStopped）");
			Assertions.assertEquals(0, service.getSocketCount());
			Assertions.assertNull(service.GetSocket(late.getSessionId()));
		} finally {
			service.stop();
		}
	}

	@Test
	public void testStartResetsBarrier() throws Exception {
		Task.tryInitThreadPool();
		var service = new BridgeService("test.stopbarrier.reset");
		try {
			service.stop(); // 屏障置位
			var late = (TcpSocket)service.newClientSocket("127.0.0.1", 1, null, null);
			Assertions.assertFalse(service.addSocketForTest(late));
			Assertions.assertTrue(late.isClosed());

			// start复位屏障：之后的登记恢复正常（对照，证明拒绝确因屏障而非残留状态）
			service.start();
			var open = (TcpSocket)service.newClientSocket("127.0.0.1", 1, null, null);
			Assertions.assertTrue(service.addSocketForTest(open), "重启后登记必须恢复");
			Assertions.assertFalse(open.isClosed());
			open.close(new IOException("normal close"));
			Assertions.assertEquals(0, service.getSocketCount());
		} finally {
			service.stop();
		}
	}
}
