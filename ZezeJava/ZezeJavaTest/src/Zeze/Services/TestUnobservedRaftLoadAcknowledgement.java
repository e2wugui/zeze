package Zeze.Services;

import java.net.ServerSocket;
import Zeze.Builtin.ServiceManagerWithRaft.SetServerLoad;
import Zeze.Net.AsyncSocket;
import Zeze.Services.ServiceManager.BServerLoad;
import harness.Fast;
import harness.ForkedJvm;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import static org.junit.jupiter.api.Assertions.*;

@Fast
@ForkedJvm
public class TestUnobservedRaftLoadAcknowledgement {
	@Test
	@Timeout(240)
	void loadBeforeAnySubscriptionReceivesSuccess() throws Exception {
		// 复用既有三节点网络夹具；独立子 JVM 内替换端口，避免与原测试并行撞口。
		var fixture = TestServiceManagerWithRaftCommitThenResponse.class;
		var portsField = fixture.getDeclaredField("ports");
		portsField.setAccessible(true);
		var ports = (int[])portsField.get(null);
		try (var a = new ServerSocket(0); var b = new ServerSocket(0); var c = new ServerSocket(0)) {
			ports[0] = a.getLocalPort();
			ports[1] = b.getLocalPort();
			ports[2] = c.getLocalPort();
		}
		try {
			TestServiceManagerWithRaftCommitThenResponse.setUp();
			var peerClass = Class.forName(fixture.getName() + "$Peer");
			var constructor = peerClass.getDeclaredConstructor(String.class);
			constructor.setAccessible(true);
			var peer = (HandshakeClient)constructor.newInstance("UnobservedLoadPeer");
			try {
				var leaderPort = fixture.getDeclaredMethod("leaderPort");
				leaderPort.setAccessible(true);
				var connect = peerClass.getDeclaredMethod("connect", int.class);
				connect.setAccessible(true);
				var socket = (AsyncSocket)connect.invoke(peer, leaderPort.invoke(null));
				var login = fixture.getDeclaredMethod("sendLogin", peerClass, AsyncSocket.class, String.class, int.class);
				login.setAccessible(true);
				socket = (AsyncSocket)login.invoke(null, peer, socket, "UnobservedLoadSession", 30_000);
				var load = new BServerLoad();
				load.ip = "127.0.0.1";
				load.port = 12345;
				var request = new SetServerLoad(load);
				request.getUnique().setRequestId(1_000_000);
				request.setCreateTime(System.currentTimeMillis());
				assertTrue(request.SendForWait(socket, 10_000).await(15_000), "无观察者的上报也必须完成");
				assertFalse(request.isTimeout());
				assertEquals(0, request.getResultCode());
			} finally {
				peer.stop();
			}
		} finally {
			TestServiceManagerWithRaftCommitThenResponse.tearDown();
		}
	}
}
