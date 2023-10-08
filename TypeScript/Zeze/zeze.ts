/* eslint-disable no-param-reassign */
/* eslint-disable no-case-declarations */
/* eslint-disable no-underscore-dangle */
/* eslint-disable class-methods-use-this */
/* eslint-disable no-unused-vars */
/* eslint-disable no-use-before-define */
/* eslint-disable no-plusplus */
/* eslint-disable no-bitwise */
/* eslint-disable prefer-template */
/* eslint-disable lines-between-class-members */
/* eslint-disable global-require */
/* eslint-disable prettier/prettier */
/* eslint-disable max-classes-per-file */
let HostLang: any;
let IsUe = false;

try {
	HostLang = require('csharp'); // puerts unity
} catch (_) {
	try {
		HostLang = require('ue'); // puerts unreal
		IsUe = true;
	} catch (__) { /* empty */ }
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
			const hex = Long.ToHex(bn, bytesCount);
			const len = hex.length / 2;
			const u8 = new Uint8Array(len);
			for (let i = 0, j = 0; i < len; i++, j += 2)
				u8[i] = parseInt(hex.slice(j, j + 2), 16);
			return u8;
		}

		public static ToHex(bn: bigint, bytesCount: number = 8) {
			const pos = bn >= 0;
			if (!pos)
				bn = Long.BitNot(-bn) + 1n;
			const base = 16;
			let hex = bn.toString(base);
			if (hex.length % 2)
				hex = '0' + hex;
			// Check the high byte _after_ proper hex padding
			const highbyte = parseInt(hex.slice(0, 2), 16);
			const highbit = (0x80 & highbyte);
			if (pos && highbit)
				hex = '00' + hex;
			const bytes = hex.length / 2;
			if (bytes > bytesCount)
				throw new Error("bigint too big. bytes=" + bytes + ", bytesCount=" + bytesCount);
			let prefixBytes = bytesCount - bytes;
			if (prefixBytes > 0) {
				let prefix = '';
				const prefixString = pos ? '00' : 'ff';
				while (prefixBytes > 0) {
					prefix += prefixString;
					prefixBytes--;
				}
				hex = prefix + hex;
			}
			return hex;
		}

		public static BitNot(bn: bigint): bigint {
			let bin = (bn).toString(2)
			let prefix = '';
			while (bin.length % 8)
				bin = '0' + bin;
			if (bin[0] === '1' && bin.slice(1).indexOf('1') !== -1)
				prefix = '11111111';
			bin = bin.split('').map(i => i === '0' ? '1' : '0').join('');
			return BigInt('0b' + prefix + bin);
		}

		public static FromUint8ArrayBigEndian(u8: Uint8Array, offset: number, len: number): bigint {
			const hex = [];
			const end = offset + len;
			let pos = true;
			if (len > 0 && (u8[offset] & 0x80))
				pos = false;
			for (let i = offset; i < end; i++) {
				let h = u8[i].toString(16);
				if (h.length % 2)
					h = '0' + h;
				hex.push(h);
			}

			let bn = BigInt('0x' + hex.join(''));
			// console.log(bn.toString(16));
			if (!pos) {
				bn = BigInt('0b' + bn.toString(2).split('').map(i => i === '0' ? 1 : 0).join('')) + BigInt(1);
				bn = -bn;
				// console.log(bn.toString(16));
			}
			return bn;
		}

		public static FromUint8ArrayLittleEndian(u8: Uint8Array, offset: number, len: number): bigint {
			const hex = [];
			const end = offset + len;
			let pos = true;
			if (len > 0 && (u8[end - 1] & 0x80))
				pos = false;
			for (let i = end - 1; i >= offset; --i) {
				let h = u8[i].toString(16);
				if (h.length % 2)
					h = '0' + h;
				hex.push(h);
			}

			let bn = BigInt('0x' + hex.join(''));
			if (!pos) {
				bn = BigInt('0b' + bn.toString(2).split('').map(i => i === '0' ? 1 : 0).join('')) + BigInt(1);
				bn = -bn;
			}
			return bn;
		}
	}

	export class Vector2 {
		public x: number;
		public y: number;

		constructor(x: number, y: number) {
			this.x = x;
			this.y = y;
		}

		public reset() {
			this.x = 0;
			this.y = 0;
		}

		public isZero(): boolean {
			return this.x === 0 && this.y === 0;
		}
	}

	export class Vector3 extends Vector2 {
		public z: number;

		constructor(x: number, y: number, z: number) {
			super(x, y);
			this.z = z;
		}

		public static FromVector2(v: Vector2): Vector3 {
			return new Vector3(v.x, v.y, 0);
		}

		public reset() {
			this.x = 0;
			this.y = 0;
			this.z = 0;
		}

		public isZero(): boolean {
			return this.x === 0 && this.y === 0 && this.z === 0;
		}
	}

	export class Vector4 extends Vector3 {
		public w: number;

		constructor(x: number, y: number, z: number, w: number) {
			super(x, y, z);
			this.w = w;
		}

		public static FromVector2(v: Vector2): Vector4 {
			return new Vector4(v.x, v.y, 0, 0);
		}

		public static FromVector3(v: Vector3): Vector4 {
			return new Vector4(v.x, v.y, v.z, 0);
		}

		public reset() {
			this.x = 0;
			this.y = 0;
			this.z = 0;
			this.w = 0;
		}

		public isZero(): boolean {
			return this.x === 0 && this.y === 0 && this.z === 0 && this.w === 0;
		}
	}

	export interface Serializable {
		Encode(bb: ByteBuffer): void;
		Decode(bb: ByteBuffer): void;
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
			bb.WriteByte(0);
		}

		public Decode(bb: ByteBuffer) {
			bb.SkipUnknownField(ByteBuffer.BEAN);
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
			const typeId = this.GetSpecialTypeIdFromBean(bean);
			this._Bean = bean;
			this._TypeId = typeId;
		}

		private _TypeId: bigint;
		private _Bean: Bean;

		public GetSpecialTypeIdFromBean: (bean: Bean) => bigint;
		public CreateBeanFromSpecialTypeId: (typeId: bigint) => Bean | null;

		public constructor(get: (bean: Bean) => bigint, create: (typeId: bigint) => Bean | null) {
			this.GetSpecialTypeIdFromBean = get;
			this.CreateBeanFromSpecialTypeId = create;
			this._Bean = new EmptyBean();
			this._TypeId = EmptyBean.TYPEID;
		}

		public isEmpty(): boolean {
			return this._TypeId === EmptyBean.TYPEID && this._Bean instanceof EmptyBean;
		}

		public Encode(bb: ByteBuffer) {
			bb.WriteLong(this.TypeId());
			this._Bean.Encode(bb);
		}

		public Decode(bb: ByteBuffer) {
			const typeId = bb.ReadLong();
			const real = this.CreateBeanFromSpecialTypeId(typeId);
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
		public static readonly Response: number = 0;
		public static readonly BitResultCode: number = 1 << 5;
		public static readonly FamilyClassMask: number = FamilyClass.BitResultCode - 1;
	}

	export abstract class Protocol implements Serializable {
		public FamilyClass: number = FamilyClass.Protocol;
		public ResultCode: number = 0; // int
		public ResultCodeModule: number = 0; // int
		public Sender: Socket | null = null;

		public abstract ModuleId(): number;
		public abstract ProtocolId(): number;
		abstract Encode(bb: ByteBuffer): void;
		abstract Decode(bb: ByteBuffer): void;

		public static MakeTypeId(moduleId: number, protocolId: number): bigint {
			if (protocolId < 0)
				protocolId += 0x100000000;
			return (BigInt(moduleId) << 32n) | BigInt(protocolId);
		}

		public TypeId(): bigint {
			return Protocol.MakeTypeId(this.ModuleId(), this.ProtocolId());
		}

		public EncodeProtocol(): ByteBuffer {
			const bb = new ByteBuffer();
			bb.WriteInt4(this.ModuleId());
			bb.WriteInt4(this.ProtocolId());
			const state = bb.BeginWriteWithSize4();
			this.Encode(bb);
			bb.EndWriteWithSize4(state);
			return bb;
		}

		public Send(socket: Socket | null): boolean {
			if (socket == null)
				return false;
			socket.Send(this.EncodeProtocol());
			return true;
		}

		public Dispatch(service: Service, factoryHandle: ProtocolFactoryHandle) {
			service.DispatchProtocol(this, factoryHandle);
		}

		public static DecodeProtocol(service: Service, singleEncodedProtocol: ByteBuffer): Protocol | null {
			const moduleId: number = singleEncodedProtocol.ReadInt4();
			const protocolId: number = singleEncodedProtocol.ReadInt4();
			const size: number = singleEncodedProtocol.ReadInt4();
			const type: bigint = Protocol.MakeTypeId(moduleId, protocolId);
			const factoryHandle = service.FactoryHandleMap.get(type);
			if (factoryHandle != null) {
				const p = factoryHandle.factory();
				p.Decode(singleEncodedProtocol);
				return p;
			}
			return null;
		}

		public static DecodeProtocols(service: Service, socket: Socket, input: ByteBuffer) {
			const os = new ByteBuffer(input.Bytes, input.ReadIndex, input.Size());
			while (os.Size() > 0) {
				let moduleId: number;
				let protocolId: number;
				let size: number;
				const readIndexSaved = os.ReadIndex;

				if (os.Size() >= 12) { // protocol header size.
					moduleId = os.ReadInt4();
					protocolId = os.ReadInt4();
					size = os.ReadInt4();
				} else {
					input.ReadIndex = readIndexSaved;
					return;
				}

				if (size > os.Size()) {
					input.ReadIndex = readIndexSaved;
					return;
				}

				const buffer = new ByteBuffer(os.Bytes, os.ReadIndex, size);
				os.ReadIndex += size;
				const type: bigint = Protocol.MakeTypeId(moduleId, protocolId);
				const factoryHandle = service.FactoryHandleMap.get(type);
				if (factoryHandle != null) {
					const p = factoryHandle.factory();
					p.Decode(buffer);
					p.Sender = socket;
					p.Dispatch(service, factoryHandle);
				} else
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

		public Encode(bb: ByteBuffer) {
			if (this.ResultCode === 0 && this.ResultCodeModule === 0)
				bb.WriteInt(FamilyClass.Protocol);
			else {
				bb.WriteInt(FamilyClass.Protocol | FamilyClass.BitResultCode);
				bb.WriteLong(Protocol.MakeTypeId(this.ResultCodeModule, this.ResultCode));
			}
			this.Argument.Encode(bb);
		}

		public Decode(bb: ByteBuffer) {
			const header = bb.ReadInt();
			this.FamilyClass = header & FamilyClass.FamilyClassMask;
			const rc = ((header & FamilyClass.BitResultCode) !== 0) ? bb.ReadLong() : 0n;
			this.ResultCode = Number(rc & 0xffff_ffffn);
			this.ResultCodeModule = Number((rc >> 32n) & 0xffff_ffffn);
			if (this.ResultCode > 0x7fff_ffff)
				this.ResultCode -= 0x1_0000_0000;
			this.Argument.Decode(bb);
		}
	}

	export type FunctionProtocolFactory = () => Protocol;
	export type FunctionProtocolHandle = (p: Protocol) => number;

	export abstract class Rpc<TArgument extends Bean, TResult extends Bean> extends ProtocolWithArgument<TArgument> {
		public Result: TResult;
		public ResponseHandle: FunctionProtocolHandle | null = null;
		public IsTimeout: boolean = false;

		private IsRequest: boolean = false;
		private sid: bigint = 0n;
		private timeout: any;

		public constructor(argument: TArgument, result: TResult) {
			super(argument);
			this.Result = result;
		}

		public Send(socket: Socket): never {
			throw new Error("Rpc Need Use SendWithCallback");
		}

		public SendWithCallback(socket: Socket, responseHandle: FunctionProtocolHandle, timeoutMs: number = 5000) {
			this.Sender = socket;
			this.ResponseHandle = responseHandle;
			this.IsRequest = true;
			this.sid = socket.service.AddRpcContext(this);

			this.timeout = setTimeout(() => {
				const context = <Rpc<TArgument, TResult>><unknown>socket.service.RemoveRpcContext(this.sid);
				if (context && context.ResponseHandle) {
					context.IsTimeout = true;
					context.ResultCode = -10; // Timeout
					context.ResultCodeModule = 0;
					context.ResponseHandle(context);
				}
			}, timeoutMs);

			super.Send(socket);
		}

		public async SendForWait(socket: Socket, timeoutMs: number = 5000): Promise<void> {
			return new Promise<void>((resolve, reject) => {
				this.SendWithCallback(socket, (response) => {
					const res = <Rpc<TArgument, TResult>><unknown>response;
					if (res.IsTimeout)
						// eslint-disable-next-line prefer-promise-reject-errors
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

		public SendResultCode(code: number, moduleId: number = 0) {
			this.ResultCode = code;
			this.ResultCodeModule = moduleId;
			this.SendResult();
		}

		public Dispatch(service: Service, factoryHandle: ProtocolFactoryHandle) {
			if (this.IsRequest) {
				service.DispatchProtocol(this, factoryHandle);
				return;
			}
			const context = <Rpc<TArgument, TResult>><unknown>service.RemoveRpcContext(this.sid);
			if (context == null)
				return;

			if (context.timeout)
				clearTimeout(context.timeout);

			context.IsRequest = false;
			context.Result = this.Result;
			context.Sender = this.Sender;
			context.ResultCode = this.ResultCode;
			context.ResultCodeModule = this.ResultCodeModule;
			if (context.ResponseHandle)
				context.ResponseHandle(context);
		}

		Decode(bb: ByteBuffer) {
			const header = bb.ReadInt();
			this.FamilyClass = header & FamilyClass.FamilyClassMask;
			this.IsRequest = this.FamilyClass === FamilyClass.Request;
			const rc = ((header & FamilyClass.BitResultCode) !== 0) ? bb.ReadLong() : 0n;
			this.ResultCode = Number(rc & 0xffff_ffffn);
			this.ResultCodeModule = Number((rc >> 32n) & 0xffff_ffffn);
			if (this.ResultCode > 0x7fff_ffff)
				this.ResultCode -= 0x1_0000_0000;
			this.sid = bb.ReadLong();
			if (this.IsRequest)
				this.Argument.Decode(bb);
			else
				this.Result.Decode(bb);
		}

		Encode(bb: ByteBuffer) {
			// skip value of this.FamilyClass
			const header = this.IsRequest ? FamilyClass.Request : FamilyClass.Response;
			if (this.ResultCode === 0 && this.ResultCodeModule === 0)
				bb.WriteInt(header);
			else {
				bb.WriteInt(header | FamilyClass.BitResultCode);
				bb.WriteLong(Protocol.MakeTypeId(this.ResultCodeModule, this.ResultCode));
			}
			bb.WriteLong(this.sid);
			if (this.IsRequest)
				this.Argument.Encode(bb);
			else
				this.Result.Encode(bb);
		}
	}

	export class ProtocolFactoryHandle {
		public factory: FunctionProtocolFactory;
		public handle: FunctionProtocolHandle | null;

		public constructor(f: FunctionProtocolFactory, h: FunctionProtocolHandle | null) {
			this.factory = f;
			this.handle = h;
		}
	}

	export class Socket {
		public service: Service;
		public SessionId: bigint;
		public InputBuffer: ByteBuffer | null = null;

		public constructor(service: Service, sessionId: bigint) {
			this.service = service;
			this.SessionId = sessionId;
		}

		public Send(buffer: ByteBuffer) {
			this.service.Send(this.SessionId, buffer)
		}

		public Close() {
			this.service.Close(this.SessionId);
		}

		public OnProcessInput(newInput: ArrayBuffer, offset: number, len: number) {
			if (this.InputBuffer != null) {
				this.InputBuffer.Append(new Uint8Array(newInput), offset, len);
				Protocol.DecodeProtocols(this.service, this, this.InputBuffer);
				if (this.InputBuffer.Size() > 0)
					this.InputBuffer.Campact();
				else
					this.InputBuffer = null;
				return;
			}
			const bufdirect = new ByteBuffer(new Uint8Array(newInput), offset, len);
			Protocol.DecodeProtocols(this.service, this, bufdirect);
			if (bufdirect.Size() > 0) {
				bufdirect.Campact();
				this.InputBuffer = bufdirect;
			}
		}
	}

	export interface IServiceEventHandle {
		OnSocketConnected(service: Service, socket: Socket): void;
		OnSocketClosed(service: Service, socket: Socket): void;
		OnSoekctInput(service: Service, socket: Socket, buffer: ArrayBuffer, offset: number, len: number): boolean; // true: processed; false: need process by default
	}

	export class ProtocolHead {
		public moduleId: number = 0;
		public protocolId: number = 0;
	}

	export class Service {
		public FactoryHandleMap: Map<bigint, ProtocolFactoryHandle> = new Map<bigint, ProtocolFactoryHandle>();

		private serialId: bigint = 0n;
		private contexts: Map<bigint, Protocol> = new Map<bigint, Protocol>();

		public AddRpcContext(rpc: Protocol): bigint {
			this.contexts.set(++this.serialId, rpc);
			return this.serialId;
		}

		public RemoveRpcContext(sid: bigint): Protocol | null {
			const ctx= this.contexts.get(sid);
			if (ctx) {
				this.contexts.delete(sid);
				return ctx;
			}
			return null;
		}

		public DispatchUnknownProtocol(socket: Socket, type: bigint, buffer: ByteBuffer) {
		}

		public DispatchProtocol(p: Protocol, factoryHandle: ProtocolFactoryHandle) {
			// eslint-disable-next-line prefer-destructuring
			const handle = factoryHandle.handle;
			if (handle)
				handle(p);
		}

		public Connection: Socket | null = null;
		public ServiceEventHandle: IServiceEventHandle | null = null;

		protected CallbackOnSocketHandshakeDone(sessionId: bigint) {
			if (this.Connection)
				this.Connection.Close();
			this.Connection = new Socket(this, sessionId);
			if (this.ServiceEventHandle)
				this.ServiceEventHandle.OnSocketConnected(this, this.Connection);
		}

		protected CallbackOnSocketClose(sessionId: bigint) {
			if (this.Connection && this.Connection.SessionId === sessionId) {
				if (this.ServiceEventHandle)
					this.ServiceEventHandle.OnSocketClosed(this, this.Connection);
				this.Connection = null;
			}
		}

		protected CallbackOnSocketProcessInputBuffer(sessionId: bigint, buffer: ArrayBuffer, offset: number, len: number) {
			if (this.Connection && this.Connection.SessionId === sessionId) {
				if (this.ServiceEventHandle) {
					if (this.ServiceEventHandle.OnSoekctInput(this, this.Connection, buffer, offset, len))
						return;
				}
				this.Connection.OnProcessInput(buffer, offset, len);
			}
		}

		private Implement: any;

		public constructor(name: string) {
			if (IsUe)
				this.Implement = new HostLang.ToTypeScriptService();
			else
				this.Implement = new HostLang.ToTypeScriptService(name);
			this.Implement.CallbackWhenSocketHandshakeDone = this.CallbackOnSocketHandshakeDone.bind(this);
			this.Implement.CallbackWhenSocketClose = this.CallbackOnSocketClose.bind(this);
			this.Implement.CallbackWhenSocketProcessInputBuffer = this.CallbackOnSocketProcessInputBuffer.bind(this);
		}

		public Connect(hostNameOrAddress: string, port: number, autoReconnect: boolean = true) {
			this.Implement.Connect(hostNameOrAddress, port, autoReconnect);
		}

		public Send(sessionId: bigint, buffer: ByteBuffer) {
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

		public constructor(buffer: Uint8Array | null = null, readIndex: number = 0, length: number = 0) {
			this.Bytes = (buffer == null) ? new Uint8Array(16) : buffer;
			this.View = new DataView(this.Bytes.buffer);
			this.ReadIndex = readIndex;
			this.WriteIndex = this.ReadIndex + length;
		}

		ToPower2(needSize: number) {
			let size = 16;
			while (size < needSize)
				size <<= 1;
			return size;
		}

		public static BlockCopy(src: Uint8Array, srcOffset: number, dst: Uint8Array, dstOffset: number, count: number) {
			for (let i = 0; i < count; ++i)
				dst[i + dstOffset] = src[i + srcOffset];
		}

		public Copy(): Uint8Array {
			const copy = new Uint8Array(this.Size());
			ByteBuffer.BlockCopy(this.Bytes, this.ReadIndex, copy, 0, this.Size());
			return copy;
		}

		public EnsureWrite(size: number) {
			const newSize = this.WriteIndex + size;
			if (newSize > this.Capacity()) {
				const newBytes = new Uint8Array(this.ToPower2(newSize));
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
			const state = this.Size();
			this.EnsureWrite(4);
			this.WriteIndex += 4;
			return state;
		}

		public EndWriteWithSize4(state: number) {
			const oldWriteIndex = state + this.ReadIndex;
			this.View.setInt32(oldWriteIndex, this.WriteIndex - oldWriteIndex - 4, true);
		}

		public EnsureRead(size: number) {
			if (this.ReadIndex + size > this.WriteIndex)
				throw new Error("EnsureRead " + this.ReadIndex + '+' + size + " > " + this.WriteIndex);
		}

		public Campact() {
			const size = this.Size();
			if (size > 0) {
				if (this.ReadIndex > 0) {
					ByteBuffer.BlockCopy(this.Bytes, this.ReadIndex, this.Bytes, 0, size);
					this.ReadIndex = 0;
					this.WriteIndex = size;
				}
			} else
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
			return this.ReadLong() !== 0n;
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
			const x = this.View.getInt32(this.ReadIndex, true);
			this.ReadIndex += 4;
			return x;
		}

		public WriteLong8(x: bigint) {
			this.EnsureWrite(8);
			const u8 = Long.ToUint8Array(x, 8);
			for (let i = u8.length - 1, j = this.WriteIndex; i >= 0; --i, ++j)
				this.Bytes[j] = u8[i];
			this.WriteIndex += 8;
		}

		public ReadLong8(): bigint {
			this.EnsureRead(8);
			const x = Long.FromUint8ArrayLittleEndian(this.Bytes, this.ReadIndex, 8);
			this.ReadIndex += 8;
			return x;
		}

		public WriteUInt(u: number) {
			if (u >= 0) {
				if (u < 0x80) {
					this.EnsureWrite(1); // 0xxx xxxx
					this.Bytes[this.WriteIndex++] = (u > 0 ? u : 0);
					return;
				}
				if (u < 0x4000) {
					this.EnsureWrite(2); // 10xx xxxx +1B
					const bytes = this.Bytes;
					const writeIndex = this.WriteIndex;
					bytes[writeIndex] = (u >> 8) + 0x80;
					bytes[writeIndex + 1] = u;
					this.WriteIndex = writeIndex + 2;
					return;
				}
				if (u < 0x20_0000) {
					this.EnsureWrite(3); // 110x xxxx +2B
					const bytes = this.Bytes;
					const writeIndex = this.WriteIndex;
					bytes[writeIndex] = (u >> 16) + 0xc0;
					bytes[writeIndex + 1] = u >> 8;
					bytes[writeIndex + 2] = u;
					this.WriteIndex = writeIndex + 3;
					return;
				}
				if (u < 0x1000_0000) {
					this.EnsureWrite(4); // 1110 xxxx +3B
					const bytes = this.Bytes;
					const writeIndex = this.WriteIndex;
					bytes[writeIndex] = (u >> 24) + 0xe0;
					bytes[writeIndex + 1] = u >> 16;
					bytes[writeIndex + 2] = u >> 8;
					bytes[writeIndex + 3] = u;
					this.WriteIndex = writeIndex + 4;
					return;
				}
			}
			this.EnsureWrite(5); // 1111 0000 +4B
			const bytes = this.Bytes;
			const writeIndex = this.WriteIndex;
			bytes[writeIndex] = 0xf0;
			bytes[writeIndex + 1] = u >> 24;
			bytes[writeIndex + 2] = u >> 16;
			bytes[writeIndex + 3] = u >> 8;
			bytes[writeIndex + 4] = u;
			this.WriteIndex = writeIndex + 5;
		}

		public ReadUInt(): number {
			this.EnsureRead(1);
			const bytes = this.Bytes;
			const readIndex = this.ReadIndex;
			let x = bytes[readIndex];
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

		public SkipUInt() {
			this.EnsureRead(1);
			const readIndex = this.ReadIndex;
			const v = this.Bytes[readIndex];
			if (v < 0x80)
				this.ReadIndex = readIndex + 1;
			else if (v < 0xc0) {
				this.EnsureRead(2);
				this.ReadIndex = readIndex + 2;
			} else if (v < 0xe0) {
				this.EnsureRead(3);
				this.ReadIndex = readIndex + 3;
			} else if (v < 0xf0) {
				this.EnsureRead(4);
				this.ReadIndex = readIndex + 4;
			} else {
				this.EnsureRead(5);
				this.ReadIndex = readIndex + 5;
			}
		}

		public WriteLong(x: bigint) {
			if (x >= 0) {
				if (x < 0x40) {
					this.EnsureWrite(1); // 00xx xxxx
					this.Bytes[this.WriteIndex++] = Number(x);
				} else if (x < 0x2000) {
					this.EnsureWrite(2); // 010x xxxx +1B
					const bytes = this.Bytes;
					const writeIndex = this.WriteIndex;
					const v = Number(x)
					bytes[writeIndex] = (v >> 8) + 0x40;
					bytes[writeIndex + 1] = v;
					this.WriteIndex = writeIndex + 2;
				} else if (x < 0x10_0000) {
					this.EnsureWrite(3); // 0110 xxxx +2B
					const bytes = this.Bytes;
					const writeIndex = this.WriteIndex;
					const v = Number(x)
					bytes[writeIndex] = (v >> 16) + 0x60;
					bytes[writeIndex + 1] = v >> 8;
					bytes[writeIndex + 2] = v;
					this.WriteIndex = writeIndex + 3;
				} else if (x < 0x800_0000) {
					this.EnsureWrite(4); // 0111 0xxx +3B
					const bytes = this.Bytes;
					const writeIndex = this.WriteIndex;
					const v = Number(x)
					bytes[writeIndex] = (v >> 24) + 0x70;
					bytes[writeIndex + 1] = v >> 16;
					bytes[writeIndex + 2] = v >> 8;
					bytes[writeIndex + 3] = v;
					this.WriteIndex = writeIndex + 4;
				} else if (x < 0x4_0000_0000n) {
					this.EnsureWrite(5); // 0111 10xx +4B
					const u8 = Long.ToUint8Array(x, 5);
					this.Bytes[this.WriteIndex] = u8[0] + 0x78;
					ByteBuffer.BlockCopy(u8, 1, this.Bytes, this.WriteIndex + 1, 4);
					this.WriteIndex += 5;
				} else if (x < 0x200_0000_0000n) {
					this.EnsureWrite(6); // 0111 110x +5B
					const u8 = Long.ToUint8Array(x, 6);
					this.Bytes[this.WriteIndex] = u8[0] + 0x7c;
					ByteBuffer.BlockCopy(u8, 1, this.Bytes, this.WriteIndex + 1, 5);
					this.WriteIndex += 6;
				} else if (x < 0x1_0000_0000_0000n) {
					this.EnsureWrite(7); // 0111 1110 +6B
					const u8 = Long.ToUint8Array(x, 7);
					this.Bytes[this.WriteIndex] = u8[0] + 0x7e;
					ByteBuffer.BlockCopy(u8, 1, this.Bytes, this.WriteIndex + 1, 6);
					this.WriteIndex += 7;
				} else if (x < 0x80_0000_0000_0000n) {
					this.EnsureWrite(8); // 0111 1111 0 +7B
					const u8 = Long.ToUint8Array(x, 8);
					this.Bytes[this.WriteIndex] = u8[0] + 0x7f;
					ByteBuffer.BlockCopy(u8, 1, this.Bytes, this.WriteIndex + 1, 7);
					this.WriteIndex += 8;
				} else {
					this.EnsureWrite(9); // 0111 1111 1 +8B
					const u8 = Long.ToUint8Array(x, 8);
					this.Bytes[this.WriteIndex] = 0x7f;
					this.Bytes[this.WriteIndex + 1] = u8[0] + 0x80;
					ByteBuffer.BlockCopy(u8, 1, this.Bytes, this.WriteIndex + 2, 7);
					this.WriteIndex += 9;
				}
			} else {
				// eslint-disable-next-line no-lonely-if
				if (x >= -0x40) {
					this.EnsureWrite(1); // 11xx xxxx
					this.Bytes[this.WriteIndex++] = Number(x);
				} else if (x >= -0x2000) {
					this.EnsureWrite(2); // 101x xxxx +1B
					const bytes = this.Bytes;
					const writeIndex = this.WriteIndex;
					const v = Number(x)
					bytes[writeIndex] = (v >> 8) - 0x40;
					bytes[writeIndex + 1] = v;
					this.WriteIndex = writeIndex + 2;
				} else if (x >= -0x10_0000) {
					this.EnsureWrite(3); // 1001 xxxx +2B
					const bytes = this.Bytes;
					const writeIndex = this.WriteIndex;
					const v = Number(x)
					bytes[writeIndex] = (v >> 16) - 0x60;
					bytes[writeIndex + 1] = v >> 8;
					bytes[writeIndex + 2] = v;
					this.WriteIndex = writeIndex + 3;
				} else if (x >= -0x800_0000) {
					this.EnsureWrite(4); // 1000 1xxx +3B
					const bytes = this.Bytes;
					const writeIndex = this.WriteIndex;
					const v = Number(x)
					bytes[writeIndex] = (v >> 24) - 0x70;
					bytes[writeIndex + 1] = v >> 16;
					bytes[writeIndex + 2] = v >> 8;
					bytes[writeIndex + 3] = v;
					this.WriteIndex = writeIndex + 4;
				} else if (x >= -0x4_0000_0000n) {
					this.EnsureWrite(5); // 1000 01xx +4B
					const u8 = Long.ToUint8Array(x, 8);
					this.Bytes[this.WriteIndex] = u8[3] - 0x78;
					ByteBuffer.BlockCopy(u8, 4, this.Bytes, this.WriteIndex + 1, 4);
					this.WriteIndex += 5;
				} else if (x >= -0x200_0000_0000n) {
					this.EnsureWrite(6); // 1000 001x +5B
					const u8 = Long.ToUint8Array(x, 8);
					this.Bytes[this.WriteIndex] = u8[2] - 0x7c;
					ByteBuffer.BlockCopy(u8, 3, this.Bytes, this.WriteIndex + 1, 5);
					this.WriteIndex += 6;
				} else if (x >= -0x1_0000_0000_0000n) {
					this.EnsureWrite(7); // 1000 0001 +6B
					const u8 = Long.ToUint8Array(x, 8);
					this.Bytes[this.WriteIndex] = 0x81;
					ByteBuffer.BlockCopy(u8, 2, this.Bytes, this.WriteIndex + 1, 6);
					this.WriteIndex += 7;
				} else if (x >= -0x80_0000_0000_0000n) {
					this.EnsureWrite(8); // 1000 0000 1 +7B
					const u8 = Long.ToUint8Array(x, 8);
					this.Bytes[this.WriteIndex] = 0x80;
					ByteBuffer.BlockCopy(u8, 1, this.Bytes, this.WriteIndex + 1, 7);
					this.WriteIndex += 8;
				} else {
					this.EnsureWrite(9); // 1000 0000 0 +8B
					const u8 = Long.ToUint8Array(x, 8);
					this.Bytes[this.WriteIndex] = 0x80;
					this.Bytes[this.WriteIndex + 1] = u8[0] - 0x80;
					ByteBuffer.BlockCopy(u8, 1, this.Bytes, this.WriteIndex + 2, 7);
					this.WriteIndex += 9;
				}
			}
		}

		public ReadLong2BE(): number {
			this.EnsureRead(2);
			const bytes = this.Bytes;
			const readIndex = this.ReadIndex;
			this.ReadIndex = readIndex + 2;
			return (bytes[readIndex] << 8) +
				bytes[readIndex + 1];
		}

		public ReadLong3BE(): number {
			this.EnsureRead(3);
			const bytes = this.Bytes;
			const readIndex = this.ReadIndex;
			this.ReadIndex = readIndex + 3;
			return (bytes[readIndex] << 16) +
				(bytes[readIndex + 1] << 8) +
				bytes[readIndex + 2];
		}

		public ReadLong4BE(): bigint {
			this.EnsureRead(4);
			const readIndex = this.ReadIndex;
			this.ReadIndex = readIndex + 4;
			return Long.FromUint8ArrayBigEndian(this.Bytes, readIndex, 4) & 0xffff_ffffn;
		}

		public ReadLong5BE(): bigint {
			this.EnsureRead(5);
			const readIndex = this.ReadIndex;
			this.ReadIndex = readIndex + 5;
			return Long.FromUint8ArrayBigEndian(this.Bytes, readIndex, 5) & 0xff_ffff_ffffn;
		}

		public ReadLong6BE(): bigint {
			this.EnsureRead(6);
			const readIndex = this.ReadIndex;
			this.ReadIndex = readIndex + 6;
			return Long.FromUint8ArrayBigEndian(this.Bytes, readIndex, 6) & 0xffff_ffff_ffffn;
		}

		public ReadLong7BE(): bigint {
			this.EnsureRead(7);
			const readIndex = this.ReadIndex;
			this.ReadIndex = readIndex + 7;
			return Long.FromUint8ArrayBigEndian(this.Bytes, readIndex, 7) & 0xff_ffff_ffff_ffffn;
		}

		public ReadLong(): bigint {
			this.EnsureRead(1);
			let b = this.Bytes[this.ReadIndex++];
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
					default: const r = this.ReadLong7BE(); return r < 0x80_0000_0000_0000n ?
						r : ((r - 0x80_0000_0000_0000n) << 8n) + BigInt(this.ReadByte());
				}
			default: // 0x10
				switch (b & 7) {
					case 4: case 5: case 6: case 7: return (BigInt(b + 0x78) << 32n) + this.ReadLong4BE();
					case 2: case 3: return (BigInt(b + 0x7c) << 40n) + this.ReadLong5BE();
					case 1: return -0x1_0000_0000_0000n + this.ReadLong6BE();
					default: let r = this.ReadLong7BE(); r = r >= 0x80_0000_0000_0000n ?
						-0x100_0000_0000_0000n + r : ((r + 0x80_0000_0000_0000n) << 8n) + BigInt(this.ReadByte());
						return r < 0x8000_0000_0000_0000n ? r : r - 0x1_0000_0000_0000_0000n; // special fix
				}
			}
		}

		public SkipLong() {
			this.EnsureRead(1);
			let b = this.Bytes[this.ReadIndex++];
			b = b < 0x80 ? b : b - 0x100;
			switch ((b >> 3) & 0x1f) {
				case 0x00: case 0x01: case 0x02: case 0x03: case 0x04: case 0x05: case 0x06: case 0x07:
				case 0x18: case 0x19: case 0x1a: case 0x1b: case 0x1c: case 0x1d: case 0x1e: case 0x1f: return;
				case 0x08: case 0x09: case 0x0a: case 0x0b:
				case 0x14: case 0x15: case 0x16: case 0x17: this.EnsureRead(1); this.ReadIndex++; return;
				case 0x0c: case 0x0d: case 0x12: case 0x13: this.EnsureRead(2); this.ReadIndex += 2; return;
				case 0x0e: case 0x11: this.EnsureRead(3); this.ReadIndex += 3; return;
				case 0x0f:
					switch (b & 7) {
						case 0: case 1: case 2: case 3: this.EnsureRead(4); this.ReadIndex += 4; return;
						case 4: case 5: this.EnsureRead(5); this.ReadIndex += 5; return;
						case 6: this.EnsureRead(6); this.ReadIndex += 6; return;
						default: this.EnsureRead(1); const n = 6 + (this.Bytes[this.ReadIndex++] >> 7); this.EnsureRead(n); this.ReadIndex += n; return;
					}
				default: // 0x10
					switch (b & 7) {
						case 4: case 5: case 6: case 7: this.EnsureRead(4); this.ReadIndex += 4; return;
						case 2: case 3: this.EnsureRead(5); this.ReadIndex += 5; return;
						case 1: this.EnsureRead(6); this.ReadIndex += 6; return;
						default: this.EnsureRead(1); const n = 7 - (this.Bytes[this.ReadIndex++] >> 7); this.EnsureRead(n); this.ReadIndex += n;
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
			const x = this.View.getFloat32(this.ReadIndex, true);
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
			const x = this.View.getFloat64(this.ReadIndex, true);
			this.ReadIndex += 8;
			return x;
		}

		public WriteVector2(v: Vector2) {
			this.EnsureWrite(8);
			const i = this.WriteIndex;
			this.View.setFloat32(i, v.x, true);
			this.View.setFloat32(i + 4, v.y, true);
			this.WriteIndex = i + 8;
		}

		public WriteVector3(v: Vector3) {
			this.EnsureWrite(12);
			const i = this.WriteIndex;
			this.View.setFloat32(i, v.x, true);
			this.View.setFloat32(i + 4, v.y, true);
			this.View.setFloat32(i + 8, v.z, true);
			this.WriteIndex = i + 12;
		}

		public WriteVector4(v: Vector4) {
			this.EnsureWrite(16);
			const i = this.WriteIndex;
			this.View.setFloat32(i, v.x, true);
			this.View.setFloat32(i + 4, v.y, true);
			this.View.setFloat32(i + 8, v.z, true);
			this.View.setFloat32(i + 12, v.w, true);
			this.WriteIndex = i + 16;
		}

		public WriteVector2Int(v: Vector2) {
			this.WriteInt(v.x);
			this.WriteInt(v.y);
		}

		public WriteVector3Int(v: Vector3) {
			this.WriteInt(v.x);
			this.WriteInt(v.y);
			this.WriteInt(v.z);
		}

		static Encoder: TextEncoder = new TextEncoder();
		static Decoder: TextDecoder = new TextDecoder();

		public WriteString(x: string) {
			const utf8 = ByteBuffer.Encoder.encode(x);
			this.WriteBytes(utf8, 0, utf8.length);
		}

		public ReadString(): string {
			return ByteBuffer.Decoder.decode(this.ReadBytes());
		}

		public WriteBytes(x: Uint8Array, offset: number = 0, length: number = -1) {
			if (length === -1)
				length = x.byteLength;
			this.WriteUInt(length);
			this.EnsureWrite(length);
			ByteBuffer.BlockCopy(x, offset, this.Bytes, this.WriteIndex, length);
			this.WriteIndex += length;
		}

		public ReadBytes(): Uint8Array {
			const n = this.ReadUInt();
			this.EnsureRead(n);
			const x = new Uint8Array(n);
			ByteBuffer.BlockCopy(this.Bytes, this.ReadIndex, x, 0, n);
			this.ReadIndex += n;
			return x;
		}

		public SkipBytes() {
			const n = this.ReadUInt();
			this.EnsureRead(n);
			this.ReadIndex += n;
		}

		public Equals(other: ByteBuffer): boolean {
			if (other == null)
				return false;

			if (this.Size !== other.Size)
				return false;

			const size = this.Size();
			for (let i = 0; i < size; i++) {
				if (this.Bytes[this.ReadIndex + i] !== other.Bytes[other.ReadIndex + i])
					return false;
			}
			return true;
		}

		static HEX = "0123456789ABCDEF";
		public static toHex(x: number): string {
			const l = x & 0x0f;
			const h = (x >> 4) & 0x0f;
			return this.HEX[h] + this.HEX[l];
		}

		public static toString(x: Uint8Array, from: number = 0, to: number = -1): string {
			let ss = "";
			let bfirst = true;
			if (to === -1)
				to = x.byteLength;
			for (let i = from; i < to; ++i) {
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

		// Only new type definitions can be added, and remember to synchronize SkipUnknownField when adding them
		public static readonly INTEGER = 0; // byte,short,int,long,bool
		public static readonly FLOAT = 1; // float
		public static readonly DOUBLE = 2; // double
		public static readonly BYTES = 3; // binary,string
		public static readonly LIST = 4; // list,set
		public static readonly MAP = 5; // map
		public static readonly BEAN = 6; // bean
		public static readonly DYNAMIC = 7; // dynamic
		public static readonly VECTOR2 = 8; // float{x,y}
		public static readonly VECTOR2INT = 9; // int{x,y}
		public static readonly VECTOR3 = 10; // float{x,y,z}
		public static readonly VECTOR3INT = 11; // int{x,y,z}
		public static readonly VECTOR4 = 12; // float{x,y,z,w} Quaternion

		public static readonly TAG_SHIFT = 4;
		public static readonly TAG_MASK = (1 << ByteBuffer.TAG_SHIFT) - 1;
		public static readonly ID_MASK = 0xff - ByteBuffer.TAG_MASK;

		public WriteTag(lastVarId: number, varId: number, type: number): number {
			const deltaId = varId - lastVarId;
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
			const deltaId = (tagByte & ByteBuffer.ID_MASK) >> ByteBuffer.TAG_SHIFT;
			return deltaId < 0xf ? deltaId : 0xf + this.ReadUInt();
		}

		public ReadBoolT(tag: number): boolean {
			const type = tag & ByteBuffer.TAG_MASK;
			if (type === ByteBuffer.INTEGER)
				return this.ReadLong() !== 0n;
			if (type === ByteBuffer.FLOAT)
				return this.ReadFloat() !== 0;
			if (type === ByteBuffer.DOUBLE)
				return this.ReadDouble() !== 0;
			this.SkipUnknownField(tag);
			return false;
		}

		public ReadIntT(tag: number): number {
			const type = tag & ByteBuffer.TAG_MASK;
			if (type === ByteBuffer.INTEGER)
				return Number(this.ReadLong());
			if (type === ByteBuffer.FLOAT)
				return this.ReadFloat();
			if (type === ByteBuffer.DOUBLE)
				return this.ReadDouble();
			this.SkipUnknownField(tag);
			return 0;
		}

		public ReadLongT(tag: number): bigint {
			const type = tag & ByteBuffer.TAG_MASK;
			if (type === ByteBuffer.INTEGER)
				return this.ReadLong();
			if (type === ByteBuffer.FLOAT)
				return BigInt(this.ReadFloat());
			if (type === ByteBuffer.DOUBLE)
				return BigInt(this.ReadDouble());
			this.SkipUnknownField(tag);
			return 0n;
		}

		public ReadFloatT(tag: number): number {
			const type = tag & ByteBuffer.TAG_MASK;
			if (type === ByteBuffer.FLOAT)
				return this.ReadFloat();
			if (type === ByteBuffer.DOUBLE)
				return this.ReadDouble();
			if (type === ByteBuffer.INTEGER)
				return Number(this.ReadLong());
			this.SkipUnknownField(tag);
			return 0;
		}

		public ReadDoubleT(tag: number): number {
			const type = tag & ByteBuffer.TAG_MASK;
			if (type === ByteBuffer.DOUBLE)
				return this.ReadDouble();
			if (type === ByteBuffer.FLOAT)
				return this.ReadFloat();
			if (type === ByteBuffer.INTEGER)
				return Number(this.ReadLong());
			this.SkipUnknownField(tag);
			return 0;
		}

		public ReadBytesT(tag: number): Uint8Array {
			const type = tag & ByteBuffer.TAG_MASK;
			if (type === ByteBuffer.BYTES)
				return this.ReadBytes();
			this.SkipUnknownField(tag);
			return new Uint8Array(0);
		}

		public ReadStringT(tag: number): string {
			const type = tag & ByteBuffer.TAG_MASK;
			if (type === ByteBuffer.BYTES)
				return this.ReadString();
			this.SkipUnknownField(tag);
			return "";
		}

		private ToFloat(i: number): number {
			return this.View.getFloat32(i, true);
		}

		public ReadVector2(): Vector2 {
			this.EnsureRead(8);
			const i = this.ReadIndex;
			const x = this.ToFloat(i);
			const y = this.ToFloat(i + 4);
			this.ReadIndex = i + 8;
			return new Vector2(x, y);
		}

		public ReadVector3(): Vector3 {
			this.EnsureRead(12);
			const i = this.ReadIndex;
			const x = this.ToFloat(i);
			const y = this.ToFloat(i + 4);
			const z = this.ToFloat(i + 8);
			this.ReadIndex = i + 12;
			return new Vector3(x, y, z);
		}

		public ReadVector4(): Vector4 {
			this.EnsureRead(16);
			const i = this.ReadIndex;
			const x = this.ToFloat(i);
			const y = this.ToFloat(i + 4);
			const z = this.ToFloat(i + 8);
			const w = this.ToFloat(i + 12);
			this.ReadIndex = i + 16;
			return new Vector4(x, y, z, w);
		}

		public ReadVector2Int(): Vector2 {
			const x = this.ReadInt();
			const y = this.ReadInt();
			return new Vector2(x, y);
		}

		public ReadVector3Int(): Vector3 {
			const x = this.ReadInt();
			const y = this.ReadInt();
			const z = this.ReadInt();
			return new Vector3(x, y, z);
		}

		public ReadVector2T(tag: number): Vector2 {
			const type = tag & ByteBuffer.TAG_MASK;
			if (type === ByteBuffer.VECTOR2)
				return this.ReadVector2();
			if (type === ByteBuffer.VECTOR3)
				return this.ReadVector3();
			if (type === ByteBuffer.VECTOR4)
				return this.ReadVector4();
			if (type === ByteBuffer.VECTOR2INT)
				return this.ReadVector2Int();
			if (type === ByteBuffer.VECTOR3INT)
				return this.ReadVector3Int();
			if (type === ByteBuffer.FLOAT)
				return new Vector2(this.ReadFloat(), 0);
			if (type === ByteBuffer.DOUBLE)
				return new Vector2(this.ReadDouble(), 0);
			if (type === ByteBuffer.INTEGER)
				return new Vector2(Number(this.ReadLong()), 0);
			this.SkipUnknownField(tag);
			return new Vector2(0, 0);
		}

		public ReadVector3T(tag: number): Vector3 {
			const type = tag & ByteBuffer.TAG_MASK;
			if (type === ByteBuffer.VECTOR3)
				return this.ReadVector3();
			if (type === ByteBuffer.VECTOR2)
				return Vector3.FromVector2(this.ReadVector2());
			if (type === ByteBuffer.VECTOR4)
				return this.ReadVector4();
			if (type === ByteBuffer.VECTOR3INT)
				return this.ReadVector3Int();
			if (type === ByteBuffer.VECTOR2INT)
				return Vector3.FromVector2(this.ReadVector2Int());
			if (type === ByteBuffer.FLOAT)
				return new Vector3(this.ReadFloat(), 0, 0);
			if (type === ByteBuffer.DOUBLE)
				return new Vector3(this.ReadDouble(), 0, 0);
			if (type === ByteBuffer.INTEGER)
				return new Vector3(Number(this.ReadLong()), 0, 0);
			this.SkipUnknownField(tag);
			return new Vector3(0, 0, 0);
		}

		public ReadVector4T(tag: number): Vector4 {
			const type = tag & ByteBuffer.TAG_MASK;
			if (type === ByteBuffer.VECTOR4)
				return this.ReadVector4();
			if (type === ByteBuffer.VECTOR3)
				return Vector4.FromVector3(this.ReadVector3());
			if (type === ByteBuffer.VECTOR2)
				return Vector4.FromVector2(this.ReadVector2());
			if (type === ByteBuffer.VECTOR3INT)
				return Vector4.FromVector3(this.ReadVector3Int());
			if (type === ByteBuffer.VECTOR2INT)
				return Vector4.FromVector2(this.ReadVector2Int());
			if (type === ByteBuffer.FLOAT)
				return new Vector4(this.ReadFloat(), 0, 0, 0);
			if (type === ByteBuffer.DOUBLE)
				return new Vector4(this.ReadDouble(), 0, 0, 0);
			if (type === ByteBuffer.INTEGER)
				return new Vector4(Number(this.ReadLong()), 0, 0, 0);
			this.SkipUnknownField(tag);
			return new Vector4(0, 0, 0, 0);
		}

		public ReadBean<T extends Serializable>(bean: T, tag: number): T {
			const type = tag & ByteBuffer.TAG_MASK;
			if (type === ByteBuffer.BEAN)
				bean.Decode(this);
			else if (type === ByteBuffer.DYNAMIC) {
				this.ReadLong();
				bean.Decode(this);
			} else
				this.SkipUnknownField(tag);
			return bean;
		}

		public ReadDynamic(dynBean: DynamicBean, tag: number): DynamicBean {
			const type = tag & ByteBuffer.TAG_MASK;
			if (type === ByteBuffer.DYNAMIC) {
				dynBean.Decode(this);
				return dynBean;
			}
			if (type === ByteBuffer.BEAN) {
				const bean = dynBean.CreateBeanFromSpecialTypeId(0n);
				if (bean != null) {
					bean.Decode(this);
					return dynBean;
				}
			}
			this.SkipUnknownField(tag);
			return dynBean;
		}

		public SkipUnknownList(tag: number, count: number) {
			while (--count >= 0)
				this.SkipUnknownField(tag);
		}

		public SkipUnknownMap(type1: number, type2: number, count: number) {
			type1 |= 0x10; // ensure high bits not zero
			type2 |= 0x10; // ensure high bits not zero
			while (--count >= 0) {
				this.SkipUnknownField(type1);
				this.SkipUnknownField(type2);
			}
		}

		public SkipUnknownField(tag: number) {
			const type = tag & ByteBuffer.TAG_MASK;
			switch (type) {
				case ByteBuffer.INTEGER:
					this.SkipLong();
					return;
				case ByteBuffer.FLOAT:
					if (tag === ByteBuffer.FLOAT) // high bits === 0
						return;
					this.EnsureRead(4);
					this.ReadIndex += 4;
					return;
				case ByteBuffer.DOUBLE:
				case ByteBuffer.VECTOR2:
					this.EnsureRead(8);
					this.ReadIndex += 8;
					return;
				case ByteBuffer.VECTOR2INT:
					this.SkipLong();
					this.SkipLong();
					return;
				case ByteBuffer.VECTOR3:
					this.EnsureRead(12);
					this.ReadIndex += 12;
					return;
				case ByteBuffer.VECTOR3INT:
					this.SkipLong();
					this.SkipLong();
					this.SkipLong();
					return;
				case ByteBuffer.VECTOR4:
					this.EnsureRead(16);
					this.ReadIndex += 16;
					return;
				case ByteBuffer.BYTES:
					this.SkipBytes();
					return;
				case ByteBuffer.LIST:
					const t1 = this.ReadByte();
					this.SkipUnknownList(t1, this.ReadTagSize(t1));
					return;
				case ByteBuffer.MAP:
					const t2 = this.ReadByte();
					this.SkipUnknownMap(t2 >> ByteBuffer.TAG_SHIFT, t2, this.ReadUInt());
					return;
				case ByteBuffer.DYNAMIC:
					this.SkipLong();
				// eslint-disable-next-line no-fallthrough
				case ByteBuffer.BEAN:
					let t3;
					// eslint-disable-next-line no-cond-assign
					while ((t3 = this.ReadByte()) !== 0) {
						if ((t3 & ByteBuffer.ID_MASK) === 0xf0)
							this.SkipUInt();
						this.SkipUnknownField(t3);
					}
					return;
				default:
					throw new Error("SkipUnknownField: type=" + type);
			}
		}
	}
}
