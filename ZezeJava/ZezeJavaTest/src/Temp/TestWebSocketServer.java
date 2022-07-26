package Temp;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.List;
import org.java_websocket.WebSocket;
import org.java_websocket.drafts.Draft_6455;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;

// gradle: implementation 'org.java-websocket:Java-WebSocket:1.5.3'
// ref: https://github.com/TooTallNate/Java-WebSocket/blob/master/src/main/example/ChatServer.java
// run: java -cp .;lib/*;build/classes/java/main Temp.TestWebSocketServer
public class TestWebSocketServer extends WebSocketServer {
	public static void main(String[] args) throws Exception {
		var s = new TestWebSocketServer(8887);
		s.start();

		for (var stdin = new BufferedReader(new InputStreamReader(System.in)); ; ) {
			String line = stdin.readLine();
			if (line == null)
				break;
			s.broadcast(line);
			if (line.equals("exit")) {
				s.stop(1000);
				break;
			}
		}
	}

	public TestWebSocketServer(int port) {
		super(new InetSocketAddress(port), 1, List.of(new Draft_6455())); // 启动1个NIO selector线程和可指定的N个工作线程
	}

	@Override
	public void onStart() {
		System.out.println("onStart: port=" + getPort());
		setConnectionLostTimeout(100);
	}

	@Override
	public void onOpen(WebSocket ws, ClientHandshake ch) {
		System.out.println("onOpen: ws=" + ws.getRemoteSocketAddress() + ", desc=" + ch.getResourceDescriptor());
		ws.send("Hello From Server"); // 单发
		broadcast("Welcome: " + ch.getResourceDescriptor()); // 广播
	}

	@Override
	public void onClose(WebSocket ws, int code, String reason, boolean remote) {
		System.out.println("onClose: ws=" + ws.getRemoteSocketAddress()
				+ ", code=" + code + ", reason=" + reason + ", remote=" + remote);
	}

	@Override
	public void onError(WebSocket ws, Exception ex) { // 非单个连接引起的异常时ws为null
		System.out.println("onError: ws=" + (ws != null ? ws.getRemoteSocketAddress() : "null"));
		ex.printStackTrace();
		if (ws != null)
			ws.close();
	}

	@Override
	public void onMessage(WebSocket ws, String message) {
		System.out.println("onMessage: ws=" + ws.getRemoteSocketAddress() + ", message=" + message);
	}

	@Override
	public void onMessage(WebSocket ws, ByteBuffer bytes) {
		System.out.println("onMessage: ws=" + ws.getRemoteSocketAddress() + ", bytes=" + bytes);
	}
}
