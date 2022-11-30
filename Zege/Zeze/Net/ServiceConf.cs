using System;
using System.Collections.Generic;
using System.Xml;
using System.Collections.Concurrent;
using Zeze.Services;
using System.Threading.Tasks;

namespace Zeze.Net
{
    public sealed class ServiceConf
    {
        public Service Service { get; private set; }
        public string Name { get; }
        public SocketOptions SocketOptions { get; set; } = new SocketOptions();
        public HandshakeOptions HandshakeOptions { get; set; } = new HandshakeOptions();

        private ConcurrentDictionary<string, Acceptor> Acceptors { get; }
            = new ConcurrentDictionary<string, Acceptor>();
        private ConcurrentDictionary<string, Connector> Connectors { get; }
            = new ConcurrentDictionary<string, Connector>();

        internal void SetService(Service service)
        {
            lock (this)
            {
                if (Service != null)
                    throw new Exception($"ServiceConf of '{Name}' Service != null");
                Service = service;
                ForEachAcceptor((a) => a.SetService(service));
                ForEachConnector((c) => c.SetService(service));
            }
        }

        public void AddConnector(Connector connector)
        {
            if (!Connectors.TryAdd(connector.Name, connector))
                throw new Exception($"Duplicate Connector={connector.Name}");
            connector.SetService(Service);
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

            bool addNew = false;

            getOrAdd = Connectors.GetOrAdd(name, (_) =>
            {
                var add = new Connector(host, port, autoReconnect);
                add.SetService(Service);
                addNew = true;
                return add;
            });

            return addNew;
        }

        public void RemoveConnector(Connector c)
        {
            Connectors.TryRemove(new KeyValuePair<string, Connector>(c.Name, c));
        }

        public void ForEachConnector(Action<Connector> action)
        {
            foreach (var c in Connectors.Values)
            {
                action(c);
            }
        }

        public async Task ForEachConnectorAsync(Func<Connector, Task> action)
        {
            foreach (var c in Connectors.Values)
            {
                await action(c);
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
            a.SetService(Service);
        }

        public void RemoveAcceptor(Acceptor a)
        {
            Acceptors.TryRemove(new KeyValuePair<string, Acceptor>(a.Name, a));
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
            if (attr.Length > 0) SocketOptions.SocketLogLevel = (Config.LogLevel)Enum.Parse(typeof(Config.LogLevel), attr);

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
            attr = self.GetAttribute("EnableEncrypt");
            if (attr.Length > 0) HandshakeOptions.EnableEncrypt = bool.Parse(attr);

            if (string.IsNullOrEmpty(Name))
            {
                conf.DefaultServiceConf = this;
            }
            else if (!conf.ServiceConfMap.TryAdd(Name, this))
            {
                throw new Exception($"Duplicate ServiceConf '{Name}'");
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

        public void Start()
        {
            ForEachAcceptor((a) => a.Start());
            ForEachConnector((c) => c.Start());
        }

        public void Stop()
        {
            ForEachAcceptor((a) => a.Stop());
            ForEachConnector((c) => c.Stop());
        }

        public void StopListen()
        {
            ForEachAcceptor((a) => a.Stop());
        }
    }
}
