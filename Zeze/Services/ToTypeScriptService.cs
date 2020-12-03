using System;
using System.Collections.Generic;

namespace Zeze.Services
{
    public class ToTypeScriptService0 : HandshakeClient
    {
        public ToTypeScriptService0(string name) : base(name, null)
        {

        }

        public override void OnSocketProcessInputBuffer(Zeze.Net.AsyncSocket so, Zeze.Serialize.ByteBuffer input)
        {
            if (so.IsHandshakeDone)
            {
                AppendInputBuffer(so.SessionId, input);
                input.ReadIndex = input.WriteIndex;
            }
            else
            {
                base.OnSocketProcessInputBuffer(so, input);
            }
        }

        public override void OnSocketClose(Zeze.Net.AsyncSocket so, Exception e)
        {
            SetSocketClose(so.SessionId);
            base.OnSocketClose(so, e);
        }

        public override void OnHandshakeDone(Zeze.Net.AsyncSocket sender)
        {
            sender.IsHandshakeDone = true;
            SetHandshakeDone(sender.SessionId);
        }

        protected Dictionary<long, Zeze.Serialize.ByteBuffer> ToBuffer = new Dictionary<long, Zeze.Serialize.ByteBuffer>();
        protected HashSet<long> ToHandshakeDone = new HashSet<long>();
        protected HashSet<long> ToSocketClose = new HashSet<long>();

        internal void SetHandshakeDone(long socketSessionId)
        {
            lock (this)
            {
                ToHandshakeDone.Add(socketSessionId);
            }
        }

        internal void SetSocketClose(long socketSessionId)
        {
            lock (this)
            {
                ToSocketClose.Add(socketSessionId);
            }
        }

        internal void AppendInputBuffer(long socketSessionId, Zeze.Serialize.ByteBuffer buffer)
        {
            lock (this)
            {
                if (ToBuffer.TryGetValue(socketSessionId, out var exist))
                {
                    exist.Append(buffer.Bytes, buffer.ReadIndex, buffer.Size);
                    return;
                }
                Serialize.ByteBuffer newBuffer = Serialize.ByteBuffer.Allocate();
                ToBuffer.Add(socketSessionId, newBuffer);
                newBuffer.Append(buffer.Bytes, buffer.ReadIndex, buffer.Size);
            }
        }
    }
}

#if USE_UNITY_PUERTS
// 下面这个类是真正开放给ts用的，使用Puerts绑定，需要Puerts支持。
// 使用的时候拷贝下面的代码到你自己的ToTypeScriptService.cs文件。
// 并且在Puerts.Binding里面增加 typeof 绑定到ts。

public delegate void CallbackOnSocketHandshakeDone(long sessionId);
public delegate void CallbackOnSocketClose(long sessionId);
public delegate void CallbackOnSocketProcessInputBuffer(long sessionId, Puerts.ArrayBuffer buffer, int offset, int len);

public class ToTypeScriptService : Zeze.Services.ToTypeScriptService0
{
    public CallbackOnSocketHandshakeDone CallbackWhenSocketHandshakeDone;
    public CallbackOnSocketClose CallbackWhenSocketClose;
    public CallbackOnSocketProcessInputBuffer CallbackWhenSocketProcessInputBuffer;

    public ToTypeScriptService(string name) : base(name)
    {
    }

    public new void Connect(string hostNameOrAddress, int port, bool autoReconnect = true)
    {
        base.Connect(hostNameOrAddress, port, autoReconnect);
    }

    public void Send(long sessionId, Puerts.ArrayBuffer buffer, int offset, int len)
    {
        base.GetSocket(sessionId)?.Send(buffer.Bytes, offset, len);
    }
        
    public void Close(long sessionId)
    {
        base.GetSocket(sessionId)?.Dispose();
    }

    public void TickUpdate()
    {
        System.Collections.Generic.HashSet<long> handshakeTmp;
        System.Collections.Generic.HashSet<long> socketCloseTmp;
        System.Collections.Generic.Dictionary<long, Serialize.ByteBuffer> inputTmp;
        lock (this)
        {
            handshakeTmp = ToHandshakeDone;
            socketCloseTmp = ToSocketClose;
            inputTmp = ToBuffer;

            ToBuffer = new System.Collections.Generic.Dictionary<long, Zeze.Serialize.ByteBuffer>();
            ToHandshakeDone = new System.Collections.Generic.HashSet<long>();
            ToSocketClose = new System.Collections.Generic.HashSet<long>();
        }

        foreach (var e in socketCloseTmp)
        {
            this.CallbackWhenSocketClose(e);
        }

        foreach (var e in handshakeTmp)
        {
            this.CallbackWhenSocketHandshakeDone(e);
        }

        foreach (var e in inputTmp)
        {
            this.CallbackWhenSocketProcessInputBuffer(e.Key, new Puerts.ArrayBuffer(e.Value.Bytes), e.Value.ReadIndex, e.Value.Size);
        }
    }
}

#endif
