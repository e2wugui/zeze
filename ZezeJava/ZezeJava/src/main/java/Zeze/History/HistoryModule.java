package Zeze.History;

import Zeze.Application;
import Zeze.Builtin.HistoryModule.tHistory;
import Zeze.Netty.HttpServer;
import Zeze.Netty.Netty;
import io.netty.handler.codec.http.HttpResponseStatus;

public class HistoryModule extends AbstractHistoryModule {
	private final Application zeze;
	private HttpServer httpServer;
	private final ApplyHelper applyHelper;

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

		// todo 怎么指定这么名字，采用一个默认的？否则指定再配置有点重复了。
		//var dbApplied = new ApplyDatabaseZeze(zeze, "_history_applied_db_");
		var dbApplied = new ApplyDatabaseMemory();
		// todo dbApplied 如果是持久化的，applyHelper.exclusiveStartKey也需要持久化。
		applyHelper = new ApplyHelper(zeze, _tHistory, dbApplied, 20_000);
	}

	public Application getZeze() {
		return zeze;
	}

	public tHistory getHistoryTable() {
		return _tHistory;
	}

	@Override
	protected void OnServletWalkPage(Zeze.Netty.HttpExchange x) throws Exception {
		var queryMap = x.queryMap();
		var count = queryMap.get("count");

		// todo 结果丢失了类型；
		//  根据需求，调整 ApplyHelper 吧。
		// var affects =
		applyHelper.apply(count != null ? Integer.parseInt(count) : 1);

		x.sendPlainText(HttpResponseStatus.OK, "OK");
	}
}
