using System;
using System.Collections.Generic;
using System.Text;
using System.Threading.Tasks;
using Microsoft.VisualStudio.TestTools.UnitTesting;
using Zeze.Transaction;

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
        public async void TestNest()
        {
            Assert.IsTrue(Procedure.Success == await demo.App.Instance.Zeze.NewProcedure(ProcTableRemove, "ProcTableRemove").CallAsync());
            Assert.IsTrue(Procedure.Success == await demo.App.Instance.Zeze.NewProcedure(ProcTableAdd, "ProcTableAdd").CallAsync());
        }

        async Task<long> ProcTableRemove()
        {
            await demo.App.Instance.demo_Module1.Table1.Remove(4321);
            return Procedure.Success;
        }

        async Task<long> ProcTableAdd()
        {
            demo.Module1.Value v1 = await demo.App.Instance.demo_Module1.Table1.GetOrAdd(4321);
            Assert.IsNotNull(v1);
            Assert.IsTrue(Procedure.Success != await demo.App.Instance.Zeze.NewProcedure(
                ProcTablePutNestAndRollback, "ProcTablePutNestAndRollback").CallAsync());
            demo.Module1.Value v2 = await demo.App.Instance.demo_Module1.Table1.Get(4321);
            Assert.IsNotNull(v1);
            Assert.IsTrue(v1 == v2);
            return Procedure.Success;
        }

        async Task<long> ProcTablePutNestAndRollback()
        {
            demo.Module1.Value v = new demo.Module1.Value();
            await demo.App.Instance.demo_Module1.Table1.Put(4321, v);
            return Procedure.Unknown;
        }
    }
}
