"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.Zeze = void 0;
const text_encoding_1 = require("text-encoding");
var HostLang;
var IsUe = false;
try {
    HostLang = require('csharp'); // puerts unity
}
catch (ex) {
    try {
        HostLang = require('ue'); // puerts unreal
        IsUe = true;
    }
    catch (ex) {
    }
}
var Zeze;
(function (Zeze) {
    class Long {
        static Validate(x) {
            if (x < Long.MIN_VALUE || x > Long.MAX_VALUE)
                throw new Error("is not a valid long value");
        }
        static ToUint8Array(bn, bytesCount = 8) {
            var hex = Long.ToHex(bn, bytesCount);
            var len = hex.length / 2;
            var u8 = new Uint8Array(len);
            var i = 0;
            var j = 0;
            while (i < len) {
                u8[i] = parseInt(hex.slice(j, j + 2), 16);
                i += 1;
                j += 2;
            }
            return u8;
        }
        static ToHex(bn, bytesCount = 8) {
            var pos = true;
            if (bn < 0) {
                pos = false;
                bn = Long.BitNot(-bn) + 1n;
            }
            var base = 16;
            var hex = bn.toString(base);
            if (hex.length % 2) {
                hex = '0' + hex;
            }
            // Check the high byte _after_ proper hex padding
            var highbyte = parseInt(hex.slice(0, 2), 16);
            var highbit = (0x80 & highbyte);
            if (pos && highbit) {
                hex = '00' + hex;
            }
            var bytes = hex.length / 2;
            if (bytes > bytesCount)
                throw new Error("bigint too big. bytes=" + bytes + ", bytesCount=" + bytesCount);
            var prefixBytes = bytesCount - bytes;
            if (prefixBytes > 0) {
                var prefix = '';
                var prefixString = pos ? '00' : 'ff';
                while (prefixBytes > 0) {
                    prefix = prefix + prefixString;
                    prefixBytes = prefixBytes - 1;
                }
                hex = prefix + hex;
            }
            return hex;
        }
        static BitNot(bn) {
            var bin = (bn).toString(2);
            var prefix = '';
            while (bin.length % 8) {
                bin = '0' + bin;
            }
            if ('1' === bin[0] && -1 !== bin.slice(1).indexOf('1')) {
                prefix = '11111111';
            }
            bin = bin.split('').map(function (i) {
                return '0' === i ? '1' : '0';
            }).join('');
            return BigInt('0b' + prefix + bin);
        }
        static FromUint8ArrayBigEndian(u8, offset, len) {
            var hex = [];
            var end = offset + len;
            var pos = true;
            if (len > 0 && (u8[offset] & 0x80)) {
                pos = false;
            }
            for (var i = offset; i < end; ++i) {
                var h = u8[i].toString(16);
                if (h.length % 2) {
                    h = '0' + h;
                }
                hex.push(h);
            }
            var bn = BigInt('0x' + hex.join(''));
            //console.log(bn.toString(16));
            if (!pos) {
                bn = BigInt('0b' + bn.toString(2).split('').map(function (i) { return '0' === i ? 1 : 0; }).join('')) + BigInt(1);
                bn = -bn;
                //console.log(bn.toString(16));
            }
            return bn;
        }
        static FromUint8ArrayLittleEndian(u8, offset, len) {
            var hex = [];
            var end = offset + len;
            var pos = true;
            if (len > 0 && (u8[end - 1] & 0x80)) {
                pos = false;
            }
            for (var i = end - 1; i >= offset; --i) {
                var h = u8[i].toString(16);
                if (h.length % 2) {
                    h = '0' + h;
                }
                hex.push(h);
            }
            var bn = BigInt('0x' + hex.join(''));
            if (!pos) {
                bn = BigInt('0b' + bn.toString(2).split('').map(function (i) { return '0' === i ? 1 : 0; }).join('')) + BigInt(1);
                bn = -bn;
            }
            return bn;
        }
    }
    Long.MAX_VALUE = 9223372036854775807n;
    Long.MIN_VALUE = -9223372036854775808n;
    Zeze.Long = Long;
    class EmptyBean {
        TypeId() {
            return EmptyBean.TYPEID;
        }
        Encode(bb) {
            bb.SkipUnknownField(ByteBuffer.BEAN);
        }
        Decode(bb) {
            bb.WriteByte(0);
        }
    }
    EmptyBean.TYPEID = 0n;
    Zeze.EmptyBean = EmptyBean;
    class DynamicBean {
        constructor(get, create) {
            this.GetSpecialTypeIdFromBean = get;
            this.CreateBeanFromSpecialTypeId = create;
            this._Bean = new EmptyBean();
            this._TypeId = EmptyBean.TYPEID;
        }
        TypeId() {
            return this._TypeId;
        }
        GetRealBean() {
            return this._Bean;
        }
        SetRealBean(bean) {
            var typeId = this.GetSpecialTypeIdFromBean(bean);
            this._Bean = bean;
            this._TypeId = typeId;
        }
        isEmpty() {
            return this._TypeId == EmptyBean.TYPEID && this._Bean instanceof EmptyBean;
        }
        Encode(bb) {
            bb.WriteLong(this.TypeId());
            this._Bean.Encode(bb);
        }
        Decode(bb) {
            var typeId = bb.ReadLong();
            var real = this.CreateBeanFromSpecialTypeId(typeId);
            if (real != null) {
                real.Decode(bb);
                this._Bean = real;
                this._TypeId = typeId;
            }
            else {
                bb.SkipUnknownField(ByteBuffer.BEAN);
                this._Bean = new EmptyBean();
                this._TypeId = EmptyBean.TYPEID;
            }
        }
    }
    Zeze.DynamicBean = DynamicBean;
    class Protocol {
        TypeId() {
            return BigInt(this.ModuleId()) << 32n | BigInt(this.ProtocolId());
        }
        EncodeProtocol() {
            var bb = new Zeze.ByteBuffer();
            bb.WriteInt4(this.ModuleId());
            bb.WriteInt4(this.ProtocolId());
            var state = bb.BeginWriteWithSize4();
            this.Encode(bb);
            bb.EndWriteWithSize4(state);
            return bb;
        }
        Send(socket) {
            var bb = this.EncodeProtocol();
            socket.Send(bb);
        }
        Dispatch(service, factoryHandle) {
            service.DispatchProtocol(this, factoryHandle);
        }
        static DecodeProtocols(service, socket, input) {
            var os = new Zeze.ByteBuffer(input.Bytes, input.ReadIndex, input.Size());
            while (os.Size() > 0) {
                var moduleId;
                var protocolId;
                var size;
                var readIndexSaved = os.ReadIndex;
                if (os.Size() >= 12) // protocl header size.
                 {
                    moduleId = os.ReadInt4();
                    protocolId = os.ReadInt4();
                    size = os.ReadInt4();
                }
                else {
                    input.ReadIndex = readIndexSaved;
                    return;
                }
                if (size > os.Size()) {
                    input.ReadIndex = readIndexSaved;
                    return;
                }
                var buffer = new Zeze.ByteBuffer(os.Bytes, os.ReadIndex, size);
                os.ReadIndex += size;
                var type = BigInt(moduleId) << 32n | BigInt(protocolId);
                var factoryHandle = service.FactoryHandleMap.get(type);
                if (null != factoryHandle) {
                    var p = factoryHandle.factory();
                    p.Decode(buffer);
                    p.Sender = socket;
                    p.Dispatch(service, factoryHandle);
                    continue;
                }
                service.DispatchUnknownProtocol(socket, type, buffer);
            }
            input.ReadIndex = os.ReadIndex;
        }
    }
    Zeze.Protocol = Protocol;
    class ProtocolWithArgument extends Protocol {
        constructor(argument) {
            super();
            this.Argument = argument;
        }
        Encode(_os_) {
            _os_.WriteLong(this.ResultCode);
            this.Argument.Encode(_os_);
        }
        Decode(_os_) {
            this.ResultCode = _os_.ReadLong();
            this.Argument.Decode(_os_);
        }
    }
    Zeze.ProtocolWithArgument = ProtocolWithArgument;
    class Rpc extends Zeze.ProtocolWithArgument {
        constructor(argument, result) {
            super(argument);
            this.IsTimeout = false;
            this.Result = result;
        }
        Send(socket) {
            throw new Error("Rpc Need Use SendWithCallback");
        }
        SendWithCallback(socket, responseHandle, timeoutMs = 5000) {
            this.Sender = socket;
            this.ResponseHandle = responseHandle;
            this.IsRequest = true;
            this.sid = socket.service.AddRpcContext(this);
            this.timeout = setTimeout(() => {
                var context = this.Sender.service.RemoveRpcContext(this.sid);
                if (context && context.ResponseHandle) {
                    context.IsTimeout = true;
                    context.ResponseHandle(context);
                }
            }, timeoutMs);
            super.Send(socket);
        }
        async SendForWait(socket, timeoutMs = 5000) {
            return new Promise((resolve, reject) => {
                this.SendWithCallback(socket, (response) => {
                    var res = response;
                    if (res.IsTimeout)
                        reject("Rpc.SendForWait Timeout");
                    else
                        resolve();
                    return 0;
                }, timeoutMs);
            });
        }
        SendResult() {
            this.IsRequest = false;
            super.Send(this.Sender);
        }
        SendResultCode(code) {
            this.ResultCode = code;
            this.SendResult();
        }
        Dispatch(service, factoryHandle) {
            if (this.IsRequest) {
                service.DispatchProtocol(this, factoryHandle);
                return;
            }
            var context = service.RemoveRpcContext(this.sid);
            if (null == context)
                return;
            if (context.timeout)
                clearTimeout(context.timeout);
            context.IsRequest = false;
            context.Result = this.Result;
            context.Sender = this.Sender;
            context.ResultCode = this.ResultCode;
            if (context.ResponseHandle)
                context.ResponseHandle(context);
        }
        Decode(bb) {
            this.IsRequest = bb.ReadBool();
            this.sid = bb.ReadLong();
            this.ResultCode = bb.ReadLong();
            if (this.IsRequest) {
                this.Argument.Decode(bb);
            }
            else {
                this.Result.Decode(bb);
            }
        }
        Encode(bb) {
            bb.WriteBool(this.IsRequest);
            bb.WriteLong(this.sid);
            bb.WriteLong(this.ResultCode);
            if (this.IsRequest) {
                this.Argument.Encode(bb);
            }
            else {
                this.Result.Encode(bb);
            }
        }
    }
    Zeze.Rpc = Rpc;
    class ProtocolFactoryHandle {
        constructor(f, h) {
            this.factory = f;
            this.handle = h;
        }
    }
    Zeze.ProtocolFactoryHandle = ProtocolFactoryHandle;
    class Socket {
        constructor(service, sessionId) {
            this.service = service;
            this.SessionId = sessionId;
        }
        Send(buffer) {
            this.service.Send(this.SessionId, buffer);
        }
        Close() {
            this.service.Close(this.SessionId);
        }
        OnProcessInput(newInput, offset, len) {
            if (null != this.InputBuffer) {
                this.InputBuffer.Append(new Uint8Array(newInput), offset, len);
                Zeze.Protocol.DecodeProtocols(this.service, this, this.InputBuffer);
                if (this.InputBuffer.Size() > 0)
                    this.InputBuffer.Campact();
                else
                    this.InputBuffer = null;
                return;
            }
            var bufdirect = new Zeze.ByteBuffer(new Uint8Array(newInput), offset, len);
            Zeze.Protocol.DecodeProtocols(this.service, this, bufdirect);
            if (bufdirect.Size() > 0) {
                bufdirect.Campact();
                this.InputBuffer = bufdirect;
            }
        }
    }
    Zeze.Socket = Socket;
    class ProtocolHead {
    }
    Zeze.ProtocolHead = ProtocolHead;
    class Service {
        constructor(name) {
            this.FactoryHandleMap = new Map();
            this.serialId = 0n;
            this.contexts = new Map();
            if (IsUe) {
                this.Implement = new HostLang.ToTypeScriptService();
            }
            else {
                this.Implement = new HostLang.ToTypeScriptService(name);
            }
            this.Implement.CallbackWhenSocketHandshakeDone = this.CallbackOnSocketHandshakeDone.bind(this);
            this.Implement.CallbackWhenSocketClose = this.CallbackOnSocketClose.bind(this);
            this.Implement.CallbackWhenSocketProcessInputBuffer = this.CallbackOnSocketProcessInputBuffer.bind(this);
        }
        AddRpcContext(rpc) {
            this.serialId = this.serialId + 1n;
            this.contexts.set(this.serialId, rpc);
            return this.serialId;
        }
        RemoveRpcContext(sid) {
            var ctx = this.contexts.get(sid);
            if (ctx) {
                this.contexts.delete(sid);
                return ctx;
            }
            return null;
        }
        DispatchUnknownProtocol(socket, type, buffer) {
        }
        DispatchProtocol(p, factoryHandle) {
            factoryHandle.handle(p);
        }
        CallbackOnSocketHandshakeDone(sessionId) {
            if (this.Connection)
                this.Connection.Close();
            this.Connection = new Socket(this, sessionId);
            if (this.ServiceEventHandle)
                this.ServiceEventHandle.OnSocketConnected(this, this.Connection);
        }
        CallbackOnSocketClose(sessionId) {
            if (this.Connection && this.Connection.SessionId == sessionId) {
                if (this.ServiceEventHandle)
                    this.ServiceEventHandle.OnSocketClosed(this, this.Connection);
                this.Connection = null;
            }
        }
        CallbackOnSocketProcessInputBuffer(sessionId, buffer, offset, len) {
            if (this.Connection.SessionId == sessionId) {
                if (this.ServiceEventHandle) {
                    if (this.ServiceEventHandle.OnSoekctInput(this, this.Connection, buffer, offset, len))
                        return;
                }
                this.Connection.OnProcessInput(buffer, offset, len);
            }
        }
        Connect(hostNameOrAddress, port, autoReconnect = true) {
            this.Implement.Connect(hostNameOrAddress, port, autoReconnect);
        }
        Send(sessionId, buffer) {
            this.Implement.Send(sessionId, buffer.Bytes.buffer, buffer.ReadIndex, buffer.Size());
        }
        Close(sessionId) {
            this.Implement.Close(sessionId);
        }
        TickUpdate() {
            this.Implement.TickUpdate();
        }
    }
    Zeze.Service = Service;
    class ByteBuffer {
        constructor(buffer = null, readIndex = 0, length = 0) {
            this.Bytes = (null == buffer) ? new Uint8Array(1024) : buffer;
            this.View = new DataView(this.Bytes.buffer);
            this.ReadIndex = readIndex;
            this.WriteIndex = this.ReadIndex + length;
        }
        Capacity() {
            return this.Bytes.byteLength;
        }
        Size() {
            return this.WriteIndex - this.ReadIndex;
        }
        ToPower2(needSize) {
            var size = 1024;
            while (size < needSize)
                size <<= 1;
            return size;
        }
        static BlockCopy(src, srcOffset, dst, dstOffset, count) {
            for (var i = 0; i < count; ++i) {
                dst[i + dstOffset] = src[i + srcOffset];
            }
        }
        Copy() {
            var copy = new Uint8Array(this.Size());
            ByteBuffer.BlockCopy(this.Bytes, this.ReadIndex, copy, 0, this.Size());
            return copy;
        }
        EnsureWrite(size) {
            var newSize = this.WriteIndex + size;
            if (newSize > this.Capacity()) {
                var newBytes = new Uint8Array(this.ToPower2(newSize));
                this.WriteIndex -= this.ReadIndex;
                ByteBuffer.BlockCopy(this.Bytes, this.ReadIndex, newBytes, 0, this.WriteIndex);
                this.ReadIndex = 0;
                this.Bytes = newBytes;
                this.View = new DataView(this.Bytes.buffer);
            }
        }
        Append(bytes, offset, len) {
            this.EnsureWrite(len);
            ByteBuffer.BlockCopy(bytes, offset, this.Bytes, this.WriteIndex, len);
            this.WriteIndex += len;
        }
        Replace(writeIndex, src, srcOffset, len) {
            if (writeIndex < this.ReadIndex || writeIndex + len > this.WriteIndex)
                throw new Error();
            ByteBuffer.BlockCopy(src, srcOffset, this.Bytes, writeIndex, len);
        }
        BeginWriteWithSize4() {
            var state = this.Size();
            this.EnsureWrite(4);
            this.WriteIndex += 4;
            return state;
        }
        EndWriteWithSize4(state) {
            var oldWriteIndex = state + this.ReadIndex;
            this.View.setInt32(oldWriteIndex, this.WriteIndex - oldWriteIndex - 4, true);
        }
        EnsureRead(size) {
            if (this.ReadIndex + size > this.WriteIndex)
                throw new Error("EnsureRead " + size);
        }
        Campact() {
            var size = this.Size();
            if (size > 0) {
                if (this.ReadIndex > 0) {
                    ByteBuffer.BlockCopy(this.Bytes, this.ReadIndex, this.Bytes, 0, size);
                    this.ReadIndex = 0;
                    this.WriteIndex = size;
                }
            }
            else
                this.Reset();
        }
        Reset() {
            this.ReadIndex = 0;
            this.WriteIndex = 0;
        }
        WriteBool(b) {
            this.EnsureWrite(1);
            this.Bytes[this.WriteIndex++] = b ? 1 : 0;
        }
        ReadBool() {
            return this.ReadLong() != 0n;
        }
        WriteByte(byte) {
            this.EnsureWrite(1);
            this.Bytes[this.WriteIndex++] = byte;
        }
        ReadByte() {
            this.EnsureRead(1);
            return this.Bytes[this.ReadIndex++];
        }
        WriteInt4(x) {
            this.EnsureWrite(4);
            this.View.setInt32(this.WriteIndex, x, true);
            this.WriteIndex += 4;
        }
        ReadInt4() {
            this.EnsureRead(4);
            var x = this.View.getInt32(this.ReadIndex, true);
            this.ReadIndex += 4;
            return x;
        }
        WriteLong8(x) {
            this.EnsureWrite(8);
            var u8 = Long.ToUint8Array(x, 8);
            for (var i = u8.length - 1, j = this.WriteIndex; i >= 0; --i, ++j)
                this.Bytes[j] = u8[i];
            this.WriteIndex += 8;
        }
        ReadLong8() {
            this.EnsureRead(8);
            var x = Long.FromUint8ArrayLittleEndian(this.Bytes, this.ReadIndex, 8);
            this.ReadIndex += 8;
            return x;
        }
        WriteUInt(u) {
            if (u >= 0) {
                if (u < 0x80) {
                    this.EnsureWrite(1); // 0xxx xxxx
                    this.Bytes[this.WriteIndex++] = (u > 0 ? u : 0);
                    return;
                }
                else if (u < 0x4000) {
                    this.EnsureWrite(2); // 10xx xxxx +1B
                    var bytes = this.Bytes;
                    var writeIndex = this.WriteIndex;
                    bytes[writeIndex] = (u >> 8) + 0x80;
                    bytes[writeIndex + 1] = u;
                    this.WriteIndex = writeIndex + 2;
                    return;
                }
                else if (u < 2097152) {
                    this.EnsureWrite(3); // 110x xxxx +2B
                    var bytes = this.Bytes;
                    var writeIndex = this.WriteIndex;
                    bytes[writeIndex] = (u >> 16) + 0xc0;
                    bytes[writeIndex + 1] = u >> 8;
                    bytes[writeIndex + 2] = u;
                    this.WriteIndex = writeIndex + 3;
                    return;
                }
                else if (u < 268435456) {
                    this.EnsureWrite(4); // 1110 xxxx +3B
                    var bytes = this.Bytes;
                    var writeIndex = this.WriteIndex;
                    bytes[writeIndex] = (u >> 24) + 0xe0;
                    bytes[writeIndex + 1] = u >> 16;
                    bytes[writeIndex + 2] = u >> 8;
                    bytes[writeIndex + 3] = u;
                    this.WriteIndex = writeIndex + 4;
                    return;
                }
            }
            this.EnsureWrite(5); // 1111 0000 +4B
            var bytes = this.Bytes;
            var writeIndex = this.WriteIndex;
            bytes[writeIndex] = 0xf0;
            bytes[writeIndex + 1] = u >> 24;
            bytes[writeIndex + 2] = u >> 16;
            bytes[writeIndex + 3] = u >> 8;
            bytes[writeIndex + 4] = u;
            this.WriteIndex = writeIndex + 5;
        }
        ReadUInt() {
            this.EnsureRead(1);
            var bytes = this.Bytes;
            var readIndex = this.ReadIndex;
            var x = bytes[readIndex];
            if (x < 0x80) {
                this.ReadIndex = readIndex + 1;
            }
            else if (x < 0xc0) {
                this.EnsureRead(2);
                x = ((x & 0x3f) << 8)
                    + bytes[readIndex + 1];
                this.ReadIndex = readIndex + 2;
            }
            else if (x < 0xe0) {
                this.EnsureRead(3);
                x = ((x & 0x1f) << 16)
                    + (bytes[readIndex + 1] << 8)
                    + bytes[readIndex + 2];
                this.ReadIndex = readIndex + 3;
            }
            else if (x < 0xf0) {
                this.EnsureRead(4);
                x = ((x & 0xf) << 24)
                    + (bytes[readIndex + 1] << 16)
                    + (bytes[readIndex + 2] << 8)
                    + bytes[readIndex + 3];
                this.ReadIndex = readIndex + 4;
            }
            else {
                this.EnsureRead(5);
                x = (bytes[readIndex + 1] << 24)
                    + (bytes[readIndex + 2] << 16)
                    + (bytes[readIndex + 3] << 8)
                    + bytes[readIndex + 4];
                this.ReadIndex = readIndex + 5;
            }
            return x;
        }
        WriteLong(x) {
            if (x >= 0) {
                if (x < 0x40) {
                    this.EnsureWrite(1); // 00xx xxxx
                    this.Bytes[this.WriteIndex++] = Number(x);
                }
                else if (x < 0x2000) {
                    this.EnsureWrite(2); // 010x xxxx +1B
                    var bytes = this.Bytes;
                    var writeIndex = this.WriteIndex;
                    var v = Number(x);
                    bytes[writeIndex] = (v >> 8) + 0x40;
                    bytes[writeIndex + 1] = v;
                    this.WriteIndex = writeIndex + 2;
                }
                else if (x < 1048576) {
                    this.EnsureWrite(3); // 0110 xxxx +2B
                    var bytes = this.Bytes;
                    var writeIndex = this.WriteIndex;
                    var v = Number(x);
                    bytes[writeIndex] = (v >> 16) + 0x60;
                    bytes[writeIndex + 1] = v >> 8;
                    bytes[writeIndex + 2] = v;
                    this.WriteIndex = writeIndex + 3;
                }
                else if (x < 134217728) {
                    this.EnsureWrite(4); // 0111 0xxx +3B
                    var bytes = this.Bytes;
                    var writeIndex = this.WriteIndex;
                    var v = Number(x);
                    bytes[writeIndex] = (v >> 24) + 0x70;
                    bytes[writeIndex + 1] = v >> 16;
                    bytes[writeIndex + 2] = v >> 8;
                    bytes[writeIndex + 3] = v;
                    this.WriteIndex = writeIndex + 4;
                }
                else if (x < 0x400000000n) {
                    this.EnsureWrite(5); // 0111 10xx +4B
                    var u8 = Long.ToUint8Array(x, 5);
                    this.Bytes[this.WriteIndex] = u8[0] + 0x78;
                    ByteBuffer.BlockCopy(u8, 1, this.Bytes, this.WriteIndex + 1, 4);
                    this.WriteIndex += 5;
                }
                else if (x < 0x20000000000n) {
                    this.EnsureWrite(6); // 0111 110x +5B
                    var u8 = Long.ToUint8Array(x, 6);
                    this.Bytes[this.WriteIndex] = u8[0] + 0x7c;
                    ByteBuffer.BlockCopy(u8, 1, this.Bytes, this.WriteIndex + 1, 5);
                    this.WriteIndex += 6;
                }
                else if (x < 0x1000000000000n) {
                    this.EnsureWrite(7); // 0111 1110 +6B
                    var u8 = Long.ToUint8Array(x, 7);
                    this.Bytes[this.WriteIndex] = u8[0] + 0x7e;
                    ByteBuffer.BlockCopy(u8, 1, this.Bytes, this.WriteIndex + 1, 6);
                    this.WriteIndex += 7;
                }
                else if (x < 0x80000000000000n) {
                    this.EnsureWrite(8); // 0111 1111 0 +7B
                    var u8 = Long.ToUint8Array(x, 8);
                    this.Bytes[this.WriteIndex] = u8[0] + 0x7f;
                    ByteBuffer.BlockCopy(u8, 1, this.Bytes, this.WriteIndex + 1, 7);
                    this.WriteIndex += 8;
                }
                else {
                    this.EnsureWrite(9); // 0111 1111 1 +8B
                    var u8 = Long.ToUint8Array(x, 8);
                    this.Bytes[this.WriteIndex] = 0x7f;
                    this.Bytes[this.WriteIndex + 1] = u8[0] + 0x80;
                    ByteBuffer.BlockCopy(u8, 1, this.Bytes, this.WriteIndex + 2, 7);
                    this.WriteIndex += 9;
                }
            }
            else {
                if (x >= -0x40) {
                    this.EnsureWrite(1); // 11xx xxxx
                    this.Bytes[this.WriteIndex++] = Number(x);
                }
                else if (x >= -0x2000) {
                    this.EnsureWrite(2); // 101x xxxx +1B
                    var bytes = this.Bytes;
                    var writeIndex = this.WriteIndex;
                    var v = Number(x);
                    bytes[writeIndex] = (v >> 8) - 0x40;
                    bytes[writeIndex + 1] = v;
                    this.WriteIndex = writeIndex + 2;
                }
                else if (x >= -1048576) {
                    this.EnsureWrite(3); // 1001 xxxx +2B
                    var bytes = this.Bytes;
                    var writeIndex = this.WriteIndex;
                    var v = Number(x);
                    bytes[writeIndex] = (v >> 16) - 0x60;
                    bytes[writeIndex + 1] = v >> 8;
                    bytes[writeIndex + 2] = v;
                    this.WriteIndex = writeIndex + 3;
                }
                else if (x >= -134217728) {
                    this.EnsureWrite(4); // 1000 1xxx +3B
                    var bytes = this.Bytes;
                    var writeIndex = this.WriteIndex;
                    var v = Number(x);
                    bytes[writeIndex] = (v >> 24) - 0x70;
                    bytes[writeIndex + 1] = v >> 16;
                    bytes[writeIndex + 2] = v >> 8;
                    bytes[writeIndex + 3] = v;
                    this.WriteIndex = writeIndex + 4;
                }
                else if (x >= -0x400000000n) {
                    this.EnsureWrite(5); // 1000 01xx +4B
                    var u8 = Long.ToUint8Array(x, 8);
                    this.Bytes[this.WriteIndex] = u8[3] - 0x78;
                    ByteBuffer.BlockCopy(u8, 4, this.Bytes, this.WriteIndex + 1, 4);
                    this.WriteIndex += 5;
                }
                else if (x >= -0x20000000000n) {
                    this.EnsureWrite(6); // 1000 001x +5B
                    var u8 = Long.ToUint8Array(x, 8);
                    this.Bytes[this.WriteIndex] = u8[2] - 0x7c;
                    ByteBuffer.BlockCopy(u8, 3, this.Bytes, this.WriteIndex + 1, 5);
                    this.WriteIndex += 6;
                }
                else if (x >= -0x1000000000000n) {
                    this.EnsureWrite(7); // 1000 0001 +6B
                    var u8 = Long.ToUint8Array(x, 8);
                    this.Bytes[this.WriteIndex] = 0x81;
                    ByteBuffer.BlockCopy(u8, 2, this.Bytes, this.WriteIndex + 1, 6);
                    this.WriteIndex += 7;
                }
                else if (x >= -0x80000000000000n) {
                    this.EnsureWrite(8); // 1000 0000 1 +7B
                    var u8 = Long.ToUint8Array(x, 8);
                    this.Bytes[this.WriteIndex] = 0x80;
                    ByteBuffer.BlockCopy(u8, 1, this.Bytes, this.WriteIndex + 1, 7);
                    this.WriteIndex += 8;
                }
                else {
                    this.EnsureWrite(9); // 1000 0000 0 +8B
                    var u8 = Long.ToUint8Array(x, 8);
                    this.Bytes[this.WriteIndex] = 0x80;
                    this.Bytes[this.WriteIndex + 1] = u8[0] - 0x80;
                    ByteBuffer.BlockCopy(u8, 1, this.Bytes, this.WriteIndex + 2, 7);
                    this.WriteIndex += 9;
                }
            }
        }
        ReadLong2BE() {
            this.EnsureRead(2);
            var bytes = this.Bytes;
            var readIndex = this.ReadIndex;
            this.ReadIndex = readIndex + 2;
            return (bytes[readIndex] << 8) +
                bytes[readIndex + 1];
        }
        ReadLong3BE() {
            this.EnsureRead(3);
            var bytes = this.Bytes;
            var readIndex = this.ReadIndex;
            this.ReadIndex = readIndex + 3;
            return (bytes[readIndex] << 16) +
                (bytes[readIndex + 1] << 8) +
                bytes[readIndex + 2];
        }
        ReadLong4BE() {
            this.EnsureRead(4);
            var readIndex = this.ReadIndex;
            this.ReadIndex = readIndex + 4;
            return Long.FromUint8ArrayBigEndian(this.Bytes, readIndex, 4) & 0xffffffffn;
        }
        ReadLong5BE() {
            this.EnsureRead(5);
            var readIndex = this.ReadIndex;
            this.ReadIndex = readIndex + 5;
            return Long.FromUint8ArrayBigEndian(this.Bytes, readIndex, 5) & 0xffffffffffn;
        }
        ReadLong6BE() {
            this.EnsureRead(6);
            var readIndex = this.ReadIndex;
            this.ReadIndex = readIndex + 6;
            return Long.FromUint8ArrayBigEndian(this.Bytes, readIndex, 6) & 0xffffffffffffn;
        }
        ReadLong7BE() {
            this.EnsureRead(7);
            var readIndex = this.ReadIndex;
            this.ReadIndex = readIndex + 7;
            return Long.FromUint8ArrayBigEndian(this.Bytes, readIndex, 7) & 0xffffffffffffffn;
        }
        ReadLong() {
            this.EnsureRead(1);
            var b = this.Bytes[this.ReadIndex++];
            b = b < 0x80 ? b : b - 0x100;
            switch ((b >> 3) & 0x1f) {
                case 0x00:
                case 0x01:
                case 0x02:
                case 0x03:
                case 0x04:
                case 0x05:
                case 0x06:
                case 0x07:
                case 0x18:
                case 0x19:
                case 0x1a:
                case 0x1b:
                case 0x1c:
                case 0x1d:
                case 0x1e:
                case 0x1f: return BigInt(b);
                case 0x08:
                case 0x09:
                case 0x0a:
                case 0x0b: return BigInt(((b - 0x40) << 8) + this.ReadByte());
                case 0x14:
                case 0x15:
                case 0x16:
                case 0x17: return BigInt(((b + 0x40) << 8) + this.ReadByte());
                case 0x0c:
                case 0x0d: return BigInt(((b - 0x60) << 16) + this.ReadLong2BE());
                case 0x12:
                case 0x13: return BigInt(((b + 0x60) << 16) + this.ReadLong2BE());
                case 0x0e: return BigInt(((b - 0x70) << 24) + this.ReadLong3BE());
                case 0x11: return BigInt(((b + 0x70) << 24) + this.ReadLong3BE());
                case 0x0f:
                    switch (b & 7) {
                        case 0:
                        case 1:
                        case 2:
                        case 3: return (BigInt(b - 0x78) << 32n) + this.ReadLong4BE();
                        case 4:
                        case 5: return (BigInt(b - 0x7c) << 40n) + this.ReadLong5BE();
                        case 6: return this.ReadLong6BE();
                        default:
                            var r = this.ReadLong7BE();
                            return r < 0x80000000000000n ?
                                r : ((r - 0x80000000000000n) << 8n) + BigInt(this.ReadByte());
                    }
                default: // 0x10
                    switch (b & 7) {
                        case 4:
                        case 5:
                        case 6:
                        case 7: return (BigInt(b + 0x78) << 32n) + this.ReadLong4BE();
                        case 2:
                        case 3: return (BigInt(b + 0x7c) << 40n) + this.ReadLong5BE();
                        case 1: return -0x1000000000000n + this.ReadLong6BE();
                        default:
                            var r = this.ReadLong7BE();
                            r = r >= 0x80000000000000n ?
                                -0x100000000000000n + r : ((r + 0x80000000000000n) << 8n) + BigInt(this.ReadByte());
                            return r < 0x8000000000000000n ? r : r - 0x10000000000000000n; // special fix
                    }
            }
        }
        WriteInt(x) {
            this.WriteLong(BigInt(x));
        }
        ReadInt() {
            return Number(this.ReadLong());
        }
        WriteFloat(x) {
            this.EnsureWrite(4);
            this.View.setFloat32(this.WriteIndex, x, true);
            this.WriteIndex += 4;
        }
        ReadFloat() {
            this.EnsureRead(4);
            var x = this.View.getFloat32(this.ReadIndex, true);
            this.ReadIndex += 4;
            return x;
        }
        WriteDouble(x) {
            this.EnsureWrite(8);
            this.View.setFloat64(this.WriteIndex, x, true);
            this.WriteIndex += 8;
        }
        ReadDouble() {
            this.EnsureRead(8);
            var x = this.View.getFloat64(this.ReadIndex, true);
            this.ReadIndex += 8;
            return x;
        }
        WriteString(x) {
            var utf8 = ByteBuffer.Encoder.encode(x);
            this.WriteBytes(utf8, 0, utf8.length);
        }
        ReadString() {
            return ByteBuffer.Decoder.decode(this.ReadBytes());
        }
        WriteBytes(x, offset = 0, length = -1) {
            if (length == -1)
                length = x.byteLength;
            this.WriteUInt(length);
            this.EnsureWrite(length);
            ByteBuffer.BlockCopy(x, offset, this.Bytes, this.WriteIndex, length);
            this.WriteIndex += length;
        }
        ReadBytes() {
            var n = this.ReadUInt();
            this.EnsureRead(n);
            var x = new Uint8Array(n);
            ByteBuffer.BlockCopy(this.Bytes, this.ReadIndex, x, 0, n);
            this.ReadIndex += n;
            return x;
        }
        SkipBytes() {
            var n = this.ReadUInt();
            this.EnsureRead(n);
            this.ReadIndex += n;
        }
        Equals(other) {
            if (other == null)
                return false;
            if (this.Size != other.Size)
                return false;
            var size = this.Size();
            for (var i = 0; i < size; i++) {
                if (this.Bytes[this.ReadIndex + i] != other.Bytes[other.ReadIndex + i])
                    return false;
            }
            return true;
        }
        static toHex(x) {
            var l = x & 0x0f;
            var h = (x >> 4) & 0x0f;
            return this.HEX[h] + this.HEX[l];
        }
        static toString(x, from = 0, to = -1) {
            var ss = "";
            var bfirst = true;
            if (to == -1)
                to = x.byteLength;
            for (var i = from; i < to; ++i) {
                if (bfirst)
                    bfirst = false;
                else
                    ss = ss.concat("-");
                ss = ss.concat(ByteBuffer.toHex(x[i]));
            }
            return ss;
        }
        // for debug
        toString() {
            return ByteBuffer.toString(this.Bytes, this.ReadIndex, this.WriteIndex);
        }
        WriteTag(lastVarId, varId, type) {
            var deltaId = varId - lastVarId;
            if (deltaId < 0xf)
                this.WriteByte((deltaId << ByteBuffer.TAG_SHIFT) + type);
            else {
                this.WriteByte(0xf0 + type);
                this.WriteUInt(deltaId - 0xf);
            }
            return varId;
        }
        WriteListType(listSize, elemType) {
            if (listSize < 0xf)
                this.WriteByte((listSize << ByteBuffer.TAG_SHIFT) + elemType);
            else {
                this.WriteByte(0xf0 + elemType);
                this.WriteUInt(listSize - 0xf);
            }
        }
        WriteMapType(mapSize, keyType, valueType) {
            this.WriteByte((keyType << ByteBuffer.TAG_SHIFT) + valueType);
            this.WriteUInt(mapSize);
        }
        ReadTagSize(tagByte) {
            var deltaId = (tagByte & ByteBuffer.ID_MASK) >> ByteBuffer.TAG_SHIFT;
            return deltaId < 0xf ? deltaId : 0xf + this.ReadUInt();
        }
        ReadBoolT(type) {
            type &= ByteBuffer.TAG_MASK;
            if (type == ByteBuffer.INTEGER)
                return this.ReadLong() != 0n;
            if (type == ByteBuffer.FLOAT)
                return this.ReadFloat() != 0;
            if (type == ByteBuffer.DOUBLE)
                return this.ReadDouble() != 0;
            this.SkipUnknownField(type);
            return false;
        }
        ReadIntT(type) {
            type &= ByteBuffer.TAG_MASK;
            if (type == ByteBuffer.INTEGER)
                return Number(this.ReadLong());
            if (type == ByteBuffer.FLOAT)
                return this.ReadFloat();
            if (type == ByteBuffer.DOUBLE)
                return this.ReadDouble();
            this.SkipUnknownField(type);
            return 0;
        }
        ReadLongT(type) {
            type &= ByteBuffer.TAG_MASK;
            if (type == ByteBuffer.INTEGER)
                return this.ReadLong();
            if (type == ByteBuffer.FLOAT)
                return BigInt(this.ReadFloat());
            if (type == ByteBuffer.DOUBLE)
                return BigInt(this.ReadDouble());
            this.SkipUnknownField(type);
            return 0n;
        }
        ReadFloatT(type) {
            type &= ByteBuffer.TAG_MASK;
            if (type == ByteBuffer.FLOAT)
                return this.ReadFloat();
            if (type == ByteBuffer.DOUBLE)
                return this.ReadDouble();
            if (type == ByteBuffer.INTEGER)
                return Number(this.ReadLong());
            this.SkipUnknownField(type);
            return 0;
        }
        ReadDoubleT(type) {
            type &= ByteBuffer.TAG_MASK;
            if (type == ByteBuffer.DOUBLE)
                return this.ReadDouble();
            if (type == ByteBuffer.FLOAT)
                return this.ReadFloat();
            if (type == ByteBuffer.INTEGER)
                return Number(this.ReadLong());
            this.SkipUnknownField(type);
            return 0;
        }
        ReadBytesT(type) {
            type &= ByteBuffer.TAG_MASK;
            if (type == ByteBuffer.BYTES)
                return this.ReadBytes();
            this.SkipUnknownField(type);
            return new Uint8Array(0);
        }
        ReadStringT(type) {
            type &= ByteBuffer.TAG_MASK;
            if (type == ByteBuffer.BYTES)
                return this.ReadString();
            this.SkipUnknownField(type);
            return "";
        }
        ReadBean(bean, type) {
            type &= ByteBuffer.TAG_MASK;
            if (type == ByteBuffer.BEAN)
                bean.Decode(this);
            else if (type == ByteBuffer.DYNAMIC) {
                this.ReadLong();
                bean.Decode(this);
            }
            else
                this.SkipUnknownField(type);
            return bean;
        }
        ReadDynamic(dynBean, type) {
            type &= ByteBuffer.TAG_MASK;
            if (type == ByteBuffer.DYNAMIC) {
                dynBean.Decode(this);
                return dynBean;
            }
            if (type == ByteBuffer.BEAN) {
                var bean = dynBean.CreateBeanFromSpecialTypeId(0);
                if (bean != null) {
                    bean.Decode(this);
                    return dynBean;
                }
            }
            this.SkipUnknownField(type);
            return dynBean;
        }
        SkipUnknownList(type, count) {
            while (--count >= 0)
                this.SkipUnknownField(type);
        }
        SkipUnknownMap(type1, type2, count) {
            while (--count >= 0) {
                this.SkipUnknownField(type1);
                this.SkipUnknownField(type2);
            }
        }
        SkipUnknownField(type) {
            switch (type & ByteBuffer.TAG_MASK) {
                case ByteBuffer.INTEGER:
                    this.ReadLong();
                    return;
                case ByteBuffer.FLOAT:
                    this.EnsureRead(4);
                    this.ReadIndex += 4;
                    return;
                case ByteBuffer.DOUBLE:
                    this.EnsureRead(8);
                    this.ReadIndex += 8;
                    return;
                case ByteBuffer.BYTES:
                    this.SkipBytes();
                    return;
                case ByteBuffer.LIST:
                    var t = this.ReadByte();
                    this.SkipUnknownList(t, this.ReadTagSize(t));
                    return;
                case ByteBuffer.MAP:
                    t = this.ReadByte();
                    this.SkipUnknownMap(t >> ByteBuffer.TAG_SHIFT, t, this.ReadUInt());
                    return;
                case ByteBuffer.DYNAMIC:
                    this.ReadLong();
                case ByteBuffer.BEAN:
                    while ((t = this.ReadByte()) != 0) {
                        if ((t & ByteBuffer.ID_MASK) == 0xf0)
                            this.ReadUInt();
                        this.SkipUnknownField(t);
                    }
                    return;
                default:
                    throw new Error("SkipUnknownField");
            }
        }
    }
    ByteBuffer.Encoder = new text_encoding_1.TextEncoder();
    ByteBuffer.Decoder = new text_encoding_1.TextDecoder();
    ByteBuffer.HEX = "0123456789ABCDEF";
    // ֻ�������µ����Ͷ��壬����ʱ�ǵ�ͬ�� SkipUnknownField
    ByteBuffer.INTEGER = 0; // byte,short,int,long,bool
    ByteBuffer.FLOAT = 1; // float
    ByteBuffer.DOUBLE = 2; // double
    ByteBuffer.BYTES = 3; // binary,string
    ByteBuffer.LIST = 4; // list,set
    ByteBuffer.MAP = 5; // map
    ByteBuffer.BEAN = 6; // bean
    ByteBuffer.DYNAMIC = 7; // dynamic
    ByteBuffer.TAG_SHIFT = 4;
    ByteBuffer.TAG_MASK = (1 << ByteBuffer.TAG_SHIFT) - 1;
    ByteBuffer.ID_MASK = 0xff - ByteBuffer.TAG_MASK;
    Zeze.ByteBuffer = ByteBuffer;
})(Zeze = exports.Zeze || (exports.Zeze = {}));
//# sourceMappingURL=zeze.js.map