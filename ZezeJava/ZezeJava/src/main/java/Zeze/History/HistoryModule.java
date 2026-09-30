package Zeze.History;

import Zeze.Application;
import Zeze.Builtin.HistoryModule.tHistory;
import Zeze.Netty.HttpServer;
import Zeze.Netty.Netty;
import io.netty.handler.codec.http.HttpResponseStatus;

/**
 * 回放消费链为实验特性：内存后端、不跨重启、仅进程内演示。
 * 持久化后端机制（ApplyDatabaseZeze，含游标/记录级原子单元）保留但未接线——
 * 接线需要配置通道与affects输出契约设计。
 */
public class HistoryModule extends AbstractHistoryModule {
	private final Application zeze;
	private HttpServer httpServer;
	private final ApplyHelper applyHelper;

	// 需要应用调用一下开启分析服务（host=null绑任意地址）。
	public void startHttpServer(Netty netty, int port) throws Exception {
		startHttpServer(netty, null, port);
	}

	/**
	 * 起回放分析端点（WalkPage）并<b>同步确认bind结果</b>：HttpServer.start对b.bind的
	 * ChannelFuture不同步不记日志，bind失败（端口占用/坏地址）异步发生在event loop上——
	 * 丢弃返回值则调用方正常返回且startServer的info日志已打出，端点静默缺失、零错误日志。
	 * 失败时回收半启动server（其内部已注册周期scheduler）后抛含cause的IllegalStateException
	 * （对齐Netty.java的.sync()规范）；httpServer字段只在成功后发布，失败不闩死幂等分支，
	 * 修复端口/地址后重调即进入。netty为调用方持有，不在此回收。
	 */
	public void startHttpServer(Netty netty, String host, int port) throws Exception {
		lock();
		try {
			if (null != httpServer)
				return; // skip duplicate start

			var server = new HttpServer();
			var future = server.start(netty, host, port);
			future.awaitUninterruptibly();
			if (!future.isSuccess()) {
				server.close();
				throw new IllegalStateException("History http server bind failed on "
						+ (null == host || host.isBlank() ? "any" : host) + ":" + port
						+ " - check port conflict or bind address.", future.cause());
			}
			RegisterHttpServlet(server);
			httpServer = server;
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
			// 0/负数与非法格式同等按默认值1处理：底层walk对count<=0不遍历直接返回null，空批OK应答与无记录不可区分。
			countValue = Math.max(1, countValue);
		}

		// 实验边界：本批affects（受影响表与键）丢弃，恒答OK——回放链不承诺增量输出契约。
		applyHelper.apply(countValue);

		x.sendPlainText(HttpResponseStatus.OK, "OK");
	}
}
