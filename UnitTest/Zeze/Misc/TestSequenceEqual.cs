using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;
using Microsoft.VisualStudio.TestTools.UnitTesting;
using Zeze.Services.ServiceManager;

namespace UnitTest.Zeze.Misc
{
    [TestClass]
    public class TestSequenceEqual
    {
        [TestMethod]
        public void TestSequenceEqual1()
        {
            var infos1 = new Dictionary<string, ServiceInfo>();
            var info1 = new ServiceInfo("gs", "1", 0);
            infos1.Add("1", info1);

            var infos2 = new Dictionary<string, ServiceInfo>();
            var info2 = new ServiceInfo("gs", "1", 0);
            infos2.Add("1", info2);

            Assert.IsTrue(Enumerable.SequenceEqual(infos1.Values, infos2.Values));
        }
    }
}
