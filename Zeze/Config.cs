using System;
using System.Collections.Generic;
using System.Text;
using System.Xml;
using System.Xml.Schema;
using Zeze.Gen;

namespace Zeze
{
    public class Config
    {
        public enum DbType
        {
            Memory,
            MySql,
            SqlServer,
        }

        public DbType DatabaseType { get; set; } = DbType.Memory;
        public string DatabaseUrl { get; set; } = "";
        public int CheckpointPeriod { get; set; } = 60000; // 60 seconds
        public int AutoKeyLocalId { get; } = 0;
        public int AutoKeyLocalStep { get; } = 4096;

        public Dictionary<string, TableConf> TableConfMap { get; } = new Dictionary<string, TableConf>();
        public TableConf DefaultTableConf { get; set; } = new TableConf();
        public TableConf GetTableConf(string name)
        {
            if (TableConfMap.TryGetValue(name, out var tableConf))
            {
                return tableConf;
            }
            return DefaultTableConf;
        }

        private Transaction.Database CreateDatabase(DbType dbType)
        {
            switch (dbType)
            {
                case DbType.Memory:
                    return new Transaction.DatabaseMemory();
                case DbType.MySql:
                    return new Transaction.DatabaseMySql(DatabaseUrl);
                case DbType.SqlServer:
                    return new Transaction.DatabaseSqlServer(DatabaseUrl);
                default:
                    throw new Exception("unknown database type.");
            }
        }

        public void CreateDatabase(Dictionary<string, Transaction.Database> map)
        {
            // add default database
            map.Add("", CreateDatabase(DatabaseType));
            // 多数据库在后面初始化。
        }

        public static Config Load(string xmlfile = null)
        {
            if (null == xmlfile)
            {
                xmlfile = "zeze.xml";
            }

            if (System.IO.File.Exists(xmlfile))
            {
                XmlDocument doc = new XmlDocument();
                doc.Load(xmlfile);
                return new Config(doc.DocumentElement);
            }

            return new Config();
        }

        public Config(XmlElement self)
        {
            if (false == self.Name.Equals("zeze"))
                throw new Exception("is it a zeze config.");

            switch (self.GetAttribute("DatabaseType"))
            {
                case "Memory": DatabaseType = DbType.Memory; break;
                case "MySql": DatabaseType = DbType.MySql; break;
                case "SqlServer": DatabaseType = DbType.SqlServer; break;
                default: throw new Exception("unknown database type.");
            }

            DatabaseUrl = self.GetAttribute("DatabaseUrl");
            CheckpointPeriod = int.Parse(self.GetAttribute("CheckpointPeriod"));
            AutoKeyLocalId = int.Parse(self.GetAttribute("AutoKeyLocalId"));
            AutoKeyLocalStep = int.Parse(self.GetAttribute("AutoKeyLocalStep"));

            XmlNodeList childNodes = self.ChildNodes;
            foreach (XmlNode node in childNodes)
            {
                if (XmlNodeType.Element != node.NodeType)
                    continue;

                XmlElement e = (XmlElement)node;
                switch (e.Name)
                {
                    case "TableConf": new TableConf(this, e); break;
                }
            }
        }

        public class TableConf
        {
            public string Name { get; set; }
            public int CacheCapaicty { get; set; } = 20000;
            public int CacheCleanPeriod { get; set; } = 3600 * 1000; // 毫秒，一小时

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

        public Config()
        {
        }
    }
}
