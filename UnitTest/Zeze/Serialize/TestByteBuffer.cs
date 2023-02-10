using System;
using System.Linq;
using System.Text;
using demo;
using demo.Module1;
using Microsoft.VisualStudio.TestTools.UnitTesting;
using Zeze.Net;
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
 
            var str = new string(new[]{(char)(0xd800 + 0x155), (char)(0xdc00 + 0x2aa),
                (char)(0xd800 + 0x2aa), (char)(0xdc00 + 0x155)}); // surrogate chars
            var b0 = Encoding.UTF8.GetBytes(str);
            bb.Reset();
            bb.WriteString(str);
            Assert.AreEqual(4 * 2, b0.Length);
            Assert.AreEqual(4 * 2, bb.Bytes[0]);
            Assert.IsTrue(b0.SequenceEqual(bb.ReadBytes()));
            bb.ReadIndex = 0;
            Assert.AreEqual(str, bb.ReadString());
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
        public void TestUInt()
        {
            ByteBuffer bb = ByteBuffer.Allocate();
            Assert.AreEqual(bb.ReadIndex, bb.WriteIndex);

            int v = 1;
            bb.WriteUInt(v);
            Assert.AreEqual(1, bb.Size);
            //Console.WriteLine(bb);
            Assert.AreEqual("01", bb.ToString());
            Assert.AreEqual(v, bb.ReadUInt());
            Assert.AreEqual(bb.ReadIndex, bb.WriteIndex);

            v = 0x80;
            bb.WriteUInt(v);
            Assert.AreEqual(2, bb.Size);
            //Console.WriteLine(bb);
            Assert.AreEqual("80-80", bb.ToString());
            Assert.AreEqual(v, bb.ReadUInt());
            Assert.AreEqual(bb.ReadIndex, bb.WriteIndex);

            v = 0x4000;
            bb.WriteUInt(v);
            Assert.AreEqual(3, bb.Size);
            //Console.WriteLine(bb);
            Assert.AreEqual("C0-40-00", bb.ToString());
            Assert.AreEqual(v, bb.ReadUInt());
            Assert.AreEqual(bb.ReadIndex, bb.WriteIndex);

            v = 0x20_0000;
            bb.WriteUInt(v);
            Assert.AreEqual(4, bb.Size);
            //Console.WriteLine(bb);
            Assert.AreEqual("E0-20-00-00", bb.ToString());
            Assert.AreEqual(v, bb.ReadUInt());
            Assert.AreEqual(bb.ReadIndex, bb.WriteIndex);

            v = 0x1000_0000;
            bb.WriteUInt(v);
            Assert.AreEqual(5, bb.Size);
            //Console.WriteLine(bb);
            Assert.AreEqual("F0-10-00-00-00", bb.ToString());
            Assert.AreEqual(v, bb.ReadUInt());
            Assert.AreEqual(bb.ReadIndex, bb.WriteIndex);

            v = -1;
            bb.WriteUInt(v);
            Assert.AreEqual(5, bb.Size);
            //Console.WriteLine(bb);
            Assert.AreEqual("F0-FF-FF-FF-FF", bb.ToString());
            Assert.AreEqual(v, bb.ReadUInt());
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
            Assert.AreEqual("40-80", bb.ToString());
            Assert.AreEqual(v, bb.ReadLong());
            Assert.AreEqual(bb.ReadIndex, bb.WriteIndex);

            v = 0x4000;
            bb.WriteLong(v);
            Assert.AreEqual(3, bb.Size);
            //Console.WriteLine(bb);
            Assert.AreEqual("60-40-00", bb.ToString());
            Assert.AreEqual(v, bb.ReadLong());
            Assert.AreEqual(bb.ReadIndex, bb.WriteIndex);

            v = 0x20_0000;
            bb.WriteLong(v);
            Assert.AreEqual(4, bb.Size);
            //Console.WriteLine(bb);
            Assert.AreEqual("70-20-00-00", bb.ToString());
            Assert.AreEqual(v, bb.ReadLong());
            Assert.AreEqual(bb.ReadIndex, bb.WriteIndex);

            v = 0x1000_0000;
            bb.WriteLong(v);
            Assert.AreEqual(5, bb.Size);
            //Console.WriteLine(bb);
            Assert.AreEqual("78-10-00-00-00", bb.ToString());
            Assert.AreEqual(v, bb.ReadLong());
            Assert.AreEqual(bb.ReadIndex, bb.WriteIndex);

            v = 0x8_0000_0000L;
            bb.WriteLong(v);
            Assert.AreEqual(6, bb.Size);
            //Console.WriteLine(bb);
            Assert.AreEqual("7C-08-00-00-00-00", bb.ToString());
            Assert.AreEqual(v, bb.ReadLong());
            Assert.AreEqual(bb.ReadIndex, bb.WriteIndex);

            v = 0x400_0000_0000L;
            bb.WriteLong(v);
            Assert.AreEqual(7, bb.Size);
            //Console.WriteLine(bb);
            Assert.AreEqual("7E-04-00-00-00-00-00", bb.ToString());
            Assert.AreEqual(v, bb.ReadLong());
            Assert.AreEqual(bb.ReadIndex, bb.WriteIndex);

            v = 0x2_0000_0000_0000L;
            bb.WriteLong(v);
            Assert.AreEqual(8, bb.Size);
            //Console.WriteLine(bb);
            Assert.AreEqual("7F-02-00-00-00-00-00-00", bb.ToString());
            Assert.AreEqual(v, bb.ReadLong());
            Assert.AreEqual(bb.ReadIndex, bb.WriteIndex);

            v = 0x100_0000_0000_0000L;
            bb.WriteLong(v);
            Assert.AreEqual(9, bb.Size);
            //Console.WriteLine(bb);
            Assert.AreEqual("7F-81-00-00-00-00-00-00-00", bb.ToString());
            Assert.AreEqual(v, bb.ReadLong());
            Assert.AreEqual(bb.ReadIndex, bb.WriteIndex);

            v = 0L;
            v = (long)((ulong)v | 0x8000_0000_0000_0000L);
            bb.WriteLong(v);
            Console.WriteLine(v);
            Assert.AreEqual(9, bb.Size);
            //Console.WriteLine(bb);
            Assert.AreEqual("80-00-00-00-00-00-00-00-00", bb.ToString());
            Assert.AreEqual(v, bb.ReadLong());
            Assert.AreEqual(bb.ReadIndex, bb.WriteIndex);

            v = -1;
            bb.WriteLong(v);
            Assert.AreEqual(1, bb.Size);
            //Console.WriteLine(bb);
            Assert.AreEqual("FF", bb.ToString());
            Assert.AreEqual(v, bb.ReadLong());
            Assert.AreEqual(bb.ReadIndex, bb.WriteIndex);
        }

        static void TestInt(int x)
        {
            ByteBuffer bb = ByteBuffer.Allocate();
            bb.WriteInt(x);
            int y = bb.ReadInt();
            Assert.AreEqual(x, y);
            Assert.AreEqual(bb.ReadIndex, bb.WriteIndex);
        }

        static void TestLong(long x)
        {
            ByteBuffer bb = ByteBuffer.Allocate();
            bb.WriteLong(x);
            long y = bb.ReadLong();
            Assert.AreEqual(x, y);
            Assert.AreEqual(bb.ReadIndex, bb.WriteIndex);
        }

        static void TestUInt(int x)
        {
            ByteBuffer bb = ByteBuffer.Allocate();
            bb.WriteUInt(x);
            int y = bb.ReadUInt();
            Assert.AreEqual(x, y);
            Assert.AreEqual(bb.ReadIndex, bb.WriteIndex);
        }

        static void TestULong(long x)
        {
            ByteBuffer bb = ByteBuffer.Allocate();
            bb.WriteULong(x);
            long y = bb.ReadULong();
            Assert.AreEqual(x, y);
            Assert.AreEqual(bb.ReadIndex, bb.WriteIndex);
        }

        static void TestAll(long x)
        {
            TestInt((int)x);
            TestInt((int)-x);
            TestUInt((int)x);
            TestUInt((int)-x);
            TestLong(x);
            TestLong(-x);
            TestULong(x);
            TestULong(-x);
        }

        [TestMethod]
        public void TestInteger()
        {
            for (int i = 0; i <= 64; ++i)
            {
                TestAll(1L << i);
                TestAll((1L << i) - 1);
                unchecked
                {
                    TestAll(((1L << i) - 1) & 0x5555_5555_5555_5555L);
                    TestAll(((1L << i) - 1) & (long)0xaaaa_aaaa_aaaa_aaaaL);
                }
            }
            TestInt(int.MinValue);
            TestInt(int.MaxValue);
            TestLong(int.MinValue);
            TestLong(int.MaxValue);
            TestLong(long.MinValue);
            TestLong(long.MaxValue);
            TestUInt(int.MinValue);
            TestUInt(int.MaxValue);
            TestULong(int.MinValue);
            TestULong(int.MaxValue);
            TestULong(long.MinValue);
            TestULong(long.MaxValue);
        }

        [TestMethod]
        public void TestBean()
        {
            Value v = new();
            v.String3 = "abc";
            v.Bytes8 = new Binary(Encoding.UTF8.GetBytes("xyz"));
            Bean1 bean1 = new(123);
            bean1.V2.Add(12, 34);
            v.List9.Add(bean1);
            Simple simple = new();
            simple.Removed.Int1 = 999;
            v.Map16.Add(new Key(11), simple);

            ByteBuffer bb = ByteBuffer.Allocate();
            v.Encode(bb);
            Value v2 = new Value();
            v2.Decode(bb);
            bb.ReadIndex = 0;
            ByteBuffer bb2 = ByteBuffer.Allocate();
            v2.Encode(bb2);

            // Console.WriteLine(v);
            // Console.WriteLine(v2);

            Assert.AreEqual(bb.Size, bb2.Size);
            Assert.AreEqual(bb, bb2);
        }
    }
}
