using System;
using System.Collections.Generic;
using System.Text;
using Zeze.Gen;
using Microsoft.VisualStudio.TestTools.UnitTesting;

namespace UnitTest.Zeze.Gen
{
    [TestClass]
    public class TestGen
    {
        [TestMethod]
        public void TestMain()
        {
            Program.Main(Array.Empty<string>());
        }
    }
}
