package Temp;

import java.net.InetSocketAddress;
import Zeze.Net.Acceptor;
import Zeze.Net.AsyncSocket;
import Zeze.Net.Selectors;
import Zeze.Net.Service;
import Zeze.Net.TcpSocket;
import Zeze.Serialize.ByteBuffer;
import org.jetbrains.annotations.NotNull;

public class TestRecv extends Service {
	public TestRecv(@NotNull String name) {
		super(name);

		var config = new Selectors.Config();
//		var sendBuffer = getConfig().getSocketOptions().getSendBuffer();
//		if (sendBuffer != null)
			config.bbPoolBlockSize = 65536;//sendBuffer;
		config.selectTimeout = -1;
//		var recvBuffer = getConfig().getSocketOptions().getReceiveBuffer();
//		if (recvBuffer != null)
			config.readBufferSize = 32768;
		setSelectors(new Selectors("Linkd", 1, config));
	}

	public static void main(String[] args) throws InterruptedException {
		var so = new TcpSocket(new TestRecv("server"), new InetSocketAddress(9999), new Acceptor(9999, null));

		Thread.sleep(Integer.MAX_VALUE);
	}

	@Override
	public void OnHandshakeDone(@NotNull AsyncSocket so) throws Exception {
		System.out.println("accept");
		((TcpSocket)so).setInputSecurityCodec(1, new byte[]{1}, 1);

	}

	@Override
	public boolean OnSocketProcessInputBuffer(@NotNull AsyncSocket so, @NotNull ByteBuffer input) throws Exception {
		if (input.size() >= 2 * 1024 * 1024) {
			System.out.println("recv: " + input.size());
			input.ReadIndex = input.WriteIndex;
		} else {
			System.out.println("input: " + input.size());
		}
		return false;
	}
}
