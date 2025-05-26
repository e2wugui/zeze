package Zeze.Net;

import java.util.concurrent.ConcurrentHashMap;
import Zeze.Netty.HttpExchange;
import Zeze.Netty.HttpWebSocketHandle;
import Zeze.Transaction.DispatchMode;
import Zeze.Transaction.TransactionLevel;
import io.netty.buffer.ByteBuf;
import org.jetbrains.annotations.NotNull;
import org.w3c.dom.Element;

public class WebsocketHandle implements HttpWebSocketHandle {
	private final String path;
	private Zeze.Netty.HttpServer httpServer; // todo 怎么获取这个变量？通过 Zeze 传递？
	private Service service;
	private final ConcurrentHashMap<HttpExchange, Websocket> websockets = new ConcurrentHashMap<>();

	public WebsocketHandle(@NotNull String path) {
		this.path = path;
	}

	public WebsocketHandle(Element e) {
		this.path = e.getAttribute("Path");
		//this.httpServer = e.getAttribute("HttpServer");
	}

	public void start() {
		httpServer.addHandler(path, TransactionLevel.None, DispatchMode.Direct,this);
	}

	public void stop() {
		httpServer.removeHandler(path);
	}

	@Override
	public void onOpen(@NotNull HttpExchange x) throws Exception {
		var websocket = new Websocket(x, service);
		if (null != websockets.putIfAbsent(x, websocket))
			throw new IllegalStateException("duplicate onOpen for a HttpExchange.");
		service.addSocket(websocket);
		service.OnHandshakeDone(websocket);
	}

	// status==WebSocketCloseStatus.ABNORMAL_CLOSURE.code()时表示连接被强制关闭
	@Override
	public void onClose(@NotNull HttpExchange x, int status, @NotNull String reason) throws Exception {
		var websocket = websockets.remove(x);
		if (null != websocket)
			getService().OnSocketClose(websocket, null);
	}

	@Override
	public void onBinary(@NotNull HttpExchange x, @NotNull ByteBuf content) throws Exception {
		var websocket = websockets.get(x);
		if (null != websocket)
			websocket.processInput(content);
		else
			x.closeConnectionNow();
	}

	public @NotNull String getName() {
		return path;
	}

	public void setService(@NotNull Service service) {
		this.service = service;
	}

	public @NotNull Service getService() {
		return this.service;
	}
}
