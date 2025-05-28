

using Org.BouncyCastle.Pqc.Crypto.Lms;
using System;
using System.Collections.Generic;
using System.Net;
using System.Text;
using Zeze.Serialize;
using Zeze.Util;

namespace Zeze.Net
{
    public abstract class AsyncSocket : IDisposable
    {
        private static readonly ILogger logger = LogManager.GetLogger(typeof(AsyncSocket));

        protected int _closedState;

        public readonly Service Service;

        public long SessionId { get; private set; }

#if !USE_CONFCS
        public TimeThrottle TimeThrottle { get; protected set; }
#endif

        /// <summary>
        /// ������Ҫ�洢��Socket�е�״̬��
        /// �򵥱�����û�п����̰߳�ȫ���⡣
        /// �ڲ���ʹ�á�
        /// </summary>
        public volatile object UserState;

        // ReSharper disable once NotAccessedField.Global
        public bool IsHandshakeDone;

        private static readonly AtomicLong SessionIdGen = new AtomicLong();
        public static Func<long> SessionIdGenFunc { get; set; }

        public Exception LastException { get; protected set; }
        public IPAddress RemoteAddress { get; protected set; }
        public IPAddress LocalAddress { get; protected set; }
        public AsyncSocketType Type { get; protected set; }
        public long ActiveRecvTime { get; private set; } // �ϴν��յ�ʱ���(����)
        public long ActiveSendTime { get; private set; } // �ϴη��͵�ʱ���(����)

        public Connector Connector { get; protected set; }

        protected AsyncSocket(Service service)
        {
            this.Service = service;
            SessionId = NextSessionId();
        }

        private static long NextSessionId()
        {
            return SessionIdGenFunc?.Invoke() ?? SessionIdGen.IncrementAndGet();
        }

        public void SetSessionId(long newSessionId)
        {
            if (Service.SocketMapInternal.TryRemove(new KeyValuePair<long, AsyncSocket>(SessionId, this)))
            {
                if (!Service.SocketMapInternal.TryAdd(newSessionId, this))
                {
                    Service.SocketMapInternal.TryAdd(SessionId, this); // rollback
                    throw new Exception($"duplicate sessionId {this}");
                }
                SessionId = newSessionId;
            }
            else
            {
                // Ϊ�˼򻯲������⣬ֻ�ܴ������Service�Ժ��Socket��SessionId��
                throw new Exception($"Not Exist In Service {this}");
            }
        }

        public void SetActiveRecvTime()
        {
            ActiveRecvTime = Time.NowUnixMillis;
        }

        public void SetActiveSendTime()
        {
            ActiveSendTime = Time.NowUnixMillis;
        }

        public bool Send(Protocol protocol)
        {
            return Send(protocol.Encode());
        }

        public bool Send(ByteBuffer bb)
        {
            return Send(bb.Bytes, bb.ReadIndex, bb.Size);
        }

        public bool Send(Binary binary)
        {
            return Send(binary.Bytes, binary.Offset, binary.Count);
        }

        public bool Send(byte[] bytes)
        {
            return Send(bytes, 0, bytes.Length);
        }

        public bool Send(string str)
        {
            return Send(Encoding.UTF8.GetBytes(str));
        }

        /// <summary>
        /// ����ֱ�Ӽӵ����ͻ��������������޸�bytes�ˡ�
        /// </summary>
        /// <param name="bytes"></param>
        /// <param name="offset"></param>
        /// <param name="length"></param>
        public abstract bool Send(byte[] bytes, int offset, int length);

        protected int ClosedState(int state)
        {
            lock (this)
            {
                if (_closedState != 0)
                    return _closedState;
                _closedState = state;
                return 0;
            }
        }

        public virtual void Close(Exception e)
        {
            LastException = e;
            if (e != null)
            {
                logger.Log(Service.SocketOptions.SocketLogLevel, e, "Close");
            }
            Dispose();
        }

        public abstract void Dispose();

        public abstract void CloseGracefully();

        public bool Closed => _closedState != 0;
    }
}