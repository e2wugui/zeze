package Zeze;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.concurrent.Future;
import Zeze.Arch.RedirectBase;
import Zeze.Collections.Queue;
import Zeze.Component.AutoKey;
import Zeze.Serialize.ByteBuffer;
import Zeze.Services.GlobalCacheManagerWithRaftAgent;
import Zeze.Services.ServiceManager.Agent;
import Zeze.Transaction.AchillesHeelDaemon;
import Zeze.Transaction.Checkpoint;
import Zeze.Transaction.Database;
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
import org.rocksdb.RocksDBException;

public final class Application {
	static final Logger logger = LogManager.getLogger(Application.class);

	private final String SolutionName;
	private final Config Conf;
	private final HashMap<String, Database> Databases = new HashMap<>();
	private final LongConcurrentHashMap<Table> Tables = new LongConcurrentHashMap<>();
	private final TaskOneByOneByKey TaskOneByOneByKey = new TaskOneByOneByKey();
	private final Locks Locks = new Locks();
	private final Agent ServiceManagerAgent;
	private AutoKey.Module autoKey;
	private Zeze.Collections.Queue.Module queueModule;
	private IGlobalAgent GlobalAgent;
	private Zeze.Transaction.AchillesHeelDaemon AchillesHeelDaemon;
	private Checkpoint _checkpoint;
	private Future<?> FlushWhenReduceTimerTask;
	private Schemas Schemas;
	private boolean IsStart;
	public RedirectBase Redirect;

	public Zeze.Transaction.AchillesHeelDaemon getAchillesHeelDaemon() {
		return AchillesHeelDaemon;
	}

	/**
	 * 本地Rocks缓存数据库虽然也用了Database接口，但它不给用户提供事务操作的表。
	 * 1. 不需要加入到Databases里面。
	 * 2. 不需要在里面注册表(Database.AddTable)。
	 * 3. Flush的时候特殊处理。see Checkpoint。
	 */
	private DatabaseRocksDb LocalRocksCacheDb;

	public DatabaseRocksDb getLocalRocksCacheDb() {
		return LocalRocksCacheDb;
	}

	public Application(String solutionName) throws Throwable {
		this(solutionName, null);
	}

	public Application(String solutionName, Config config) throws Throwable {
		SolutionName = solutionName;
		Conf = config != null ? config : Config.Load();
		Conf.CreateDatabase(this, Databases);
		ServiceManagerAgent = new Agent(this);
		ShutdownHook.add(this, () -> {
			logger.info("zeze({}) ShutdownHook begin", SolutionName);
			Stop();
			logger.info("zeze({}) ShutdownHook end", SolutionName);
		});
	}

	public Application() {
		SolutionName = "";
		Conf = null;
		ServiceManagerAgent = null;
		ShutdownHook.add(this, () -> {
			logger.info("zeze ShutdownHook begin");
			Stop();
			logger.info("zeze ShutdownHook end");
		});
	}

	public HashMap<String, Database> getDatabases() {
		return Databases;
	}

	public Config getConfig() {
		return Conf;
	}

	public boolean isStart() {
		return IsStart;
	}

	public Agent getServiceManagerAgent() {
		return ServiceManagerAgent;
	}

	public IGlobalAgent getGlobalAgent() {
		return GlobalAgent;
	}

	public Checkpoint getCheckpoint() {
		return _checkpoint;
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
		return Locks;
	}

	public Schemas getSchemas() {
		return Schemas;
	}

	public void setSchemas(Schemas value) {
		Schemas = value;
	}

	public String getSolutionName() {
		return SolutionName;
	}

	public void AddTable(String dbName, Table table) {
		TableKey.Tables.put(table.getId(), table.getName());
		var db = GetDatabase(dbName);
		if (Tables.putIfAbsent(table.getId(), table) != null)
			throw new IllegalStateException("duplicate table name=" + table.getName());
		db.AddTable(table);
	}

	public void RemoveTable(String dbName, Table table) {
		Tables.remove(table.getId());
		GetDatabase(dbName).RemoveTable(table);
	}

	public Table GetTable(int id) {
		return Tables.get(id);
	}

	public Table GetTableSlow(String name) {
		for (var table : Tables) {
			if (table.getName().equals(name))
				return table;
		}
		return null;
	}

	public Database GetDatabase(String name) {
		var db = Databases.get(name);
		if (db == null)
			throw new IllegalStateException("database not exist name=" + name);
		return db;
	}

	public AutoKey GetAutoKey(String name) {
		return autoKey.getOrAdd(name);
	}

	public Zeze.Collections.Queue.Module getQueueModule() {
		return queueModule;
	}

	public Procedure NewProcedure(FuncLong action, String actionName) {
		return NewProcedure(action, actionName, TransactionLevel.Serializable, null);
	}

	public Procedure NewProcedure(FuncLong action, String actionName, TransactionLevel level, Object userState) {
		if (!IsStart)
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

	public synchronized void Start() throws Throwable {
		if (IsStart)
			return;
		var serverId = Conf != null ? Conf.getServerId() : -1;
		logger.info("Start ServerId={}", serverId);

		// Start Thread Pool
		Task.tryInitThreadPool(this, null, null); // 确保Task线程池已经建立,如需定制,在Start前先手动初始化

		if (serverId >= 0) {
			// 自动初始化的组件。
			autoKey = new AutoKey.Module(this);
			queueModule = new Queue.Module(this);

			// XXX Remove Me
			Conf.ClearInUseAndIAmSureAppStopped(this, Databases);

			// Set Database InUse
			for (var db : Databases.values())
				db.getDirectOperates().SetInUse(serverId, Conf.getGlobalCacheManagerHostNameOrAddress());

			// Open RocksCache
			var dbConf = new Config.DatabaseConf();
			dbConf.setName("zeze_cache_" + serverId);
			dbConf.setDatabaseUrl(dbConf.getName());
			deleteDirectory(new File(dbConf.getDatabaseUrl()));
			dbConf.setDatabaseType(Config.DbType.RocksDb);
			LocalRocksCacheDb = new DatabaseRocksDb(dbConf);
			LocalRocksCacheDb.Open(this);
		}

		// Start ServiceManager
		var serviceManagerConf = Conf != null ? Conf.GetServiceConf(Agent.DefaultServiceName) : null;
		if (serviceManagerConf != null && ServiceManagerAgent != null) {
			ServiceManagerAgent.getClient().Start();
			ServiceManagerAgent.WaitConnectorReady();
		}

		if (serverId >= 0) {
			// Open Databases
			for (var db : Databases.values())
				db.Open(this);

			// Open Global
			var hosts = Str.trim(Conf.getGlobalCacheManagerHostNameOrAddress().split(";"));
			if (hosts.length > 0) {
				var isRaft = hosts[0].endsWith(".xml");
				if (!isRaft) {
					var impl = new GlobalAgent(this, hosts, Conf.getGlobalCacheManagerPort());
					GlobalAgent = impl;
					AchillesHeelDaemon = new AchillesHeelDaemon(this, impl.Agents);
					impl.Start();
				} else {
					var impl = new GlobalCacheManagerWithRaftAgent(this, hosts);
					GlobalAgent = impl;
					AchillesHeelDaemon = new AchillesHeelDaemon(this, impl.Agents);
					impl.Start();
				}
			}

			// Checkpoint
			_checkpoint = new Checkpoint(this, Conf.getCheckpointMode(), Databases.values(), serverId);
			_checkpoint.Start(Conf.getCheckpointPeriod()); // 定时模式可以和其他模式混用。

			/////////////////////////////////////////////////////
			// Schemas
			var defaultDb = GetDatabase(Conf.getDefaultTableConf().getDatabaseName());
			if (Schemas != null) {
				Schemas.Compile();
				var keyOfSchemas = ByteBuffer.Allocate(24);
				keyOfSchemas.WriteString("zeze.Schemas." + serverId);
				while (true) {
					var dataVersion = defaultDb.getDirectOperates().GetDataWithVersion(keyOfSchemas);
					long version = 0;
					if (dataVersion != null && dataVersion.Data != null) {
						var SchemasPrevious = new Schemas();
						try {
							SchemasPrevious.Decode(dataVersion.Data);
							SchemasPrevious.Compile();
						} catch (Throwable ex) {
							SchemasPrevious = null;
							logger.error("Schemas Implement Changed?", ex);
						}
						ResetDB.checkAndRemoveTable(SchemasPrevious, this);
						version = dataVersion.Version;
					}
					var newData = ByteBuffer.Allocate(1024);
					Schemas.Encode(newData);
					var versionRc = defaultDb.getDirectOperates().SaveDataWithSameVersion(keyOfSchemas, newData, version);
					if (versionRc.getValue())
						break;
				}
			}
			// start last
			if (null != AchillesHeelDaemon)
				AchillesHeelDaemon.start();
			IsStart = true;
		}
	}

	public synchronized void Stop() throws Throwable {
		if (!IsStart)
			return;
		IsStart = false;
		logger.info("Stop ServerId={}", Conf != null ? Conf.getServerId() : -1);

		ShutdownHook.remove(this);

		if (null != AchillesHeelDaemon) {
			AchillesHeelDaemon.stopAndJoin();
		}

		if (GlobalAgent != null) {
			GlobalAgent.close();
			GlobalAgent = null;
		}
		if (FlushWhenReduceTimerTask != null) {
			FlushWhenReduceTimerTask.cancel(false);
			FlushWhenReduceTimerTask = null;
		}

		if (_checkpoint != null) {
			_checkpoint.StopAndJoin();
			_checkpoint = null;
		}
		for (var db : Databases.values())
			db.Close();
		if (LocalRocksCacheDb != null) {
			var dir = LocalRocksCacheDb.getDatabaseUrl();
			LocalRocksCacheDb.Close();
			deleteDirectory(new File(dir));
			LocalRocksCacheDb = null;
		}
		if (ServiceManagerAgent != null)
			ServiceManagerAgent.Stop();
		if (queueModule != null) {
			queueModule.UnRegisterZezeTables(this);
			queueModule = null;
		}
		if (autoKey != null) {
			autoKey.UnRegisterZezeTables(this);
			autoKey = null;
		}
		if (Conf != null)
			Conf.ClearInUseAndIAmSureAppStopped(this, Databases);
	}

	public void CheckpointRun() {
		_checkpoint.RunOnce();
	}

	public TaskOneByOneByKey getTaskOneByOneByKey() {
		return TaskOneByOneByKey;
	}

	public void runTaskOneByOneByKey(Object oneByOneKey, String actionName, FuncLong func) {
		TaskOneByOneByKey.Execute(oneByOneKey, NewProcedure(func, actionName), DispatchMode.Normal);
	}

	public void runTaskOneByOneByKey(int oneByOneKey, String actionName, FuncLong func) {
		TaskOneByOneByKey.Execute(oneByOneKey, NewProcedure(func, actionName), DispatchMode.Normal);
	}

	public void runTaskOneByOneByKey(long oneByOneKey, String actionName, FuncLong func) {
		TaskOneByOneByKey.Execute(oneByOneKey, NewProcedure(func, actionName), DispatchMode.Normal);
	}
}
