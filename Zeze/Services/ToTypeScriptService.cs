﻿using System;
using System.Collections.Generic;

#if USE_PUERTS

namespace Zeze.Services
{
    public class ToTypeScriptService : HandshakeClient
    {
        public delegate void CallbackOnSocketHandshakeDone(ToTypeScriptService service, long sessionId);
        public delegate void CallbackOnSocketClose(ToTypeScriptService service, long sessionId);
        public delegate void CallbackOnSocketProcessInputBuffer(ToTypeScriptService service, Puerts.ArrayBuffer buffer);

        private CallbackOnSocketHandshakeDone CallbackSocketHandshakeDone;
        private CallbackOnSocketClose CallbackSocketClose;
        private CallbackOnSocketProcessInputBuffer CallbackSocketProcessInputBuffer;

        public ToTypeScriptService(string name,
            CallbackOnSocketHandshakeDone onSocketHandshakeDone,
            CallbackOnSocketClose onSocketClose,
            CallbackOnSocketProcessInputBuffer onSocketProcessInputBuffer)
            : base(name, null)
        {
            this.CallbackSocketHandshakeDone = onSocketHandshakeDone;
            this.CallbackSocketClose = onSocketClose;
            this.CallbackSocketProcessInputBuffer = onSocketProcessInputBuffer;
        }

        public void Send(long sessionId, Puerts.ArrayBuffer buffer, int offset, int len)
        {
            base.GetSocket(sessionId)?.Send(buffer.Bytes, offset, len);
        }
        
        public void Close(long sessionId)
        {
            base.GetSocket(sessionId)?.Dispose();
        }

        public override void OnHandshakeDone(Zeze.Net.AsyncSocket sender)
        {
            sender.IsHandshakeDone = true;
            SetHandshakeDone(sender.SessionId);
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

        private Dictionary<long, Zeze.Serialize.ByteBuffer> ToBuffer = new Dictionary<long, Zeze.Serialize.ByteBuffer>();
        private HashSet<long> ToHandshakeDone = new HashSet<long>();
        private HashSet<long> ToSocketClose = new HashSet<long>();

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

        public void TickUpdate()
        {
            HashSet<long> handshakeTmp;
            HashSet<long> socketCloseTmp;
            Dictionary<long, Serialize.ByteBuffer> inputTmp;
            lock (this)
            {
                handshakeTmp = ToHandshakeDone;
                socketCloseTmp = ToSocketClose;
                inputTmp = ToBuffer;

                ToBuffer = new Dictionary<long, Zeze.Serialize.ByteBuffer>();
                ToHandshakeDone = new HashSet<long>();
                ToSocketClose = new HashSet<long>();
            }

            foreach (var e in socketCloseTmp)
            {
                this.CallbackSocketClose(e);
            }

            foreach (var e in handshakeTmp)
            {
                this.CallbackSocketHandshakeDone(e);
            }

            foreach (var e in inputTmp)
            {
                this.CallbackSocketProcessInputBuffer(e.Key, new Puerts.ArrayBuffer(e.Value.Bytes), e.Value.ReadIndex, e.Value.Size());
            }
        }
    }
}

#endif // USE_PUERTS