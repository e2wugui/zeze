﻿using System;
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
            Assert.IsTrue(Procedure.Success == new Procedure(ProcRemove).Call());
            Task[] tasks = new Task[1000];
            for (int i = 0; i < tasks.Length; ++i)
            {
                tasks[i] = Task.Run(new Procedure(ProcAdd).Call);
            }
            Task.WaitAll(tasks);
            sum = tasks.Length;
            Assert.IsTrue(Procedure.Success == new Procedure(ProcVerify).Call());
            Assert.IsTrue(Procedure.Success == new Procedure(ProcRemove).Call());
        }

        int ProcRemove()
        {
            demo.App.Instance.demo_Module1_Module1.Table1.Remove(123123);
            return Procedure.Success;
        }

        int ProcAdd()
        {
            demo.Module1.Value v = demo.App.Instance.demo_Module1_Module1.Table1.GetOrAdd(123123);
            v.Int1 += 1;
            return Procedure.Success;
        }

        int ProcVerify()
        {
            demo.Module1.Value v = demo.App.Instance.demo_Module1_Module1.Table1.GetOrAdd(123123);
            Assert.IsTrue(v.Int1 == sum);
            return Procedure.Success;
        }
    }
}
