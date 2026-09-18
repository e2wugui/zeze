package Zeze.Net;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import Zeze.Netty.HttpExchange;
import Zeze.Netty.HttpServer;
import Zeze.Netty.HttpWebSocketHandle;
import Zeze.Transaction.DispatchMode;
import Zeze.Transaction.TransactionLevel;
import io.netty.buffer.ByteBuf;
import org.jetbrains.annotations.NotNull;
import org.w3c.dom.Element;

public class WebsocketHandle implements HttpWebSocketHandle {
	private final String path;
	private Zeze.Netty.HttpServer httpServer;
	private Service service;
	private final ConcurrentHashMap<HttpExchange, Websocket> websockets = new ConcurrentHashMap<>();

	public WebsocketHandle(@NotNull String path, @NotNull HttpServer httpServer) {
		this.path = path;
		this.httpServer = httpServer;
	}

	public WebsocketHandle(Element e) {
		this.path = e.getAttribute("Path");
	}

	public void start() {
		if (null == httpServer)
			httpServer = service.getZeze().getAppBase().getHttpServer();
		if (null == httpServer)
			throw new IllegalStateException("httpServer not found.");
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
		// FND8-55：统一走Service.tryAccept（限流+注册+握手完成），websocket接受路径不再
		// 游离于maxConnections之外。超限显式关闭返回（不依赖Netty异常兜底——websockets表
		// 清理走onClose级联，链路迂回）；撞号（addSocket false，连接已被关闭）不再回调
		// OnHandshakeDone。
		try {
			service.tryAccept(websocket);
		} catch (IllegalStateException e) { // too many connections
			websocket.close(e);
		}
	}

	// status==WebSocketCloseStatus.ABNORMAL_CLOSURE.code()时表示连接被强制关闭
	@Override
	public void onClose(@NotNull HttpExchange x, int status, @NotNull String reason) throws Exception {
		var websocket = websockets.remove(x);
		if (null != websocket)
			websocket.close(new IOException("peer closed. status=" + status + " reason=" + reason));
	}

	@Override
	public void onContent(@NotNull HttpExchange x, @NotNull ByteBuf content, boolean isText, boolean isFinal) throws Exception {
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
