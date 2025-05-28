
using System;
using System.Collections.Generic;
using System.Text;
using System.Net.Sockets;
using Zeze.Serialize;
using System.Net;
using Zeze.Util;

namespace Zeze.Net
{
    /// <summary>
    /// 使用Socket的BeginXXX,EndXXX XXXAsync方法的异步包装类。
    /// 目前只支持Tcp。
    /// </summary>
    public class TcpSocket : AsyncSocket
    {
        private static readonly ILogger logger = LogManager.GetLogger(typeof(TcpSocket));

        private byte[] _inputBuffer;
        private List<ArraySegment<byte>> _outputBufferList;
        private int _outputBufferListCountSum;
        private List<ArraySegment<byte>> _outputBufferListSending; // 正在发送的 buffers.
        private int _outputBufferListSendingCountSum;

        public readonly Acceptor Acceptor;

        public Socket Socket { get; private set; } // 这个给出去真的好吗？

        private readonly SocketAsyncEventArgs eventArgsAccept;
        private SocketAsyncEventArgs eventArgsReceive;
        private SocketAsyncEventArgs eventArgsSend;

        private readonly BufferCodec inputCodecBuffer = new BufferCodec(); // 记录这个变量用来操作buffer
        private readonly BufferCodec outputCodecBuffer = new BufferCodec(); // 记录这个变量用来操作buffer

        private Codec inputCodecChain;
        private Codec outputCodecChain;

        /// <summary>
        /// for server socket
        /// </summary>
        public TcpSocket(Service service, EndPoint localEP, Acceptor acceptor)
            : base(service)
        {
            Acceptor = acceptor;
            Type = AsyncSocketType.eServerSocket;
            service.TryStartKeepAliveCheckTimer();

            Socket = new Socket(SocketType.Stream, ProtocolType.Tcp)
            {
                Blocking = false
            };
            Socket.SetSocketOption(SocketOptionLevel.Socket, SocketOptionName.ReuseAddress, true);

            // xxx 只能设置到 ServerSocket 中，以后 Accept 的连接通过继承机制得到这个配置。
            // 不知道 c# 会不会也这样，先这样写。
            if (service.SocketOptions.ReceiveBuffer != null)
                Socket.ReceiveBufferSize = service.SocketOptions.ReceiveBuffer.Value;

            Socket.Bind(localEP);
            Socket.Listen(service.SocketOptions.Backlog);

#if !USE_CONFCS
            TimeThrottle = null;
#endif
            eventArgsAccept = new SocketAsyncEventArgs();
            eventArgsAccept.Completed += OnAsyncIOCompleted;

            BeginAcceptAsync();
        }

        /// <summary>
        /// use inner. create when accept;
        /// </summary>
        TcpSocket(Service service, Socket accepted, Acceptor acceptor)
            : base(service)
        {
            Acceptor = acceptor;
            Type = AsyncSocketType.eServer;

            Socket = accepted;
            Socket.Blocking = false;

            // 据说连接接受以后设置无效，应该从 ServerSocket 继承
            if (service.SocketOptions.ReceiveBuffer != null)
                Socket.ReceiveBufferSize = service.SocketOptions.ReceiveBuffer.Value;
            if (service.SocketOptions.SendBuffer != null)
                Socket.SendBufferSize = service.SocketOptions.SendBuffer.Value;
            if (service.SocketOptions.NoDelay != null)
                Socket.NoDelay = service.SocketOptions.NoDelay.Value;

#if !USE_CONFCS
            TimeThrottle = TimeThrottle.Create(service.SocketOptions);
#endif
            _inputBuffer = new byte[service.SocketOptions.InputBufferSize];

            LocalAddress = ((IPEndPoint)Socket.LocalEndPoint).Address;
            RemoteAddress = ((IPEndPoint)Socket.RemoteEndPoint).Address;

            BeginReceiveAsync();
        }

        /// <summary>
        /// for client socket. connect
        /// </summary>
        public TcpSocket(Service service, string hostNameOrAddress, int port, object userState = null, Connector connector = null)
            : base(service)
        {
            Connector = connector;
            Type = AsyncSocketType.eClient;
            service.TryStartKeepAliveCheckTimer();

            Socket = new Socket(SocketType.Stream, ProtocolType.Tcp)
            {
                Blocking = false
            };
            UserState = userState;

            if (service.SocketOptions.ReceiveBuffer != null)
                Socket.ReceiveBufferSize = service.SocketOptions.ReceiveBuffer.Value;
            if (service.SocketOptions.SendBuffer != null)
                Socket.SendBufferSize = service.SocketOptions.SendBuffer.Value;
            if (service.SocketOptions.NoDelay != null)
                Socket.NoDelay = service.SocketOptions.NoDelay.Value;

#if !USE_CONFCS
            TimeThrottle = TimeThrottle.Create(service.SocketOptions);
#endif
            Dns.BeginGetHostAddresses(hostNameOrAddress, OnAsyncGetHostAddresses, port);
        }

        protected TcpSocket(Service service, object userState = null)
            : base(service)
        {
            UserState = userState;
        }

        public void SetOutputSecurityCodec(byte[] key, int compress)
        {
            lock (this)
            {
                Codec chain = outputCodecBuffer;
                if (key != null)
                    chain = new Encrypt(chain, key);
                if (compress != 0)
                    chain = new Compress(chain);
                outputCodecChain?.Dispose();
                outputCodecChain = chain;
                IsOutputSecurity = true;
            }
        }

        public bool IsInputSecurity { get; private set; }
        public bool IsOutputSecurity { get; private set; }
        public bool IsSecurity => IsInputSecurity && IsOutputSecurity;

        public void VerifySecurity()
        {
            if (Service.Config.HandshakeOptions.EncryptType != 0 && !IsSecurity)
                throw new Exception($"{Service.Name} !IsSecurity");
        }

        public void SetInputSecurityCodec(byte[] key, int compress)
        {
            lock (this)
            {
                Codec chain = inputCodecBuffer;
                if (compress != 0)
                    chain = new Decompress(chain);
                if (key != null)
                    chain = new Decrypt(chain, key);
                inputCodecChain?.Dispose();
                inputCodecChain = chain;
                IsInputSecurity = true;
            }
        }

        public override bool Send(byte[] bytes, int offset, int length)
        {
            ByteBuffer.VerifyArrayIndex(bytes, offset, length);

            try
            {
                lock (this)
                {
                    if (_closedState != 0)
                        return false;
                    if (outputCodecChain != null)
                    {
                        // 压缩加密等 codec 链操作。
                        outputCodecBuffer.Buffer.EnsureWrite(length); // reserve
                        outputCodecChain.update(bytes, offset, length);
                        outputCodecChain.flush();

                        // 修改参数，后面继续使用处理过的数据继续发送。
                        bytes = outputCodecBuffer.Buffer.Bytes;
                        offset = outputCodecBuffer.Buffer.ReadIndex;
                        length = outputCodecBuffer.Buffer.Size;

                        // outputBufferCodec 释放对byte[]的引用。
                        outputCodecBuffer.Buffer.FreeInternalBuffer();
                    }

                    if (_outputBufferList == null)
                        _outputBufferList = new List<ArraySegment<byte>>();
                    _outputBufferList.Add(new ArraySegment<byte>(bytes, offset, length));
                    _outputBufferListCountSum += length;

                    if (_outputBufferListSending == null) // 没有在发送中，马上请求发送，否则等回调处理。
                    {
                        _outputBufferListSending = _outputBufferList;
                        _outputBufferList = null;
                        _outputBufferListSendingCountSum = _outputBufferListCountSum;
                        _outputBufferListCountSum = 0;

                        if (eventArgsSend == null)
                        {
                            eventArgsSend = new SocketAsyncEventArgs();
                            eventArgsSend.Completed += OnAsyncIOCompleted;
                        }
                        eventArgsSend.BufferList = _outputBufferListSending;
                        if (!Socket.SendAsync(eventArgsSend))
                            ProcessSend(eventArgsSend);
                    }
                    SetActiveSendTime();
                    return true;
                }
            }
            catch (Exception ex)
            {
                Close(ex);
                return false;
            }
        }

        private void OnAsyncIOCompleted(object sender, SocketAsyncEventArgs e)
        {
            if (Socket == null) // async closed
                return;

            try
            {
                switch (e.LastOperation)
                {
                    case SocketAsyncOperation.Accept:
                        ProcessAccept(e);
                        break;
                    case SocketAsyncOperation.Send:
                        ProcessSend(e);
                        break;
                    case SocketAsyncOperation.Receive:
                        ProcessReceive(e);
                        break;
                    default:
                        throw new ArgumentException($"Invalid LastOperation={e.LastOperation}");
                }
            }
            catch (Exception ex)
            {
                Close(ex);
            }
        }

        private void BeginAcceptAsync()
        {
            eventArgsAccept.AcceptSocket = null;
            if (!Socket.AcceptAsync(eventArgsAccept))
                ProcessAccept(eventArgsAccept);
        }

        private void ProcessAccept(SocketAsyncEventArgs e)
        {
            if (e.SocketError == SocketError.Success)
            {
                AsyncSocket accepted = null;
                try
                {
                    accepted = new TcpSocket(Service, e.AcceptSocket, Acceptor);
                    Service.OnSocketAccept(accepted);
                }
                catch (Exception ce)
                {
                    accepted?.Close(ce);
                    try
                    {
                        Service.OnSocketAcceptError(this, ce);
                    }
                    catch (Exception ex)
                    {
                        logger.Error(ex);
                    }
                }
                BeginAcceptAsync();
            }
            /*
            else
                Console.Error.WriteLine("ProcessAccept " + e.SocketError);
            */
        }

        private void OnAsyncGetHostAddresses(IAsyncResult ar)
        {
            if (Socket == null)
                return; // async close?

            try
            {
                int port = (int)ar.AsyncState;
                IPAddress[] addrs = Dns.EndGetHostAddresses(ar);
                Socket.BeginConnect(addrs, port, OnAsyncConnect, this);
            }
            catch (Exception e)
            {
                try
                {
                    Service.OnSocketConnectError(this, e);
                }
                catch (Exception ex)
                {
                    logger.Error(ex);
                }
                Close(null);
            }
        }

        private void OnAsyncConnect(IAsyncResult ar)
        {
            if (Socket == null)
                return; // async close?

            try
            {
                Socket.EndConnect(ar);
                Connector?.OnSocketConnected(this);
                LocalAddress = ((IPEndPoint)Socket.LocalEndPoint).Address;
                RemoteAddress = ((IPEndPoint)Socket.RemoteEndPoint).Address;
                Service.OnSocketConnected(this);
                _inputBuffer = new byte[Service.SocketOptions.InputBufferSize];
                BeginReceiveAsync();
            }
            catch (Exception e)
            {
                try
                {
                    Service.OnSocketConnectError(this, e);
                }
                catch (Exception ex)
                {
                    logger.Error(ex);
                }
                Close(null);
            }
        }

        private void BeginReceiveAsync()
        {
            if (eventArgsReceive == null)
            {
                eventArgsReceive = new SocketAsyncEventArgs();
                eventArgsReceive.Completed += OnAsyncIOCompleted;
            }

            eventArgsReceive.SetBuffer(_inputBuffer, 0, _inputBuffer.Length);
            if (!Socket.ReceiveAsync(eventArgsReceive))
                ProcessReceive(eventArgsReceive);
        }

        private void ProcessReceive(SocketAsyncEventArgs e)
        {
            if (e.BytesTransferred > 0 && e.SocketError == SocketError.Success)
            {
                SetActiveRecvTime();
                if (inputCodecChain != null)
                {
                    // 解密解压处理，处理结果直接加入 inputCodecBuffer。
                    inputCodecBuffer.Buffer.EnsureWrite(e.BytesTransferred);
                    inputCodecChain.update(_inputBuffer, 0, e.BytesTransferred);
                    inputCodecChain.flush();

                    Service.OnSocketProcessInputBuffer(this, inputCodecBuffer.Buffer);
                }
                else if (inputCodecBuffer.Buffer.Size > 0)
                {
                    // 上次解析有剩余数据（不完整的协议），把新数据加入。
                    inputCodecBuffer.Buffer.Append(_inputBuffer, 0, e.BytesTransferred);

                    Service.OnSocketProcessInputBuffer(this, inputCodecBuffer.Buffer);
                }
                else
                {
                    var avoidCopy = ByteBuffer.Wrap(_inputBuffer, 0, e.BytesTransferred);

                    Service.OnSocketProcessInputBuffer(this, avoidCopy);

                    if (avoidCopy.Size > 0) // 有剩余数据（不完整的协议），加入 inputCodecBuffer 等待新的数据。
                        inputCodecBuffer.Buffer.Append(avoidCopy.Bytes, avoidCopy.ReadIndex, avoidCopy.Size);
                }

                // 1 检测 buffer 是否满，2 剩余数据 Campact，3 需要的话，释放buffer内存。
                int remain = inputCodecBuffer.Buffer.Size;
                if (remain > 0)
                {
                    if (remain >= Service.SocketOptions.InputBufferMaxProtocolSize)
                        throw new Exception("InputBufferMaxProtocolSize " + Service.SocketOptions.InputBufferMaxProtocolSize);

                    inputCodecBuffer.Buffer.Campact();
                }
                else
                {
                    inputCodecBuffer.Buffer.FreeInternalBuffer(); // 解析缓冲如果为空，马上释放内部bytes[]。
                }

                BeginReceiveAsync();
            }
            else
            {
                Close(null); // 正常关闭，不设置异常
            }
        }

        private void ProcessSend(SocketAsyncEventArgs e)
        {
            if (e.BytesTransferred >= 0 && e.SocketError == SocketError.Success)
            {
                BeginSendAsync(e.BytesTransferred);
            }
            else
            {
                Close(new SocketException((int)e.SocketError));
            }
        }

        private void BeginSendAsync(int _bytesTransferred)
        {
            var realClose = false;
            lock (this)
            {
                // 听说 BeginSend 成功回调的时候，所有数据都会被发送，这样的话就可以直接清除_outputBufferSending，而不用这么麻烦。
                // MUST 下面的条件必须满足，不做判断。
                // _outputBufferSending != null
                // _outputBufferSending.Count > 0
                // sum(_outputBufferSending[i].Count) <= bytesTransferred
                int bytesTransferred = _bytesTransferred; // 后面还要用已经发送的原始值，本来下面计算也可以得到，但这样更容易理解。
                if (bytesTransferred == _outputBufferListSendingCountSum) // 全部发送完，优化。
                {
                    _outputBufferListSending.Clear();
                }
                else if (bytesTransferred > _outputBufferListSendingCountSum)
                {
                    throw new Exception("hasSend too big.");
                }
                else
                {
                    // 部分发送
                    for (int i = 0; i < _outputBufferListSending.Count; ++i)
                    {
                        int bytesCount = _outputBufferListSending[i].Count;
                        if (bytesTransferred >= bytesCount)
                        {
                            bytesTransferred -= bytesCount;
                            if (bytesTransferred > 0)
                                continue;

                            _outputBufferListSending.RemoveRange(0, i + 1);
                            break;
                        }
                        // 已经发送的数据比数组中的少。
                        ArraySegment<byte> segment = _outputBufferListSending[i];
                        // Slice .net framework 没有定义。
                        // ReSharper disable once AssignNullToNotNullAttribute
                        _outputBufferListSending[i] = new ArraySegment<byte>(
                            segment.Array, bytesTransferred + segment.Offset, segment.Count - bytesTransferred);
                        _outputBufferListSending.RemoveRange(0, i);
                        break;
                    }
                }

                if (_outputBufferListSending.Count == 0)
                {
                    // 全部发送完
                    _outputBufferListSending = _outputBufferList; // maybe null
                    _outputBufferList = null;
                    _outputBufferListSendingCountSum = _outputBufferListCountSum;
                    _outputBufferListCountSum = 0;
                }
                else if (_outputBufferList != null)
                {
                    // 没有发送完，并且有要发送的
                    _outputBufferListSending.AddRange(_outputBufferList);
                    _outputBufferList = null;
                    _outputBufferListSendingCountSum = _outputBufferListCountSum
                                                       + (_outputBufferListSendingCountSum - _bytesTransferred);
                    _outputBufferListCountSum = 0;
                }
                else
                {
                    // 没有发送完，也没有要发送的
                    _outputBufferListSendingCountSum -= _bytesTransferred;
                }

                if (_outputBufferListSending != null) // 全部发送完，并且 _outputBufferList == null 时，可能为 null
                {
                    eventArgsSend.BufferList = _outputBufferListSending;
                    if (!Socket.SendAsync(eventArgsSend))
                        ProcessSend(eventArgsSend);
                }
                else
                {
                    // 输出buffer全部清空，并且处于关闭状态，锁外执行真正的Close。
                    realClose = _closedState == int.MaxValue;
                }
            }
            if (realClose)
                RealClose();
        }

        public override void CloseGracefully()
        {
            if (0 != ClosedState(int.MaxValue))
                return;

            Trigger();

            bool realClose;
            lock (this)
            {
                // 如果当前输出buffer已经是空的，马上关闭。否则等到刷新完成关闭（see BeginSendAsync）。
                realClose = _outputBufferListSending == null;
            }
            if (realClose)
                RealClose();
            else
                Scheduler.Schedule(_ => RealClose(), 120 * 1000);
        }

        private void Trigger()
        {
            // 在锁外回调。
            try
            {
                Connector?.OnSocketClose(this, LastException);
            }
            catch (Exception e)
            {
                logger.Error(e);
            }
            try
            {
                Service.OnSocketClose(this, LastException);
            }
            catch (Exception e)
            {
                logger.Error(e);
            }
        }

        private void RealClose()
        {
            try
            {
                Socket?.Dispose();
                Socket = null;
            }
            catch (Exception e)
            {
                logger.Error(e);
            }

            try
            {
                Service.OnSocketDisposed(this);
            }
            catch (Exception e)
            {
                logger.Error(e);
            }
#if !USE_CONFCS
            TimeThrottle?.Close();
#endif
        }

        public override void Dispose()
        {
            if (0 != ClosedState(1))
                return;

            Trigger();
            RealClose();
        }

        public override string ToString()
        {
            return $"({Socket?.LocalEndPoint}-{Socket?.RemoteEndPoint})";
        }
    }
}
