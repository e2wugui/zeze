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
    public class TestTable
    {
        private static readonly NLog.Logger logger = NLog.LogManager.GetCurrentClassLogger();

        [TestInitialize]
        public void TestInit()
        {
            demo.App.Instance.Start();
            demo.App.Instance.Zeze.NewProcedure(async () =>
            {
                await demo.App.Instance.demo_Module1.Table1.RemoveAsync(1);
                await demo.App.Instance.demo_Module1.Table2.RemoveAsync(new demo.Module1.Key(1));
                return Procedure.Success;
            }, "RemoveDataFirst").Call();
        }

        [TestCleanup]
        public void TestCleanup()
        {
            demo.App.Instance.Stop();
        }

        [TestMethod]
        public void TestUpdate()
        {
            Assert.IsTrue(Procedure.Success == demo.App.Instance.Zeze.NewProcedure(ProcGetOrAdd, "ProcGetOrAdd").Call());
            Assert.IsTrue(Procedure.Success == demo.App.Instance.Zeze.NewProcedure(ProcGetUpdate, "ProcGetUpdate").Call());
            Assert.IsTrue(Procedure.Success == demo.App.Instance.Zeze.NewProcedure(ProcGetUpdateCheckRemove, "ProcGetUpdateCheckRemove").Call());
        }

        async Task<long> ProcGetUpdate()
        {
            demo.Module1.Value v = await demo.App.Instance.demo_Module1.Table1.GetAsync(1);

            v.Int1 = 11;
            v.Long2 = 22;
            v.String3 = "33";
            v.Bool4 = true;
            v.Short5 = 55;
            v.Float6 = 66;
            v.Double7 = 77;
            v.List9.Add(new demo.Bean1());
            v.Set10.Add(1010);
            v.Map11.Add(2, new demo.Module2.Value());
            v.Bean12.Int1 = 1212;
            v.Byte13 = 131;
            return Procedure.Success;
        }

        async Task<long> ProcGetUpdateCheckRemove()
        {
            demo.Module1.Value v = await demo.App.Instance.demo_Module1.Table1.GetAsync(1);

            Assert.IsTrue(v.Int1 == 11);
            Assert.IsTrue(v.Long2 == 22);
            Assert.IsTrue(v.String3.Equals("33"));
            Assert.IsTrue(v.Bool4);
            Assert.IsTrue(v.Short5 == 55);
            Assert.IsTrue(v.Float6 == 66);
            Assert.IsTrue(v.Double7 == 77);
            Assert.IsTrue(v.List9.Count == 2);
            Assert.IsTrue(v.Set10.Contains(10));
            Assert.IsTrue(v.Set10.Contains(1010));
            Assert.IsTrue(v.Set10.Count == 2);
            Assert.IsTrue(v.Map11.Count == 2);
            Assert.IsTrue(v.Bean12.Int1 == 1212);
            Assert.IsTrue(v.Byte13 == 131);

            return Procedure.Success;
        }

        [TestMethod]
        public void TestGetOrAdd()
        {
            Assert.IsTrue(Procedure.Success == demo.App.Instance.Zeze.NewProcedure(ProcGetOrAdd, "ProcGetOrAdd").Call());
            Assert.IsTrue(Procedure.Success == demo.App.Instance.Zeze.NewProcedure(ProcGetOrAddCheckAndRemove, "ProcGetOrAddCheckAndRemove").Call());
        }

        async Task<long> ProcGetOrAdd()
        {
            demo.Module1.Value v = await demo.App.Instance.demo_Module1.Table1.GetOrAddAsync(1);

            v.Int1 = 1;
            v.Long2 = 2;
            v.String3 = "3";
            v.Bool4 = true;
            v.Short5 = 5;
            v.Float6 = 6;
            v.Double7 = 7;
            v.List9.Add(new demo.Bean1());
            v.Set10.Add(10);
            v.Map11.Add(1, new demo.Module2.Value());
            v.Bean12.Int1 = 12;
            v.Byte13 = 13;

            return Procedure.Success;
        }

        async Task<long> ProcGetOrAddCheckAndRemove()
        {
            var v = await demo.App.Instance.demo_Module1.Table1.GetAsync(1);
            Assert.IsNotNull(v);

            Assert.IsTrue(v.Int1 == 1);
            Assert.IsTrue(v.Long2 == 2);
            Assert.IsTrue(v.String3.Equals("3"));
            Assert.IsTrue(v.Bool4);
            Assert.IsTrue(v.Short5 == 5);
            Assert.IsTrue(v.Float6 == 6);
            Assert.IsTrue(v.Double7 == 7);
            Assert.IsTrue(v.List9.Count == 1);
            Assert.IsTrue(v.Set10.Contains(10));
            Assert.IsTrue(v.Set10.Count == 1);
            Assert.IsTrue(v.Map11.Count == 1);
            Assert.IsTrue(v.Bean12.Int1 == 12);
            Assert.IsTrue(v.Byte13 == 13);

            await demo.App.Instance.demo_Module1.Table1.RemoveAsync(1);
            Assert.IsNull(await demo.App.Instance.demo_Module1.Table1.GetAsync(1));
            return Procedure.Success;
        }

        [TestMethod]
        public void Test1TableGetPut()
        {
            Assert.IsTrue(Procedure.Success == demo.App.Instance.Zeze.NewProcedure(ProcGet11, "ProcGet11").Call());
            Assert.IsTrue(Procedure.Success == demo.App.Instance.Zeze.NewProcedure(ProcGet12, "ProcGet12").Call());
        }

        [TestMethod]
        public void Test2TableGetPut()
        {
            Assert.IsTrue(Procedure.Success == demo.App.Instance.Zeze.NewProcedure(ProcGet21, "ProcGet21").Call());
            Assert.IsTrue(Procedure.Success == demo.App.Instance.Zeze.NewProcedure(ProcGet22, "ProcGet22").Call());
        }

        async Task<long> ProcGet21()
        {
            await ProcGet11();
            demo.Module1.Key key = new demo.Module1.Key(1);
            Assert.IsNull(await demo.App.Instance.demo_Module1.Table2.GetAsync(key));
            demo.Module1.Value v = new demo.Module1.Value();

            v.Int1 = 1;
            v.Long2 = 2;
            v.String3 = "3";
            v.Bool4 = true;
            v.Short5 = 5;
            v.Float6 = 6;
            v.Double7 = 7;
            v.List9.Add(new demo.Bean1());
            v.Set10.Add(10);
            v.Map11.Add(1, new demo.Module2.Value());
            v.Bean12.Int1 = 12;
            v.Byte13 = 13;

            await demo.App.Instance.demo_Module1.Table2.PutAsync(key, v);
            Assert.IsTrue(v == await demo.App.Instance.demo_Module1.Table2.GetAsync(key));
            return Procedure.Success;
        }

        async Task<long> ProcGet22()
        {
            await ProcGet12();
            demo.Module1.Key key = new demo.Module1.Key(1);
            var v = await demo.App.Instance.demo_Module1.Table2.GetAsync(key);
            Assert.IsNotNull(v);

            Assert.IsTrue(v.Int1 == 1);
            Assert.IsTrue(v.Long2 == 2);
            Assert.IsTrue(v.String3.Equals("3"));
            Assert.IsTrue(v.Bool4);
            Assert.IsTrue(v.Short5 == 5);
            Assert.IsTrue(v.Float6 == 6);
            Assert.IsTrue(v.Double7 == 7);
            Assert.IsTrue(v.List9.Count == 1);
            Assert.IsTrue(v.Set10.Contains(10));
            Assert.IsTrue(v.Set10.Count == 1);
            Assert.IsTrue(v.Map11.Count == 1);
            Assert.IsTrue(v.Bean12.Int1 == 12);
            Assert.IsTrue(v.Byte13 == 13);

            await demo.App.Instance.demo_Module1.Table2.RemoveAsync(key);
            Assert.IsNull(demo.App.Instance.demo_Module1.Table2.GetAsync(key));
            return Procedure.Success;
        }

        async Task<long> ProcGet11()
        {
            Assert.IsNull(await demo.App.Instance.demo_Module1.Table1.GetAsync(1));
            demo.Module1.Value v = new demo.Module1.Value();

            v.Int1 = 1;
            v.Long2 = 2;
            v.String3 = "3";
            v.Bool4 = true;
            v.Short5 = 5;
            v.Float6 = 6;
            v.Double7 = 7;
            v.List9.Add(new demo.Bean1());
            v.Set10.Add(10);
            v.Map11.Add(1, new demo.Module2.Value());
            v.Bean12.Int1 = 12;
            v.Byte13 = 13;

            await demo.App.Instance.demo_Module1.Table1.PutAsync(1, v);
            Assert.IsTrue(v == await demo.App.Instance.demo_Module1.Table1.GetAsync(1));
            return Procedure.Success;
        }

        async Task<long> ProcGet12()
        {
            var v = await demo.App.Instance.demo_Module1.Table1.GetAsync(1);
            Assert.IsNotNull(v);

            Assert.IsTrue(v.Int1 == 1);
            Assert.IsTrue(v.Long2 == 2);
            Assert.IsTrue(v.String3.Equals("3"));
            Assert.IsTrue(v.Bool4);
            Assert.IsTrue(v.Short5 == 5);
            Assert.IsTrue(v.Float6 == 6);
            Assert.IsTrue(v.Double7 == 7);
            Assert.IsTrue(v.List9.Count == 1);
            Assert.IsTrue(v.Set10.Contains(10));
            Assert.IsTrue(v.Set10.Count == 1);
            Assert.IsTrue(v.Map11.Count == 1);
            Assert.IsTrue(v.Bean12.Int1 == 12);
            Assert.IsTrue(v.Byte13 == 13);

            await demo.App.Instance.demo_Module1.Table1.RemoveAsync(1);
            Assert.IsNull(demo.App.Instance.demo_Module1.Table1.GetAsync(1));
            return Procedure.Success;
        }
    }
}
