package UnitTest.Zeze.Net;

import Zeze.Net.*;
import Zeze.Transaction.*;
import UnitTest.*;

//C# TO JAVA CONVERTER TODO TASK: Java annotations will not correspond to .NET attributes:
//ORIGINAL LINE: [TestClass] public class TestRpc
public class TestRpc {
//C# TO JAVA CONVERTER TODO TASK: Java annotations will not correspond to .NET attributes:
//ORIGINAL LINE: [TestMethod] public void TestRpcSimple()
	public final void TestRpcSimple() {
		Service server = new Service("TestRpc.Server");

		FirstRpc forid = new FirstRpc();
		server.AddFactoryHandle(forid.getTypeId(), new Service.ProtocolFactoryHandle() {Factory = () -> new FirstRpc(), Handle = Service.<FirstRpc>MakeHandle(this, this.getClass().getMethod("ProcessFirstRpcRequest"))});

		AsyncSocket servetrSocket = server.NewServerSocket(IPAddress.Any, 5000);
		Client client = new Client(this);
		client.AddFactoryHandle(forid.getTypeId(), new Service.ProtocolFactoryHandle() {Factory = () -> new FirstRpc()});

		AsyncSocket clientSocket = client.NewClientSocket("127.0.0.1", 5000, null);
		connected.WaitOne();

		FirstRpc first = new FirstRpc();
		first.getArgument().Int1 = 1234;
		//Console.WriteLine("SendFirstRpcRequest");
		first.SendForWait(clientSocket).Task.Wait();
		//Console.WriteLine("FirstRpc Wait End");
		assert first.getArgument().getInt1() == first.getResult().getInt1();
	}

	private ManualResetEvent connected = new ManualResetEvent(false);

	public final int ProcessFirstRpcRequest(FirstRpc rpc) {
		rpc.getResult().Assign(rpc.getArgument());
		rpc.SendResult();
		System.out.println("ProcessFirstRpcRequest result.Int1=" + rpc.getResult().getInt1());
		return Procedure.Success;
	}

	public static class FirstRpc extends Rpc<demo.Module1.Value, demo.Module1.Value> {
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