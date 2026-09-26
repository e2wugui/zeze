package Zeze.History;

import Zeze.Application;
import Zeze.Builtin.HistoryModule.tHistory;
import Zeze.Netty.HttpServer;
import Zeze.Netty.Netty;
import io.netty.handler.codec.http.HttpResponseStatus;

/**
 * 回放消费链为实验特性（GC-D01裁撤半接线）：内存后端、不跨重启、仅进程内演示。
 * 持久化后端机制（ApplyDatabaseZeze，含游标/记录级原子单元）保留但未接线——
 * 接线需要配置通道与affects输出契约设计，按需整体复活。
 */
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

		// 内存后端：不跨重启，重启后回放副本与游标一起归零（类文档的实验边界）。
		var dbApplied = new ApplyDatabaseMemory();
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

		var countValue = 1;
		if (null != count) {
			try {
				countValue = Integer.parseInt(count);
			} catch (NumberFormatException e) {
				// 非法count参数按默认值1处理。
			}
		}

		// 实验边界（GC-D01）：本批affects（受影响表与键）丢弃，恒答OK——回放链
		// 不承诺增量输出契约。
		applyHelper.apply(countValue);

		x.sendPlainText(HttpResponseStatus.OK, "OK");
	}
}
