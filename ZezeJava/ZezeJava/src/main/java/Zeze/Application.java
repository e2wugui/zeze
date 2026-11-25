package Zeze;

import java.util.Objects;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;
import java.util.TimeZone;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;
import java.util.concurrent.locks.ReentrantLock;

import Zeze.Arch.ProviderApp;
import Zeze.Arch.RedirectBase;
import Zeze.Component.AutoKey;
import Zeze.Component.DelayRemove;
import Zeze.Component.SafeBatch;
import Zeze.Component.Takeover;
import Zeze.Component.Timer;
import Zeze.Dbh2.Dbh2AgentManager;
import Zeze.Hot.HotHandle;
import Zeze.Hot.HotManager;
import Zeze.Hot.HotUpgradeMemoryTable;
import Zeze.Net.Binary;
import Zeze.Onz.Onz;
import Zeze.Serialize.ByteBuffer;
import Zeze.Services.Daemon;
import Zeze.Services.GlobalCacheManagerWithRaftAgent;
import Zeze.Services.ServiceManager.AbstractAgent;
import Zeze.Services.ServiceManager.Agent;
import Zeze.Services.ServiceManagerAgentWithRaft;
import Zeze.Transaction.AchillesHeelDaemon;
import Zeze.Transaction.Checkpoint;
import Zeze.Transaction.Database;
import Zeze.Transaction.DatabaseRelationalMapping;
import Zeze.Transaction.DatabaseRocksDb;
import Zeze.Transaction.DispatchMode;
import Zeze.Transaction.GlobalAgent;
import Zeze.Transaction.IGlobalAgent;
import Zeze.Transaction.IHistory;
import Zeze.Transaction.Locks;
import Zeze.Transaction.Procedure;
import Zeze.Transaction.ProcedureLockWatcher;
import Zeze.Transaction.ProtocolProcedure;
import Zeze.Transaction.Table;
import Zeze.Transaction.TableKey;
import Zeze.Transaction.TransactionLevel;
import Zeze.Util.Action0;
import Zeze.Util.DeadlockBreaker;
import Zeze.Util.EventDispatcher;
import Zeze.Util.FuncLong;
import Zeze.Util.LongConcurrentHashMap;
import Zeze.Util.ShutdownHook;
import Zeze.Util.Str;
import Zeze.Util.Task;
import Zeze.Util.TaskOneByOneByKey;
import Zeze.Util.TaskSpec;
import Zeze.Util.ZezeCounter;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jspecify.annotations.Nullable;
import org.jetbrains.annotations.UnmodifiableView;

/**
 * Zeze 应用核心：组装数据库、表、锁、组件（Timer/AutoKey/Queue 等）与服务管理器，编排 start/stop 生命周期。
 */
public final class Application extends ReentrantLock {
	static final Logger logger = LogManager.getLogger(Application.class);

	// 终检点后等待在飞flush归零/被中断时重join检查点线程的上限，超时告警继续关库。
	private static final long CHECKPOINT_DRAIN_TIMEOUT_MILLIS = 30_000;

	private final String projectName;
	private final Config conf;
	private final HashMap<String, Database> databases = new LinkedHashMap<>();
	private final ArrayList<Database> databasesInUse = new ArrayList<>(); // 仅记录本实例成功取得的启动登记。
	private final LongConcurrentHashMap<Table> tables = new LongConcurrentHashMap<>();
	private final ConcurrentHashMap<String, Table> tableNameMap = new ConcurrentHashMap<>();
	private final Locks locks = new Locks();
	private final @Nullable AbstractAgent serviceManager;
	private AutoKey.@Nullable Module autoKey;
	private @Nullable Timer timer;
	private @Nullable SafeBatch safeBatch;
	private Zeze.Collections.Queue.@Nullable Module queueModule;
	private @Nullable DelayRemove delayRemove;
	private volatile @Nullable Takeover takeover;
	private @Nullable IHistory history;
	private @Nullable IGlobalAgent globalAgent;
	private @Nullable AchillesHeelDaemon achillesHeelDaemon;
	private @Nullable DeadlockBreaker deadlockBreaker;
	// stop 置 null 后需立即对事务/Reduce 线程可见，防止终检点后继续登记脏集。
	private volatile @Nullable Checkpoint checkpoint;
	private @Nullable Future<?> flushWhenReduceTimerTask;
	private @Nullable Schemas schemas;
	private @Nullable Schemas schemasPrevious;
	private final ProcedureLockWatcher procedureLockWatcher;

	public enum StartState {
		eUninitialized,
		eStarting,
		eStarted,
		eStopping,
		eStopped, // 终态，构造期组件已拆除，实例不可复用。
	}

	private volatile StartState startState = StartState.eUninitialized;
	public @Nullable RedirectBase redirect;

	private @Nullable Onz onz;

	private final HotHandle<EventDispatcher.EventHandle> hotHandle = new HotHandle<>();

	private final LocalCacheResources localCache = new LocalCacheResources();

	private @Nullable Dbh2AgentManager dbh2AgentManager;
	private @Nullable HotManager hotManager;

	private @Nullable AppBase appBase;
	private @Nullable ProviderApp providerApp;

	private final ArrayList<Table> replaceTableRecent = new ArrayList<>();
	private final ArrayList<HotUpgradeMemoryTable> hotUpgradeMemoryTables = new ArrayList<>();

	// 只由持锁者（checkpointRunThread/stop）读写；"checkpoint在跑中"=非null且未完成。
	private @Nullable Future<?> checkpointFuture;

	private static final ConcurrentHashMap<String, Application> instances = new ConcurrentHashMap<>();

	public static @Nullable Application getAppInstance(String projectName) {
		return instances.get(projectName);
	}

	public ProcedureLockWatcher getProcedureLockWatcher() {
		return procedureLockWatcher;
	}

	public HotHandle<EventDispatcher.EventHandle> getHotHandle() {
		return hotHandle;
	}

	public @Nullable Onz getOnz() {
		return onz;
	}

	/**
	 * 供 verifyCallerCold 的调用方获取 caller。
	 * getCallerClass() 必须在公开入口内直接调用，经框架方法转发会误取到 Zeze 内部类。
	 */
	public static final StackWalker CALLER_WALKER =
		StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE);

	/**
	 * 是否需要热更新调用者校验；调用方应先检查，避免无热更时的走栈开销。
	 */
	public boolean isHotVerifyEnabled() {
		return hotManager != null;
	}

	// caller 必须由公开入口直接获取。
	public void verifyCallerCold(Class<?> caller) {
		var callerCl = caller.getClassLoader();
		// 只限制我们自己的HotModule，其他都允许。
		if (hotManager != null && HotManager.isHotModule(callerCl))
			throw new IllegalStateException("caller must not hot.");
	}

	public void setHotManager(@Nullable HotManager value) {
		hotManager = value;
	}

	public @Nullable HotManager getHotManager() {
		return hotManager;
	}

	public @Nullable Dbh2AgentManager getDbh2AgentManager() {
		return dbh2AgentManager;
	}

	public Dbh2AgentManager tryNewDbh2AgentManager() throws Exception {
		if (dbh2AgentManager == null)
			dbh2AgentManager = new Dbh2AgentManager(Objects.requireNonNull(serviceManager), conf);
		return dbh2AgentManager;
	}

	public Application(String solutionName) throws Exception {
		this(solutionName, null);
	}

	public static @Nullable AbstractAgent createServiceManager(Config conf,
															   String raftSessionNamePrefix) throws Exception {
		return switch (conf.getServiceManager()) {
			case "raft" -> {
				var smConf = conf.getServiceManagerConf();
				if (smConf == null)
					throw new IllegalStateException("ServiceManager=raft requires a ServiceManagerConf node");
				if (smConf.getSessionName().isEmpty())
					smConf.setSessionName(raftSessionNamePrefix + "#" + conf.getServerId());
				yield new ServiceManagerAgentWithRaft(conf);
			}
			case "disable" -> null;
			default -> new Agent(conf);
		};
	}

	public Application(String projectName, @Nullable Config config) throws Exception {
		this.projectName = projectName;
		conf = config != null ? config : Config.load();
		if (conf.getServerId() > 0x3FFF) // 16383 encoded size = 2 bytes
			throw new IllegalStateException("serverId too big. > 16383.");
		procedureLockWatcher = new ProcedureLockWatcher(this);

		Task.tryInitThreadPool(this.getConfig(), null, null); // 如需定制线程池，须在创建 Application 前初始化。

		serviceManager = createServiceManager(conf, projectName); // Dbh2 建库依赖 serviceManager，须先初始化。
		boolean databasesCreated = false;
		try {
			conf.createDatabase(this, databases);
			databasesCreated = true; // createDatabase失败时由其自身回收已创建的库。
			ZezeCounter.tryInit();

			if (!isNoDatabase()) {
				autoKey = new AutoKey.Module(this);
				queueModule = new Zeze.Collections.Queue.Module(this);
				if (conf.isHistory())
					history = new Zeze.History.HistoryModule(this);
				delayRemove = new DelayRemove(this);
				takeover = new Takeover(this);
				onz = new Onz(this);
			}
		} catch (Throwable failure) {
			closeConstructionResources(failure, databasesCreated);
			throw failure;
		}
	}

	private void closeConstructionResources(Throwable failure, boolean databasesCreated) {
		if (history != null)
			cleanupAfterFailure(failure, history::close);
		if (databasesCreated) {
			// 保留取得顺序，先拆卸后取得的库，再关闭它们依赖的Dbh2管理器和SM。
			var acquired = new ArrayList<>(databases.values());
			for (int i = acquired.size() - 1; i >= 0; --i)
				cleanupAfterFailure(failure, acquired.get(i)::close);
		}
		if (dbh2AgentManager != null)
			cleanupAfterFailure(failure, dbh2AgentManager::stop);
		if (serviceManager != null)
			cleanupAfterFailure(failure, serviceManager::close);
	}

	private static void cleanupAfterFailure(Throwable failure, Action0 close) {
		try {
			close.run();
		} catch (Throwable cleanupFailure) {
			if (failure != cleanupFailure)
				failure.addSuppressed(cleanupFailure);
		}
	}

	public @Nullable AppBase getAppBase() {
		return appBase;
	}

	public @Nullable ProviderApp getProviderApp() {
		return providerApp;
	}

	public void setProviderApp(ProviderApp providerApp) {
		this.providerApp = providerApp;
	}

	public void initialize(AppBase app) {
		lock();
		try {
			appBase = app;
			if (timer == null && !isNoDatabase() && redirect != null)
				timer = Timer.create(app);
			if (timer != null && safeBatch == null)
				safeBatch = new SafeBatch(this);
		} finally {
			unlock();
		}
	}

	public boolean isNoDatabase() {
		return conf.isNoDatabase() || conf.getServerId() < 0;
	}

	public HashMap<String, Database> getDatabases() {
		return databases;
	}

	public Config getConfig() {
		return conf;
	}

	public boolean isStart() {
		return startState == StartState.eStarted;
	}

	public @Nullable AbstractAgent getServiceManager() {
		return serviceManager;
	}

	// 未启用 GCM 时返回 null。
	public @Nullable IGlobalAgent getGlobalAgent() {
		return globalAgent;
	}

	// 未启动或停机时返回 null。
	public @Nullable Checkpoint getCheckpoint() {
		return checkpoint;
	}

	public Locks getLocks() {
		return locks;
	}

	public @Nullable Schemas getSchemas() {
		return schemas;
	}

	public @Nullable Schemas getSchemasPrevious() {
		return schemasPrevious;
	}

	public void setSchemas(Schemas value) {
		schemas = value;
	}

	public String getProjectName() {
		return projectName;
	}

	public @Nullable AchillesHeelDaemon getAchillesHeelDaemon() {
		return achillesHeelDaemon;
	}

	public @Nullable DatabaseRocksDb getLocalRocksCacheDb() {
		return localCache.database();
	}

	public Database addTable(String dbName, Table table) {
		var db = getDatabase(dbName);
		// 先通过 ID/名称唯一性校验再发布索引，避免失败时留下半注册状态。
		// 调用方持 Application 锁或处于单线程启动期。
		if (tables.containsKey(table.getId()))
			throw new IllegalStateException("duplicate table id=" + table.getId());
		if (tableNameMap.containsKey(table.getName()))
			throw new IllegalStateException("duplicate table name=" + table.getName());
		TableKey.tables.put(table.getId(), table.getName());
		tables.put(table.getId(), table);
		tableNameMap.put(table.getName(), table);
		db.addTable(table);
		return db;
	}

	// Hot Install 内部使用。
	public void __install_prepare__() {
		hotUpgradeMemoryTables.clear();
		replaceTableRecent.clear();
	}

	public @Nullable Schemas __upgrade_schemas__(@Nullable Schemas schemas) throws Exception {
		var current = this.schemas;
		this.schemasPrevious = null;
		this.schemas = schemas;
		schemasCompatible();
		return current;
	}

	public void __install_alter__() {
		for (var table : replaceTableRecent) {
			if (!(table.getDatabase() instanceof DatabaseRelationalMapping))
				continue;
			if (!table.isRelationalMapping())
				continue;
			logger.info("tryAlter {}", table.getName());
			table.tryAlter();
		}
		replaceTableRecent.clear();
		// 热更路径与启动路径同构：DDL成功后才发布元数据（__upgrade_schemas__只检查不保存）。
		try {
			publishSchemas();
		} catch (Exception e) {
			throw Task.forceThrow(e);
		}
	}

	public ArrayList<HotUpgradeMemoryTable> __get_upgrade_memory_table__() {
		return hotUpgradeMemoryTables;
	}

	// 热更通过此入口替换表，不调用 addTable/removeTable。
	public Database replaceTable(String dbName, Table table) {
		var db = getDatabase(dbName);
		var exist = tables.get(table.getId());
		if (exist == table) {
			replaceTableRecent.add(table);
			TableKey.tables.put(table.getId(), table.getName());
			return db; // 热更回滚可能重复注册同一表。
		}
		var targetDb = exist != null ? exist.getDatabase() : db;
		var previousTableKeyName = TableKey.tables.get(table.getId());
		var previousTableByName = tableNameMap.get(table.getName());
		var previousTargetDbTable = targetDb.getTable(table.getName());
		var previousDbTable = targetDb != db ? db.getTable(table.getName()) : previousTargetDbTable;
		var previousRecentSize = replaceTableRecent.size();

		// 先注册索引再 open：关系映射表构造 Storage 时会回调 db.getTable(name) 读取新表。
		replaceTableRecent.add(table);
		TableKey.tables.put(table.getId(), table.getName());
		tables.put(table.getId(), table);
		tableNameMap.put(table.getName(), table);
		targetDb.replaceTable(table);
		if (targetDb != db)
			db.replaceTable(table);

		try {
			if (exist != null) {
				// 热更开表与首次启动不同，需要传入旧表以继承状态。
				table.open(exist, this);

				if (exist.isMemory() && table.isMemory()) {
					hotUpgradeMemoryTables.add(new HotUpgradeMemoryTable(exist, table));
					// exist.disable() 在升级之后调用。
				} else {
					// 旧表禁用。防止应用保留了旧表引用，还去使用导致错误。
					exist.disable();
				}
			} else if (isStart()) {
				var storage = table.open(this, db, null);
				db.__add_storage__(storage);
			}
		} catch (Throwable failure) {
			if (replaceTableRecent.size() > previousRecentSize)
				replaceTableRecent.remove(replaceTableRecent.size() - 1);
			if (exist != null)
				tables.put(table.getId(), exist);
			else
				tables.remove(table.getId(), table);
			if (previousTableKeyName != null)
				TableKey.tables.replace(table.getId(), table.getName(), previousTableKeyName);
			else
				TableKey.tables.remove(table.getId(), table.getName());
			if (previousTableByName != null)
				tableNameMap.put(table.getName(), previousTableByName);
			else
				tableNameMap.remove(table.getName(), table);
			if (previousTargetDbTable != null) {
				if (!table.isMemory())
					cleanupAfterFailure(failure, () -> targetDb.removeTable(table));
				targetDb.replaceTable(previousTargetDbTable);
			} else {
				cleanupAfterFailure(failure, () -> targetDb.removeTable(table));
			}
			if (targetDb != db) {
				if (previousDbTable != null)
					db.replaceTable(previousDbTable);
				else
					cleanupAfterFailure(failure, () -> db.removeTable(table));
			}
			throw failure;
		}
		return db;
	}

	public StartState getStartState() {
		return startState;
	}

	public void openDynamicTable(String dbName, Table table) {
		lock();
		try {
			// 先注册回放日志工厂再开表，避免注册失败时留下半登记表。
			if (history != null) {
				try {
					history.registerLogs(Collections.singletonList(table));
				} catch (Exception e) {
					throw new RuntimeException(e);
				}
			}
			var previousName = TableKey.tables.get(table.getId());
			var database = addTable(dbName, table);
			try {
				database.openDynamicTable(this, table);
			} catch (Throwable failure) {
				tables.remove(table.getId(), table);
				tableNameMap.remove(table.getName(), table);
				if (previousName != null)
					TableKey.tables.replace(table.getId(), table.getName(), previousName);
				else
					TableKey.tables.remove(table.getId(), table.getName());
				cleanupAfterFailure(failure, () -> database.removeTable(table));
				throw failure;
			}
		} finally {
			unlock();
		}
	}

	public void removeTable(String dbName, Table table) {
		tables.remove(table.getId());
		tableNameMap.remove(table.getName());
		getDatabase(dbName).removeTable(table);
	}

	public @Nullable Table getTable(int id) {
		return tables.get(id);
	}

	public @Nullable Table getTable(String name) {
		return tableNameMap.get(name);
	}

	public @UnmodifiableView Map<String, Table> getTables() {
		return Collections.unmodifiableMap(tableNameMap);
	}

	public Database getDatabase(String name) {
		var db = databases.get(name);
		if (db == null)
			throw new IllegalStateException("database not exist name=" + name);
		return db;
	}

	public AutoKey getAutoKey(String name) {
		//noinspection DataFlowIssue
		return autoKey.getOrAdd(name);
	}

	public Timer getTimer() {
		return Objects.requireNonNull(timer, "Application Timer is not initialized or has been removed");
	}

	public @Nullable Timer getTimerOrNull() {
		return timer;
	}

	public SafeBatch getSafeBatch() {
		return Objects.requireNonNull(safeBatch, "Application SafeBatch is not initialized or has been removed");
	}

	public @Nullable SafeBatch getSafeBatchOrNull() {
		return safeBatch;
	}

	public Zeze.Collections.Queue.Module getQueueModule() {
		return Objects.requireNonNull(queueModule, "Application QueueModule is not initialized or has been removed");
	}


	public DelayRemove getDelayRemove() {
		return Objects.requireNonNull(delayRemove, "Application DelayRemove is not initialized or has been removed");
	}

	public Takeover getTakeover() {
		return Objects.requireNonNull(takeover, "Application Takeover is not initialized or has been removed");
	}

	public @Nullable Takeover getTakeoverOrNull() {
		return takeover;
	}

	public @Nullable IHistory getHistory() {
		return history;
	}

	public Procedure newProcedure(FuncLong action, @Nullable String actionName) {
		return newProcedure(action, actionName, TransactionLevel.Serializable);
	}

	public Procedure newProcedure(FuncLong action, @Nullable String actionName,
								  @Nullable TransactionLevel level) {
		if (!isStart()) {
			throw new IllegalStateException("App Not Start: " + startState
				+ ", action=" + (actionName != null && !actionName.isEmpty() ? actionName : action.getClass()));
		}
		return new Procedure(this, action, actionName, level);
	}

	public ProtocolProcedure newProcedure(FuncLong action, @Nullable String actionName,
										  @Nullable TransactionLevel level, String protocolClassName,
										  Binary protocolRawArgument) {
		if (!isStart()) {
			throw new IllegalStateException("App Not Start: " + startState
				+ ", action=" + (actionName != null && !actionName.isEmpty() ? actionName : action.getClass()));
		}
		return new ProtocolProcedure(this, action, actionName, level, protocolClassName, protocolRawArgument);
	}

	public void endStart() {
		if (delayRemove != null)
			delayRemove.continueJobs();
	}

	// 检查数据库元数据兼容性，暂不发布新元数据。
	void schemasCompatible() throws Exception {
		//noinspection DataFlowIssue
		var defaultDb = getDatabase(conf.getDefaultTableConf().getDatabaseName());
		if (schemas != null) {
			schemas.compile();
			schemas.setAppVersion(conf.getAppVersion());
			var keyOfSchemas = ByteBuffer.Allocate(32);
			var serverId = conf.getServerId();
			keyOfSchemas.WriteString("zeze.Schemas.V4." + serverId);
			//noinspection DataFlowIssue
			var dataVersion = defaultDb.getDirectOperates().getDataWithVersion(keyOfSchemas);
			if (dataVersion != null && dataVersion.data != null) {
				schemasPrevious = new Schemas();
				try {
					schemasPrevious.decode(dataVersion.data);
					schemasPrevious.compile();
				} catch (Exception ex) {
					schemasPrevious = null;
					throw new IllegalStateException("Schemas Implement Changed? serverId=" + serverId, ex);
				}
				if (schemas.getAppVersion() < schemasPrevious.getAppVersion()) {
					logger.info("OldAppVersion Skip.");
					return;
				}

				schemas.checkCompatible(schemasPrevious, this);
			}
			// 元数据须在开表及 tryAlter 成功后发布，避免 DDL 失败重启时误认为升级已完成。
		}
	}

	/**
	 * DDL 全部成功后以 CAS 发布元数据，与 schemasCompatible 共用启动锁。
	 * DDL 成功但发布前崩溃时，tryAlter 根据物理列目录幂等重放。
	 */
	void publishSchemas() throws Exception {
		if (schemas == null)
			return;
		if (schemasPrevious != null && schemas.getAppVersion() < schemasPrevious.getAppVersion())
			return;
		//noinspection DataFlowIssue
		var defaultDb = getDatabase(conf.getDefaultTableConf().getDatabaseName());
		var keyOfSchemas = ByteBuffer.Allocate(32);
		var serverId = conf.getServerId();
		keyOfSchemas.WriteString("zeze.Schemas.V4." + serverId);
		while (true) {
			//noinspection DataFlowIssue
			var dataVersion = defaultDb.getDirectOperates().getDataWithVersion(keyOfSchemas);
			long version = 0;
			if (dataVersion != null && dataVersion.data != null) {
				schemasPrevious = new Schemas();
				try {
					schemasPrevious.decode(dataVersion.data);
					schemasPrevious.compile();
				} catch (Exception ex) {
					schemasPrevious = null;
					throw new IllegalStateException("Schemas Implement Changed? serverId=" + serverId, ex);
				}
				if (schemas.getAppVersion() < schemasPrevious.getAppVersion()) {
					logger.info("OldAppVersion Skip Publish.");
					return;
				}
				// CAS 冲突后重检兼容性，但不能再次备份或 rename 已打开的新表。
				schemas.checkCompatibleForPublish(schemasPrevious, this);
				version = dataVersion.version;
			}
			var newData = ByteBuffer.Allocate(1024);
			schemas.encode(newData);
			var versionRc = defaultDb.getDirectOperates().saveDataWithSameVersion(keyOfSchemas, newData, version);
			if (versionRc == null || versionRc.getValue())
				break;
		}
	}

	private void atomicOpenDatabase() throws Exception {
		//noinspection DataFlowIssue
		var defaultDb = getDatabase(conf.getDefaultTableConf().getDatabaseName());
		while (true) {
			//noinspection DataFlowIssue
			if (!defaultDb.getDirectOperates().tryLock()) {
				logger.info("lock default database fail. sleep and try again...");
				//noinspection BusyWait
				Thread.sleep(1000);
				continue;
			}
			try {
				schemasCompatible();
				if (history != null)
					history.registerLogs(tableNameMap.values());

				for (var e : databases.entrySet()) {
					var timeBegin = System.nanoTime();
					var db = e.getValue();
					db.open(this);
					logger.info("open {} tables from database '{}' ({} ms)",
						db.getTables().size(), e.getKey(), (System.nanoTime() - timeBegin) / 1_000_000);
				}

				// 共享 tHistory 的应用须使用同一 gid 发号名，否则数值重叠会互相覆盖。
				// 更换发号名需显式迁移，或为 tHistory 分库。
				if (history != null)
					history.afterOpen();

				for (var db : getDatabases().values()) {
					if (!(db instanceof DatabaseRelationalMapping))
						continue;
					for (var table : db.getTables()) {
						if (!table.isRelationalMapping())
							continue;
						table.tryAlter();
					}
				}

				publishSchemas();
			} finally {
				defaultDb.getDirectOperates().unlock();
			}
			break;
		}
	}

	public static void logSystemProperties() {
		var rt = Runtime.getRuntime();
		logger.info("java.version={}; os={},{},{}; cpu.cores={}; jvm.heap={}/{}M; file.encoding={}; timezone.offset={}",
			System.getProperty("java.version"), System.getProperty("os.name"), System.getProperty("os.version"),
			System.getProperty("os.arch"), rt.availableProcessors(), rt.totalMemory() >> 20,
			rt.maxMemory() >> 20, Charset.defaultCharset().displayName(), TimeZone.getDefault().getRawOffset());
		logger.info("user.name={}; user.dir={}", System.getProperty("user.name"), System.getProperty("user.dir"));
		logger.info("java.class.path={}", System.getProperty("java.class.path"));
		logger.info("sun.java.command={}", System.getProperty("sun.java.command"));
		int i = 0;
		for (var arg : ManagementFactory.getRuntimeMXBean().getInputArguments())
			logger.info("jvm.arg[{}]={}", i++, arg);
	}

	public static boolean logZezeVersion() throws IOException {
		var logged = false;
		var urls = Application.class.getClassLoader().getResources("zeze.git.properties");
		while (urls.hasMoreElements()) {
			try (var is = urls.nextElement().openStream()) {
				var p = new Properties();
				p.load(is);
				logger.log(logged ? Level.WARN : Level.INFO, "Zeze Version={}, BuildTime={}, Rev={}",
					p.getProperty("git.build.version"), p.getProperty("git.build.time"),
					p.getProperty("git.commit.id.full"));
				logged = true;
			}
		}
		return logged;
	}

	private void addShutdownHook() {
		ShutdownHook.add(this, () -> {
			logger.info("zeze({}) ShutdownHook begin", this.projectName);
			stop();
			logger.info("zeze({}) ShutdownHook end", this.projectName);
		});
	}

	public void start() throws Exception {
		lock();
		try {
			if (startState == StartState.eStarted)
				return;
			if (startState != StartState.eUninitialized)
				throw new IllegalStateException("Application '" + getProjectName()
					+ " startState = " + startState);
			startState = StartState.eStarting;

			startUnderLock();

			startState = StartState.eStarted;

			if (null != instances.putIfAbsent(getProjectName(), this))
				logger.warn("Project {} already exists", getProjectName());
		} finally {
			unlock();
		}
	}

	public void stop() throws Exception {
		lock();
		try {
			if (startState == StartState.eUninitialized || startState == StartState.eStopped)
				return;

			startState = StartState.eStopping;

			stopUnderLock();

			startState = StartState.eStopped;
		} finally {
			unlock();
		}
	}

	// 由公开 start/stop 持有 Application 的原有锁后调用。
	private void startUnderLock() throws Exception {
		logSystemProperties();
		logZezeVersion();
		var serverId = conf.getServerId();
		logger.info("Start ServerId={}", serverId);

		var hasDatabase = !isNoDatabase();
		if (hasDatabase) {
			if ("true".equalsIgnoreCase(System.getProperty(Daemon.propertyNameClearInUse))) {
				conf.clearInUse(databases);
			}

			for (var db : databases.values()) {
				//noinspection DataFlowIssue
				db.getDirectOperates().setInUse(serverId, conf.getGlobalCacheManagerHostNameOrAddress());
				databasesInUse.add(db);
			}
			addShutdownHook();

			localCache.open(this, serverId);
		} else {
			addShutdownHook();
		}

		var serviceManagerConf = conf.getServiceConf(Agent.defaultServiceName);
		// Raft SM 地址来自 raftXml，无 ServiceConf 节点时也需启动。
		var isRaftServiceManager = "raft".equals(conf.getServiceManager());
		if ((serviceManagerConf != null || isRaftServiceManager) && serviceManager != null) {
			serviceManager.start();
			try {
				serviceManager.waitReady();
			} catch (Exception ex) {
				// Raft 首次选主可能导致等待失败，重试一次。
				//noinspection ConstantValue
				if (ex instanceof InterruptedException)
					Thread.currentThread().interrupt();
				serviceManager.waitReady();
			}
		}

		// Dbh2管理器随启动闭环（"7788集成形态不监听"立案）：本地提交的CommitService必须真实监听
		// ——悬挂事务探测端点=Zeze.Dbh2.Commit Acceptor（commitServiceAcceptor发布），无人监听时
		// 桶侧探测永远失败→事务锁死桶键→后来者撞键；redo守护同随start启用。远程提交分支只连接
		// CommitServer不绑定。stop()早已对称调用manager.stop()。manager非null ⇔ 配置了Dbh2库。
		if (dbh2AgentManager != null)
			dbh2AgentManager.start();

		if (hasDatabase) {
			atomicOpenDatabase();

			var hosts = Str.trim(conf.getGlobalCacheManagerHostNameOrAddress().split(";"));
			if (hosts.length > 0) {
				var isRaft = hosts[0].endsWith(".xml");
				if (!isRaft) {
					var impl = new GlobalAgent(this, hosts, conf.getGlobalCacheManagerPort());
					globalAgent = impl;
					achillesHeelDaemon = new AchillesHeelDaemon(this, impl.getAgents());
					impl.start();
				} else {
					var impl = new GlobalCacheManagerWithRaftAgent(this, hosts);
					globalAgent = impl;
					achillesHeelDaemon = new AchillesHeelDaemon(this, impl.getAgents());
					impl.start();
				}
			}

			deadlockBreaker = new DeadlockBreaker(this);
			checkpoint = new Checkpoint(this, conf.getCheckpointMode(), databases.values(), serverId);
			//noinspection DataFlowIssue
			checkpoint.start(conf.getCheckpointPeriod());

			// 守护线程须在 checkpoint 就绪后启动。
			if (achillesHeelDaemon != null)
				achillesHeelDaemon.start();

			// 启动返回前完成租约 claim，依赖已建立的 Checkpoint；后续组件通过 addScope 注册。
			if (takeover != null) {
				//noinspection DataFlowIssue
				takeover.start();
				// SM Suspect提示→tryTransfer（非raft与raft版SM都会在会话断开时广播Suspect）。
				if (serviceManager != null)
					//noinspection DataFlowIssue
					serviceManager.setOnSuspect(deadServerId -> takeover.tryTransfer(deadServerId));
			}

			//noinspection DataFlowIssue
			delayRemove.start();
			if (timer != null)
				timer.loadCustomClassAnd();
			if (deadlockBreaker != null)
				deadlockBreaker.start();
			if (onz != null)
				onz.start();
		}
	}

	private void stopUnderLock() {
		// 只移除本实例的注册，避免删除同名实例。
		instances.remove(getProjectName(), this);

		if (null != checkpointFuture) {
			// 检查点任务失败不能中断后续停机步骤。
			try {
				checkpointFuture.get();
			} catch (java.util.concurrent.ExecutionException e) {
				logger.error("checkpoint task exception (ignored to complete stop)", e);
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			}
			checkpointFuture = null;
		}
		if (onz != null) {
			stopStep("onz.stop", onz::stop);
			onz = null;
		}

		if (deadlockBreaker != null) {
			stopStep("deadlockBreaker.shutdown", deadlockBreaker::shutdown);
			deadlockBreaker = null;
		}

		ShutdownHook.remove(this);
		logger.info("Stop ServerId={}", conf.getServerId());

		if (takeover != null) // 早期释放：正常关闭刷新租约宽限期，一个TTL后可被接管（缩容；数据库尚未关闭）。
			//noinspection DataFlowIssue
			stopStep("takeover.release", takeover::release);

		if (achillesHeelDaemon != null) {
			stopStep("achillesHeelDaemon.stopAndJoin", achillesHeelDaemon::stopAndJoin);
			achillesHeelDaemon = null;
		}

		// 先停止事务生产组件并等待在途任务，再关提交闸门、完成终检点，最后撤销 GCM 权限。
		// 组件停机事务仍可能需要 GCM；关闸后到达的提交返回 Closed。
		// 最终 flush 必须先于 NormalClose，否则旧脏值可能覆盖其他进程接管后的新值。
		if (delayRemove != null) {
			stopStep("delayRemove.stop", delayRemove::stop);
			delayRemove = null;
		}

		if (safeBatch != null) {
			stopStep("safeBatch.stop", safeBatch::stop);
			safeBatch = null;
		}
		if (timer != null) {
			stopStep("timer.stop", timer::stop);
			timer = null;
		}

		if (globalAgent != null && checkpoint != null)
			stopStep("checkpointRun before close commit gate", this::checkpointRun);

		if (checkpoint != null) {
			// 先撤销入口，再由 stopAndJoin 关闸排空提交，避免终检点遗漏迟到的脏集。
			var cp = Objects.requireNonNull(checkpoint);
			checkpoint = null;
			stopStep("checkpoint.stopAndJoin", () -> {
				try {
					cp.stopAndJoin();
				} catch (Throwable ex) {
					// 中断后线程可能仍在 flush，关库前有界重等。
					logger.error("checkpoint stopAndJoin interrupted/failed, bounded re-join before close", ex);
					cp.joinIgnoreInterrupt(CHECKPOINT_DRAIN_TIMEOUT_MILLIS);
				}
			});
			// 同时等待业务线程和 Reduce 发起的 flush，避免关库与 RocksDB 事务并发。
			if (!cp.waitNoActiveFlush(CHECKPOINT_DRAIN_TIMEOUT_MILLIS))
				logger.error("checkpoint active flush not drained in {}ms, continue to close databases "
					+ "(risk of close racing in-flight flush)", CHECKPOINT_DRAIN_TIMEOUT_MILLIS);
		}

		if (flushWhenReduceTimerTask != null) {
			var task = flushWhenReduceTimerTask;
			stopStep("flushWhenReduceTimerTask.cancel", () -> task.cancel(false));
			flushWhenReduceTimerTask = null;
		}

		if (globalAgent != null) {
			var ga = globalAgent;
			stopStep("globalAgent.stop", ga::stop);
			// Releaser 线程由 GCM 触发，关库前也需有界等待其 flush 结束。
			stopStep("globalAgent.awaitReleaser", () -> ga.awaitReleaser(CHECKPOINT_DRAIN_TIMEOUT_MILLIS));
			globalAgent = null;
		}

		if (history != null) {
			var plugin = history;
			stopStep("history.close", plugin::close);
			history = null;
		}

		localCache.closeForStop();

		if (serviceManager != null)
			stopStep("serviceManager.close", serviceManager::close);

		if (queueModule != null) {
			var qm = queueModule;
			stopStep("queueModule.UnRegisterZezeTables", () -> qm.UnRegisterZezeTables(this));
			queueModule = null;
		}
		if (autoKey != null) {
			stopStep("autoKey.UnRegister", autoKey::UnRegister);
			autoKey = null;
		}
		if (takeover != null) {
			var tk = Objects.requireNonNull(takeover);
			stopStep("takeover.UnRegisterZezeTables", () -> tk.UnRegisterZezeTables(this));
			takeover = null;
		}
		clearOwnedDatabaseRegistrations();

		for (var e : databases.entrySet())
			stopStep("db.close '" + e.getKey() + '\'', e.getValue()::close);

		if (dbh2AgentManager != null) {
			stopStep("dbh2AgentManager.stop", dbh2AgentManager::stop);
			dbh2AgentManager = null;
		}
	}

	private void clearOwnedDatabaseRegistrations() {
		for (int i = databasesInUse.size() - 1; i >= 0; --i) {
			var database = databasesInUse.get(i);
			stopStep("clearInUse '" + database.getConf().getName() + '\'', () -> {
				//noinspection DataFlowIssue
				database.getDirectOperates().clearInUse(
					conf.getServerId(), conf.getGlobalCacheManagerHostNameOrAddress());
			});
		}
		databasesInUse.clear();
	}

	// 隔离各停机步骤的异常，保证剩余资源仍能释放。
	static void stopStep(String name, Action0 action) {
		try {
			action.run();
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			logger.error("stop step '{}' interrupted, continue", name, e);
		} catch (Throwable e) {
			logger.error("stop step '{}' exception, continue", name, e);
		}
	}

	public void checkpointRun() {
		// 单次快照读取，避免 stop 在判空与调用间清空字段。
		var cp = checkpoint;
		if (cp != null)
			cp.runOnce();
	}

	private volatile boolean fatalStopping;

	public boolean isFatalStopping() {
		return fatalStopping;
	}

	/**
	 * 在现有 GCM 权限预算内停止提交、排空并重试保存；watchdog 须先于锁与 I/O 启动。
	 * 截止时间固定，不随心跳续期；不发送 NormalClose，由 Reduce 或 GCM 超时回收权限。
	 */
	public static void haltAfterCheckpoint(Application zeze, int exitCode) {
		long remaining = HALT_DEADLINE_MS;
		var global = zeze.getGlobalAgent();
		if (global != null) {
			long now = System.currentTimeMillis();
			for (int i = 0; i < global.getAgentCount(); i++)
				remaining = Math.min(remaining, global.getAgent(i).getFatalSaveDeadline() - now);
		}
		zeze.fatalStopping = true;
		// 权限已过期时不得继续保存。
		if (remaining <= 0) {
			Runtime.getRuntime().halt(exitCode);
			return;
		}
		haltWithDeadline(remaining, () -> {
			var cp = zeze.getCheckpoint();
			if (cp != null)
				cp.saveBeforeFatalExit();
			try {
				LogManager.shutdown();
			} catch (Throwable ignored) {
			}
		}, () -> Runtime.getRuntime().halt(exitCode));
	}

	/** 最终保存上限，包含在现有权限释放预算内。 */
	public static final long HALT_DEADLINE_MS = 30_000;

	/** 主路径尽力保存，独立 watchdog 保证按时终止；CAS 保证终止动作至多执行一次。 */
	static void haltWithDeadline(long deadlineMs, Action0 bestEffort, Runnable halt) {
		var halted = new java.util.concurrent.atomic.AtomicBoolean();
		Runnable haltOnce = () -> {
			if (halted.compareAndSet(false, true))
				halt.run();
		};
		var watchdog = Thread.ofPlatform().daemon(true).name("HaltDeadlineWatchdog").unstarted(() -> {
			try {
				Thread.sleep(deadlineMs);
			} catch (InterruptedException ignored) {
				return; // 主路径已先完成终止动作
			}
			haltOnce.run();
		});
		watchdog.start();
		try {
			bestEffort.run();
		} catch (Throwable ex) {
			try {
				logger.fatal("haltWithDeadline best effort fail", ex);
			} catch (Throwable ignored) {
			}
		}
		haltOnce.run();
		watchdog.interrupt(); // 主路径已完成终止动作。
	}

	public void checkpointRunThread() {
		if (checkpoint == null)
			return;
		lock();
		try {
			var f = checkpointFuture;
			// 任务可能在 submit 返回前完成，不能由任务清空字段。
			// 持 Application 锁检查 Future 是否完成，判断是否可提交下一轮。
			if (startState == StartState.eStarted && (f == null || f.isDone()))
				//noinspection DataFlowIssue
				checkpointFuture = TaskSpec.ofAction(checkpoint::runOnce)
					.name("CheckpointRunThread").submitNow();
		} finally {
			unlock();
		}
	}

	/** @deprecated 请直接使用 {@link Task#getOneByOne()}。 */
	@Deprecated
	@SuppressWarnings("MethodMayBeStatic")
	public TaskOneByOneByKey getTaskOneByOneByKey() {
		return Task.getOneByOne();
	}

	/**
	 * @deprecated 请使用 {@code TaskSpec.ofProcedure(newProcedure(func, actionName)).executeOneByOne(oneByOneKey)}。
	 */
	@Deprecated
	public void runTaskOneByOneByKey(Object oneByOneKey, @Nullable String actionName, FuncLong func) {
		TaskSpec.ofProcedure(newProcedure(func, actionName))
			.dispatchMode(DispatchMode.Normal).executeOneByOne(oneByOneKey);
	}

	/**
	 * @deprecated 请使用 {@code TaskSpec.ofProcedure(newProcedure(func, actionName)).executeOneByOne(oneByOneKey)}。
	 */
	@Deprecated
	public void runTaskOneByOneByKey(int oneByOneKey, @Nullable String actionName, FuncLong func) {
		TaskSpec.ofProcedure(newProcedure(func, actionName))
			.dispatchMode(DispatchMode.Normal).executeOneByOne(oneByOneKey);
	}

	/**
	 * @deprecated 请使用 {@code TaskSpec.ofProcedure(newProcedure(func, actionName)).executeOneByOne(oneByOneKey)}。
	 */
	@Deprecated
	public void runTaskOneByOneByKey(long oneByOneKey, @Nullable String actionName, FuncLong func) {
		TaskSpec.ofProcedure(newProcedure(func, actionName))
			.dispatchMode(DispatchMode.Normal).executeOneByOne(oneByOneKey);
	}
}
