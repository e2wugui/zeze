using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;
using Microsoft.VisualStudio.TestTools.UnitTesting;
using Zeze.Transaction;

namespace UnitTest.Zeze.Trans
{
    [TestClass]
    public class TestNestProcedureModifyMapSet
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
        public void TestNestModifyMap()
        {
            demo.App.Instance.Zeze.NewProcedure(() =>
            {
                demo.App.Instance.demo_Module1.Table1.Remove(1);
                return 0;
            }, "ModifyMapRemove").Call();

            demo.App.Instance.Zeze.NewProcedure(() =>
            {
                var value = demo.App.Instance.demo_Module1.Table1.GetOrAdd(1);
                value.Map15[1] = 1;

                demo.App.Instance.Zeze.NewProcedure(() =>
                {
                    Assert.IsTrue(value.Map15.TryGetValue(1, out var mv1));
                    Assert.AreEqual(1, mv1);
                    value.Map15[1] = 2;
                    Assert.IsTrue(value.Map15.TryGetValue(1, out var mv2));
                    Assert.AreEqual(2, mv2);
                    return Procedure.LogicError;
                }, "ModifyMapPut2").Call();

                Assert.IsTrue(value.Map15.TryGetValue(1, out var mv1));
                Assert.AreEqual(1, mv1);
                return 0;
            }, "ModifyMapPut1");
        }

        [TestMethod]
        public void TestNestModifySet()
        {
            demo.App.Instance.Zeze.NewProcedure(() =>
            {
                demo.App.Instance.demo_Module1.Table1.Remove(1);
                return 0;
            }, "ModifyMapRemove").Call();

            demo.App.Instance.Zeze.NewProcedure(() =>
            {
                var value = demo.App.Instance.demo_Module1.Table1.GetOrAdd(1);
                value.Set10.Add(1);

                demo.App.Instance.Zeze.NewProcedure(() =>
                {
                    Assert.IsTrue(value.Set10.Contains(1));
                    value.Set10.Remove(1);
                    Assert.IsFalse(value.Set10.Contains(1));
                    return Procedure.LogicError;
                }, "ModifySetRemove1").Call();

                Assert.IsTrue(value.Set10.Contains(1));
                return 0;
            }, "ModifyMapAdd1");
        }
    }
}
