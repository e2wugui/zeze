using System;
using System.Collections.Generic;
using System.Text;
using Microsoft.VisualStudio.TestTools.UnitTesting;
using Zeze.Util;

namespace UnitTest.Zeze.Util
{
    [TestClass]
    public class TestMessageFormatter
    {
        [TestMethod]
        public void TestFormatter()
        {
            {
                FormattingTuple tuple = MessageFormatter.arrayFormat("none", 1, "str");
                Assert.AreEqual("none", tuple.getMessage());
            }
            {
                FormattingTuple tuple = MessageFormatter.arrayFormat("1{}", 1, "str");
                Assert.AreEqual("11", tuple.getMessage());
            }
            {
                FormattingTuple tuple = MessageFormatter.arrayFormat("2{}{}", 1, "str");
                Assert.AreEqual("21str", tuple.getMessage());
            }
            {
                FormattingTuple tuple = MessageFormatter.arrayFormat("3{}{}{}", 1, "str");
                Assert.AreEqual("31str{}", tuple.getMessage());
            }
            {
                FormattingTuple tuple = MessageFormatter.arrayFormat("dup{1}{1}{2}", 1, "str");
                Assert.AreEqual("dup{1}{1}{2}", tuple.getMessage());
            }
        }
    }
}
