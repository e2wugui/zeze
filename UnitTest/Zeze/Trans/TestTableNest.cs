using System;
using System.Collections.Generic;
using System.Text;
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
        [TestMethod]
        public void TestNest()
        {
            Assert.IsTrue(Procedure.Success == new Procedure(ProcTableRemove).Call());
            Assert.IsTrue(Procedure.Success == new Procedure(ProcTableAdd).Call());
        }

        int ProcTableRemove()
        {
            demo.App.Instance.demo_Module1_Module1.Table1.Remove(4321);
            return Procedure.Success;
        }

        int ProcTableAdd()
        {
            demo.Module1.Value v1 = demo.App.Instance.demo_Module1_Module1.Table1.GetOrAdd(4321);
            Assert.IsNotNull(v1);
            Assert.IsTrue(Procedure.Success != new Procedure(ProcTablePutNestAndRollback).Call());
            demo.Module1.Value v2 = demo.App.Instance.demo_Module1_Module1.Table1.Get(4321);
            Assert.IsNotNull(v1);
            Assert.IsTrue(v1 == v2);
            return Procedure.Success;
        }

        int ProcTablePutNestAndRollback()
        {
            demo.Module1.Value v = new demo.Module1.Value();
            demo.App.Instance.demo_Module1_Module1.Table1.Put(4321, v);
            return Procedure.Unknown;
        }
    }
}
