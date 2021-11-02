package UnitTest.Zeze.Net;


import Zeze.Net.*;
import Zeze.Transaction.*;
import Zeze.Util.Factory;
import Zeze.Util.ManualResetEvent;
import junit.framework.TestCase;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

public class TestRpc extends TestCase{
	
	 ManualResetEvent connected = new ManualResetEvent(false);
	
	public final void testRpcSimple() throws InterruptedException {
		Service server = new Service("TestRpc.Server");
		Zeze.Util.Task.tryInitThreadPool(null, null, null);
		FirstRpc forid = new FirstRpc();
		Factory<Protocol> f =  () -> new FirstRpc();
		server.AddFactoryHandle(forid.getTypeId(), new Service.ProtocolFactoryHandle(f,x-> ProcessFirstRpcRequest(x)));

		AsyncSocket servetrSocket = server.NewServerSocket("127.0.0.1", 5000);
		Client client = new Client(this);
		client.AddFactoryHandle(forid.getTypeId(), new Service.ProtocolFactoryHandle(() -> new FirstRpc()));

		AsyncSocket clientSocket = client.NewClientSocket("127.0.0.1", 5000, null);
		connected.WaitOne();

		FirstRpc first = new FirstRpc();
		first.Argument.setInt1(1234);
		//Console.WriteLine("SendFirstRpcRequest");
		first.SendForWait(clientSocket).Wait();
		//Console.WriteLine("FirstRpc Wait End");
		assert first.Argument.getInt1() == first.Result.getInt1();
	}

	

	public final int ProcessFirstRpcRequest(Protocol p) {
		FirstRpc rpc = (FirstRpc) p;
		rpc.Result.Assign(rpc.Argument);
		rpc.SendResult();
		System.out.println("ProcessFirstRpcRequest result.Int1=" + rpc.Result.getInt1());
		return Procedure.Success;
	}

	public static class FirstRpc extends Rpc<demo.Module1.Value, demo.Module1.Value> {
		public FirstRpc() {
			Argument = new demo.Module1.Value();
			Result = new demo.Module1.Value();
		}
		
		@Override
		public int getModuleId() {
			return 1;
		}

		@Override
		public int getProtocolId() {
			return 1;
		}
	}
	public static class Client extends Service {
		private TestRpc test;
		public Client(TestRpc test) {
			super("TestRpc.Client");
			this.test = test;
		}
		@Override
		public void OnSocketConnected(AsyncSocket so) {
			super.OnSocketConnected(so);
			test.connected.Set();
		}
	}
}