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
import Zeze.Transaction.ResetDB;
import Zeze.Transaction.Table;
import Zeze.Transaction.TableKey;
import Zeze.Transaction.TransactionLevel;
import Zeze.Util.FuncLong;
import Zeze.Util.LongConcurrentHashMap;
import Zeze.Util.ShutdownHook;
import Zeze.Util.Str;
import Zeze.Util.Task;
import Zeze.Util.TaskOneByOneByKey;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public final class Application {
	static final Logger logger = LogManager.getLogger(Application.class);

	private final String projectName;
	private final Config conf;
	private final HashMap<String, Database> databases = new HashMap<>();
	private final LongConcurrentHashMap<Table> tables = new LongConcurrentHashMap<>();
	private final ConcurrentHashMap<String, Table> tableNameMap = new ConcurrentHashMap<>();
	private final TaskOneByOneByKey taskOneByOneByKey = new TaskOneByOneByKey();
	private final Locks locks = new Locks();
	private final AbstractAgent serviceManager;
	private AutoKey.Module autoKey;
	@Deprecated // 暂时保留
	private AutoKeyOld.Module autoKeyOld;
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

	public Application() {
		projectName = "";
		conf = null;
		serviceManager = null;
	}

	public Application(String solutionName) throws Exception {
		this(solutionName, null);
	}

	@SuppressWarnings("deprecation")
	public Application(String projectName, Config config) throws Exception {
		this.projectName = projectName;
		conf = config != null ? config : Config.load();
		conf.createDatabase(this, databases);

		// Start Thread Pool
		Task.tryInitThreadPool(this, null, null); // 确保Task线程池已经建立,如需定制,在createZeze前先手动初始化

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

	public synchronized void initialize(AppBase app) {
		if (timer == null && !isNoDatabase() && redirect != null)
			timer = Timer.create(app);
	}

	public boolean isNoDatabase() {
		return conf == null || conf.isNoDatabase() || conf.getServerId() < 0;
	}

	public HashMap<String, Database> getDatabases() {
		return databases;
	}

	public Config getConfig() {
		return conf;
	}

	public boolean isStart() {
		return startState == 2;
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

	public Locks getLocks() {
		return locks;
	}

	public Schemas getSchemas() {
		return schemas;
	}
	public Schemas getSchemasPrevious() {
		return schemasPrevious;
	}

	public void setSchemas(Schemas value) {
		schemas = value;
	}

	public String getProjectName() {
		return projectName;
	}

	public AchillesHeelDaemon getAchillesHeelDaemon() {
		return achillesHeelDaemon;
	}

	public DatabaseRocksDb getLocalRocksCacheDb() {
		return LocalRocksCacheDb;
	}

	public Database addTable(String dbName, Table table) {
		TableKey.tables.put(table.getId(), table.getName());
		var db = getDatabase(dbName);
		if (tables.putIfAbsent(table.getId(), table) != null)
			throw new IllegalStateException("duplicate table id=" + table.getId());
		if (tableNameMap.putIfAbsent(table.getName(), table) != null)
			throw new IllegalStateException("duplicate table name=" + table.getName());
		db.addTable(table);
		return db;
	}

	public synchronized void openDynamicTable(String dbName, Table table) {
		addTable(dbName, table).openDynamicTable(this, table);
	}

	public void removeTable(String dbName, Table table) {
		tables.remove(table.getId());
		tableNameMap.remove(table.getName());
		getDatabase(dbName).removeTable(table);
	}

	public Table getTable(int id) {
		return tables.get(id);
	}

	public Table getTable(String name) {
		return tableNameMap.get(name);
	}

	public Map<String, Table> getTables() {
		return Collections.unmodifiableMap(tableNameMap);
	}

	public Database getDatabase(String name) {
		var db = databases.get(name);
		if (db == null)
			throw new IllegalStateException("database not exist name=" + name);
		return db;
	}

	public AutoKey getAutoKey(String name) {
		return autoKey.getOrAdd(name);
	}

	@Deprecated // 暂时保留
	public AutoKeyOld getAutoKeyOld(String name) {
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

	public Procedure newProcedure(FuncLong action, String actionName) {
		return newProcedure(action, actionName, TransactionLevel.Serializable, null);
	}

	public Procedure newProcedure(FuncLong action, String actionName, TransactionLevel level, Object userState) {
		if (startState != 2)
			throw new IllegalStateException("App Not Start");
		return new Procedure(this, action, actionName, level, userState);
	}

	static void deleteDirectory(File directoryToBeDeleted) throws IOException, InterruptedException {
		File[] allContents = directoryToBeDeleted.listFiles();
		if (allContents != null) {
			for (File file : allContents) {
				deleteDirectory(file);
			}
		}
		for (int i = 0; directoryToBeDeleted.exists(); i++) {
			//noinspection ResultOfMethodCallIgnored
			directoryToBeDeleted.delete();
			if (!directoryToBeDeleted.exists())
				break;
			if (i >= 50)
				throw new IOException("delete failed: " + directoryToBeDeleted.getAbsolutePath());
			//noinspection BusyWait
			Thread.sleep(100);
		}
	}

	public void endStart() {
		delayRemove.continueJobs();
	}

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
					schemasPrevious = new Schemas();
					try {
						schemasPrevious.decode(dataVersion.data);
						schemasPrevious.compile();
					} catch (Exception ex) {
						schemasPrevious = null;
						logger.error("Schemas Implement Changed?", ex);
					}
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

	private void alterRelationalTable() {
		for (var db : getDatabases().values()) {
			if (!(db instanceof DatabaseMySql))
				continue;
			var mysql = (DatabaseMySql)db;
			// todo 需要 schemas 的版本号，如果已经是最新的不需要再次执行 alter。
			// todo lock database
			for (var table : db.getTables()) {
				if (!table.isRelationalMapping())
					continue;
				table.tryAlter();
			}
			// todo unlock database
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
		var serverId = conf != null ? conf.getServerId() : -1;
		logger.info("Start ServerId={}", serverId);

		var noDatabase = isNoDatabase();
		if (!noDatabase) {
			assert conf != null;
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
		var serviceManagerConf = conf != null ? conf.getServiceConf(Agent.defaultServiceName) : null;
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
			schemasCompatible();

			// Open Databases
			for (var db : databases.values())
				db.open(this);

			// 关系表映射 alter table
			// 需要总控，所以不在 table 创建的时候处理。
			alterRelationalTable(); // 总控互斥流程。

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
	}

	public synchronized void stop() throws Exception {
		if (startState == 0)
			return;
		startState = 1;
		ShutdownHook.remove(this);
		logger.info("Stop ServerId={}", conf != null ? conf.getServerId() : -1);

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

		if (conf != null)
			conf.clearInUseAndIAmSureAppStopped(this, databases);

		for (var db : databases.values())
			db.close();
		startState = 0;
	}

	public void checkpointRun() {
		checkpoint.runOnce();
	}

	public TaskOneByOneByKey getTaskOneByOneByKey() {
		return taskOneByOneByKey;
	}

	public void runTaskOneByOneByKey(Object oneByOneKey, String actionName, FuncLong func) {
		taskOneByOneByKey.Execute(oneByOneKey, newProcedure(func, actionName), DispatchMode.Normal);
	}

	public void runTaskOneByOneByKey(int oneByOneKey, String actionName, FuncLong func) {
		taskOneByOneByKey.Execute(oneByOneKey, newProcedure(func, actionName), DispatchMode.Normal);
	}

	public void runTaskOneByOneByKey(long oneByOneKey, String actionName, FuncLong func) {
		taskOneByOneByKey.Execute(oneByOneKey, newProcedure(func, actionName), DispatchMode.Normal);
	}
}
