package Zeze.History;

import java.util.Map;
import java.util.Set;
import Zeze.Application;
import Zeze.Builtin.HistoryModule.tHistory;
import Zeze.Netty.HttpServer;
import Zeze.Netty.Netty;
import Zeze.Transaction.Changes;
import Zeze.Transaction.IHistory;
import Zeze.Transaction.Procedure;
import Zeze.Transaction.ProtocolProcedure;
import Zeze.Transaction.Table;
import Zeze.Util.Id128;
import io.netty.handler.codec.http.HttpResponseStatus;
import org.jspecify.annotations.Nullable;

/**
 * tHistory 历史记录模块：管理 tHistory 持久化表、提交前 gid 分配与对账账本，以及实验性的内存回放分析服务。
 */
public class HistoryModule extends AbstractHistoryModule implements IHistory {
	private final Application zeze;
	private final String historyName;
	private @Nullable HttpServer httpServer;
	private @Nullable ApplyHelper applyHelper;
	private volatile @Nullable PendingGidLedger pendingGidLedger;
	private volatile boolean closed;
	private boolean tablesRegistered;
	private @Nullable HistoryRuntimeBeans runtimeBeanLogs;

	/** 工具入口只取已有模块，关闭配置时不隐式创建。 */
	public static HistoryModule get(Application zeze) {
		if (zeze.getHistory() instanceof HistoryModule module)
			return module;
		throw new IllegalStateException("History is not initialized for " + zeze.getProjectName());
	}

	public String getHistoryName() {
		return historyName;
	}

	private void ensureOpen() {
		if (closed)
			throw new IllegalStateException("History is closed");
	}

	public PendingGidLedger getPendingGidLedger() {
		// 已创建的账本不与在飞回放争锁，首次创建仍与关闭互斥。
		var ledger = pendingGidLedger;
		if (ledger != null) {
			ensureOpen();
			return ledger;
		}
		lock();
		try {
			ensureOpen();
			ledger = pendingGidLedger;
			if (ledger == null)
				pendingGidLedger = ledger = new PendingGidLedger(zeze.getProjectName());
			return ledger;
		} finally {
			unlock();
		}
	}

	@Override
	public void registerLogs(Iterable<Table> tables) throws Exception {
		Helper.registerTableLogs(tables);
	}

	@Override
	public void afterOpen() {
		OwnerCheck.verify(this);
		lock();
		try {
			ensureOpen();
			if (runtimeBeanLogs == null)
				runtimeBeanLogs = HistoryRuntimeBeans.subscribe();
		} finally {
			unlock();
		}
	}

	@Override
	public PreparedCommit prepareCommit(Procedure procedure) throws CommitRejectedException {
		if (procedure.getZeze() != zeze)
			throw new IllegalArgumentException("procedure belongs to another Application");
		var ledger = getPendingGidLedger();
		var gid = allocateAndRegisterGid(procedure, ledger);
		return new PreparedCommit() {
			private boolean captured;

			@Override
			public IHistory.PendingHistory capture(Changes changes) {
				if (captured)
					throw new IllegalStateException("history commit already captured");
				captured = true;
				var data = procedure instanceof ProtocolProcedure pp
						? History.buildLogChanges(gid, changes, pp.getProtocolClassName(), pp.getProtocolRawArgument())
						: History.buildLogChanges(gid, changes, null, null);
				return new History(_tHistory, ledger, data);
			}
		};
	}

	/** 取号和登记共同构成应用数据前的准备阶段，受控失败只在这里转换。 */
	@SuppressWarnings("ConstantConditions")
	private Id128 allocateAndRegisterGid(Procedure procedure, PendingGidLedger ledger)
			throws CommitRejectedException {
		try {
			var sm = zeze.getServiceManager();
			if (sm == null)
				throw new IllegalStateException("History requires a ServiceManager with Id128 allocation");
			var gid = sm.getUsableTid128CacheFuture(historyName).get().next();
			ledger.register(gid, System.nanoTime());
			return gid;
		} catch (Exception ex) {
			if (ex instanceof InterruptedException)
				Thread.currentThread().interrupt();
			throw new CommitRejectedException("history gid allocation failed: " + procedure.getActionName(), ex);
		}
	}

	/** 在模块锁内完成回放，关闭不会与访问业务库的在飞回放交错。 */
	public Map<ApplyTable<?, ?>, Set<Object>> apply(int count) throws Exception {
		lock();
		try {
			ensureOpen();
			if (applyHelper == null)
				applyHelper = new ApplyHelper(zeze, _tHistory, new ApplyDatabaseMemory(), 20_000);
			return applyHelper.apply(count);
		} finally {
			unlock();
		}
	}

	// 需要应用调用一下开启分析服务（host=null绑任意地址）。
	public void startHttpServer(Netty netty, int port) throws Exception {
		startHttpServer(netty, null, port);
	}

	/**
	 * 启动回放分析 HTTP 服务并同步确认端口绑定结果；失败时回收半启动服务并抛出异常（不关闭调用方持有的 netty）。
	 */
	public void startHttpServer(Netty netty, @Nullable String host, int port) throws Exception {
		lock();
		try {
			ensureOpen();
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

	/** 只停止 HTTP 分析服务；模块整体生命周期由 close 管理。 */
	public void stop() {
		final HttpServer server;
		lock();
		try {
			server = httpServer;
		} finally {
			unlock();
		}
		if (server == null)
			return;
		// close 会等待 HTTP handler 排空，handler 可能正在等待模块锁。
		server.close();
		lock();
		try {
			if (httpServer == server)
				httpServer = null;
		} finally {
			unlock();
		}
	}

	public HistoryModule(Application zeze) {
		this.zeze = zeze;
		historyName = zeze.getConfig().getHistory();
		RegisterZezeTables(zeze);
		tablesRegistered = true;
	}

	@Override
	public void close() {
		final PendingGidLedger ledger;
		lock();
		try {
			closed = true;
			if (runtimeBeanLogs != null) {
				runtimeBeanLogs.unsubscribe();
				runtimeBeanLogs = null;
			}
			ledger = pendingGidLedger;
			applyHelper = null;
		} finally {
			unlock();
		}
		Throwable failure = closeStep(null, this::stop);
		if (ledger != null)
			failure = closeStep(failure, ledger::stop);
		failure = closeStep(failure, this::unregisterTables);
		if (failure != null)
			throw new IllegalStateException("History close failed", failure);
	}

	private void unregisterTables() {
		lock();
		try {
			if (tablesRegistered) {
				UnRegisterZezeTables(zeze);
				tablesRegistered = false;
			}
		} finally {
			unlock();
		}
	}

	private static @Nullable Throwable closeStep(@Nullable Throwable failure, Runnable action) {
		try {
			action.run();
		} catch (Throwable ex) {
			if (failure == null)
				return ex;
			if (failure != ex)
				failure.addSuppressed(ex);
		}
		return failure;
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
				// 非法 count 按默认值 1 处理
			}
			// 夹紧下界为 1，避免底层 walk 对 count<=0 直接跳过。
			countValue = Math.max(1, countValue);
		}

		apply(countValue);

		x.sendPlainText(HttpResponseStatus.OK, "OK");
	}
}
