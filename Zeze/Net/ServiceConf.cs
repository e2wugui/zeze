using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;
using System.Xml;
using System.Collections.Concurrent;

namespace Zeze.Net
{
    public sealed class ServiceConf
    {
        public string Name { get; }
        public Net.SocketOptions SocketOptions { get; set; } = new Net.SocketOptions();
        public Services.HandshakeOptions HandshakeOptions { get; set; } = new Services.HandshakeOptions();

        private ConcurrentDictionary<string, Acceptor> Acceptors { get; }
            = new ConcurrentDictionary<string, Acceptor>();
        private ConcurrentDictionary<string, Connector> Connectors { get; }
            = new ConcurrentDictionary<string, Connector>();

        public void AddConnector(Connector connector)
        {
            if (!Connectors.TryAdd(connector.Name, connector))
                throw new Exception($"Duplicate Connector={connector.Name}");
        }

        public Connector FindConnector(string name)
        {
            if (Connectors.TryGetValue(name, out var exist))
            {
                return exist;
            }
            return null;
        }

        public Connector FindConnector(string host, int port)
        {
            return FindConnector($"{host}:{port}");
        }

        /// <summary>
        /// 查找，不存在则创建。
        /// </summary>
        /// <param name="host"></param>
        /// <param name="port"></param>
        /// <param name="autoReconnect"></param>
        /// <param name="getOrAdd"></param>
        /// <returns>true if addNew</returns>
        public bool TryGetOrAddConnector(string host, int port, bool autoReconnect, out Connector getOrAdd)
        {
            var name = $"{host}:{port}";
            var addNew = false;
            getOrAdd = Connectors.GetOrAdd(name, (_) =>
            {
                addNew = true;
                return new Connector(host, port, autoReconnect);
            });
            return addNew;
        }

        public void RemoveConnector(Connector c)
        {
            Connectors.TryRemove(c.Name, out var _);
        }

        public void ForEachConnector(Action<Connector> action)
        {
            foreach (var c in Connectors.Values)
            {
                action(c);
            }
        }

        public int ConnectorCount()
        {
            return Connectors.Count;
        }

        public bool ForEachConnector(Func<Connector, bool> func)
        {
            foreach (var c in Connectors.Values)
            {
                if (false == func(c))
                    return false;
            }
            return true;
        }

        public void AddAcceptor(Acceptor a)
        {
            if (!Acceptors.TryAdd(a.Name, a))
                throw new Exception($"Duplicate Acceptor={a.Name}");
        }

        public void RemoveAcceptor(Acceptor a)
        {
            Acceptors.TryRemove(a.Name, out var _);
        }

        public void ForEachAcceptor(Action<Acceptor> action)
        {
            foreach (var a in Acceptors.Values)
            {
                action(a);
            }
        }

        public bool ForEachAcceptor(Func<Acceptor, bool> func)
        {
            foreach (var a in Acceptors.Values)
            {
                if (false == func(a))
                    return false;
            }
            return true;
        }

        public int AcceptorCount()
        {
            return Acceptors.Count;
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
            if (attr.Length > 0) SocketOptions.SendBuffer = int.Parse(attr);
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
            {
                if (!conf.ServiceConfMap.TryAdd(Name, this))
                {
                    throw new Exception($"Duplicate ServiceConf '{Name}'");
                }
            }
            else
            {
                conf.DefaultServiceConf = this;
            }

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
                    case "Connector": AddConnector(Connector.Create(e)); break;
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
}
