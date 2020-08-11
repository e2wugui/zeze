using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Net.Sockets;
using System.Diagnostics;

namespace Zeze.Net
{
    /// <summary>
    /// 使用Socket的BeginXXX,EndXXX方法的异步包装类。
    /// 目前只支持Tcp。
    /// </summary>
    public class AsyncSocket : IDisposable
    {
        private Zeze.Serialize.ByteBuffer _inputBuffer;
        private List<System.ArraySegment<byte>> _outputBufferList = null;
        private int _outputBufferListCountSum = 0;
        private List<System.ArraySegment<byte>> _outputBufferListSending = null; // 正在发送的 buffers.
        private int _outputBufferListSendingCountSum = 0;

        public Manager Manager { get; private set; }
        public Exception LastException { get; private set; }
        public long SerialNo { get; private set; }
        public Socket Socket { get; private set; } // 这个给出去真的好吗？

        /// <summary>
        /// 保存需要存储在Socket中的状态，比如加解密的功能。
        /// 简单变量，没有考虑线程安全问题。
        /// 内部不使用。
        /// </summary>
        public Object UserState { get; set; } // 

        private static Zeze.Util.AtomicLong SerialNoGen = new Zeze.Util.AtomicLong();

        private SocketAsyncEventArgs eventArgsAccept;
        private SocketAsyncEventArgs eventArgsReceive;
        private SocketAsyncEventArgs eventArgsSend;

        /// <summary>
        /// for server socket
        /// </summary>
        public AsyncSocket(Manager manager, System.Net.EndPoint localEP)
        {
            this.Manager = manager;

            Socket = new Socket(SocketType.Stream, ProtocolType.Tcp);
            Socket.Blocking = false;
            Socket.SetSocketOption(SocketOptionLevel.Socket, SocketOptionName.ReuseAddress, true);

            // xxx 只能设置到 ServerSocket 中，以后 Accept 的连接通过继承机制得到这个配置。
            // 不知道 c# 会不会也这样，先这样写。
            if (null != manager.SocketOptions.ReceiveBuffer)
                Socket.ReceiveBufferSize = manager.SocketOptions.ReceiveBuffer.Value;

            Socket.Bind(localEP);
            Socket.Listen(manager.SocketOptions.Backlog);

            this.SerialNo = SerialNoGen.IncrementAndGet();

            eventArgsAccept = new SocketAsyncEventArgs();
            eventArgsAccept.Completed += OnAsyncIOCompleted;

            BeginAcceptAsync();
        }

        /// <summary>
        /// use inner. create when accept;
        /// </summary>
        /// <param name="accepted"></param>
        AsyncSocket(Manager manager, Socket accepted)
        {
            this.Manager = manager;

            Socket = accepted;
            Socket.Blocking = false;

            // 据说连接接受以后设置无效，应该从 ServerSocket 继承
            if (null != manager.SocketOptions.ReceiveBuffer)
                Socket.ReceiveBufferSize = manager.SocketOptions.ReceiveBuffer.Value;
            if (null != manager.SocketOptions.SendBuffer)
                Socket.SendBufferSize = manager.SocketOptions.SendBuffer.Value;
            if (null != manager.SocketOptions.NoDelay)
                Socket.NoDelay = manager.SocketOptions.NoDelay.Value;

            BeginReceiveAsync();

            this.SerialNo = SerialNoGen.IncrementAndGet();
        }

        /// <summary>
        /// for client socket. connect
        /// </summary>
        /// <param name="host"></param>
        /// <param name="port"></param>
        public AsyncSocket(Manager manager, string host, int port)
        {
            this.Manager = manager;

            Socket = new Socket(SocketType.Stream, ProtocolType.Tcp);
            Socket.Blocking = false;

            if (null != manager.SocketOptions.ReceiveBuffer)
                Socket.ReceiveBufferSize = manager.SocketOptions.ReceiveBuffer.Value;
            if (null != manager.SocketOptions.SendBuffer)
                Socket.SendBufferSize = manager.SocketOptions.SendBuffer.Value;
            if (null != manager.SocketOptions.NoDelay)
                Socket.NoDelay = manager.SocketOptions.NoDelay.Value;

            System.Net.Dns.BeginGetHostAddresses(host, OnAsyncGetHostAddresses, port);

            this.SerialNo = SerialNoGen.IncrementAndGet();
        }

        public void Send(Zeze.Serialize.ByteBuffer bb)
        {
            Send(bb.Bytes, bb.ReadIndex, bb.Size);
        }

        public void Send(byte[] bytes)
        {
            Send(bytes, 0, bytes.Length);
        }

        /// <summary>
        /// 加到发送缓冲区，直接引用bytes数组。不能再修改了。
        /// </summary>
        /// <param name="bytes"></param>
        /// <param name="offset"></param>
        /// <param name="length"></param>
        public void Send(byte[] bytes, int offset, int length)
        {
            Zeze.Serialize.Helper.VerifyParameter(bytes, offset, length);
            if (length <= 0)
                return; // 不允许发送空缓存

            lock (this)
            {
                if (null == _outputBufferList)
                    _outputBufferList = new List<ArraySegment<byte>>();
                _outputBufferList.Add(new ArraySegment<byte>(bytes, offset, length));
                _outputBufferListCountSum += _outputBufferList[^1].Count;
            }
            BeginSendAsync(0); // must be zero.
        }

        public void Send(string str)
        {
            Send(Encoding.UTF8.GetBytes(str));
        }

        private void OnAsyncIOCompleted(object sender, SocketAsyncEventArgs e)
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
                    throw new ArgumentException();
            }
        }

        private void BeginAcceptAsync()
        {
            eventArgsAccept.AcceptSocket = null;
            if (false == Socket.AcceptAsync(eventArgsAccept))
                ProcessAccept(eventArgsAccept);
        }

        private void ProcessAccept(SocketAsyncEventArgs e)
        {
            AsyncSocket accepted = null;
            try
            {
                accepted = new AsyncSocket(this.Manager, e.AcceptSocket);
                this.Manager.OnSocketAccept(accepted);
            }
            catch (Exception ce)
            {
                accepted?.Close(ce);
            }
            BeginAcceptAsync();
        }

        private void OnAsyncGetHostAddresses(IAsyncResult ar)
        {
            try
            {
                int port = (System.Int32)ar.AsyncState;
                System.Net.IPAddress[] addrs = System.Net.Dns.EndGetHostAddresses(ar);
                Socket.BeginConnect(addrs, port, OnAsyncConnect, this);
            }
            catch (Exception e)
            {
                this.Manager.OnSocketConnectError(this, e);
                Close(e);
            }
        }

        private void OnAsyncConnect(IAsyncResult ar)
        {
            try
            {
                this.Socket.EndConnect(ar);
                this.Manager.OnSocketConnected(this);
                BeginReceiveAsync();
            }
            catch (Exception e)
            {
                this.Manager.OnSocketConnectError(this, e);
                Close(e);
            }
        }

        private void BeginReceiveAsync()
        {
            if (null == eventArgsReceive)
            {
                eventArgsReceive = new SocketAsyncEventArgs();
                eventArgsReceive.Completed += OnAsyncIOCompleted;
            }

            if (null == _inputBuffer)
            {
                _inputBuffer = Serialize.ByteBuffer.Allocate(Manager.SocketOptions.InputBufferInitCapacity);
            }
            else
            {
                if (_inputBuffer.Size == 0 && _inputBuffer.Capacity > Manager.SocketOptions.InputBufferResetThreshold)
                    _inputBuffer = Serialize.ByteBuffer.Allocate(Manager.SocketOptions.InputBufferInitCapacity);
                _inputBuffer.Campact(); // 上次接收还有剩余数据.
            }

            if (_inputBuffer.Capacity - _inputBuffer.WriteIndex < Manager.SocketOptions.InputBufferInitCapacity)
                _inputBuffer.EnsureWrite(Manager.SocketOptions.InputBufferInitCapacity);

            byte[] buffer = _inputBuffer.Bytes;
            int offset = _inputBuffer.WriteIndex;
            int size = _inputBuffer.Capacity - _inputBuffer.WriteIndex;
            eventArgsReceive.SetBuffer(buffer, offset, size);

            if (false == this.Socket.ReceiveAsync(eventArgsReceive))
                ProcessReceive(eventArgsReceive);
        }

        private void ProcessReceive(SocketAsyncEventArgs e)
        {
            if (e.BytesTransferred > 0 && e.SocketError == SocketError.Success)
            {
                _inputBuffer.WriteIndex += e.BytesTransferred;
                if (_inputBuffer.WriteIndex > _inputBuffer.Capacity) // 这个应该不会发生。
                {
                    Close(new Exception("input buffer overflow."));
                    return;
                }
                this.Manager.OnSocketProcessInputBuffer(this, _inputBuffer);
                BeginReceiveAsync();
            }
            else
            {
                Close(null); // 正常关闭，不设置异常
            }
        }

        private void ProcessSend(SocketAsyncEventArgs e)
        {
            if (e.BytesTransferred > 0 && e.SocketError == SocketError.Success)
            {
                BeginSendAsync(e.BytesTransferred); // 必须大于0
            }
            else
            {
                Close(new SocketException((int)e.SocketError)) ;
            }
        }

        private void BeginSendAsync(int hasSend)
        {
            lock(this)
            {
                if (null == eventArgsSend)
                {
                    eventArgsSend = new SocketAsyncEventArgs();
                    eventArgsSend.Completed += OnAsyncIOCompleted;
                }

                if (hasSend > 0)
                {
                    // 听说 BeginSend 成功回调的时候，所有数据都会被发送，这样的话就可以直接清除_outputBufferSending，而不用这么麻烦。
                    // MUST 下面的条件必须满足，不做判断。
                    // _outputBufferSending != null
                    // _outputBufferSending.Count > 0
                    // sum(_outputBufferSending[i].Count) <= hasSend
                    if (hasSend == _outputBufferListSendingCountSum) // 全部发送完
                    {
                        _outputBufferListSending.Clear();
                    }
                    else if (hasSend > _outputBufferListSendingCountSum)
                    {
                        throw new Exception("hasSend too big.");
                    }
                    else
                    {
                        for (int i = 0; i < _outputBufferListSending.Count; ++i)
                        {
                            int bytesCount = _outputBufferListSending[i].Count;
                            if (hasSend >= bytesCount)
                            {
                                hasSend -= bytesCount;
                                if (hasSend > 0)
                                    continue;

                                _outputBufferListSending.RemoveRange(0, i + 1);
                                break;
                            }
                            // 已经发送的数据比数组中的少。
                            ArraySegment<byte> segment = _outputBufferListSending[i];
                            _outputBufferListSending[i] = segment.Slice(hasSend, segment.Count - hasSend);
                            _outputBufferListSending.RemoveRange(0, i);
                            break;
                        }
                    }

                    if (_outputBufferListSending.Count == 0)
                    {
                        _outputBufferListSending = _outputBufferList;
                        _outputBufferList = null;
                        _outputBufferListSendingCountSum = _outputBufferListCountSum;
                        _outputBufferListCountSum = 0;
                    }
                    else if (null != _outputBufferList)
                    {
                        _outputBufferListSending.AddRange(_outputBufferList);
                        _outputBufferList = null;
                        _outputBufferListSendingCountSum = _outputBufferListCountSum;
                        _outputBufferListCountSum = 0;
                    }

                    if (null != _outputBufferListSending)
                    {
                        eventArgsSend.BufferList = _outputBufferListSending;
                        if (false == Socket.SendAsync(eventArgsSend))
                            ProcessSend(eventArgsSend);
                    }
                    return;
                }
                
                if (null == _outputBufferListSending && null != _outputBufferList)
                {
                    _outputBufferListSending = _outputBufferList;
                    _outputBufferList = null;
                    _outputBufferListSendingCountSum = _outputBufferListCountSum;
                    _outputBufferListCountSum = 0;

                    eventArgsSend.BufferList = _outputBufferListSending;
                    if (false == Socket.SendAsync(eventArgsSend))
                        ProcessSend(eventArgsSend);
                }
            }
        }

        private void Close(Exception e)
        {
            this.LastException = e;
            Dispose();
        }

        public void Dispose()
        {
            lock(this)
            {
                try
                {
                    Socket?.Dispose();
                    Socket = null;
                    Manager?.OnSocketClose(this, this.LastException);
                    Manager = null;
                }
                catch (Exception)
                {
                    // skip Dispose error
                }
            }
        }
    }
}
