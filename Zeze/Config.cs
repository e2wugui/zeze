using System;
using System.Collections.Concurrent;
using System.Collections.Generic;
using System.Text;
using System.Xml;
using System.Xml.Schema;
using Zeze.Net;

namespace Zeze
{
    public sealed class Config
    {
        public interface ICustomize
        {
            public string Name { get; }
            public void Parse(XmlElement self);
        }

        public enum DbType
        {
            Memory,
            MySql,
            SqlServer,
            Tikv,
            RocksDb,
        }

		public string Name { get; private set; } = "";
        public int WorkerThreads { get; set; }
        public int CompletionPortThreads { get; set; }
        public int CheckpointPeriod { get; set; } = 60000; // 60 seconds
        public Transaction.CheckpointMode CheckpointMode { get; set; }
            = Transaction.CheckpointMode.Table;

        public NLog.LogLevel ProcessReturnErrorLogLevel { get; set; } = NLog.LogLevel.Info;
        public int InternalThreadPoolWorkerCount { get; set; }
        public int ServerId { get; set; }
        public string GlobalCacheManagerHostNameOrAddress { get; set; }
        public int GlobalCacheManagerPort { get; private set; }
        public ConcurrentDictionary<string, TableConf> TableConfMap { get; }
            = new ConcurrentDictionary<string, TableConf>();
        public TableConf DefaultTableConf { get; set; }
        public bool AllowReadWhenRecoredNotAccessed { get; set; } = true;
        public bool AllowSchemasReuseVariableIdWithSameType { get; set; } = true;
        public bool FastRedoWhenConflict { get; set; } = false;
        public ConcurrentDictionary<string, ICustomize> Customize { get; }
            = new ConcurrentDictionary<string, ICustomize>();

        public bool DonotCheckSchemasWhenTableIsNew { get; set; } = false;

        /// <summary>
        /// 根据自定义配置名字查找。
        /// 因为外面需要通过AddCustomize注册进来，
        /// 如果外面保存了配置引用，是不需要访问这个接口的。
        /// </summary>
        /// <typeparam name="T"></typeparam>
        /// <param name="name"></param>
        /// <param name="customize"></param>
        public bool GetCustomize<T>(out T customize) where T : ICustomize, new()
        {
            T forName = new T();
            if (Customize.TryGetValue(forName.Name, out var _customize))
            {
                customize = (T)_customize;
                return true;
            }
            customize = default(T);
            return false;
        }

        public void AddCustomize(ICustomize c)
        {
            if (!Customize.TryAdd(c.Name, c))
                throw new Exception($"Duplicate Customize Config '{c.Name}'");
        }

        public TableConf GetTableConf(string name)
        {
            if (TableConfMap.TryGetValue(name, out var tableConf))
            {
                return tableConf;
            }
            return DefaultTableConf;
        }

        public ConcurrentDictionary<string, DatabaseConf> DatabaseConfMap { get; }
            = new ConcurrentDictionary<string, DatabaseConf>();

        private Transaction.Database CreateDatabase(Application zeze, DbType dbType, string url)
        {
            switch (dbType)
            {
                case DbType.Memory:
                    return new Transaction.DatabaseMemory(zeze, url);
#if USE_DATABASE
                case DbType.MySql:
                    return new Transaction.DatabaseMySql(zeze, url);
                case DbType.SqlServer:
                    return new Transaction.DatabaseSqlServer(zeze, url);
                case DbType.Tikv:
                    return new Tikv.DatabaseTikv(zeze, url);
                case DbType.RocksDb:
                    return new Transaction.DatabaseRocksDb(zeze, url);
#endif
                default:
                    throw new Exception("unknown database type.");
            }
        }

        public void CreateDatabase(Application zeze, Dictionary<string, Transaction.Database> map)
        {
            // add other database
            foreach (var db in DatabaseConfMap.Values)
            {
                map.Add(db.Name, CreateDatabase(zeze, db.DatabaseType, db.DatabaseUrl));
            }
        }

        public void ClearInUseAndIAmSureAppStopped(Application zeze,
            Dictionary<string, Transaction.Database> databases = null)
        {
            if (null == databases)
            {
                databases = new Dictionary<string, Transaction.Database>();
                CreateDatabase(zeze, databases);
            }
            foreach (var db in databases.Values)
            {
                db.DirectOperates.ClearInUse(ServerId, GlobalCacheManagerHostNameOrAddress);
            }
        }


        public ConcurrentDictionary<string, ServiceConf> ServiceConfMap { get; }
            = new ConcurrentDictionary<string, ServiceConf>();
        public ServiceConf DefaultServiceConf { get; internal set; } = new ServiceConf();

        public ServiceConf GetServiceConf(string name)
        {
            if (ServiceConfMap.TryGetValue(name, out var serviceConf))
                return serviceConf;
            return null;
        }

        /// <summary>
        /// 由于这个方法没法加入Customize配置，为了兼容和内部测试保留，
        /// 应用应该自己LoadAndParse。
        /// var c = new Config();
        /// c.AddCustomize(...);
        /// c.LoadAndParse();
        /// </summary>
        /// <param name="xmlfile"></param>
        /// <returns></returns>
        public static Config Load(string xmlfile = "zeze.xml")
        {
            return new Config().LoadAndParse(xmlfile);
        }

        public Config LoadAndParse(string xmlfile = "zeze.xml")
        {
            if (System.IO.File.Exists(xmlfile))
            {
                XmlDocument doc = new XmlDocument();
                doc.Load(xmlfile);
                Parse(doc.DocumentElement);
            }

            if (null == DefaultTableConf)
                DefaultTableConf = new TableConf();
            if (DatabaseConfMap.Count == 0) // add default databaseconf.
            {
                if (!DatabaseConfMap.TryAdd("", new DatabaseConf()))
                {
                    throw new Exception("Concurrent Add Default Database.");
                }
            }
            return this;
        }

        public void Parse(XmlElement self)
        {
            if (false == self.Name.Equals("zeze"))
                throw new Exception("is it a zeze config.");
			
			Name = self.GetAttribute("name");

            CheckpointPeriod = int.Parse(self.GetAttribute("CheckpointPeriod"));
            ServerId = int.Parse(self.GetAttribute("ServerId"));

            GlobalCacheManagerHostNameOrAddress = self.GetAttribute("GlobalCacheManagerHostNameOrAddress");
            string attr = self.GetAttribute("GlobalCacheManagerPort");
            GlobalCacheManagerPort = attr.Length > 0 ? int.Parse(attr) : 0;

            attr = self.GetAttribute("ProcessReturnErrorLogLevel");
            if (attr.Length > 0)
                ProcessReturnErrorLogLevel = NLog.LogLevel.FromString(attr);

            attr = self.GetAttribute("InternalThreadPoolWorkerCount");
            InternalThreadPoolWorkerCount = attr.Length > 0 ? int.Parse(attr) : 10;

            attr = self.GetAttribute("WorkerThreads");
            WorkerThreads = attr.Length > 0 ? int.Parse(attr) : -1;

            attr = self.GetAttribute("CompletionPortThreads");
            CompletionPortThreads = attr.Length > 0 ? int.Parse(attr) : -1;

            attr = self.GetAttribute("AllowReadWhenRecoredNotAccessed");
            AllowReadWhenRecoredNotAccessed = attr.Length > 0 ? bool.Parse(attr) : true;
            attr = self.GetAttribute("AllowSchemasReuseVariableIdWithSameType");
            AllowSchemasReuseVariableIdWithSameType = attr.Length > 0 ? bool.Parse(attr) : true;

            attr = self.GetAttribute("CheckpointMode");
            if (attr.Length > 0)
                CheckpointMode = (Transaction.CheckpointMode)Enum.Parse(typeof(Transaction.CheckpointMode), attr);
            if (CheckpointMode == Transaction.CheckpointMode.Period && GlobalCacheManagerHostNameOrAddress.Length > 0)
            {
                Application.logger.Warn("CheckpointMode.Period Cannot Work With Global. Change To CheckpointMode.Table Now.");
                CheckpointMode = Transaction.CheckpointMode.Table;
            }
            attr = self.GetAttribute("DonotCheckSchemasWhenTableIsNew");
            if (attr.Length > 0)
                DonotCheckSchemasWhenTableIsNew = bool.Parse(attr);

            attr = self.GetAttribute("FastRedoWhenConflict");
            if (attr.Length > 0)
                FastRedoWhenConflict = bool.Parse(attr);

            XmlNodeList childNodes = self.ChildNodes;
            foreach (XmlNode node in childNodes)
            {
                if (XmlNodeType.Element != node.NodeType)
                    continue;

                XmlElement e = (XmlElement)node;
                switch (e.Name)
                {
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
                        var cname = e.GetAttribute("Name");
                        if (false == Customize.TryGetValue(cname, out var customizeConf))
                            throw new Exception($"Unknown CustomizeConf Name='{cname}'");
                        customizeConf.Parse(e);
                        break;

                    default:
                        throw new Exception("unknown node name: " + e.Name);
                }
            }
        }

        public sealed class DatabaseConf
        {
            public string Name { get; } = "";
            public DbType DatabaseType { get; } = DbType.Memory;
            public string DatabaseUrl { get; } = "";

            public DatabaseConf()
            { 
            }

            public DatabaseConf(Config conf, XmlElement self)
            {
                Name = self.GetAttribute("Name");
                switch (self.GetAttribute("DatabaseType"))
                {
                    case "Memory": DatabaseType = DbType.Memory; break;
                    case "MySql": DatabaseType = DbType.MySql; break;
                    case "SqlServer": DatabaseType = DbType.SqlServer; break;
                    case "Tikv": DatabaseType = DbType.Tikv; break;
                    case "RocksDb": DatabaseType = DbType.RocksDb; break;
                    default: throw new Exception("unknown database type.");
                }
                DatabaseUrl = self.GetAttribute("DatabaseUrl");
                if (!conf.DatabaseConfMap.TryAdd(Name, this))
                    throw new Exception($"Duplicate Database '{Name}'");
            }
        }

        public sealed class TableConf
        {
            public string Name { get; }
            public int CacheCapacity { get; set; } = 20000;
            public int CacheConcurrencyLevel { get; set; }
            public int CacheInitialCapacity { get; set; }
            public int CacheNewAccessHotThreshold { get; set; }
            public int CacheCleanPeriod { get; set; } = 10000;
            public int CacheNewLruHotPeriod { get; set; } = 10000;
            public int CacheMaxLruInitialCapacity { get; set; } = 100000;
            public int CacheCleanPeriodWhenExceedCapacity { get; set; } = 1000;
            public bool CheckpointWhenCommit { get; set; } = false;

            // 自动倒库，当新库(DatabaseName)没有找到记录时，从旧库(DatabaseOldName)中读取，
            // Open 的时候找到旧库并打开Database.Table用来读取。
            // 内存表不支持倒库。
            public string DatabaseName { get; } = "";
            public string DatabaseOldName { get; } = "";
            public int DatabaseOldMode { get; } = 0; // 0 none; 1 如果新库没有找到记录，尝试从旧库读取;


            public TableConf()
            {

            }

            public TableConf(Config conf, XmlElement self)
            {
                Name = self.GetAttribute("Name");

                string attr = self.GetAttribute("CacheCapacity");
                if (attr.Length > 0)
                    CacheCapacity = int.Parse(attr);

                attr = self.GetAttribute("CacheCleanPeriod");
                if (attr.Length > 0)
                    CacheCleanPeriod = int.Parse(attr);
                DatabaseName = self.GetAttribute("DatabaseName");
                DatabaseOldName = self.GetAttribute("DatabaseOldName");
                attr = self.GetAttribute("DatabaseOldMode");
                DatabaseOldMode = attr.Length > 0 ? int.Parse(attr) : 0;
                attr = self.GetAttribute("CheckpointWhenCommit");
                if (attr.Length > 0)
                    CheckpointWhenCommit = bool.Parse(attr);
                attr = self.GetAttribute("CacheConcurrencyLevel");
                if (attr.Length > 0)
                    CacheConcurrencyLevel = int.Parse(attr);
                attr = self.GetAttribute("CacheInitialCapacity");
                if (attr.Length > 0)
                    CacheInitialCapacity = int.Parse(attr);
                attr = self.GetAttribute("CacheNewAccessHotThreshold");
                if (attr.Length > 0)
                    CacheNewAccessHotThreshold = int.Parse(attr);
                attr = self.GetAttribute("CacheCleanPeriodWhenExceedCapacity");
                if (attr.Length > 0)
                    CacheCleanPeriodWhenExceedCapacity = int.Parse(attr);
                attr = self.GetAttribute("CacheMaxLruInitialCapacity");
                if (attr.Length > 0)
                    CacheMaxLruInitialCapacity = int.Parse(attr);

                if (Name.Length > 0)
                {
                    if (!conf.TableConfMap.TryAdd(Name, this))
                    {
                        throw new Exception($"Duplicate Table '{Name}'");
                    }
                }
                else if (conf.DefaultTableConf == null)
                {
                    conf.DefaultTableConf = this;
                }
                else
                    throw new Exception("too many DefaultTableConf.");
            }
        }

        public Config()
        {
        }
    }
}
