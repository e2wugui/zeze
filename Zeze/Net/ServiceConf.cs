using System;
using System.Collections.Generic;
using System.Xml;
using System.Collections.Concurrent;
using System.IO;
using System.Net;
using Zeze.Services;
using System.Threading.Tasks;
using Zeze.Util;

namespace Zeze.Net
{
    public sealed class ServiceConf
    {
        public Service Service { get; private set; }
        public readonly string Name;
        public SocketOptions SocketOptions = new SocketOptions();
        public HandshakeOptions HandshakeOptions = new HandshakeOptions();

        private readonly ConcurrentDictionary<string, Acceptor> Acceptors
            = new ConcurrentDictionary<string, Acceptor>();

        private readonly ConcurrentDictionary<string, Connector> Connectors
            = new ConcurrentDictionary<string, Connector>();

        internal void SetService(Service service)
        {
            lock (this)
            {
                if (Service != null)
                    throw new Exception($"ServiceConf of '{Name}' Service != null");
                Service = service;
                ForEachAcceptor(a => a.SetService(service));
                ForEachConnector(c => c.SetService(service));
            }
        }

        public void AddConnector(Connector connector)
        {
            connector.SetService(Service);
            if (!Connectors.TryAdd(connector.Name, connector))
                throw new Exception($"Duplicate Connector={connector.Name}");
        }

        public Connector FindConnector(string name)
        {
            return Connectors.TryGetValue(name, out var exist) ? exist : null;
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
            bool addNew = false;
            getOrAdd = Connectors.GetOrAdd($"{host}:{port}", _ =>
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
                action(c);
        }

        public async Task ForEachConnectorAsync(Func<Connector, Task> action)
        {
            foreach (var c in Connectors.Values)
                await action(c);
        }

        public int ConnectorCount()
        {
            return Connectors.Count;
        }

        public bool ForEachConnector(Func<Connector, bool> func)
        {
            foreach (var c in Connectors.Values)
            {
                if (!func(c))
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
                action(a);
        }

        public bool ForEachAcceptor(Func<Acceptor, bool> func)
        {
            foreach (var a in Acceptors.Values)
            {
                if (!func(a))
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

            // SocketOptions
            var attr = self.GetAttribute("NoDelay");
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
            if (attr.Length > 0)
                SocketOptions.SocketLogLevel = (LogLevel)Enum.Parse(typeof(LogLevel), attr);
            attr = self.GetAttribute("TimeThrottleSeconds");
            if (attr.Length > 0) SocketOptions.TimeThrottleSeconds = int.Parse(attr);
            attr = self.GetAttribute("TimeThrottleLimit");
            if (attr.Length > 0) SocketOptions.TimeThrottleLimit = int.Parse(attr);
            attr = self.GetAttribute("TimeThrottleBandwidth");
            if (attr.Length > 0) SocketOptions.TimeThrottleBandwidth = int.Parse(attr);
            attr = self.GetAttribute("OverBandwidth");
            if (attr.Length > 0) SocketOptions.OverBandwidth = long.Parse(attr);
            attr = self.GetAttribute("OverBandwidthFusingRate");
            if (attr.Length > 0) SocketOptions.OverBandwidthFusingRate = double.Parse(attr);
            attr = self.GetAttribute("OverBandwidthNormalRate");
            if (attr.Length > 0) SocketOptions.OverBandwidthNormalRate = double.Parse(attr);

            // HandshakeOptions
            attr = self.GetAttribute("DhGroups");
            if (attr.Length > 0)
            {
                HandshakeOptions.DhGroups = new HashSet<int>();
                foreach (string dg in attr.Split(','))
                {
                    string dgTmp = dg.Trim();
                    if (dgTmp.Length == 0)
                        continue;
                    HandshakeOptions.AddDhGroup(int.Parse(dgTmp));
                }
            }
            attr = self.GetAttribute("SecureIp");
            if (attr.Length > 0) HandshakeOptions.SecureIp = IPAddress.Parse(attr).GetAddressBytes();
            attr = self.GetAttribute("RsaPubKey");
            if (attr.Length > 0) HandshakeOptions.LoadRsaPubKey(Str.toBytes(attr));
            attr = self.GetAttribute("RsaPriKeyFile");
            if (attr.Length > 0) HandshakeOptions.LoadRsaPriKey(File.ReadAllBytes(attr));
            attr = self.GetAttribute("CompressS2c");
            if (attr.Length > 0) HandshakeOptions.CompressS2c = int.Parse(attr);
            attr = self.GetAttribute("CompressC2s");
            if (attr.Length > 0) HandshakeOptions.CompressC2s = int.Parse(attr);
            attr = self.GetAttribute("EncryptType");
            if (attr.Length > 0) HandshakeOptions.EncryptType = int.Parse(attr);

            attr = self.GetAttribute("KeepCheckPeriod");
            if (attr.Length > 0) HandshakeOptions.KeepCheckPeriod = int.Parse(attr);
            attr = self.GetAttribute("KeepSendTimeout");
            if (attr.Length > 0) HandshakeOptions.KeepSendTimeout = int.Parse(attr);
            attr = self.GetAttribute("KeepRecvTimeout");
            if (attr.Length > 0) HandshakeOptions.KeepRecvTimeout = int.Parse(attr);

            if (string.IsNullOrEmpty(Name))
                conf.DefaultServiceConf = this;
            else if (!conf.ServiceConfMap.TryAdd(Name, this))
                throw new Exception($"Duplicate ServiceConf '{Name}'");

            // connection creator options
            foreach (XmlNode node in self.ChildNodes)
            {
                if (node.NodeType != XmlNodeType.Element)
                    continue;

                var e = (XmlElement)node;
                switch (e.Name)
                {
                    case "Acceptor":
                        AddAcceptor(new Acceptor(e));
                        break;
                    case "Connector":
                        AddConnector(Connector.Create(e));
                        break;
                    default: throw new Exception("unknown node name: " + e.Name);
                }
            }
        }

        public void Start()
        {
            ForEachAcceptor(a => a.Start());
            ForEachConnector(c => c.Start());
        }

        public void Stop()
        {
            ForEachAcceptor(a => a.Stop());
            ForEachConnector(c => c.Stop());
        }

        public void StopListen()
        {
            ForEachAcceptor(a => a.Stop());
        }
    }
}
