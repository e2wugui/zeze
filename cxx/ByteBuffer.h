
#pragma once

#include <string.h>
#include <stdexcept>

namespace Zeze
{
namespace Serialize
{
    class ByteBuffer;

    class Serializable
    {
    public:
        virtual void Decode(ByteBuffer & bb) = 0;
        virtual void Encode(ByteBuffer & bb) = 0;
        virtual ~Serializable() { }
    };

    class ByteBuffer
    {
        ByteBuffer() = delete;
        ByteBuffer(const ByteBuffer&) = delete;
        ByteBuffer& operator=(const ByteBuffer&) = delete;

    public:
        unsigned char * Bytes;
        int ReadIndex;
        int WriteIndex;
        int Capacity;
        int Size() { return WriteIndex - ReadIndex; }

        explicit ByteBuffer(int capacity)
        {
            IsEncodeMode = true;
            Capacity = ToPower2(capacity);
            Bytes = new unsigned char[(size_t)Capacity];
            ReadIndex = 0;
            WriteIndex = 0;
        }

        // 应该仅用于Decode。
        ByteBuffer(unsigned char* bytes, int offset, int length)
        {
            IsEncodeMode = false;
            Bytes = bytes;
            Capacity = length; // XXX
            ReadIndex = offset;
            WriteIndex = offset + length;
        }

        ~ByteBuffer()
        {
            if (IsEncodeMode)
                delete[] Bytes;
        }
    private:
        bool IsEncodeMode;

    public:
        void Append(char b)
        {
            EnsureWrite(1);
            Bytes[WriteIndex] = (unsigned char)b;
            WriteIndex += 1;
        }

        void Append(const char * src, int offset, int len)
        {
            EnsureWrite(len);
            memcpy(Bytes + WriteIndex, src + offset, (size_t)len);
            WriteIndex += len;
        }

        void Replace(int writeIndex, const char* src, int offset, int len)
        {
            if (writeIndex < ReadIndex || writeIndex + len > WriteIndex)
                throw std::exception();
            memcpy(Bytes + writeIndex, src + offset, (size_t)len);
        }

        void BeginWriteWithSize4(int & state)
        {
            state = Size();
            EnsureWrite(4);
            WriteIndex += 4;
        }

        void EndWriteWithSize4(int state)
        {
			int oldWriteIndex = state + ReadIndex;
            int size = WriteIndex - oldWriteIndex - 4;
            Replace(oldWriteIndex, (const char *)(&size), 0, 4);
        }

        void BeginWriteSegment(int & oldSize)
        {
            oldSize = Size();
            EnsureWrite(1);
            WriteIndex += 1;
        }

        void EndWriteSegment(int oldSize)
        {
            int startPos = ReadIndex + oldSize;
            int segmentSize = WriteIndex - startPos - 1;

            // 0 111 1111
            if (segmentSize < 0x80)
            {
                Bytes[startPos] = (unsigned char)segmentSize;
            }
            else if (segmentSize < 0x4000) // 10 11 1111, -
            {
                EnsureWrite(1);
                Bytes[WriteIndex] = Bytes[startPos + 1];
                Bytes[startPos + 1] = (unsigned char)segmentSize;

                Bytes[startPos] = (unsigned char)((segmentSize >> 8) | 0x80);
                WriteIndex += 1;
            }
            else if (segmentSize < 0x200000) // 110 1 1111, -,-
            {
                EnsureWrite(2);
                Bytes[WriteIndex + 1] = Bytes[startPos + 2];
                Bytes[startPos + 2] = (unsigned char)segmentSize;

                Bytes[WriteIndex] = Bytes[startPos + 1];
                Bytes[startPos + 1] = (unsigned char)(segmentSize >> 8);

                Bytes[startPos] = (unsigned char)((segmentSize >> 16) | 0xc0);
                WriteIndex += 2;
            }
            else if (segmentSize < 0x10000000) // 1110 1111,-,-,-
            {
                EnsureWrite(3);
                Bytes[WriteIndex + 2] = Bytes[startPos + 3];
                Bytes[startPos + 3] = (unsigned char)segmentSize;

                Bytes[WriteIndex + 1] = Bytes[startPos + 2];
                Bytes[startPos + 2] = (unsigned char)(segmentSize >> 8);

                Bytes[WriteIndex] = Bytes[startPos + 1];
                Bytes[startPos + 1] = (unsigned char)(segmentSize >> 16);

                Bytes[startPos] = (unsigned char)((segmentSize >> 24) | 0xe0);
                WriteIndex += 3;
            }
            else
            {
                throw std::exception("exceed max segment size");
            }
        }

        void ReadSegment(int & startIndex, int & segmentSize)
        {
            EnsureRead(1);
            int h = Bytes[ReadIndex++];

            startIndex = ReadIndex;

            if (h < 0x80)
            {
                segmentSize = h;
                ReadIndex += segmentSize;
            }
            else if (h < 0xc0)
            {
                EnsureRead(1);
                segmentSize = ((h & 0x3f) << 8) | Bytes[ReadIndex];
                int endPos = ReadIndex + segmentSize;
                Bytes[ReadIndex] = Bytes[endPos];
                ReadIndex += segmentSize + 1;
            }
            else if (h < 0xe0)
            {
                EnsureRead(2);
                segmentSize = ((h & 0x1f) << 16) | ((int)Bytes[ReadIndex] << 8) | Bytes[ReadIndex + 1];
                int endPos = ReadIndex + segmentSize;
                Bytes[ReadIndex] = Bytes[endPos];
                Bytes[ReadIndex + 1] = Bytes[endPos + 1];
                ReadIndex += segmentSize + 2;
            }
            else if (h < 0xf0)
            {
                EnsureRead(3);
                segmentSize = ((h & 0x0f) << 24) | ((int)Bytes[ReadIndex] << 16) | ((int)Bytes[ReadIndex + 1] << 8) | Bytes[ReadIndex + 2];
                int endPos = ReadIndex + segmentSize;
                Bytes[ReadIndex] = Bytes[endPos];
                Bytes[ReadIndex + 1] = Bytes[endPos + 1];
                Bytes[ReadIndex + 2] = Bytes[endPos + 2];
                ReadIndex += segmentSize + 3;
            }
            else
            {
                throw std::exception("exceed max size");
            }
            if (ReadIndex > WriteIndex)
            {
                throw std::exception("segment data not enough");
            }
        }

        void BeginReadSegment(int & saveState)
        {
            int startPos;
            int segmentSize;
            ReadSegment(startPos, segmentSize);

            saveState = ReadIndex;
            ReadIndex = startPos;
        }

        void EndReadSegment(int saveState)
        {
            ReadIndex = saveState;
        }

        void Campact()
        {
            int size = Size();
            if (size > 0)
            {
                if (ReadIndex > 0)
                {
                    memmove(Bytes, Bytes + ReadIndex, (size_t)size);
                    ReadIndex = 0;
                    WriteIndex = size;
                }
            }
            else
            {
                Reset();
            }
        }

        /*
        char* Copy()
        {
            int size = Size();
            char * copy = new char[size];
            memcpy(copy, Bytes + ReadIndex, size);
            return copy;
        }
        */

        void Reset()
        {
            ReadIndex = WriteIndex = 0;
        }

        static int ToPower2(int needSize)
        {
            int size = 1024;
            while (size < needSize)
                size <<= 1;
            return size;
        }


        void EnsureWrite(int size)
        {
            if (false == IsEncodeMode)
                throw std::exception("not encode mode");

            int newSize = WriteIndex + size;
            if (newSize > Capacity)
            {
                Capacity = ToPower2(newSize);
                unsigned char * newBytes = new unsigned char[(size_t)Capacity];
                WriteIndex -= ReadIndex;
                memcpy(newBytes, Bytes + ReadIndex, (size_t)WriteIndex);
                ReadIndex = 0;
                delete [] Bytes;
                Bytes = newBytes;
            }
        }

        void EnsureRead(int size)
        {
            if (IsEncodeMode)
                throw std::exception("not decode mode");

            if (ReadIndex + size > WriteIndex)
                 throw std::exception("EnsureRead");
        }

        void WriteBool(bool b)
        {
            EnsureWrite(1);
            Bytes[WriteIndex++] = (unsigned char)(b ? 1 : 0);
        }

        bool ReadBool()
        {
            EnsureRead(1);
            return Bytes[ReadIndex++] != 0;
        }

        void WriteByte(char x)
        {
            EnsureWrite(1);
            Bytes[WriteIndex++] = (unsigned char)x;
        }

        char ReadByte()
        {
            EnsureRead(1);
            return (char)Bytes[ReadIndex++];
        }


        void WriteShort(short x)
        {
            if (x >= 0)
            {
                if (x < 0x80)
                {
                    EnsureWrite(1);
                    Bytes[WriteIndex++] = (unsigned char)x;
                    return;
                }

                if (x < 0x4000)
                {
                    EnsureWrite(2);
                    Bytes[WriteIndex + 1] = (unsigned char)x;
                    Bytes[WriteIndex] = (unsigned char)((x >> 8) | 0x80);
                    WriteIndex += 2;
                    return;
                }
            }
            EnsureWrite(3);
            Bytes[WriteIndex] = (unsigned char)0xff;
            Bytes[WriteIndex + 2] = (unsigned char)x;
            Bytes[WriteIndex + 1] = (unsigned char)(x >> 8);
            WriteIndex += 3;
        }

        short ReadShort()
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
            throw std::exception();
        }

        void WriteInt4(int x)
        {
            char * bs = (char*)&x;
            Append(bs, 0, 4);
        }

        int ReadInt4()
        {
            EnsureRead(4);
            int x = *(int*)(Bytes + ReadIndex);
            ReadIndex += 4;
            return x;
        }

        void WriteLong8(long long x)
        {
            char * bs = (char*)(&x);
            //if (false == BitConverter.IsLittleEndian)
            //    Array.Reverse(bs);
            Append(bs, 0, 8);
        }

        long long ReadLong8()
        {
            EnsureRead(8);
            long long x = *(long long *)(Bytes + ReadIndex);
            ReadIndex += 8;
            return x;
        }

        void WriteInt(int x)
        {
            WriteUint((unsigned int)x);
        }

        int ReadInt()
        {
            return (int)ReadUint();
        }

    private:
        void WriteUint(unsigned int x)
        {
            // 0 111 1111
            if (x < 0x80)
            {
                EnsureWrite(1);
                Bytes[WriteIndex++] = (unsigned char)x;
            }
            else if (x < 0x4000) // 10 11 1111, -
            {
                EnsureWrite(2);
                Bytes[WriteIndex + 1] = (unsigned char)x;
                Bytes[WriteIndex] = (unsigned char)((x >> 8) | 0x80);
                WriteIndex += 2;
            }
            else if (x < 0x200000) // 110 1 1111, -,-
            {
                EnsureWrite(3);
                Bytes[WriteIndex + 2] = (unsigned char)x;
                Bytes[WriteIndex + 1] = (unsigned char)(x >> 8);
                Bytes[WriteIndex] = (unsigned char)((x >> 16) | 0xc0);
                WriteIndex += 3;
            }
            else if (x < 0x10000000) // 1110 1111,-,-,-
            {
                EnsureWrite(4);
                Bytes[WriteIndex + 3] = (unsigned char)x;
                Bytes[WriteIndex + 2] = (unsigned char)(x >> 8);
                Bytes[WriteIndex + 1] = (unsigned char)(x >> 16);
                Bytes[WriteIndex] = (unsigned char)((x >> 24) | 0xe0);
                WriteIndex += 4;
            }
            else
            {
                EnsureWrite(5);
                Bytes[WriteIndex] = (unsigned char)0xf0;
                Bytes[WriteIndex + 4] = (unsigned char)x;
                Bytes[WriteIndex + 3] = (unsigned char)(x >> 8);
                Bytes[WriteIndex + 2] = (unsigned char)(x >> 16);
                Bytes[WriteIndex + 1] = (unsigned char)(x >> 24);
                WriteIndex += 5;
            }
        }

        unsigned int ReadUint()
        {
            EnsureRead(1);
            unsigned int h = Bytes[ReadIndex];
            if (h < 0x80)
            {
                ReadIndex++;
                return h;
            }
            else if (h < 0xc0)
            {
                EnsureRead(2);
                unsigned int x = ((h & 0x3f) << 8) | Bytes[ReadIndex + 1];
                ReadIndex += 2;
                return x;
            }
            else if (h < 0xe0)
            {
                EnsureRead(3);
                unsigned int x = ((h & 0x1f) << 16) | ((unsigned int)Bytes[ReadIndex + 1] << 8) | Bytes[ReadIndex + 2];
                ReadIndex += 3;
                return x;
            }
            else if (h < 0xf0)
            {

                EnsureRead(4);
                unsigned int x = ((h & 0x0f) << 24) | ((unsigned int)Bytes[ReadIndex + 1] << 16) | ((unsigned int)Bytes[ReadIndex + 2] << 8) | Bytes[ReadIndex + 3];
                ReadIndex += 4;
                return x;
            }
            else
            {
                EnsureRead(5);
                unsigned int x = ((unsigned int)Bytes[ReadIndex + 1] << 24) | ((unsigned int)(Bytes[ReadIndex + 2] << 16)) | ((unsigned int)Bytes[ReadIndex + 3] << 8) | Bytes[ReadIndex + 4];
                ReadIndex += 5;
                return x;
            }
        }
    public:
        void WriteLong(long long x)
        {
            WriteUlong((unsigned long long)x);
        }

        long long ReadLong()
        {
            return (long long)ReadUlong();
        }

    private:
        void WriteUlong(unsigned long long x)
        {
            // 0 111 1111
            if (x < 0x80)
            {
                EnsureWrite(1);
                Bytes[WriteIndex++] = (unsigned char)x;
            }
            else if (x < 0x4000) // 10 11 1111, -
            {
                EnsureWrite(2);
                Bytes[WriteIndex + 1] = (unsigned char)x;
                Bytes[WriteIndex] = (unsigned char)((x >> 8) | 0x80);
                WriteIndex += 2;
            }
            else if (x < 0x200000) // 110 1 1111, -,-
            {
                EnsureWrite(3);
                Bytes[WriteIndex + 2] = (unsigned char)x;
                Bytes[WriteIndex + 1] = (unsigned char)(x >> 8);
                Bytes[WriteIndex] = (unsigned char)((x >> 16) | 0xc0);
                WriteIndex += 3;
            }
            else if (x < 0x10000000) // 1110 1111,-,-,-
            {
                EnsureWrite(4);
                Bytes[WriteIndex + 3] = (unsigned char)x;
                Bytes[WriteIndex + 2] = (unsigned char)(x >> 8);
                Bytes[WriteIndex + 1] = (unsigned char)(x >> 16);
                Bytes[WriteIndex] = (unsigned char)((x >> 24) | 0xe0);
                WriteIndex += 4;
            }
            else if (x < 0x800000000L) // 1111 0xxx,-,-,-,-
            {
                EnsureWrite(5);
                Bytes[WriteIndex + 4] = (unsigned char)x;
                Bytes[WriteIndex + 3] = (unsigned char)(x >> 8);
                Bytes[WriteIndex + 2] = (unsigned char)(x >> 16);
                Bytes[WriteIndex + 1] = (unsigned char)(x >> 24);
                Bytes[WriteIndex] = (unsigned char)((x >> 32) | 0xf0);
                WriteIndex += 5;
            }
            else if (x < 0x40000000000L) // 1111 10xx, 
            {
                EnsureWrite(6);
                Bytes[WriteIndex + 5] = (unsigned char)x;
                Bytes[WriteIndex + 4] = (unsigned char)(x >> 8);
                Bytes[WriteIndex + 3] = (unsigned char)(x >> 16);
                Bytes[WriteIndex + 2] = (unsigned char)(x >> 24);
                Bytes[WriteIndex + 1] = (unsigned char)(x >> 32);
                Bytes[WriteIndex] = (unsigned char)((x >> 40) | 0xf8);
                WriteIndex += 6;
            }
            else if (x < 0x200000000000L) // 1111 110x,
            {
                EnsureWrite(7);
                Bytes[WriteIndex + 6] = (unsigned char)x;
                Bytes[WriteIndex + 5] = (unsigned char)(x >> 8);
                Bytes[WriteIndex + 4] = (unsigned char)(x >> 16);
                Bytes[WriteIndex + 3] = (unsigned char)(x >> 24);
                Bytes[WriteIndex + 2] = (unsigned char)(x >> 32);
                Bytes[WriteIndex + 1] = (unsigned char)(x >> 40);
                Bytes[WriteIndex] = (unsigned char)((x >> 48) | 0xfc);
                WriteIndex += 7;
            }
            else if (x < 0x100000000000000L) // 1111 1110
            {
                EnsureWrite(8);
                Bytes[WriteIndex + 7] = (unsigned char)x;
                Bytes[WriteIndex + 6] = (unsigned char)(x >> 8);
                Bytes[WriteIndex + 5] = (unsigned char)(x >> 16);
                Bytes[WriteIndex + 4] = (unsigned char)(x >> 24);
                Bytes[WriteIndex + 3] = (unsigned char)(x >> 32);
                Bytes[WriteIndex + 2] = (unsigned char)(x >> 40);
                Bytes[WriteIndex + 1] = (unsigned char)(x >> 48);
                Bytes[WriteIndex] = (unsigned char)0xfe;
                WriteIndex += 8;
            }
            else // 1111 1111
            {
                EnsureWrite(9);
                Bytes[WriteIndex] = (unsigned char)0xff;
                Bytes[WriteIndex + 8] = (unsigned char)x;
                Bytes[WriteIndex + 7] = (unsigned char)(x >> 8);
                Bytes[WriteIndex + 6] = (unsigned char)(x >> 16);
                Bytes[WriteIndex + 5] = (unsigned char)(x >> 24);
                Bytes[WriteIndex + 4] = (unsigned char)(x >> 32);
                Bytes[WriteIndex + 3] = (unsigned char)(x >> 40);
                Bytes[WriteIndex + 2] = (unsigned char)(x >> 48);
                Bytes[WriteIndex + 1] = (unsigned char)(x >> 56);
                WriteIndex += 9;
            }
        }

        unsigned long long ReadUlong()
        {
            EnsureRead(1);
            unsigned int h = Bytes[ReadIndex];
            if (h < 0x80)
            {
                ReadIndex++;
                return h;
            }
            else if (h < 0xc0)
            {
                EnsureRead(2);
                unsigned int x = ((h & 0x3f) << 8) | Bytes[ReadIndex + 1];
                ReadIndex += 2;
                return x;
            }
            else if (h < 0xe0)
            {
                EnsureRead(3);
                unsigned int x = ((h & 0x1f) << 16) | ((unsigned int)Bytes[ReadIndex + 1] << 8) | Bytes[ReadIndex + 2];
                ReadIndex += 3;
                return x;
            }
            else if (h < 0xf0)
            {
                EnsureRead(4);
                unsigned int x = ((h & 0x0f) << 24) | ((unsigned int)Bytes[ReadIndex + 1] << 16) | ((unsigned int)Bytes[ReadIndex + 2] << 8) | Bytes[ReadIndex + 3];
                ReadIndex += 4;
                return x;
            }
            else if (h < 0xf8)
            {
                EnsureRead(5);
                unsigned int xl = ((unsigned int)Bytes[ReadIndex + 1] << 24) | ((unsigned int)(Bytes[ReadIndex + 2] << 16)) | ((unsigned int)Bytes[ReadIndex + 3] << 8) | (Bytes[ReadIndex + 4]);
                unsigned int xh = h & 0x07;
                ReadIndex += 5;
                return ((unsigned long long)xh << 32) | xl;
            }
            else if (h < 0xfc)
            {
                EnsureRead(6);
                unsigned int xl = ((unsigned int)Bytes[ReadIndex + 2] << 24) | ((unsigned int)(Bytes[ReadIndex + 3] << 16)) | ((unsigned int)Bytes[ReadIndex + 4] << 8) | (Bytes[ReadIndex + 5]);
                unsigned int xh = ((h & 0x03) << 8) | Bytes[ReadIndex + 1];
                ReadIndex += 6;
                return ((unsigned long long)xh << 32) | xl;
            }
            else if (h < 0xfe)
            {
                EnsureRead(7);
                unsigned int xl = ((unsigned int)Bytes[ReadIndex + 3] << 24) | ((unsigned int)(Bytes[ReadIndex + 4] << 16)) | ((unsigned int)Bytes[ReadIndex + 5] << 8) | (Bytes[ReadIndex + 6]);
                unsigned int xh = ((h & 0x01) << 16) | ((unsigned int)Bytes[ReadIndex + 1] << 8) | Bytes[ReadIndex + 2];
                ReadIndex += 7;
                return ((unsigned long long)xh << 32) | xl;
            }
            else if (h < 0xff)
            {
                EnsureRead(8);
                unsigned int xl = ((unsigned int)Bytes[ReadIndex + 4] << 24) | ((unsigned int)(Bytes[ReadIndex + 5] << 16)) | ((unsigned int)Bytes[ReadIndex + 6] << 8) | (Bytes[ReadIndex + 7]);
                unsigned int xh = /*((h & 0x01) << 24) |*/ ((unsigned int)Bytes[ReadIndex + 1] << 16) | ((unsigned int)Bytes[ReadIndex + 2] << 8) | Bytes[ReadIndex + 3];
                ReadIndex += 8;
                return ((unsigned long long)xh << 32) | xl;
            }
            else
            {
                EnsureRead(9);
                unsigned int xl = ((unsigned int)Bytes[ReadIndex + 5] << 24) | ((unsigned int)(Bytes[ReadIndex + 6] << 16)) | ((unsigned int)Bytes[ReadIndex + 7] << 8) | (Bytes[ReadIndex + 8]);
                unsigned int xh = ((unsigned int)Bytes[ReadIndex + 1] << 24) | ((unsigned int)Bytes[ReadIndex + 2] << 16) | ((unsigned int)Bytes[ReadIndex + 3] << 8) | Bytes[ReadIndex + 4];
                ReadIndex += 9;
                return ((unsigned long long)xh << 32) | xl;
            }
        }

    public:
        void WriteFloat(float x)
        {
            char * bs = (char *)(&x);
            Append(bs, 0, 4);
        }

        float ReadFloat()
        {
            EnsureRead(4);
            float x = *(float *)(Bytes + ReadIndex);
            ReadIndex += 4;
            return x;
        }

        void WriteDouble(double x)
        {
            char * bs = (char*)(&x);
            Append(bs, 0, 8);
        }

        double ReadDouble()
        {
            EnsureRead(8);
            double x = *(double*)(Bytes + ReadIndex);
            ReadIndex += 8;
            return x;
        }

        // string must be utf-8 encode
        void WriteString(const std::string & x)
        {
            WriteBytes(x);
        }

        std::string ReadString()
        {
            int n = ReadInt();
            EnsureRead(n);
            std::string x((const char *)(Bytes + ReadIndex), (size_t)n);
            ReadIndex += n;
            return x;
        }

        void ReadStringNoCopy(const char* & outstr, int& outlength)
        {
            int n = ReadInt();
            EnsureRead(n);
            outstr = (const char *)(Bytes + ReadIndex);
            outlength = n;
            ReadIndex += n;
        }

        void WriteBytes(const std::string & x)
        {
            int length = (int)x.length();
            WriteInt(length);
            EnsureWrite(length);
            memcpy(Bytes + WriteIndex, x.data(), (size_t)length);
            WriteIndex += length;
        }

        std::string ReadBytes()
        {
            return ReadString();
        }

        void SkipBytes()
        {
            int n = ReadInt();
            EnsureRead(n);
            ReadIndex += n;
        }

        // 只能增加新的类型定义，增加时记得同步 SkipUnknownField
    public:
        const static int
            INT = 0,
            LONG = 1,
            STRING = 2,
            BOOL = 3,
            BYTE = 4,
            SHORT = 5,
            FLOAT = 6,
            DOUBLE = 7,
            BYTES = 8,
            LIST = 9,
            SET = 10,
            MAP = 11,
            BEAN = 12,
            DYNAMIC = 13,
            TAG_MAX = 31;

        const static int TAG_SHIFT = 5;
        const static int TAG_MASK = (1 << TAG_SHIFT) - 1;
        const static int ID_MASK = (1 << (31 - TAG_SHIFT)) - 1;

        static void SkipUnknownField(int tagid, ByteBuffer bb)
        {
            int tagType = tagid & TAG_MASK;
            switch (tagType)
            {
            case BOOL:
                bb.ReadBool();
                break;
            case BYTE:
                bb.ReadByte();
                break;
            case SHORT:
                bb.ReadShort();
                break;
            case INT:
                bb.ReadInt();
                break;
            case LONG:
                bb.ReadLong();
                break;
            case FLOAT:
                bb.ReadFloat();
                break;
            case DOUBLE:
                bb.ReadDouble();
                break;
            case STRING:
            case BYTES:
            case LIST:
            case SET:
            case MAP:
            case BEAN:
                bb.SkipBytes();
                break;
            case DYNAMIC:
                bb.ReadLong8();
                bb.SkipBytes();
                break;
            default:
                throw std::exception("SkipUnknownField");
            }
        }
    };
} // namespace Serialize
} // namespace Zeze
