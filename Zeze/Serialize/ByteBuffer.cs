using System;
using System.Collections.Generic;
using System.Text;
using System.Runtime.CompilerServices;
using System.Diagnostics.CodeAnalysis;

namespace Zeze.Serialize
{
    public class ByteBuffer
    {
#pragma warning disable CA1819 // 属性不应返回数组
        public byte[] Bytes { get; private set; }
#pragma warning restore CA1819
        public int ReadIndex { get; set; }
        public int WriteIndex { get; set; }
        public int Capacity { get { return Bytes.Length; } }
        public int Size { get { return WriteIndex - ReadIndex; } }
 
        public ByteBuffer() : this(1024)
        {
        }

        public ByteBuffer(int capacity)
        {
            this.Bytes = new byte[PropSize(capacity)];
            this.ReadIndex = 0;
            this.WriteIndex = 0;
        }

        ByteBuffer(byte[] bytes) : this(bytes, 0, bytes.Length)
        {
        }

        ByteBuffer(byte[] bytes, int readIndex, int writeIndex)
        {
            /*
            if (readIndex < 0 || readIndex >= bytes.Length)
                throw new Exception();
            if (writeIndex < 0 || writeIndex > bytes.Length)
                throw new Exception();
            if (readIndex > writeIndex)
                throw new Exception();
            */

            this.Bytes = bytes;
            this.ReadIndex = readIndex;
            this.WriteIndex = writeIndex;
        }

        public static ByteBuffer Wrap(byte[] bytes)
        {
            return new ByteBuffer(bytes, 0, bytes.Length);
        }

        public static ByteBuffer Wrap(byte[] bytes, int offset, int length)
        {
            return new ByteBuffer(bytes, offset, offset + length);
        }

        [MethodImpl(MethodImplOptions.AggressiveInlining)]
        public void Append(byte[] bs)
        {
            Append(bs, 0, bs.Length);
        }

        [MethodImpl(MethodImplOptions.AggressiveInlining)]
        public void Append(byte[] bs, int offset, int len)
        {
            EnsureWrite(len);
            Buffer.BlockCopy(bs, offset, Bytes, WriteIndex, len);
            WriteIndex += len;
        }

        [MethodImpl(MethodImplOptions.AggressiveInlining)]
        public void Replace(int writeIndex, byte[] src)
        {
            Replace(writeIndex, src, 0, src.Length);
        }

        [MethodImpl(MethodImplOptions.AggressiveInlining)]
        public void Replace(int writeIndex, byte[] src, int offset, int len)
        {
            if (writeIndex < this.ReadIndex || writeIndex + len > this.WriteIndex)
                throw new Exception();
            Buffer.BlockCopy(src, offset, this.Bytes, writeIndex, len);
        }

        [MethodImpl(MethodImplOptions.AggressiveInlining)]
        public void Reset()
        {
            ReadIndex = WriteIndex = 0;
        }

        [MethodImpl(MethodImplOptions.AggressiveInlining)]
        private static int PropSize(int needSize)
        {
            int size = 1024;
            for (; size < needSize; size <<= 1)
            {
            }
            return size;
        }


        [MethodImpl(MethodImplOptions.AggressiveInlining)]
        private void EnsureWrite(int size)
        {
            int newSize = WriteIndex + size;
            if (newSize > Capacity)
            {
                byte[] newBytes = new byte[PropSize(newSize)];
                WriteIndex -= ReadIndex;
                Buffer.BlockCopy(Bytes, ReadIndex, newBytes, 0, WriteIndex);
                ReadIndex = 0;
                Bytes = newBytes;
            }
        }

        [MethodImpl(MethodImplOptions.AggressiveInlining)]
        private void EnsureRead(int size)
        {
            if (ReadIndex + size > WriteIndex)
                 throw new Exception();
        }

        public void WriteBool(bool b)
        {
            EnsureWrite(1);
            Bytes[WriteIndex++] = (byte)(b ? 1 : 0);
        }

        public bool ReadBool()
        {
            EnsureRead(1);
            return Bytes[ReadIndex++] != 0;
        }

        public void WriteByte(byte x)
        {
            EnsureWrite(1);
            Bytes[WriteIndex++] = x;
        }

        public byte ReadByte()
        {
            EnsureRead(1);
            return Bytes[ReadIndex++];
        }


        public void WriteShort(short x)
        {
            if (x >= 0)
            {
                if (x < 0x80)
                {
                    EnsureWrite(1);
                    Bytes[WriteIndex++] = (byte)x;
                    return;
                }

                if (x < 0x4000)
                {
                    EnsureWrite(2);
                    Bytes[WriteIndex + 1] = (byte)x;
                    Bytes[WriteIndex] = (byte)((x >> 8) | 0x80);
                    WriteIndex += 2;
                    return;
                }
            }
            EnsureWrite(3);
            Bytes[WriteIndex] = 0xff;
            Bytes[WriteIndex + 2] = (byte)x;
            Bytes[WriteIndex + 1] = (byte)(x >> 8);
            WriteIndex += 3;
        }

        public short ReadShort()
        {
            EnsureRead(1);
            int h = Bytes[ReadIndex];
            if (h < 0x80)
            {
                ReadIndex++;
                return (short)h;
            }
            if (h < 0xc0)
            {
                EnsureRead(2);
                int x = ((h & 0x3f) << 8) | Bytes[ReadIndex + 1];
                ReadIndex += 2;
                return (short)x;
            }
            if ((h == 0xff))
            {
                EnsureRead(3);
                int x = (Bytes[ReadIndex + 1] << 8) | Bytes[ReadIndex + 2];
                ReadIndex += 3;
                return (short)x;
            }
            throw new Exception();
        }

        [MethodImpl(MethodImplOptions.AggressiveInlining)]
        public void WriteInt4(int x)
        {
            byte[] bs = BitConverter.GetBytes(x);
            //if (false == BitConverter.IsLittleEndian)
            //    Array.Reverse(bs);
            Append(bs);
        }

        [MethodImpl(MethodImplOptions.AggressiveInlining)]
        public int ReadInt4()
        {
            EnsureRead(4);
            int x = BitConverter.ToInt32(Bytes, ReadIndex);
            ReadIndex += 4;
            return x;
        }

        [MethodImpl(MethodImplOptions.AggressiveInlining)]
        public void WriteLong8(long x)
        {
            byte[] bs = BitConverter.GetBytes(x);
            //if (false == BitConverter.IsLittleEndian)
            //    Array.Reverse(bs);
            Append(bs);
        }

        [MethodImpl(MethodImplOptions.AggressiveInlining)]
        public long ReadLong8()
        {
            EnsureRead(8);
            long x = BitConverter.ToInt64(Bytes, ReadIndex);
            ReadIndex += 8;
            return x;
        }

        [MethodImpl(MethodImplOptions.AggressiveInlining)]
        public void WriteInt(int x)
        {
            WriteUint((uint)x);
        }

        [MethodImpl(MethodImplOptions.AggressiveInlining)]
        public int ReadInt()
        {
            return (int)ReadUint();
        }

        private void WriteUint(uint x)
        {
            // 0 111 1111
            if (x < 0x80)
            {
                EnsureWrite(1);
                Bytes[WriteIndex++] = (byte)x;
            }
            else if (x < 0x4000) // 10 11 1111, -
            {
                EnsureWrite(2);
                Bytes[WriteIndex + 1] = (byte)x;
                Bytes[WriteIndex] = (byte)((x >> 8) | 0x80);
                WriteIndex += 2;
            }
            else if (x < 0x200000) // 110 1 1111, -,-
            {
                EnsureWrite(3);
                Bytes[WriteIndex + 2] = (byte)x;
                Bytes[WriteIndex + 1] = (byte)(x >> 8);
                Bytes[WriteIndex] = (byte)((x >> 16) | 0xc0);
                WriteIndex += 3;
            }
            else if (x < 0x10000000) // 1110 1111,-,-,-
            {
                EnsureWrite(4);
                Bytes[WriteIndex + 3] = (byte)x;
                Bytes[WriteIndex + 2] = (byte)(x >> 8);
                Bytes[WriteIndex + 1] = (byte)(x >> 16);
                Bytes[WriteIndex] = (byte)((x >> 24) | 0xe0);
                WriteIndex += 4;
            }
            else
            {
                EnsureWrite(5);
                Bytes[WriteIndex] = 0xf0;
                Bytes[WriteIndex + 4] = (byte)x;
                Bytes[WriteIndex + 3] = (byte)(x >> 8);
                Bytes[WriteIndex + 2] = (byte)(x >> 16);
                Bytes[WriteIndex + 1] = (byte)(x >> 24);
                WriteIndex += 5;
            }
        }

        private uint ReadUint()
        {
            EnsureRead(1);
            uint h = Bytes[ReadIndex];
            if (h < 0x80)
            {
                ReadIndex++;
                return h;
            }
            else if (h < 0xc0)
            {
                EnsureRead(2);
                uint x = ((h & 0x3f) << 8) | Bytes[ReadIndex + 1];
                ReadIndex += 2;
                return x;
            }
            else if (h < 0xe0)
            {
                EnsureRead(3);
                uint x = ((h & 0x1f) << 16) | ((uint)Bytes[ReadIndex + 1] << 8) | Bytes[ReadIndex + 2];
                ReadIndex += 3;
                return x;
            }
            else if (h < 0xf0)
            {

                EnsureRead(4);
                uint x = ((h & 0x0f) << 24) | ((uint)Bytes[ReadIndex + 1] << 16) | ((uint)Bytes[ReadIndex + 2] << 8) | Bytes[ReadIndex + 3];
                ReadIndex += 4;
                return x;
            }
            else
            {
                EnsureRead(5);
                uint x = ((uint)Bytes[ReadIndex + 1] << 24) | ((uint)(Bytes[ReadIndex + 2] << 16)) | ((uint)Bytes[ReadIndex + 3] << 8) | Bytes[ReadIndex + 4];
                ReadIndex += 5;
                return x;
            }
        }

        [MethodImpl(MethodImplOptions.AggressiveInlining)]
        public void WriteLong(long x)
        {
            WriteUlong((ulong)x);
        }

        [MethodImpl(MethodImplOptions.AggressiveInlining)]
        public long ReadLong()
        {
            return (long)ReadUlong();
        }

        private void WriteUlong(ulong x)
        {
            // 0 111 1111
            if (x < 0x80)
            {
                EnsureWrite(1);
                Bytes[WriteIndex++] = (byte)x;
            }
            else if (x < 0x4000) // 10 11 1111, -
            {
                EnsureWrite(2);
                Bytes[WriteIndex + 1] = (byte)x;
                Bytes[WriteIndex] = (byte)((x >> 8) | 0x80);
                WriteIndex += 2;
            }
            else if (x < 0x200000) // 110 1 1111, -,-
            {
                EnsureWrite(3);
                Bytes[WriteIndex + 2] = (byte)x;
                Bytes[WriteIndex + 1] = (byte)(x >> 8);
                Bytes[WriteIndex] = (byte)((x >> 16) | 0xc0);
                WriteIndex += 3;
            }
            else if (x < 0x10000000) // 1110 1111,-,-,-
            {
                EnsureWrite(4);
                Bytes[WriteIndex + 3] = (byte)x;
                Bytes[WriteIndex + 2] = (byte)(x >> 8);
                Bytes[WriteIndex + 1] = (byte)(x >> 16);
                Bytes[WriteIndex] = (byte)((x >> 24) | 0xe0);
                WriteIndex += 4;
            }
            else if (x < 0x800000000L) // 1111 0xxx,-,-,-,-
            {
                EnsureWrite(5);
                Bytes[WriteIndex + 4] = (byte)x;
                Bytes[WriteIndex + 3] = (byte)(x >> 8);
                Bytes[WriteIndex + 2] = (byte)(x >> 16);
                Bytes[WriteIndex + 1] = (byte)(x >> 24);
                Bytes[WriteIndex] = (byte)((x >> 32) | 0xf0);
                WriteIndex += 5;
            }
            else if (x < 0x40000000000L) // 1111 10xx, 
            {
                EnsureWrite(6);
                Bytes[WriteIndex + 5] = (byte)x;
                Bytes[WriteIndex + 4] = (byte)(x >> 8);
                Bytes[WriteIndex + 3] = (byte)(x >> 16);
                Bytes[WriteIndex + 2] = (byte)(x >> 24);
                Bytes[WriteIndex + 1] = (byte)(x >> 32);
                Bytes[WriteIndex] = (byte)((x >> 40) | 0xf8);
                WriteIndex += 6;
            }
            else if (x < 0x200000000000L) // 1111 110x,
            {
                EnsureWrite(7);
                Bytes[WriteIndex + 6] = (byte)x;
                Bytes[WriteIndex + 5] = (byte)(x >> 8);
                Bytes[WriteIndex + 4] = (byte)(x >> 16);
                Bytes[WriteIndex + 3] = (byte)(x >> 24);
                Bytes[WriteIndex + 2] = (byte)(x >> 32);
                Bytes[WriteIndex + 1] = (byte)(x >> 40);
                Bytes[WriteIndex] = (byte)((x >> 48) | 0xfc);
                WriteIndex += 7;
            }
            else if (x < 0x100000000000000L) // 1111 1110
            {
                EnsureWrite(8);
                Bytes[WriteIndex + 7] = (byte)x;
                Bytes[WriteIndex + 6] = (byte)(x >> 8);
                Bytes[WriteIndex + 5] = (byte)(x >> 16);
                Bytes[WriteIndex + 4] = (byte)(x >> 24);
                Bytes[WriteIndex + 3] = (byte)(x >> 32);
                Bytes[WriteIndex + 2] = (byte)(x >> 40);
                Bytes[WriteIndex + 1] = (byte)(x >> 48);
                Bytes[WriteIndex] = 0xfe;
                WriteIndex += 8;
            }
            else // 1111 1111
            {
                EnsureWrite(9);
                Bytes[WriteIndex] = 0xff;
                Bytes[WriteIndex + 8] = (byte)x;
                Bytes[WriteIndex + 7] = (byte)(x >> 8);
                Bytes[WriteIndex + 6] = (byte)(x >> 16);
                Bytes[WriteIndex + 5] = (byte)(x >> 24);
                Bytes[WriteIndex + 4] = (byte)(x >> 32);
                Bytes[WriteIndex + 3] = (byte)(x >> 40);
                Bytes[WriteIndex + 2] = (byte)(x >> 48);
                Bytes[WriteIndex + 1] = (byte)(x >> 56);
                WriteIndex += 9;
            }
        }

        private ulong ReadUlong()
        {
            EnsureRead(1);
            uint h = Bytes[ReadIndex];
            if (h < 0x80)
            {
                ReadIndex++;
                return h;
            }
            else if (h < 0xc0)
            {
                EnsureRead(2);
                uint x = ((h & 0x3f) << 8) | Bytes[ReadIndex + 1];
                ReadIndex += 2;
                return x;
            }
            else if (h < 0xe0)
            {
                EnsureRead(3);
                uint x = ((h & 0x1f) << 16) | ((uint)Bytes[ReadIndex + 1] << 8) | Bytes[ReadIndex + 2];
                ReadIndex += 3;
                return x;
            }
            else if (h < 0xf0)
            {
                EnsureRead(4);
                uint x = ((h & 0x0f) << 24) | ((uint)Bytes[ReadIndex + 1] << 16) | ((uint)Bytes[ReadIndex + 2] << 8) | Bytes[ReadIndex + 3];
                ReadIndex += 4;
                return x;
            }
            else if (h < 0xf8)
            {
                EnsureRead(5);
                uint xl = ((uint)Bytes[ReadIndex + 1] << 24) | ((uint)(Bytes[ReadIndex + 2] << 16)) | ((uint)Bytes[ReadIndex + 3] << 8) | (Bytes[ReadIndex + 4]);
                uint xh = h & 0x07;
                ReadIndex += 5;
                return ((ulong)xh << 32) | xl;
            }
            else if (h < 0xfc)
            {
                EnsureRead(6);
                uint xl = ((uint)Bytes[ReadIndex + 2] << 24) | ((uint)(Bytes[ReadIndex + 3] << 16)) | ((uint)Bytes[ReadIndex + 4] << 8) | (Bytes[ReadIndex + 5]);
                uint xh = ((h & 0x03) << 8) | Bytes[ReadIndex + 1];
                ReadIndex += 6;
                return ((ulong)xh << 32) | xl;
            }
            else if (h < 0xfe)
            {
                EnsureRead(7);
                uint xl = ((uint)Bytes[ReadIndex + 3] << 24) | ((uint)(Bytes[ReadIndex + 4] << 16)) | ((uint)Bytes[ReadIndex + 5] << 8) | (Bytes[ReadIndex + 6]);
                uint xh = ((h & 0x01) << 16) | ((uint)Bytes[ReadIndex + 1] << 8) | Bytes[ReadIndex + 2];
                ReadIndex += 7;
                return ((ulong)xh << 32) | xl;
            }
            else if (h < 0xff)
            {
                EnsureRead(8);
                uint xl = ((uint)Bytes[ReadIndex + 4] << 24) | ((uint)(Bytes[ReadIndex + 5] << 16)) | ((uint)Bytes[ReadIndex + 6] << 8) | (Bytes[ReadIndex + 7]);
                uint xh = /*((h & 0x01) << 24) |*/ ((uint)Bytes[ReadIndex + 1] << 16) | ((uint)Bytes[ReadIndex + 2] << 8) | Bytes[ReadIndex + 3];
                ReadIndex += 8;
                return ((ulong)xh << 32) | xl;
            }
            else
            {
                EnsureRead(9);
                uint xl = ((uint)Bytes[ReadIndex + 5] << 24) | ((uint)(Bytes[ReadIndex + 6] << 16)) | ((uint)Bytes[ReadIndex + 7] << 8) | (Bytes[ReadIndex + 8]);
                uint xh = ((uint)Bytes[ReadIndex + 1] << 24) | ((uint)Bytes[ReadIndex + 2] << 16) | ((uint)Bytes[ReadIndex + 3] << 8) | Bytes[ReadIndex + 4];
                ReadIndex += 9;
                return ((ulong)xh << 32) | xl;
            }
        }

        [MethodImpl(MethodImplOptions.AggressiveInlining)]
        public void WriteFloat(float x)
        {
            byte[] bs = BitConverter.GetBytes(x);
            //if (false == BitConverter.IsLittleEndian)
            //    Array.Reverse(bs);
            Append(bs);
        }

        [MethodImpl(MethodImplOptions.AggressiveInlining)]
        public float ReadFloat()
        {
            EnsureRead(4);
            float x = BitConverter.ToSingle(Bytes, ReadIndex);
            ReadIndex += 4;
            return x;
        }

        [MethodImpl(MethodImplOptions.AggressiveInlining)]
        public void WriteDouble(double x)
        {
            byte[] bs = BitConverter.GetBytes(x);
            //if (false == BitConverter.IsLittleEndian)
            //    Array.Reverse(bs);
            Append(bs);
        }

        [MethodImpl(MethodImplOptions.AggressiveInlining)]
        public double ReadDouble()
        {
            EnsureRead(8);
            double x = BitConverter.ToDouble(Bytes, ReadIndex);
            ReadIndex += 8;
            return x;
        }

        [MethodImpl(MethodImplOptions.AggressiveInlining)]
        public void WriteString(string x)
        {
            WriteBytes(Encoding.UTF8.GetBytes(x));
        }

        public string ReadString()
        {
            int n = ReadInt();
            EnsureRead(n);
            string x = Encoding.UTF8.GetString(Bytes, ReadIndex, n);
            ReadIndex += n;
            return x;
        }

        [MethodImpl(MethodImplOptions.AggressiveInlining)]
        public void WriteBytes(byte[] x)
        {
            WriteBytes(x, 0, x.Length);
        }

        public void WriteBytes(byte[] x, int offset, int length)
        {
            WriteInt(length);
            EnsureWrite(length);
            Buffer.BlockCopy(x, offset, Bytes, WriteIndex, length);
            WriteIndex += length;
        }

        public byte[] ReadBytes()
        {
            int n = ReadInt();
            EnsureRead(n);
            byte[] x = new byte[n];
            Buffer.BlockCopy(Bytes, ReadIndex, x, 0, n);
            ReadIndex += n;
            return x;
        }

        [MethodImpl(MethodImplOptions.AggressiveInlining)]
        public void SkipBytes()
        {
            int n = ReadInt();
            EnsureRead(n);
            ReadIndex += n;
        }

        [MethodImpl(MethodImplOptions.AggressiveInlining)]
        public ByteBuffer ReadByteBuffer()
        {
            return ByteBuffer.Wrap(ReadBytes());
        }

        [MethodImpl(MethodImplOptions.AggressiveInlining)]
        public void WriteByteBuffer(ByteBuffer o)
        {
            WriteBytes(o.Bytes, o.ReadIndex, o.Size);
        }

        public override string ToString()
        {
            return BitConverter.ToString(Bytes, ReadIndex, Size);
        }

        public override bool Equals(object obj)
        {
            return (obj is ByteBuffer other) && Equals(other);
        }

        public bool Equals([AllowNull] ByteBuffer other)
        {
            if (other == null)
                return false;

            if (this.Size != other.Size)
                return false;

            for (int i = 0, n = this.Size; i < n; i++)
            {
                if (Bytes[ReadIndex + i] != other.Bytes[other.ReadIndex + i])
                    return false;
            }

            return true;
        }
        public override int GetHashCode()
        {
            throw new NotImplementedException();
        }
    }
}
