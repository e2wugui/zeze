package Zeze;

import Zeze.Net.*;
import java.util.*;

import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.logging.log4j.Level;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.io.*;
import java.util.concurrent.ConcurrentHashMap;

public final class Config {
	public interface ICustomize {
		String getName();
		void Parse(Element self);
	}

	public enum DbType {
		Memory,
		MySql,
		SqlServer,
		Tikv
	}

	private int WorkerThreads;
	public int getWorkerThreads() {
		return WorkerThreads;
	}
	public void setWorkerThreads(int value) {
		WorkerThreads = value;
	}
	private int CompletionPortThreads;
	public int getCompletionPortThreads() {
		return CompletionPortThreads;
	}
	public void setCompletionPortThreads(int value) {
		CompletionPortThreads = value;
	}
	private int CheckpointPeriod = 60000;
	public int getCheckpointPeriod() {
		return CheckpointPeriod;
	}
	public void setCheckpointPeriod(int value) {
		CheckpointPeriod = value;
	}
	private Zeze.Transaction.CheckpointMode CheckpointMode = Zeze.Transaction.CheckpointMode.Period;
	public Zeze.Transaction.CheckpointMode getCheckpointMode() {
		return CheckpointMode;
	}
	public void setCheckpointMode(Zeze.Transaction.CheckpointMode value) {
		CheckpointMode = value;
	}

	private Level ProcessReturnErrorLogLevel = Level.INFO;
	public Level getProcessReturnErrorLogLevel() {
		return ProcessReturnErrorLogLevel;
	}
	public void setProcessReturnErrorLogLevel(Level value) {
		ProcessReturnErrorLogLevel = value;
	}
	private int InternalThreadPoolWorkerCount;
	public int getInternalThreadPoolWorkerCount() {
		return InternalThreadPoolWorkerCount;
	}
	public void setInternalThreadPoolWorkerCount(int value) {
		InternalThreadPoolWorkerCount = value;
	}
	private int ServerId;
	public int getServerId() {
		return ServerId;
	}
	public void setServerId(int value) {
		ServerId = value;
	}
	private String GlobalCacheManagerHostNameOrAddress = "";
	public String getGlobalCacheManagerHostNameOrAddress() {
		return GlobalCacheManagerHostNameOrAddress;
	}
	public void setGlobalCacheManagerHostNameOrAddress(String value) {
		GlobalCacheManagerHostNameOrAddress = value;
	}
	private int GlobalCacheManagerPort;
	public int getGlobalCacheManagerPort() {
		return GlobalCacheManagerPort;
	}
	private void setGlobalCacheManagerPort(int value) {
		GlobalCacheManagerPort = value;
	}
	private final ConcurrentHashMap<String, TableConf> TableConfMap = new ConcurrentHashMap<> ();
	public ConcurrentHashMap<String, TableConf> getTableConfMap() {
		return TableConfMap;
	}
	private TableConf DefaultTableConf;
	public TableConf getDefaultTableConf() {
		return DefaultTableConf;
	}
	public void setDefaultTableConf(TableConf value) {
		DefaultTableConf = value;
	}
	private boolean AllowReadWhenRecoredNotAccessed = true;
	public boolean getAllowReadWhenRecoredNotAccessed() {
		return AllowReadWhenRecoredNotAccessed;
	}
	public void setAllowReadWhenRecoredNotAccessed(boolean value) {
		AllowReadWhenRecoredNotAccessed = value;
	}
	private boolean AllowSchemasReuseVariableIdWithSameType = true;
	public boolean getAllowSchemasReuseVariableIdWithSameType() {
		return AllowSchemasReuseVariableIdWithSameType;
	}
	public void setAllowSchemasReuseVariableIdWithSameType(boolean value) {
		AllowSchemasReuseVariableIdWithSameType = value;
	}
	private final ConcurrentHashMap<String, ICustomize> Customize = new ConcurrentHashMap<> ();
	public ConcurrentHashMap<String, ICustomize> getCustomize() {
		return Customize;
	}

	/** 
	 根据自定义配置名字查找。
	 因为外面需要通过AddCustomize注册进来，
	 如果外面保存了配置引用，是不需要访问这个接口的。
	 
	 <typeparam name="T"></typeparam>
	*/
	@SuppressWarnings("unchecked")
	public <T extends ICustomize> T GetCustomize(T customize) {
		return (T)(getCustomize().get(customize.getName()));
	}

	public void AddCustomize(ICustomize c) {
		if (null != getCustomize().putIfAbsent(c.getName(), c)) {
			throw new RuntimeException("Duplicate Customize Config '" + c.getName() + "'");
		}
	}

	public TableConf GetTableConf(String name) {
		var tableConf = getTableConfMap().get(name);
		if (null != tableConf)
			return tableConf;
		return getDefaultTableConf();
	}

	private final ConcurrentHashMap<String, DatabaseConf> DatabaseConfMap = new ConcurrentHashMap<> ();
	public ConcurrentHashMap<String, DatabaseConf> getDatabaseConfMap() {
		return DatabaseConfMap;
	}

	private Zeze.Transaction.Database CreateDatabase(DatabaseConf conf) {
		switch (conf.DatabaseType) {
			case Memory:
				return new Zeze.Transaction.DatabaseMemory(conf);
			case MySql:
				return new Zeze.Transaction.DatabaseMySql(conf);
			case SqlServer:
				return new Zeze.Transaction.DatabaseSqlServer(conf);
			//case Tikv:
			//	return new Zeze.Tikv.DatabaseTikv(conf.getDatabaseUrl());
			default:
				throw new RuntimeException("unknown database type.");
		}
	}

	public void CreateDatabase(HashMap<String, Zeze.Transaction.Database> map) {
		// add other database
		for (var db : getDatabaseConfMap().values()) {
			map.put(db.Name, CreateDatabase(db));
		}
	}


	public void ClearInUseAndIAmSureAppStopped() {
		ClearInUseAndIAmSureAppStopped(null);
	}

	public void ClearInUseAndIAmSureAppStopped(HashMap<String, Zeze.Transaction.Database> databases) {
		if (null == databases) {
			databases = new HashMap<>();
			CreateDatabase(databases);
		}
		for (var db : databases.values()) {
			db.getDirectOperates().ClearInUse(getServerId(), getGlobalCacheManagerHostNameOrAddress());
		}
	}


	private final ConcurrentHashMap<String, ServiceConf> ServiceConfMap = new ConcurrentHashMap<>();
	public ConcurrentHashMap<String, ServiceConf> getServiceConfMap() {
		return ServiceConfMap;
	}
	private ServiceConf DefaultServiceConf = new ServiceConf();
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
	 由于这个方法没法加入Customize配置，为了兼容和内部测试保留，
	 应用应该自己LoadAndParse。
	 var c = new Config();
	 c.AddCustomize(...);
	 c.LoadAndParse();
	 
	*/

	public static Config Load() {
		return Load("zeze.xml");
	}

	public static Config Load(String xmlfile) {
		return (new Config()).LoadAndParse(xmlfile);
	}


	public Config LoadAndParse() {
		return LoadAndParse("zeze.xml");
	}

	public Config LoadAndParse(String xmlfile) {
		if ((new File(xmlfile)).isFile()) {
			DocumentBuilderFactory db = DocumentBuilderFactory.newInstance();
			db.setXIncludeAware(true);	
			db.setNamespaceAware(true);
			try {
				Document doc = db.newDocumentBuilder().parse(xmlfile);
				Parse(doc.getDocumentElement());
			}
			catch (Exception ex) {
				throw new RuntimeException(ex);
			}
		}
		if (null == getDefaultTableConf()) {
			setDefaultTableConf(new TableConf());
		}
		if (getDatabaseConfMap().isEmpty()) { // add default databaseconf.

			if (null != getDatabaseConfMap().putIfAbsent("", new DatabaseConf())) {
				throw new RuntimeException("Concurrent Add Default Database.");
			}
		}
		return this;
	}

	public void Parse(Element self) {
		if (!self.getNodeName().equals("zeze")) {
			throw new RuntimeException("is it a zeze config.");
		}

		setCheckpointPeriod(Integer.parseInt(self.getAttribute("CheckpointPeriod")));
		setServerId(Integer.parseInt(self.getAttribute("ServerId")));

		setGlobalCacheManagerHostNameOrAddress(self.getAttribute("GlobalCacheManagerHostNameOrAddress"));
		String attr = self.getAttribute("GlobalCacheManagerPort");
		setGlobalCacheManagerPort(attr.length() > 0 ? Integer.parseInt(attr) : 0);

		attr = self.getAttribute("ProcessReturnErrorLogLevel");
		if (attr.length() > 0) {
			setProcessReturnErrorLogLevel(Level.toLevel(attr));
		}

		attr = self.getAttribute("InternalThreadPoolWorkerCount");
		setInternalThreadPoolWorkerCount(attr.length() > 0 ? Integer.parseInt(attr) : 10);

		attr = self.getAttribute("WorkerThreads");
		setWorkerThreads(attr.length() > 0 ? Integer.parseInt(attr) : -1);

		attr = self.getAttribute("CompletionPortThreads");
		setCompletionPortThreads(attr.length() > 0 ? Integer.parseInt(attr) : -1);

		attr = self.getAttribute("AllowReadWhenRecoredNotAccessed");
		setAllowReadWhenRecoredNotAccessed(attr.length() <= 0 || Boolean.parseBoolean(attr));
		attr = self.getAttribute("AllowSchemasReuseVariableIdWithSameType");
		setAllowSchemasReuseVariableIdWithSameType(attr.length() <= 0 || Boolean.parseBoolean(attr));

		attr = self.getAttribute("CheckpointMode");
		if (attr.length() > 0) {
			setCheckpointMode(Zeze.Transaction.CheckpointMode.valueOf(attr));
		}

		NodeList childnodes = self.getChildNodes();
		for (int i = 0; i < childnodes.getLength(); ++i) {
			Node node = childnodes.item(i);
			if (Node.ELEMENT_NODE != node.getNodeType()) {
				continue;
			}

			Element e = (Element) node;
			switch (e.getNodeName()) {
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
						throw new RuntimeException("Unknown CustomizeConf Name='" + cname + "'");

					customizeConf.Parse(e);
					break;

				default:
					throw new RuntimeException("unknown node name: " + e.getNodeName());
			}
		}
	}

	public final static class DatabaseConf {
		private String Name = "";
		public String getName() {
			return Name;
		}
		private DbType DatabaseType = DbType.Memory;
		public DbType getDatabaseType() {
			return DatabaseType;
		}
		private String DatabaseUrl = "";
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

		public DatabaseConf() {
		}

		public DatabaseConf(Config conf, Element self) {
			Name = self.getAttribute("Name");
			switch (self.getAttribute("DatabaseType")) {
				case "Memory":
					DatabaseType = DbType.Memory;
					break;
				case "MySql":
					DatabaseType = DbType.MySql;
					break;
				case "SqlServer":
					DatabaseType = DbType.SqlServer;
					break;
				case "Tikv":
					DatabaseType = DbType.Tikv;
					break;
				default:
					throw new RuntimeException("unknown database type.");
			}
			DatabaseUrl = self.getAttribute("DatabaseUrl");
			if (null != conf.getDatabaseConfMap().putIfAbsent(getName(), this)) {
				throw new RuntimeException("Duplicate Database '" + getName() + "'");
			}
		}
	}

	public final static class TableConf {
		private String Name;
		public String getName() {
			return Name;
		}
		private long CacheCapacity = 20000;
		public long getCacheCapacity() {
			return CacheCapacity;
		}
		public void setCacheCapacity(long value) {
			CacheCapacity = value;
		}
		private int CacheConcurrencyLevel;
		public int getCacheConcurrencyLevel() {
			return CacheConcurrencyLevel;
		}
		public void setCacheConcurrencyLevel(int value) {
			CacheConcurrencyLevel = value;
		}
		private int CacheInitialCapaicty;
		public int getCacheInitialCapaicty() {
			return CacheInitialCapaicty;
		}
		public void setCacheInitialCapaicty(int value) {
			CacheInitialCapaicty = value;
		}
		private int CacheNewAccessHotThreshold;
		public int getCacheNewAccessHotThreshold() {
			return CacheNewAccessHotThreshold;
		}
		public void setCacheNewAccessHotThreshold(int value) {
			CacheNewAccessHotThreshold = value;
		}
		private int CacheCleanPeriod = 1000;
		public int getCacheCleanPeriod() {
			return CacheCleanPeriod;
		}
		public void setCacheCleanPeriod(int value) {
			CacheCleanPeriod = value;
		}
		private int CacheNewLruHotPeriod = 1000;
		public int getCacheNewLruHotPeriod() {
			return CacheNewLruHotPeriod;
		}
		public void setCacheNewLruHotPeriod(int value) {
			CacheNewLruHotPeriod = value;
		}
		private int CacheMaxLruInitialCapaicty = 100000;
		public int getCacheMaxLruInitialCapaicty() {
			return CacheMaxLruInitialCapaicty;
		}
		public void setCacheMaxLruInitialCapaicty(int value) {
			CacheMaxLruInitialCapaicty = value;
		}
		private int CacheCleanPeriodWhenExceedCapacity;
		public int getCacheCleanPeriodWhenExceedCapacity() {
			return CacheCleanPeriodWhenExceedCapacity;
		}
		public void setCacheCleanPeriodWhenExceedCapacity(int value) {
			CacheCleanPeriodWhenExceedCapacity = value;
		}
		private boolean CheckpointWhenCommit = false;
		public boolean getCheckpointWhenCommit() {
			return CheckpointWhenCommit;
		}
		public void setCheckpointWhenCommit(boolean value) {
			CheckpointWhenCommit = value;
		}

		// 自动倒库，当新库(DatabaseName)没有找到记录时，从旧库(DatabaseOldName)中读取，
		// Open 的时候找到旧库并打开Database.Table用来读取。
		// 内存表不支持倒库。
		private String DatabaseName = "";
		public String getDatabaseName() {
			return DatabaseName;
		}
		private String DatabaseOldName = "";
		public String getDatabaseOldName() {
			return DatabaseOldName;
		}
		private int DatabaseOldMode = 0;
		public int getDatabaseOldMode() {
			return DatabaseOldMode;
		}


		public TableConf() {

		}

		public TableConf(Config conf, Element self) {
			Name = self.getAttribute("Name");

			String attr = self.getAttribute("CacheCapacity");
			if (attr.length() > 0) {
				setCacheCapacity(Long.parseLong(attr));
			}

			attr = self.getAttribute("CacheCleanPeriod");
			if (attr.length() > 0) {
				setCacheCleanPeriod(Integer.parseInt(attr));
			}
			DatabaseName = self.getAttribute("DatabaseName");
			DatabaseOldName = self.getAttribute("DatabaseOldName");
			attr = self.getAttribute("DatabaseOldMode");
			DatabaseOldMode = attr.length() > 0 ? Integer.parseInt(attr) : 0;
			attr = self.getAttribute("CheckpointWhenCommit");
			if (attr.length() > 0) {
				setCheckpointWhenCommit(Boolean.parseBoolean(attr));
			}
			attr = self.getAttribute("CacheConcurrencyLevel");
			if (attr.length() > 0) {
				setCacheConcurrencyLevel(Integer.parseInt(attr));
			}
			attr = self.getAttribute("CacheInitialCapaicty");
			if (attr.length() > 0) {
				setCacheInitialCapaicty(Integer.parseInt(attr));
			}
			attr = self.getAttribute("CacheNewAccessHotThreshold");
			if (attr.length() > 0) {
				setCacheNewAccessHotThreshold(Integer.parseInt(attr));
			}
			attr = self.getAttribute("CacheCleanPeriodWhenExceedCapacity");
			if (attr.length() > 0) {
				setCacheCleanPeriodWhenExceedCapacity(Integer.parseInt(attr));
			}
			attr = self.getAttribute("CacheMaxLruInitialCapaicty");
			if (attr.length() > 0) {
				setCacheMaxLruInitialCapaicty(Integer.parseInt(attr));
			}

			if (getName().length() > 0) {
				if (null != conf.getTableConfMap().putIfAbsent(getName(), this)) {
					throw new RuntimeException("Duplicate Table '" + getName() + "'");
				}
			}
			else if (conf.getDefaultTableConf() == null) {
				conf.setDefaultTableConf(this);
			}
			else {
				throw new RuntimeException("too many DefaultTableConf.");
			}
		}
	}

	public Config() {
	}
}