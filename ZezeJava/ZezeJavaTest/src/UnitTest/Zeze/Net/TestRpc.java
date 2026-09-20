package UnitTest.Zeze.Net;

import harness.Fast;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import Zeze.Net.AsyncSocket;
import Zeze.Net.Protocol;
import Zeze.Net.Rpc;
import Zeze.Net.Service;
import Zeze.Transaction.Procedure;
import Zeze.Util.Factory;
import demo.Module1.BValue;
import org.junit.jupiter.api.Assertions;

@Fast
public class TestRpc {
	final Zeze.Util.TaskCompletionSource<AsyncSocket> connected = new Zeze.Util.TaskCompletionSource<>();

	@Test
	public final void testRpcSimple() throws Exception {
		Service server = new Service("TestRpc.Server");
		Zeze.Util.Task.tryInitThreadPool();
		FirstRpc first = new FirstRpc();
		Factory<Protocol<?>> f = FirstRpc::new;
		System.out.println(first.getTypeId());
		server.AddFactoryHandle(first.getTypeId(), new Service.ProtocolFactoryHandle<>(f, TestRpc::ProcessFirstRpcRequest));

		// R2-U2：端口0让OS分配临时端口（同仓TestSocketAcceptCloseOnce等模式）——固定5000在
		// 多工作树/CI并行跑测试时互撞bind失败。
		var listener = (Zeze.Net.TcpSocket)server.newServerSocket("127.0.0.1", 0, null);
		Client client = null;
		try {
			var local = listener.getLocalInet();
			Assertions.assertNotNull(local, "listen socket local address");
			int port = local.getPort();

			client = new Client(this);
			client.AddFactoryHandle(first.getTypeId(), new Service.ProtocolFactoryHandle<>(FirstRpc::new));

			AsyncSocket clientSocket = client.newClientSocket("127.0.0.1", port, null, null);
			// 原先无参get()无限park（2026-09-20审核）：连接建立失败时挂死worker而非判失败
			Assertions.assertNotNull(connected.get(10, java.util.concurrent.TimeUnit.SECONDS), "10s内连接必须建立");

			first = new FirstRpc();
			first.Argument.setInt_1(1234);
			//Console.WriteLine("SendFirstRpcRequest");
			first.SendForWait(clientSocket, 10_000).await();
			//Console.WriteLine("FirstRpc Wait End");
			Assertions.assertEquals(first.Argument.getInt_1(), first.Result.getInt_1());
		} finally {
			// 原先成功路径也不清理（2026-09-20审核）：selector/监听端口泄漏至JVM退出
			if (client != null)
				client.stop();
			server.stop();
		}
	}

	public static long ProcessFirstRpcRequest(Protocol<?> p) {
		FirstRpc rpc = (FirstRpc)p;
		rpc.Result.assign(rpc.Argument);
		rpc.SendResult();
		System.out.println("ProcessFirstRpcRequest result.Int1=" + rpc.Result.getInt_1());
		return Procedure.Success;
	}

	public static class FirstRpc extends Rpc<BValue, BValue> {
		public FirstRpc() {
			Argument = new BValue();
			Result = new BValue();
		}

		@Override
		public int getModuleId() {
			return 1;
		}

		@Override
		public int getProtocolId() {
			return -1;
		}
	}

	public static class Client extends Service {
		private final TestRpc test;

		public Client(TestRpc test) {
			super("TestRpc.Client");
			this.test = test;
		}

		@Override
		public void OnSocketConnected(@NotNull AsyncSocket so) throws Exception {
			super.OnSocketConnected(so);
			test.connected.setResult(so);
		}
	}
}
