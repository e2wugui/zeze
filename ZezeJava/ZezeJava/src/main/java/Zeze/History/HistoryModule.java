package Zeze.History;

import java.util.Objects;

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
 * 回放消费链为实验特性：内存后端、不跨重启、仅进程内演示。
 * 持久化后端机制（ApplyDatabaseZeze，含游标/记录级原子单元）保留但未接线——
 * 接线需要配置通道与affects输出契约设计。
 */
public class HistoryModule extends AbstractHistoryModule implements IHistory {
	private final Application zeze;
	private final String historyName;
	private @Nullable HttpServer httpServer;
	private @Nullable ApplyHelper applyHelper;
	private volatile @Nullable PendingGidLedger pendingGidLedger;
	private volatile boolean closed;
	private boolean tablesRegistered;

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
			if (pendingGidLedger == null)
				pendingGidLedger = new PendingGidLedger(zeze.getProjectName());
			return Objects.requireNonNull(pendingGidLedger);
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
	 * 起回放分析端点（WalkPage）并<b>同步确认bind结果</b>：HttpServer.start对b.bind的
	 * ChannelFuture不同步不记日志，不同步确认则bind失败（端口占用/坏地址）端点静默缺失。
	 * 失败时回收半启动server后抛含cause的IllegalStateException（对齐Netty.java的.sync()
	 * 规范）；httpServer字段只在成功后发布，失败不闩死幂等分支，修复后重调即进入。
	 * netty为调用方持有，不在此回收。
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
				// 非法count参数按默认值1处理。
			}
			// 0/负数与非法格式同等按默认值1处理：底层walk对count<=0不遍历直接返回null，空批OK应答与无记录不可区分。
			countValue = Math.max(1, countValue);
		}

		// 实验边界：本批affects（受影响表与键）丢弃，恒答OK——回放链不承诺增量输出契约。
		apply(countValue);

		x.sendPlainText(HttpResponseStatus.OK, "OK");
	}
}
