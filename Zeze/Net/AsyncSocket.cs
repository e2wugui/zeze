﻿using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Net.Sockets;
using System.Diagnostics;
using Zeze.Serialize;
using System.Net;

namespace Zeze.Net
{
    /// <summary>
    /// 使用Socket的BeginXXX,EndXXX XXXAsync方法的异步包装类。
    /// 目前只支持Tcp。
    /// </summary>
    public sealed class AsyncSocket : IDisposable
    {
        private byte[] _inputBuffer;
        private List<System.ArraySegment<byte>> _outputBufferList = null;
        private int _outputBufferListCountSum = 0;
        private List<System.ArraySegment<byte>> _outputBufferListSending = null; // 正在发送的 buffers.
        private int _outputBufferListSendingCountSum = 0;
        public Service Service { get; private set; }
        public Connector Connector { get; set; }
        public Acceptor Acceptor { get; set; }

        public Exception LastException { get; private set; }
        public long SessionId { get; private set; }
        public Socket Socket { get; private set; } // 这个给出去真的好吗？

        /// <summary>
        /// 保存需要存储在Socket中的状态。
        /// 简单变量，没有考虑线程安全问题。
        /// 内部不使用。
        /// </summary>
        public Object UserState { get; set; } 
        public bool IsHandshakeDone { get; set; }

        private static global::Zeze.Util.AtomicLong SessionIdGen = new global::Zeze.Util.AtomicLong();

        private SocketAsyncEventArgs eventArgsAccept;
        private SocketAsyncEventArgs eventArgsReceive;
        private SocketAsyncEventArgs eventArgsSend;

        private BufferCodec inputCodecBuffer = new BufferCodec(); // 记录这个变量用来操作buffer
        private BufferCodec outputCodecBuffer = new BufferCodec(); // 记录这个变量用来操作buffer

        private Codec inputCodecChain;
        private Codec outputCodecChain;

        public string RemoteAddress { get; private set; }

        /// <summary>
        /// for server socket
        /// </summary>
        public AsyncSocket(Service service, System.Net.EndPoint localEP)
        {
            this.Service = service;

            Socket = new Socket(SocketType.Stream, ProtocolType.Tcp);
            Socket.Blocking = false;
            Socket.SetSocketOption(SocketOptionLevel.Socket, SocketOptionName.ReuseAddress, true);

            // xxx 只能设置到 ServerSocket 中，以后 Accept 的连接通过继承机制得到这个配置。
            // 不知道 c# 会不会也这样，先这样写。
            if (null != service.SocketOptions.ReceiveBuffer)
                Socket.ReceiveBufferSize = service.SocketOptions.ReceiveBuffer.Value;

            Socket.Bind(localEP);
            Socket.Listen(service.SocketOptions.Backlog);

            this.SessionId = SessionIdGen.IncrementAndGet();

            eventArgsAccept = new SocketAsyncEventArgs();
            eventArgsAccept.Completed += OnAsyncIOCompleted;

            BeginAcceptAsync();
        }

        /// <summary>
        /// use inner. create when accept;
        /// </summary>
        /// <param name="accepted"></param>
        AsyncSocket(Service service, Socket accepted)
        {
            this.Service = service;

            Socket = accepted;
            Socket.Blocking = false;

            // 据说连接接受以后设置无效，应该从 ServerSocket 继承
            if (null != service.SocketOptions.ReceiveBuffer)
                Socket.ReceiveBufferSize = service.SocketOptions.ReceiveBuffer.Value;
            if (null != service.SocketOptions.SendBuffer)
                Socket.SendBufferSize = service.SocketOptions.SendBuffer.Value;
            if (null != service.SocketOptions.NoDelay)
                Socket.NoDelay = service.SocketOptions.NoDelay.Value;

            this.SessionId = SessionIdGen.IncrementAndGet();

            this._inputBuffer = new byte[service.SocketOptions.InputBufferSize];

            RemoteAddress = (Socket.RemoteEndPoint as IPEndPoint).Address.ToString();

            BeginReceiveAsync();
        }

        /// <summary>
        /// for client socket. connect
        /// </summary>
        /// <param name="hostNameOrAddress"></param>
        /// <param name="port"></param>
        public AsyncSocket(Service service, string hostNameOrAddress, int port, object userState = null)
        {
            this.Service = service;

            Socket = new Socket(SocketType.Stream, ProtocolType.Tcp);
            Socket.Blocking = false;
            UserState = userState;

            if (null != service.SocketOptions.ReceiveBuffer)
                Socket.ReceiveBufferSize = service.SocketOptions.ReceiveBuffer.Value;
            if (null != service.SocketOptions.SendBuffer)
                Socket.SendBufferSize = service.SocketOptions.SendBuffer.Value;
            if (null != service.SocketOptions.NoDelay)
                Socket.NoDelay = service.SocketOptions.NoDelay.Value;

            this.SessionId = SessionIdGen.IncrementAndGet();

            System.Net.Dns.BeginGetHostAddresses(hostNameOrAddress, OnAsyncGetHostAddresses, port);
        }

        public void SetOutputSecurityCodec(byte[] key, bool compress)
        {
            lock (this)
            {
                Codec chain = outputCodecBuffer;
                if (null != key)
                    chain = new Encrypt(chain, key);
                if (compress)
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
            if (!IsSecurity)
                throw new Exception($"{Service.Name} !IsSecurity");
        }

        public void SetInputSecurityCodec(byte[] key, bool compress)
        {
            lock (this)
            {
                Codec chain = inputCodecBuffer;
                if (compress)
                    chain = new Decompress(chain);
                if (null != key)
                    chain = new Decrypt(chain, key);
                inputCodecChain?.Dispose();
                inputCodecChain = chain;
                IsInputSecurity = true;
            }
        }

        public bool Send(Protocol protocol)
        {
            return Send(protocol.Encode());
        }

        public bool Send(global::Zeze.Serialize.ByteBuffer bb)
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

        /// <summary>
        /// 可能直接加到发送缓冲区，不能再修改bytes了。
        /// </summary>
        /// <param name="bytes"></param>
        /// <param name="offset"></param>
        /// <param name="length"></param>
        public bool Send(byte[] bytes, int offset, int length)
        {
            ByteBuffer.VerifyArrayIndex(bytes, offset, length);

            lock (this)
            {
                if (null == Socket)
                    return false;

                if (null == _outputBufferList)
                {
                    _outputBufferList = new List<ArraySegment<byte>>();
                }

                if (_outputBufferListCountSum + length > Service.SocketOptions.OutputBufferMaxSize)
                    return false;

                _outputBufferList.Add(new ArraySegment<byte>(bytes, offset, length));
                _outputBufferListCountSum += length;

                if (null == _outputBufferListSending)
                {
                    // 没有在发送中，马上请求发送，否则等回调处理。
                    doCodecAndPrepareSend();
                    if (null == eventArgsSend)
                    {
                        eventArgsSend = new SocketAsyncEventArgs();
                        eventArgsSend.Completed += OnAsyncIOCompleted;
                    }
                    eventArgsSend.BufferList = _outputBufferListSending;
                    if (false == Socket.SendAsync(eventArgsSend))
                        ProcessSend(eventArgsSend);
                }
                return true;
            }
        }

        public bool Send(string str)
        {
            return Send(Encoding.UTF8.GetBytes(str));
        }

        private void doCodecAndPrepareSend()
        {
            if (null != outputCodecChain)
            {
                if (null != _outputBufferList)
                {
                    if (null == _outputBufferListSending)
                        _outputBufferListSending = new List<ArraySegment<byte>>();

                    foreach (var buffer in _outputBufferList)
                    {
                        // 压缩加密等 codec 链操作。
                        outputCodecBuffer.Buffer.EnsureWrite(buffer.Count); // reserve
                        outputCodecChain.update(buffer.Array, buffer.Offset, buffer.Count);
                        outputCodecChain.flush();

                        var codec = outputCodecBuffer.Buffer;
                        var size = codec.Size;
                        _outputBufferListSending.Add(new ArraySegment<byte>(codec.Bytes, codec.ReadIndex, size));
                        _outputBufferListSendingCountSum += size;
                        // 加入Sending后outputBufferCodec需要释放对byte[]的引用。
                        codec.FreeInternalBuffer();
                    }
                    _outputBufferList = null;
                    _outputBufferListCountSum = 0;
                }
            }
            else if (null != _outputBufferList)
            {
                if (null == _outputBufferListSending)
                    _outputBufferListSending = _outputBufferList;
                else
                    _outputBufferListSending.AddRange(_outputBufferList);
                _outputBufferListSendingCountSum += _outputBufferListCountSum;

                _outputBufferList = null;
                _outputBufferListCountSum = 0;
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
                        throw new ArgumentException();
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
            if (false == Socket.AcceptAsync(eventArgsAccept))
                ProcessAccept(eventArgsAccept);
        }

        private void ProcessAccept(SocketAsyncEventArgs e)
        {
            if (e.SocketError == SocketError.Success)
            {
                AsyncSocket accepted = null;
                try
                {
                    accepted = new AsyncSocket(this.Service, e.AcceptSocket);
                    accepted.Acceptor = this.Acceptor;
                    this.Service.OnSocketAccept(accepted);
                }
                catch (Exception ce)
                {
                    accepted?.Close(ce);
                }
                BeginAcceptAsync();
            }
            /*
            else
            {
                Console.WriteLine("ProcessAccept " + e.SocketError);
            }
            */
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
                this.Service.OnSocketConnectError(this, e);
                Close(null);
            }
        }

        private void OnAsyncConnect(IAsyncResult ar)
        {
            try
            {
                this.Socket.EndConnect(ar);
                this.Connector?.OnSocketConnected(this);
                this.RemoteAddress = (this.Socket.RemoteEndPoint as IPEndPoint).Address.ToString();
                this.Service.OnSocketConnected(this);
                this._inputBuffer = new byte[Service.SocketOptions.InputBufferSize];
                BeginReceiveAsync();
            }
            catch (Exception e)
            {
                this.Service.OnSocketConnectError(this, e);
                Close(null);
            }
        }

        private void BeginReceiveAsync()
        {
            if (null == eventArgsReceive)
            {
                eventArgsReceive = new SocketAsyncEventArgs();
                eventArgsReceive.Completed += OnAsyncIOCompleted;
            }

            eventArgsReceive.SetBuffer(_inputBuffer, 0, _inputBuffer.Length);
            if (false == this.Socket.ReceiveAsync(eventArgsReceive))
                ProcessReceive(eventArgsReceive);
        }

        private void ProcessReceive(SocketAsyncEventArgs e)
        {
            if (e.BytesTransferred > 0 && e.SocketError == SocketError.Success)
            {
                if (null != inputCodecChain)
                {
                    // 解密解压处理，处理结果直接加入 inputCodecBuffer。
                    inputCodecBuffer.Buffer.EnsureWrite(e.BytesTransferred);
                    inputCodecChain.update(_inputBuffer, 0, e.BytesTransferred);
                    inputCodecChain.flush();

                    this.Service.OnSocketProcessInputBuffer(this, inputCodecBuffer.Buffer);
                }
                else if (inputCodecBuffer.Buffer.Size > 0)
                {
                    // 上次解析有剩余数据（不完整的协议），把新数据加入。
                    inputCodecBuffer.Buffer.Append(_inputBuffer, 0, e.BytesTransferred);

                    this.Service.OnSocketProcessInputBuffer(this, inputCodecBuffer.Buffer);
                }
                else
                {
                    ByteBuffer avoidCopy = ByteBuffer.Wrap(_inputBuffer, 0, e.BytesTransferred);

                    this.Service.OnSocketProcessInputBuffer(this, avoidCopy);

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
                Close(new SocketException((int)e.SocketError)) ;
            }
        }

        private void BeginSendAsync(int _bytesTransferred)
        {
            lock(this)
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
                        _outputBufferListSending[i] = new ArraySegment<byte>(segment.Array, bytesTransferred, segment.Count - bytesTransferred);
                        _outputBufferListSending.RemoveRange(0, i);
                        break;
                    }
                }

                if (_outputBufferListSending.Count == 0)
                {
                    // 全部发送完
                    _outputBufferListSendingCountSum = 0;
                    doCodecAndPrepareSend();
                    if (_outputBufferListSending.Count == 0)
                        _outputBufferListSending = null; // free
                }
                else if (null != _outputBufferList)
                {
                    // 没有发送完，并且有要发送的
                    // 需要先调整剩余数量，然后doCodecAndPrepareSend会统计新的。
                    _outputBufferListSendingCountSum -= _bytesTransferred;
                    doCodecAndPrepareSend();
                }
                else
                {
                    // 没有发送完，也没有要发送的
                    _outputBufferListSendingCountSum -= _bytesTransferred;
                }

                if (null != _outputBufferListSending) // 全部发送完，并且 _outputBufferList == null 时，可能为 null
                {
                    eventArgsSend.BufferList = _outputBufferListSending;
                    if (false == Socket.SendAsync(eventArgsSend))
                        ProcessSend(eventArgsSend);
                }
            }
        }

        public void Close(Exception e)
        {
            this.LastException = e;
            Dispose();
        }

        public void Dispose()
        {
            lock (this)
            {
                if (Socket == null)
                    return;

                try
                {
                    Connector?.OnSocketClose(this);
                    Service.OnSocketClose(this, this.LastException);
                    Socket?.Dispose();
                    Socket = null;
                }
                catch (Exception)
                {
                    // skip Dispose error
                }
            }

            lock (this)
            {
                try
                {
                    Service.OnSocketDisposed(this);
                }
                catch (Exception)
                {
                    // skip Dispose error
                }
            }
        }

        public void SetSessionId(long newSessionId)
        {
            if (Service.SocketMapInternal.TryRemove(KeyValuePair.Create(SessionId, this)))
            {
                if (!Service.SocketMapInternal.TryAdd(newSessionId, this))
                {
                    Service.SocketMapInternal.TryAdd(SessionId, this); // rollback
                    throw new Exception($"duplicate sessionid {this}");
                }
                SessionId = newSessionId;
            }
            else
            {
                // 为了简化并发问题，只能加入Service以后的Socket的SessionId。
                throw new Exception($"Not Exist In Service {this}");
            }
        }
    }
}
