package Zeze;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;
import Zeze.Arch.RedirectBase;
import Zeze.Component.AutoKey;
import Zeze.Component.AutoKeyOld;
import Zeze.Component.DelayRemove;
import Zeze.Component.Timer;
import Zeze.Dbh2.Dbh2AgentManager;
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
import Zeze.Transaction.ProcedureStatistics;
import Zeze.Transaction.ResetDB;
import Zeze.Transaction.Table;
import Zeze.Transaction.TableKey;
import Zeze.Transaction.TableStatistics;
import Zeze.Transaction.TransactionLevel;
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
import org.rocksdb.RocksDBException;

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
	private Checkpoint checkpoint;
	private Future<?> flushWhenReduceTimerTask;
	private Schemas schemas;
	private Schemas schemasPrevious; // maybe null

	private int startState; // 0:未start; 1:开始start但未完成; 2:完成了start
	public RedirectBase redirect;
	/**
	 * 本地Rocks缓存数据库虽然也用了Database接口，但它不给用户提供事务操作的表。
	 * 1. 不需要加入到Databases里面。
	 * 2. 不需要在里面注册表(Database.AddTable)。
	 * 3. Flush的时候特殊处理。see Checkpoint。
	 */
	private DatabaseRocksDb LocalRocksCacheDb;

	private Dbh2AgentManager dbh2AgentManager;

	public Dbh2AgentManager getDbh2AgentManager() {
		return dbh2AgentManager;
	}

	public Dbh2AgentManager tryNewDbh2AgentManager() throws RocksDBException {
		if (null == dbh2AgentManager)
			dbh2AgentManager = new Dbh2AgentManager(conf);
		return dbh2AgentManager;
	}

	public Application(@NotNull String solutionName) throws Exception {
		this(solutionName, null);
	}

	@SuppressWarnings("deprecation")
	public Application(@NotNull String projectName, @Nullable Config config) throws Exception {

		this.projectName = projectName;
		conf = config != null ? config : Config.load();
		if (conf.getServerId() > 0x3FFF) // 16383 encoded size = 2 bytes
			throw new IllegalStateException("serverId too big. > 16383.");

		// Start Thread Pool
		Task.tryInitThreadPool(this, null, null); // 确保Task线程池已经建立,如需定制,在createZeze前先手动初始化

		conf.createDatabase(this, databases);
		PerfCounter.instance.tryStartScheduledLog();

		switch (conf.getServiceManager()) {
		case "raft":
			if (conf.getServiceManagerConf().getSessionName().isEmpty()) {
				conf.getServiceManagerConf().setSessionName(projectName + "#" + conf.getServerId());
			}
			serviceManager = new ServiceManagerAgentWithRaft(this);
			break;

		case "disable":
			serviceManager = null;
			break;

		default:
			serviceManager = new Agent(this);
			break;
		}

		if (!isNoDatabase()) {
			// 自动初始化的组件。
			autoKey = new AutoKey.Module(this);
			autoKeyOld = new AutoKeyOld.Module(this);
			queueModule = new Zeze.Collections.Queue.Module(this);
			delayRemove = new DelayRemove(this);
		}
	}

	public synchronized void initialize(@NotNull AppBase app) {
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
		return startState == 2;
	}

	public AbstractAgent getServiceManager() {
		return serviceManager;
	}

	public @Nullable IGlobalAgent getGlobalAgent() {
		return globalAgent;
	}

	public @Nullable Checkpoint getCheckpoint() {
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

	// 用于热更的时候替换Table.
	// 热更不会调用addTable,removeTable。
	public @NotNull Database replaceTable(@NotNull String dbName, @NotNull Table table) {
		TableKey.tables.put(table.getId(), table.getName()); // always put
		var db = getDatabase(dbName);
		var exist = tables.put(table.getId(), table);
		if (null != exist) {
			// 旧表存在，新表需要处理open，但这个open跟第一次启动不一样，有一些状态从旧表得到。
			table.open(exist);
			// 旧表禁用。防止应用保留了旧表引用，还去使用导致错误。
			exist.disable();
		}
		tableNameMap.put(table.getName(), table); // always put, 操作在tables阶段完成。
		db.replaceTable(table);
		return db;
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
		if (startState != 2)
			throw new IllegalStateException("App Not Start");
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
	private void schemasCompatible() throws Exception {
		var defaultDb = getDatabase(conf.getDefaultTableConf().getDatabaseName());
		if (schemas != null) {
			schemas.compile();
			var keyOfSchemas = ByteBuffer.Allocate(24);
			var serverId = conf.getServerId();
			keyOfSchemas.WriteString("zeze.Schemas." + serverId);
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
						logger.error("Schemas Implement Changed? serverId={}", serverId, ex);
					}
					if (schemas.getAppPublishVersion() < schemasPrevious.getAppPublishVersion())
						return; // 当前的发布版本小于先前时，不做任何操作，直接返回。

					ResetDB.checkAndRemoveTable(schemasPrevious, this);
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

	public synchronized void start() throws Exception {
		if (startState == 2)
			return;
		if (startState == 1)
			stop();
		startState = 1;
		ShutdownHook.add(this, () -> {
			logger.info("zeze({}) ShutdownHook begin", this.projectName);
			stop();
			logger.info("zeze({}) ShutdownHook end", this.projectName);
		});
		var serverId = conf.getServerId();
		logger.info("Start ServerId={}", serverId);

		var noDatabase = isNoDatabase();
		if (!noDatabase) {
			if ("true".equalsIgnoreCase(System.getProperty(Daemon.propertyNameClearInUse)))
				conf.clearInUseAndIAmSureAppStopped(this, databases);

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
					achillesHeelDaemon = new AchillesHeelDaemon(this, impl.agents);
					impl.start();
				} else {
					var impl = new GlobalCacheManagerWithRaftAgent(this, hosts);
					globalAgent = impl;
					achillesHeelDaemon = new AchillesHeelDaemon(this, impl.agents);
					impl.start();
				}
			}

			// Checkpoint
			checkpoint = new Checkpoint(this, conf.getCheckpointMode(), databases.values(), serverId);
			checkpoint.start(conf.getCheckpointPeriod()); // 定时模式可以和其他模式混用。

			// start last
			if (null != achillesHeelDaemon)
				achillesHeelDaemon.start();
			startState = 2;

			delayRemove.start();
			if (timer != null)
				timer.loadCustomClassAnd();
		} else
			startState = 2;
		ProcedureStatistics.getInstance().start(conf.getProcedureStatisticsReportPerod());
		TableStatistics.getInstance().start(conf.getTableStatisticsReportPeriod());
	}

	public synchronized void stop() throws Exception {
		if (startState == 0)
			return;
		startState = 1;
		ShutdownHook.remove(this);
		TableStatistics.getInstance().stop();
		ProcedureStatistics.getInstance().stop();
		logger.info("Stop ServerId={}", conf.getServerId());

		if (delayRemove != null) {
			delayRemove.stop();
			delayRemove = null;
		}

		if (timer != null) {
			timer.stop();
			timer = null;
		}

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
		startState = 0;
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
