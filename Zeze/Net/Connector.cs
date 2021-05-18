using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
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
        public string HostNameOrAddress { get; }
        public int Port { get; } = 0;
        public bool IsAutoReconnect { get; } = true;
        public int MaxReconnectDelay { get; set; }
        public bool IsConnected { get; private set; } = false;
        private int ConnectDelay;
        public bool IsHandshakeDone => Socket != null && Socket.IsHandshakeDone;
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

        public virtual void OnSocketClose(AsyncSocket closed)
        {
            if (Socket != closed)
                return;

            Socket = null;

            if (false == IsAutoReconnect)
                return;

            if (ConnectDelay == 0)
            {
                ConnectDelay = 2000;
            }
            else
            {
                ConnectDelay *= 2;
                if (ConnectDelay > MaxReconnectDelay)
                    ConnectDelay = MaxReconnectDelay;
            }
            var service = closed.Service;
            Util.Scheduler.Instance.Schedule((ThisTask) => Start(service), ConnectDelay); ;
        }

        public virtual void OnSocketConnected(AsyncSocket so)
        {
            ConnectDelay = 0;
            IsConnected = true;
        }

        public virtual void OnSocketHandshakeDone(AsyncSocket so)
        {
        }

        public virtual void Start(Service service)
        {
            Socket?.Dispose();
            IsConnected = false;
            Socket = service.NewClientSocket(HostNameOrAddress, Port);
            Socket.Connector = this;
        }

        public virtual void Stop(Service service)
        {
            Socket?.Dispose();
            Socket = null;
        }
    }
}
