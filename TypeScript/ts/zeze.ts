
import Long from "long"
import { TextEncoder, TextDecoder } from "encoding"

// 根据实际使用的 puerts 的库，修改下面的 import。
import * as HostLang from 'csharp' // 'ue'

export module Zeze {
	export interface Serializable {
		Encode(_os_: ByteBuffer): void;
		Decode(_os_: ByteBuffer): void;
	}

	export interface Bean extends Serializable {
		TypeId(): Long;
    }

	export class EmptyBean implements Bean {
		public static readonly TYPEID: Long = new Long(0, 0, true);

		public TypeId(): Long {
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
		public ResultHandle: FunctionProtocolHandle;
		public TimeoutHandle: FunctionProtocolHandle;

		private IsRequest: boolean;
		private sid: Long;
		private timeout: any;

		public constructor(argument: TArgument, result: TResult) {
			super(argument);
			this.Result = result;
		}

		public SendWithCallback(socket: Socket, resultHandle: FunctionProtocolHandle,
			timeoutHandle: FunctionProtocolHandle = null, timeoutMs: number = 0): void {

			this.Sender = socket;
			this.ResultHandle = resultHandle;
			this.IsRequest = true;
			this.sid = socket.service.AddRpcContext(this);

			if (null != timeoutHandle && timeoutMs > 0) {
				this.TimeoutHandle = timeoutHandle;
				this.timeout = setTimeout(() => {
					var context = <Rpc<TArgument, TResult>>this.Sender.service.RemoveRpcContext(this.sid);
					if (null != context) {
						context.TimeoutHandle(context);
                    }
				}, timeoutMs);
            }

			super.Send(socket);
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

			context.ResultHandle(context);
		}

		Decode(bb: Zeze.ByteBuffer): void {
			this.sid = bb.ReadLong();
			this.IsRequest = (this.sid.high & 0x80000000) != 0;
			if (this.IsRequest) {
				this.sid.high &= 0x7fffffff;
				this.ResultCode = bb.ReadInt();
				this.Argument.Decode(bb);
			}
			else {
				this.ResultCode = bb.ReadInt();
				this.Result.Decode(bb);
			}
		}

		Encode(bb: Zeze.ByteBuffer): void {
			if (this.IsRequest) {
				this.sid.high |= 0x80000000;
				bb.WriteLong(this.sid);
				bb.WriteInt(this.ResultCode);
				this.Argument.Encode(bb);
			}
			else {
				bb.WriteLong(this.sid);
				bb.WriteInt(this.ResultCode);
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

		public constructor(service: Service, sessionId: Long) {
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

		private serialId: Long = new Long(0, 0, true);
		private contexts: Map<Long, Zeze.Protocol> = new Map<Long, Zeze.Protocol>();

		public AddRpcContext(rpc: Zeze.Protocol): Long {
			this.serialId = this.serialId.add(1);
			this.contexts.set(this.serialId, rpc);
			return this.serialId;
		}

		public RemoveRpcContext(sid: Long): Zeze.Protocol {
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

		private Implement: HostLang.ToTypeScriptService;

		public constructor(name: string) {
			this.Implement = new HostLang.ToTypeScriptService(name); // delete name parameter in unreal

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

		public WriteLong8(x: Long): void {
			this.EnsureWrite(8);
			this.View.setUint32(this.WriteIndex, x.low, true);
			this.View.setUint32(this.WriteIndex + 4, x.high, true);
			this.WriteIndex += 8;
		}

		public ReadLong8(): Long {
			this.EnsureRead(8);
			var low32 = this.View.getUint32(this.ReadIndex, true);
			var high32 = this.View.getUint32(this.ReadIndex + 4, true);
			this.ReadIndex += 8;
			return new Long(low32, high32, true);
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

		public WriteLong(x: Long): void {
			this.WriteUlong(x);
		}

		public ReadLong(): Long {
			return this.ReadUlong();
		}

		WriteUlong(x: Long): void {
			// 0 111 1111
			if (x.high == 0) {
				if (x.low >= 0) {
					if (x.low < 0x80) {
						this.EnsureWrite(1);
						this.Bytes[this.WriteIndex++] = x.low;
						return;
					}
					if (x.low < 0x4000) // 10 11 1111, -
					{
						this.EnsureWrite(2);
						this.Bytes[this.WriteIndex + 1] = x.low;
						this.Bytes[this.WriteIndex] = ((x.low >> 8) | 0x80);
						this.WriteIndex += 2;
						return;
					}
					if (x.low < 0x200000) // 110 1 1111, -,-
					{
						this.EnsureWrite(3);
						this.Bytes[this.WriteIndex + 2] = x.low;
						this.Bytes[this.WriteIndex + 1] = (x.low >> 8);
						this.Bytes[this.WriteIndex] = ((x.low >> 16) | 0xc0);
						this.WriteIndex += 3;
						return;
					}
					if (x.low < 0x10000000) // 1110 1111,-,-,-
					{
						this.EnsureWrite(4);
						this.Bytes[this.WriteIndex + 3] = x.low;
						this.Bytes[this.WriteIndex + 2] = (x.low >> 8);
						this.Bytes[this.WriteIndex + 1] = (x.low >> 16);
						this.Bytes[this.WriteIndex] = ((x.low >> 24) | 0xe0);
						this.WriteIndex += 4;
						return;
					}
                }
				// fall down
			}
			if (x.high >= 0) {
				if (x.high < 0x8) // 1111 0xxx,-,-,-,-
				{
					this.EnsureWrite(5);
					this.View.setUint32(this.WriteIndex + 1, x.low, false);
					this.Bytes[this.WriteIndex] = ((x.high) | 0xf0);
					this.WriteIndex += 5;
					return;
				}
				if (x.high < 0x400) // 1111 10xx, 
				{
					this.EnsureWrite(6);
					this.View.setUint32(this.WriteIndex + 2, x.low, false);
					this.Bytes[this.WriteIndex + 1] = (x.high & 0xff);
					this.Bytes[this.WriteIndex] = ((x.high >> 8) | 0xf8);
					this.WriteIndex += 6;
					return;
				}
				if (x.high < 0x2000) // 1111 110x,
				{
					this.EnsureWrite(7);
					this.View.setUint32(this.WriteIndex + 3, x.low, false);
					this.Bytes[this.WriteIndex + 2] = (x.high & 0xff);
					this.Bytes[this.WriteIndex + 1] = (x.high >> 8);
					this.Bytes[this.WriteIndex] = ((x.high >> 16) | 0xfc);
					this.WriteIndex += 7;
					return;
				}
				if (x.high < 0x1000000) // 1111 1110
				{
					this.EnsureWrite(8);
					this.View.setUint32(this.WriteIndex + 4, x.low, false);
					this.Bytes[this.WriteIndex + 3] = (x.high & 0xff);
					this.Bytes[this.WriteIndex + 2] = (x.high >> 8);
					this.Bytes[this.WriteIndex + 1] = (x.high >> 16);
					this.Bytes[this.WriteIndex] = 0xfe;
					this.WriteIndex += 8;
					return;
				}
				// fall down
            }
			// 1111 1111
			this.EnsureWrite(9);
			this.View.setUint32(this.WriteIndex + 5, x.low, false);
			this.View.setUint32(this.WriteIndex + 1, x.high, false);
			this.Bytes[this.WriteIndex] = 0xff;
			this.WriteIndex += 9;
		}

        ReadUlong() : Long {
			this.EnsureRead(1);
			var h = this.Bytes[this.ReadIndex];
			if (h < 0x80) {
				this.ReadIndex++;
				return new Long(h, 0, true);
			}
			if (h < 0xc0) {
				this.EnsureRead(2);
				var x = ((h & 0x3f) << 8) | this.Bytes[this.ReadIndex + 1];
				this.ReadIndex += 2;
				return new Long(x, 0, true);
			}
			if (h < 0xe0) {
				this.EnsureRead(3);
				var x = ((h & 0x1f) << 16) | (this.Bytes[this.ReadIndex + 1] << 8) | this.Bytes[this.ReadIndex + 2];
				this.ReadIndex += 3;
				return new Long(x, 0, true);
			}
			if (h < 0xf0) {
				this.EnsureRead(4);
				var x = ((h & 0x0f) << 24) | (this.Bytes[this.ReadIndex + 1] << 16) | (this.Bytes[this.ReadIndex + 2] << 8) | this.Bytes[this.ReadIndex + 3];
				this.ReadIndex += 4;
				return new Long(x, 0, true);
			}
			if (h < 0xf8) {
				this.EnsureRead(5);
				var xl = this.View.getUint32(this.ReadIndex + 1, false);
				var xh = h & 0x07;
				this.ReadIndex += 5;
				return new Long(xl, xh, true);
			}
			if (h < 0xfc) {
				this.EnsureRead(6);
				var xl = this.View.getUint32(this.ReadIndex + 2, false);
				var xh = ((h & 0x03) << 8) | this.Bytes[this.ReadIndex + 1];
				this.ReadIndex += 6;
				return new Long(xl, xh, true);
			}
			if (h < 0xfe) {
				this.EnsureRead(7);
				var xl = this.View.getUint32(this.ReadIndex + 3, false);
				var xh = ((h & 0x01) << 16) | (this.Bytes[this.ReadIndex + 1] << 8) | this.Bytes[this.ReadIndex + 2];
				this.ReadIndex += 7;
				return new Long(xl, xh, true);
			}
			if (h < 0xff) {
				this.EnsureRead(8);
				var xl = this.View.getUint32(this.ReadIndex + 4, false);
				var xh = /*((h & 0x01) << 24) |*/ (this.Bytes[this.ReadIndex + 1] << 16) | (this.Bytes[this.ReadIndex + 2] << 8) | this.Bytes[this.ReadIndex + 3];
				this.ReadIndex += 8;
				return new Long(xl, xh, true);
			}
			{
				this.EnsureRead(9);
				var xl = this.View.getUint32(this.ReadIndex + 5, false);
				var xh = this.View.getUint32(this.ReadIndex + 1, false);
				this.ReadIndex += 9;
				return new Long(xl, xh, true);
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
