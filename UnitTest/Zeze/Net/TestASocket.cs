using System;
using System.Collections.Generic;
using System.Text;
using System.Threading;
using Microsoft.VisualStudio.TestTools.UnitTesting;
using Zeze.Net;

namespace UnitTest.Zeze.Net
{
    [TestClass]
    public class TestASocket
    {
        [TestMethod]
        public void TestConnect()
        {
            Manager manager = new Manager();
            using ASocket so = new ASocket(manager, "www.163.com", 80);
            Thread.Sleep(2000);
        }
    }
}
