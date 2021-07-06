using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;
using Microsoft.VisualStudio.TestTools.UnitTesting;
using Zeze.Util;

namespace UnitTest.Zeze.Util
{
    [TestClass]
    public class TestHugeArray
    {
        [TestMethod]
        public void Test1()
        {
            {
                var a = new HugeArray<string>(0);
                Assert.AreEqual(a.Count, 0);
                Assert.AreEqual(a.BlockSize, 256);
                Assert.AreEqual(a.BlockBits, 8);
                Assert.AreEqual(a.BlockMask, 255);
                Assert.IsTrue(a[0] == null);
                a[1] = "1";
                Assert.AreEqual(a.Count, 2);
                Assert.AreEqual(a.BlockCount, 1);
                Assert.IsTrue(a[0] == null);
                Assert.IsTrue(a[1].Equals("1"));
                Assert.IsTrue(a.GetOrAdd(0, ()=>"0").Equals("0"));
                Assert.IsTrue(a[0].Equals("0"));

                a[257] = "257";
                Assert.AreEqual(a.Count, 258);
                Assert.AreEqual(a.BlockCount, 2);
                Assert.IsTrue(a[255] == null);
                Assert.IsTrue(a[256] == null);
                Assert.IsTrue(a[257].Equals("257"));

                a[-1] = "-1";
                Assert.AreEqual(a.Count, 258 + 1);
                Assert.AreEqual(a.BlockCount, 3);
                Assert.IsTrue(a[-1].Equals("-1"));

                a[-256] = "-256";
                Assert.AreEqual(a.Count, 258 + 256);
                Assert.AreEqual(a.BlockCount, 3);
                Assert.IsTrue(a[-256].Equals("-256"));

                a[-257] = "-257";
                Assert.AreEqual(a.Count, 258 + 257);
                Assert.AreEqual(a.BlockCount, 4);
                Assert.IsTrue(a[-257].Equals("-257"));
            }
            {
                var a = new HugeArray<string>(256);
                Assert.AreEqual(a.Count, 0);
                Assert.AreEqual(a.BlockSize, 256);
                Assert.AreEqual(a.BlockBits, 8);
                Assert.AreEqual(a.BlockMask, 255);
            }
            {
                var a = new HugeArray<string>(900);
                Assert.AreEqual(a.Count, 0);
                Assert.AreEqual(a.BlockSize, 1024);
                Assert.AreEqual(a.BlockBits, 10);
                Assert.AreEqual(a.BlockMask, 1023);
            }
        }
    }
}
