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
                throw new Error("bigint too big. bytesCount=" + bytesCount);
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
        Encode(_os_) {
            _os_.WriteInt(0);
        }
        Decode(_os_) {
            _os_.ReadInt();
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
        Encode(_os_) {
            _os_.WriteLong8(this.TypeId());
            var _state_ = _os_.BeginWriteSegment();
            this._Bean.Encode(_os_);
            _os_.EndWriteSegment(_state_);
        }
        Decode(_os_) {
            var typeId = _os_.ReadLong8();
            var real = this.CreateBeanFromSpecialTypeId(typeId);
            if (null != real) {
                var _state_ = _os_.BeginReadSegment();
                real.Decode(_os_);
                _os_.EndReadSegment(_state_);
                this._Bean = real;
                this._TypeId = typeId;
            }
            else {
                _os_.SkipBytes();
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
            _os_.WriteInt(this.ResultCode);
            this.Argument.Encode(_os_);
        }
        Decode(_os_) {
            this.ResultCode = _os_.ReadInt();
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
            this.ResultCode = bb.ReadInt();
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
            bb.WriteInt(this.ResultCode);
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
        BeginWriteSegment() {
            var oldSize = this.Size();
            this.EnsureWrite(1);
            this.WriteIndex += 1;
            return oldSize;
        }
        EndWriteSegment(oldSize) {
            var startPos = this.ReadIndex + oldSize;
            var segmentSize = this.WriteIndex - startPos - 1;
            // 0 111 1111
            if (segmentSize < 0x80) {
                this.Bytes[startPos] = segmentSize;
            }
            else if (segmentSize < 0x4000) // 10 11 1111, -
             {
                this.EnsureWrite(1);
                this.Bytes[this.WriteIndex] = this.Bytes[startPos + 1];
                this.Bytes[startPos + 1] = segmentSize;
                this.Bytes[startPos] = ((segmentSize >> 8) | 0x80);
                this.WriteIndex += 1;
            }
            else if (segmentSize < 0x200000) // 110 1 1111, -,-
             {
                this.EnsureWrite(2);
                this.Bytes[this.WriteIndex + 1] = this.Bytes[startPos + 2];
                this.Bytes[startPos + 2] = segmentSize;
                this.Bytes[this.WriteIndex] = this.Bytes[startPos + 1];
                this.Bytes[startPos + 1] = (segmentSize >> 8);
                this.Bytes[startPos] = ((segmentSize >> 16) | 0xc0);
                this.WriteIndex += 2;
            }
            else if (segmentSize < 0x10000000) // 1110 1111,-,-,-
             {
                this.EnsureWrite(3);
                this.Bytes[this.WriteIndex + 2] = this.Bytes[startPos + 3];
                this.Bytes[startPos + 3] = segmentSize;
                this.Bytes[this.WriteIndex + 1] = this.Bytes[startPos + 2];
                this.Bytes[startPos + 2] = (segmentSize >> 8);
                this.Bytes[this.WriteIndex] = this.Bytes[startPos + 1];
                this.Bytes[startPos + 1] = (segmentSize >> 16);
                this.Bytes[startPos] = ((segmentSize >> 24) | 0xe0);
                this.WriteIndex += 3;
            }
            else {
                throw new Error("exceed max segment size");
            }
        }
        EnsureRead(size) {
            if (this.ReadIndex + size > this.WriteIndex)
                throw new Error("EnsureRead " + size);
        }
        ReadSegment() {
            this.EnsureRead(1);
            var h = this.Bytes[this.ReadIndex++];
            var startIndex = this.ReadIndex;
            var segmentSize = 0;
            if (h < 0x80) {
                segmentSize = h;
                this.ReadIndex += segmentSize;
            }
            else if (h < 0xc0) {
                this.EnsureRead(1);
                segmentSize = ((h & 0x3f) << 8) | this.Bytes[this.ReadIndex];
                var endPos = this.ReadIndex + segmentSize;
                this.Bytes[this.ReadIndex] = this.Bytes[endPos];
                this.ReadIndex += segmentSize + 1;
            }
            else if (h < 0xe0) {
                this.EnsureRead(2);
                segmentSize = ((h & 0x1f) << 16) | (this.Bytes[this.ReadIndex] << 8) | this.Bytes[this.ReadIndex + 1];
                var endPos = this.ReadIndex + segmentSize;
                this.Bytes[this.ReadIndex] = this.Bytes[endPos];
                this.Bytes[this.ReadIndex + 1] = this.Bytes[endPos + 1];
                this.ReadIndex += segmentSize + 2;
            }
            else if (h < 0xf0) {
                this.EnsureRead(3);
                segmentSize = ((h & 0x0f) << 24) | (this.Bytes[this.ReadIndex] << 16) | (this.Bytes[this.ReadIndex + 1] << 8) | this.Bytes[this.ReadIndex + 2];
                var endPos = this.ReadIndex + segmentSize;
                this.Bytes[this.ReadIndex] = this.Bytes[endPos];
                this.Bytes[this.ReadIndex + 1] = this.Bytes[endPos + 1];
                this.Bytes[this.ReadIndex + 2] = this.Bytes[endPos + 2];
                this.ReadIndex += segmentSize + 3;
            }
            else {
                throw new Error("exceed max size");
            }
            if (this.ReadIndex > this.WriteIndex) {
                throw new Error("segment data not enough");
            }
            return { startIndex: startIndex, segmentSize: segmentSize };
        }
        BeginReadSegment() {
            const { startIndex, segmentSize } = this.ReadSegment();
            var saveState = this.ReadIndex;
            this.ReadIndex = startIndex;
            return saveState;
        }
        EndReadSegment(saveState) {
            this.ReadIndex = saveState;
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
            else {
                this.Reset();
            }
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
            this.EnsureRead(1);
            return this.Bytes[this.ReadIndex++] != 0;
        }
        WriteByte(byte) {
            this.EnsureWrite(1);
            this.Bytes[this.WriteIndex++] = byte;
        }
        ReadByte() {
            this.EnsureRead(1);
            return this.Bytes[this.ReadIndex++];
        }
        WriteShort(x) {
            if (x >= 0) {
                if (x < 0x80) {
                    this.EnsureWrite(1);
                    this.Bytes[this.WriteIndex++] = x;
                    return;
                }
                if (x < 0x4000) {
                    this.EnsureWrite(2);
                    this.Bytes[this.WriteIndex + 1] = x;
                    this.Bytes[this.WriteIndex] = ((x >> 8) | 0x80);
                    this.WriteIndex += 2;
                    return;
                }
            }
            this.EnsureWrite(3);
            this.Bytes[this.WriteIndex] = 0xff;
            this.View.setInt16(this.WriteIndex + 1, x, false);
            this.WriteIndex += 3;
        }
        ReadShort() {
            this.EnsureRead(1);
            var h = this.Bytes[this.ReadIndex];
            if (h < 0x80) {
                this.ReadIndex++;
                return h;
            }
            if (h < 0xc0) {
                this.EnsureRead(2);
                var x = ((h & 0x3f) << 8) | this.Bytes[this.ReadIndex + 1];
                this.ReadIndex += 2;
                return x;
            }
            if ((h == 0xff)) {
                this.EnsureRead(3);
                var x = this.View.getInt16(this.ReadIndex + 1, false);
                this.ReadIndex += 3;
                return x;
            }
            throw new Error();
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
            for (var i = u8.length - 1, j = this.WriteIndex; i >= 0; --i, ++j) {
                this.Bytes[j] = u8[i];
            }
            this.WriteIndex += 8;
        }
        ReadLong8() {
            this.EnsureRead(8);
            var x = Long.FromUint8ArrayLittleEndian(this.Bytes, this.ReadIndex, 8);
            this.ReadIndex += 8;
            return x;
        }
        WriteInt(x) {
            if (x >= 0) {
                // 0 111 1111
                if (x < 0x80) {
                    this.EnsureWrite(1);
                    this.Bytes[this.WriteIndex++] = x;
                    return;
                }
                if (x < 0x4000) // 10 11 1111, -
                 {
                    this.EnsureWrite(2);
                    this.Bytes[this.WriteIndex + 1] = x;
                    this.Bytes[this.WriteIndex] = ((x >> 8) | 0x80);
                    this.WriteIndex += 2;
                    return;
                }
                if (x < 0x200000) // 110 1 1111, -,-
                 {
                    this.EnsureWrite(3);
                    this.Bytes[this.WriteIndex + 2] = x;
                    this.Bytes[this.WriteIndex + 1] = (x >> 8);
                    this.Bytes[this.WriteIndex] = ((x >> 16) | 0xc0);
                    this.WriteIndex += 3;
                    return;
                }
                if (x < 0x10000000) // 1110 1111,-,-,-
                 {
                    this.EnsureWrite(4);
                    this.Bytes[this.WriteIndex + 3] = x;
                    this.Bytes[this.WriteIndex + 2] = (x >> 8);
                    this.Bytes[this.WriteIndex + 1] = (x >> 16);
                    this.Bytes[this.WriteIndex] = ((x >> 24) | 0xe0);
                    this.WriteIndex += 4;
                    return;
                }
            }
            this.EnsureWrite(5);
            this.Bytes[this.WriteIndex] = 0xf0;
            this.View.setInt32(this.WriteIndex + 1, x, false);
            this.WriteIndex += 5;
        }
        ReadInt() {
            this.EnsureRead(1);
            var h = this.Bytes[this.ReadIndex];
            if (h < 0x80) {
                this.ReadIndex++;
                return h;
            }
            if (h < 0xc0) {
                this.EnsureRead(2);
                var x = ((h & 0x3f) << 8) | this.Bytes[this.ReadIndex + 1];
                this.ReadIndex += 2;
                return x;
            }
            if (h < 0xe0) {
                this.EnsureRead(3);
                var x = ((h & 0x1f) << 16) | (this.Bytes[this.ReadIndex + 1] << 8) | this.Bytes[this.ReadIndex + 2];
                this.ReadIndex += 3;
                return x;
            }
            if (h < 0xf0) {
                this.EnsureRead(4);
                var x = ((h & 0x0f) << 24) | (this.Bytes[this.ReadIndex + 1] << 16) | (this.Bytes[this.ReadIndex + 2] << 8) | this.Bytes[this.ReadIndex + 3];
                this.ReadIndex += 4;
                return x;
            }
            this.EnsureRead(5);
            var x = this.View.getInt32(this.ReadIndex + 1, false);
            this.ReadIndex += 5;
            return x;
        }
        WriteLong(x) {
            // 0 111 1111
            if (x >= 0) {
                if (x < 0x80) {
                    this.EnsureWrite(1);
                    this.Bytes[this.WriteIndex++] = Number(x);
                    return;
                }
                if (x < 0x4000) // 10 11 1111, -
                 {
                    this.EnsureWrite(2);
                    var uint16 = Number(x);
                    this.Bytes[this.WriteIndex + 1] = uint16 & 0xff;
                    this.Bytes[this.WriteIndex] = ((uint16 >> 8) | 0x80);
                    this.WriteIndex += 2;
                    return;
                }
                if (x < 0x200000) // 110 1 1111, -,-
                 {
                    this.EnsureWrite(3);
                    var uint32 = Number(x);
                    this.Bytes[this.WriteIndex + 2] = uint32 & 0xff;
                    this.Bytes[this.WriteIndex + 1] = (uint32 >> 8 & 0xff);
                    this.Bytes[this.WriteIndex] = ((uint32 >> 16) | 0xc0);
                    this.WriteIndex += 3;
                    return;
                }
                if (x < 0x10000000) // 1110 1111,-,-,-
                 {
                    this.EnsureWrite(4);
                    var uint32 = Number(x);
                    this.Bytes[this.WriteIndex + 3] = uint32 & 0xff;
                    this.Bytes[this.WriteIndex + 2] = (uint32 >> 8 & 0xff);
                    this.Bytes[this.WriteIndex + 1] = (uint32 >> 16 & 0xff);
                    this.Bytes[this.WriteIndex] = ((uint32 >> 24) | 0xe0);
                    this.WriteIndex += 4;
                    return;
                }
                if (x < 0x800000000) // 1111 0xxx,-,-,-,-
                 {
                    this.EnsureWrite(5);
                    var u8 = Long.ToUint8Array(x, 5);
                    this.Bytes[this.WriteIndex] = (u8[0] | 0xf0);
                    ByteBuffer.BlockCopy(u8, 1, this.Bytes, this.WriteIndex + 1, 4);
                    this.WriteIndex += 5;
                    return;
                }
                if (x < 0x40000000000n) // 1111 10xx, 
                 {
                    this.EnsureWrite(6);
                    var u8 = Long.ToUint8Array(x, 6);
                    this.Bytes[this.WriteIndex] = (u8[0] | 0xf8);
                    ByteBuffer.BlockCopy(u8, 1, this.Bytes, this.WriteIndex + 1, 5);
                    this.WriteIndex += 6;
                    return;
                }
                if (x < 0x2000000000000n) // 1111 110x,
                 {
                    this.EnsureWrite(7);
                    var u8 = Long.ToUint8Array(x, 7);
                    this.Bytes[this.WriteIndex] = (u8[0] | 0xfc);
                    ByteBuffer.BlockCopy(u8, 1, this.Bytes, this.WriteIndex + 1, 6);
                    this.WriteIndex += 7;
                    return;
                }
                if (x < 0x100000000000000n) // 1111 1110
                 {
                    this.EnsureWrite(8);
                    var u8 = Long.ToUint8Array(x, 8);
                    this.Bytes[this.WriteIndex] = 0xfe;
                    ByteBuffer.BlockCopy(u8, 1, this.Bytes, this.WriteIndex + 1, 7);
                    this.WriteIndex += 8;
                    return;
                }
                // else fall down
            }
            // 1111 1111
            this.EnsureWrite(9);
            this.Bytes[this.WriteIndex] = 0xff;
            var u8 = Long.ToUint8Array(x, 8);
            ByteBuffer.BlockCopy(u8, 0, this.Bytes, this.WriteIndex + 1, 8);
            this.WriteIndex += 9;
        }
        ReadLong() {
            this.EnsureRead(1);
            var h = this.Bytes[this.ReadIndex];
            if (h < 0x80) {
                this.ReadIndex++;
                return BigInt(h);
            }
            if (h < 0xc0) {
                this.EnsureRead(2);
                var x = ((h & 0x3f) << 8) | this.Bytes[this.ReadIndex + 1];
                this.ReadIndex += 2;
                return BigInt(x);
            }
            if (h < 0xe0) {
                this.EnsureRead(3);
                var x = ((h & 0x1f) << 16) | (this.Bytes[this.ReadIndex + 1] << 8) | this.Bytes[this.ReadIndex + 2];
                this.ReadIndex += 3;
                return BigInt(x);
            }
            if (h < 0xf0) {
                this.EnsureRead(4);
                var x = ((h & 0x0f) << 24) | (this.Bytes[this.ReadIndex + 1] << 16) | (this.Bytes[this.ReadIndex + 2] << 8) | this.Bytes[this.ReadIndex + 3];
                this.ReadIndex += 4;
                return BigInt(x);
            }
            if (h < 0xf8) {
                this.EnsureRead(5);
                this.Bytes[this.ReadIndex] = this.Bytes[this.ReadIndex] & 0x07;
                var bn = Long.FromUint8ArrayBigEndian(this.Bytes, this.ReadIndex, 5);
                this.ReadIndex += 5;
                return bn;
            }
            if (h < 0xfc) {
                this.EnsureRead(6);
                this.Bytes[this.ReadIndex] = this.Bytes[this.ReadIndex] & 0x03;
                var bn = Long.FromUint8ArrayBigEndian(this.Bytes, this.ReadIndex, 6);
                this.ReadIndex += 6;
                return bn;
            }
            if (h < 0xfe) {
                this.EnsureRead(7);
                this.Bytes[this.ReadIndex] = this.Bytes[this.ReadIndex] & 0x01;
                var bn = Long.FromUint8ArrayBigEndian(this.Bytes, this.ReadIndex, 7);
                this.ReadIndex += 7;
                return bn;
            }
            if (h < 0xff) {
                this.EnsureRead(8);
                var bn = Long.FromUint8ArrayBigEndian(this.Bytes, this.ReadIndex + 1, 7);
                this.ReadIndex += 8;
                return bn;
            }
            {
                this.EnsureRead(9);
                var bn = Long.FromUint8ArrayBigEndian(this.Bytes, this.ReadIndex + 1, 8);
                this.ReadIndex += 9;
                return bn;
            }
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
            this.WriteInt(length);
            this.EnsureWrite(length);
            ByteBuffer.BlockCopy(x, offset, this.Bytes, this.WriteIndex, length);
            this.WriteIndex += length;
        }
        ReadBytes() {
            var n = this.ReadInt();
            this.EnsureRead(n);
            var x = new Uint8Array(n);
            ByteBuffer.BlockCopy(this.Bytes, this.ReadIndex, x, 0, n);
            this.ReadIndex += n;
            return x;
        }
        SkipBytes() {
            var n = this.ReadInt();
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
        static SkipUnknownField(tagid, bb) {
            var tagType = tagid & ByteBuffer.TAG_MASK;
            switch (tagType) {
                case ByteBuffer.BOOL:
                    bb.ReadBool();
                    break;
                case ByteBuffer.BYTE:
                    bb.ReadByte();
                    break;
                case ByteBuffer.SHORT:
                    bb.ReadShort();
                    break;
                case ByteBuffer.INT:
                    bb.ReadInt();
                    break;
                case ByteBuffer.LONG:
                    bb.ReadLong();
                    break;
                case ByteBuffer.FLOAT:
                    bb.ReadFloat();
                    break;
                case ByteBuffer.DOUBLE:
                    bb.ReadDouble();
                    break;
                case ByteBuffer.STRING:
                case ByteBuffer.BYTES:
                case ByteBuffer.LIST:
                case ByteBuffer.SET:
                case ByteBuffer.MAP:
                case ByteBuffer.BEAN:
                    bb.SkipBytes();
                    break;
                case ByteBuffer.DYNAMIC:
                    bb.ReadLong8();
                    bb.SkipBytes();
                    break;
                default:
                    throw new Error("SkipUnknownField");
            }
        }
    }
    ByteBuffer.Encoder = new text_encoding_1.TextEncoder();
    ByteBuffer.Decoder = new text_encoding_1.TextDecoder();
    ByteBuffer.HEX = "0123456789ABCDEF";
    // ֻ�������µ����Ͷ��壬����ʱ�ǵ�ͬ�� SkipUnknownField
    ByteBuffer.INT = 0;
    ByteBuffer.LONG = 1;
    ByteBuffer.STRING = 2;
    ByteBuffer.BOOL = 3;
    ByteBuffer.BYTE = 4;
    ByteBuffer.SHORT = 5;
    ByteBuffer.FLOAT = 6;
    ByteBuffer.DOUBLE = 7;
    ByteBuffer.BYTES = 8;
    ByteBuffer.LIST = 9;
    ByteBuffer.SET = 10;
    ByteBuffer.MAP = 11;
    ByteBuffer.BEAN = 12;
    ByteBuffer.DYNAMIC = 13;
    ByteBuffer.TAG_MAX = 31;
    ByteBuffer.TAG_SHIFT = 5;
    ByteBuffer.TAG_MASK = (1 << ByteBuffer.TAG_SHIFT) - 1;
    ByteBuffer.ID_MASK = (1 << (31 - ByteBuffer.TAG_SHIFT)) - 1;
    Zeze.ByteBuffer = ByteBuffer;
})(Zeze = exports.Zeze || (exports.Zeze = {}));
//# sourceMappingURL=zeze.js.map