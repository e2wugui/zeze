package Temp;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.ByteBuffer;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.drafts.Draft_6455;
import org.java_websocket.handshake.ServerHandshake;

// gradle: implementation 'org.java-websocket:Java-WebSocket:1.5.3'
// ref: https://github.com/TooTallNate/Java-WebSocket/blob/master/src/main/example/ExampleClient.java
// run: java -cp .;lib/*;build/classes/java/main Temp.TestWebSocketClient
public class TestWebSocketClient extends WebSocketClient {
	public static void main(String[] args) throws Exception {
		var c = new TestWebSocketClient(8887);
		c.connect();

		for (var stdin = new BufferedReader(new InputStreamReader(System.in)); ; ) {
			String line = stdin.readLine();
			if (line == null)
				break;
			c.send(line);
			if (line.equals("exit")) {
				c.close();
				break;
			}
		}
	}

	public TestWebSocketClient(int port) throws URISyntaxException {
		super(new URI("ws://127.0.0.1:" + port), new Draft_6455()); // 使用BIO网络API,启动1个读线程和1个写线程
	}

	@Override
	public void onOpen(ServerHandshake sh) {
		System.out.println("onOpen: status=" + sh.getHttpStatus() + ", message=" + sh.getHttpStatusMessage());
		send("Hello From Client");
	}

	@Override
	public void onClose(int code, String reason, boolean remote) {
		System.out.println("onClose: code=" + code + ", reason=" + reason + ", remote=" + remote);
	}

	@Override
	public void onError(Exception ex) {
		ex.printStackTrace();
		close();
	}

	@Override
	public void onMessage(String message) {
		System.out.println("onMessage: message=" + message);
	}

	@Override
	public void onMessage(ByteBuffer bytes) {
		System.out.println("onMessage: bytes=" + bytes);
	}
}
