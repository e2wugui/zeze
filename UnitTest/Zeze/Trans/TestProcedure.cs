using System;
using System.Collections.Generic;
using System.Text;
using System.Threading.Tasks;
using Microsoft.VisualStudio.TestTools.UnitTesting;
using Zeze.Serialize;
using Zeze.Transaction;

namespace UnitTest.Zeze.Trans
{
    [TestClass]
    public class TestProcedure
    {
        readonly TestBegin.MyBean bean = new();

#pragma warning disable CS1998 // Async method lacks 'await' operators and will run synchronously
        public async Task<long> ProcTrue()
#pragma warning restore CS1998 // Async method lacks 'await' operators and will run synchronously
        {
            bean.I = 123;
            Assert.AreEqual(bean.I, 123);
            return Procedure.Success;
        }

#pragma warning disable CS1998 // Async method lacks 'await' operators and will run synchronously
        public async Task<long> ProcFalse()
#pragma warning restore CS1998 // Async method lacks 'await' operators and will run synchronously
        {
            bean.I = 456;
            Assert.AreEqual(bean.I, 456);
            return Procedure.Unknown;
        }

        public async Task<long> ProcNest()
        {
            Assert.AreEqual(bean.I, 0);
            bean.I = 1;
            Assert.AreEqual(bean.I, 1);
            {
                var r = await demo.App.Instance.Zz.NewProcedure(ProcFalse, "ProcFalse").CallAsync();
                Assert.IsTrue(r != Procedure.Success);
                Assert.AreEqual(bean.I, 1);
            }

            {
                var r = await demo.App.Instance.Zz.NewProcedure(ProcTrue, "ProcFalse").CallAsync();
                Assert.IsTrue(r == Procedure.Success);
                Assert.AreEqual(bean.I, 123);
            }

            return Procedure.Success;
        }

        [TestInitialize]
        public void TestInit()
        {
            demo.App.Instance.Start();
        }

        [TestCleanup]
        public void TestCleanup()
        {
            demo.App.Instance.Stop();
        }

        [TestMethod]
        public void Test1()
        {
            var root = new TableKey("1", 1);
            // 特殊测试，拼凑一个record用来提供需要的信息。
            var r = new Record<long, TestBegin.MyBean>(null, 1, bean);
            bean.InitRootInfo(r.CreateRootInfoIfNeed(root), null);
            var rc = demo.App.Instance.Zz.NewProcedure(ProcNest, "ProcNest").CallSynchronously();
            Assert.IsTrue(rc == Procedure.Success);
            // 最后一个 Call，事务外，bean 已经没法访问事务支持的属性了。直接访问内部变量。
            Assert.AreEqual(bean._i, 123);
        }
    }
}
