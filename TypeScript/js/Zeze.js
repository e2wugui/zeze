import Long from "long.js";
export var Zeze;
(function (Zeze) {
    class EmptyBean {
        TypeId() {
            return EmptyBean.TYPEID;
        }
        Encode(_os_) {
            //_os_.WriteInt(0); // ����Bean��ϵ�л���ʽӦ��д��0������������ʷԭ�򣬲�дҲ���ԣ����⴦���ˡ�
        }
        Decode(_os_) {
            //_os_.ReadInt();
        }
    }
    EmptyBean.TYPEID = new Long(0, 0, true);
    Zeze.EmptyBean = EmptyBean;
    class Protocol {
        TypeId() {
            return this.ModuleId() << 16 | this.ProtocolId();
        }
        Encode(_os_) {
            _os_.WriteInt(this.ResultCode);
            this.Argument.Encode(_os_);
        }
        Decode(_os_) {
            this.ResultCode = _os_.ReadInt();
            this.Argument.Decode(_os_);
        }
        Send() {
            // TODO access service or connection
        }
    }
    Zeze.Protocol = Protocol;
    class ByteBuffer {
        constructor(buffer = null) {
            this.Bytes = (null == buffer) ? new Uint8Array(1024) : buffer;
            this.View = new DataView(this.Bytes.buffer);
            this.ReadIndex = 0;
            this.WriteIndex = 0;
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
        BlockCopy(src, srcOffset, dst, dstOffset, count) {
            for (var i = 0; i < count; ++i) {
                dst[i + dstOffset] = src[i + srcOffset];
            }
        }
        EnsureWrite(size) {
            var newSize = this.WriteIndex + size;
            if (newSize > this.Capacity()) {
                var newBytes = new Uint8Array(this.ToPower2(newSize));
                this.WriteIndex -= this.ReadIndex;
                this.BlockCopy(this.Bytes, this.ReadIndex, newBytes, 0, this.WriteIndex);
                this.ReadIndex = 0;
                this.Bytes = newBytes;
                this.View = new DataView(this.Bytes.buffer);
            }
        }
        Append(bytes, offset, len) {
            this.EnsureWrite(len);
            this.BlockCopy(bytes, offset, this.Bytes, this.WriteIndex, len);
            this.WriteIndex += len;
        }
        Replace(writeIndex, src, srcOffset, len) {
            if (writeIndex < this.ReadIndex || writeIndex + len > this.WriteIndex)
                throw new Error();
            this.BlockCopy(src, srcOffset, this.Bytes, writeIndex, len);
        }
        BeginWriteWithSize4() {
            var state = this.WriteIndex;
            this.EnsureWrite(4);
            this.WriteIndex += 4;
            return state;
        }
        EndWriteWithSize4(state) {
            this.View.setInt32(state, this.WriteIndex - state - 4, true);
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
                    this.BlockCopy(this.Bytes, this.ReadIndex, this.Bytes, 0, size);
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
            this.View.setUint32(this.WriteIndex, x.low, true);
            this.View.setUint32(this.WriteIndex + 4, x.high, true);
            this.WriteIndex += 8;
        }
        ReadLong8() {
            this.EnsureRead(8);
            var low32 = this.View.getUint32(this.ReadIndex, true);
            var high32 = this.View.getUint32(this.ReadIndex + 4, true);
            this.ReadIndex += 8;
            return new Long(low32, high32, true);
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
            this.WriteUlong(x);
        }
        ReadLong() {
            return this.ReadUlong();
        }
        WriteUlong(x) {
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
        ReadUlong() {
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
            this.BlockCopy(x, offset, this.Bytes, this.WriteIndex, length);
            this.WriteIndex += length;
        }
        ReadBytes() {
            var n = this.ReadInt();
            this.EnsureRead(n);
            var x = new Uint8Array(n);
            this.BlockCopy(this.Bytes, this.ReadIndex, x, 0, n);
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
    ByteBuffer.Encoder = new TextEncoder();
    ByteBuffer.Decoder = new TextDecoder();
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
})(Zeze || (Zeze = {}));
