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

        public Service(string name, Config config)
        {
            Name = name;
            Config = config.GetServiceConf(name);
            SocketOptions = Config.SocketOptions;
        }

        public Service(string name, Application app)
        {
            Name = name;
            Zeze = app;

            if (null != app)
            {
                Config = app.Config.GetServiceConf(name);
                SocketOptions = Config.SocketOptions;
            }
            else
            {
                Config = new Zeze.Config.ServiceConf();
            }
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
            Config?.Start(this);
        }

        public virtual void Close()
        {
            Config?.Stop(this);

            foreach (var e in _asocketMap)
            {
                e.Value.Dispose(); // remove in callback OnSocketClose
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

        public AsyncSocket NewClientSocket(string hostNameOrAddress, int port, object userState = null)
        {
            return new AsyncSocket(this, hostNameOrAddress, port, userState);
        }

        /// <summary>
        /// ASocket 关闭的时候总是回调。
        /// </summary>
        /// <param name="so"></param>
        /// <param name="e"></param>
        public virtual void OnSocketClose(AsyncSocket so, Exception e)
        {
            _asocketMap.TryRemove(so.SessionId, out var _);

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

        // 用来派发异步rpc回调。
        public virtual void DispatchRpcResponse(Protocol rpc, Func<Protocol, int> responseHandle, ProtocolFactoryHandle factoryHandle)
        {
            if (null != Zeze && false == factoryHandle.NoProcedure)
            {
                global::Zeze.Util.Task.Run(Zeze.NewProcedure(() => responseHandle(rpc), rpc.GetType().FullName + ":Response", rpc.UserState));
            }
            else
            {
                global::Zeze.Util.Task.Run(() => responseHandle(rpc), rpc);
            }
        }

        public virtual void DispatchProtocol(Protocol p, ProtocolFactoryHandle factoryHandle)
        {
            if (null != factoryHandle.Handle)
            {
                if (null != Zeze && false == factoryHandle.NoProcedure)
                {
                    global::Zeze.Util.Task.Run(Zeze.NewProcedure(() => factoryHandle.Handle(p), p.GetType().FullName, p.UserState));
                }
                else
                {
                    global::Zeze.Util.Task.Run(() => factoryHandle.Handle(p), p);
                }
            }
            else
            {
                logger.Log(SocketOptions.SocketLogLevel, "Protocol Handle Not Found. {0}", p);
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
            public Func<Protocol, int> Handle { get; set; }
            public bool NoProcedure { get; set; } = false;
        }

        public ConcurrentDictionary<int, ProtocolFactoryHandle> Factorys { get; } = new ConcurrentDictionary<int, ProtocolFactoryHandle>();

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

        public ProtocolFactoryHandle FindProtocolFactoryHandle(int type)
        {
            if (Factorys.TryGetValue(type, out ProtocolFactoryHandle factory))
            {
                return factory;
            }

            return null;
        }

        ////////////////////////////////////////////////////////////////////////////////////////////////
        /// Rpc Context. 模板不好放进去，使用基类 Protocol
        private Util.AtomicLong SessionIdGen = new Util.AtomicLong();
        private readonly ConcurrentDictionary<long, Protocol> RpcContexts = new ConcurrentDictionary<long, Protocol>();

        public long NextSessionId()
        {
            return SessionIdGen.IncrementAndGet();
        }

        internal long AddRpcContext(Protocol p)
        {
            while (true)
            {
                long sessionId = SessionIdGen.IncrementAndGet();
                if (RpcContexts.TryAdd(sessionId, p))
                {
                    return sessionId;
                }
            }
        }

        internal T RemoveRpcContext<T>(long sid) where T : Protocol
        {
            if (RpcContexts.TryRemove(sid, out var p))
            {
                return (T)p;
            }
            return null;
        }

        public abstract class ManualContext
        {
            public long SessionId { get; internal set; }
            public object UserState { get; set; }

            public virtual void OnRemoved()
            {
            }

            // after OnRemoved if Timeout
            public virtual void OnTimeout()
            {
            }

        }

        private readonly ConcurrentDictionary<long, ManualContext> ManualContexts = new ConcurrentDictionary<long, ManualContext>();

        public long AddManualContextWithTimeout(ManualContext context, long timeout = 10*1000)
        {
            while (true)
            {
                long sessionId = SessionIdGen.IncrementAndGet();
                if (ManualContexts.TryAdd(sessionId, context))
                {
                    context.SessionId = sessionId;
                    Util.Scheduler.Instance.Schedule((ThisTask) => TryRemoveManualContext<ManualContext>(sessionId)?.OnTimeout(), timeout);
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
            if (ManualContexts.TryRemove(sessionId, out var c))
            {
                c.OnRemoved();
                return (T)c;
            }
            return null;
        }

        // 还是不直接暴露内部的容器。提供这个方法给外面用。以后如果有问题，可以改这里。
        public void Foreach(Action<AsyncSocket> action)
        {
            foreach (var socket in _asocketMap.Values)
            {
                action(socket);
            }
        }
    }
}
