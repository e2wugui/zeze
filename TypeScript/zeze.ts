
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

		public static Validate(x: bigint): void {
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
		Encode(_os_: ByteBuffer): void;
		Decode(_os_: ByteBuffer): void;
	}

	export interface Bean extends Serializable {
		TypeId(): bigint;
    }

	export class EmptyBean implements Bean {
		public static readonly TYPEID: bigint = 0n;

		public TypeId(): bigint {
			return EmptyBean.TYPEID;
		}

		public Encode(_os_: ByteBuffer): void {
			_os_.WriteInt(0);
		}

		public Decode(_os_: ByteBuffer): void {
			_os_.ReadInt();
        }
	}

	export abstract class Protocol implements Serializable {
		public ResultCode: number; // int
		public Sender: Socket;

		public abstract ModuleId(): number;
		public abstract ProtocolId(): number;
		abstract Encode(_os_: ByteBuffer): void;
		abstract Decode(_os_: ByteBuffer): void;

		public TypeId(): number {
			return this.ModuleId() << 16 | this.ProtocolId();
		}

		public EncodeProtocol(): Zeze.ByteBuffer {
			var bb = new Zeze.ByteBuffer();
			bb.WriteInt4(this.TypeId());
			var state = bb.BeginWriteWithSize4();
			this.Encode(bb);
			bb.EndWriteWithSize4(state);
			return bb;
		}

		public Send(socket: Socket): void {
			var bb = this.EncodeProtocol();
			socket.Send(bb);
		}

		public Dispatch(service: Service, factoryHandle: ProtocolFactoryHandle): void {
			service.DispatchProtocol(this, factoryHandle);
		}

		public static DecodeProtocols(service: Service, socket: Socket, input: Zeze.ByteBuffer): void {
			var os = new Zeze.ByteBuffer(input.Bytes, input.ReadIndex, input.Size());
			while (os.Size() > 0) {
				var type: number;
				var size: number;
				var readIndexSaved = os.ReadIndex;

				if (os.Size() >= 8) // protocl header size.
				{
					type = os.ReadInt4();
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

		public Encode(_os_: ByteBuffer): void {
			_os_.WriteInt(this.ResultCode);
			this.Argument.Encode(_os_);
		}

		public Decode(_os_: ByteBuffer): void {
			this.ResultCode = _os_.ReadInt();
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

		public Send(socket: Socket): void {
			throw new Error("Rpc Need Use SendWithCallback");
        }

		public SendWithCallback(socket: Socket, responseHandle: FunctionProtocolHandle, timeoutMs: number = 5000): void {
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

		public SendResult(): void {
			this.IsRequest = false;
			super.Send(this.Sender);
		}

		public SendResultCode(code: number): void {
			this.ResultCode = code;
			this.SendResult();
		}

		public Dispatch(service: Service, factoryHandle: ProtocolFactoryHandle): void {
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

		Decode(bb: Zeze.ByteBuffer): void {
			this.IsRequest = bb.ReadBool();
			this.sid = bb.ReadLong();
			this.ResultCode = bb.ReadInt();
			if (this.IsRequest) {
				this.Argument.Decode(bb);
			} else {
				this.Result.Decode(bb);
			}
		}

		Encode(bb: Zeze.ByteBuffer): void {
			bb.WriteBool(this.IsRequest);
			bb.WriteLong(this.sid);
			bb.WriteInt(this.ResultCode);
			if (this.IsRequest) {
				this.Argument.Encode(bb);
			} else {
				this.Result.Encode(bb);
			}
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

		public Send(buffer: Zeze.ByteBuffer): void {
			this.service.Send(this.SessionId, buffer)
		}

		public Close(): void {
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
		OnSocketConnected(service: Service, socket: Socket): void;
		OnSocketClosed(service: Service, socket: Socket): void;
		OnSoekctInput(service: Service, socket: Socket, buffer: ArrayBuffer, offset: number, len: number): boolean; // true 已经处理了，false 进行默认处理
    }

	export class Service {
		public FactoryHandleMap: Map<number, Zeze.ProtocolFactoryHandle> = new Map<number, Zeze.ProtocolFactoryHandle>();

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

		public DispatchUnknownProtocol(socket: Socket, type: number, buffer: Zeze.ByteBuffer): void {
		}

		public DispatchProtocol(p: Zeze.Protocol, factoryHandle: ProtocolFactoryHandle): void {
			factoryHandle.handle(p);
        }

		public Connection: Socket;
		public ServiceEventHandle: IServiceEventHandle;

		protected CallbackOnSocketHandshakeDone(sessionId: bigint): void {
			if (this.Connection)
				this.Connection.Close();
			this.Connection = new Socket(this, sessionId);
			if (this.ServiceEventHandle)
				this.ServiceEventHandle.OnSocketConnected(this, this.Connection);
        }

		protected CallbackOnSocketClose(sessionId: bigint): void {
			if (this.Connection && this.Connection.SessionId == sessionId) {
				if (this.ServiceEventHandle)
					this.ServiceEventHandle.OnSocketClosed(this, this.Connection);
				this.Connection = null;
            }
		}

		protected CallbackOnSocketProcessInputBuffer(sessionId: bigint, buffer: ArrayBuffer, offset: number, len: number): void {
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

		public Connect(hostNameOrAddress: string, port: number, autoReconnect: boolean = true): void {
			this.Implement.Connect(hostNameOrAddress, port, autoReconnect);
        }

		public Send(sessionId: bigint, buffer: Zeze.ByteBuffer): void {
			this.Implement.Send(sessionId, buffer.Bytes.buffer, buffer.ReadIndex, buffer.Size());
		}

		public Close(sessionId: bigint): void {
			this.Implement.Close(sessionId);
		}

		public TickUpdate(): void {
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

		public EnsureWrite(size: number): void {
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

		public Append(bytes: Uint8Array, offset: number, len: number): void {
			this.EnsureWrite(len);
			ByteBuffer.BlockCopy(bytes, offset, this.Bytes, this.WriteIndex, len);
			this.WriteIndex += len;
		}

		public Replace(writeIndex: number, src: Uint8Array, srcOffset: number, len: number): void {
			if (writeIndex < this.ReadIndex || writeIndex + len > this.WriteIndex)
				throw new Error();
			ByteBuffer.BlockCopy(src, srcOffset, this.Bytes, writeIndex, len);
		}

		public BeginWriteWithSize4(): number {
			var state = this.WriteIndex;
			this.EnsureWrite(4);
			this.WriteIndex += 4;
			return state;
		}

		public EndWriteWithSize4(state: number): void {
			this.View.setInt32(state, this.WriteIndex - state - 4, true);
		}

		public BeginWriteSegment(): number {
			var oldSize = this.Size();
			this.EnsureWrite(1);
			this.WriteIndex += 1;
			return oldSize;
		}

		public EndWriteSegment(oldSize: number): void {
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

		public EnsureRead(size: number): void {
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

		public BeginReadSegment(): number {
			const { startIndex, segmentSize } = this.ReadSegment();
			var saveState = this.ReadIndex;
			this.ReadIndex = startIndex;
			return saveState;
		}

		public EndReadSegment(saveState: number): void {
			this.ReadIndex = saveState;
		}

		public Campact(): void {
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

		public Reset(): void {
			this.ReadIndex = 0;
			this.WriteIndex = 0;
		}

		public WriteBool(b: boolean): void {
			this.EnsureWrite(1);
			this.Bytes[this.WriteIndex++] = b ? 1 : 0;
		}

		public ReadBool(): boolean {
			this.EnsureRead(1);
			return this.Bytes[this.ReadIndex++] != 0;
		}

		public WriteByte(byte: number): void {
			this.EnsureWrite(1);
			this.Bytes[this.WriteIndex ++] = byte;
		}

		public ReadByte(): number {
			this.EnsureRead(1);
			return this.Bytes[this.ReadIndex++];
		}

		public WriteShort(x: number): void {
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

		public ReadShort(): number {
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

		public WriteInt4(x: number): void {
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

		public WriteLong8(x: bigint): void {
			this.EnsureWrite(8);
			var u8 = Long.ToUint8Array(x, 8);
			for (var i = u8.length - 1, j = this.WriteIndex; i >= 0; --i, ++j) {
				this.Bytes[j] = u8[i];
            }
			this.WriteIndex += 8;
		}

		public ReadLong8(): bigint {
			this.EnsureRead(8);
			var x = Long.FromUint8ArrayLittleEndian(this.Bytes, this.ReadIndex, 8);
			this.ReadIndex += 8;
			return x;
		}

		public WriteInt(x: number): void {
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

		public ReadInt(): number {
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

		public WriteLong(x: bigint): void {
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
				if (x < 0x200000000000n) // 1111 110x,
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

		public ReadLong(): bigint {
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

		public WriteFloat(x: number): void {
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

		public WriteDouble(x: number): void {
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

		public WriteString(x: string): void {
			var utf8 = ByteBuffer.Encoder.encode(x);
			this.WriteBytes(utf8, 0, utf8.length);
		}

		public ReadString(): string {
			return ByteBuffer.Decoder.decode(this.ReadBytes());
		}

		public WriteBytes(x: Uint8Array, offset: number = 0, length: number = -1): void {
			if (length == -1)
				length = x.byteLength;
			this.WriteInt(length);
			this.EnsureWrite(length);
			ByteBuffer.BlockCopy(x, offset, this.Bytes, this.WriteIndex, length);
			this.WriteIndex += length;
		}

		public ReadBytes(): Uint8Array {
			var n = this.ReadInt();
			this.EnsureRead(n);
			var x = new Uint8Array(n);
			ByteBuffer.BlockCopy(this.Bytes, this.ReadIndex, x, 0, n);
			this.ReadIndex += n;
			return x;
		}

		public SkipBytes(): void {
			var n = this.ReadInt();
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
		public static readonly INT = 0;
		public static readonly LONG = 1;
		public static readonly STRING = 2;
		public static readonly BOOL = 3;
		public static readonly BYTE = 4;
		public static readonly SHORT = 5;
		public static readonly FLOAT = 6;
		public static readonly DOUBLE = 7;
		public static readonly BYTES = 8;
		public static readonly LIST = 9;
		public static readonly SET = 10;
		public static readonly MAP = 11;
		public static readonly BEAN = 12;
		public static readonly DYNAMIC = 13;
		public static readonly TAG_MAX = 31;

		public static readonly TAG_SHIFT = 5;
		public static readonly TAG_MASK = (1 << ByteBuffer.TAG_SHIFT) - 1;
		public static readonly ID_MASK = (1 << (31 - ByteBuffer.TAG_SHIFT)) - 1;

		public static SkipUnknownField(tagid: number, bb: ByteBuffer): void {
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
}
