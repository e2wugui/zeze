package Zeze;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import javax.xml.parsers.DocumentBuilderFactory;
import Zeze.Game.Online;
import Zeze.Net.ServiceConf;
import Zeze.Transaction.CheckpointMode;
import Zeze.Transaction.Database;
import Zeze.Transaction.DatabaseMemory;
import Zeze.Transaction.DatabaseMySql;
import Zeze.Transaction.DatabaseRocksDb;
import Zeze.Transaction.DatabaseSqlServer;
import Zeze.Transaction.DatabaseTikv;
import org.apache.logging.log4j.Level;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public final class Config {
	public interface ICustomize {
		String getName();

		void Parse(Element self);
	}

	public enum DbType {
		Memory,
		MySql,
		SqlServer,
		Tikv,
		RocksDb,
	}

	private String name = "";
	private int ScheduledThreads;
	private int WorkerThreads;
	private int CompletionPortThreads;
	private int CheckpointPeriod = 60000;
	private int CheckpointModeTableFlushConcurrent = 2;
	private int CheckpointModeTableFlushSetCount = 100;
	private CheckpointMode checkpointMode = CheckpointMode.Table;
	private Level ProcessReturnErrorLogLevel = Level.INFO;
	private int ServerId;
	private String GlobalCacheManagerHostNameOrAddress = "";
	// 分成多行配置，支持多HostNameOrAddress或者多raft.xml。
	// 多行的时候，所有服务器的顺序必须保持一致。
	// 为了保持原来接口不变，多行会被编码成一个string保存到GlobalCacheManagerHostNameOrAddress中。
	public GlobalCacheManagersConf GlobalCacheManagers;
	private int GlobalCacheManagerPort;
	private final ConcurrentHashMap<String, TableConf> TableConfMap = new ConcurrentHashMap<>();
	private TableConf DefaultTableConf;
	private boolean AllowReadWhenRecordNotAccessed = true;
	private boolean AllowSchemasReuseVariableIdWithSameType = true;
	private boolean FastRedoWhenConflict = false;
	private final ConcurrentHashMap<String, ICustomize> Customize = new ConcurrentHashMap<>();
	private boolean AutoResetTable = false;
	private final ConcurrentHashMap<String, DatabaseConf> DatabaseConfMap = new ConcurrentHashMap<>();
	private final ConcurrentHashMap<String, ServiceConf> ServiceConfMap = new ConcurrentHashMap<>();
	private ServiceConf DefaultServiceConf = new ServiceConf();

	private int OnlineLogoutDelay = 60 * 10 * 1000; // 10 minutes

	public Config() {
	}

	public String getName() {
		return name;
	}

	public int getOnlineLogoutDelay() {
		return OnlineLogoutDelay;
	}

	public void setOnlineLogoutDelay(int delay) {
		OnlineLogoutDelay = delay;
	}

	public int getScheduledThreads() {
		return ScheduledThreads;
	}

	public int getWorkerThreads() {
		return WorkerThreads;
	}

	public void setWorkerThreads(int value) {
		WorkerThreads = value;
	}

	public int getCompletionPortThreads() {
		return CompletionPortThreads;
	}

	public void setCompletionPortThreads(int value) {
		CompletionPortThreads = value;
	}

	public int getCheckpointPeriod() {
		return CheckpointPeriod;
	}

	public void setCheckpointPeriod(int value) {
		CheckpointPeriod = value;
	}

	public int getCheckpointModeTableFlushConcurrent() { return CheckpointModeTableFlushConcurrent; }

	public void setCheckpointModeTableFlushConcurrent(int value) { CheckpointModeTableFlushConcurrent = value; }

	public int getCheckpointModeTableFlushSetCount() { return CheckpointModeTableFlushSetCount; }

	public void setCheckpointModeTableFlushSetCount(int value) { CheckpointModeTableFlushSetCount = value; }

	public CheckpointMode getCheckpointMode() {
		return checkpointMode;
	}

	public void setCheckpointMode(CheckpointMode value) {
		checkpointMode = value;
	}

	public Level getProcessReturnErrorLogLevel() {
		return ProcessReturnErrorLogLevel;
	}

	public void setProcessReturnErrorLogLevel(Level value) {
		ProcessReturnErrorLogLevel = value;
	}

	public int getServerId() {
		return ServerId;
	}

	public void setServerId(int value) {
		ServerId = value;
	}

	public String getGlobalCacheManagerHostNameOrAddress() {
		return GlobalCacheManagerHostNameOrAddress;
	}

	public void setGlobalCacheManagerHostNameOrAddress(String value) {
		GlobalCacheManagerHostNameOrAddress = value;
	}

	public GlobalCacheManagersConf getGlobalCacheManagers() {
		return GlobalCacheManagers;
	}

	public int getGlobalCacheManagerPort() {
		return GlobalCacheManagerPort;
	}

	public void setGlobalCacheManagerPort(int value) {
		GlobalCacheManagerPort = value;
	}

	public ConcurrentHashMap<String, TableConf> getTableConfMap() {
		return TableConfMap;
	}

	public TableConf getDefaultTableConf() {
		return DefaultTableConf;
	}

	public void setDefaultTableConf(TableConf value) {
		DefaultTableConf = value;
	}

	public boolean getAllowReadWhenRecordNotAccessed() {
		return AllowReadWhenRecordNotAccessed;
	}

	public void setAllowReadWhenRecordNotAccessed(boolean value) {
		AllowReadWhenRecordNotAccessed = value;
	}

	public boolean getAllowSchemasReuseVariableIdWithSameType() {
		return AllowSchemasReuseVariableIdWithSameType;
	}

	public void setAllowSchemasReuseVariableIdWithSameType(boolean value) {
		AllowSchemasReuseVariableIdWithSameType = value;
	}

	public boolean getFastRedoWhenConflict() {
		return FastRedoWhenConflict;
	}

	public void setFastRedoWhenConflict(boolean value) {
		FastRedoWhenConflict = value;
	}

	public ConcurrentHashMap<String, ICustomize> getCustomize() {
		return Customize;
	}

	public boolean autoResetTable() {
		return AutoResetTable;
	}

	/**
	 * 根据自定义配置名字查找。
	 * 因为外面需要通过AddCustomize注册进来，
	 * 如果外面保存了配置引用，是不需要访问这个接口的。
	 *
	 * <typeparam name="T"></typeparam>
	 */
	@SuppressWarnings("unchecked")
	public <T extends ICustomize> T GetCustomize(T customize) {
		var exist = getCustomize().get(customize.getName());
		if (null == exist)
			return customize;
		return (T)exist;
	}

	public Config AddCustomize(ICustomize c) {
		if (getCustomize().putIfAbsent(c.getName(), c) != null)
			throw new IllegalStateException("Duplicate Customize Config '" + c.getName() + "'");
		return this;
	}

	public TableConf GetTableConf(String name) {
		var tableConf = getTableConfMap().get(name);
		return tableConf != null ? tableConf : getDefaultTableConf();
	}

	public ConcurrentHashMap<String, DatabaseConf> getDatabaseConfMap() {
		return DatabaseConfMap;
	}

	private static Database CreateDatabase(Application zeze, DatabaseConf conf) {
		switch (conf.DatabaseType) {
		case Memory:
			return new DatabaseMemory(conf);
		case MySql:
			return new DatabaseMySql(conf);
		case SqlServer:
			return new DatabaseSqlServer(conf);
		case Tikv:
			return new DatabaseTikv(conf);
		case RocksDb:
			if (!zeze.getConfig().getGlobalCacheManagerHostNameOrAddress().isEmpty())
				throw new IllegalStateException("RocksDb Can Not Work With GlobalCacheManager.");
			return new DatabaseRocksDb(conf);
		default:
			throw new UnsupportedOperationException("unknown database type.");
		}
	}

	public void CreateDatabase(Application zeze, HashMap<String, Database> map) {
		// add other database
		for (var db : getDatabaseConfMap().values())
			map.put(db.Name, CreateDatabase(zeze, db));
	}

	public void ClearInUseAndIAmSureAppStopped(Application zeze, HashMap<String, Database> databases) {
		if (databases == null) {
			databases = new HashMap<>();
			CreateDatabase(zeze, databases);
		}
		for (var db : databases.values())
			db.getDirectOperates().ClearInUse(getServerId(), getGlobalCacheManagerHostNameOrAddress());
	}

	public ConcurrentHashMap<String, ServiceConf> getServiceConfMap() {
		return ServiceConfMap;
	}

	public ServiceConf getDefaultServiceConf() {
		return DefaultServiceConf;
	}

	public void setDefaultServiceConf(ServiceConf value) {
		DefaultServiceConf = value;
	}

	public ServiceConf GetServiceConf(String name) {
		return getServiceConfMap().get(name);
	}

	/**
	 * 由于这个方法没法加入Customize配置，为了兼容和内部测试保留，
	 * 应用应该自己LoadAndParse。
	 * var c = new Config();
	 * c.AddCustomize(...);
	 * c.LoadAndParse();
	 */

	public static Config Load() {
		return Load("zeze.xml");
	}

	public static Config Load(String xmlFile) {
		return new Config().LoadAndParse(xmlFile);
	}

	public Config LoadAndParse() {
		return LoadAndParse("zeze.xml");
	}

	public Config LoadAndParse(String xmlFile) {
		if (new File(xmlFile).isFile()) {
			DocumentBuilderFactory db = DocumentBuilderFactory.newInstance();
			db.setXIncludeAware(true);
			db.setNamespaceAware(true);
			try {
				Document doc = db.newDocumentBuilder().parse(xmlFile);
				Parse(doc.getDocumentElement());
			} catch (Throwable ex) {
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

	public void Parse(Element self) {
		if (!self.getNodeName().equals("zeze"))
			throw new IllegalStateException("is it a zeze config?");
		String name = self.getAttribute("name");
		if (name.length() > 0)
			this.name = name;

		setCheckpointPeriod(Integer.parseInt(self.getAttribute("CheckpointPeriod")));
		setServerId(Integer.parseInt(self.getAttribute("ServerId")));

		setGlobalCacheManagerHostNameOrAddress(self.getAttribute("GlobalCacheManagerHostNameOrAddress"));
		String attr = self.getAttribute("GlobalCacheManagerPort");
		setGlobalCacheManagerPort(attr.length() > 0 ? Integer.parseInt(attr) : 0);

		attr = self.getAttribute("OnlineLogoutDelay");
		if (!attr.isEmpty())
			OnlineLogoutDelay = Integer.parseInt(attr);

		attr = self.getAttribute("CheckpointModeTableFlushConcurrent");
		if (!attr.isEmpty())
			CheckpointModeTableFlushConcurrent = Integer.parseInt(attr);

		attr = self.getAttribute("CheckpointModeTableFlushSetCount");
		if (!attr.isEmpty())
			CheckpointModeTableFlushSetCount = Integer.parseInt(attr);

		attr = self.getAttribute("ProcessReturnErrorLogLevel");
		if (!attr.isEmpty())
			setProcessReturnErrorLogLevel(Level.toLevel(attr));

		attr = self.getAttribute("WorkerThreads");
		setWorkerThreads(attr.length() > 0 ? Integer.parseInt(attr) : -1);

		attr = self.getAttribute("ScheduledThreads");
		ScheduledThreads = attr.length() > 0 ? Integer.parseInt(attr) : -1;

		attr = self.getAttribute("CompletionPortThreads");
		setCompletionPortThreads(attr.length() > 0 ? Integer.parseInt(attr) : -1);

		attr = self.getAttribute("AllowReadWhenRecordNotAccessed");
		setAllowReadWhenRecordNotAccessed(attr.length() <= 0 || Boolean.parseBoolean(attr));
		attr = self.getAttribute("AllowSchemasReuseVariableIdWithSameType");
		setAllowSchemasReuseVariableIdWithSameType(attr.length() <= 0 || Boolean.parseBoolean(attr));

		attr = self.getAttribute("FastRedoWhenConflict");
		setFastRedoWhenConflict((attr.length() <= 0 || Boolean.parseBoolean(attr)));

		attr = self.getAttribute("CheckpointMode");
		if (!attr.isEmpty())
			setCheckpointMode(CheckpointMode.valueOf(attr));
		if (checkpointMode == CheckpointMode.Period && !GlobalCacheManagerHostNameOrAddress.isEmpty()) {
			Application.logger.warn("CheckpointMode.Period Cannot Work With Global. Change To CheckpointMode.Table Now.");
			checkpointMode = CheckpointMode.Table;
		}
		if (checkpointMode == CheckpointMode.Immediately)
			throw new UnsupportedOperationException();

		attr = self.getAttribute("AutoResetTable");
		if (!attr.isEmpty())
			AutoResetTable = Boolean.parseBoolean(attr);

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
				var cname = e.getAttribute("Name");
				var customizeConf = getCustomize().get(cname);
				if (null == customizeConf)
					throw new UnsupportedOperationException("Unknown CustomizeConf Name='" + cname + "'");

				customizeConf.Parse(e);
				break;

			default:
				throw new UnsupportedOperationException("unknown node name: " + e.getNodeName());
			}
		}
		if (GlobalCacheManagerHostNameOrAddress.equals("GlobalCacheManagersConf"))
			GlobalCacheManagerHostNameOrAddress = GlobalCacheManagers.toString();
	}

	public static final class GlobalCacheManagersConf {
		private final List<String> Hosts = new ArrayList<>();

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
					Hosts.add(attr);
					break;
				default:
					throw new UnsupportedOperationException("unknown node name: " + e.getNodeName());
				}
			}
			if (conf.GlobalCacheManagers != null)
				throw new IllegalStateException("too many GlobalCacheManagersConf.");
			conf.GlobalCacheManagers = this;
		}

		@Override
		public String toString() {
			var sb = new StringBuilder();
			boolean first = true;
			for (var host : Hosts) {
				if (first)
					first = false;
				else
					sb.append(";");
				sb.append(host);
			}
			return sb.toString();
		}
	}

	public static final class DbcpConf {
		public String DriverClassName;
		public Integer InitialSize;
		public Integer MaxTotal;
		public Integer MaxIdle;
		public Integer MinIdle;
		public Long MaxWaitMillis;

		public String UserName;
		public String Password;

		private static String EmptyToNullString(String attr) {
			var trim = attr.trim();
			return trim.isEmpty() ? null : trim;
		}

		private static Integer EmptyToNullInteger(String attr) {
			var str = EmptyToNullString(attr);
			return str == null ? null : Integer.parseInt(str);
		}

		private static Long EmptyToNullLong(String attr) {
			var str = EmptyToNullString(attr);
			return str == null ? null : Long.parseLong(str);
		}

		public DbcpConf() {
		}

		public DbcpConf(Element self) {
			DriverClassName = EmptyToNullString(self.getAttribute("DriverClassName"));
			InitialSize = EmptyToNullInteger(self.getAttribute("InitialSize"));
			MaxTotal = EmptyToNullInteger(self.getAttribute("MaxTotal"));
			MaxIdle = EmptyToNullInteger(self.getAttribute("MaxIdle"));
			MinIdle = EmptyToNullInteger(self.getAttribute("MinIdle"));
			MaxWaitMillis = EmptyToNullLong(self.getAttribute("MaxWaitMillis"));

			UserName = EmptyToNullString(self.getAttribute("UserName"));
			Password = EmptyToNullString(self.getAttribute("Password"));
		}
	}

	public static final class DatabaseConf {
		private String Name = "";
		private DbType DatabaseType = DbType.Memory;
		private String DatabaseUrl = "";
		private DbcpConf DbcpConf; // only valid when jdbc: mysql, sqlserver,
		private boolean distTxn; // 是否启用分布式事务(目前仅TiKV支持)

		public String getName() {
			return Name;
		}

		public DbType getDatabaseType() {
			return DatabaseType;
		}

		public String getDatabaseUrl() {
			return DatabaseUrl;
		}

		public void setName(String name) {
			Name = name;
		}

		public void setDatabaseType(DbType databaseType) {
			DatabaseType = databaseType;
		}

		public void setDatabaseUrl(String databaseUrl) {
			DatabaseUrl = databaseUrl;
		}

		public DbcpConf getDbcpConf() {
			return DbcpConf;
		}

		public void setDbcpConf(DbcpConf conf) {
			DbcpConf = conf;
		}

		public boolean isDistTxn() {
			return distTxn;
		}

		public void setDistTxn(boolean distTxn) {
			this.distTxn = distTxn;
		}

		public DatabaseConf() {
		}

		public DatabaseConf(Config conf, Element self) {
			Name = self.getAttribute("Name");
			switch (self.getAttribute("DatabaseType")) {
			case "Memory":
				// DatabaseType = DbType.Memory;
				break;
			case "MySql":
				DatabaseType = DbType.MySql;
				DbcpConf = new DbcpConf(self);
				if (null == DbcpConf.DriverClassName)
					DbcpConf.DriverClassName = "com.mysql.cj.jdbc.Driver";
				break;
			case "SqlServer":
				DatabaseType = DbType.SqlServer;
				DbcpConf = new DbcpConf(self);
				if (null == DbcpConf.DriverClassName)
					DbcpConf.DriverClassName = "com.microsoft.sqlserver.jdbc.SQLServerDriver";
				break;
			case "Tikv":
				DatabaseType = DbType.Tikv;
				break;
			case "RocksDB":
				DatabaseType = DbType.RocksDb;
				break;
			default:
				throw new UnsupportedOperationException("unknown database type.");
			}
			DatabaseUrl = self.getAttribute("DatabaseUrl");
			distTxn = "true".equalsIgnoreCase(self.getAttribute("distTxn"));

			if (conf.getDatabaseConfMap().putIfAbsent(getName(), this) != null)
				throw new IllegalStateException("Duplicate Database '" + getName() + "'");
		}
	}

	public static final class TableConf {
		private String Name;
		private int CacheCapacity = 20000;
		private int CacheConcurrencyLevel;
		private int CacheInitialCapacity;
		private int CacheNewAccessHotThreshold;
		private float CacheFactor = 5.0f;

		public String getName() {
			return Name;
		}

		public int getRealCacheCapacity() {
			return (int)(CacheCapacity * CacheFactor);
		}

		public int getCacheCapacity() {
			return CacheCapacity;
		}

		public void setCacheCapacity(int value) {
			CacheCapacity = value;
		}

		public float getCacheFactor() {
			return CacheFactor;
		}

		public void setCacheFactor(float factor) {
			CacheFactor = factor;
		}

		public int getCacheConcurrencyLevel() {
			return CacheConcurrencyLevel;
		}

		public void setCacheConcurrencyLevel(int value) {
			CacheConcurrencyLevel = value;
		}

		public int getCacheInitialCapacity() {
			return CacheInitialCapacity;
		}

		public void setCacheInitialCapacity(int value) {
			CacheInitialCapacity = value;
		}

		public int getCacheNewAccessHotThreshold() {
			return CacheNewAccessHotThreshold;
		}

		public void setCacheNewAccessHotThreshold(int value) {
			CacheNewAccessHotThreshold = value;
		}

		private int CacheCleanPeriod = 10000;
		private int CacheNewLruHotPeriod = 10000;
		private int CacheMaxLruInitialCapacity = 100000;
		private int CacheCleanPeriodWhenExceedCapacity = 1000;
		private boolean CheckpointWhenCommit = false;
		// 自动倒库，当新库(DatabaseName)没有找到记录时，从旧库(DatabaseOldName)中读取，
		// Open 的时候找到旧库并打开Database.Table用来读取。
		// 内存表不支持倒库。
		private String DatabaseName = "";
		private String DatabaseOldName = "";
		private int DatabaseOldMode = 0;

		public int getCacheCleanPeriod() {
			return CacheCleanPeriod;
		}

		public void setCacheCleanPeriod(int value) {
			CacheCleanPeriod = value;
		}

		public int getCacheNewLruHotPeriod() {
			return CacheNewLruHotPeriod;
		}

		public void setCacheNewLruHotPeriod(int value) {
			CacheNewLruHotPeriod = value;
		}

		public int getCacheMaxLruInitialCapacity() {
			return CacheMaxLruInitialCapacity;
		}

		public void setCacheMaxLruInitialCapacity(int value) {
			CacheMaxLruInitialCapacity = value;
		}

		public int getCacheCleanPeriodWhenExceedCapacity() {
			return CacheCleanPeriodWhenExceedCapacity;
		}

		public void setCacheCleanPeriodWhenExceedCapacity(int value) {
			CacheCleanPeriodWhenExceedCapacity = value;
		}

		public boolean getCheckpointWhenCommit() {
			return CheckpointWhenCommit;
		}

		public void setCheckpointWhenCommit(boolean value) {
			CheckpointWhenCommit = value;
		}

		public String getDatabaseName() {
			return DatabaseName;
		}

		public String getDatabaseOldName() {
			return DatabaseOldName;
		}

		public int getDatabaseOldMode() {
			return DatabaseOldMode;
		}

		public TableConf() {
		}

		public TableConf(Config conf, Element self) {
			Name = self.getAttribute("Name");

			String attr = self.getAttribute("CacheCapacity");
			if (!attr.isEmpty())
				setCacheCapacity(Integer.parseInt(attr));
			attr = self.getAttribute("CacheCleanPeriod");
			if (!attr.isEmpty())
				setCacheCleanPeriod(Integer.parseInt(attr));
			attr = self.getAttribute("CacheFactor");
			if (!attr.isEmpty())
				setCacheFactor(Float.parseFloat(attr));

			DatabaseName = self.getAttribute("DatabaseName");
			DatabaseOldName = self.getAttribute("DatabaseOldName");
			attr = self.getAttribute("DatabaseOldMode");
			DatabaseOldMode = attr.length() > 0 ? Integer.parseInt(attr) : 0;

			attr = self.getAttribute("CheckpointWhenCommit");
			if (!attr.isEmpty())
				setCheckpointWhenCommit(Boolean.parseBoolean(attr));
			attr = self.getAttribute("CacheConcurrencyLevel");
			if (!attr.isEmpty())
				setCacheConcurrencyLevel(Integer.parseInt(attr));
			attr = self.getAttribute("CacheInitialCapacity");
			if (!attr.isEmpty())
				setCacheInitialCapacity(Integer.parseInt(attr));
			attr = self.getAttribute("CacheNewAccessHotThreshold");
			if (!attr.isEmpty())
				setCacheNewAccessHotThreshold(Integer.parseInt(attr));
			attr = self.getAttribute("CacheCleanPeriodWhenExceedCapacity");
			if (!attr.isEmpty())
				setCacheCleanPeriodWhenExceedCapacity(Integer.parseInt(attr));
			attr = self.getAttribute("CacheMaxLruInitialCapacity");
			if (!attr.isEmpty())
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
