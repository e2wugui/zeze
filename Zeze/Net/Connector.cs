using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading;
using System.Threading.Tasks;
using System.Xml;

namespace Zeze.Net
{
    /// <summary>
    /// 连接器：建立并保持一个连接，可以设置自动重连及相关参数。
    /// 可以继承并重载相关事件函数。重载实现里面需要调用 base.OnXXX。
    /// 继承是为了给链接扩充状态，比如：应用的连接需要login，可以维护额外的状态。
    /// 继承类启用方式：
    /// 1. 在配置中通过 class="FullClassName" 的。
    /// 2. 动态创建并加入Service
    /// </summary>
    public class Connector
    {
        public Service Service { get; private set; }

        public string HostNameOrAddress { get; }
        public int Port { get; } = 0;
        public bool IsAutoReconnect { get; } = true;
        public int MaxReconnectDelay { get; set; }
        public bool IsConnected { get; private set; } = false;
        private int ConnectDelay;
        public bool IsHandshakeDone => HandshakeDoneEvent.WaitOne(0);
        public ManualResetEvent HandshakeDoneEvent { get; } = new ManualResetEvent(false);
        public string Name => $"{HostNameOrAddress}:{Port}";

        public AsyncSocket Socket { get; private set; }

        public Connector(string host, int port = 0, bool autoReconnect = true)
        {
            HostNameOrAddress = host;
            Port = port;
            IsAutoReconnect = autoReconnect;
        }

        public static Connector Create(XmlElement e)
        {
            var className = e.GetAttribute("Class");
            return string.IsNullOrEmpty(className)
                    ? new Connector(e)
                    : (Connector)Activator.CreateInstance(Type.GetType(className), e);
        }

        public Connector(XmlElement self)
        {
            string attr = self.GetAttribute("Port");
            if (attr.Length > 0)
                Port = int.Parse(attr);
            HostNameOrAddress = self.GetAttribute("HostNameOrAddress");
            attr = self.GetAttribute("IsAutoReconnect");
            if (attr.Length > 0)
                IsAutoReconnect = bool.Parse(attr);
            attr = self.GetAttribute("MaxReconnectDelay");
            if (attr.Length > 0)
                MaxReconnectDelay = int.Parse(attr) * 1000;
            if (MaxReconnectDelay < 8000)
                MaxReconnectDelay = 8000;
        }

        internal void SetService(Service service)
        {
            lock (this)
            {
                if (Service != null)
                    throw new Exception($"Connector of '{Name}' Service != null");
                Service = service;
            }
        }

        public virtual void OnSocketClose(AsyncSocket closed)
        {
            lock (this)
            {
                if (Socket != closed)
                    return;

                Stop();

                if (false == IsAutoReconnect)
                    return;

                if (ConnectDelay <= 0)
                {
                    ConnectDelay = 1000;
                }
                else
                {
                    ConnectDelay *= 2;
                    if (ConnectDelay > MaxReconnectDelay)
                        ConnectDelay = MaxReconnectDelay;
                }
                Util.Scheduler.Instance.Schedule((ThisTask) => Start(), ConnectDelay); ;
            }
        }

        public virtual void OnSocketConnected(AsyncSocket so)
        {
            lock (this)
            {
                ConnectDelay = 0;
                IsConnected = true;
            }
        }

        public virtual void OnSocketHandshakeDone(AsyncSocket so)
        {
            HandshakeDoneEvent.Set();
        }

        public virtual void Start()
        {
            lock (this)
            {
                if (null != Socket)
                    return;

                IsConnected = false;
                HandshakeDoneEvent.Reset();
                Socket = Service.NewClientSocket(HostNameOrAddress, Port);
                Socket.Connector = this;
            }
        }

        public virtual void Stop()
        {
            lock (this)
            {
                if (null == Socket)
                    return;
                HandshakeDoneEvent.Reset();
                var tmp = Socket;
                Socket = null; // 阻止重连
                tmp.Dispose();
                IsConnected = false;
            }
        }
    }
}
