using System;
using System.Collections.Generic;
using System.Reflection;
using System.Text;
using System.Threading.Tasks;
using Zeze.Serialize;
using System.Collections.Concurrent;

namespace Zeze.Net
{
    public class Manager
    {
        private static readonly NLog.Logger logger = NLog.LogManager.GetCurrentClassLogger();

        /// <summary>
        /// 同一个 Manager 下的所有连接都是用相同配置。
        /// </summary>
        public SocketOptions SocketOptions { get; set; }

        private Dictionary<long, AsyncSocket> _asocketMap = new Dictionary<long, AsyncSocket>();

        public Manager()
        {
            this.SocketOptions = new SocketOptions();
        }

        /// <summary>
        /// 只包含成功建立的连接：服务器Accept和客户端Connected的连接。
        /// </summary>
        /// <param name="serialNo"></param>
        /// <returns></returns>
        public virtual AsyncSocket GetSocket(long serialNo)
        {
            lock (_asocketMap)
            {
                AsyncSocket value = null;
                if (_asocketMap.TryGetValue(serialNo, out value))
                    return value;
                return null;
            }
        }

        public virtual AsyncSocket GetSocket()
        {
            lock (_asocketMap)
            {
                foreach (var e in _asocketMap)
                {
                    return e.Value;
                }
                throw new Exception("no socket found.");
            }
        }

        /// <summary>
        /// ASocket 关闭的时候总是回调。
        /// </summary>
        /// <param name="so"></param>
        /// <param name="e"></param>
        public virtual void OnSocketClose(AsyncSocket so, Exception e)
        {
            lock (_asocketMap)
            {
                _asocketMap.Remove(so.SerialNo);
            }
            if (null != e)
                logger.Debug(e, "OnSocketClose");
        }

        /// <summary>
        /// 服务器接受到新连接回调。
        /// </summary>
        /// <param name="so"></param>
        public virtual void OnSocketAccept(AsyncSocket so)
        {
            lock(_asocketMap)
            {
                _asocketMap.Add(so.SerialNo, so);
            }
        }

        /// <summary>
        /// 连接失败回调。同时也会回调OnSocketClose。
        /// </summary>
        /// <param name="so"></param>
        /// <param name="e"></param>
        public virtual void OnSocketConnectError(AsyncSocket so, Exception e)
        {
            logger.Debug(e, "OnSocketConnectError");
        }

        /// <summary>
        /// 连接成功回调。
        /// </summary>
        /// <param name="so"></param>
        public virtual void OnSocketConnected(AsyncSocket so)
        {
            lock (_asocketMap)
            {
                _asocketMap.Add(so.SerialNo, so);
            }
            Console.WriteLine("OnSocketConnected: " + so.SerialNo);
            string head = "HEAD http://www.163.com/\r\nHost: www.163.com\r\nAccept:*/*\r\n\r\n";
            so.Send(head);
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

        public virtual void DispatchProtocol(Protocol p)
        {
            Task.Run(p.Run);
        }

        public virtual void DispatchUnknownProtocol(AsyncSocket so, int type, ByteBuffer data)
        {
            throw new Exception("Unknown Protocol (" + (type >> 16 & 0xffff) + ", " + (type & 0xffff) + ") size=" + data.Size);
        }

        ////////////////////////////////////////////////////////////////////////////////////////////////
        /// 协议工厂
        protected Dictionary<int, Func<Protocol>> Factorys { get; } = new Dictionary<int, Func<Protocol>>();

        public Protocol CreateProtocol(int type, ByteBuffer bb)
        {
            Func<Protocol> factory;
            if (false == Factorys.TryGetValue(type, out factory))
            {
                return null;
            }

            Protocol p = factory();
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
                Protocol p;
                contexts.Remove(sid, out p);
                return (T)p;
            }
        }
    }
}
