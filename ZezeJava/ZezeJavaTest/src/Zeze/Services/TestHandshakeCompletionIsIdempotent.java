package Zeze.Services;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.concurrent.atomic.AtomicInteger;
import Zeze.Config;
import Zeze.Net.AsyncSocket;
import Zeze.Net.TcpSocket;
import Zeze.Serialize.ByteBuffer;
import Zeze.Services.Handshake.CHandshakeDone;
import Zeze.Services.Handshake.KeepAlive;
import Zeze.Util.Task;
import harness.Fast;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;

@Fast
public class TestHandshakeCompletionIsIdempotent {
	@Test
	void repeatedWireCompletionDoesNotReinitializeTheConnection() throws Exception {
		Task.tryInitThreadPool();
		var completions = new AtomicInteger();
		var service = new HandshakeServer("HandshakeCompletion", new Config()) {
			@Override
			public void OnHandshakeDone(AsyncSocket socket) throws Exception {
				super.OnHandshakeDone(socket);
				completions.incrementAndGet();
			}
		};
		var listener = (TcpSocket)service.newServerSocket(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), null);
		try (var peer = new Socket()) {
			peer.connect(listener.getLocalAddress());
			peer.setSoTimeout(3_000);
			var input = peer.getInputStream();
			var header = input.readNBytes(12); // 先消费服务端 SHandshake0。
			assertEquals(12, header.length);
			input.readNBytes(ByteBuffer.ToInt(header, 8));
			var bytes = ByteBuffer.Allocate();
			new CHandshakeDone().encodeWithHead(bytes);
			new CHandshakeDone().encodeWithHead(bytes);
			var barrier = new KeepAlive();
			barrier.setSessionId(1);
			barrier.encodeWithHead(bytes);
			peer.getOutputStream().write(bytes.Bytes, 0, bytes.WriteIndex);
			// KeepAlive 的应答作为同连接 Selector 处理屏障，不依赖睡眠等待第二个通知。
			header = input.readNBytes(12);
			assertEquals(12, header.length);
			assertEquals(KeepAlive.ProtocolId_, ByteBuffer.ToInt(header, 4));
			input.readNBytes(ByteBuffer.ToInt(header, 8));
			assertEquals(1, completions.get());
		} finally {
			listener.close();
			service.stop();
		}
	}
}
