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
		super(new InetSocketAddress(port), 1, List.of(new Draft_6455()));
	}

	@Override
	public void onStart() {
		System.out.println("Server started on port: " + getPort());
		setConnectionLostTimeout(100);
	}

	@Override
	public void onOpen(WebSocket ws, ClientHandshake handshake) {
		ws.send("Welcome to the server!"); // 单发
		broadcast("new connection: " + handshake.getResourceDescriptor()); // 广播
		System.out.println(ws.getRemoteSocketAddress().getAddress().getHostAddress() + " entered the room!");
	}

	@Override
	public void onClose(WebSocket ws, int code, String reason, boolean remote) {
		broadcast(ws + " has left the room!");
		System.out.println(ws + " has left the room!");
	}

	@Override
	public void onError(WebSocket ws, Exception ex) { // 非单个连接引起的异常时ws为null
		ex.printStackTrace();
	}

	@Override
	public void onMessage(WebSocket ws, String message) {
		broadcast(message);
		System.out.println(ws + ": " + message);
	}

	@Override
	public void onMessage(WebSocket ws, ByteBuffer message) {
		broadcast(message.array());
		System.out.println(ws + ": " + message);
	}
}
