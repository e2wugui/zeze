using System;
using Microsoft.VisualStudio.TestTools.UnitTesting;

using Zeze.Serialize;

namespace UnitTest.Zeze.Serialize
{
    [TestClass]
    public class TestByteBuffer
    {
        [TestMethod]
        public void TestBytes()
        {
            ByteBuffer bb = ByteBuffer.Allocate();

            byte[] v = new byte[0];
            bb.WriteBytes(v);
            Assert.AreEqual(1, bb.Size);
            Assert.AreEqual("00", bb.ToString());
            Assert.AreEqual(BitConverter.ToString(v), BitConverter.ToString(bb.ReadBytes()));
            Assert.AreEqual(bb.ReadIndex, bb.WriteIndex);

            v = new byte[]{ 1, 2 };
            bb.WriteBytes(v);
            Assert.AreEqual(3, bb.Size);
            //Console.WriteLine(bb);
            Assert.AreEqual("02-01-02", bb.ToString());
            Assert.AreEqual(BitConverter.ToString(v), BitConverter.ToString(bb.ReadBytes()));
            Assert.AreEqual(bb.ReadIndex, bb.WriteIndex);
        }

        [TestMethod]
        public void TestBasic()
        {
            ByteBuffer bb = ByteBuffer.Allocate();
            Assert.AreEqual(bb.ReadIndex, bb.WriteIndex);

            {
                bool v = true;
                bb.WriteBool(v);
                Assert.AreEqual(1, bb.Size);
                Assert.AreEqual(1, bb.Bytes[bb.ReadIndex]);
                Assert.AreEqual(v, bb.ReadBool());
                Assert.AreEqual(bb.ReadIndex, bb.WriteIndex);
            }
            {
                byte v = 1;
                bb.WriteByte(v);
                Assert.AreEqual(1, bb.Size);
                Assert.AreEqual(1, bb.Bytes[bb.ReadIndex]);
                Assert.AreEqual(v, bb.ReadByte());
                Assert.AreEqual(bb.ReadIndex, bb.WriteIndex);
            }
            {
                double v = 1.1;
                bb.WriteDouble(v);
                Assert.AreEqual(8, bb.Size);
                //Console.WriteLine(bb);
                Assert.AreEqual("9A-99-99-99-99-99-F1-3F", bb.ToString());
                Assert.AreEqual(v, bb.ReadDouble());
                Assert.AreEqual(bb.ReadIndex, bb.WriteIndex);
            }
            {
                float v = 1.1f;
                bb.WriteFloat(v);
                Assert.AreEqual(4, bb.Size);
                //Console.WriteLine(bb);
                Assert.AreEqual("CD-CC-8C-3F", bb.ToString());
                Assert.AreEqual(v, bb.ReadFloat());
                Assert.AreEqual(bb.ReadIndex, bb.WriteIndex);
            }
            {
                int int4 = 0x12345678;
                bb.WriteInt4(int4);
                Assert.AreEqual(4, bb.Size);
                //Console.WriteLine(bb);
                Assert.AreEqual("78-56-34-12", bb.ToString());
                Assert.AreEqual(int4, bb.ReadInt4());
                Assert.AreEqual(bb.ReadIndex, bb.WriteIndex);
            }
            {
                long long8 = 0x1234567801020304;
                bb.WriteLong8(long8);
                Assert.AreEqual(8, bb.Size);
                //Console.WriteLine(bb);
                Assert.AreEqual("04-03-02-01-78-56-34-12", bb.ToString());
                Assert.AreEqual(long8, bb.ReadLong8());
                Assert.AreEqual(bb.ReadIndex, bb.WriteIndex);
            }
            {
                long long8 = -12345678;
                bb.WriteLong8(long8);
                Assert.AreEqual(8, bb.Size);
                //Console.WriteLine(bb);
                Assert.AreEqual("B2-9E-43-FF-FF-FF-FF-FF", bb.ToString());
                Assert.AreEqual(long8, bb.ReadLong8());
                Assert.AreEqual(bb.ReadIndex, bb.WriteIndex);
            }
            {
                long long8 = -1;
                bb.WriteLong8(long8);
                Assert.AreEqual(8, bb.Size);
                //Console.WriteLine(bb);
                Assert.AreEqual("FF-FF-FF-FF-FF-FF-FF-FF", bb.ToString());
                Assert.AreEqual(long8, bb.ReadLong8());
                Assert.AreEqual(bb.ReadIndex, bb.WriteIndex);
            }
        }

        [TestMethod]
        public void TestShort()
        {
            ByteBuffer bb = ByteBuffer.Allocate();
            Assert.AreEqual(bb.ReadIndex, bb.WriteIndex);

            short v = 1;
            bb.WriteShort(v);
            Assert.AreEqual(1, bb.Size);
            //Console.WriteLine(bb);
            Assert.AreEqual("01", bb.ToString());
            Assert.AreEqual(v, bb.ReadShort());
            Assert.AreEqual(bb.ReadIndex, bb.WriteIndex);

            v = 0x80;
            bb.WriteShort(v);
            Assert.AreEqual(2, bb.Size);
            //Console.WriteLine(bb);
            Assert.AreEqual("80-80", bb.ToString());
            Assert.AreEqual(v, bb.ReadShort());
            Assert.AreEqual(bb.ReadIndex, bb.WriteIndex);

            v = 0x4000;
            bb.WriteShort(v);
            Assert.AreEqual(3, bb.Size);
            //Console.WriteLine(bb);
            Assert.AreEqual("FF-40-00", bb.ToString());
            Assert.AreEqual(v, bb.ReadShort());
            Assert.AreEqual(bb.ReadIndex, bb.WriteIndex);

            v = -1;
            bb.WriteShort(v);
            Assert.AreEqual(3, bb.Size);
            //Console.WriteLine(bb);
            Assert.AreEqual("FF-FF-FF", bb.ToString());
            Assert.AreEqual(v, bb.ReadShort());
            Assert.AreEqual(bb.ReadIndex, bb.WriteIndex);
        }

        [TestMethod]
        public void TestInt()
        {
            ByteBuffer bb = ByteBuffer.Allocate();
            Assert.AreEqual(bb.ReadIndex, bb.WriteIndex);

            int v = 1;
            bb.WriteInt(v);
            Assert.AreEqual(1, bb.Size);
            //Console.WriteLine(bb);
            Assert.AreEqual("01", bb.ToString());
            Assert.AreEqual(v, bb.ReadInt());
            Assert.AreEqual(bb.ReadIndex, bb.WriteIndex);

            v = 0x80;
            bb.WriteInt(v);
            Assert.AreEqual(2, bb.Size);
            //Console.WriteLine(bb);
            Assert.AreEqual("80-80", bb.ToString());
            Assert.AreEqual(v, bb.ReadInt());
            Assert.AreEqual(bb.ReadIndex, bb.WriteIndex);

            v = 0x4000;
            bb.WriteInt(v);
            Assert.AreEqual(3, bb.Size);
            //Console.WriteLine(bb);
            Assert.AreEqual("C0-40-00", bb.ToString());
            Assert.AreEqual(v, bb.ReadInt());
            Assert.AreEqual(bb.ReadIndex, bb.WriteIndex);

            v = 0x200000;
            bb.WriteInt(v);
            Assert.AreEqual(4, bb.Size);
            //Console.WriteLine(bb);
            Assert.AreEqual("E0-20-00-00", bb.ToString());
            Assert.AreEqual(v, bb.ReadInt());
            Assert.AreEqual(bb.ReadIndex, bb.WriteIndex);

            v = 0x10000000;
            bb.WriteInt(v);
            Assert.AreEqual(5, bb.Size);
            //Console.WriteLine(bb);
            Assert.AreEqual("F0-10-00-00-00", bb.ToString());
            Assert.AreEqual(v, bb.ReadInt());
            Assert.AreEqual(bb.ReadIndex, bb.WriteIndex);
        }

        [TestMethod]
        public void TestLong()
        {
            ByteBuffer bb = ByteBuffer.Allocate();
            Assert.AreEqual(bb.ReadIndex, bb.WriteIndex);

            long v = 1;
            bb.WriteLong(v);
            Assert.AreEqual(1, bb.Size);
            //Console.WriteLine(bb);
            Assert.AreEqual("01", bb.ToString());
            Assert.AreEqual(v, bb.ReadLong());
            Assert.AreEqual(bb.ReadIndex, bb.WriteIndex);

            v = 0x80;
            bb.WriteLong(v);
            Assert.AreEqual(2, bb.Size);
            //Console.WriteLine(bb);
            Assert.AreEqual("80-80", bb.ToString());
            Assert.AreEqual(v, bb.ReadLong());
            Assert.AreEqual(bb.ReadIndex, bb.WriteIndex);

            v = 0x4000;
            bb.WriteLong(v);
            Assert.AreEqual(3, bb.Size);
            //Console.WriteLine(bb);
            Assert.AreEqual("C0-40-00", bb.ToString());
            Assert.AreEqual(v, bb.ReadLong());
            Assert.AreEqual(bb.ReadIndex, bb.WriteIndex);

            v = 0x200000;
            bb.WriteLong(v);
            Assert.AreEqual(4, bb.Size);
            //Console.WriteLine(bb);
            Assert.AreEqual("E0-20-00-00", bb.ToString());
            Assert.AreEqual(v, bb.ReadLong());
            Assert.AreEqual(bb.ReadIndex, bb.WriteIndex);

            v = 0x10000000;
            bb.WriteLong(v);
            Assert.AreEqual(5, bb.Size);
            //Console.WriteLine(bb);
            Assert.AreEqual("F0-10-00-00-00", bb.ToString());
            Assert.AreEqual(v, bb.ReadLong());
            Assert.AreEqual(bb.ReadIndex, bb.WriteIndex);

            v = 0x800000000L;
            bb.WriteLong(v);
            Assert.AreEqual(6, bb.Size);
            //Console.WriteLine(bb);
            Assert.AreEqual("F8-08-00-00-00-00", bb.ToString());
            Assert.AreEqual(v, bb.ReadLong());
            Assert.AreEqual(bb.ReadIndex, bb.WriteIndex);

            v = 0x40000000000L;
            bb.WriteLong(v);
            Assert.AreEqual(7, bb.Size);
            //Console.WriteLine(bb);
            Assert.AreEqual("FC-04-00-00-00-00-00", bb.ToString());
            Assert.AreEqual(v, bb.ReadLong());
            Assert.AreEqual(bb.ReadIndex, bb.WriteIndex);

            v = 0x2000000000000L;
            bb.WriteLong(v);
            Assert.AreEqual(8, bb.Size);
            //Console.WriteLine(bb);
            Assert.AreEqual("FE-02-00-00-00-00-00-00", bb.ToString());
            Assert.AreEqual(v, bb.ReadLong());
            Assert.AreEqual(bb.ReadIndex, bb.WriteIndex);

            v = 0x100000000000000L;
            bb.WriteLong(v);
            Assert.AreEqual(9, bb.Size);
            //Console.WriteLine(bb);
            Assert.AreEqual("FF-01-00-00-00-00-00-00-00", bb.ToString());
            Assert.AreEqual(v, bb.ReadLong());
            Assert.AreEqual(bb.ReadIndex, bb.WriteIndex);

            v = 0L;
            v = (long)((ulong)v | 0x8000000000000000L);
            bb.WriteLong(v);
            Console.WriteLine(v);
            Assert.AreEqual(9, bb.Size);
            //Console.WriteLine(bb);
            Assert.AreEqual("FF-80-00-00-00-00-00-00-00", bb.ToString());
            Assert.AreEqual(v, bb.ReadLong());
            Assert.AreEqual(bb.ReadIndex, bb.WriteIndex);
        }
    }
}
