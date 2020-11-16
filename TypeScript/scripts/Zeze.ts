
import Long from "long.js"

export namespace Serialize
{
	export interface Serializable {
		Encode(_os_: ByteBuffer): void;
		Decode(_os_: ByteBuffer): void;
	}

	export class ByteBuffer {
		public Bytes: ArrayBuffer;
		public ReadIndex: number;
		public WriteIndex: number;

		View: DataView;

		public Capacity(): number {
			return this.Bytes.byteLength;
        }

		public Size(): number {
			return this.WriteIndex - this.ReadIndex;
		}

		public constructor(buffer: ArrayBuffer = null) {
			this.Bytes = (null == buffer) ? new ArrayBuffer(1024) : buffer;
			this.View = new DataView(this.Bytes);
			this.ReadIndex = 0;
			this.WriteIndex = 0;
		}

		ToPower2(needSize: number) {
			var size = 1024;
			while (size < needSize)
				size <<= 1;
			return size;
        }

		BlockCopy(src: ArrayBuffer, srcOffset: number, dst: ArrayBuffer, dstOffset: number, count: number) {
			for (var i = 0; i < count; ++i) {
				dst[i + dstOffset] = src[i + srcOffset];
            }
        }

		public EnsureWrite(size: number): void {
			var newSize = this.WriteIndex + size;
			if (newSize > this.Capacity()) {
				var newBuffer = new ArrayBuffer(this.ToPower2(newSize));
				this.WriteIndex -= this.ReadIndex;
				this.BlockCopy(this.Bytes, this.ReadIndex, newBuffer, 0, this.WriteIndex);
				this.ReadIndex = 0;
				this.Bytes = newBuffer;
				this.View = new DataView(this.Bytes);
			}
		}

		public Append(bytes: ArrayBuffer, offset: number, len: number): void {
			this.EnsureWrite(len);
			this.BlockCopy(bytes, offset, this.Bytes, this.WriteIndex, len);
			this.WriteIndex += len;
		}

		public Replace(writeIndex: number, src: ArrayBuffer, srcOffset: number, len: number): void {
			if (writeIndex < this.ReadIndex || writeIndex + len > this.WriteIndex)
				throw new Error();
			this.BlockCopy(src, srcOffset, this.Bytes, writeIndex, len);
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
					this.BlockCopy(this.Bytes, this.ReadIndex, this.Bytes, 0, size);
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
			this.Bytes[this.WriteIndex + 2] = x;
			this.Bytes[this.WriteIndex + 1] = (x >> 8);
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
				var x = (this.Bytes[this.ReadIndex + 1] << 8) | this.Bytes[this.ReadIndex + 2];
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
			this.WriteUint(x);
		}

		public ReadInt(): number {
			return this.ReadUint();
		}

		WriteUint(x: number): void {
			// 0 111 1111
			if (x < 0x80) {
				this.EnsureWrite(1);
				this.Bytes[this.WriteIndex++] = x;
			}
			else if (x < 0x4000) // 10 11 1111, -
			{
				this.EnsureWrite(2);
				this.Bytes[this.WriteIndex + 1] = x;
				this.Bytes[this.WriteIndex] = ((x >> 8) | 0x80);
				this.WriteIndex += 2;
			}
			else if (x < 0x200000) // 110 1 1111, -,-
			{
				this.EnsureWrite(3);
				this.Bytes[this.WriteIndex + 2] = x;
				this.Bytes[this.WriteIndex + 1] = (x >> 8);
				this.Bytes[this.WriteIndex] = ((x >> 16) | 0xc0);
				this.WriteIndex += 3;
			}
			else if (x < 0x10000000) // 1110 1111,-,-,-
			{
				this.EnsureWrite(4);
				this.Bytes[this.WriteIndex + 3] = x;
				this.Bytes[this.WriteIndex + 2] = (x >> 8);
				this.Bytes[this.WriteIndex + 1] = (x >> 16);
				this.Bytes[this.WriteIndex] = ((x >> 24) | 0xe0);
				this.WriteIndex += 4;
			}
			else {
				this.EnsureWrite(5);
				this.Bytes[this.WriteIndex] = 0xf0;
				this.Bytes[this.WriteIndex + 4] = x;
				this.Bytes[this.WriteIndex + 3] = (x >> 8);
				this.Bytes[this.WriteIndex + 2] = (x >> 16);
				this.Bytes[this.WriteIndex + 1] = (x >> 24);
				this.WriteIndex += 5;
			}
		}

		ReadUint(): number {
			this.EnsureRead(1);
			var h = this.Bytes[this.ReadIndex];
			if (h < 0x80) {
				this.ReadIndex++;
				return h;
			}
			else if (h < 0xc0) {
				this.EnsureRead(2);
				var x = ((h & 0x3f) << 8) | this.Bytes[this.ReadIndex + 1];
				this.ReadIndex += 2;
				return x;
			}
			else if (h < 0xe0) {
				this.EnsureRead(3);
				var x = ((h & 0x1f) << 16) | (this.Bytes[this.ReadIndex + 1] << 8) | this.Bytes[this.ReadIndex + 2];
				this.ReadIndex += 3;
				return x;
			}
			else if (h < 0xf0) {
				this.EnsureRead(4);
				var x = ((h & 0x0f) << 24) | (this.Bytes[this.ReadIndex + 1] << 16) | (this.Bytes[this.ReadIndex + 2] << 8) | this.Bytes[this.ReadIndex + 3];
				this.ReadIndex += 4;
				return x;
			}
			else {
				this.EnsureRead(5);
				var x = (this.Bytes[this.ReadIndex + 1] << 24) | ((this.Bytes[this.ReadIndex + 2] << 16)) | (this.Bytes[this.ReadIndex + 3] << 8) | this.Bytes[this.ReadIndex + 4];
				this.ReadIndex += 5;
				return x;
			}
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
				if (x.low < 0x80) {
					this.EnsureWrite(1);
					this.Bytes[this.WriteIndex++] = x.low;
				}
				else if (x.low < 0x4000) // 10 11 1111, -
				{
					this.EnsureWrite(2);
					this.Bytes[this.WriteIndex + 1] = x.low;
					this.Bytes[this.WriteIndex] = ((x.low >> 8) | 0x80);
					this.WriteIndex += 2;
				}
				else if (x.low < 0x200000) // 110 1 1111, -,-
				{
					this.EnsureWrite(3);
					this.Bytes[this.WriteIndex + 2] = x.low;
					this.Bytes[this.WriteIndex + 1] = (x.low >> 8);
					this.Bytes[this.WriteIndex] = ((x.low >> 16) | 0xc0);
					this.WriteIndex += 3;
				}
				else if (x.low < 0x10000000) // 1110 1111,-,-,-
				{
					this.EnsureWrite(4);
					this.Bytes[this.WriteIndex + 3] = x.low;
					this.Bytes[this.WriteIndex + 2] = (x.low >> 8);
					this.Bytes[this.WriteIndex + 1] = (x.low >> 16);
					this.Bytes[this.WriteIndex] = ((x.low >> 24) | 0xe0);
					this.WriteIndex += 4;
				}
			} else {
				if (x.high < 0x8) // 1111 0xxx,-,-,-,-
				{
					this.EnsureWrite(5);
					this.Bytes[this.WriteIndex + 4] = x.low;
					this.Bytes[this.WriteIndex + 3] = (x.low >> 8);
					this.Bytes[this.WriteIndex + 2] = (x.low >> 16);
					this.Bytes[this.WriteIndex + 1] = (x.low >> 24);
					this.Bytes[this.WriteIndex] = ((x.high) | 0xf0);
					this.WriteIndex += 5;
				}
				else if (x.high < 0x400) // 1111 10xx, 
				{
					this.EnsureWrite(6);
					this.Bytes[this.WriteIndex + 5] = x.low;
					this.Bytes[this.WriteIndex + 4] = (x.low >> 8);
					this.Bytes[this.WriteIndex + 3] = (x.low >> 16);
					this.Bytes[this.WriteIndex + 2] = (x.low >> 24);
					this.Bytes[this.WriteIndex + 1] = (x.high & 0xff);
					this.Bytes[this.WriteIndex] = ((x.high >> 8) | 0xf8);
					this.WriteIndex += 6;
				}
				else if (x.high < 0x2000) // 1111 110x,
				{
					this.EnsureWrite(7);
					this.Bytes[this.WriteIndex + 6] = x.low;
					this.Bytes[this.WriteIndex + 5] = (x.low >> 8);
					this.Bytes[this.WriteIndex + 4] = (x.low >> 16);
					this.Bytes[this.WriteIndex + 3] = (x.low >> 24);
					this.Bytes[this.WriteIndex + 2] = (x.high & 0xff);
					this.Bytes[this.WriteIndex + 1] = (x.high >> 8);
					this.Bytes[this.WriteIndex] = ((x.high >> 16) | 0xfc);
					this.WriteIndex += 7;
				}
				else if (x.high < 0x1000000) // 1111 1110
				{
					this.EnsureWrite(8);
					this.Bytes[this.WriteIndex + 7] = x.low;
					this.Bytes[this.WriteIndex + 6] = (x.low >> 8);
					this.Bytes[this.WriteIndex + 5] = (x.low >> 16);
					this.Bytes[this.WriteIndex + 4] = (x.low >> 24);
					this.Bytes[this.WriteIndex + 3] = (x.high & 0xff);
					this.Bytes[this.WriteIndex + 2] = (x.high >> 8);
					this.Bytes[this.WriteIndex + 1] = (x.high >> 16);
					this.Bytes[this.WriteIndex] = 0xfe;
					this.WriteIndex += 8;
				}
				else // 1111 1111
				{
					this.EnsureWrite(9);
					this.Bytes[this.WriteIndex + 8] = x.low;
					this.Bytes[this.WriteIndex + 7] = (x.low >> 8);
					this.Bytes[this.WriteIndex + 6] = (x.low >> 16);
					this.Bytes[this.WriteIndex + 5] = (x.low >> 24);
					this.Bytes[this.WriteIndex + 4] = (x.high & 0xff);
					this.Bytes[this.WriteIndex + 3] = (x.high >> 8);
					this.Bytes[this.WriteIndex + 2] = (x.high >> 16);
					this.Bytes[this.WriteIndex + 1] = (x.high >> 24);
					this.Bytes[this.WriteIndex] = 0xff;
					this.WriteIndex += 9;
				}
			}
		}

        ReadUlong() : Long {
			this.EnsureRead(1);
			var h = this.Bytes[this.ReadIndex];
			if (h < 0x80) {
				this.ReadIndex++;
				return h;
			}
			else if (h < 0xc0) {
				this.EnsureRead(2);
				var x = ((h & 0x3f) << 8) | this.Bytes[this.ReadIndex + 1];
				this.ReadIndex += 2;
				return new Long(x, 0, true);
			}
			else if (h < 0xe0) {
				this.EnsureRead(3);
				var x = ((h & 0x1f) << 16) | (this.Bytes[this.ReadIndex + 1] << 8) | this.Bytes[this.ReadIndex + 2];
				this.ReadIndex += 3;
				return new Long(x, 0, true);
			}
			else if (h < 0xf0) {
				this.EnsureRead(4);
				var x = ((h & 0x0f) << 24) | (this.Bytes[this.ReadIndex + 1] << 16) | (this.Bytes[this.ReadIndex + 2] << 8) | this.Bytes[this.ReadIndex + 3];
				this.ReadIndex += 4;
				return new Long(x, 0, true);
			}
			else if (h < 0xf8) {
				this.EnsureRead(5);
				var xl = (this.Bytes[this.ReadIndex + 1] << 24) | ((this.Bytes[this.ReadIndex + 2] << 16)) | (this.Bytes[this.ReadIndex + 3] << 8) | (this.Bytes[this.ReadIndex + 4]);
				var xh = h & 0x07;
				this.ReadIndex += 5;
				return new Long(xl, xh, true);
			}
			else if (h < 0xfc) {
				this.EnsureRead(6);
				var xl = (this.Bytes[this.ReadIndex + 2] << 24) | ((this.Bytes[this.ReadIndex + 3] << 16)) | (this.Bytes[this.ReadIndex + 4] << 8) | (this.Bytes[this.ReadIndex + 5]);
				var xh = ((h & 0x03) << 8) | this.Bytes[this.ReadIndex + 1];
				this.ReadIndex += 6;
				return new Long(xl, xh, true);
			}
			else if (h < 0xfe) {
				this.EnsureRead(7);
				var xl = (this.Bytes[this.ReadIndex + 3] << 24) | ((this.Bytes[this.ReadIndex + 4] << 16)) | (this.Bytes[this.ReadIndex + 5] << 8) | (this.Bytes[this.ReadIndex + 6]);
				var xh = ((h & 0x01) << 16) | (this.Bytes[this.ReadIndex + 1] << 8) | this.Bytes[this.ReadIndex + 2];
				this.ReadIndex += 7;
				return new Long(xl, xh, true);
			}
			else if (h < 0xff) {
				this.EnsureRead(8);
				var xl = (this.Bytes[this.ReadIndex + 4] << 24) | ((this.Bytes[this.ReadIndex + 5] << 16)) | (this.Bytes[this.ReadIndex + 6] << 8) | (this.Bytes[this.ReadIndex + 7]);
				var xh = /*((h & 0x01) << 24) |*/ (this.Bytes[this.ReadIndex + 1] << 16) | (this.Bytes[this.ReadIndex + 2] << 8) | this.Bytes[this.ReadIndex + 3];
				this.ReadIndex += 8;
				return new Long(xl, xh, true);
			}
			else {
				this.EnsureRead(9);
				var xl = (this.Bytes[this.ReadIndex + 5] << 24) | (this.Bytes[this.ReadIndex + 6] << 16) | (this.Bytes[this.ReadIndex + 7] << 8) | (this.Bytes[this.ReadIndex + 8]);
				var xh = (this.Bytes[this.ReadIndex + 1] << 24) | (this.Bytes[this.ReadIndex + 2] << 16) | (this.Bytes[this.ReadIndex + 3] << 8) | this.Bytes[this.ReadIndex + 4];
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
			this.WriteBytes(utf8.buffer, 0, utf8.length);
		}

		public ReadString(): string {
			var n = this.ReadInt();
			this.EnsureRead(n);
			var x = ByteBuffer.Decoder.decode(this.Bytes.slice(this.ReadIndex, this.ReadIndex + n));
			this.ReadIndex += n;
			return x;
		}

		public WriteBytes(x: ArrayBuffer, offset: number = 0, length: number = -1): void {
			if (length == -1)
				length = x.byteLength;
			this.WriteInt(length);
			this.EnsureWrite(length);
			this.BlockCopy(x, offset, this.Bytes, this.WriteIndex, length);
			this.WriteIndex += length;
		}

		public ReadBytes(): ArrayBuffer {
			var n = this.ReadInt();
			this.EnsureRead(n);
			var x = this.Bytes.slice(this.ReadIndex, this.ReadIndex + n);
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

		// for debug
		public toString(): string {
			var ss = "";
			for (var i = this.ReadIndex; i < this.WriteIndex; ++i) {
				ss = ss.concat(this.Bytes[i].toString() + " ");
			}
			return ss;
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
