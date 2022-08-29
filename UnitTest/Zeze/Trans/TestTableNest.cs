using System;
using System.Collections.Generic;
using System.Text;
using System.Threading.Tasks;
using Microsoft.VisualStudio.TestTools.UnitTesting;
using Zeze.Transaction;
using Zeze.Util;

namespace UnitTest.Zeze.Trans
{
    [TestClass]
    public class TestTableNest
    {
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
        public void TestNest()
        {
            Assert.IsTrue(ResultCode.Success == demo.App.Instance.Zeze.NewProcedure(ProcTableRemove, "ProcTableRemove").CallSynchronously());
            Assert.IsTrue(ResultCode.Success == demo.App.Instance.Zeze.NewProcedure(ProcTableAdd, "ProcTableAdd").CallSynchronously());
        }

        async Task<long> ProcTableRemove()
        {
            await demo.App.Instance.demo_Module1.Table1.RemoveAsync(4321);
            return ResultCode.Success;
        }

        async Task<long> ProcTableAdd()
        {
            demo.Module1.Value v1 = await demo.App.Instance.demo_Module1.Table1.GetOrAddAsync(4321);
            Assert.IsNotNull(v1);
            Assert.IsTrue(ResultCode.Success != await demo.App.Instance.Zeze.NewProcedure(
                ProcTablePutNestAndRollback, "ProcTablePutNestAndRollback").CallAsync());
            demo.Module1.Value v2 = await demo.App.Instance.demo_Module1.Table1.GetAsync(4321);
            Assert.IsNotNull(v1);
            Assert.IsTrue(v1 == v2);
            return ResultCode.Success;
        }

        async Task<long> ProcTablePutNestAndRollback()
        {
            demo.Module1.Value v = new demo.Module1.Value();
            await demo.App.Instance.demo_Module1.Table1.PutAsync(4321, v);
            return ResultCode.Unknown;
        }
    }
}
