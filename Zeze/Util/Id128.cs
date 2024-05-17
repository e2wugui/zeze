using System;
using System.Text;
using Zeze.Serialize;
using System.Numerics;

namespace Zeze.Util
{
    public class Id128 : IComparable<Id128>, Serializable
    {
        public static readonly Id128 Zero = new Id128Zero();

        private ulong low;
        private ulong high;

        public Id128()
        {
        }

        public Id128(ulong high, ulong low)
        {
            this.low = low;
            this.high = high;
        }

        public ulong getLow()
        {
            return low;
        }

        public ulong getHigh()
        {
            return high;
        }

        public virtual void Assign(ulong high, ulong low)
        {
            this.low = low;
            this.high = high;
        }

        public virtual void Assign(Id128 id128)
        {
            low = id128.low;
            high = id128.high;
        }

        /**
         * 增加id的值。直接改变现有变量。
         */
        public virtual void Increment(ulong num)
        {
            low += num;
            if (low < num)
                high++;
        }

        /**
         * 增加id的值，返回一个新对象。
         *
         * @return new Id128 instance that added.
         */
        public Id128 Add(ulong num)
        {
            var result = Clone();
            result.Increment(num);
            return result;
        }

        public Id128 Clone()
        {
            return new Id128(high, low);
        }

        public void EncodeRaw(ByteBuffer bb)
        {
            bb.WriteULong((long)high);
            bb.WriteULong((long)low);
        }

        public virtual void DecodeRaw(ByteBuffer bb)
        {
            high = (ulong)bb.ReadULong();
            low = (ulong)bb.ReadULong();
        }

        public void Encode(ByteBuffer bb)
        {
            bb.WriteByte((1 << ByteBuffer.TAG_SHIFT) + ByteBuffer.LIST);
            bb.WriteListType(2, ByteBuffer.INTEGER);
            bb.WriteLong((long)high);
            bb.WriteLong((long)low);
            bb.WriteByte(0); // end of bean
        }

        public virtual void Decode(ByteBuffer bb)
        {
            int t = bb.ReadByte();
            if (bb.ReadTagSize(t) == 1)
            {
                t &= ByteBuffer.TAG_MASK;
                if (t != ByteBuffer.LIST)
                    throw new Exception("Decode Id128 error: type=" + t);
                t = bb.ReadByte();
                int n = bb.ReadTagSize(t);
                t &= ByteBuffer.TAG_MASK;
                if (t != ByteBuffer.INTEGER)
                    throw new Exception("Decode Id128 error: subtype=" + t);
                if (n != 2)
                    throw new Exception("Decode Id128 error: size=" + n);
                high = (ulong)bb.ReadLong();
                low = (ulong)bb.ReadLong();
                bb.ReadTagSize(t = bb.ReadByte());
            }
            bb.SkipUnknownFieldOrThrow(t, "Id128");
        }

        public override int GetHashCode()
        {
            return low.GetHashCode() ^ high.GetHashCode();
        }

        public override bool Equals(object o)
        {
            if (this == o)
                return true;
            if (!(o is Id128))
                return false;
            var id128 = (Id128)o;
            return low == id128.low && high == id128.high;
        }

        public int CompareTo(Id128 o)
        {
            var c = high.CompareTo(o.high);
            return c != 0 ? c : low.CompareTo(o.low);
        }

        public override string ToString()
        {
            var bytes = new byte[17];
            Buffer.BlockCopy(BitConverter.GetBytes(low), 0, bytes, 1, 8);
            Buffer.BlockCopy(BitConverter.GetBytes(high), 0, bytes, 9, 8);
            return new BigInteger(bytes).ToString();
        }

        public void BuildString(StringBuilder sb, int level)
        {
            sb.Append(Str.Indent(level)).Append(this);
        }

        private class Id128Zero : Id128
        {
            public override void Assign(Id128 id128)
            {
                throw new NotImplementedException();
            }


            public override void Assign(ulong high, ulong low)
            {
                throw new NotImplementedException();
            }


            public override void Increment(ulong num)
            {
                throw new NotImplementedException();
            }

            public override void DecodeRaw(ByteBuffer bb)
            {
                throw new NotImplementedException();
            }

            public override void Decode(ByteBuffer bb)
            {
                throw new NotImplementedException();
            }
        }
    }
}
