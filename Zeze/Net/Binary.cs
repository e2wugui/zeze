using System;
using System.Collections.Generic;
using System.Text;

namespace Zeze.Net
{
    // Bean 类型 binary 的辅助类。
    // 构造之后就是只读的。
    // byte[] bytes 参数传入以后，就不能再修改了。
    public class Binary
    {
        private byte[] _Bytes;

        public byte this[int index] { get { return _Bytes[index]; } }

        internal byte[] Bytes { get { return _Bytes; } } // 内部用于系列化和网络发送，读取操作。

        public int Offset { get; }
        public int Count { get; }

        private static readonly Binary _Empty = new Binary(System.Array.Empty<byte>());
        public static Binary Empty { get { return _Empty; } }

        /// <summary>
        /// 这里实际上直接wrap传入的bytes，所以必须保证之后不能再修改bytes的值了。
        /// </summary>
        /// <param name="bytes"></param>
        /// <param name="offset"></param>
        /// <param name="count"></param>
        public Binary(byte[] bytes, int offset, int count)
        {
            _Bytes = bytes;
            Offset = offset;
            Count = count;
        }

        /// <summary>
        /// 这里实际上直接wrap传入的bytes，所以必须保证之后不能再修改bytes的值了。
        /// </summary>
        /// <param name="bytes"></param>
        public Binary(byte[] bytes) : this(bytes, 0, bytes.Length)
        {
        }

        /// <summary>
        /// 这里实际上直接wrap传入的bytes，所以必须保证之后不能再修改bytes的值了。
        /// </summary>
        /// <param name="bb"></param>
        public Binary(Zeze.Serialize.ByteBuffer bb) : this(bb.Bytes, bb.ReadIndex, bb.Size)
        {

        }

        // 这里调用Copy是因为ByteBuffer可能分配的保留内存较大。Copy返回实际大小的数据。
        public Binary(Zeze.Serialize.Serializable _s_)
            : this(Zeze.Serialize.ByteBuffer.Encode(_s_).Copy())
        {
        }

        public void Decode(Zeze.Serialize.Serializable _s_)
        {
            Zeze.Serialize.ByteBuffer _bb_ = Zeze.Serialize.ByteBuffer.Wrap(_Bytes, Offset, Count);
            _s_.Decode(_bb_);
        }

        public override string ToString()
        {
            return System.BitConverter.ToString(_Bytes, Offset, Count);
        }

        public override bool Equals(object obj)
        {
            if (this == obj)
                return true;

            if (obj is Binary other)
                return Equals(other);

            return false;
        }

        public bool Equals(Binary other)
        {
            if (other == null)
                return false;

            if (this.Count != other.Count)
                return false;

            for (int i = 0, n = this.Count; i < n; ++i)
            {
                if (_Bytes[Offset + i] != other._Bytes[other.Offset + i])
                    return false;
            }

            return true;
        }

        public override int GetHashCode()
        {
            int hash = 0;
            for (int i = Offset; i < Count; ++i)
                hash += _Bytes[i];
            return hash;
        }
    }
}
