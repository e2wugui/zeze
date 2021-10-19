package UnitTest.Zeze.Net;

import Zeze.Net.*;
import Zeze.Serialize.*;
import Zeze.Util.*;
import junit.framework.TestCase;

public class TestAsyncSocket extends TestCase {
	public static class ServiceClient extends Service {
		public TaskCompletionSource<Boolean> Future = new TaskCompletionSource<>();
		public ServiceClient() {
			super("TestAsyncSocket.ServiceClient");
		}

		@Override
		public void OnSocketConnected(AsyncSocket so) {
			super.OnSocketConnected(so);
			System.out.println("OnSocketConnected: " + so.getSessionId());
		}

		@Override
		public void OnSocketProcessInputBuffer(AsyncSocket so, ByteBuffer input) {
			System.out.println("input size=" + input.Size());
			System.out.println(input.ReadString());
			input.ReadIndex = input.WriteIndex;
			Future.SetResult(true);
		}
	}

	public final void testConnect() {
		ServiceClient client = new ServiceClient();
		try (AsyncSocket so = client.NewClientSocket("www.163.com", 80, null)) {
			String head = "GET http://www.163.com/\r\nHost: www.163.com\r\nAccept:*/*\r\n\r\n";
			boolean suc = so.Send(head);
			if(!suc) {
				System.out.println("send fail");
				return;
			}
			client.Future.Wait();
		}
	}
}