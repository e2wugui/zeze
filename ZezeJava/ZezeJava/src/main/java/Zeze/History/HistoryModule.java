package Zeze.History;

import Zeze.Application;
import Zeze.Builtin.HistoryModule.ZezeHistoryTable_m_a_g_i_c;
import Zeze.Netty.HttpServer;
import Zeze.Netty.Netty;
import io.netty.handler.codec.http.HttpResponseStatus;

public class HistoryModule extends AbstractHistoryModule {
	private final Application zeze;
	private HttpServer httpServer;

	// 需要应用调用一下开启分析服务。
	public void startHttpServer(Netty netty, int port) throws Exception {
		lock();
		try {
			if (null != httpServer)
				return; // skip duplicate start

			httpServer = new HttpServer();
			httpServer.start(netty, port);
			RegisterHttpServlet(httpServer);
		} finally {
			unlock();
		}
	}

	public void stop() {
		lock();
		try {
			if (null != httpServer) {
				httpServer.close();
				httpServer = null;
			}
		} finally {
			unlock();
		}
	}

	public HistoryModule(Application zeze) {
		this.zeze = zeze;
		RegisterZezeTables(zeze);
	}

	public Application getZeze() {
		return zeze;
	}

	public ZezeHistoryTable_m_a_g_i_c getTable() {
		return _ZezeHistoryTable_m_a_g_i_c;
	}

	@Override
	protected void OnServletWalkPage(Zeze.Netty.HttpExchange x) throws Exception {
		var queryMap = x.queryMap();
		var start = queryMap.get("start");
		var limit = queryMap.get("limit");

		Long exclusiveStartKey = start != null ? Long.parseLong(start) : null;
		var proposeLimit = limit != null ? Integer.parseInt(limit) : 20;

		var text = new StringBuilder();
		getTable().walkDatabase(exclusiveStartKey, proposeLimit, (key, value) -> {
			text.append(key.toString()).append("->").append(value.toString()).append("\n");
			return true;
		});
		x.sendPlainText(HttpResponseStatus.OK, text.toString());
	}
}
