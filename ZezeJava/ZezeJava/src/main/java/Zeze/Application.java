package Zeze;

import java.io.File;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
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
import Zeze.History.HistoryModule;
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
import Zeze.Util.FileMutex;
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
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnmodifiableView;

public final class Application extends ReentrantLock {
	static final @NotNull Logger logger = LogManager.getLogger(Application.class);

	// R3-X①：终检点后等待在飞flush归零/被中断时重join检查点线程的上限，超时告警继续关库。
	private static final long CHECKPOINT_DRAIN_TIMEOUT_MILLIS = 30_000;

	private final @NotNull String projectName;
	private final @NotNull Config conf;
	private final @NotNull HashMap<String, Database> databases = new HashMap<>();
	private final LongConcurrentHashMap<Table> tables = new LongConcurrentHashMap<>();
	private final ConcurrentHashMap<String, Table> tableNameMap = new ConcurrentHashMap<>();
	private final Locks locks = new Locks();
	private final AbstractAgent serviceManager;
	private AutoKey.Module autoKey;
	private AutoKey transactionIdAutoKey;
	private Timer timer;
	private SafeBatch safeBatch;
	private Zeze.Collections.Queue.Module queueModule;
	private DelayRemove delayRemove;
	private volatile Takeover takeover;
	private HistoryModule historyModule;
	private IGlobalAgent globalAgent;
	private AchillesHeelDaemon achillesHeelDaemon;
	private DeadlockBreaker deadlockBreaker;
	// FND7-54返工：volatile——停机拒绝契约（perform轮次间/tryUpdateAndCheckpoint入口/落库点、
	// TableX.flushWhenReduce）依赖事务线程及时读到stop()的置null；plain字段无happens-before，
	// redo轮次里的检查可能长期读到旧引用，注册进已结束终检点的checkpoint（孤儿脏集=原bug复活）。
	// 读频次每事务个位数，volatile代价可忽略。
	private volatile Checkpoint checkpoint;
	private @Nullable Future<?> flushWhenReduceTimerTask;
	private Schemas schemas;
	private @Nullable Schemas schemasPrevious;
	private final @NotNull ProcedureLockWatcher procedureLockWatcher;

	public enum StartState {
		eUninitialized, // 构造完成，从未start。
		eStarting, // start()进行中（含中途崩溃遗留）。
		eStarted, // 运行中。
		eStopping, // stop()进行中（含中途崩溃遗留）。
		eStopped, // stop()完成，终态：构造期组件已拆除且start不重建，实例不可复用。
	}

	private volatile @NotNull StartState startState = StartState.eUninitialized;
	public RedirectBase redirect;

	private Onz onz;

	private final HotHandle<EventDispatcher.EventHandle> hotHandle = new HotHandle<>();

	/**
	 * 本地Rocks缓存数据库虽然也用了Database接口，但它不给用户提供事务操作的表。
	 * 1. 不需要加入到Databases里面。
	 * 2. 不需要在里面注册表(Database.AddTable)。
	 * 3. Flush的时候特殊处理。see Checkpoint。
	 */
	private DatabaseRocksDb LocalRocksCacheDb;

	// FND8-26：LocalRocksCacheDb目录（zeze_cache_<serverId>，目录名仅含serverId，同JVM
	// 多App撞号或跨进程同CWD误配时互删活跃目录）的互斥锁，持有覆盖start与stop两个
	// 删除入口的整个生命周期；失败语义见FileMutex。
	private @Nullable FileMutex localRocksCacheMutex;

	private @Nullable Dbh2AgentManager dbh2AgentManager;
	private HotManager hotManager;

	private AppBase appBase;
	private ProviderApp providerApp;

	private final ArrayList<Table> replaceTableRecent = new ArrayList<>();
	private final ArrayList<HotUpgradeMemoryTable> hotUpgradeMemoryTables = new ArrayList<>();

	// 只由持锁者（checkpointRunThread/stop）读写；"checkpoint在跑中"=非null且未完成（FND3-49判据化）。
	private Future<?> checkpointFuture;

	private static final ConcurrentHashMap<String, Application> instances = new ConcurrentHashMap<>();

	// 根据实例Id得到Application实例。
	public static @Nullable Application getAppInstance(@NotNull String projectName) {
		return instances.get(projectName);
	}

	public @NotNull ProcedureLockWatcher getProcedureLockWatcher() {
		return procedureLockWatcher;
	}

	public @NotNull HotHandle<EventDispatcher.EventHandle> getHotHandle() {
		return hotHandle;
	}

	public Onz getOnz() {
		return onz;
	}

	/**
	 * 复用的caller遍历器，仅用于verifyCallerCold的调用方获取调用者class。
	 * getCallerClass()必须在公开入口方法的直接方法体内调用，不能经由框架方法转发，
	 * 否则得到的caller是Zeze内部类，校验失去意义。
	 */
	public static final StackWalker CALLER_WALKER =
			StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE);

	/**
	 * 是否配置了热更新模块。没有热更时调用者校验没有约束对象，调用方应先检查本方法，
	 * 避免无谓的走栈开销。
	 */
	public boolean isHotVerifyEnabled() {
		return hotManager != null;
	}

	// verifyCallerNotHot(jdk.internal.reflect.Reflection.getCallerClass());
	// caller必须外部调用得到。
	public void verifyCallerCold(@NotNull Class<?> caller) {
		var callerCl = caller.getClassLoader();
		// 只限制我们自己的HotModule，其他都允许。
		if (hotManager != null && HotManager.isHotModule(callerCl))
			throw new IllegalStateException("caller must not hot.");
	}

	public void setHotManager(HotManager value) {
		hotManager = value;
	}

	public HotManager getHotManager() {
		return hotManager;
	}

	public @Nullable Dbh2AgentManager getDbh2AgentManager() {
		return dbh2AgentManager;
	}

	public @NotNull Dbh2AgentManager tryNewDbh2AgentManager() throws Exception {
		if (dbh2AgentManager == null)
			dbh2AgentManager = new Dbh2AgentManager(serviceManager, conf);
		return dbh2AgentManager;
	}

	public Application(@NotNull String solutionName) throws Exception {
		this(solutionName, null);
	}

	public static @Nullable AbstractAgent createServiceManager(@NotNull Config conf,
															   @NotNull String raftSessionNamePrefix) throws Exception {
		return switch (conf.getServiceManager()) {
			case "raft" -> {
				if (conf.getServiceManagerConf().getSessionName().isEmpty())
					conf.getServiceManagerConf().setSessionName(raftSessionNamePrefix + "#" + conf.getServerId());
				yield new ServiceManagerAgentWithRaft(conf);
			}
			case "disable" -> null;
			default -> new Agent(conf);
		};
	}

	public Application(@NotNull String projectName, @Nullable Config config) throws Exception {
		this.projectName = projectName;
		conf = config != null ? config : Config.load();
		if (conf.getServerId() > 0x3FFF) // 16383 encoded size = 2 bytes
			throw new IllegalStateException("serverId too big. > 16383.");
		procedureLockWatcher = new ProcedureLockWatcher(this);

		Task.tryInitThreadPool(this); // 确保Task线程池已经建立,如需定制,在createZeze前先手动初始化

		serviceManager = createServiceManager(conf, projectName); // 必须在createDatabase之前初始化。里面的Dbh2需要用到serviceManager
		conf.createDatabase(this, databases);
		ZezeCounter.tryInit();

		if (!isNoDatabase()) {
			// 自动初始化的组件。
			autoKey = new AutoKey.Module(this);
			queueModule = new Zeze.Collections.Queue.Module(this);
			historyModule = new HistoryModule(this);
			delayRemove = new DelayRemove(this);
			takeover = new Takeover(this);
			onz = new Onz(this);
		}
	}

	public AppBase getAppBase() {
		return appBase;
	}

	public ProviderApp getProviderApp() {
		return providerApp;
	}

	public void setProviderApp(ProviderApp providerApp) {
		this.providerApp = providerApp;
	}

	public void initialize(@NotNull AppBase app) {
		lock();
		try {
			appBase = app;
			if (timer == null && !isNoDatabase() && redirect != null)
				timer = Timer.create(app);
			if (null != timer)
				safeBatch = new SafeBatch(this);
		} finally {
			unlock();
		}
	}

	public boolean isNoDatabase() {
		return conf.isNoDatabase() || conf.getServerId() < 0;
	}

	public @NotNull HashMap<String, Database> getDatabases() {
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
	public void setCheckpoint(Checkpoint value) {
		lock();
		try {
			if (value == null)
				throw new NullPointerException();
			if (IsStart)
				throw new IllegalStateException("Checkpoint only can setup before start.");
			_checkpoint = value;
		} finally {
			unlock();
		}
	}
	*/

	public @NotNull Locks getLocks() {
		return locks;
	}

	public Schemas getSchemas() {
		return schemas;
	}

	public @Nullable Schemas getSchemasPrevious() {
		return schemasPrevious;
	}

	public void setSchemas(Schemas value) {
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
		var db = getDatabase(dbName);
		// 两道唯一性校验全部通过后再统一登记（异常路径原子化）：原先TableKey.tables.put与
		// tables.putIfAbsent先于校验执行，表名冲突抛异常后tables残留半注册幻影表（从未open），
		// 重复id时还会先污染既有表的id→name映射。调用方均持Application锁（openDynamicTable）
		// 或处于启动期单线程模块注册，check-then-put无新增并发窗口。
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
	}

	public @NotNull ArrayList<HotUpgradeMemoryTable> __get_upgrade_memory_table__() {
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
		if (exist != null) {
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

	public @NotNull StartState getStartState() {
		return startState;
	}

	public void openDynamicTable(@NotNull String dbName, @NotNull Table table) {
		lock();
		try {
			addTable(dbName, table).openDynamicTable(this, table);
		} finally {
			unlock();
		}
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

	public @NotNull @UnmodifiableView Map<String, Table> getTables() {
		return Collections.unmodifiableMap(tableNameMap);
	}

	public @NotNull Database getDatabase(@NotNull String name) {
		var db = databases.get(name);
		if (db == null)
			throw new IllegalStateException("database not exist name=" + name);
		return db;
	}

	public @NotNull AutoKey getAutoKey(@NotNull String name) {
		return autoKey.getOrAdd(name);
	}

	public AutoKey getTransactionIdAutoKey() {
		return transactionIdAutoKey;
	}

	public Timer getTimer() {
		return timer;
	}

	public SafeBatch getSafeBatch() {
		return safeBatch;
	}

	public Zeze.Collections.Queue.Module getQueueModule() {
		return queueModule;
	}

	public HistoryModule getHistoryModule() {
		return historyModule;
	}

	public DelayRemove getDelayRemove() {
		return delayRemove;
	}

	public Takeover getTakeover() {
		return takeover;
	}

	public @NotNull Procedure newProcedure(@NotNull FuncLong action, @Nullable String actionName) {
		return newProcedure(action, actionName, TransactionLevel.Serializable);
	}

	public @NotNull Procedure newProcedure(@NotNull FuncLong action, @Nullable String actionName,
										   @Nullable TransactionLevel level) {
		if (!isStart()) {
			throw new IllegalStateException("App Not Start: " + startState
					+ ", action=" + (actionName != null && !actionName.isEmpty() ? actionName : action.getClass()));
		}
		return new Procedure(this, action, actionName, level);
	}

	public @NotNull ProtocolProcedure newProcedure(@NotNull FuncLong action, @Nullable String actionName,
												   @Nullable TransactionLevel level, @NotNull String protocolClassName,
												   @NotNull Binary protocolRawArgument) {
		if (!isStart()) {
			throw new IllegalStateException("App Not Start: " + startState
					+ ", action=" + (actionName != null && !actionName.isEmpty() ? actionName : action.getClass()));
		}
		return new ProtocolProcedure(this, action, actionName, level, protocolClassName, protocolRawArgument);
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
		// noDatabase模式不创建delayRemove（构造期跳过）：生命周期钩子对两种模式都要有
		// 定义良好的行为——no-op（与无数据库语义一致）（FND4-81）。
		if (delayRemove != null)
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

	// 数据库Meta兼容检查，初始化。
	private void schemasCompatible() throws Exception {
		var defaultDb = getDatabase(conf.getDefaultTableConf().getDatabaseName());
		if (schemas != null) {
			schemas.compile();
			schemas.setAppVersion(conf.getAppVersion());
			var keyOfSchemas = ByteBuffer.Allocate(32);
			var serverId = conf.getServerId();
			keyOfSchemas.WriteString("zeze.Schemas.V4." + serverId);
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
					if (schemas.getAppVersion() < schemasPrevious.getAppVersion()) {
						logger.info("OldAppVersion Skip.");
						return; // 当前的发布版本小于先前时，不做任何操作，直接返回。
					}

					schemas.checkCompatible(schemasPrevious, this);
					version = dataVersion.version;
				}
				// schemasPrevious maybe null
				//schemas.buildRelationalTables(this, schemasPrevious);

				var newData = ByteBuffer.Allocate(1024);
				schemas.encode(newData);
				var versionRc = defaultDb.getDirectOperates().saveDataWithSameVersion(keyOfSchemas, newData, version);
				if (versionRc == null || versionRc.getValue())
					break;
			}
		}
	}

	private void atomicOpenDatabase() throws Exception {
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
				if (conf.isHistory())
					Zeze.History.Helper.registerAllTableLogs(this);

				// Open Databases
				for (var e : databases.entrySet()) {
					var timeBegin = System.nanoTime();
					var db = e.getValue();
					db.open(this);
					logger.info("open {} tables from database '{}' ({} ms)",
							db.getTables().size(), e.getKey(), (System.nanoTime() - timeBegin) / 1_000_000);
				}

				for (var db : getDatabases().values()) {
					if (!(db instanceof DatabaseRelationalMapping))
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
				return; // 幂等
			if (startState != StartState.eUninitialized)
				throw new IllegalStateException("Application '" + getProjectName()
						+ " startState = " + startState);
			startState = StartState.eStarting;

			logSystemProperties();
			logZezeVersion();
			var serverId = conf.getServerId();
			logger.info("Start ServerId={}", serverId);

			var hasDatabase = !isNoDatabase();
			if (hasDatabase) {
				if ("true".equalsIgnoreCase(System.getProperty(Daemon.propertyNameClearInUse))) {
					conf.clearInUse(databases);
					//var defaultDb = getDatabase(conf.getDefaultTableConf().getDatabaseName());
					//defaultDb.getDirectOperates().unlock();
				}

				// Set Database InUse
				for (var db : databases.values())
					db.getDirectOperates().setInUse(serverId, conf.getGlobalCacheManagerHostNameOrAddress());
				addShutdownHook();

				// Open RocksCache
				var dbConf = new Config.DatabaseConf();
				dbConf.setName("zeze_cache_" + serverId);
				dbConf.setDatabaseUrl(dbConf.getName());
				// FND8-26：先锁后删——同JVM/跨进程撞serverId在删目录前即fail-fast。
				localRocksCacheMutex = FileMutex.acquire(dbConf.getDatabaseUrl() + ".lock",
						"zeze_cache dir (serverId=" + conf.getServerId() + ")");
				deleteDirectory(new File(dbConf.getDatabaseUrl()));
				dbConf.setDatabaseType(Config.DbType.RocksDb);
				LocalRocksCacheDb = new DatabaseRocksDb(this, dbConf, true);
				LocalRocksCacheDb.open(this);
			} else {
				addShutdownHook();
			}

			var serviceManagerConf = conf.getServiceConf(Agent.defaultServiceName);
			// raft版SM的地址来自raftXml而非ServiceConf节点，按Agent服务名查serviceConfMap必为null，
			// 旧门槛会跳过serviceManager.start()，raft版SM永不启动，subscribeService挂死在waitLoginReady。
			var isRaftServiceManager = "raft".equals(conf.getServiceManager());
			if ((serviceManagerConf != null || isRaftServiceManager) && serviceManager != null) {
				serviceManager.start();
				try {
					serviceManager.waitReady();
				} catch (Exception ex) {
					// raft 版第一次等待由于选择leader原因肯定会失败一次。
					//noinspection ConstantValue
					if (ex instanceof InterruptedException)
						Thread.currentThread().interrupt(); // 恢复被底层清除的中断标志
					serviceManager.waitReady();
				}
			}

			if (hasDatabase) {
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

				deadlockBreaker = new DeadlockBreaker(this);
				// Checkpoint
				checkpoint = new Checkpoint(this, conf.getCheckpointMode(), databases.values(), serverId);
				checkpoint.start(conf.getCheckpointPeriod()); // 定时模式可以和其他模式混用。

				// start last
				if (achillesHeelDaemon != null)
					achillesHeelDaemon.start();

				// 接管租约：Application.start()返回前claim完成（需Checkpoint已建立），
				// 此后Timer.start/startLast、CsQueue构造全走addScope晚注册stamp。
				if (takeover != null) {
					takeover.start();
					// SM Suspect提示→tryTransfer（非raft与raft版SM都会在会话断开时广播Suspect）。
					if (serviceManager != null)
						serviceManager.setOnSuspect(deadServerId -> takeover.tryTransfer(deadServerId));
				}

				delayRemove.start();
				if (timer != null)
					timer.loadCustomClassAnd();
				if (deadlockBreaker != null)
					deadlockBreaker.start();
				if (onz != null)
					onz.start();
				if (autoKey != null)
					transactionIdAutoKey = autoKey.getOrAdd("TransactionIdAutoKey");
			}

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

			// 拆解全程处于eStopping。
			startState = StartState.eStopping;

			// FND-A1-6：同名实例时putIfAbsent只保留先注册者，无条件remove会错删他人的注册，
			// 导致幸存实例的Online.findOnline失效（延迟登出静默丢失）。remove(key,value)
			// 只删属于自己的注册（Application按引用判等）。
			instances.remove(getProjectName(), this);

			if (null != checkpointFuture) {
				// FND-A1-10：get()在检查点任务以异常完成时抛ExecutionException并从stop逃逸，
				// startState滞留 eStopping、数据库未关，后续start()无法恢复。任务异常
				// 已由Task框架记录，这里吞掉保证停机流程继续走完。
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
				stopStep("takeover.release", takeover::release);

			if (achillesHeelDaemon != null) {
				stopStep("achillesHeelDaemon.stopAndJoin", achillesHeelDaemon::stopAndJoin);
				achillesHeelDaemon = null;
			}

			// FND7-54：先停事务生产组件（delayRemove/safeBatch/timer）并等待在途任务，再关
			// globalAgent（组件事务可能还需GCM申请锁），最后checkpoint.stopAndJoin作为终检点
			// 收尾——保证"最后一个提交先于最后一次flush"。终检点之后到达的提交由
			// Transaction.perform/RelativeRecordSet.tryUpdateAndCheckpoint的停机拒绝转为
			// Closed显式失败，不再静默丢弃（假成功）。
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

			if (globalAgent != null) {
				var ga = globalAgent;
				stopStep("globalAgent.close", ga::close);
				// FND10 txn-01：Releaser（GCM断连/守护Release触发的降级+checkpoint线程）不被close
				// 收编，其checkpointRun→flush与下方LocalRocksCacheDb.close+deleteDirectory并发属
				// ad5801593判例的native UAF类窗口。关库前有界join，超时告警继续。
				stopStep("globalAgent.awaitReleaser", () -> ga.awaitReleaser(CHECKPOINT_DRAIN_TIMEOUT_MILLIS));
				globalAgent = null;
			}
			if (flushWhenReduceTimerTask != null) {
				var task = flushWhenReduceTimerTask;
				stopStep("flushWhenReduceTimerTask.cancel", () -> task.cancel(false));
				flushWhenReduceTimerTask = null;
			}

			if (checkpoint != null) {
				// FND7-54：先置null再join——join期间到达的提交立即进入停机拒绝（Closed），
				// 终检点（join内的final flush）只负责此前已注册的脏集。
				var cp = checkpoint;
				checkpoint = null;
				stopStep("checkpoint.stopAndJoin", () -> {
					try {
						cp.stopAndJoin();
					} catch (Throwable ex) {
						// R3-X①（FND7-56边界收窄）：stopAndJoin的join被中断（forceThrow）时
						// 检查点线程仍存活（可能正要进入final flush），吞掉异常直接继续会在它
						// 还要落库时关库——与在飞数据通路并发close是native UAF类（ad5801593）。
						// 有界忽略中断重join，超时告警继续（FND7-56的"终态必达"不变）。
						logger.error("checkpoint stopAndJoin interrupted/failed, bounded re-join before close", ex);
						cp.joinIgnoreInterrupt(CHECKPOINT_DRAIN_TIMEOUT_MILLIS);
					}
				});
				// R3-X①：mid-flush halt窄窗——FND7-54的停机拒绝只拦新提交，不等待已过门的
				// 在飞flush（Immediately模式业务线程的checkpoint.flush、Reduce降级flush、
				// checkpointRun的runOnce）：它们已打开LocalRocksCacheDb事务，与随后的
				// close+deleteDirectory并发同样属于ad5801593的native UAF类。有界等待归零，
				// 超时告警继续（30s上限，保证停机不因此永久挂起）。
				if (!cp.waitNoActiveFlush(CHECKPOINT_DRAIN_TIMEOUT_MILLIS))
					logger.error("checkpoint active flush not drained in {}ms, continue to close databases "
							+ "(risk of close racing in-flight flush)", CHECKPOINT_DRAIN_TIMEOUT_MILLIS);
			}

			if (LocalRocksCacheDb != null) {
				var rocksCacheDb = LocalRocksCacheDb;
				var dir = rocksCacheDb.getDatabaseUrl();
				stopStep("LocalRocksCacheDb.close", () -> {
					rocksCacheDb.close();
					deleteDirectory(new File(dir));
				});
				LocalRocksCacheDb = null;
			}
			// FND8-26：start失败于锁后（LocalRocksCacheDb尚未赋值）也要释放，故在块外无条件调用。
			if (localRocksCacheMutex != null) {
				localRocksCacheMutex.close();
				localRocksCacheMutex = null;
			}

			if (serviceManager != null)
				stopStep("serviceManager.close", serviceManager::close);

			if (queueModule != null) {
				var qm = queueModule;
				stopStep("queueModule.UnRegisterZezeTables", () -> qm.UnRegisterZezeTables(this));
				queueModule = null;
			}
			if (historyModule != null) {
				var hm = historyModule;
				stopStep("historyModule.UnRegisterZezeTables", () -> hm.UnRegisterZezeTables(this));
				historyModule = null;
			}
			if (autoKey != null) {
				stopStep("autoKey.UnRegister", autoKey::UnRegister);
				autoKey = null;
				transactionIdAutoKey = null;
			}
			if (takeover != null) {
				var tk = takeover;
				stopStep("takeover.UnRegisterZezeTables", () -> tk.UnRegisterZezeTables(this));
				takeover = null;
			}
			if (!isNoDatabase())
				stopStep("clearInUse", () -> conf.clearInUse(databases));

			for (var e : databases.entrySet())
				stopStep("db.close '" + e.getKey() + '\'', e.getValue()::close);

			if (dbh2AgentManager != null) {
				stopStep("dbh2AgentManager.stop", dbh2AgentManager::stop);
				dbh2AgentManager = null;
			}
			startState = StartState.eStopped;
		} finally {
			unlock();
		}
	}

	// FND7-56：停机步骤异常隔离——任一拆卸步骤抛出（Service.stop关连接的IO异常、
	// stopAndJoin的join中断forceThrow等）只记日志继续，不得跳过其后步骤
	// （db.close/clearInUse/各UnRegister），保证终态必达eStopped
	// （对齐GlobalAgent.stop的per-agent兜底与FND-A1-10意图）。
	private static void stopStep(@NotNull String name, @NotNull Action0 action) {
		try {
			action.run();
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt(); // 恢复中断标志，继续拆卸其余步骤
			logger.error("stop step '{}' interrupted, continue", name, e);
		} catch (Throwable e) { // logger.error
			logger.error("stop step '{}' exception, continue", name, e);
		}
	}

	public void checkpointRun() {
		// 同endStart（FND4-81）：noDatabase模式不创建checkpoint。
		// FND8-21：volatile单次快照读——原判空后二次读字段，stop()持Application锁置null
		// 恰好落在两条载入之间时解引用得null即NPE（本方法无锁，锁只约束stop与checkpointRunThread）。
		var cp = checkpoint;
		if (cp != null)
			cp.runOnce();
	}

	/**
	 * FND8-21统一收口：checkpoint尽力保存后无条件halt。三处共用（Transaction.perform的
	 * finalCommit失败分支、AchillesHeelDaemon的ProcessDaemon/ThreadDaemon超时分支）——
	 * FND4-04：checkpointRun/LogManager失败不得吞掉halt本身。fatal自身再包独立try，
	 * 日志系统异常也不得拦下终态。
	 */
	public static void haltAfterCheckpoint(@NotNull Application zeze, int exitCode) {
		try {
			zeze.checkpointRun();
		} catch (Throwable ex) {
			try {
				logger.fatal("checkpointRun before halt({}) fail", exitCode, ex);
			} catch (Throwable ignored) {
			}
		}
		try {
			LogManager.shutdown();
		} catch (Throwable ignored) {
		}
		Runtime.getRuntime().halt(exitCode);
	}

	public void checkpointRunThread() {
		if (checkpoint == null)
			return;
		lock();
		try {
			var f = checkpointFuture;
			// FND3-49："在跑中"是字段的派生判据（非null且未完成），不由任务清零：
			// pool.submit先入队后返回，任务可能在赋值前完成，任务内finally清空=白清，
			// 迟到赋值会留下已完成的哨兵future，后续调用被永久阻断。
			// 字段只由持锁者（本方法/stop）读写，任务不触碰。
			if (startState == StartState.eStarted && (f == null || f.isDone()))
				checkpointFuture = TaskSpec.ofAction(checkpoint::runOnce)
						.name("CheckpointRunThread").submitNow();
		} finally {
			unlock();
		}
	}

	/** @deprecated 请直接使用 {@link Task#getOneByOne()}。 */
	@Deprecated
	@SuppressWarnings("MethodMayBeStatic")
	public @NotNull TaskOneByOneByKey getTaskOneByOneByKey() {
		return Task.getOneByOne();
	}

	public void runTaskOneByOneByKey(@NotNull Object oneByOneKey, @Nullable String actionName, @NotNull FuncLong func) {
		TaskSpec.ofProcedure(newProcedure(func, actionName))
				.dispatchMode(DispatchMode.Normal).executeOneByOne(oneByOneKey);
	}

	public void runTaskOneByOneByKey(int oneByOneKey, @Nullable String actionName, @NotNull FuncLong func) {
		TaskSpec.ofProcedure(newProcedure(func, actionName))
				.dispatchMode(DispatchMode.Normal).executeOneByOne(oneByOneKey);
	}

	public void runTaskOneByOneByKey(long oneByOneKey, @Nullable String actionName, @NotNull FuncLong func) {
		TaskSpec.ofProcedure(newProcedure(func, actionName))
				.dispatchMode(DispatchMode.Normal).executeOneByOne(oneByOneKey);
	}
}
