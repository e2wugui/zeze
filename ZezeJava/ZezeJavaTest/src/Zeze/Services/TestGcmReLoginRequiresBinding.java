package Zeze.Services;

import java.net.InetAddress;
import java.util.Objects;
import Zeze.Config;
import Zeze.Net.Connector;
import Zeze.Net.Service;
import Zeze.Net.TcpSocket;
import Zeze.Services.GlobalCacheManager.Login;
import Zeze.Services.GlobalCacheManager.ReLogin;
import Zeze.Transaction.DispatchMode;
import Zeze.Transaction.TransactionLevel;
import Zeze.Util.Task;
import harness.Fast;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.parallel.ResourceLock;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Fast
@Timeout(15)
@ResourceLock("GlobalCacheManagerServer.instance")
public class TestGcmReLoginRequiresBinding {
	@Test
	public void synchronousServerRequiresPreviousBinding() throws Exception {
		Task.tryInitThreadPool();
		var server = GlobalCacheManagerServer.getInstance();
		try {
			server.start(InetAddress.getLoopbackAddress(), 0, new Config());
			checkBinding(server);
		} finally {
			server.stop();
		}
	}

	@Test
	public void asynchronousServerRequiresPreviousBinding() throws Exception {
		Task.tryInitThreadPool();
		var server = new GlobalCacheManagerAsyncServer();
		try {
			server.start(InetAddress.getLoopbackAddress(), 0, new Config());
			checkBinding(server);
		} finally {
			server.stop();
		}
	}

	private static void checkBinding(Object server) throws Exception {
		var listener = server.getClass().getDeclaredField("serverSocket");
		listener.setAccessible(true);
		var address = Objects.requireNonNull(((TcpSocket)listener.get(server)).getLocalInet());
		var client = new Service("GcmReLoginRequiresBinding", new Config());
		client.AddFactoryHandle(Login.TypeId_, new Service.ProtocolFactoryHandle<>(
				Login::new, null, TransactionLevel.None, DispatchMode.Direct));
		client.AddFactoryHandle(ReLogin.TypeId_, new Service.ProtocolFactoryHandle<>(
				ReLogin::new, null, TransactionLevel.None, DispatchMode.Direct));
		var first = new Connector(address.getHostString(), address.getPort(), false);
		var second = new Connector(address.getHostString(), address.getPort(), false);
		first.SetService(client);
		second.SetService(client);
		try {
			first.start();
			var firstSocket = first.WaitReady();
			var login = new Login();
			login.Argument.serverId = 1;
			assertTrue(login.SendForWait(firstSocket, 3000).await(3000));
			assertEquals(0, login.getResultCode());

			var rejected = new Login();
			rejected.Argument.serverId = 2;
			assertTrue(rejected.SendForWait(firstSocket, 3000).await(3000));
			assertEquals(GlobalCacheManagerConst.LoginBindSocketFail, rejected.getResultCode());

			second.start();
			var reLogin = new ReLogin();
			reLogin.Argument.serverId = 2;
			assertTrue(reLogin.SendForWait(second.WaitReady(), 3000).await(3000));
			assertEquals(GlobalCacheManagerConst.ReLoginBindSocketFail, reLogin.getResultCode());
		} finally {
			try {
				first.stop();
			} finally {
				try {
					second.stop();
				} finally {
					client.stop();
				}
			}
		}
	}
}
