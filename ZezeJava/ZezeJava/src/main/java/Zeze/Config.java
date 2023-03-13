package Zeze;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import javax.xml.parsers.DocumentBuilderFactory;
import Zeze.Net.ServiceConf;
import Zeze.Transaction.CheckpointFlushMode;
import Zeze.Transaction.CheckpointMode;
import Zeze.Transaction.Database;
import Zeze.Transaction.DatabaseMemory;
import Zeze.Transaction.DatabaseMySql;
import Zeze.Transaction.DatabaseRocksDb;
import Zeze.Transaction.DatabaseSqlServer;
import Zeze.Transaction.DatabaseTikv;
import com.amazonaws.regions.Regions;
import org.apache.logging.log4j.Level;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public final class Config {
	public interface ICustomize {
		String getName();

		void parse(Element self);
	}

	public enum DbType {
		Memory,
		MySql,
		SqlServer,
		Tikv,
		RocksDb,
		DynamoDb,
		Dbh2,
	}

	private String name = "";
	private int scheduledThreads;
	private int workerThreads;
	private int completionPortThreads;
	private int checkpointPeriod = 60000;
	private CheckpointFlushMode checkpointFlushMode = CheckpointFlushMode.SingleThread;
	private int checkpointModeTableFlushConcurrent = 2;
	private int checkpointModeTableFlushSetCount = 50;
	private CheckpointMode checkpointMode = CheckpointMode.Table;
	private Level processReturnErrorLogLevel = Level.INFO;
	private int serverId;
	private boolean noDatabase = false;
	private String globalCacheManagerHostNameOrAddress = "";

	private String serviceManager = ""; // ”“|”raft"|"disable", default: enable service manager
	// raft：本来可以直接在这里配置raftXmlFile。但是，
	// 1. 文件名名字空间属于自定义的，不污染这里了，
	// 2. 这里定义服务类型，raft配置可以留在文件中，
	// 3. raft配置，service配置可以共存，这里配置选择方式。
	// end serviceManager

	// 分成多行配置，支持多HostNameOrAddress或者多raft.xml。
	// 多行的时候，所有服务器的顺序必须保持一致。
	// 为了保持原来接口不变，多行会被编码成一个string保存到GlobalCacheManagerHostNameOrAddress中。
	public GlobalCacheManagersConf globalCacheManagers;
	public ServiceManagerConf serviceManagerConf;

	public String getServiceManager() {
		return serviceManager;
	}

	private int globalCacheManagerPort;
	private final ConcurrentHashMap<String, TableConf> tableConfMap = new ConcurrentHashMap<>();
	private TableConf defaultTableConf;
	private boolean allowReadWhenRecordNotAccessed = true;
	private boolean allowSchemasReuseVariableIdWithSameType = true;
	private boolean fastRedoWhenConflict = false;
	private final ConcurrentHashMap<String, ICustomize> customize = new ConcurrentHashMap<>();
	private boolean autoResetTable = false;
	private final ConcurrentHashMap<String, DatabaseConf> databaseConfMap = new ConcurrentHashMap<>();
	private final ConcurrentHashMap<String, ServiceConf> serviceConfMap = new ConcurrentHashMap<>();
	private ServiceConf defaultServiceConf = new ServiceConf();

	private int onlineLogoutDelay = 60 * 10 * 1000; // 10 minutes

	private int delayRemoveHourStart = 3;
	private int delayRemoveHourEnd = 7;
	private int delayRemoveDays = 7; // a week

	private int offlineTimerLimit = 200;

	public Config() {
	}

	public String getName() {
		return name;
	}

	public int getOfflineTimerLimit() {
		return offlineTimerLimit;
	}

	public int getDelayRemoveHourStart() {
		return delayRemoveHourStart;
	}

	public int getDelayRemoveHourEnd() {
		return delayRemoveHourEnd;
	}

	public int getDelayRemoveDays() {
		return delayRemoveDays;
	}

	public int getOnlineLogoutDelay() {
		return onlineLogoutDelay;
	}

	public void setOnlineLogoutDelay(int delay) {
		onlineLogoutDelay = delay;
	}

	public int getScheduledThreads() {
		return scheduledThreads;
	}

	public int getWorkerThreads() {
		return workerThreads;
	}

	public void setWorkerThreads(int value) {
		workerThreads = value;
	}

	public int getCompletionPortThreads() {
		return completionPortThreads;
	}

	public void setCompletionPortThreads(int value) {
		completionPortThreads = value;
	}

	public int getCheckpointPeriod() {
		return checkpointPeriod;
	}

	public void setCheckpointPeriod(int value) {
		checkpointPeriod = value;
	}

	public CheckpointFlushMode getCheckpointFlushMode() {
		return checkpointFlushMode;
	}

	public void setCheckpointFlushMode(CheckpointFlushMode value) {
		checkpointFlushMode = value;
	}

	public int getCheckpointModeTableFlushConcurrent() {
		return checkpointModeTableFlushConcurrent;
	}

	public void setCheckpointModeTableFlushConcurrent(int value) {
		checkpointModeTableFlushConcurrent = value;
	}

	public int getCheckpointModeTableFlushSetCount() {
		return checkpointModeTableFlushSetCount;
	}

	public void setCheckpointModeTableFlushSetCount(int value) {
		checkpointModeTableFlushSetCount = value;
	}

	public CheckpointMode getCheckpointMode() {
		return checkpointMode;
	}

	public void setCheckpointMode(CheckpointMode value) {
		checkpointMode = value;
	}

	public Level getProcessReturnErrorLogLevel() {
		return processReturnErrorLogLevel;
	}

	public void setProcessReturnErrorLogLevel(Level value) {
		processReturnErrorLogLevel = value;
	}

	public int getServerId() {
		return serverId;
	}

	public void setServerId(int value) {
		serverId = value;
	}

	public boolean isNoDatabase() {
		return noDatabase;
	}

	public void setNoDatabase(boolean value) {
		noDatabase = value;
	}

	public String getGlobalCacheManagerHostNameOrAddress() {
		return globalCacheManagerHostNameOrAddress;
	}

	public void setGlobalCacheManagerHostNameOrAddress(String value) {
		globalCacheManagerHostNameOrAddress = value;
	}

	public GlobalCacheManagersConf getGlobalCacheManagers() {
		return globalCacheManagers;
	}

	public ServiceManagerConf getServiceManagerConf() {
		return serviceManagerConf;
	}

	public int getGlobalCacheManagerPort() {
		return globalCacheManagerPort;
	}

	public void setGlobalCacheManagerPort(int value) {
		globalCacheManagerPort = value;
	}

	public ConcurrentHashMap<String, TableConf> getTableConfMap() {
		return tableConfMap;
	}

	public TableConf getDefaultTableConf() {
		return defaultTableConf;
	}

	public void setDefaultTableConf(TableConf value) {
		defaultTableConf = value;
	}

	public boolean getAllowReadWhenRecordNotAccessed() {
		return allowReadWhenRecordNotAccessed;
	}

	public void setAllowReadWhenRecordNotAccessed(boolean value) {
		allowReadWhenRecordNotAccessed = value;
	}

	public boolean getAllowSchemasReuseVariableIdWithSameType() {
		return allowSchemasReuseVariableIdWithSameType;
	}

	public void setAllowSchemasReuseVariableIdWithSameType(boolean value) {
		allowSchemasReuseVariableIdWithSameType = value;
	}

	public boolean getFastRedoWhenConflict() {
		return fastRedoWhenConflict;
	}

	public void setFastRedoWhenConflict(boolean value) {
		fastRedoWhenConflict = value;
	}

	public ConcurrentHashMap<String, ICustomize> getCustomize() {
		return customize;
	}

	public boolean autoResetTable() {
		return autoResetTable;
	}

	/**
	 * 根据自定义配置名字查找。
	 * 因为外面需要通过AddCustomize注册进来，
	 * 如果外面保存了配置引用，是不需要访问这个接口的。
	 *
	 * <typeparam name="T"></typeparam>
	 */
	@SuppressWarnings("unchecked")
	public <T extends ICustomize> T getCustomize(T customize) {
		var exist = getCustomize().get(customize.getName());
		if (null == exist)
			return customize;
		return (T)exist;
	}

	public Config addCustomize(ICustomize c) {
		if (getCustomize().putIfAbsent(c.getName(), c) != null)
			throw new IllegalStateException("Duplicate Customize Config '" + c.getName() + "'");
		return this;
	}

	public TableConf getTableConf(String name) {
		var tableConf = getTableConfMap().get(name);
		return tableConf != null ? tableConf : getDefaultTableConf();
	}

	public ConcurrentHashMap<String, DatabaseConf> getDatabaseConfMap() {
		return databaseConfMap;
	}

	private static Database createDatabase(Application zeze, DatabaseConf conf) {
		switch (conf.databaseType) {
		case Memory:
			return new DatabaseMemory(conf);
		case MySql:
			return new DatabaseMySql(conf);
		case SqlServer:
			return new DatabaseSqlServer(conf);
		case Tikv:
			return new DatabaseTikv(conf);
		case RocksDb:
			if (!zeze.getConfig().getGlobalCacheManagerHostNameOrAddress().isBlank())
				throw new IllegalStateException("RocksDb Can Not Work With GlobalCacheManager.");
			return new DatabaseRocksDb(conf);
		default:
			throw new UnsupportedOperationException("unknown database type.");
		}
	}

	public void createDatabase(Application zeze, HashMap<String, Database> map) {
		// add other database
		for (var db : getDatabaseConfMap().values())
			map.put(db.name, createDatabase(zeze, db));
	}

	public void clearInUseAndIAmSureAppStopped(Application zeze, HashMap<String, Database> databases) {
		if (databases == null) {
			databases = new HashMap<>();
			createDatabase(zeze, databases);
		}
		for (var db : databases.values())
			db.getDirectOperates().clearInUse(getServerId(), getGlobalCacheManagerHostNameOrAddress());
	}

	public void dropMysqlOperatesProcedures() {
		for (var conf : getDatabaseConfMap().values()) {
			if (conf.databaseType == DbType.MySql) {
				var db = new DatabaseMySql(conf);
				try {
					db.dropOperatesProcedures();
				} finally {
					db.close();
				}
			}
		}
	}

	public ConcurrentHashMap<String, ServiceConf> getServiceConfMap() {
		return serviceConfMap;
	}

	public ServiceConf getDefaultServiceConf() {
		return defaultServiceConf;
	}

	public void setDefaultServiceConf(ServiceConf value) {
		defaultServiceConf = value;
	}

	public ServiceConf getServiceConf(String name) {
		return getServiceConfMap().get(name);
	}

	/**
	 * 由于这个方法没法加入Customize配置，为了兼容和内部测试保留，
	 * 应用应该自己LoadAndParse。
	 * var c = new Config();
	 * c.AddCustomize(...);
	 * c.LoadAndParse();
	 */

	public static Config load() {
		return load("zeze.xml");
	}

	public static Config load(String xmlFile) {
		return new Config().loadAndParse(xmlFile);
	}

	public Config loadAndParse() {
		return loadAndParse("zeze.xml");
	}

	public Config loadAndParse(String xmlFile) {
		if (new File(xmlFile).isFile()) {
			DocumentBuilderFactory db = DocumentBuilderFactory.newInstance();
			db.setXIncludeAware(true);
			db.setNamespaceAware(true);
			try {
				Document doc = db.newDocumentBuilder().parse(xmlFile);
				parse(doc.getDocumentElement());
			} catch (Exception ex) {
				throw new RuntimeException(ex);
			}
		}
		if (getDefaultTableConf() == null)
			setDefaultTableConf(new TableConf());
		if (getDatabaseConfMap().isEmpty()) { // add default databaseConf.
			if (getDatabaseConfMap().putIfAbsent("", new DatabaseConf()) != null)
				throw new IllegalStateException("Concurrent Add Default Database.");
		}
		return this;
	}

	public void parse(Element self) {
		if (!self.getNodeName().equals("zeze"))
			throw new IllegalStateException("is it a zeze config?");
		String name = self.getAttribute("name");
		if (!name.isBlank())
			this.name = name;

		setCheckpointPeriod(Integer.parseInt(self.getAttribute("CheckpointPeriod")));
		setServerId(Integer.parseInt(self.getAttribute("ServerId")));
		noDatabase = self.getAttribute("NoDatabase").trim().equalsIgnoreCase("true");

		setGlobalCacheManagerHostNameOrAddress(self.getAttribute("GlobalCacheManagerHostNameOrAddress").trim());
		String attr = self.getAttribute("GlobalCacheManagerPort");
		setGlobalCacheManagerPort(attr.isBlank() ? 0 : Integer.parseInt(attr));

		attr = self.getAttribute("OnlineLogoutDelay");
		if (!attr.isBlank())
			onlineLogoutDelay = Integer.parseInt(attr);

		attr = self.getAttribute("CheckpointModeTableFlushConcurrent");
		if (!attr.isBlank())
			checkpointModeTableFlushConcurrent = Integer.parseInt(attr);

		attr = self.getAttribute("CheckpointModeTableFlushSetCount");
		if (!attr.isBlank())
			checkpointModeTableFlushSetCount = Integer.parseInt(attr);

		attr = self.getAttribute("ProcessReturnErrorLogLevel");
		if (!attr.isBlank())
			setProcessReturnErrorLogLevel(Level.toLevel(attr));

		attr = self.getAttribute("WorkerThreads");
		setWorkerThreads(attr.isBlank() ? -1 : Integer.parseInt(attr));

		attr = self.getAttribute("ScheduledThreads");
		scheduledThreads = attr.isBlank() ? -1 : Integer.parseInt(attr);

		attr = self.getAttribute("CompletionPortThreads");
		setCompletionPortThreads(attr.isBlank() ? -1 : Integer.parseInt(attr));

		attr = self.getAttribute("AllowReadWhenRecordNotAccessed");
		setAllowReadWhenRecordNotAccessed(attr.isBlank() || Boolean.parseBoolean(attr));
		attr = self.getAttribute("AllowSchemasReuseVariableIdWithSameType");
		setAllowSchemasReuseVariableIdWithSameType(attr.isBlank() || Boolean.parseBoolean(attr));

		attr = self.getAttribute("FastRedoWhenConflict");
		setFastRedoWhenConflict(attr.isBlank() || Boolean.parseBoolean(attr));

		attr = self.getAttribute("CheckpointMode");
		if (!attr.isBlank())
			setCheckpointMode(CheckpointMode.valueOf(attr));
		attr = self.getAttribute("CheckpointFlushMode");
		if (!attr.isBlank())
			setCheckpointFlushMode(CheckpointFlushMode.valueOf(attr));

		if (checkpointMode == CheckpointMode.Period && !globalCacheManagerHostNameOrAddress.isBlank()) {
			Application.logger.warn("CheckpointMode.Period Cannot Work With Global. Change To CheckpointMode.Table Now.");
			checkpointMode = CheckpointMode.Table;
		}
		if (checkpointMode == CheckpointMode.Immediately)
			throw new UnsupportedOperationException();

		attr = self.getAttribute("AutoResetTable");
		if (!attr.isBlank())
			autoResetTable = Boolean.parseBoolean(attr);

		attr = self.getAttribute("DelayRemoveHourStart");
		if (!attr.isBlank())
			delayRemoveHourStart = Integer.parseInt(attr);

		attr = self.getAttribute("DelayRemoveHourEnd");
		if (!attr.isBlank())
			delayRemoveHourEnd = Integer.parseInt(attr);

		attr = self.getAttribute("DelayRemoveDays");
		if (!attr.isBlank())
			delayRemoveDays = Integer.parseInt(attr);

		attr = self.getAttribute("OfflineTimerLimit");
		if (!attr.isBlank())
			offlineTimerLimit = Integer.parseInt(attr);

		serviceManager = self.getAttribute("ServiceManager").trim();

		NodeList childNodes = self.getChildNodes();
		for (int i = 0; i < childNodes.getLength(); i++) {
			Node node = childNodes.item(i);
			if (node.getNodeType() != Node.ELEMENT_NODE)
				continue;

			Element e = (Element)node;
			switch (e.getNodeName()) {
			case "GlobalCacheManagersConf":
				new GlobalCacheManagersConf(this, e);
				break;

			case "ServiceManagerConf":
				new ServiceManagerConf(this, e);
				break;

			case "TableConf":
				new TableConf(this, e);
				break;

			case "DatabaseConf":
				new DatabaseConf(this, e);
				break;

			case "ServiceConf":
				new ServiceConf(this, e);
				break;

			case "CustomizeConf":
				var cname = e.getAttribute("Name").trim();
				var customizeConf = getCustomize().get(cname);
				if (null == customizeConf)
					throw new UnsupportedOperationException("Unknown CustomizeConf Name='" + cname + "'");

				customizeConf.parse(e);
				break;

			default:
				throw new UnsupportedOperationException("unknown node name: " + e.getNodeName());
			}
		}
		if (globalCacheManagerHostNameOrAddress.equals("GlobalCacheManagersConf"))
			globalCacheManagerHostNameOrAddress = globalCacheManagers.toString();
	}

	public static final class ServiceManagerConf {
		private String sessionName;
		private String raftXml;
		private long loginTimeout = 12000;

		public ServiceManagerConf(Config conf, Element self) {
			sessionName = self.getAttribute("sessionName").trim();
			raftXml = self.getAttribute("raftXml").trim();
			String attr = self.getAttribute("loginTimeout").trim();
			if (!attr.isBlank())
				loginTimeout = Long.parseLong(attr);

			conf.serviceManagerConf = this;
		}

		public String getRaftXml() {
			return raftXml;
		}

		public String getSessionName() {
			return sessionName;
		}

		public long getLoginTimeout() {
			return loginTimeout;
		}

		public void setRaftXml(String raftXml) {
			this.raftXml = raftXml;
		}

		public void setSessionName(String sessionName) {
			this.sessionName = sessionName;
		}

		public void setLoginTimeout(long timeout) {
			loginTimeout = timeout;
		}
	}

	public static final class GlobalCacheManagersConf {
		private final List<String> hosts = new ArrayList<>();

		public GlobalCacheManagersConf(Config conf, Element self) {
			NodeList childNodes = self.getChildNodes();
			for (int i = 0, n = childNodes.getLength(); i < n; i++) {
				Node node = childNodes.item(i);
				if (node.getNodeType() != Node.ELEMENT_NODE)
					continue;

				Element e = (Element)node;
				//noinspection SwitchStatementWithTooFewBranches
				switch (e.getNodeName()) {
				case "host":
					var attr = e.getAttribute("name").trim();
					hosts.add(attr);
					break;
				default:
					throw new UnsupportedOperationException("unknown node name: " + e.getNodeName());
				}
			}
			if (conf.globalCacheManagers != null)
				throw new IllegalStateException("too many GlobalCacheManagersConf.");
			conf.globalCacheManagers = this;
		}

		@Override
		public String toString() {
			var sb = new StringBuilder();
			boolean first = true;
			for (var host : hosts) {
				if (first)
					first = false;
				else
					sb.append(";");
				sb.append(host);
			}
			return sb.toString();
		}
	}

	public static final class DynamoConf {
		public Regions region = Regions.CN_NORTH_1;

		public DynamoConf() {
		}

		public DynamoConf(Element self) {
			var attr = self.getAttribute("region");
			if (!attr.isBlank())
				region = Regions.valueOf(attr);
		}
	}

	public static final class DruidConf {
		public String driverClassName;
		public Integer initialSize;
		public Integer maxActive;
		public Integer maxIdle;
		public Integer minIdle;
		public Long maxWait;
		public Integer phyMaxUseCount;
		public Long phyTimeoutMillis;

		public Integer maxOpenPreparedStatements;

		public String userName;
		public String password;

		private static String EmptyToNullString(String attr) {
			var trim = attr.trim();
			return trim.isBlank() ? null : trim;
		}

		private static Integer EmptyToNullInteger(String attr) {
			var str = EmptyToNullString(attr);
			return str == null ? null : Integer.parseInt(str);
		}

		private static Long EmptyToNullLong(String attr) {
			var str = EmptyToNullString(attr);
			return str == null ? null : Long.parseLong(str);
		}

		public DruidConf() {
		}

		public DruidConf(Element self) {
			driverClassName = EmptyToNullString(self.getAttribute("DriverClassName"));
			initialSize = EmptyToNullInteger(self.getAttribute("InitialSize"));
			maxActive = EmptyToNullInteger(self.getAttribute("MaxActive"));
			maxIdle = EmptyToNullInteger(self.getAttribute("MaxIdle"));
			minIdle = EmptyToNullInteger(self.getAttribute("MinIdle"));
			maxWait = EmptyToNullLong(self.getAttribute("MaxWait"));
			maxOpenPreparedStatements = EmptyToNullInteger(self.getAttribute("MaxOpenPreparedStatements"));
			phyMaxUseCount = EmptyToNullInteger(self.getAttribute("PhyMaxUseCount"));
			phyTimeoutMillis = EmptyToNullLong(self.getAttribute("PhyTimeoutMillis"));

			userName = EmptyToNullString(self.getAttribute("UserName"));
			password = EmptyToNullString(self.getAttribute("Password"));
		}
	}

	public static final class DatabaseConf {
		private String name = "";
		private DbType databaseType = DbType.Memory;
		private String databaseUrl = "";
		private DruidConf druidConf; // only valid when jdbc: mysql, sqlserver,
		private DynamoConf dynamoConf; // only valid when dynamodb
		private boolean distTxn; // 是否启用分布式事务(目前仅TiKV支持)
		private boolean disableOperates;

		public String getName() {
			return name;
		}

		public DbType getDatabaseType() {
			return databaseType;
		}

		public String getDatabaseUrl() {
			return databaseUrl;
		}

		public void setName(String name) {
			this.name = name;
		}

		public void setDatabaseType(DbType databaseType) {
			this.databaseType = databaseType;
		}

		public DynamoConf getDynamoConf() {
			return dynamoConf;
		}

		public void setDynamoConf(DynamoConf conf) {
			dynamoConf = conf;
		}

		public void setDatabaseUrl(String databaseUrl) {
			this.databaseUrl = databaseUrl;
		}

		public DruidConf getDruidConf() {
			return druidConf;
		}

		public void setDruidConf(DruidConf conf) {
			druidConf = conf;
		}

		public boolean isDistTxn() {
			return distTxn;
		}

		public boolean isDisableOperates() {
			return disableOperates;
		}

		public void setDistTxn(boolean distTxn) {
			this.distTxn = distTxn;
		}

		public void setDisableOperates(boolean disableOperates) {
			this.disableOperates = disableOperates;
		}

		public DatabaseConf() {
		}

		public DatabaseConf(Config conf, Element self) {
			name = self.getAttribute("Name").trim();
			switch (self.getAttribute("DatabaseType").trim()) {
			case "Memory":
				// DatabaseType = DbType.Memory;
				break;
			case "MySql":
				databaseType = DbType.MySql;
				druidConf = new DruidConf(self);
				if (null == druidConf.driverClassName)
					druidConf.driverClassName = "com.mysql.cj.jdbc.Driver";
				break;
			case "SqlServer":
				databaseType = DbType.SqlServer;
				druidConf = new DruidConf(self);
				if (null == druidConf.driverClassName)
					druidConf.driverClassName = "com.microsoft.sqlserver.jdbc.SQLServerDriver";
				break;
			case "Tikv":
				databaseType = DbType.Tikv;
				break;
			case "RocksDB":
				databaseType = DbType.RocksDb;
				break;
			case "DynamoDB":
				databaseType = DbType.DynamoDb;
				dynamoConf = new DynamoConf(self);
				break;
			default:
				throw new UnsupportedOperationException("unknown database type.");
			}
			databaseUrl = self.getAttribute("DatabaseUrl").trim();
			distTxn = "true".equalsIgnoreCase(self.getAttribute("distTxn").trim());
			disableOperates = "true".equalsIgnoreCase(self.getAttribute("DisableOperates").trim());

			if (conf.getDatabaseConfMap().putIfAbsent(getName(), this) != null)
				throw new IllegalStateException("Duplicate Database '" + getName() + "'");
		}
	}

	public static final class TableConf {
		private String name;
		private int cacheCapacity = 20000;
		private int cacheConcurrencyLevel;
		private int cacheInitialCapacity;
		private int cacheNewAccessHotThreshold;
		private float cacheFactor = 5.0f;

		private int cacheCleanPeriod = 10000;
		private int cacheNewLruHotPeriod = 10000;
		private int cacheMaxLruInitialCapacity = 100000;
		private int cacheCleanPeriodWhenExceedCapacity = 1000;
		private boolean checkpointWhenCommit;
		// 自动倒库，当新库(DatabaseName)没有找到记录时，从旧库(DatabaseOldName)中读取，
		// Open 的时候找到旧库并打开Database.Table用来读取。
		// 内存表不支持倒库。
		private String databaseName = "";
		private String databaseOldName = "";
		private int databaseOldMode;

		public String getName() {
			return name;
		}

		public int getRealCacheCapacity() {
			return (int)(cacheCapacity * cacheFactor);
		}

		public int getCacheCapacity() {
			return cacheCapacity;
		}

		public void setCacheCapacity(int value) {
			cacheCapacity = value;
		}

		public float getCacheFactor() {
			return cacheFactor;
		}

		public void setCacheFactor(float factor) {
			cacheFactor = factor;
		}

		public int getCacheConcurrencyLevel() {
			return cacheConcurrencyLevel;
		}

		public void setCacheConcurrencyLevel(int value) {
			cacheConcurrencyLevel = value;
		}

		public int getCacheInitialCapacity() {
			return cacheInitialCapacity;
		}

		public void setCacheInitialCapacity(int value) {
			cacheInitialCapacity = value;
		}

		public int getCacheNewAccessHotThreshold() {
			return cacheNewAccessHotThreshold;
		}

		public void setCacheNewAccessHotThreshold(int value) {
			cacheNewAccessHotThreshold = value;
		}

		public int getCacheCleanPeriod() {
			return cacheCleanPeriod;
		}

		public void setCacheCleanPeriod(int value) {
			cacheCleanPeriod = value;
		}

		public int getCacheNewLruHotPeriod() {
			return cacheNewLruHotPeriod;
		}

		public void setCacheNewLruHotPeriod(int value) {
			cacheNewLruHotPeriod = value;
		}

		public int getCacheMaxLruInitialCapacity() {
			return cacheMaxLruInitialCapacity;
		}

		public void setCacheMaxLruInitialCapacity(int value) {
			cacheMaxLruInitialCapacity = value;
		}

		public int getCacheCleanPeriodWhenExceedCapacity() {
			return cacheCleanPeriodWhenExceedCapacity;
		}

		public void setCacheCleanPeriodWhenExceedCapacity(int value) {
			cacheCleanPeriodWhenExceedCapacity = value;
		}

		public boolean getCheckpointWhenCommit() {
			return checkpointWhenCommit;
		}

		public void setCheckpointWhenCommit(boolean value) {
			checkpointWhenCommit = value;
		}

		public String getDatabaseName() {
			return databaseName;
		}

		public String getDatabaseOldName() {
			return databaseOldName;
		}

		public int getDatabaseOldMode() {
			return databaseOldMode;
		}

		public TableConf() {
		}

		public TableConf(Config conf, Element self) {
			name = self.getAttribute("Name").trim();

			String attr = self.getAttribute("CacheCapacity");
			if (!attr.isBlank())
				setCacheCapacity(Integer.parseInt(attr));
			attr = self.getAttribute("CacheCleanPeriod");
			if (!attr.isBlank())
				setCacheCleanPeriod(Integer.parseInt(attr));
			attr = self.getAttribute("CacheFactor");
			if (!attr.isBlank())
				setCacheFactor(Float.parseFloat(attr));

			databaseName = self.getAttribute("DatabaseName").trim();
			databaseOldName = self.getAttribute("DatabaseOldName").trim();
			attr = self.getAttribute("DatabaseOldMode");
			databaseOldMode = attr.isBlank() ? 0 : Integer.parseInt(attr);

			attr = self.getAttribute("CheckpointWhenCommit");
			if (!attr.isBlank())
				setCheckpointWhenCommit(Boolean.parseBoolean(attr));
			attr = self.getAttribute("CacheConcurrencyLevel");
			if (!attr.isBlank())
				setCacheConcurrencyLevel(Integer.parseInt(attr));
			attr = self.getAttribute("CacheInitialCapacity");
			if (!attr.isBlank())
				setCacheInitialCapacity(Integer.parseInt(attr));
			attr = self.getAttribute("CacheNewAccessHotThreshold");
			if (!attr.isBlank())
				setCacheNewAccessHotThreshold(Integer.parseInt(attr));
			attr = self.getAttribute("CacheCleanPeriodWhenExceedCapacity");
			if (!attr.isBlank())
				setCacheCleanPeriodWhenExceedCapacity(Integer.parseInt(attr));
			attr = self.getAttribute("CacheMaxLruInitialCapacity");
			if (!attr.isBlank())
				setCacheMaxLruInitialCapacity(Integer.parseInt(attr));

			if (getName().length() > 0) {
				if (conf.getTableConfMap().putIfAbsent(getName(), this) != null)
					throw new IllegalStateException("Duplicate Table '" + getName() + "'");
			} else if (conf.getDefaultTableConf() == null)
				conf.setDefaultTableConf(this);
			else
				throw new IllegalStateException("too many DefaultTableConf.");
		}
	}
}
