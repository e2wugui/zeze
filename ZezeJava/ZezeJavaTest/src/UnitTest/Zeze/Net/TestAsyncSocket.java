package UnitTest.Zeze.Net;

import Zeze.Serialize.*;
import Zeze.Net.AsyncSocket;
import Zeze.Net.Service;
import Zeze.Util.*;
import junit.framework.TestCase;

public class TestAsyncSocket extends TestCase {
	static {
		Task.tryInitThreadPool();
	}

	public static class ServiceClient extends Service {
		public final TaskCompletionSource<Boolean> Future = new TaskCompletionSource<>();

		public ServiceClient() {
			super("TestAsyncSocket.ServiceClient");
		}

		@Override
		public void OnSocketConnected(AsyncSocket so) throws Exception {
			super.OnSocketConnected(so);
			System.out.println("OnSocketConnected: " + so.getSessionId());
			String head = "GET / HTTP/1.1\r\nHost: www.163.com\r\nAccept:*/*\r\n\r\n";
			if (!so.Send(head))
				System.out.println("send fail");
		}

		@Override
		public boolean OnSocketProcessInputBuffer(AsyncSocket so, ByteBuffer input) {
			System.out.println("input size=" + input.size());
			System.out.println(new String(input.Bytes, input.ReadIndex, input.size()));
			input.ReadIndex = input.WriteIndex;
			Future.setResult(true);
			return false;
		}
	}

	public final void testConnect() {
		ServiceClient client = new ServiceClient();
		try (AsyncSocket ignored = client.newClientSocket("www.163.com", 80, null, null)) {
			client.Future.await();
		}
	}
}
