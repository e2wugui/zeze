package UnitTest.Zeze.Net;

import Zeze.Net.AsyncSocket;
import Zeze.Net.Protocol;
import Zeze.Net.Rpc;
import Zeze.Net.Service;
import Zeze.Transaction.Procedure;
import Zeze.Util.Factory;
import demo.Module1.BValue;
import junit.framework.TestCase;
import org.junit.Assert;

public class TestRpc extends TestCase {
	final Zeze.Util.TaskCompletionSource<AsyncSocket> connected = new Zeze.Util.TaskCompletionSource<>();

	public final void testRpcSimple() throws Exception {
		Service server = new Service("TestRpc.Server");
		Zeze.Util.Task.tryInitThreadPool();
		FirstRpc first = new FirstRpc();
		Factory<Protocol<?>> f = FirstRpc::new;
		System.out.println(first.getTypeId());
		server.AddFactoryHandle(first.getTypeId(), new Service.ProtocolFactoryHandle<>(f, TestRpc::ProcessFirstRpcRequest));

		server.newServerSocket("127.0.0.1", 5000, null);
		Client client = new Client(this);
		client.AddFactoryHandle(first.getTypeId(), new Service.ProtocolFactoryHandle<>(FirstRpc::new));

		AsyncSocket clientSocket = client.newClientSocket("127.0.0.1", 5000, null, null);
		connected.get();

		first = new FirstRpc();
		first.Argument.setInt_1(1234);
		//Console.WriteLine("SendFirstRpcRequest");
		first.SendForWait(clientSocket).await();
		//Console.WriteLine("FirstRpc Wait End");
		Assert.assertEquals(first.Argument.getInt_1(), first.Result.getInt_1());
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
		public void OnSocketConnected(AsyncSocket so) throws Exception {
			super.OnSocketConnected(so);
			test.connected.setResult(so);
		}
	}
}
