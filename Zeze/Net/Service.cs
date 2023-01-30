using System;
using System.Collections.Generic;
using Zeze.Serialize;
using System.Collections.Concurrent;
using System.Net;
using System.Net.Sockets;
using System.Net.NetworkInformation;
using Zeze.Util;
using System.Threading.Tasks;
using Zeze.Transaction;
using NLog;
using NLog.Fluent;

namespace Zeze.Net
{
    public class Service
    {
#if HAS_NLOG
        private static readonly NLog.Logger logger = NLog.LogManager.GetCurrentClassLogger();
#elif HAS_MYLOG
        private static readonly Zeze.MyLog logger = global::Zeze.MyLog.GetLogger(typeof(Service));
#endif

        /// <summary>
        /// 同一个 Service 下的所有连接都是用相同配置。
        /// </summary>
        public SocketOptions SocketOptions { get; private set; } = new SocketOptions();
        public ServiceConf Config { get; private set; }
        public Application Zeze { get; }
        public string Name { get; }

        protected ConcurrentDictionary<long, AsyncSocket> SocketMap { get; }
            = new ConcurrentDictionary<long, AsyncSocket>();

        internal ConcurrentDictionary<long, AsyncSocket> SocketMapInternal => SocketMap;

        private void InitConfig(Config config)
        {
            Config = config?.GetServiceConf(Name);
            if (null == Config)
            {
                // setup program default
                Config = new ServiceConf();
                if (null != config)
                {
                    // reference to config default
                    Config.SocketOptions = config.DefaultServiceConf.SocketOptions;
                    Config.HandshakeOptions = config.DefaultServiceConf.HandshakeOptions;
                }
            }
            Config.SetService(this);
            SocketOptions = Config.SocketOptions;
        }

        public Service(string name, Config config)
        {
            Name = name;
            InitConfig(config);
        }

        public Service(string name, Application app)
        {
            Name = name;
            Zeze = app;
            InitConfig(app?.Config);
        }

        public Service(string name)
        {
            Name = name;
        }

        /// <summary>
        /// 只包含成功建立的连接：服务器Accept和客户端Connected的连接。
        /// ServerSocket 保存在 Acceptor 中。这里查询不到。
        /// </summary>
        /// <param name="serialNo"></param>
        /// <returns></returns>
        public virtual AsyncSocket GetSocket(long serialNo)
        {
            if (SocketMap.TryGetValue(serialNo, out var value))
                return value;
            return null;
        }

        public virtual AsyncSocket GetSocket()
        {
            foreach (var e in SocketMap)
            {
                return e.Value;
            }
            return null;
        }

        public virtual void Start()
        {
            Config?.Start();
        }

        public virtual void Stop()
        {
            Config?.Stop();

            foreach (var e in SocketMap)
            {
                e.Value.Dispose(); // remove in callback OnSocketClose
            }

            // 先不清除，让Rpc的TimerTask仍然在超时以后触发回调。
            // 【考虑一下】也许在服务停止时马上触发回调并且清除上下文比较好。
            // 【注意】直接清除会导致同步等待的操作无法继续。异步只会没有回调，没问题。
            // _RpcContexts.Clear();
        }

        public AsyncSocket NewServerSocket(string ipaddress, int port, Acceptor acceptor)
        {
            return new AsyncSocket(this, new IPEndPoint(IPAddress.Parse(ipaddress), port), acceptor);
        }

        public AsyncSocket NewServerSocket(IPAddress ipaddress, int port, Acceptor acceptor)
        {
            return new AsyncSocket(this, new IPEndPoint(ipaddress, port), acceptor);
        }

        public AsyncSocket NewServerSocket(EndPoint localEP, Acceptor acceptor)
        {
            return new AsyncSocket(this, localEP, acceptor);
        }

        public AsyncSocket NewClientSocket(string hostNameOrAddress, int port, object userState, Connector connector)
        {
            return new AsyncSocket(this, hostNameOrAddress, port, userState, connector);
        }

        /// <summary>
        /// ASocket 关闭的时候总是回调。
        /// </summary>
        /// <param name="so"></param>
        /// <param name="e"></param>
        public virtual void OnSocketClose(AsyncSocket so, Exception e)
        {
            SocketMap.TryRemove(new KeyValuePair<long, AsyncSocket>(so.SessionId, so));

            if (null != e)
            {
#if HAS_NLOG
                logger.Log(Mission.NlogLogLevel(SocketOptions.SocketLogLevel), e, "OnSocketClose");
#elif HAS_MYLOG
                logger.Log(SocketOptions.SocketLogLevel, e, "OnSocketClose");
#endif
            }
        }

        /// <summary>
        /// 可靠rpc调用：一般用于重新发送没有返回结果的rpc。
        /// 在 OnSocketClose 之后调用，此时外面【必须】拿不到此 AsyncSocket 了。
        /// 当 OnSocketDisposed 调用发生时，AsyncSocket.Socket已经设为 null。
        /// 对于那些在 AsyncSocket.Dispose 时已经得到的 AsyncSocket 引用，
        /// 使用时判断返回值：主要是 Send 返回 false。
        /// </summary>
        /// <param name="so"></param>
        public virtual void OnSocketDisposed(AsyncSocket so)
        {
            // 一般实现：遍历RpcContexts，
            /*
            var ctxSends = GetRpcContextsToSender(so);
            var ctxPending = RemoveRpcContets(ctxSends.Keys);
            foreach (var ctx in ctxRemoved)
            {
                // process
            }
            */
        }

        public Dictionary<long, Protocol> GetRpcContexts(Func<Protocol, bool> filter)
        {
            var result = new Dictionary<long, Protocol>();
            foreach (var ctx in RpcContexts)
            {
                if (filter(ctx.Value))
                {
                    result.Add(ctx.Key, ctx.Value);
                }
            }
            return result;
        }

        public ICollection<Protocol> RemoveRpcContexts(ICollection<long> sids)
        {
            var result = new List<Protocol>(sids.Count);
            foreach (var sid in sids)
            {
                var ctx = this.RemoveRpcContext<Protocol>(sid);
                if (null != ctx)
                {
                    result.Add(ctx);
                }
            }
            return result;
        }

        /// <summary>
        /// 服务器接受到新连接回调。
        /// </summary>
        /// <param name="so"></param>
        public virtual void OnSocketAccept(AsyncSocket so)
        {
            SocketMap.TryAdd(so.SessionId, so);
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
            sender.Connector?.OnSocketHandshakeDone(sender);
        }

        /// <summary>
        /// 连接失败回调。同时也会回调OnSocketClose。
        /// </summary>
        /// <param name="so"></param>
        /// <param name="e"></param>
        public virtual void OnSocketConnectError(AsyncSocket so, Exception e)
        {
            SocketMap.TryRemove(new KeyValuePair<long, AsyncSocket>(so.SessionId, so));
#if HAS_NLOG
            logger.Log(Mission.NlogLogLevel(SocketOptions.SocketLogLevel), e, "OnSocketConnectError");
#elif HAS_MYLOG
            logger.Log(SocketOptions.SocketLogLevel, e, "OnSocketConnectError");
#endif
        }

        public virtual void OnSocketAcceptError(AsyncSocket listener, Exception e)
        {
#if HAS_NLOG
            logger.Log(Mission.NlogLogLevel(SocketOptions.SocketLogLevel), e, $"OnSocketAcceptError {listener}");
#elif HAS_MYLOG
            logger.Log(SocketOptions.SocketLogLevel, e, $"OnSocketAcceptError {listener}");
#endif
        }

        /// <summary>
        /// 连接成功回调。
        /// </summary>
        /// <param name="so"></param>
        public virtual void OnSocketConnected(AsyncSocket so)
        {
            SocketMap.TryAdd(so.SessionId, so);
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

        // 用来派发异步rpc回调。
        public virtual void DispatchRpcResponse(Protocol rpc,
            Func<Protocol, Task<long>> responseHandle,
            ProtocolFactoryHandle factoryHandle)
        {
#if !USE_CONFCS
            if (null != Zeze && Transaction.TransactionLevel.None != factoryHandle.TransactionLevel)
            {
                _ = Mission.CallAsync(Zeze.NewProcedure(async () => await responseHandle(rpc),
                    rpc.GetType().FullName + ":Response", factoryHandle.TransactionLevel, rpc.UserState), rpc, null);
            }
            else
#endif
            {
                _ = Mission.CallAsync(responseHandle, rpc, null);
            }
        }

        public virtual void DispatchProtocol2(object key, Protocol p, ProtocolFactoryHandle factoryHandle)
        {
            if (null != factoryHandle.Handle)
            {
#if !USE_CONFCS
                if (null != Zeze && Transaction.TransactionLevel.None != factoryHandle.TransactionLevel)
                {
                    Zeze.TaskOneByOneByKey.Execute(key, Zeze.NewProcedure(
                            () => factoryHandle.Handle(p), p.GetType().FullName,
                            factoryHandle.TransactionLevel, p.UserState),
                            p, (p, code) => p.TrySendResultCode(code)
                        );
                }
                else
                {
                    Zeze.TaskOneByOneByKey.Execute(key, factoryHandle.Handle, p, (p, code) => p.TrySendResultCode(code));
                }
#else
                _ = Mission.CallAsync(factoryHandle.Handle, p, (p, code) => p.TrySendResultCode(code));
#endif
            }
            else
            {
#if HAS_NLOG
                logger.Log(Mission.NlogLogLevel(SocketOptions.SocketLogLevel), "Protocol Handle Not Found. {0}", p);
#elif HAS_MYLOG
                logger.Log(SocketOptions.SocketLogLevel, "Protocol Handle Not Found. {0}", p);
#endif
            }
        }

        public virtual bool IsHandshakeProtocol(long typeId)
        {
            return false;
        }

        public virtual async void DispatchProtocol(Protocol p, ProtocolFactoryHandle factoryHandle)
        {
            if (null != factoryHandle.Handle)
            {
                if (IsHandshakeProtocol(p.TypeId))
                {
                    // handshake protocol call direct in io-thread.
                    await Mission.CallAsync(factoryHandle.Handle, p, null);
                }
#if !USE_CONFCS
                else if (null != Zeze && Transaction.TransactionLevel.None != factoryHandle.TransactionLevel)
                {
                    _ = Mission.CallAsync(Zeze.NewProcedure(() => factoryHandle.Handle(p),
                        p.GetType().FullName, factoryHandle.TransactionLevel, p.UserState), p, null);
                }
#endif
                else
                {
                    _ = Mission.CallAsync(factoryHandle.Handle, p, null);
                }
            }
            else
            {
#if HAS_NLOG
                logger.Log(Mission.NlogLogLevel(SocketOptions.SocketLogLevel), "Protocol Handle Not Found. {0}", p);
#elif HAS_MYLOG
                logger.Log(SocketOptions.SocketLogLevel, "Protocol Handle Not Found. {0}", p);
#endif
            }
        }

        public virtual void DispatchUnknownProtocol(AsyncSocket so, int moduleId, int protocolId, ByteBuffer data)
        {
            throw new Exception($"Unknown Protocol=({moduleId}, {protocolId}) size={data.Size}");
        }

        ////////////////////////////////////////////////////////////////////////////////////////////////
        /// 协议工厂
        public class ProtocolFactoryHandle
        {
            public Func<Protocol> Factory { get; set; }
            public Func<Protocol, Task<long>> Handle { get; set; }
            public Transaction.TransactionLevel TransactionLevel { get; set; } = TransactionLevel.Serializable;
            public bool NoProcedure => TransactionLevel == TransactionLevel.None;

            // 协议请求的派发（线程）模式。【警告，现在这个参数同时用于服务器和客户端，如果协议收发都需要处理时，无法支持两种派发模式】
            public DispatchMode Mode = DispatchMode.Normal;

            /////////////////////////////////////////////////////////////////////////////////////////////
            // 由于c#只有一个线程池，所以默认情况下，没有使用线程派发模式，需要的应用重载下面的方法，自行使用Mode配置。
            // DispatchProtocol,DispatchProtocol2,DispatchRpcResponse
        }

        public ConcurrentDictionary<long, ProtocolFactoryHandle> Factorys { get; }
            = new ConcurrentDictionary<long, ProtocolFactoryHandle>();

        public void AddFactoryHandle(long type, ProtocolFactoryHandle factory)
        {
            if (false == Factorys.TryAdd(type, factory))
            {
                Factorys.TryGetValue(type, out var exist);
                var existType = exist.Factory().GetType();
                throw new Exception($"duplicate factory type={type} moduleid={(type >> 32) & 0x7fff} id={type & 0x7fff} exist={existType}");
            }
        }

        /* target: 静态方法可以传null */
        /*
        public static Func<Protocol, int> MakeHandle<T>(object target , MethodInfo method) where T : Protocol
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
        */

        public ProtocolFactoryHandle FindProtocolFactoryHandle(long type)
        {
            if (Factorys.TryGetValue(type, out ProtocolFactoryHandle factory))
            {
                return factory;
            }

            return null;
        }

        ////////////////////////////////////////////////////////////////////////////////////////////////
        /// Rpc Context. 模板不好放进去，使用基类 Protocol
        private static AtomicLong StaticSessionIdAtomicLong { get; } = new AtomicLong();
        public Func<long> SessionIdGenerator { get; set; }

        private readonly ConcurrentDictionary<long, Protocol> RpcContextsPrivate = new();
        public IReadOnlyDictionary<long, Protocol> RpcContexts => RpcContextsPrivate;

        public long NextSessionId()
        {
            if (null != SessionIdGenerator)
                return SessionIdGenerator();
            return StaticSessionIdAtomicLong.IncrementAndGet();
        }

        internal long AddRpcContext(Protocol p)
        {
            while (true)
            {
                long sessionId = NextSessionId();
                if (RpcContextsPrivate.TryAdd(sessionId, p))
                {
                    return sessionId;
                }
            }
        }

        internal void TryRemoveRpcContext(long sid, Protocol current)
        {
            RpcContextsPrivate.TryRemove(new KeyValuePair<long, Protocol>(sid, current));
        }

        internal T RemoveRpcContext<T>(long sid) where T : Protocol
        {
            if (RpcContextsPrivate.TryRemove(sid, out var p))
            {
                return (T)p;
            }
            return null;
        }

        public abstract class ManualContext
        {
            public long SessionId { get; internal set; }
            public object UserState { get; set; }
            public Service Service { get; set; }
            public bool IsTimeout { get; internal set; } = false;

            public virtual void OnRemoved()
            {
            }
        }

        private readonly ConcurrentDictionary<long, ManualContext> ManualContexts = new();

        public long AddManualContextWithTimeout(ManualContext context, long timeout = 10 * 1000)
        {
            while (true)
            {
                long sessionId = NextSessionId();
                if (ManualContexts.TryAdd(sessionId, context))
                {
                    context.SessionId = sessionId;
                    context.Service = this;
                    Util.Scheduler.Schedule((ThisTask) => TryRemoveManualContext<ManualContext>(sessionId, true), timeout);
                    return sessionId;
                }
            }
        }

        public T TryGetManualContext<T>(long sessionId) where T : ManualContext
        {
            if (ManualContexts.TryGetValue(sessionId, out var c))
                return (T)c;
            return null;
        }

        public T TryRemoveManualContext<T>(long sessionId) where T : ManualContext
        {
            return TryRemoveManualContext<T>(sessionId, false);
        }

        private T TryRemoveManualContext<T>(long sessionId, bool isTimeout) where T : ManualContext
        {
            if (ManualContexts.TryRemove(sessionId, out var c))
            {
                c.IsTimeout = isTimeout;
                c.OnRemoved();
                return (T)c;
            }
            return null;
        }

        // 还是不直接暴露内部的容器。提供这个方法给外面用。以后如果有问题，可以改这里。
        public void Foreach(Action<AsyncSocket> action)
        {
            foreach (var socket in SocketMap.Values)
            {
                action(socket);
            }
        }

        public static string GetOneNetworkInterfaceIpAddress(AddressFamily family = AddressFamily.Unspecified)
        {
            foreach (NetworkInterface neti in NetworkInterface.GetAllNetworkInterfaces())
            {
                if (neti.NetworkInterfaceType == NetworkInterfaceType.Loopback)
                    continue;

                IPInterfaceProperties property = neti.GetIPProperties();
                foreach (UnicastIPAddressInformation ip in property.UnicastAddresses)
                {
                    switch (ip.Address.AddressFamily)
                    {
                        case AddressFamily.InterNetworkV6:
                        case AddressFamily.InterNetwork:
                            if (family == AddressFamily.Unspecified
                                || family == ip.Address.AddressFamily)
                            {
                                return ip.Address.ToString();
                            }
                            continue;
                    }
                }
            }
            return null;
        }

        public (string, int) GetOneAcceptorAddress()
        {
            string ip = string.Empty;
            int port = 0;

            Config.ForEachAcceptor(
                (a) =>
                {
                    if (false == string.IsNullOrEmpty(a.Ip) && a.Port != 0)
                    {
                        // 找到ip，port都配置成明确地址的。
                        ip = a.Ip;
                        port = a.Port;
                        return false;
                    }
                    // 获得最后一个配置的port。允许返回(null, port)。
                    port = a.Port;
                    return true;
                });

            return (ip, port);
        }

        public (string, int) GetOnePassiveAddress()
        {
            var (ip, port) = GetOneAcceptorAddress();
            if (port == 0)
                throw new Exception("Acceptor: No Config.");

            if (string.IsNullOrEmpty(ip))
            {
                // 可能绑定在任意地址上。尝试获得网卡的地址。
                ip = GetOneNetworkInterfaceIpAddress();
                if (string.IsNullOrEmpty(ip))
                {
                    // 实在找不到ip地址，就设置成loopback。
#if HAS_NLOG || HAS_MYLOG
                    logger.Warn("PassiveAddress No Config. set ip to 127.0.0.1");
#endif
                    ip = "127.0.0.1";
                }
            }
            return (ip, port);
        }

        public bool CheckThrottle(AsyncSocket sender, int size)
        {
#if !USE_CONFCS
            var throttle = sender.TimeThrottle;
            if (null != throttle && false == throttle.CheckNow(size))
            {
                // TrySendResultCode(Zeze.Util.ResultCode.Busy); // 超过速度限制，不报告错误。因为可能是一种攻击。
                sender.Dispose();
                return false; // 超过速度控制，丢弃这条协议。
            }
#endif
            return true;
        }
    }
}
