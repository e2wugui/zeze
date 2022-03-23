using System;
using System.Collections.Generic;
using System.Text;
using System.Threading.Tasks;
using Microsoft.VisualStudio.TestTools.UnitTesting;
using Zeze.Transaction;

namespace UnitTest.Zeze.Trans
{
    [TestClass]
    public class TestConflict
    {
        int sum;

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
        public void TestConflictAdd()
        {
            Assert.IsTrue(Procedure.Success == demo.App.Instance.Zeze.NewProcedure(ProcRemove, "ProcRemove").Call());
            Task[] tasks = new Task[2000];
            for (int i = 0; i < tasks.Length; ++i)
            {
               tasks[i] = demo.App.Instance.Zeze.NewProcedure(ProcAdd, "ProcAdd").CallAsync();
            }
            Task.WaitAll(tasks);
            sum = tasks.Length;
            Assert.IsTrue(Procedure.Success == demo.App.Instance.Zeze.NewProcedure(ProcVerify, "ProcVerify").Call());
            Assert.IsTrue(Procedure.Success == demo.App.Instance.Zeze.NewProcedure(ProcRemove, "ProcRemove").Call());
        }

        async Task<long> ProcRemove()
        {
            await demo.App.Instance.demo_Module1.Table1.RemoveAsync(123123);
            return Procedure.Success;
        }

        async Task<long> ProcAdd()
        {
            demo.Module1.Value v = await demo.App.Instance.demo_Module1.Table1.GetOrAddAsync(123123);
            v.Int1 += 1;
            return Procedure.Success;
        }

        async Task<long> ProcVerify()
        {
            demo.Module1.Value v = await demo.App.Instance.demo_Module1.Table1.GetOrAddAsync(123123);
            Assert.IsTrue(v.Int1 == sum);
            return Procedure.Success;
        }
    }
}
