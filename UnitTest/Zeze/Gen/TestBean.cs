using System;
using System.Collections.Generic;
using System.Text;
using Microsoft.VisualStudio.TestTools.UnitTesting;
using Zeze.Serialize;

namespace UnitTest.Zeze.Gen
{
    [TestClass]
    public class TestBean
    {
        [TestMethod]
        public void TestEncode()
        {
            demo.Module1.Value e = new demo.Module1.Value();
            ByteBuffer bb = ByteBuffer.Allocate();
            e.Encode(bb);

            demo.Module1.Value d = new demo.Module1.Value();
            d.Decode(bb);

            Assert.AreEqual(bb.ReadIndex, bb.WriteIndex);
            Assert.AreEqual(e.ToString(), d.ToString());
        }

        [TestMethod]
        public void TestCopy()
        {
            demo.Module1.Value e = new demo.Module1.Value();
            demo.Module1.Value d = e.Copy();
            Assert.AreEqual(e.ToString(), d.ToString());
        }
    }
}
