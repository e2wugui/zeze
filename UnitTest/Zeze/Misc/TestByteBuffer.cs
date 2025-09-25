using Microsoft.VisualStudio.TestTools.UnitTesting;
using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;
using Zeze.Serialize;

namespace UnitTest.Zeze.Misc
{
    [TestClass]
    public class TestByteBuffer
    {
        [TestMethod]
        public void Test1()
        {
            var b1 = new byte[5];
            var b2 = new byte[8];
            var b3 = new byte[10];

            var len = 4 + b1.Length + 4 + b2.Length + 4 + b3.Length;
            var bb = ByteBuffer.Allocate(len);
            bb.WriteInt4(b1.Length);
            bb.Append(b1);
            bb.WriteInt4(b2.Length);
            bb.Append(b2);
            bb.WriteInt4(b3.Length);
            bb.Append(b3);
            Assert.AreEqual(len, bb.Bytes.Length);
        }
    }
}
