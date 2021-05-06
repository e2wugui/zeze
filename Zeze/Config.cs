using System;
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
        }

        public int WorkerThreads { get; set; }
        public int CompletionPortThreads { get; set; }
        public int CheckpointPeriod { get; set; } = 60000; // 60 seconds
        public NLog.LogLevel ProcessReturnErrorLogLevel { get; set; } = NLog.LogLevel.Info;
        public int InternalThreadPoolWorkerCount { get; set; }
        public int AutoKeyLocalId { get; set; } = 0;
        public int AutoKeyLocalStep { get; private set; } = 4096;
        public string GlobalCacheManagerHostNameOrAddress { get; set; }
        public int GlobalCacheManagerPort { get; private set; }
        public Dictionary<string, TableConf> TableConfMap { get; } = new Dictionary<string, TableConf>();
        public TableConf DefaultTableConf { get; set; } = new TableConf();
        public bool AllowReadWhenRecoredNotAccessed { get; set; } = true;
        public bool AllowSchemasReuseVariableIdWithSameType { get; set; } = true;
        public Dictionary<string, ICustomize> Customize { get; } = new Dictionary<string, ICustomize>();

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
            Customize.Add(c.Name, c);
        }

        public TableConf GetTableConf(string name)
        {
            if (TableConfMap.TryGetValue(name, out var tableConf))
            {
                return tableConf;
            }
            return DefaultTableConf;
        }
        public Dictionary<string, DatabaseConf> DatabaseConfMap { get; } = new Dictionary<string, DatabaseConf>();

        private Transaction.Database CreateDatabase(DbType dbType, string url)
        {
            switch (dbType)
            {
                case DbType.Memory:
                    return new Transaction.DatabaseMemory(url);
#if USE_DATABASE
                case DbType.MySql:
                    return new Transaction.DatabaseMySql(url);
                case DbType.SqlServer:
                    return new Transaction.DatabaseSqlServer(url);
                case DbType.Tikv:
                    return new Tikv.DatabaseTikv(url);
#endif
                default:
                    throw new Exception("unknown database type.");
            }
        }

        public void CreateDatabase(Dictionary<string, Transaction.Database> map)
        {
            // add other database
            foreach (var db in DatabaseConfMap.Values)
            {
                map.Add(db.Name, CreateDatabase(db.DatabaseType, db.DatabaseUrl));
            }
        }

        public Dictionary<string, ServiceConf> ServiceConfMap { get; } = new Dictionary<string, ServiceConf>();
        public ServiceConf DefaultServiceConf { get; set; } = new ServiceConf();

        public ServiceConf GetServiceConf(string name)
        {
            if (ServiceConfMap.TryGetValue(name, out var serviceConf))
                return serviceConf;
            return DefaultServiceConf;
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
        public static Config Load(string xmlfile = null)
        {
            return new Config().LoadAndParse(xmlfile);
        }

        public Config LoadAndParse(string xmlfile = null)
        {
            if (null == xmlfile)
            {
                xmlfile = "zeze.xml";
            }

            if (System.IO.File.Exists(xmlfile))
            {
                XmlDocument doc = new XmlDocument();
                doc.Load(xmlfile);
                Parse(doc.DocumentElement);
            }

            return this;
        }

        public void Parse(XmlElement self)
        {
            if (false == self.Name.Equals("zeze"))
                throw new Exception("is it a zeze config.");

            CheckpointPeriod = int.Parse(self.GetAttribute("CheckpointPeriod"));
            AutoKeyLocalId = int.Parse(self.GetAttribute("AutoKeyLocalId"));
            AutoKeyLocalStep = int.Parse(self.GetAttribute("AutoKeyLocalStep"));
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
            if (DatabaseConfMap.Count == 0) // add default databaseconf.
                DatabaseConfMap.Add("", new DatabaseConf());
        }

        public sealed class DatabaseConf
        {
            public string Name { get; set; } = "";
            public DbType DatabaseType { get; set; } = DbType.Memory;
            public string DatabaseUrl { get; set; } = "";

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
                    default: throw new Exception("unknown database type.");
                }
                DatabaseUrl = self.GetAttribute("DatabaseUrl");
                conf.DatabaseConfMap.Add(Name, this);
            }
        }

        public sealed class TableConf
        {
            public string Name { get; set; }
            public int CacheCapaicty { get; set; } = 20000;
            public int CacheCleanPeriod { get; set; } = 3600 * 1000; // 毫秒，一小时

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

                string attr = self.GetAttribute("CacheCapaicty");
                if (attr.Length > 0)
                    CacheCapaicty = int.Parse(attr);

                attr = self.GetAttribute("CacheCleanPeriod");
                if (attr.Length > 0)
                    CacheCleanPeriod = int.Parse(attr);
                DatabaseName = self.GetAttribute("DatabaseName");
                DatabaseOldName = self.GetAttribute("DatabaseOldName");
                attr = self.GetAttribute("DatabaseOldMode");
                DatabaseOldMode = attr.Length > 0 ? int.Parse(attr) : 0;

                if (Name.Length > 0)
                {
                    conf.TableConfMap.Add(Name, this);
                }
                else
                {
                    conf.DefaultTableConf = this;
                }
            }
        }

        public sealed class ServiceConf
        {
            public string Name { get; }
            public Net.SocketOptions SocketOptions { get; set; } = new Net.SocketOptions();
            public Services.HandshakeOptions HandshakeOptions { get; set; } = new Services.HandshakeOptions();

            private List<Acceptor> Acceptors { get; } = new List<Acceptor>();
            private List<Connector> Connectors { get; } = new List<Connector>();

            public void AddConnector(Connector connector)
            {
                lock (Connectors)
                {
                    Connectors.Add(connector);
                }
            }

            public Connector FindConnector(string host, int port)
            {
                lock (Connectors)
                {
                    foreach (var c in Connectors)
                    {
                        if (c.HostNameOrAddress.Equals(host) && c.Port.Equals(port))
                            return c;
                    }
                    return null;
                }
            }

            public Connector GetOrAddConnector(string host, int port, bool autoReconnect)
            {
                lock (Connectors)
                {
                    var exist = FindConnector(host, port);
                    if (null != exist)
                        return exist;
                    Connector add = new Connector(host, port, autoReconnect);
                    Connectors.Add(add);
                    return add;
                }
            }

            public void RemoveConnector(Connector c)
            {
                lock (Connectors)
                {
                    Connectors.Remove(c);
                }
            }

            public void ForEachConnector(Action<Connector> action)
            {
                lock (Connectors)
                {
                    Connectors.ForEach(action);
                }
            }

            public void AddAcceptor(Acceptor a)
            {
                lock (Acceptors)
                {
                    Acceptors.Add(a);
                }
            }

            public void RemoveAcceptor(Acceptor a)
            {
                lock (Acceptors)
                {
                    Acceptors.Remove(a);
                }
            }

            public void ForEachAcceptor(Action<Acceptor> action)
            {
                lock (Acceptors)
                {
                    Acceptors.ForEach(action);
                }
            }

            public ServiceConf()
            {
                Name = "";
            }

            public ServiceConf(Config conf, XmlElement self)
            {
                Name = self.GetAttribute("Name");

                string attr;

                // SocketOptions
                attr = self.GetAttribute("NoDelay");
                if (attr.Length > 0) SocketOptions.NoDelay = bool.Parse(attr);
                attr = self.GetAttribute("SendBuffer");
                if (attr.Length > 0) SocketOptions.SendBuffer= int.Parse(attr);
                attr = self.GetAttribute("ReceiveBuffer");
                if (attr.Length > 0) SocketOptions.ReceiveBuffer = int.Parse(attr);
                attr = self.GetAttribute("InputBufferSize");
                if (attr.Length > 0) SocketOptions.InputBufferSize = int.Parse(attr);
                attr = self.GetAttribute("InputBufferMaxProtocolSize");
                if (attr.Length > 0) SocketOptions.InputBufferMaxProtocolSize = int.Parse(attr);
                attr = self.GetAttribute("OutputBufferMaxSize");
                if (attr.Length > 0) SocketOptions.OutputBufferMaxSize = int.Parse(attr);
                attr = self.GetAttribute("Backlog");
                if (attr.Length > 0) SocketOptions.Backlog = int.Parse(attr);
                attr = self.GetAttribute("SocketLogLevel");
                if (attr.Length > 0) SocketOptions.SocketLogLevel = NLog.LogLevel.FromString(attr);

                // HandshakeOptions
                attr = self.GetAttribute("DhGroups");
                if (attr.Length > 0)
                {
                    HandshakeOptions.DhGroups = new HashSet<int>();
                    foreach (string dg in attr.Split(','))
                    {
                        string dgtmp = dg.Trim();
                        if (dgtmp.Length == 0)
                            continue;
                        HandshakeOptions.AddDhGroup(int.Parse(dgtmp));
                    }
                }
                attr = self.GetAttribute("SecureIp");
                if (attr.Length > 0) HandshakeOptions.SecureIp = System.Net.IPAddress.Parse(attr).GetAddressBytes();
                attr = self.GetAttribute("S2cNeedCompress");
                if (attr.Length > 0) HandshakeOptions.S2cNeedCompress = bool.Parse(attr);
                attr = self.GetAttribute("C2sNeedCompress");
                if (attr.Length > 0) HandshakeOptions.C2sNeedCompress = bool.Parse(attr);
                attr = self.GetAttribute("DhGroup");
                if (attr.Length > 0) HandshakeOptions.DhGroup = byte.Parse(attr);

                if (Name.Length > 0)
                    conf.ServiceConfMap.Add(Name, this);
                else
                    conf.DefaultServiceConf = this;

                // connection creator options
                XmlNodeList childNodes = self.ChildNodes;
                foreach (XmlNode node in childNodes)
                {
                    if (XmlNodeType.Element != node.NodeType)
                        continue;

                    XmlElement e = (XmlElement)node;
                    switch (e.Name)
                    {
                        case "Acceptor": AddAcceptor(new Acceptor(e)); break;
                        case "Connector": AddConnector(new Connector(e)); break;
                        default: throw new Exception("unknown node name: " + e.Name);
                    }
                }
            }

            public void Start(Service service)
            {
                ForEachAcceptor((a) => a.Start(service));
                ForEachConnector((c) => c.Start(service));
            }

            public void Stop(Service service)
            {
                ForEachAcceptor((a) => a.Stop(service));
                ForEachConnector((c) => c.Stop(service));
            }

            public void StopListen(Service service)
            {
                ForEachAcceptor((a) => a.Stop(service));
            }
        }

        public Config()
        {
        }
    }
}
