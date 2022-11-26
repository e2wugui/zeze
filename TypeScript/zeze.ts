
import { TextEncoder, TextDecoder } from "text-encoding"

var HostLang;
var IsUe: boolean = false;

try {
	HostLang = require('csharp'); // puerts unity
} catch (ex) {
	try {
		HostLang = require('ue'); // puerts unreal
		IsUe = true;
	}
	catch (ex) {
	}
}

export module Zeze {
	export class Long {
		public static readonly MAX_VALUE: bigint = 9223372036854775807n;
		public static readonly MIN_VALUE: bigint = -9223372036854775808n;

		public static Validate(x: bigint) {
			if (x < Long.MIN_VALUE || x > Long.MAX_VALUE)
				throw new Error("is not a valid long value");
		}

		public static ToUint8Array(bn: bigint, bytesCount: number = 8): Uint8Array {
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

		public static ToHex(bn: bigint, bytesCount: number = 8) {
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

		public static BitNot(bn: bigint): bigint {
			var bin = (bn).toString(2)
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

		public static FromUint8ArrayBigEndian(u8: Uint8Array, offset: number, len: number): bigint {
			var hex = [];
			var end = offset + len;
			var pos = true;
			if (len > 0 && (u8[offset] & 0x80)) {
				pos = false;
			}
			for (var i = offset; i < end; ++i) {
				var h = u8[i].toString(16);
				if (h.length % 2) { h = '0' + h; }
				hex.push(h);
			}

			var bn = BigInt('0x' + hex.join(''));
			//console.log(bn.toString(16));
			if (!pos) {
				bn = BigInt('0b' + bn.toString(2).split('').map(function (i) { return '0' === i ? 1 : 0 }).join('')) + BigInt(1);
				bn = -bn;
				//console.log(bn.toString(16));
			}
			return bn;
		}

		public static FromUint8ArrayLittleEndian(u8: Uint8Array, offset: number, len: number): bigint {
			var hex = [];
			var end = offset + len;
			var pos = true;
			if (len > 0 && (u8[end - 1] & 0x80)) {
				pos = false;
			}
			for (var i = end - 1; i >= offset; --i) {
				var h = u8[i].toString(16);
				if (h.length % 2) { h = '0' + h; }
				hex.push(h);
			}

			var bn = BigInt('0x' + hex.join(''));
			if (!pos) {
				bn = BigInt('0b' + bn.toString(2).split('').map(function (i) { return '0' === i ? 1 : 0 }).join('')) + BigInt(1);
				bn = -bn;
			}
			return bn;
		}
	}

	export interface Serializable {
		Encode(_os_: ByteBuffer);
		Decode(_os_: ByteBuffer);
	}

	export interface Bean extends Serializable {
		TypeId(): bigint;
	}

	export class EmptyBean implements Bean {
		public static readonly TYPEID: bigint = 0n;

		public TypeId(): bigint {
			return EmptyBean.TYPEID;
		}

		public Encode(bb: ByteBuffer) {
			bb.SkipUnknownField(ByteBuffer.BEAN);
		}

		public Decode(bb: ByteBuffer) {
			bb.WriteByte(0);
		}
	}

	export class DynamicBean implements Bean {
		public TypeId(): bigint {
			return this._TypeId;
		}

		public GetRealBean(): Bean {
			return this._Bean;
		}

		public SetRealBean(bean: Bean) {
			var typeId = this.GetSpecialTypeIdFromBean(bean);
			this._Bean = bean;
			this._TypeId = typeId;
		}

		private _TypeId: bigint;
		private _Bean: Bean;

		public GetSpecialTypeIdFromBean: (Bean) => bigint;
		public CreateBeanFromSpecialTypeId: (bigint) => Bean;

		public constructor(get: (Bean) => bigint, create: (bigint) => Bean) {
			this.GetSpecialTypeIdFromBean = get;
			this.CreateBeanFromSpecialTypeId = create;
			this._Bean = new EmptyBean();
			this._TypeId = EmptyBean.TYPEID;
		}

		public isEmpty(): boolean {
			return this._TypeId == EmptyBean.TYPEID && this._Bean instanceof EmptyBean;
		}

		public Encode(bb: ByteBuffer) {
			bb.WriteLong(this.TypeId());
			this._Bean.Encode(bb);
		}

		public Decode(bb: ByteBuffer) {
			var typeId = bb.ReadLong();
			var real = this.CreateBeanFromSpecialTypeId(typeId);
			if (real != null) {
				real.Decode(bb);
				this._Bean = real;
				this._TypeId = typeId;
			} else {
				bb.SkipUnknownField(ByteBuffer.BEAN);
				this._Bean = new EmptyBean();
				this._TypeId = EmptyBean.TYPEID;
			}
		}
	}

	export class FamilyClass {
		public static readonly Protocol: number = 2;
		public static readonly Request: number = 1;
		public static readonly Response: number = 2;
		public static readonly BitResultCode: number = 1 << 5;
		public static readonly FamilyClassMask: number = FamilyClass.BitResultCode - 1;

    }

	export abstract class Protocol implements Serializable {
		public FamilyClass: number = Zeze.FamilyClass.Protocol;
		public ResultCode: bigint; // int
		public Sender: Socket;

		public abstract ModuleId(): number;
		public abstract ProtocolId(): number;
		abstract Encode(_os_: ByteBuffer);
		abstract Decode(_os_: ByteBuffer);

		public TypeId(): bigint {
			return BigInt(this.ModuleId()) << 32n | BigInt(this.ProtocolId());
		}

		public EncodeProtocol(): Zeze.ByteBuffer {
			var bb = new Zeze.ByteBuffer();
			bb.WriteInt4(this.ModuleId());
			bb.WriteInt4(this.ProtocolId());
			var state = bb.BeginWriteWithSize4();
			this.Encode(bb);
			bb.EndWriteWithSize4(state);
			return bb;
		}

		public Send(socket: Socket) {
			var bb = this.EncodeProtocol();
			socket.Send(bb);
		}

		public Dispatch(service: Service, factoryHandle: ProtocolFactoryHandle) {
			service.DispatchProtocol(this, factoryHandle);
		}

		public static DecodeProtocol(service: Service, singleEncodedProtocol: ByteBuffer): Protocol {
			var moduleId: number = singleEncodedProtocol.ReadInt4();
			var protocolId: number = singleEncodedProtocol.ReadInt4();
			var size: number = singleEncodedProtocol.ReadInt4();
			var type: bigint = BigInt(moduleId) << 32n | BigInt(protocolId);
			var factoryHandle = service.FactoryHandleMap.get(type);
			if (factoryHandle != null) {
				var p = factoryHandle.factory();
				p.Decode(singleEncodedProtocol);
				return p;
			}
			return null;
		}

		public static DecodeProtocols(service: Service, socket: Socket, input: Zeze.ByteBuffer) {
			var os = new Zeze.ByteBuffer(input.Bytes, input.ReadIndex, input.Size());
			while (os.Size() > 0) {
				var moduleId: number;
				var protocolId: number;
				var size: number;
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
				var type: bigint = BigInt(moduleId) << 32n | BigInt(protocolId);
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

	export abstract class ProtocolWithArgument<ArgumentType extends Bean> extends Protocol {
		public Argument: ArgumentType; // need init in subclass(real protocol)

		public constructor(argument: ArgumentType) {
			super();
			this.Argument = argument;
		}

		public Encode(_os_: ByteBuffer) {
			var compress = this.FamilyClass;
			if (this.ResultCode != 0n)
				compress |= Zeze.FamilyClass.BitResultCode;
			_os_.WriteInt(compress);
			if (this.ResultCode != 0n)
				_os_.WriteLong(this.ResultCode);
			this.Argument.Encode(_os_);
		}

		public Decode(_os_: ByteBuffer) {
			var compress = _os_.ReadInt();
			this.FamilyClass = compress & Zeze.FamilyClass.FamilyClassMask;
			this.ResultCode = ((compress & Zeze.FamilyClass.BitResultCode) != 0) ? _os_.ReadLong() : 0n;
			this.Argument.Decode(_os_);
		}
	}

	export type FunctionProtocolFactory = () => Zeze.Protocol;
	export type FunctionProtocolHandle = (p: Zeze.Protocol) => number;

	export abstract class Rpc<TArgument extends Zeze.Bean, TResult extends Zeze.Bean> extends Zeze.ProtocolWithArgument<TArgument> {
		public Result: TResult;
		public ResponseHandle: FunctionProtocolHandle;
		public IsTimeout: boolean = false;

		private IsRequest: boolean;
		private sid: bigint;
		private timeout: any;

		public constructor(argument: TArgument, result: TResult) {
			super(argument);
			this.Result = result;
		}

		public Send(socket: Socket) {
			throw new Error("Rpc Need Use SendWithCallback");
		}

		public SendWithCallback(socket: Socket, responseHandle: FunctionProtocolHandle, timeoutMs: number = 5000) {
			this.Sender = socket;
			this.ResponseHandle = responseHandle;
			this.IsRequest = true;
			this.sid = socket.service.AddRpcContext(this);

			this.timeout = setTimeout(() => {
				var context = <Rpc<TArgument, TResult>>this.Sender.service.RemoveRpcContext(this.sid);
				if (context && context.ResponseHandle) {
					context.IsTimeout = true;
					context.ResponseHandle(context);
				}
			}, timeoutMs);

			super.Send(socket);
		}

		public async SendForWait(socket: Socket, timeoutMs: number = 5000): Promise<void> {
			return new Promise<void>((resolve, reject) => {
				this.SendWithCallback(socket, (response) => {
					var res = <Rpc<TArgument, TResult>>response;
					if (res.IsTimeout)
						reject("Rpc.SendForWait Timeout");
					else
						resolve();
					return 0;
				}, timeoutMs);
			});
		}

		public SendResult() {
			this.IsRequest = false;
			super.Send(this.Sender);
		}

		public SendResultCode(code: bigint) {
			this.ResultCode = code;
			this.SendResult();
		}

		public Dispatch(service: Service, factoryHandle: ProtocolFactoryHandle) {
			if (this.IsRequest) {
				service.DispatchProtocol(this, factoryHandle);
				return;
			}
			var context = <Rpc<TArgument, TResult>>service.RemoveRpcContext(this.sid);
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

		Decode(bb: Zeze.ByteBuffer) {
			var compress = bb.ReadInt();
			this.FamilyClass = compress & Zeze.FamilyClass.FamilyClassMask;
			this.IsRequest = this.FamilyClass == Zeze.FamilyClass.Request;
			this.ResultCode = ((compress & Zeze.FamilyClass.BitResultCode) != 0) ? bb.ReadLong() : 0n;
			this.sid = bb.ReadLong();
			if (this.IsRequest)
				this.Argument.Decode(bb);
			else
				this.Result.Decode(bb);
		}

		Encode(bb: Zeze.ByteBuffer) {
			// skip value of this.FamilyClass
			var compress = this.IsRequest ? Zeze.FamilyClass.Request : Zeze.FamilyClass.Response;
			if (this.ResultCode != 0n)
				compress |= Zeze.FamilyClass.BitResultCode;
			bb.WriteInt(compress);
			if (this.ResultCode != 0n)
				bb.WriteLong(this.ResultCode);
			bb.WriteLong(this.sid);
			if (this.IsRequest)
				this.Argument.Encode(bb);
			else
				this.Result.Encode(bb);
		}
	}

	export class ProtocolFactoryHandle {
		public factory: FunctionProtocolFactory;
		public handle: FunctionProtocolHandle;

		public constructor(f: FunctionProtocolFactory, h: FunctionProtocolHandle) {
			this.factory = f;
			this.handle = h;
		}
	}

	export class Socket {
		public service: Service;
		public SessionId: bigint;
		public InputBuffer: Zeze.ByteBuffer;

		public constructor(service: Service, sessionId: bigint) {
			this.service = service;
			this.SessionId = sessionId;
		}

		public Send(buffer: Zeze.ByteBuffer) {
			this.service.Send(this.SessionId, buffer)
		}

		public Close() {
			this.service.Close(this.SessionId);
		}

		public OnProcessInput(newInput: ArrayBuffer, offset: number, len: number) {
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

	export interface IServiceEventHandle {
		OnSocketConnected(service: Service, socket: Socket);
		OnSocketClosed(service: Service, socket: Socket);
		OnSoekctInput(service: Service, socket: Socket, buffer: ArrayBuffer, offset: number, len: number): boolean; // true 已经处理了，false 进行默认处理
	}

	export class ProtocolHead {
		public moduleId: number;
		public protocolId: number;
	}

	export class Service {
		public FactoryHandleMap: Map<bigint, Zeze.ProtocolFactoryHandle> = new Map<bigint, Zeze.ProtocolFactoryHandle>();

		private serialId: bigint = 0n;
		private contexts: Map<bigint, Zeze.Protocol> = new Map<bigint, Zeze.Protocol>();

		public AddRpcContext(rpc: Zeze.Protocol): bigint {
			this.serialId = this.serialId + 1n;
			this.contexts.set(this.serialId, rpc);
			return this.serialId;
		}

		public RemoveRpcContext(sid: bigint): Zeze.Protocol {
			var ctx= this.contexts.get(sid);
			if (ctx) {
				this.contexts.delete(sid);
				return ctx;
			}
			return null;
		}

		public DispatchUnknownProtocol(socket: Socket, type: bigint, buffer: Zeze.ByteBuffer) {
		}

		public DispatchProtocol(p: Zeze.Protocol, factoryHandle: ProtocolFactoryHandle) {
			factoryHandle.handle(p);
		}

		public Connection: Socket;
		public ServiceEventHandle: IServiceEventHandle;

		protected CallbackOnSocketHandshakeDone(sessionId: bigint) {
			if (this.Connection)
				this.Connection.Close();
			this.Connection = new Socket(this, sessionId);
			if (this.ServiceEventHandle)
				this.ServiceEventHandle.OnSocketConnected(this, this.Connection);
		}

		protected CallbackOnSocketClose(sessionId: bigint) {
			if (this.Connection && this.Connection.SessionId == sessionId) {
				if (this.ServiceEventHandle)
					this.ServiceEventHandle.OnSocketClosed(this, this.Connection);
				this.Connection = null;
			}
		}

		protected CallbackOnSocketProcessInputBuffer(sessionId: bigint, buffer: ArrayBuffer, offset: number, len: number) {
			if (this.Connection.SessionId == sessionId) {
				if (this.ServiceEventHandle) {
					if (this.ServiceEventHandle.OnSoekctInput(this, this.Connection, buffer, offset, len))
						return;
				}
				this.Connection.OnProcessInput(buffer, offset, len);
			}
		}

		private Implement;

		public constructor(name: string) {
			if (IsUe) {
				this.Implement = new HostLang.ToTypeScriptService();
			} else {
				this.Implement = new HostLang.ToTypeScriptService(name);
			}
			this.Implement.CallbackWhenSocketHandshakeDone = this.CallbackOnSocketHandshakeDone.bind(this);
			this.Implement.CallbackWhenSocketClose = this.CallbackOnSocketClose.bind(this);
			this.Implement.CallbackWhenSocketProcessInputBuffer = this.CallbackOnSocketProcessInputBuffer.bind(this);
		}

		public Connect(hostNameOrAddress: string, port: number, autoReconnect: boolean = true) {
			this.Implement.Connect(hostNameOrAddress, port, autoReconnect);
		}

		public Send(sessionId: bigint, buffer: Zeze.ByteBuffer) {
			this.Implement.Send(sessionId, buffer.Bytes.buffer, buffer.ReadIndex, buffer.Size());
		}

		public Close(sessionId: bigint) {
			this.Implement.Close(sessionId);
		}

		public TickUpdate() {
			this.Implement.TickUpdate();
		}
	}

	export class ByteBuffer {
		public Bytes: Uint8Array;
		public ReadIndex: number;
		public WriteIndex: number;
		View: DataView;

		public Capacity(): number {
			return this.Bytes.byteLength;
		}

		public Size(): number {
			return this.WriteIndex - this.ReadIndex;
		}

		public constructor(buffer: Uint8Array = null, readIndex: number = 0, length: number = 0) {
			this.Bytes = (null == buffer) ? new Uint8Array(1024) : buffer;
			this.View = new DataView(this.Bytes.buffer);
			this.ReadIndex = readIndex;
			this.WriteIndex = this.ReadIndex + length;
		}

		ToPower2(needSize: number) {
			var size = 1024;
			while (size < needSize)
				size <<= 1;
			return size;
		}

		public static BlockCopy(src: Uint8Array, srcOffset: number, dst: Uint8Array, dstOffset: number, count: number) {
			for (var i = 0; i < count; ++i) {
				dst[i + dstOffset] = src[i + srcOffset];
			}
		}

		public Copy(): Uint8Array {
			var copy = new Uint8Array(this.Size());
			ByteBuffer.BlockCopy(this.Bytes, this.ReadIndex, copy, 0, this.Size());
			return copy;
		}

		public EnsureWrite(size: number) {
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

		public Append(bytes: Uint8Array, offset: number, len: number) {
			this.EnsureWrite(len);
			ByteBuffer.BlockCopy(bytes, offset, this.Bytes, this.WriteIndex, len);
			this.WriteIndex += len;
		}

		public Replace(writeIndex: number, src: Uint8Array, srcOffset: number, len: number) {
			if (writeIndex < this.ReadIndex || writeIndex + len > this.WriteIndex)
				throw new Error();
			ByteBuffer.BlockCopy(src, srcOffset, this.Bytes, writeIndex, len);
		}

		public BeginWriteWithSize4(): number {
			var state = this.Size();
			this.EnsureWrite(4);
			this.WriteIndex += 4;
			return state;
		}

		public EndWriteWithSize4(state: number) {
			var oldWriteIndex = state + this.ReadIndex;
			this.View.setInt32(oldWriteIndex, this.WriteIndex - oldWriteIndex - 4, true);
		}

		public EnsureRead(size: number) {
			if (this.ReadIndex + size > this.WriteIndex)
				throw new Error("EnsureRead " + size);
		}

		public Campact() {
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

		public Reset() {
			this.ReadIndex = 0;
			this.WriteIndex = 0;
		}

		public WriteBool(b: boolean) {
			this.EnsureWrite(1);
			this.Bytes[this.WriteIndex++] = b ? 1 : 0;
		}

		public ReadBool(): boolean {
			return this.ReadLong() != 0n;
		}

		public WriteByte(byte: number) {
			this.EnsureWrite(1);
			this.Bytes[this.WriteIndex++] = byte;
		}

		public ReadByte(): number {
			this.EnsureRead(1);
			return this.Bytes[this.ReadIndex++];
		}

		public WriteInt4(x: number) {
			this.EnsureWrite(4);
			this.View.setInt32(this.WriteIndex, x, true);
			this.WriteIndex += 4;
		}

		public ReadInt4(): number {
			this.EnsureRead(4);
			var x = this.View.getInt32(this.ReadIndex, true);
			this.ReadIndex += 4;
			return x;
		}

		public WriteLong8(x: bigint) {
			this.EnsureWrite(8);
			var u8 = Long.ToUint8Array(x, 8);
			for (var i = u8.length - 1, j = this.WriteIndex; i >= 0; --i, ++j)
				this.Bytes[j] = u8[i];
			this.WriteIndex += 8;
		}

		public ReadLong8(): bigint {
			this.EnsureRead(8);
			var x = Long.FromUint8ArrayLittleEndian(this.Bytes, this.ReadIndex, 8);
			this.ReadIndex += 8;
			return x;
		}

		public WriteUInt(u: number) {
			if (u >= 0) {
				if (u < 0x80) {
					this.EnsureWrite(1); // 0xxx xxxx
					this.Bytes[this.WriteIndex++] = (u > 0 ? u : 0);
					return;
				} else if (u < 0x4000) {
					this.EnsureWrite(2); // 10xx xxxx +1B
					var bytes = this.Bytes;
					var writeIndex = this.WriteIndex;
					bytes[writeIndex] = (u >> 8) + 0x80;
					bytes[writeIndex + 1] = u;
					this.WriteIndex = writeIndex + 2;
					return;
				} else if (u < 0x20_0000) {
					this.EnsureWrite(3); // 110x xxxx +2B
					var bytes = this.Bytes;
					var writeIndex = this.WriteIndex;
					bytes[writeIndex] = (u >> 16) + 0xc0;
					bytes[writeIndex + 1] = u >> 8;
					bytes[writeIndex + 2] = u;
					this.WriteIndex = writeIndex + 3;
					return;
				} else if (u < 0x1000_0000) {
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

		public ReadUInt(): number {
			this.EnsureRead(1);
			var bytes = this.Bytes;
			var readIndex = this.ReadIndex;
			var x = bytes[readIndex];
			if (x < 0x80) {
				this.ReadIndex = readIndex + 1;
			} else if (x < 0xc0) {
				this.EnsureRead(2);
				x = ((x & 0x3f) << 8)
					+ bytes[readIndex + 1];
				this.ReadIndex = readIndex + 2;
			} else if (x < 0xe0) {
				this.EnsureRead(3);
				x = ((x & 0x1f) << 16)
					+ (bytes[readIndex + 1] << 8)
					+ bytes[readIndex + 2];
				this.ReadIndex = readIndex + 3;
			} else if (x < 0xf0) {
				this.EnsureRead(4);
				x = ((x & 0xf) << 24)
					+ (bytes[readIndex + 1] << 16)
					+ (bytes[readIndex + 2] << 8)
					+ bytes[readIndex + 3];
				this.ReadIndex = readIndex + 4;
			} else {
				this.EnsureRead(5);
				x = (bytes[readIndex + 1] << 24)
					+ (bytes[readIndex + 2] << 16)
					+ (bytes[readIndex + 3] << 8)
					+ bytes[readIndex + 4];
				this.ReadIndex = readIndex + 5;
			}
			return x;
		}

		public WriteLong(x: bigint) {
			if (x >= 0) {
				if (x < 0x40) {
					this.EnsureWrite(1); // 00xx xxxx
					this.Bytes[this.WriteIndex++] = Number(x);
				} else if (x < 0x2000) {
					this.EnsureWrite(2); // 010x xxxx +1B
					var bytes = this.Bytes;
					var writeIndex = this.WriteIndex;
					var v = Number(x)
					bytes[writeIndex] = (v >> 8) + 0x40;
					bytes[writeIndex + 1] = v;
					this.WriteIndex = writeIndex + 2;
				} else if (x < 0x10_0000) {
					this.EnsureWrite(3); // 0110 xxxx +2B
					var bytes = this.Bytes;
					var writeIndex = this.WriteIndex;
					var v = Number(x)
					bytes[writeIndex] = (v >> 16) + 0x60;
					bytes[writeIndex + 1] = v >> 8;
					bytes[writeIndex + 2] = v;
					this.WriteIndex = writeIndex + 3;
				} else if (x < 0x800_0000) {
					this.EnsureWrite(4); // 0111 0xxx +3B
					var bytes = this.Bytes;
					var writeIndex = this.WriteIndex;
					var v = Number(x)
					bytes[writeIndex] = (v >> 24) + 0x70;
					bytes[writeIndex + 1] = v >> 16;
					bytes[writeIndex + 2] = v >> 8;
					bytes[writeIndex + 3] = v;
					this.WriteIndex = writeIndex + 4;
				} else if (x < 0x4_0000_0000n) {
					this.EnsureWrite(5); // 0111 10xx +4B
					var u8 = Long.ToUint8Array(x, 5);
					this.Bytes[this.WriteIndex] = u8[0] + 0x78;
					ByteBuffer.BlockCopy(u8, 1, this.Bytes, this.WriteIndex + 1, 4);
					this.WriteIndex += 5;
				} else if (x < 0x200_0000_0000n) {
					this.EnsureWrite(6); // 0111 110x +5B
					var u8 = Long.ToUint8Array(x, 6);
					this.Bytes[this.WriteIndex] = u8[0] + 0x7c;
					ByteBuffer.BlockCopy(u8, 1, this.Bytes, this.WriteIndex + 1, 5);
					this.WriteIndex += 6;
				} else if (x < 0x1_0000_0000_0000n) {
					this.EnsureWrite(7); // 0111 1110 +6B
					var u8 = Long.ToUint8Array(x, 7);
					this.Bytes[this.WriteIndex] = u8[0] + 0x7e;
					ByteBuffer.BlockCopy(u8, 1, this.Bytes, this.WriteIndex + 1, 6);
					this.WriteIndex += 7;
				} else if (x < 0x80_0000_0000_0000n) {
					this.EnsureWrite(8); // 0111 1111 0 +7B
					var u8 = Long.ToUint8Array(x, 8);
					this.Bytes[this.WriteIndex] = u8[0] + 0x7f;
					ByteBuffer.BlockCopy(u8, 1, this.Bytes, this.WriteIndex + 1, 7);
					this.WriteIndex += 8;
				} else {
					this.EnsureWrite(9); // 0111 1111 1 +8B
					var u8 = Long.ToUint8Array(x, 8);
					this.Bytes[this.WriteIndex] = 0x7f;
					this.Bytes[this.WriteIndex + 1] = u8[0] + 0x80;
					ByteBuffer.BlockCopy(u8, 1, this.Bytes, this.WriteIndex + 2, 7);
					this.WriteIndex += 9;
				}
			} else {
				if (x >= -0x40) {
					this.EnsureWrite(1); // 11xx xxxx
					this.Bytes[this.WriteIndex++] = Number(x);
				} else if (x >= -0x2000) {
					this.EnsureWrite(2); // 101x xxxx +1B
					var bytes = this.Bytes;
					var writeIndex = this.WriteIndex;
					var v = Number(x)
					bytes[writeIndex] = (v >> 8) - 0x40;
					bytes[writeIndex + 1] = v;
					this.WriteIndex = writeIndex + 2;
				} else if (x >= -0x10_0000) {
					this.EnsureWrite(3); // 1001 xxxx +2B
					var bytes = this.Bytes;
					var writeIndex = this.WriteIndex;
					var v = Number(x)
					bytes[writeIndex] = (v >> 16) - 0x60;
					bytes[writeIndex + 1] = v >> 8;
					bytes[writeIndex + 2] = v;
					this.WriteIndex = writeIndex + 3;
				} else if (x >= -0x800_0000) {
					this.EnsureWrite(4); // 1000 1xxx +3B
					var bytes = this.Bytes;
					var writeIndex = this.WriteIndex;
					var v = Number(x)
					bytes[writeIndex] = (v >> 24) - 0x70;
					bytes[writeIndex + 1] = v >> 16;
					bytes[writeIndex + 2] = v >> 8;
					bytes[writeIndex + 3] = v;
					this.WriteIndex = writeIndex + 4;
				} else if (x >= -0x4_0000_0000n) {
					this.EnsureWrite(5); // 1000 01xx +4B
					var u8 = Long.ToUint8Array(x, 8);
					this.Bytes[this.WriteIndex] = u8[3] - 0x78;
					ByteBuffer.BlockCopy(u8, 4, this.Bytes, this.WriteIndex + 1, 4);
					this.WriteIndex += 5;
				} else if (x >= -0x200_0000_0000n) {
					this.EnsureWrite(6); // 1000 001x +5B
					var u8 = Long.ToUint8Array(x, 8);
					this.Bytes[this.WriteIndex] = u8[2] - 0x7c;
					ByteBuffer.BlockCopy(u8, 3, this.Bytes, this.WriteIndex + 1, 5);
					this.WriteIndex += 6;
				} else if (x >= -0x1_0000_0000_0000n) {
					this.EnsureWrite(7); // 1000 0001 +6B
					var u8 = Long.ToUint8Array(x, 8);
					this.Bytes[this.WriteIndex] = 0x81;
					ByteBuffer.BlockCopy(u8, 2, this.Bytes, this.WriteIndex + 1, 6);
					this.WriteIndex += 7;
				} else if (x >= -0x80_0000_0000_0000n) {
					this.EnsureWrite(8); // 1000 0000 1 +7B
					var u8 = Long.ToUint8Array(x, 8);
					this.Bytes[this.WriteIndex] = 0x80;
					ByteBuffer.BlockCopy(u8, 1, this.Bytes, this.WriteIndex + 1, 7);
					this.WriteIndex += 8;
				} else {
					this.EnsureWrite(9); // 1000 0000 0 +8B
					var u8 = Long.ToUint8Array(x, 8);
					this.Bytes[this.WriteIndex] = 0x80;
					this.Bytes[this.WriteIndex + 1] = u8[0] - 0x80;
					ByteBuffer.BlockCopy(u8, 1, this.Bytes, this.WriteIndex + 2, 7);
					this.WriteIndex += 9;
				}
			}
		}

		public ReadLong2BE(): number {
			this.EnsureRead(2);
			var bytes = this.Bytes;
			var readIndex = this.ReadIndex;
			this.ReadIndex = readIndex + 2;
			return (bytes[readIndex] << 8) +
				bytes[readIndex + 1];
		}

		public ReadLong3BE(): number {
			this.EnsureRead(3);
			var bytes = this.Bytes;
			var readIndex = this.ReadIndex;
			this.ReadIndex = readIndex + 3;
			return (bytes[readIndex] << 16) +
				(bytes[readIndex + 1] << 8) +
				bytes[readIndex + 2];
		}

		public ReadLong4BE(): bigint {
			this.EnsureRead(4);
			var readIndex = this.ReadIndex;
			this.ReadIndex = readIndex + 4;
			return Long.FromUint8ArrayBigEndian(this.Bytes, readIndex, 4) & 0xffff_ffffn;
		}

		public ReadLong5BE(): bigint {
			this.EnsureRead(5);
			var readIndex = this.ReadIndex;
			this.ReadIndex = readIndex + 5;
			return Long.FromUint8ArrayBigEndian(this.Bytes, readIndex, 5) & 0xff_ffff_ffffn;
		}

		public ReadLong6BE(): bigint {
			this.EnsureRead(6);
			var readIndex = this.ReadIndex;
			this.ReadIndex = readIndex + 6;
			return Long.FromUint8ArrayBigEndian(this.Bytes, readIndex, 6) & 0xffff_ffff_ffffn;
		}

		public ReadLong7BE(): bigint {
			this.EnsureRead(7);
			var readIndex = this.ReadIndex;
			this.ReadIndex = readIndex + 7;
			return Long.FromUint8ArrayBigEndian(this.Bytes, readIndex, 7) & 0xff_ffff_ffff_ffffn;
		}

		public ReadLong(): bigint {
			this.EnsureRead(1);
			var b = this.Bytes[this.ReadIndex++];
			b = b < 0x80 ? b : b - 0x100;
			switch ((b >> 3) & 0x1f) {
			case 0x00: case 0x01: case 0x02: case 0x03: case 0x04: case 0x05: case 0x06: case 0x07:
			case 0x18: case 0x19: case 0x1a: case 0x1b: case 0x1c: case 0x1d: case 0x1e: case 0x1f: return BigInt(b);
			case 0x08: case 0x09: case 0x0a: case 0x0b: return BigInt(((b - 0x40) << 8) + this.ReadByte());
			case 0x14: case 0x15: case 0x16: case 0x17: return BigInt(((b + 0x40) << 8) + this.ReadByte());
			case 0x0c: case 0x0d: return BigInt(((b - 0x60) << 16) + this.ReadLong2BE());
			case 0x12: case 0x13: return BigInt(((b + 0x60) << 16) + this.ReadLong2BE());
			case 0x0e: return BigInt(((b - 0x70) << 24) + this.ReadLong3BE());
			case 0x11: return BigInt(((b + 0x70) << 24) + this.ReadLong3BE());
			case 0x0f:
				switch (b & 7) {
					case 0: case 1: case 2: case 3: return (BigInt(b - 0x78) << 32n) + this.ReadLong4BE();
					case 4: case 5: return (BigInt(b - 0x7c) << 40n) + this.ReadLong5BE();
					case 6: return this.ReadLong6BE();
					default: var r = this.ReadLong7BE(); return r < 0x80_0000_0000_0000n ?
						r : ((r - 0x80_0000_0000_0000n) << 8n) + BigInt(this.ReadByte());
				}
			default: // 0x10
				switch (b & 7) {
					case 4: case 5: case 6: case 7: return (BigInt(b + 0x78) << 32n) + this.ReadLong4BE();
					case 2: case 3: return (BigInt(b + 0x7c) << 40n) + this.ReadLong5BE();
					case 1: return -0x1_0000_0000_0000n + this.ReadLong6BE();
					default: var r = this.ReadLong7BE(); r = r >= 0x80_0000_0000_0000n ?
						-0x100_0000_0000_0000n + r : ((r + 0x80_0000_0000_0000n) << 8n) + BigInt(this.ReadByte());
						return r < 0x8000_0000_0000_0000n ? r : r - 0x1_0000_0000_0000_0000n; // special fix
				}
			}
		}

		public WriteInt(x: number) {
			this.WriteLong(BigInt(x));
		}

		public ReadInt(): number {
			return Number(this.ReadLong());
		}

		public WriteFloat(x: number) {
			this.EnsureWrite(4);
			this.View.setFloat32(this.WriteIndex, x, true);
			this.WriteIndex += 4;
		}

		public ReadFloat(): number {
			this.EnsureRead(4);
			var x = this.View.getFloat32(this.ReadIndex, true);
			this.ReadIndex += 4;
			return x;
		}

		public WriteDouble(x: number) {
			this.EnsureWrite(8);
			this.View.setFloat64(this.WriteIndex, x, true);
			this.WriteIndex += 8;
		}

		public ReadDouble(): number {
			this.EnsureRead(8);
			var x = this.View.getFloat64(this.ReadIndex, true);
			this.ReadIndex += 8;
			return x;
		}

		static Encoder: TextEncoder = new TextEncoder();
		static Decoder: TextDecoder = new TextDecoder();

		public WriteString(x: string) {
			var utf8 = ByteBuffer.Encoder.encode(x);
			this.WriteBytes(utf8, 0, utf8.length);
		}

		public ReadString(): string {
			return ByteBuffer.Decoder.decode(this.ReadBytes());
		}

		public WriteBytes(x: Uint8Array, offset: number = 0, length: number = -1) {
			if (length == -1)
				length = x.byteLength;
			this.WriteUInt(length);
			this.EnsureWrite(length);
			ByteBuffer.BlockCopy(x, offset, this.Bytes, this.WriteIndex, length);
			this.WriteIndex += length;
		}

		public ReadBytes(): Uint8Array {
			var n = this.ReadUInt();
			this.EnsureRead(n);
			var x = new Uint8Array(n);
			ByteBuffer.BlockCopy(this.Bytes, this.ReadIndex, x, 0, n);
			this.ReadIndex += n;
			return x;
		}

		public SkipBytes() {
			var n = this.ReadUInt();
			this.EnsureRead(n);
			this.ReadIndex += n;
		}

		public Equals(other: ByteBuffer): boolean {
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

		static HEX = "0123456789ABCDEF";
		public static toHex(x: number): string {
			var l = x & 0x0f;
			var h = (x >> 4) & 0x0f;
			return this.HEX[h] + this.HEX[l];
		}

		public static toString(x: Uint8Array, from: number = 0, to: number = -1): string {
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
		public toString(): string {
			return ByteBuffer.toString(this.Bytes, this.ReadIndex, this.WriteIndex);
		}

		// 只能增加新的类型定义，增加时记得同步 SkipUnknownField
		public static readonly INTEGER = 0; // byte,short,int,long,bool
		public static readonly FLOAT = 1; // float
		public static readonly DOUBLE = 2; // double
		public static readonly BYTES = 3; // binary,string
		public static readonly LIST = 4; // list,set
		public static readonly MAP = 5; // map
		public static readonly BEAN = 6; // bean
		public static readonly DYNAMIC = 7; // dynamic

		public static readonly TAG_SHIFT = 4;
		public static readonly TAG_MASK = (1 << ByteBuffer.TAG_SHIFT) - 1;
		public static readonly ID_MASK = 0xff - ByteBuffer.TAG_MASK;

		public WriteTag(lastVarId: number, varId: number, type: number): number {
			var deltaId = varId - lastVarId;
			if (deltaId < 0xf)
				this.WriteByte((deltaId << ByteBuffer.TAG_SHIFT) + type);
			else {
				this.WriteByte(0xf0 + type);
				this.WriteUInt(deltaId - 0xf);
			}
			return varId;
		}

		public WriteListType(listSize: number, elemType: number) {
			if (listSize < 0xf)
				this.WriteByte((listSize << ByteBuffer.TAG_SHIFT) + elemType);
			else {
				this.WriteByte(0xf0 + elemType);
				this.WriteUInt(listSize - 0xf);
			}
		}

		public WriteMapType(mapSize: number, keyType: number, valueType: number) {
			this.WriteByte((keyType << ByteBuffer.TAG_SHIFT) + valueType);
			this.WriteUInt(mapSize);
		}

		public ReadTagSize(tagByte: number): number {
			var deltaId = (tagByte & ByteBuffer.ID_MASK) >> ByteBuffer.TAG_SHIFT;
			return deltaId < 0xf ? deltaId : 0xf + this.ReadUInt();
		}

		public ReadBoolT(type: number): boolean {
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

		public ReadIntT(type: number): number {
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

		public ReadLongT(type: number): bigint {
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

		public ReadFloatT(type: number): number {
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

		public ReadDoubleT(type: number): number {
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

		public ReadBytesT(type: number): Uint8Array {
			type &= ByteBuffer.TAG_MASK;
			if (type == ByteBuffer.BYTES)
				return this.ReadBytes();
			this.SkipUnknownField(type);
			return new Uint8Array(0);
		}

		public ReadStringT(type: number): string {
			type &= ByteBuffer.TAG_MASK;
			if (type == ByteBuffer.BYTES)
				return this.ReadString();
			this.SkipUnknownField(type);
			return "";
		}

		public ReadBean<T extends Serializable>(bean: T, type: number): T {
			type &= ByteBuffer.TAG_MASK;
			if (type == ByteBuffer.BEAN)
				bean.Decode(this);
			else if (type == ByteBuffer.DYNAMIC) {
				this.ReadLong();
				bean.Decode(this);
			} else
				this.SkipUnknownField(type);
			return bean;
		}

		public ReadDynamic(dynBean: DynamicBean, type: number): DynamicBean {
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

		public SkipUnknownList(type: number, count: number) {
			while (--count >= 0)
				this.SkipUnknownField(type);
		}

		public SkipUnknownMap(type1: number, type2: number, count: number) {
			while (--count >= 0) {
				this.SkipUnknownField(type1);
				this.SkipUnknownField(type2);
			}
		}

		public SkipUnknownField(type: number) {
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
}
