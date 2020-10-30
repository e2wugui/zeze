using System;
using System.Collections.Generic;
using System.Reflection;
using System.Text;
using System.Threading.Tasks;
using Zeze.Serialize;
using System.Collections.Concurrent;
using Zeze.Transaction;
using System.Net;

namespace Zeze.Net
{
    public class Service
    {
        private static readonly NLog.Logger logger = NLog.LogManager.GetCurrentClassLogger();

        /// <summary>
        /// 同一个 Service 下的所有连接都是用相同配置。
        /// </summary>
        public SocketOptions SocketOptions { get; private set; } = new SocketOptions();
        public Config.ServiceConf Config { get; private set; }
        public Application Zeze { get; }
        public string Name { get; }

        protected readonly ConcurrentDictionary<long, AsyncSocket> _asocketMap = new ConcurrentDictionary<long, AsyncSocket>();

        public Service(string name, Application zeze)
        {
            Name = name;
            Zeze = zeze;

            Config = zeze.Config.GetServiceConf(name);
            SocketOptions = Config.SocketOptions;
        }

        public Service(string name)
        {
            Name = name;
        }

        /// <summary>
        /// 只包含成功建立的连接：服务器Accept和客户端Connected的连接。
        /// </summary>
        /// <param name="serialNo"></param>
        /// <returns></returns>
        public virtual AsyncSocket GetSocket(long serialNo)
        {
            if (_asocketMap.TryGetValue(serialNo, out var value))
                return value;
            return null;
        }

        public virtual AsyncSocket GetSocket()
        {
            foreach (var e in _asocketMap)
            {
                return e.Value;
            }
            return null;
        }

        public virtual void Start()
        {
            // 这里不判断是否重复Start了。
            foreach (var a in Config.Acceptors)
            {
                a.Socket?.Dispose();
                a.Socket = a.Ip.Length > 0 ? NewServerSocket(a.Ip, a.Port) : NewServerSocket(System.Net.IPAddress.Any, a.Port);
            }
            foreach (var c in Config.Connectors)
            {
                c.Connect(this);
                //_asocketMap.TryAdd(c.Socket.SessionId, c.Socket); // 连接成功才加入map。
            }
        }

        public virtual void Close()
        {
            foreach (var a in Config.Acceptors)
            {
                a.Socket?.Dispose();
                a.Socket = null;
            }

            foreach (var c in Config.Connectors)
            {
                c.Socket?.Dispose();
                c.Socket = null;
            }

            foreach (var e in _asocketMap)
            {
                e.Value.Dispose(); // remove in callback OnSocketClose
            }
        }

        // 用于控制是否接受新连接
        public virtual void StopListen()
        {
            foreach (var a in Config.Acceptors)
            {
                a.Socket?.Dispose();
                a.Socket = null;
            }
        }

        public AsyncSocket NewServerSocket(string ipaddress, int port)
        {
            return NewServerSocket(IPAddress.Parse(ipaddress), port);
        }

        public AsyncSocket NewServerSocket(IPAddress ipaddress, int port)
        {
            return NewServerSocket(new IPEndPoint(ipaddress, port));
        }

        public AsyncSocket NewServerSocket(EndPoint localEP)
        {
            return new AsyncSocket(this, localEP);
        }

        public AsyncSocket NewClientSocket(string hostNameOrAddress, int port)
        {
            return new AsyncSocket(this, hostNameOrAddress, port);
        }

        /// <summary>
        /// ASocket 关闭的时候总是回调。
        /// </summary>
        /// <param name="so"></param>
        /// <param name="e"></param>
        public virtual void OnSocketClose(AsyncSocket so, Exception e)
        {
            _asocketMap.TryRemove(so.SessionId, out var _);

            foreach (var c in Config.Connectors)
            {
                c.OnSocketClose(this, so);
            }

            if (null != e)
                logger.Log(SocketOptions.SocketLogLevel, e, "OnSocketClose");
        }

        /// <summary>
        /// 服务器接受到新连接回调。
        /// </summary>
        /// <param name="so"></param>
        public virtual void OnSocketAccept(AsyncSocket so)
        {
            _asocketMap.TryAdd(so.SessionId, so);
            OnHandshakeDone(so);
        }

        /// <summary>
        /// 连接完成建立调用。
        /// 未加密压缩的连接在 OnSocketAccept OnSocketConnected 里面调用这个方法。
        /// 加密压缩的连接在相应的方法中调用（see Services\Handshake.cs）。
        /// 注意：修改OnHandshakeDone的时机，需要重载OnSocketAccept OnSocketConnected，并且不再调用Service的默认实现。
        /// </summary>
        public virtual void OnHandshakeDone(AsyncSocket sender)
        {
            sender.IsHandshakeDone = true;
        }

        /// <summary>
        /// 连接失败回调。同时也会回调OnSocketClose。
        /// </summary>
        /// <param name="so"></param>
        /// <param name="e"></param>
        public virtual void OnSocketConnectError(AsyncSocket so, Exception e)
        {
            _asocketMap.TryRemove(so.SessionId, out var _);
            foreach (var c in Config.Connectors)
            {
                c.OnSocketClose(this, so);
            }
            logger.Log(SocketOptions.SocketLogLevel, e, "OnSocketConnectError");
        }

        /// <summary>
        /// 连接成功回调。
        /// </summary>
        /// <param name="so"></param>
        public virtual void OnSocketConnected(AsyncSocket so)
        {
            _asocketMap.TryAdd(so.SessionId, so);
            OnHandshakeDone(so);
        }

        /// <summary>
        /// 处理数据。
        /// 在异步线程中回调，要注意线程安全。
        /// </summary>
        /// <param name="so"></param>
        /// <param name="input"></param>
        public virtual void OnSocketProcessInputBuffer(AsyncSocket so, ByteBuffer input)
        {
            Protocol.Decode(this, so, input);
        }

        public enum DispatchType
        {
            Request = 0,
            Response = 1,
            Timeout = 2,
        }

        public Func<Protocol, int> GetProtocolHandle(int typeid, DispatchType dispatchType, out bool noProcedure)
        {
            if (Factorys.TryGetValue(typeid, out var protocolFactoryHandle))
            {
                noProcedure = protocolFactoryHandle.NoProcedure;
                switch (dispatchType)
                {
                    case DispatchType.Request: return protocolFactoryHandle.HandleRequest;
                    case DispatchType.Response: return protocolFactoryHandle.HandleResponse;
                    case DispatchType.Timeout: return protocolFactoryHandle.HandleTimeout;
                }
            }
            noProcedure = true;
            return null;
        }

        public virtual void DispatchProtocol(Protocol p, DispatchType dispatchType)
        {
            Func<Protocol, int> handle = GetProtocolHandle(p.TypeId, dispatchType, out var noProcedure);
            if (null != handle)
            {
                if (null != Zeze && false == noProcedure)
                {
                    Task.Run(Zeze.NewProcedure(() => handle(p), p.GetType().FullName).Call);
                }
                else
                {
                    Task.Run(() => handle(p));
                }
            }
            else
            {
                logger.Log(SocketOptions.SocketLogLevel, "Protocol Handle Not Found. {0}@{1}", p, dispatchType);
            }
        }

        public virtual void DispatchUnknownProtocol(AsyncSocket so, int type, ByteBuffer data)
        {
            throw new Exception("Unknown Protocol (" + (type >> 16 & 0xffff) + ", " + (type & 0xffff) + ") size=" + data.Size);
        }

        ////////////////////////////////////////////////////////////////////////////////////////////////
        /// 协议工厂
        public class ProtocolFactoryHandle
        { 
            public Func<Protocol> Factory { get; set; }
            public Func<Protocol, int> HandleRequest { get; set; }
            public Func<Protocol, int> HandleResponse { get; set; }
            public Func<Protocol, int> HandleTimeout { get; set; }
            public bool NoProcedure { get; set; } = false;
        }

        private ConcurrentDictionary<int, ProtocolFactoryHandle> Factorys { get; } = new ConcurrentDictionary<int, ProtocolFactoryHandle>();

        public void AddFactoryHandle(int type, ProtocolFactoryHandle factory)
        {
            if (false == Factorys.TryAdd(type, factory))
                throw new Exception($"duplicate factory type={type} moduleid={(type >> 16) & 0x7fff} id={type & 0x7fff}");
        }

        public static Func<Protocol, int> MakeHandle<T>(object target /*静态方法可以传null*/, MethodInfo method) where T : Protocol
        {
            return (Protocol p) =>
            {
                if (method.IsStatic)
                {
                    var handler = Delegate.CreateDelegate(typeof(Func<T, int>), method);
                    return ((Func<T, int>)handler)((T)p);
                }
                else 
                {
                    var handler = Delegate.CreateDelegate(typeof(Func<T, int>), target, method);
                    return ((Func<T, int>)handler)((T)p);
                }
            };
        }

        public Protocol CreateProtocol(int type, ByteBuffer bb)
        {
            if (false == Factorys.TryGetValue(type, out ProtocolFactoryHandle factory))
            {
                return null;
            }

            Protocol p = factory.Factory();
            p.Decode(bb);
            return p;
        }

        ////////////////////////////////////////////////////////////////////////////////////////////////
        /// Rpc Context. 模板不好放进去，使用基类 Protocol
        private long serialId = 0;
        private readonly Dictionary<long, Protocol> contexts = new Dictionary<long, Protocol>();

        internal long AddRpcContext(Protocol p)
        {
            lock (contexts)
            {
                while (true)
                {
                    ++serialId;
                    if (serialId <= 0) // 高位保留给rpc，用来区分是否请求. 另外保留 0。
                        serialId = 1;

                    if (contexts.TryAdd(serialId, p))
                    {
                        return serialId;
                    }
                }
            }
        }

        internal T RemoveRpcContext<T>(long sid) where T : Protocol
        {
            lock (contexts)
            {
                contexts.Remove(sid, out Protocol p);
                return (T)p;
            }
        }
    }
}
