using System;
using System.Collections.Generic;
using System.Text;
using System.Runtime.CompilerServices;
using Zeze.Net;
using Zeze.Util;

namespace Zeze.Serialize
{
#pragma warning disable CS0162 // Code is heuristically unreachable
    // ReSharper disable HeuristicUnreachableCode
    // ReSharper disable IntVariableOverflowInUncheckedContext
    public class ByteBuffer
    {
        public const bool IGNORE_INCOMPATIBLE_FIELD = false;

        public byte[] Bytes { get; private set; }
        public int ReadIndex { get; set; }
        public int WriteIndex { get; set; }
        public int Capacity => Bytes.Length;
        public int Size => WriteIndex - ReadIndex;

        // decode
        public static ByteBuffer Wrap(ByteBuffer other)
        {
            return Wrap(other.Bytes, other.ReadIndex, other.Size);
        }

        public static ByteBuffer Wrap(byte[] bytes)
        {
            return new ByteBuffer(bytes, 0, bytes.Length);
        }

        public static ByteBuffer Wrap(byte[] bytes, int offset, int length)
        {
            VerifyArrayIndex(bytes, offset, length);
            return new ByteBuffer(bytes, offset, offset + length);
        }

        public static ByteBuffer Wrap(Binary binary)
        {
            return Wrap(binary.Bytes, binary.Offset, binary.Count);
        }

        // encode
        public static ByteBuffer Allocate(int capacity = 16)
        {
            return new ByteBuffer(capacity == 0 ? Array.Empty<byte>() : new byte[capacity]);
        }

        public static ByteBuffer Allocate(byte[] initBytes)
        {
            return new ByteBuffer(initBytes);
        }

        // encode
        protected ByteBuffer(byte[] initBytes)
        {
            Bytes = initBytes;
            ReadIndex = 0;
            WriteIndex = 0;
        }

        // decode
        protected ByteBuffer(byte[] bytes, int readIndex, int writeIndex)
        {
            Bytes = bytes;
            ReadIndex = readIndex;
            WriteIndex = writeIndex;
        }

        [MethodImpl(MethodImplOptions.AggressiveInlining)]
        public void FreeInternalBuffer()
        {
            Bytes = Array.Empty<byte>();
            Reset();
        }

        [MethodImpl(MethodImplOptions.AggressiveInlining)]
        public ByteBuffer Wraps(byte[] bytes)
        {
            Wraps(bytes, 0, bytes.Length);
            return this;
        }

        [MethodImpl(MethodImplOptions.AggressiveInlining)]
        public void Wraps(byte[] bytes, int offset, int length)
        {
            VerifyArrayIndex(bytes, offset, length);
            Bytes = bytes;
            ReadIndex = offset;
            WriteIndex = offset + length;
        }

        [MethodImpl(MethodImplOptions.AggressiveInlining)]
        public void Append(byte b)
        {
            EnsureWrite(1);
            Bytes[WriteIndex++] = b;
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
            if (writeIndex < ReadIndex || writeIndex + len > WriteIndex)
            {
                throw new Exception("Replace writeIndex=" + writeIndex + ", len=" + len
                                    + " at " + ReadIndex + '/' + WriteIndex);
            }
            Buffer.BlockCopy(src, offset, Bytes, writeIndex, len);
        }

        [MethodImpl(MethodImplOptions.AggressiveInlining)]
        public void BeginWriteWithSize4(out int state)
        {
            state = Size;
            EnsureWrite(4);
            WriteIndex += 4;
        }

        [MethodImpl(MethodImplOptions.AggressiveInlining)]
        public void EndWriteWithSize4(int state)
        {
            var oldWriteIndex = state + ReadIndex;
            Replace(oldWriteIndex, BitConverter.GetBytes(WriteIndex - oldWriteIndex - 4));
        }

        /// <summary>
        /// 这个方法把剩余可用数据移到buffer开头。
        /// 【注意】这个方法会修改ReadIndex，WriteIndex。
        /// 最好仅在全部读取写入处理完成以后调用处理一次，
        /// 为下一次写入读取做准备。
        /// </summary>
        [MethodImpl(MethodImplOptions.AggressiveInlining)]
        public void Campact()
        {
            int size = Size;
            if (size > 0)
            {
                if (ReadIndex > 0)
                {
                    Buffer.BlockCopy(Bytes, ReadIndex, Bytes, 0, size);
                    ReadIndex = 0;
                    WriteIndex = size;
                }
            }
            else
                Reset();
        }

        [MethodImpl(MethodImplOptions.AggressiveInlining)]
        public byte[] Copy()
        {
            int size = Size;
            if (size == 0)
                return Array.Empty<byte>();
            byte[] copy = new byte[size];
            Buffer.BlockCopy(Bytes, ReadIndex, copy, 0, size);
            return copy;
        }

        [MethodImpl(MethodImplOptions.AggressiveInlining)]
        public void Reset()
        {
            ReadIndex = 0;
            WriteIndex = 0;
        }

        [MethodImpl(MethodImplOptions.AggressiveInlining)]
        protected int ToPower2(int needSize)
        {
            if ((uint)needSize > 0x4000_0000u)
                throw new Exception("invalid needSize=" + needSize + " at " + ReadIndex + '/' + WriteIndex);
            int size = 16;
            while (size < needSize)
                size <<= 1;
            return size;
        }

        [MethodImpl(MethodImplOptions.AggressiveInlining)]
        public void EnsureWrite(int size)
        {
            int newSize = WriteIndex + size;
            if (newSize > Capacity)
            {
                byte[] newBytes = new byte[ToPower2(newSize)];
                Buffer.BlockCopy(Bytes, ReadIndex, newBytes, 0, WriteIndex -= ReadIndex);
                ReadIndex = 0;
                Bytes = newBytes;
            }
        }

        [MethodImpl(MethodImplOptions.AggressiveInlining)]
        protected void EnsureRead(int size)
        {
            if (ReadIndex + size > WriteIndex)
                throw new Exception("EnsureRead " + ReadIndex + '+' + size + " > " + WriteIndex);
        }

        public void WriteBool(bool b)
        {
            EnsureWrite(1);
            Bytes[WriteIndex++] = (byte)(b ? 1 : 0);
        }

        public bool ReadBool()
        {
            EnsureRead(1);
            int b = Bytes[ReadIndex];
            if ((b & ~1) == 0) // fast-path
            {
                ReadIndex++;
                return b != 0;
            }
            return ReadLong() != 0; // rare-path
        }

        public void WriteByte(byte x)
        {
            EnsureWrite(1);
            Bytes[WriteIndex++] = x;
        }

        public void WriteByte(int x)
        {
            EnsureWrite(1);
            Bytes[WriteIndex++] = (byte)x;
        }

        public byte ReadByte()
        {
            EnsureRead(1);
            return Bytes[ReadIndex++];
        }

        [MethodImpl(MethodImplOptions.AggressiveInlining)]
        public void WriteInt4(int x)
        {
            byte[] bs = BitConverter.GetBytes(x);
            //if (!BitConverter.IsLittleEndian)
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
            //if (!BitConverter.IsLittleEndian)
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

        // v看成无符号数时,与WriteULongSize的结果一致,即相当于WriteULongSize((long)(uint)v);
        public static int WriteUIntSize(int v)
        {
            long u = (uint)v;
            //@formatter:off
            if (u <        0x80) return 1;
            if (u <      0x4000) return 2;
            if (u <   0x20_0000) return 3;
            if (u < 0x1000_0000) return 4;
            return 5;
            //@formatter:on
        }

        // v看成无符号数时,与WriteULong的结果一致,即相当于WriteULong((long)(uint)v);
        public void WriteUInt(int x)
        {
            uint u = (uint)x;
            if (u < 0x80)
            {
                EnsureWrite(1); // 0xxx xxxx
                Bytes[WriteIndex++] = (byte)u;
            }
            else if (u < 0x4000)
            {
                EnsureWrite(2); // 10xx xxxx +1B
                byte[] bytes = Bytes;
                int writeIndex = WriteIndex;
                bytes[writeIndex] = (byte)((u >> 8) + 0x80);
                bytes[writeIndex + 1] = (byte)u;
                WriteIndex = writeIndex + 2;
            }
            else if (u < 0x20_0000)
            {
                EnsureWrite(3); // 110x xxxx +2B
                byte[] bytes = Bytes;
                int writeIndex = WriteIndex;
                bytes[writeIndex] = (byte)((u >> 16) + 0xc0);
                bytes[writeIndex + 1] = (byte)(u >> 8);
                bytes[writeIndex + 2] = (byte)u;
                WriteIndex = writeIndex + 3;
            }
            else if (u < 0x1000_0000)
            {
                EnsureWrite(4); // 1110 xxxx +3B
                byte[] bytes = Bytes;
                int writeIndex = WriteIndex;
                bytes[writeIndex] = (byte)((u >> 24) + 0xe0);
                bytes[writeIndex + 1] = (byte)(u >> 16);
                bytes[writeIndex + 2] = (byte)(u >> 8);
                bytes[writeIndex + 3] = (byte)u;
                WriteIndex = writeIndex + 4;
            }
            else
            {
                EnsureWrite(5); // 1111 0000 +4B
                byte[] bytes = Bytes;
                int writeIndex = WriteIndex;
                bytes[writeIndex] = 0xf0;
                bytes[writeIndex + 1] = (byte)(u >> 24);
                bytes[writeIndex + 2] = (byte)(u >> 16);
                bytes[writeIndex + 3] = (byte)(u >> 8);
                bytes[writeIndex + 4] = (byte)u;
                WriteIndex = writeIndex + 5;
            }
        }

        // 返回值应被看作是无符号32位整数
        public int ReadUInt()
        {
            return (int)ReadULong();
        }

        public void SkipUInt()
        {
            EnsureRead(1);
            int readIndex = ReadIndex;
            int v = Bytes[readIndex];
            if (v < 0x80)
                ReadIndex = readIndex + 1;
            else if (v < 0xc0)
            {
                EnsureRead(2);
                ReadIndex = readIndex + 2;
            }
            else if (v < 0xe0)
            {
                EnsureRead(3);
                ReadIndex = readIndex + 3;
            }
            else if (v < 0xf0)
            {
                EnsureRead(4);
                ReadIndex = readIndex + 4;
            }
            else
            {
                EnsureRead(5);
                ReadIndex = readIndex + 5;
            }
        }

        public static int WriteLongSize(long v)
        {
            //@formatter:off
            if (v >= 0)
            {
                if (v <                0x40 ) return 1;
                if (v <              0x2000 ) return 2;
                if (v <           0x10_0000 ) return 3;
                if (v <          0x800_0000 ) return 4;
                if (v <       0x4_0000_0000L) return 5;
                if (v <     0x200_0000_0000L) return 6;
                if (v <  0x1_0000_0000_0000L) return 7;
                if (v < 0x80_0000_0000_0000L) return 8;
                                              return 9;
            }
            if (v >= -               0x40 ) return 1;
            if (v >= -             0x2000 ) return 2;
            if (v >= -          0x10_0000 ) return 3;
            if (v >= -         0x800_0000 ) return 4;
            if (v >= -      0x4_0000_0000L) return 5;
            if (v >= -    0x200_0000_0000L) return 6;
            if (v >= - 0x1_0000_0000_0000L) return 7;
            if (v >= -0x80_0000_0000_0000L) return 8;
            return 9;
            //@formatter:on
        }

        public void WriteLong(long x)
        {
            if (x >= 0)
            {
                if (x < 0x40)
                {
                    EnsureWrite(1); // 00xx xxxx
                    Bytes[WriteIndex++] = (byte)x;
                }
                else if (x < 0x2000)
                {
                    EnsureWrite(2); // 010x xxxx +1B
                    byte[] bytes = Bytes;
                    int writeIndex = WriteIndex;
                    bytes[writeIndex] = (byte)((x >> 8) + 0x40);
                    bytes[writeIndex + 1] = (byte)x;
                    WriteIndex = writeIndex + 2;
                }
                else if (x < 0x10_0000)
                {
                    EnsureWrite(3); // 0110 xxxx +2B
                    byte[] bytes = Bytes;
                    int writeIndex = WriteIndex;
                    bytes[writeIndex] = (byte)((x >> 16) + 0x60);
                    bytes[writeIndex + 1] = (byte)(x >> 8);
                    bytes[writeIndex + 2] = (byte)x;
                    WriteIndex = writeIndex + 3;
                }
                else if (x < 0x800_0000)
                {
                    EnsureWrite(4); // 0111 0xxx +3B
                    byte[] bytes = Bytes;
                    int writeIndex = WriteIndex;
                    bytes[writeIndex] = (byte)((x >> 24) + 0x70);
                    bytes[writeIndex + 1] = (byte)(x >> 16);
                    bytes[writeIndex + 2] = (byte)(x >> 8);
                    bytes[writeIndex + 3] = (byte)x;
                    WriteIndex = writeIndex + 4;
                }
                else if (x < 0x4_0000_0000L)
                {
                    EnsureWrite(5); // 0111 10xx +4B
                    byte[] bytes = Bytes;
                    int writeIndex = WriteIndex;
                    bytes[writeIndex] = (byte)((x >> 32) + 0x78);
                    bytes[writeIndex + 1] = (byte)(x >> 24);
                    bytes[writeIndex + 2] = (byte)(x >> 16);
                    bytes[writeIndex + 3] = (byte)(x >> 8);
                    bytes[writeIndex + 4] = (byte)x;
                    WriteIndex = writeIndex + 5;
                }
                else if (x < 0x200_0000_0000L)
                {
                    EnsureWrite(6); // 0111 110x +5B
                    byte[] bytes = Bytes;
                    int writeIndex = WriteIndex;
                    bytes[writeIndex] = (byte)((x >> 40) + 0x7c);
                    bytes[writeIndex + 1] = (byte)(x >> 32);
                    bytes[writeIndex + 2] = (byte)(x >> 24);
                    bytes[writeIndex + 3] = (byte)(x >> 16);
                    bytes[writeIndex + 4] = (byte)(x >> 8);
                    bytes[writeIndex + 5] = (byte)x;
                    WriteIndex = writeIndex + 6;
                }
                else if (x < 0x1_0000_0000_0000L)
                {
                    EnsureWrite(7); // 0111 1110 +6B
                    byte[] bytes = Bytes;
                    int writeIndex = WriteIndex;
                    bytes[writeIndex] = 0x7e;
                    bytes[writeIndex + 1] = (byte)(x >> 40);
                    bytes[writeIndex + 2] = (byte)(x >> 32);
                    bytes[writeIndex + 3] = (byte)(x >> 24);
                    bytes[writeIndex + 4] = (byte)(x >> 16);
                    bytes[writeIndex + 5] = (byte)(x >> 8);
                    bytes[writeIndex + 6] = (byte)x;
                    WriteIndex = writeIndex + 7;
                }
                else if (x < 0x80_0000_0000_0000L)
                {
                    EnsureWrite(8); // 0111 1111 0 +7B
                    byte[] bytes = Bytes;
                    int writeIndex = WriteIndex;
                    bytes[writeIndex] = 0x7f;
                    bytes[writeIndex + 1] = (byte)(x >> 48);
                    bytes[writeIndex + 2] = (byte)(x >> 40);
                    bytes[writeIndex + 3] = (byte)(x >> 32);
                    bytes[writeIndex + 4] = (byte)(x >> 24);
                    bytes[writeIndex + 5] = (byte)(x >> 16);
                    bytes[writeIndex + 6] = (byte)(x >> 8);
                    bytes[writeIndex + 7] = (byte)x;
                    WriteIndex = writeIndex + 8;
                }
                else
                {
                    EnsureWrite(9); // 0111 1111 1 +8B
                    byte[] bytes = Bytes;
                    int writeIndex = WriteIndex;
                    bytes[writeIndex] = 0x7f;
                    bytes[writeIndex + 1] = (byte)((x >> 56) + 0x80);
                    bytes[writeIndex + 2] = (byte)(x >> 48);
                    bytes[writeIndex + 3] = (byte)(x >> 40);
                    bytes[writeIndex + 4] = (byte)(x >> 32);
                    bytes[writeIndex + 5] = (byte)(x >> 24);
                    bytes[writeIndex + 6] = (byte)(x >> 16);
                    bytes[writeIndex + 7] = (byte)(x >> 8);
                    bytes[writeIndex + 8] = (byte)x;
                    WriteIndex = writeIndex + 9;
                }
            }
            else
            {
                if (x >= -0x40)
                {
                    EnsureWrite(1); // 11xx xxxx
                    Bytes[WriteIndex++] = (byte)x;
                }
                else if (x >= -0x2000)
                {
                    EnsureWrite(2); // 101x xxxx +1B
                    byte[] bytes = Bytes;
                    int writeIndex = WriteIndex;
                    bytes[writeIndex] = (byte)((x >> 8) - 0x40);
                    bytes[writeIndex + 1] = (byte)x;
                    WriteIndex = writeIndex + 2;
                }
                else if (x >= -0x10_0000)
                {
                    EnsureWrite(3); // 1001 xxxx +2B
                    byte[] bytes = Bytes;
                    int writeIndex = WriteIndex;
                    bytes[writeIndex] = (byte)((x >> 16) - 0x60);
                    bytes[writeIndex + 1] = (byte)(x >> 8);
                    bytes[writeIndex + 2] = (byte)x;
                    WriteIndex = writeIndex + 3;
                }
                else if (x >= -0x800_0000)
                {
                    EnsureWrite(4); // 1000 1xxx +3B
                    byte[] bytes = Bytes;
                    int writeIndex = WriteIndex;
                    bytes[writeIndex] = (byte)((x >> 24) - 0x70);
                    bytes[writeIndex + 1] = (byte)(x >> 16);
                    bytes[writeIndex + 2] = (byte)(x >> 8);
                    bytes[writeIndex + 3] = (byte)x;
                    WriteIndex = writeIndex + 4;
                }
                else if (x >= -0x4_0000_0000L)
                {
                    EnsureWrite(5); // 1000 01xx +4B
                    byte[] bytes = Bytes;
                    int writeIndex = WriteIndex;
                    bytes[writeIndex] = (byte)((x >> 32) - 0x78);
                    bytes[writeIndex + 1] = (byte)(x >> 24);
                    bytes[writeIndex + 2] = (byte)(x >> 16);
                    bytes[writeIndex + 3] = (byte)(x >> 8);
                    bytes[writeIndex + 4] = (byte)x;
                    WriteIndex = writeIndex + 5;
                }
                else if (x >= -0x200_0000_0000L)
                {
                    EnsureWrite(6); // 1000 001x +5B
                    byte[] bytes = Bytes;
                    int writeIndex = WriteIndex;
                    bytes[writeIndex] = (byte)((x >> 40) - 0x7c);
                    bytes[writeIndex + 1] = (byte)(x >> 32);
                    bytes[writeIndex + 2] = (byte)(x >> 24);
                    bytes[writeIndex + 3] = (byte)(x >> 16);
                    bytes[writeIndex + 4] = (byte)(x >> 8);
                    bytes[writeIndex + 5] = (byte)x;
                    WriteIndex = writeIndex + 6;
                }
                else if (x >= -0x1_0000_0000_0000L)
                {
                    EnsureWrite(7); // 1000 0001 +6B
                    byte[] bytes = Bytes;
                    int writeIndex = WriteIndex;
                    bytes[writeIndex] = 0x81;
                    bytes[writeIndex + 1] = (byte)(x >> 40);
                    bytes[writeIndex + 2] = (byte)(x >> 32);
                    bytes[writeIndex + 3] = (byte)(x >> 24);
                    bytes[writeIndex + 4] = (byte)(x >> 16);
                    bytes[writeIndex + 5] = (byte)(x >> 8);
                    bytes[writeIndex + 6] = (byte)x;
                    WriteIndex = writeIndex + 7;
                }
                else if (x >= -0x80_0000_0000_0000L)
                {
                    EnsureWrite(8); // 1000 0000 1 +7B
                    byte[] bytes = Bytes;
                    int writeIndex = WriteIndex;
                    bytes[writeIndex] = 0x80;
                    bytes[writeIndex + 1] = (byte)(x >> 48);
                    bytes[writeIndex + 2] = (byte)(x >> 40);
                    bytes[writeIndex + 3] = (byte)(x >> 32);
                    bytes[writeIndex + 4] = (byte)(x >> 24);
                    bytes[writeIndex + 5] = (byte)(x >> 16);
                    bytes[writeIndex + 6] = (byte)(x >> 8);
                    bytes[writeIndex + 7] = (byte)x;
                    WriteIndex = writeIndex + 8;
                }
                else
                {
                    EnsureWrite(9); // 1000 0000 0 +8B
                    byte[] bytes = Bytes;
                    int writeIndex = WriteIndex;
                    bytes[writeIndex] = 0x80;
                    bytes[writeIndex + 1] = (byte)((x >> 56) - 0x80);
                    bytes[writeIndex + 2] = (byte)(x >> 48);
                    bytes[writeIndex + 3] = (byte)(x >> 40);
                    bytes[writeIndex + 4] = (byte)(x >> 32);
                    bytes[writeIndex + 5] = (byte)(x >> 24);
                    bytes[writeIndex + 6] = (byte)(x >> 16);
                    bytes[writeIndex + 7] = (byte)(x >> 8);
                    bytes[writeIndex + 8] = (byte)x;
                    WriteIndex = writeIndex + 9;
                }
            }
        }

        public static int WriteULongSize(long v)
        {
            //@formatter:off
            if (v <                 0x80 ) return v >= 0 ? 1 : 9;
            if (v <               0x4000 ) return 2;
            if (v <            0x20_0000 ) return 3;
            if (v <          0x1000_0000 ) return 4;
            if (v <        0x8_0000_0000L) return 5;
            if (v <      0x400_0000_0000L) return 6;
            if (v <   0x2_0000_0000_0000L) return 7;
            if (v < 0x100_0000_0000_0000L) return 8;
            return 9;
            //@formatter:on
        }

        public void WriteULong(long x)
        {
            ulong u = (ulong)x;
            if (u < 0x80) // 0xxx xxxx
            {
                EnsureWrite(1);
                Bytes[WriteIndex++] = (byte)u;
            }
            else if (u < 0x4000) // 10xx xxxx +1B
            {
                EnsureWrite(2);
                byte[] bytes = Bytes;
                int writeIndex = WriteIndex;
                bytes[writeIndex] = (byte)((u >> 8) + 0x80);
                bytes[writeIndex + 1] = (byte)u;
                WriteIndex = writeIndex + 2;
            }
            else if (u < 0x20_0000) // 110x xxxx +2B
            {
                EnsureWrite(3);
                byte[] bytes = Bytes;
                int writeIndex = WriteIndex;
                bytes[writeIndex] = (byte)((u >> 16) + 0xc0);
                bytes[writeIndex + 1] = (byte)(u >> 8);
                bytes[writeIndex + 2] = (byte)u;
                WriteIndex = writeIndex + 3;
            }
            else if (u < 0x1000_0000) // 1110 xxxx +3B
            {
                EnsureWrite(4);
                byte[] bytes = Bytes;
                int writeIndex = WriteIndex;
                bytes[writeIndex] = (byte)((u >> 24) + 0xe0);
                bytes[writeIndex + 1] = (byte)(u >> 16);
                bytes[writeIndex + 2] = (byte)(u >> 8);
                bytes[writeIndex + 3] = (byte)u;
                WriteIndex = writeIndex + 4;
            }
            else if (u < 0x8_0000_0000L) // 1111 0xxx +4B
            {
                EnsureWrite(5);
                byte[] bytes = Bytes;
                int writeIndex = WriteIndex;
                bytes[writeIndex] = (byte)((u >> 32) + 0xf0);
                bytes[writeIndex + 1] = (byte)(u >> 24);
                bytes[writeIndex + 2] = (byte)(u >> 16);
                bytes[writeIndex + 3] = (byte)(u >> 8);
                bytes[writeIndex + 4] = (byte)u;
                WriteIndex = writeIndex + 5;
            }
            else if (u < 0x400_0000_0000L) // 1111 10xx +5B
            {
                EnsureWrite(6);
                byte[] bytes = Bytes;
                int writeIndex = WriteIndex;
                bytes[writeIndex] = (byte)((u >> 40) + 0xf8);
                bytes[writeIndex + 1] = (byte)(u >> 32);
                bytes[writeIndex + 2] = (byte)(u >> 24);
                bytes[writeIndex + 3] = (byte)(u >> 16);
                bytes[writeIndex + 4] = (byte)(u >> 8);
                bytes[writeIndex + 5] = (byte)u;
                WriteIndex = writeIndex + 6;
            }
            else if (u < 0x2_0000_0000_0000L) // 1111 110x +6B
            {
                EnsureWrite(7);
                byte[] bytes = Bytes;
                int writeIndex = WriteIndex;
                bytes[writeIndex] = (byte)((u >> 48) + 0xfc);
                bytes[writeIndex + 1] = (byte)(u >> 40);
                bytes[writeIndex + 2] = (byte)(u >> 32);
                bytes[writeIndex + 3] = (byte)(u >> 24);
                bytes[writeIndex + 4] = (byte)(u >> 16);
                bytes[writeIndex + 5] = (byte)(u >> 8);
                bytes[writeIndex + 6] = (byte)u;
                WriteIndex = writeIndex + 7;
            }
            else if (u < 0x100_0000_0000_0000L) // 1111 1110 +7B
            {
                EnsureWrite(8);
                byte[] bytes = Bytes;
                int writeIndex = WriteIndex;
                bytes[writeIndex] = 0xfe;
                bytes[writeIndex + 1] = (byte)(u >> 48);
                bytes[writeIndex + 2] = (byte)(u >> 40);
                bytes[writeIndex + 3] = (byte)(u >> 32);
                bytes[writeIndex + 4] = (byte)(u >> 24);
                bytes[writeIndex + 5] = (byte)(u >> 16);
                bytes[writeIndex + 6] = (byte)(u >> 8);
                bytes[writeIndex + 7] = (byte)u;
                WriteIndex = writeIndex + 8;
            }
            else // 1111 1111 +8B
            {
                EnsureWrite(9);
                byte[] bytes = Bytes;
                int writeIndex = WriteIndex;
                bytes[writeIndex] = 0xff;
                bytes[writeIndex + 1] = (byte)(u >> 56);
                bytes[writeIndex + 2] = (byte)(u >> 48);
                bytes[writeIndex + 3] = (byte)(u >> 40);
                bytes[writeIndex + 4] = (byte)(u >> 32);
                bytes[writeIndex + 5] = (byte)(u >> 24);
                bytes[writeIndex + 6] = (byte)(u >> 16);
                bytes[writeIndex + 7] = (byte)(u >> 8);
                bytes[writeIndex + 8] = (byte)u;
                WriteIndex = writeIndex + 9;
            }
        }

        // 返回值应被看作是无符号64位整数
        public long ReadULong()
        {
            int b = ReadByte();
            //@formatter:off
            switch (b >> 4)
            {
            case  0: case  1: case  2: case  3: case 4: case 5: case 6: case 7: return b;
            case  8: case  9: case 10: case 11: return ((b & 0x3f) <<  8) + ReadLong1();
            case 12: case 13:                   return ((b & 0x1f) << 16) + ReadLong2BE();
            case 14:                            return ((b & 0x0f) << 24) + ReadLong3BE();
            default:
                switch (b & 0xf)
                {
                case  0: case  1: case  2: case  3: case 4: case 5: case 6: case 7:
                    return ((long)(b & 7) << 32) + ReadLong4BE();
                case  8: case  9: case 10: case 11: return ((long)(b & 3) << 40) + ReadLong5BE();
                case 12: case 13:                   return ((long)(b & 1) << 48) + ReadLong6BE();
                case 14:                            return ReadLong7BE();
                default:                            return ReadLong8BE();
                }
            }
            //@formatter:on
        }

        public void SkipULong()
        {
            int b = ReadByte();
            //@formatter:off
            switch ((b >> 4) & 0xf)
            {
            case  0: case  1: case  2: case  3: case 4: case 5: case 6: case 7: return;
            case  8: case  9: case 10: case 11: Skip(1); return;
            case 12: case 13:                   Skip(2); return;
            case 14:                            Skip(3); return;
            default:
                switch (b & 0xf)
                {
                case  0: case  1: case  2: case  3: case 4: case 5: case 6: case 7: Skip(4); return;
                case  8: case  9: case 10: case 11: Skip(5); return;
                case 12: case 13:                   Skip(6); return;
                case 14:                            Skip(7); return;
                default:                            Skip(8); return;
                }
            }
            //@formatter:on
        }

        public long ReadLong1()
        {
            EnsureRead(1);
            return Bytes[ReadIndex++];
        }

        public long ReadLong2BE()
        {
            EnsureRead(2);
            byte[] bytes = Bytes;
            int readIndex = ReadIndex;
            ReadIndex = readIndex + 2;
            return (bytes[readIndex] << 8) +
                   bytes[readIndex + 1];
        }

        public long ReadLong3BE()
        {
            EnsureRead(3);
            byte[] bytes = Bytes;
            int readIndex = ReadIndex;
            ReadIndex = readIndex + 3;
            return (bytes[readIndex] << 16) +
                   (bytes[readIndex + 1] << 8) +
                   bytes[readIndex + 2];
        }

        public long ReadLong4BE()
        {
            EnsureRead(4);
            byte[] bytes = Bytes;
            int readIndex = ReadIndex;
            ReadIndex = readIndex + 4;
            return ((long)bytes[readIndex] << 24) +
                   (bytes[readIndex + 1] << 16) +
                   (bytes[readIndex + 2] << 8) +
                   bytes[readIndex + 3];
        }

        public long ReadLong5BE()
        {
            EnsureRead(5);
            byte[] bytes = Bytes;
            int readIndex = ReadIndex;
            ReadIndex = readIndex + 5;
            return ((long)bytes[readIndex] << 32) +
                   ((long)bytes[readIndex + 1] << 24) +
                   (bytes[readIndex + 2] << 16) +
                   (bytes[readIndex + 3] << 8) +
                   bytes[readIndex + 4];
        }

        public long ReadLong6BE()
        {
            EnsureRead(6);
            byte[] bytes = Bytes;
            int readIndex = ReadIndex;
            ReadIndex = readIndex + 6;
            return ((long)bytes[readIndex] << 40) +
                   ((long)bytes[readIndex + 1] << 32) +
                   ((long)bytes[readIndex + 2] << 24) +
                   (bytes[readIndex + 3] << 16) +
                   (bytes[readIndex + 4] << 8) +
                   bytes[readIndex + 5];
        }

        public long ReadLong7BE()
        {
            EnsureRead(7);
            byte[] bytes = Bytes;
            int readIndex = ReadIndex;
            ReadIndex = readIndex + 7;
            return ((long)bytes[readIndex] << 48) +
                   ((long)bytes[readIndex + 1] << 40) +
                   ((long)bytes[readIndex + 2] << 32) +
                   ((long)bytes[readIndex + 3] << 24) +
                   (bytes[readIndex + 4] << 16) +
                   (bytes[readIndex + 5] << 8) +
                   bytes[readIndex + 6];
        }

        public long ReadLong8BE()
        {
            EnsureRead(8);
            byte[] bytes = Bytes;
            int readIndex = ReadIndex;
            ReadIndex = readIndex + 8;
            return ((long)bytes[readIndex] << 56) +
                   ((long)bytes[readIndex + 1] << 48) +
                   ((long)bytes[readIndex + 2] << 40) +
                   ((long)bytes[readIndex + 3] << 32) +
                   ((long)bytes[readIndex + 4] << 24) +
                   (bytes[readIndex + 5] << 16) +
                   (bytes[readIndex + 6] << 8) +
                   bytes[readIndex + 7];
        }

        public long ReadLong()
        {
            EnsureRead(1);
            int b = (sbyte)Bytes[ReadIndex++];
            //@formatter:off
            switch ((b >> 3) & 0x1f)
            {
                case 0x00: case 0x01: case 0x02: case 0x03: case 0x04: case 0x05: case 0x06: case 0x07:
                case 0x18: case 0x19: case 0x1a: case 0x1b: case 0x1c: case 0x1d: case 0x1e: case 0x1f: return b;
                case 0x08: case 0x09: case 0x0a: case 0x0b: return ((b - 0x40) << 8) + ReadLong1();
                case 0x14: case 0x15: case 0x16: case 0x17: return ((b + 0x40) << 8) + ReadLong1();
                case 0x0c: case 0x0d: return ((b - 0x60) << 16) + ReadLong2BE();
                case 0x12: case 0x13: return ((b + 0x60) << 16) + ReadLong2BE();
                case 0x0e: return ((b - 0x70L) << 24) + ReadLong3BE();
                case 0x11: return ((b + 0x70L) << 24) + ReadLong3BE();
                case 0x0f:
                    switch (b & 7) {
                        case 0: case 1: case 2: case 3: return ((long)(b - 0x78) << 32) + ReadLong4BE();
                        case 4: case 5: return ((long)(b - 0x7c) << 40) + ReadLong5BE();
                        case 6: return ReadLong6BE();
                        default: long r = ReadLong7BE(); return r < 0x80_0000_0000_0000L ?
                                r : ((r - 0x80_0000_0000_0000L) << 8) + ReadLong1();
                    }
                default: // 0x10
                    switch (b & 7) {
                        case 4: case 5: case 6: case 7: return ((long)(b + 0x78) << 32) + ReadLong4BE();
                        case 2: case 3: return ((long)(b + 0x7c) << 40) + ReadLong5BE();
                        case 1: return -0x0001_0000_0000_0000L + ReadLong6BE();
                        default: long r = ReadLong7BE(); return r >= 0x80_0000_0000_0000L ?
                                -0x0100_0000_0000_0000L + r : ((r + 0x80_0000_0000_0000L) << 8) + ReadLong1();
                    }
            }
            //@formatter:on
        }

        public void SkipLong()
        {
            EnsureRead(1);
            int b = Bytes[ReadIndex++];
            //@formatter:off
            switch ((b >> 3) & 0x1f)
            {
            case 0x00: case 0x01: case 0x02: case 0x03: case 0x04: case 0x05: case 0x06: case 0x07:
            case 0x18: case 0x19: case 0x1a: case 0x1b: case 0x1c: case 0x1d: case 0x1e: case 0x1f: return;
            case 0x08: case 0x09: case 0x0a: case 0x0b:
            case 0x14: case 0x15: case 0x16: case 0x17: Skip(1); return;
            case 0x0c: case 0x0d: case 0x12: case 0x13: Skip(2); return;
            case 0x0e: case 0x11:                       Skip(3); return;
            case 0x0f:
                switch (b & 7)
                {
                case 0: case 1: case 2: case 3: Skip(4); return;
                case 4: case 5:                 Skip(5); return;
                case 6:                         Skip(6); return;
                default: EnsureRead(1); Skip(6 + (Bytes[ReadIndex++] >> 7)); return;
                }
            default: // 0x10
                switch (b & 7)
                {
                case 4: case 5: case 6: case 7: Skip(4); return;
                case 2: case 3:                 Skip(5); return;
                case 1:                         Skip(6); return;
                default: EnsureRead(1); Skip(7 - (Bytes[ReadIndex++] >> 7)); return;
                }
            }
            //@formatter:on
        }

        [MethodImpl(MethodImplOptions.AggressiveInlining)]
        public void WriteInt(int x)
        {
            WriteLong(x);
        }

        [MethodImpl(MethodImplOptions.AggressiveInlining)]
        public int ReadInt()
        {
            return (int)ReadLong();
        }

        [MethodImpl(MethodImplOptions.AggressiveInlining)]
        public void WriteFloat(float x)
        {
            byte[] bs = BitConverter.GetBytes(x);
            //if (!BitConverter.IsLittleEndian)
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
            //if (!BitConverter.IsLittleEndian)
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
            int n = ReadUInt();
            if (n == 0)
                return "";
            if (n < 0)
                throw new Exception("invalid length for ReadString: " + n + " at " + ReadIndex + '/' + WriteIndex);
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
            WriteUInt(length);
            EnsureWrite(length);
            Buffer.BlockCopy(x, offset, Bytes, WriteIndex, length);
            WriteIndex += length;
        }

        [MethodImpl(MethodImplOptions.AggressiveInlining)]
        public void WriteBinary(Binary binary)
        {
            WriteBytes(binary.Bytes, binary.Offset, binary.Count);
        }

        // ReSharper disable once UnusedAutoPropertyAccessor.Global
        public static bool BinaryNoCopy { get; set; } // 没有线程保护
        // XXX 对于byte[]类型直接使用引用，不拷贝。全局配置，只能用于Linkd这种纯转发的程序，优化。

        public byte[] FetchBytes(int n)
        {
            EnsureRead(n);
            var x = new byte[n];
            Buffer.BlockCopy(Bytes, ReadIndex, x, 0, n);
            ReadIndex += n;
            return x;
        }

        public Binary ReadBinary()
        {
            return BinaryNoCopy ? new Binary(ReadByteBuffer()) : new Binary(ReadBytes());
        }

        public byte[] ReadBytes()
        {
            int n = ReadUInt();
            if (n == 0)
                return Array.Empty<byte>();
            if (n < 0)
                throw new Exception("invalid length for ReadBytes: " + n + " at " + ReadIndex + '/' + WriteIndex);
            EnsureRead(n);
            byte[] x = new byte[n];
            Buffer.BlockCopy(Bytes, ReadIndex, x, 0, n);
            ReadIndex += n;
            return x;
        }

        [MethodImpl(MethodImplOptions.AggressiveInlining)]
        public void Skip(int n)
        {
            EnsureRead(n);
            ReadIndex += n;
        }

        [MethodImpl(MethodImplOptions.AggressiveInlining)]
        public void SkipBytes()
        {
            int n = ReadUInt();
            if (n < 0)
                throw new Exception("invalid length for SkipBytes: " + n + " at " + ReadIndex + '/' + WriteIndex);
            Skip(n);
        }

        [MethodImpl(MethodImplOptions.AggressiveInlining)]
        public void SkipBytes4()
        {
            int n = ReadInt4();
            if (n < 0)
                throw new Exception("invalid length for SkipBytes4: " + n + " at " + ReadIndex + '/' + WriteIndex);
            Skip(n);
        }

        /// <summary>
        /// 会推进ReadIndex，但是返回的ByteBuffer和原来的共享内存。
        /// </summary>
        /// <returns></returns>
        [MethodImpl(MethodImplOptions.AggressiveInlining)]
        public ByteBuffer ReadByteBuffer()
        {
            int n = ReadUInt();
            if (n < 0)
                throw new Exception("invalid length for ReadByteBuffer: " + n + " at " + ReadIndex + '/' + WriteIndex);
            EnsureRead(n);
            int cur = ReadIndex;
            ReadIndex = cur + n;
            return Wrap(Bytes, cur, n);
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
            return obj is ByteBuffer other && Equals(other);
        }

        public static long ToLong(byte[] bytes, int offset)
        {
            return bytes[offset] +
                   (bytes[offset + 1] << 8) +
                   (bytes[offset + 2] << 16) +
                   ((long)bytes[offset + 3] << 24) +
                   ((long)bytes[offset + 4] << 32) +
                   ((long)bytes[offset + 5] << 40) +
                   ((long)bytes[offset + 6] << 48) +
                   ((long)bytes[offset + 7] << 56);
        }

        public static long ToLong(byte[] bytes, int offset, int length)
        {
            long v = 0;
            //@formatter:off
            switch (length)
            {
                default: v = (long)bytes[offset + 7] << 56; goto case 7;
                case 7: v += (long)bytes[offset + 6] << 48; goto case 6;
                case 6: v += (long)bytes[offset + 5] << 40; goto case 5;
                case 5: v += (long)bytes[offset + 4] << 32; goto case 4;
                case 4: v += (long)bytes[offset + 3] << 24; goto case 3;
                case 3: v += bytes[offset + 2] << 16; goto case 2;
                case 2: v += bytes[offset + 1] << 8; goto case 1;
                case 1: v += bytes[offset]; goto case 0;
                case 0: break;
            }
            //@formatter:on
            return v;
        }

        public static long ToLongBE(byte[] bytes, int offset, int length)
        {
            long v = 0;
            int s = 0;
            //@formatter:off
            switch (length)
            {
                default: v = bytes[offset + 7]; s = 8; goto case 7;
                case 7: v += bytes[offset + 6] << s; s += 8; goto case 6;
                case 6: v += bytes[offset + 5] << s; s += 8; goto case 5;
                case 5: v += bytes[offset + 4] << s; s += 8; goto case 4;
                case 4: v += (long)bytes[offset + 3] << s; s += 8; goto case 3;
                case 3: v += (long)bytes[offset + 2] << s; s += 8; goto case 2;
                case 2: v += (long)bytes[offset + 1] << s; s += 8; goto case 1;
                case 1: v += (long)bytes[offset] << s; break;
                case 0: break;
            }
            //@formatter:on
            return v;
        }

        public bool Equals(ByteBuffer other)
        {
            if (other == null)
                return false;

            if (Size != other.Size)
                return false;

            for (int i = 0, n = Size; i < n; i++)
            {
                if (Bytes[ReadIndex + i] != other.Bytes[other.ReadIndex + i])
                    return false;
            }

            return true;
        }

        public override int GetHashCode()
        {
            // ReSharper disable NonReadonlyMemberInGetHashCode
            return FixedHash.calc_hashnr(Bytes, ReadIndex, Size);
            // ReSharper restore NonReadonlyMemberInGetHashCode
        }

        // 只能增加新的类型定义，增加时记得同步 SkipUnknownField
        public const int
            INTEGER = 0, // byte,short,int,long,bool
            FLOAT = 1, // float
            DOUBLE = 2, // double
            BYTES = 3, // binary,string,decimal
            LIST = 4, // list,set
            MAP = 5, // map
            BEAN = 6, // bean
            DYNAMIC = 7, // dynamic
            VECTOR2 = 8, // float{x,y}
            VECTOR2INT = 9, // int{x,y}
            VECTOR3 = 10, // float{x,y,z}
            VECTOR3INT = 11, // int{x,y,z}
            VECTOR4 = 12, // float{x,y,z,w} Quaternion
            END = 15;

        public const int TAG_SHIFT = 4;
        public const int TAG_MASK = (1 << TAG_SHIFT) - 1;
        public const int ID_MASK = 0xff - TAG_MASK;

        // 只用于lua描述特殊类型
        public const int
            LUA_BOOL = INTEGER + (1 << TAG_SHIFT),
            LUA_SET = LIST + (1 << TAG_SHIFT);

        /*
        // 在生成代码的时候使用这个方法检查。生成后的代码不使用这个方法。
        // 可以定义的最大 Variable.Id 为 Zeze.Transaction.Bean.MaxVariableId
        public static int MakeTagId(int tag, int id)
        {
            if (tag < 0 || tag > TAG_MAX)
                throw new OverflowException("tag < 0 || tag > TAG_MAX");
            if (id < 0 || id > ID_MASK)
                throw new OverflowException("id < 0 || id > ID_MASK");

            return (id << TAG_SHIFT) + tag;
        }

        public static int GetTag(int tagId)
        {
            return tagId & TAG_MASK;
        }

        public static int GetId(int tagId)
        {
        }
        */

        public static void VerifyArrayIndex(byte[] bytes, int offset, int length)
        {
            int bytesLen = bytes.Length;
            long offsetL = (uint)offset;
            long endIndexL = (uint)offset + (uint)length;
            if (endIndexL > bytesLen || offsetL > endIndexL)
                throw new Exception($"{bytesLen},{offset},{length}");
        }

        public void Encode<T>(ICollection<T> c)
        {
            WriteUInt(c.Count);
            foreach (var s in c)
            {
                SerializeHelper<T>.Encode(this, s);
            }
        }

        public void Decode<T>(ICollection<T> c)
        {
            for (int i = ReadUInt(); i > 0; --i)
            {
                c.Add(SerializeHelper<T>.Decode(this));
            }
        }

        public static ByteBuffer Encode(Serializable sa)
        {
            var bb = Allocate();
            sa.Encode(bb);
            return bb;
        }

        public int WriteTag(int lastVarId, int varId, int type)
        {
            int deltaId = varId - lastVarId;
            if (deltaId < 0xf)
                WriteByte((deltaId << TAG_SHIFT) + type);
            else
            {
                WriteByte(0xf0 + type);
                WriteUInt(deltaId - 0xf);
            }
            return varId;
        }

        public void WriteListType(int listSize, int elemType)
        {
            if (listSize < 0xf)
                WriteByte((listSize << TAG_SHIFT) + elemType);
            else
            {
                WriteByte(0xf0 + elemType);
                WriteUInt(listSize - 0xf);
            }
        }

        public void WriteMapType(int mapSize, int keyType, int valueType)
        {
            WriteByte((keyType << TAG_SHIFT) + valueType);
            WriteUInt(mapSize);
        }

        public int ReadTagSize(int tagByte)
        {
            int deltaId = (tagByte & ID_MASK) >> TAG_SHIFT;
            return deltaId < 0xf ? deltaId : 0xf + ReadUInt();
        }

        public bool ReadBool(int tag)
        {
            int type = tag & TAG_MASK;
            if (type == INTEGER)
                return ReadLong() != 0;
            if (type == FLOAT)
                return ReadFloat() != 0;
            if (type == DOUBLE)
                return ReadDouble() != 0;
            if (IGNORE_INCOMPATIBLE_FIELD)
            {
                SkipUnknownField(tag);
                return false;
            }
            throw new Exception("can not ReadBool for type=" + type + " at " + ReadIndex + '/' + WriteIndex);
        }

        public byte ReadByte(int tag)
        {
            int type = tag & TAG_MASK;
            if (type == INTEGER)
                return (byte)ReadLong();
            if (type == FLOAT)
                return (byte)ReadFloat();
            if (type == DOUBLE)
                return (byte)ReadDouble();
            if (IGNORE_INCOMPATIBLE_FIELD)
            {
                SkipUnknownField(tag);
                return 0;
            }
            throw new Exception("can not ReadByte for type=" + type + " at " + ReadIndex + '/' + WriteIndex);
        }

        public short ReadShort(int tag)
        {
            int type = tag & TAG_MASK;
            if (type == INTEGER)
                return (short)ReadLong();
            if (type == FLOAT)
                return (short)ReadFloat();
            if (type == DOUBLE)
                return (short)ReadDouble();
            if (IGNORE_INCOMPATIBLE_FIELD)
            {
                SkipUnknownField(tag);
                return 0;
            }
            throw new Exception("can not ReadShort for type=" + type + " at " + ReadIndex + '/' + WriteIndex);
        }

        public int ReadInt(int tag)
        {
            int type = tag & TAG_MASK;
            if (type == INTEGER)
                return (int)ReadLong();
            if (type == FLOAT)
                return (int)ReadFloat();
            if (type == DOUBLE)
                return (int)ReadDouble();
            if (IGNORE_INCOMPATIBLE_FIELD)
            {
                SkipUnknownField(tag);
                return 0;
            }
            throw new Exception("can not ReadInt for type=" + type + " at " + ReadIndex + '/' + WriteIndex);
        }

        public long ReadLong(int tag)
        {
            int type = tag & TAG_MASK;
            if (type == INTEGER)
                return ReadLong();
            if (type == FLOAT)
                return (long)ReadFloat();
            if (type == DOUBLE)
                return (long)ReadDouble();
            if (IGNORE_INCOMPATIBLE_FIELD)
            {
                SkipUnknownField(tag);
                return 0;
            }
            throw new Exception("can not ReadLong for type=" + type + " at " + ReadIndex + '/' + WriteIndex);
        }

        public float ReadFloat(int tag)
        {
            int type = tag & TAG_MASK;
            if (type == FLOAT)
                return ReadFloat();
            if (type == DOUBLE)
                return (float)ReadDouble();
            if (type == INTEGER)
                return ReadLong();
            if (IGNORE_INCOMPATIBLE_FIELD)
            {
                SkipUnknownField(tag);
                return 0;
            }
            throw new Exception("can not ReadFloat for type=" + type + " at " + ReadIndex + '/' + WriteIndex);
        }

        public double ReadDouble(int tag)
        {
            int type = tag & TAG_MASK;
            if (type == DOUBLE)
                return ReadDouble();
            if (type == FLOAT)
                return ReadFloat();
            if (type == INTEGER)
                return ReadLong();
            if (IGNORE_INCOMPATIBLE_FIELD)
            {
                SkipUnknownField(tag);
                return 0;
            }
            throw new Exception("can not ReadDouble for type=" + type + " at " + ReadIndex + '/' + WriteIndex);
        }

        public Binary ReadBinary(int tag)
        {
            int type = tag & TAG_MASK;
            if (type == BYTES)
                return ReadBinary();
            if (IGNORE_INCOMPATIBLE_FIELD)
            {
                SkipUnknownField(tag);
                return Binary.Empty;
            }
            throw new Exception("can not ReadBinary for type=" + type + " at " + ReadIndex + '/' + WriteIndex);
        }

        public string ReadString(int tag)
        {
            int type = tag & TAG_MASK;
            if (type == BYTES)
                return ReadString();
            if (IGNORE_INCOMPATIBLE_FIELD)
            {
                SkipUnknownField(tag);
                return "";
            }
            throw new Exception("can not ReadString for type=" + type + " at " + ReadIndex + '/' + WriteIndex);
        }

        public Vector2 ReadVector2()
        {
            var r = new Vector2();
            r.Decode(this);
            return r;
        }

        public Vector3 ReadVector3()
        {
            var r = new Vector3();
            r.Decode(this);
            return r;
        }

        public Vector4 ReadVector4()
        {
            var r = new Vector4();
            r.Decode(this);
            return r;
        }

        public Quaternion ReadQuaternion()
        {
            var r = new Quaternion();
            r.Decode(this);
            return r;
        }

        public Vector2Int ReadVector2Int()
        {
            var r = new Vector2Int();
            r.Decode(this);
            return r;
        }

        public Vector3Int ReadVector3Int()
        {
            var r = new Vector3Int();
            r.Decode(this);
            return r;
        }

        public Vector2 ReadVector2(int tag)
        {
            int type = tag & TAG_MASK;
            if (type == VECTOR2)
                return ReadVector2();
            if (type == VECTOR3)
                return ReadVector3();
            if (type == VECTOR4)
                return ReadVector4();
            if (type == VECTOR2INT)
                return new Vector2(ReadVector2Int());
            if (type == VECTOR3INT)
                return new Vector3(ReadVector3Int());
            if (type == FLOAT)
                return new Vector2(ReadFloat(), 0);
            if (type == DOUBLE)
                return new Vector2((float)ReadDouble(), 0);
            if (type == INTEGER)
                return new Vector2(ReadLong(), 0);
            if (IGNORE_INCOMPATIBLE_FIELD)
            {
                SkipUnknownField(tag);
                return new Vector2();
            }
            throw new Exception("can not ReadVector2 for type=" + type + " at " + ReadIndex + '/' + WriteIndex);
        }

        public Vector3 ReadVector3(int tag)
        {
            int type = tag & TAG_MASK;
            if (type == VECTOR3)
                return ReadVector3();
            if (type == VECTOR2)
                return new Vector3(ReadVector2());
            if (type == VECTOR4)
                return ReadVector4();
            if (type == VECTOR3INT)
                return new Vector3(ReadVector3Int());
            if (type == VECTOR2INT)
                return new Vector3(ReadVector2Int());
            if (type == FLOAT)
                return new Vector3(ReadFloat(), 0, 0);
            if (type == DOUBLE)
                return new Vector3((float)ReadDouble(), 0, 0);
            if (type == INTEGER)
                return new Vector3(ReadLong(), 0, 0);
            if (IGNORE_INCOMPATIBLE_FIELD)
            {
                SkipUnknownField(tag);
                return new Vector3();
            }
            throw new Exception("can not ReadVector3 for type=" + type + " at " + ReadIndex + '/' + WriteIndex);
        }

        public Vector4 ReadVector4(int tag)
        {
            int type = tag & TAG_MASK;
            if (type == VECTOR4)
                return ReadVector4();
            if (type == VECTOR3)
                return new Vector4(ReadVector3());
            if (type == VECTOR2)
                return new Vector4(ReadVector2());
            if (type == VECTOR3INT)
                return new Vector4(ReadVector3Int());
            if (type == VECTOR2INT)
                return new Vector4(ReadVector2Int());
            if (type == FLOAT)
                return new Vector4(ReadFloat(), 0, 0, 0);
            if (type == DOUBLE)
                return new Vector4((float)ReadDouble(), 0, 0, 0);
            if (type == INTEGER)
                return new Vector4(ReadLong(), 0, 0, 0);
            if (IGNORE_INCOMPATIBLE_FIELD)
            {
                SkipUnknownField(tag);
                return new Vector4();
            }
            throw new Exception("can not ReadVector4 for type=" + type + " at " + ReadIndex + '/' + WriteIndex);
        }

        public Quaternion ReadQuaternion(int tag)
        {
            int type = tag & TAG_MASK;
            if (type == VECTOR4)
                return new Quaternion(ReadVector4());
            if (type == VECTOR3)
                return new Quaternion(ReadVector3());
            if (type == VECTOR2)
                return new Quaternion(ReadVector2());
            if (type == VECTOR3INT)
                return new Quaternion(ReadVector3Int());
            if (type == VECTOR2INT)
                return new Quaternion(ReadVector2Int());
            if (type == FLOAT)
                return new Quaternion(ReadFloat(), 0, 0, 0);
            if (type == DOUBLE)
                return new Quaternion((float)ReadDouble(), 0, 0, 0);
            if (type == INTEGER)
                return new Quaternion(ReadLong(), 0, 0, 0);
            if (IGNORE_INCOMPATIBLE_FIELD)
            {
                SkipUnknownField(tag);
                return new Quaternion();
            }
            throw new Exception("can not ReadQuaternion for type=" + type + " at " + ReadIndex + '/' + WriteIndex);
        }

        public Vector2Int ReadVector2Int(int tag)
        {
            int type = tag & TAG_MASK;
            if (type == VECTOR2INT)
                return ReadVector2Int();
            if (type == VECTOR3INT)
                return ReadVector3Int();
            if (type == VECTOR2)
                return new Vector2Int(ReadVector2());
            if (type == VECTOR3)
                return new Vector3Int(ReadVector3());
            if (type == VECTOR4)
                return new Vector3Int(ReadVector4());
            if (type == INTEGER)
                return new Vector2Int(ReadInt(), 0);
            if (type == FLOAT)
                return new Vector2Int((int)ReadFloat(), 0);
            if (type == DOUBLE)
                return new Vector2Int((int)ReadDouble(), 0);
            if (IGNORE_INCOMPATIBLE_FIELD)
            {
                SkipUnknownField(tag);
                return new Vector2Int();
            }
            throw new Exception("can not ReadVector2Int for type=" + type + " at " + ReadIndex + '/' + WriteIndex);
        }

        public Vector3Int ReadVector3Int(int tag)
        {
            int type = tag & TAG_MASK;
            if (type == VECTOR3INT)
                return ReadVector3Int();
            if (type == VECTOR2INT)
                return new Vector3Int(ReadVector2Int());
            if (type == VECTOR3)
                return new Vector3Int(ReadVector3());
            if (type == VECTOR2)
                return new Vector3Int(ReadVector2());
            if (type == VECTOR4)
                return new Vector3Int(ReadVector4());
            if (type == INTEGER)
                return new Vector3Int(ReadInt(), 0, 0);
            if (type == FLOAT)
                return new Vector3Int((int)ReadFloat(), 0, 0);
            if (type == DOUBLE)
                return new Vector3Int((int)ReadDouble(), 0, 0);
            if (IGNORE_INCOMPATIBLE_FIELD)
            {
                SkipUnknownField(tag);
                return new Vector3Int();
            }
            throw new Exception("can not ReadVector3Int for type=" + type + " at " + ReadIndex + '/' + WriteIndex);
        }

        public T ReadBean<T>(T bean, int tag) where T : Serializable
        {
            int type = tag & TAG_MASK;
            if (type == BEAN)
                bean.Decode(this);
            else if (type == DYNAMIC)
            {
                SkipLong();
                bean.Decode(this);
            }
            else if (IGNORE_INCOMPATIBLE_FIELD)
                SkipUnknownField(tag);
            else
            {
                throw new Exception("can not ReadBean(" + bean.GetType().Name + ") for type=" + type
                                    + " at " + ReadIndex + '/' + WriteIndex);
            }
            return bean;
        }

#if USE_CONFCS
        public ConfDynamicBean ReadDynamic(ConfDynamicBean dynBean, int tag)
#else
        public Transaction.DynamicBean ReadDynamic(Transaction.DynamicBean dynBean, int tag)
#endif
        {
            int type = tag & TAG_MASK;
            if (type == DYNAMIC)
            {
                dynBean.Decode(this);
                return dynBean;
            }
            if (type == BEAN)
            {
                var bean = dynBean.CreateBeanFromSpecialTypeId(0);
                if (bean != null)
                {
                    bean.Decode(this);
                    return dynBean;
                }
            }
            if (IGNORE_INCOMPATIBLE_FIELD)
            {
                SkipUnknownField(tag);
                return dynBean;
            }
            throw new Exception("can not ReadDynamic for type=" + type + " at " + ReadIndex + '/' + WriteIndex);
        }

        public void SkipUnknownFieldOrThrow(int tag, string curType)
        {
            if (IGNORE_INCOMPATIBLE_FIELD)
            {
                SkipUnknownField(tag);
                return;
            }
            throw new Exception("can not read " + curType + " for type=" + (tag & TAG_MASK)
                                + " at " + ReadIndex + '/' + WriteIndex);
        }

        public void SkipUnknownField(int tag, int count)
        {
            while (--count >= 0)
                SkipUnknownField(tag);
        }

        public void SkipUnknownField(int type1, int type2, int count)
        {
            type1 |= 0x10; // ensure high bits not zero
            type2 |= 0x10; // ensure high bits not zero
            while (--count >= 0)
            {
                SkipUnknownField(type1);
                SkipUnknownField(type2);
            }
        }

        public void SkipUnknownField(int tag)
        {
            int type = tag & TAG_MASK;
            switch (type)
            {
                case INTEGER:
                    SkipLong();
                    return;
                case FLOAT:
                    if (tag == FLOAT) // high bits == 0
                        return;
                    Skip(4);
                    return;
                case DOUBLE:
                case VECTOR2:
                    Skip(8);
                    return;
                case VECTOR2INT:
                    SkipLong();
                    SkipLong();
                    return;
                case VECTOR3:
                    Skip(12);
                    return;
                case VECTOR3INT:
                    SkipLong();
                    SkipLong();
                    SkipLong();
                    return;
                case VECTOR4:
                    Skip(16);
                    return;
                case BYTES:
                    SkipBytes();
                    return;
                case LIST:
                    int t = ReadByte();
                    SkipUnknownField(t, ReadTagSize(t));
                    return;
                case MAP:
                    t = ReadByte();
                    SkipUnknownField(t >> TAG_SHIFT, t, ReadUInt());
                    return;
                case DYNAMIC:
                    SkipLong();
                    goto case BEAN;
                case BEAN:
                    while ((t = ReadByte()) != 0)
                    {
                        if ((t & ID_MASK) == 0xf0)
                            SkipUInt();
                        SkipUnknownField(t);
                    }
                    return;
                default:
                    throw new Exception("SkipUnknownField: type=" + type + " at " + ReadIndex + '/' + WriteIndex);
            }
        }

        public static void BuildString<T>(StringBuilder sb, IEnumerable<T> c)
        {
            Str.BuildString(sb, c);
        }

        public static void BuildString<TK, TV>(StringBuilder sb, IDictionary<TK, TV> dic, IComparer<TK> comparer = null)
        {
            Str.BuildString(sb, dic, comparer);
        }

        public static bool Equals(byte[] left, byte[] right)
        {
            if (left == null || right == null)
                return left == right;
            if (left.Length != right.Length)
                return false;
            for (int i = 0; i < left.Length; i++)
            {
                if (left[i] != right[i])
                    return false;
            }
            return true;
        }

        public static int Compare(byte[] left, byte[] right)
        {
            if (left == null || right == null)
            {
                if (left == right) // both null
                    return 0;
                if (left == null) // null is small
                    return -1;
                return 1;
            }
            if (left.Length != right.Length)
                return left.Length.CompareTo(right.Length); // shorter is small

            for (int i = 0; i < left.Length; i++)
            {
                int c = left[i].CompareTo(right[i]);
                if (c != 0)
                    return c;
            }
            return 0;
        }

        [MethodImpl(MethodImplOptions.AggressiveInlining)]
        public static byte[] Copy(byte[] src)
        {
            byte[] result = new byte[src.Length];
            Buffer.BlockCopy(src, 0, result, 0, src.Length);
            return result;
        }

        [MethodImpl(MethodImplOptions.AggressiveInlining)]
        public static byte[] Copy(byte[] src, int offset, int length)
        {
            byte[] result = new byte[length];
            Buffer.BlockCopy(src, offset, result, 0, length);
            return result;
        }
    }
}
