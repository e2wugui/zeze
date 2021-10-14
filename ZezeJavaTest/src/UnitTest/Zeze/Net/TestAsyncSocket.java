package UnitTest.Zeze.Net;

import Zeze.Net.*;
import Zeze.Serialize.*;
import Zeze.Util.*;
import UnitTest.*;

//C# TO JAVA CONVERTER TODO TASK: Java annotations will not correspond to .NET attributes:
//ORIGINAL LINE: [TestClass] public class TestAsyncSocket
public class TestAsyncSocket {
	public static class ServiceClient extends Service {
		public TaskCompletionSource<Boolean> Future = new TaskCompletionSource<Boolean>();
		public ServiceClient() {
			super("TestAsyncSocket.ServiceClient");

		}

		@Override
		public void OnSocketConnected(AsyncSocket so) {
			super.OnSocketConnected(so);
			System.out.println("OnSocketConnected: " + so.SessionId);
			String head = "GET http://www.163.com/\r\nHost: www.163.com\r\nAccept:*/*\r\n\r\n";
			so.Send(head);
		}

		@Override
		public void OnSocketProcessInputBuffer(AsyncSocket so, ByteBuffer input) {
			System.out.println("input size=" + input.Size);
			System.out.println(Encoding.UTF8.GetString(input.Bytes, input.ReadIndex, input.Size));
			input.ReadIndex = input.WriteIndex;
			Future.SetResult(true);
		}
	}

//C# TO JAVA CONVERTER TODO TASK: Java annotations will not correspond to .NET attributes:
//ORIGINAL LINE: [TestMethod] public void TestConnect()
	public final void TestConnect() {
		ServiceClient client = new ServiceClient();
		try (AsyncSocket so = client.NewClientSocket("www.163.com", 80, null)) {
			client.Future.Task.Wait();
		}
	}
}