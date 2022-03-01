using System;
using System.Collections.Concurrent;
using System.Collections.Generic;
using System.IO;
using System.Linq;
using System.Text;
using System.Threading.Tasks;
using System.Xml;

namespace Zeze.Raft
{
    public sealed class RaftConfig
    {
        private XmlDocument XmlDocument { get; }

        public string XmlFileName { get; }
        private XmlElement Self { get; }

        internal ConcurrentDictionary<string, Node> Nodes { get; }
            = new ConcurrentDictionary<string, Node>();

        // 【这个参数不保存】可以在启动的时候从参数读取并设置。
        public string Name { get; internal set; }
        // 多数确认时：大于等于这个即可，因为还有自己(Leader)。
        public int HalfCount => Nodes.Count / 2;
        public string DbHome { get; set; } = "./";

        public const int DefaultAppendEntriesTimeout = 2000;
        public const int DefaultLeaderHeartbeatTimer = DefaultAppendEntriesTimeout + 200;

        /// <summary>
        /// 复制日志超时，以及发送失败重试超时。
        /// </summary>
        public int AppendEntriesTimeout { get; set; } = DefaultAppendEntriesTimeout;
        /// <summary>
        /// 不精确 Heartbeat Idle 算法：
        /// </summary>
        public int LeaderHeartbeatTimer { get; set; } = DefaultLeaderHeartbeatTimer;

        public int ElectionRandomMax { get; set; } = 300;
        public int ElectionTimeout => LeaderHeartbeatTimer + 100 + Util.Random.Instance.Next(ElectionRandomMax);
        public int ElectionTimeoutMax => LeaderHeartbeatTimer + 100 + ElectionRandomMax * 2;
        /// <summary>
        /// 限制每次复制日志时打包的最大数量。
        /// </summary>
        public int MaxAppendEntriesCount { get; set; } = 500;

        /// <summary>
        /// 创建snapshot最小的日志数量。如果少于这个数，不会创建新的snapshot。
        /// 当然实在需要的时候可以创建。see LogSequence.StartSnapshot
        /// </summary>
        public int SnapshotMinLogCount { get; set; } = 10000;

        /// <summary>
        /// 每天创建 snapshot 的时间，一般负载每天有个低估，
        /// 在这个时候创建snapshot是比较合适的。
        /// 如果需要其他定时模式，自己创建定时器，
        /// 并调用LogSequence.StartSnapshot();
        /// 同时把 SnapshotHourOfDay 配置成-1，关闭默认的定时器。
        /// </summary>
        public int SnapshotHourOfDay { get; set; } = 6;
        public int SnapshotMinute { get; set; } = 0;
        // 需要的时间应小于LeaderHeartbeatTimer
        public int BackgroundApplyCount { get; set; } = 500;

        public int UniqueRequestExpiredDays { get; set; } = 7;

        private RaftConfig(XmlDocument xml, string filename, XmlElement self)
        {
            XmlDocument = xml;
            XmlFileName = filename;
            Self = self;

            Name = self.GetAttribute("Name");
            DbHome = self.GetAttribute("DbHome");
            if (string.IsNullOrEmpty(DbHome))
                DbHome = ".";

            var attr = self.GetAttribute("AppendEntriesTimeout");
            if (!string.IsNullOrEmpty(attr)) AppendEntriesTimeout = int.Parse(attr);
            attr = self.GetAttribute("LeaderHeartbeatTimer");
            if (!string.IsNullOrEmpty(attr)) LeaderHeartbeatTimer = int.Parse(attr);
            attr = self.GetAttribute("MaxAppendEntriesCount");
            if (!string.IsNullOrEmpty(attr)) MaxAppendEntriesCount = int.Parse(attr);
            attr = self.GetAttribute("SnapshotMinLogCount");
            if (!string.IsNullOrEmpty(attr)) SnapshotMinLogCount = int.Parse(attr);
            attr = self.GetAttribute("SnapshotHourOfDay");
            if (!string.IsNullOrEmpty(attr)) SnapshotHourOfDay = int.Parse(attr);
            attr = self.GetAttribute("SnapshotMinute");
            if (!string.IsNullOrEmpty(attr)) SnapshotMinute = int.Parse(attr);
            attr = self.GetAttribute("ElectionRandomMax");
            if (!string.IsNullOrEmpty(attr)) ElectionRandomMax = int.Parse(attr);
            attr = self.GetAttribute("BackgroundApplyCount");
            if (!string.IsNullOrEmpty(attr)) BackgroundApplyCount = int.Parse(attr);
            attr = self.GetAttribute("UniqueRequestExpiredDays");
            if (!string.IsNullOrEmpty(attr)) UniqueRequestExpiredDays = int.Parse(attr);

            XmlNodeList childNodes = self.ChildNodes;
            foreach (XmlNode node in childNodes)
            {
                if (XmlNodeType.Element != node.NodeType)
                    continue;

                XmlElement e = (XmlElement)node;
                switch (e.Name)
                {
                    case "node":
                        AddNode(new Node(e));
                        break;
                }
            }
        }

        public void Verify()
        {
            if (AppendEntriesTimeout < 1000)
                throw new Exception("AppendEntriesTimeout < 1000");
            if (LeaderHeartbeatTimer < AppendEntriesTimeout + 200)
                throw new Exception("LeaderHeartbeatTimer < AppendEntriesTimeout + 200");

            if (MaxAppendEntriesCount < 100)
                MaxAppendEntriesCount = 100;

            if (SnapshotMinute < 0)
                SnapshotMinute = 0;
            else if (SnapshotMinute > 59)
                SnapshotMinute = 59;
        }

        internal void Save()
        {
            // skip default
            if (AppendEntriesTimeout != DefaultAppendEntriesTimeout)
                Self.SetAttribute("AppendEntriesTimeout", AppendEntriesTimeout.ToString());
            if (LeaderHeartbeatTimer != DefaultLeaderHeartbeatTimer)
                Self.SetAttribute("LeaderHeartbeatTimer", LeaderHeartbeatTimer.ToString());
            if (ElectionRandomMax != 300)
                Self.SetAttribute("ElectionRandomMax", ElectionRandomMax.ToString());
            if (MaxAppendEntriesCount != 500)
                Self.SetAttribute("MaxAppendEntriesCount", MaxAppendEntriesCount.ToString());
            if (SnapshotMinLogCount != 10000)
                Self.SetAttribute("SnapshotMinLogCount", SnapshotMinLogCount.ToString());
            if (SnapshotHourOfDay != 6)
                Self.SetAttribute("SnapshotHourOfDay", SnapshotHourOfDay.ToString());
            if (SnapshotMinute != 0)
                Self.SetAttribute("SnapshotMinute", SnapshotMinute.ToString());
            if (BackgroundApplyCount != 500)
                Self.SetAttribute("BackgroundApplyCount", BackgroundApplyCount.ToString());
            if (UniqueRequestExpiredDays != 7)
                Self.SetAttribute("UniqueRequestExpiredDays", UniqueRequestExpiredDays.ToString());

            foreach (var node in Nodes)
            {
                node.Value.Save(XmlDocument, Self);
            }

            using (TextWriter sw = Gen.Program.OpenStreamWriter(XmlFileName))
            {
                XmlDocument.Save(sw);
            }
        }

        public static RaftConfig Load(string xmlfile = "raft.xml")
        {
            if (File.Exists(xmlfile))
            {
                XmlDocument doc = new XmlDocument();
                doc.Load(xmlfile);
                return new RaftConfig(doc, xmlfile, doc.DocumentElement);
            }

            throw new Exception($"Raft.Config: '{xmlfile}' not exists.");
        }

        private void AddNode(Node node)
        {
            if (false == Nodes.TryAdd(node.Name, node))
                throw new Exception($"duplicate node '{node.Name}'");
        }

        public sealed class Node
        {
            public string Host { get; set; }
            public int Port { get; set; }
            public XmlElement Self { get; private set; }
            public string Name => $"{Host}:{Port}";

            public Node(XmlElement self)
            {
                Self = self;
                Host = self.GetAttribute("Host");
                Port = int.Parse(self.GetAttribute("Port"));
            }

            public Node(string host, int port)
            {
                Host = host;
                Port = port;
            }

            public void Save(XmlDocument doc, XmlElement parent)
            {
                if (null == Self)
                {
                    Self = doc.CreateElement("node");
                    parent.AppendChild(Self);
                }
                Self.SetAttribute("HostNameOrAddress", Host);
                Self.SetAttribute("Port", Port.ToString());
            }
        }

    }
}
