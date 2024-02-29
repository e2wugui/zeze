package Zeze;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;
import Zeze.Arch.ProviderApp;
import Zeze.Arch.RedirectBase;
import Zeze.Component.Auth;
import Zeze.Component.AutoKey;
import Zeze.Component.AutoKeyOld;
import Zeze.Component.DelayRemove;
import Zeze.Component.Timer;
import Zeze.Dbh2.Dbh2AgentManager;
import Zeze.Hot.HotHandle;
import Zeze.Hot.HotManager;
import Zeze.Hot.HotUpgradeMemoryTable;
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
import Zeze.Transaction.DatabaseMySql;
import Zeze.Transaction.DatabaseRocksDb;
import Zeze.Transaction.DispatchMode;
import Zeze.Transaction.GlobalAgent;
import Zeze.Transaction.IGlobalAgent;
import Zeze.Transaction.Locks;
import Zeze.Transaction.Procedure;
import Zeze.Transaction.ProcedureLockWatcher;
import Zeze.Transaction.Table;
import Zeze.Transaction.TableKey;
import Zeze.Transaction.TransactionLevel;
import Zeze.Util.DeadlockBreaker;
import Zeze.Util.EventDispatcher;
import Zeze.Util.FuncLong;
import Zeze.Util.LongConcurrentHashMap;
import Zeze.Util.PerfCounter;
import Zeze.Util.ShutdownHook;
import Zeze.Util.Str;
import Zeze.Util.Task;
import Zeze.Util.TaskOneByOneByKey;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class Application {
	static final Logger logger = LogManager.getLogger(Application.class);

	private final @NotNull String projectName;
	private final @NotNull Config conf;
	private final HashMap<String, Database> databases = new HashMap<>();
	private final LongConcurrentHashMap<Table> tables = new LongConcurrentHashMap<>();
	private final ConcurrentHashMap<String, Table> tableNameMap = new ConcurrentHashMap<>();
	private final TaskOneByOneByKey taskOneByOneByKey = new TaskOneByOneByKey();
	private final Locks locks = new Locks();
	private final AbstractAgent serviceManager;
	private @Nullable AutoKey.Module autoKey;
	@Deprecated // 暂时保留
	private @Nullable AutoKeyOld.Module autoKeyOld;
	private Timer timer;
	private Zeze.Collections.Queue.Module queueModule;
	private DelayRemove delayRemove;
	private IGlobalAgent globalAgent;
	private AchillesHeelDaemon achillesHeelDaemon;
	private DeadlockBreaker deadlockBreaker;
	private Checkpoint checkpoint;
	private Future<?> flushWhenReduceTimerTask;
	private Schemas schemas;
	private Schemas schemasPrevious; // maybe null
	private final ProcedureLockWatcher procedureLockWatcher;

	public ProcedureLockWatcher getProcedureLockWatcher() {
		return procedureLockWatcher;
	}

	public enum StartState {
		eStopped,
		eStartingOrStopping,
		eStarted,
	}

	private StartState startState = StartState.eStopped;
	public RedirectBase redirect;

	private Auth auth;
	private Onz onz;

	private final HotHandle<EventDispatcher.EventHandle> hotHandle = new HotHandle<>();

	public HotHandle<EventDispatcher.EventHandle> getHotHandle() {
		return hotHandle;
	}

	public Onz getOnz() {
		return onz;
	}

	public void enableAuth() {
		if (isStart())
			throw new IllegalStateException("must enable auth before start.");

		if (null != auth)
			throw new IllegalStateException("auth has enabled.");

		auth = new Auth(this);
	}

	public Auth getAuth() {
		return auth;
	}

	/**
	 * 本地Rocks缓存数据库虽然也用了Database接口，但它不给用户提供事务操作的表。
	 * 1. 不需要加入到Databases里面。
	 * 2. 不需要在里面注册表(Database.AddTable)。
	 * 3. Flush的时候特殊处理。see Checkpoint。
	 */
	private DatabaseRocksDb LocalRocksCacheDb;

	private Dbh2AgentManager dbh2AgentManager;
	private HotManager hotManager;

	// verifyCallerNotHot(jdk.internal.reflect.Reflection.getCallerClass());
	// caller必须外部调用得到。
	public void verifyCallerCold(Class<?> caller) {
		var callerCl = caller.getClassLoader();
		// 只限制我们自己的HotModule，其他都允许。
		if (null != hotManager && HotManager.isHotModule(callerCl))
			throw new IllegalStateException("caller must not hot.");
	}

	public void setHotManager(HotManager value) {
		hotManager = value;
	}

	public HotManager getHotManager() {
		return hotManager;
	}

	public Dbh2AgentManager getDbh2AgentManager() {
		return dbh2AgentManager;
	}

	public Dbh2AgentManager tryNewDbh2AgentManager() throws Exception {
		if (null == dbh2AgentManager)
			dbh2AgentManager = new Dbh2AgentManager(serviceManager, conf);
		return dbh2AgentManager;
	}

	public Application(@NotNull String solutionName) throws Exception {
		this(solutionName, null);
	}

	public static AbstractAgent createServiceManager(Config conf, String raftSessionNamePrefix) throws Exception {
		switch (conf.getServiceManager()) {
		case "raft":
			if (conf.getServiceManagerConf().getSessionName().isEmpty()) {
				conf.getServiceManagerConf().setSessionName(raftSessionNamePrefix + "#" + conf.getServerId());
			}
			return new ServiceManagerAgentWithRaft(conf);

		case "disable":
			return null;

		default:
			return new Agent(conf);
		}
	}

	@SuppressWarnings("deprecation")
	public Application(@NotNull String projectName, @Nullable Config config) throws Exception {
		this.projectName = projectName;
		conf = config != null ? config : Config.load();
		if (conf.getServerId() > 0x3FFF) // 16383 encoded size = 2 bytes
			throw new IllegalStateException("serverId too big. > 16383.");
		procedureLockWatcher = new ProcedureLockWatcher(this);

		// Start Thread Pool
		Task.tryInitThreadPool(this); // 确保Task线程池已经建立,如需定制,在createZeze前先手动初始化

		serviceManager = createServiceManager(conf, projectName); // 必须在createDatabase之前初始化。里面的Dbh2需要用到serviceManager
		conf.createDatabase(this, databases);
		PerfCounter.instance.tryStartScheduledLog();

		if (!isNoDatabase()) {
			// 自动初始化的组件。
			autoKey = new AutoKey.Module(this);
			autoKeyOld = new AutoKeyOld.Module(this);
			queueModule = new Zeze.Collections.Queue.Module(this);
			delayRemove = new DelayRemove(this);

			onz = new Onz(this);
		}
	}

	private AppBase appBase;
	private ProviderApp providerApp; // maybe null

	public AppBase getAppBase() {
		return appBase;
	}

	public ProviderApp getProviderApp() {
		return providerApp;
	}

	public void setProviderApp(ProviderApp providerApp) {
		this.providerApp = providerApp;
	}

	public synchronized void initialize(@NotNull AppBase app) {
		appBase = app;
		if (timer == null && !isNoDatabase() && redirect != null)
			timer = Timer.create(app);
	}

	public boolean isNoDatabase() {
		return conf.isNoDatabase() || conf.getServerId() < 0;
	}

	public HashMap<String, Database> getDatabases() {
		return databases;
	}

	public @NotNull Config getConfig() {
		return conf;
	}

	public boolean isStart() {
		return startState == StartState.eStarted;
	}

	public AbstractAgent getServiceManager() {
		return serviceManager;
	}

	public IGlobalAgent getGlobalAgent() {
		return globalAgent;
	}

	public Checkpoint getCheckpoint() {
		return checkpoint;
	}

	/*
	public synchronized void setCheckpoint(Checkpoint value) {
		if (value == null)
			throw new NullPointerException();
		if (IsStart)
			throw new IllegalStateException("Checkpoint only can setup before start.");
		_checkpoint = value;
	}
	*/

	public @NotNull Locks getLocks() {
		return locks;
	}

	public Schemas getSchemas() {
		return schemas;
	}

	public Schemas getSchemasPrevious() {
		return schemasPrevious;
	}

	public void setSchemas(@NotNull Schemas value) {
		schemas = value;
	}

	public @NotNull String getProjectName() {
		return projectName;
	}

	public AchillesHeelDaemon getAchillesHeelDaemon() {
		return achillesHeelDaemon;
	}

	public DatabaseRocksDb getLocalRocksCacheDb() {
		return LocalRocksCacheDb;
	}

	public @NotNull Database addTable(@NotNull String dbName, @NotNull Table table) {
		TableKey.tables.put(table.getId(), table.getName());
		var db = getDatabase(dbName);
		if (tables.putIfAbsent(table.getId(), table) != null)
			throw new IllegalStateException("duplicate table id=" + table.getId());
		if (tableNameMap.putIfAbsent(table.getName(), table) != null)
			throw new IllegalStateException("duplicate table name=" + table.getName());
		db.addTable(table);
		return db;
	}

	private final ArrayList<Table> replaceTableRecent = new ArrayList<>();
	private final ArrayList<HotUpgradeMemoryTable> hotUpgradeMemoryTables = new ArrayList<>();

	// Hot Install 内部使用。
	public void __install_prepare__() {
		hotUpgradeMemoryTables.clear();
		replaceTableRecent.clear();
	}

	public Schemas __upgrade_schemas__(Schemas schemas) {
		var current = this.schemas;
		this.schemasPrevious = null;
		this.schemas = schemas;
		schemasCompatible();
		return current;
	}

	public void __install_alter__() {
		for (var table : replaceTableRecent) {
			if (!table.isRelationalMapping())
				continue;
			table.tryAlter();
		}
		replaceTableRecent.clear();
	}

	public ArrayList<HotUpgradeMemoryTable> __get_upgrade_memory_table__() {
		return hotUpgradeMemoryTables;
	}

	// 用于热更的时候替换Table.
	// 热更不会调用addTable,removeTable。
	public @NotNull Database replaceTable(@NotNull String dbName, @NotNull Table table) {
		replaceTableRecent.add(table);
		TableKey.tables.put(table.getId(), table.getName()); // always put
		var exist = tables.put(table.getId(), table);
		var db = getDatabase(dbName);
		if (exist == table)
			return db; // 热更回滚导致反复重新注册，如果存在的表就是自己，不再执行后面的操作。
		if (null != exist) {
			// 1. exist.isMemory() || table.isMemory()
			// 内存表配置发生改变，不会继承数据。【需要再次确认一下能不能重用这个处理流程，大概可以。】
			// 2. !exist.isMemory() && !table.isMemory()
			// 都是持久表，不需要继承数据。这个open跟第一次启动不一样，有一些状态从旧表得到。
			// 3. exist.isMemory() && table.isMemory()
			// 也需要走这个初始化。【需要再次确认一下。】
			table.open(exist, this);

			if (exist.isMemory() && table.isMemory()) {
				// 内存表特殊处理。
				//logger.info("+++++++++++++++++++++++++++++++++++ UpgradeMemory " + table.getName());
				hotUpgradeMemoryTables.add(new HotUpgradeMemoryTable(exist, table));
				// exist.disable() 在升级之后调用。
			} else {
				// 旧表禁用。防止应用保留了旧表引用，还去使用导致错误。
				exist.disable();
			}
		} else if (isStart()) {
			// new table
			var storage = table.open(this, db, null);
			db.__add_storage__(storage);
		}
		tableNameMap.put(table.getName(), table); // always put, 操作在tables阶段完成。
		db.replaceTable(table);
		return db;
	}

	public StartState getStartState() {
		return startState;
	}

	public synchronized void openDynamicTable(@NotNull String dbName, @NotNull Table table) {
		addTable(dbName, table).openDynamicTable(this, table);
	}

	public void removeTable(@NotNull String dbName, @NotNull Table table) {
		tables.remove(table.getId());
		tableNameMap.remove(table.getName());
		getDatabase(dbName).removeTable(table);
	}

	public @Nullable Table getTable(int id) {
		return tables.get(id);
	}

	public @Nullable Table getTable(@NotNull String name) {
		return tableNameMap.get(name);
	}

	public @NotNull Map<String, Table> getTables() {
		return Collections.unmodifiableMap(tableNameMap);
	}

	public Database getDatabase(@NotNull String name) {
		var db = databases.get(name);
		if (db == null)
			throw new IllegalStateException("database not exist name=" + name);
		return db;
	}

	public @NotNull AutoKey getAutoKey(@NotNull String name) {
		//noinspection DataFlowIssue
		return autoKey.getOrAdd(name);
	}

	@Deprecated // 暂时保留
	public @NotNull AutoKeyOld getAutoKeyOld(@NotNull String name) {
		//noinspection DataFlowIssue
		return autoKeyOld.getOrAdd(name);
	}

	public Timer getTimer() {
		return timer;
	}

	public Zeze.Collections.Queue.Module getQueueModule() {
		return queueModule;
	}

	public DelayRemove getDelayRemove() {
		return delayRemove;
	}

	public @NotNull Procedure newProcedure(@NotNull FuncLong action, @Nullable String actionName) {
		return newProcedure(action, actionName, TransactionLevel.Serializable, null);
	}

	public @NotNull Procedure newProcedure(@NotNull FuncLong action, @Nullable String actionName,
										   @Nullable TransactionLevel level, @Nullable Object userState) {
		if (!isStart()) {
			throw new IllegalStateException("App Not Start: " + startState
					+ ", action=" + (actionName != null ? actionName : action.getClass()));
		}
		return new Procedure(this, action, actionName, level, userState);
	}

	public static void deleteDirectory(@NotNull File directoryToBeDeleted) throws IOException, InterruptedException {
		var allContents = directoryToBeDeleted.listFiles();
		if (allContents != null) {
			for (File file : allContents)
				deleteDirectory(file);
		}
		for (int i = 0; directoryToBeDeleted.exists(); ) {
			//noinspection ResultOfMethodCallIgnored
			directoryToBeDeleted.delete();
			if (!directoryToBeDeleted.exists())
				break;
			if (++i >= 100)
				throw new IOException("delete failed: " + directoryToBeDeleted.getAbsolutePath());
			//noinspection BusyWait
			Thread.sleep(100);
		}
	}

	// 先把要删的目录改名再删除,会更安全一些,降低并发访问目录中文件的可能性
	public static void renameAndDeleteDirectory(@NotNull File directoryToBeDeleted)
			throws IOException, InterruptedException {
		if (directoryToBeDeleted.isDirectory()) {
			var path = directoryToBeDeleted.getAbsolutePath();
			var newFile = new File(path + ".del");
			for (int i = 0; !directoryToBeDeleted.renameTo(newFile); newFile = new File(path + ".del" + i)) {
				if (++i >= 10000)
					throw new IOException("rename failed: " + path);
			}
			directoryToBeDeleted = newFile;
		}
		deleteDirectory(directoryToBeDeleted);
	}

	public void endStart() {
		delayRemove.continueJobs();
	}

	/*
	static byte[] debugDataVersion;
	static void checkAndSet(ByteBuffer cur) {
		if (debugDataVersion == null) {
			debugDataVersion = cur.Copy();
			return;
		}
		if (!Arrays.equals(debugDataVersion, cur.Copy())) {
			System.out.println("DataVersion.Data Changed!");
		}
	}
	*/

	// 数据库Meta兼容检查，返回旧的Schemas。
	private void schemasCompatible() {
		var defaultDb = getDatabase(conf.getDefaultTableConf().getDatabaseName());
		if (schemas != null) {
			schemas.compile();
			schemas.setAppVersion(conf.getAppVersion());
			var keyOfSchemas = ByteBuffer.Allocate(32);
			var serverId = conf.getServerId();
			keyOfSchemas.WriteString("zeze.Schemas.V3." + serverId);
			while (true) {
				var dataVersion = defaultDb.getDirectOperates().getDataWithVersion(keyOfSchemas);
				long version = 0;
				if (dataVersion != null && dataVersion.data != null) {
					//checkAndSet(dataVersion.data);
					schemasPrevious = new Schemas();
					try {
						schemasPrevious.decode(dataVersion.data);
						schemasPrevious.compile();
					} catch (Exception ex) {
						schemasPrevious = null;
						throw new IllegalStateException("Schemas Implement Changed? serverId=" + serverId, ex);
					}
					if (schemas.getAppVersion() < schemasPrevious.getAppVersion())
						return; // 当前的发布版本小于先前时，不做任何操作，直接返回。

					schemas.checkCompatible(schemasPrevious, this);
					version = dataVersion.version;
				}
				// schemasPrevious maybe null
				schemas.buildRelationalTables(this, schemasPrevious);

				var newData = ByteBuffer.Allocate(1024);
				schemas.encode(newData);
				var versionRc = defaultDb.getDirectOperates().saveDataWithSameVersion(keyOfSchemas, newData, version);
				if (versionRc.getValue())
					break;
			}
		}
	}

	private void atomicOpenDatabase() throws InterruptedException {
		var defaultDb = getDatabase(conf.getDefaultTableConf().getDatabaseName());
		while (true) {
			if (!defaultDb.getDirectOperates().tryLock()) {
				logger.info("lock default database fail. sleep and try again...");
				// alter 可能很慢，这里多睡一下也行，但是为了兼容不是关系表，选一个合适的值吧。
				//noinspection BusyWait
				Thread.sleep(1000);
				continue;
			}
			try {
				// 由于有了flag，这里实际上就不再会并发了。当然原有的支持并发的代码可以保留。
				schemasCompatible();

				// Open Databases
				for (var e : databases.entrySet()) {
					var timeBegin = System.nanoTime();
					var db = e.getValue();
					db.open(this);
					logger.info("open {} tables from database '{}' ({} ms)",
							db.getTables().size(), e.getKey(), (System.nanoTime() - timeBegin) / 1_000_000);
				}

				for (var db : getDatabases().values()) {
					if (!(db instanceof DatabaseMySql))
						continue;
					for (var table : db.getTables()) {
						if (!table.isRelationalMapping())
							continue;
						table.tryAlter();
					}
				}
			} finally {
				defaultDb.getDirectOperates().unlock();
			}
			break; // done
		}
	}

	public static boolean logZezeVersion() throws IOException {
		var logged = false;
		var urls = Application.class.getClassLoader().getResources("zeze.git.properties");
		while (urls.hasMoreElements()) {
			try (var is = urls.nextElement().openStream()) {
				var p = new Properties();
				p.load(is);
				logger.info("Zeze Version={}, BuildTime={}, Rev={}", p.getProperty("git.build.version"),
						p.getProperty("git.build.time"), p.getProperty("git.commit.id.full"));
				logged = true;
			}
		}
		return logged;
	}

	public synchronized void start() throws Exception {
		if (startState == StartState.eStarted)
			return;
		if (startState == StartState.eStartingOrStopping)
			stop();
		startState = StartState.eStartingOrStopping;
		ShutdownHook.add(this, () -> {
			logger.info("zeze({}) ShutdownHook begin", this.projectName);
			stop();
			logger.info("zeze({}) ShutdownHook end", this.projectName);
		});

		logZezeVersion();

		var serverId = conf.getServerId();
		logger.info("Start ServerId={}", serverId);

		var noDatabase = isNoDatabase();
		if (!noDatabase) {
			if ("true".equalsIgnoreCase(System.getProperty(Daemon.propertyNameClearInUse))) {
				conf.clearInUseAndIAmSureAppStopped(this, databases);
				//var defaultDb = getDatabase(conf.getDefaultTableConf().getDatabaseName());
				//defaultDb.getDirectOperates().unlock();
			}

			// Set Database InUse
			for (var db : databases.values())
				db.getDirectOperates().setInUse(serverId, conf.getGlobalCacheManagerHostNameOrAddress());

			// Open RocksCache
			var dbConf = new Config.DatabaseConf();
			dbConf.setName("zeze_cache_" + serverId);
			dbConf.setDatabaseUrl(dbConf.getName());
			deleteDirectory(new File(dbConf.getDatabaseUrl()));
			dbConf.setDatabaseType(Config.DbType.RocksDb);
			LocalRocksCacheDb = new DatabaseRocksDb(this, dbConf);
			LocalRocksCacheDb.open(this);
		}

		// Start ServiceManager
		var serviceManagerConf = conf.getServiceConf(Agent.defaultServiceName);
		if (serviceManagerConf != null && serviceManager != null) {
			serviceManager.start();
			try {
				serviceManager.waitReady();
			} catch (Exception ignored) {
				// raft 版第一次等待由于选择leader原因肯定会失败一次。
				serviceManager.waitReady();
			}
		}

		if (!noDatabase) {
			atomicOpenDatabase();

			// Open Global
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

			this.deadlockBreaker = new DeadlockBreaker(this);
			// Checkpoint
			checkpoint = new Checkpoint(this, conf.getCheckpointMode(), databases.values(), serverId);
			checkpoint.start(conf.getCheckpointPeriod()); // 定时模式可以和其他模式混用。

			// start last
			if (null != achillesHeelDaemon)
				achillesHeelDaemon.start();
			startState = StartState.eStarted;

			delayRemove.start();
			if (auth != null)
				auth.start();
			if (timer != null) {
				timer.loadCustomClassAnd();
			}
			if (null != deadlockBreaker)
				deadlockBreaker.start();
			if (null != onz)
				onz.start();
		} else
			startState = StartState.eStarted;
	}

	public synchronized void stop() throws Exception {
		if (null != onz) {
			onz.stop();
			onz = null;
		}

		if (null != deadlockBreaker) {
			this.deadlockBreaker.shutdown();
			deadlockBreaker = null;
		}

		if (startState == StartState.eStopped)
			return;
		startState = StartState.eStartingOrStopping;
		ShutdownHook.remove(this);
		logger.info("Stop ServerId={}", conf.getServerId());

		if (achillesHeelDaemon != null) {
			achillesHeelDaemon.stopAndJoin();
			achillesHeelDaemon = null;
		}

		if (globalAgent != null) {
			globalAgent.close();
			globalAgent = null;
		}
		if (flushWhenReduceTimerTask != null) {
			flushWhenReduceTimerTask.cancel(false);
			flushWhenReduceTimerTask = null;
		}

		if (checkpoint != null) {
			checkpoint.stopAndJoin();
			checkpoint = null;
		}

		if (delayRemove != null) {
			delayRemove.stop();
			delayRemove = null;
		}

		if (timer != null) {
			timer.stop();
			timer = null;
		}

		if (auth != null) {
			auth.stop();
			auth = null;
		}

		if (LocalRocksCacheDb != null) {
			var dir = LocalRocksCacheDb.getDatabaseUrl();
			LocalRocksCacheDb.close();
			deleteDirectory(new File(dir));
			LocalRocksCacheDb = null;
		}

		if (serviceManager != null)
			serviceManager.close();

		if (queueModule != null) {
			queueModule.UnRegisterZezeTables(this);
			queueModule = null;
		}
		if (autoKey != null) {
			autoKey.UnRegister();
			autoKey = null;
		}
		if (autoKeyOld != null) {
			autoKeyOld.UnRegister();
			autoKeyOld = null;
		}

		conf.clearInUseAndIAmSureAppStopped(this, databases);

		for (var db : databases.values())
			db.close();

		if (null != dbh2AgentManager) {
			dbh2AgentManager.stop();
			dbh2AgentManager = null;
		}
		startState = StartState.eStopped;
	}

	public void checkpointRun() {
		checkpoint.runOnce();
	}

	public @NotNull TaskOneByOneByKey getTaskOneByOneByKey() {
		return taskOneByOneByKey;
	}

	public void runTaskOneByOneByKey(@NotNull Object oneByOneKey, @Nullable String actionName, @NotNull FuncLong func) {
		taskOneByOneByKey.Execute(oneByOneKey, newProcedure(func, actionName), DispatchMode.Normal);
	}

	public void runTaskOneByOneByKey(int oneByOneKey, @Nullable String actionName, @NotNull FuncLong func) {
		taskOneByOneByKey.Execute(oneByOneKey, newProcedure(func, actionName), DispatchMode.Normal);
	}

	public void runTaskOneByOneByKey(long oneByOneKey, @Nullable String actionName, @NotNull FuncLong func) {
		taskOneByOneByKey.Execute(oneByOneKey, newProcedure(func, actionName), DispatchMode.Normal);
	}
}
