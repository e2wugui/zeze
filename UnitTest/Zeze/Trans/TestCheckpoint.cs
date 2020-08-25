using System;
using System.Collections.Generic;
using System.Text;
using Microsoft.VisualStudio.TestTools.UnitTesting;
using Zeze.Serialize;
using Zeze.Transaction;

namespace UnitTest.Zeze.Trans
{
    [TestClass]
    public class TestCheckpoint
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
        public void TestCp()
        {
            Assert.IsTrue(new Procedure(ProcClear).Call() == Procedure.Success);
            Assert.IsTrue(new Procedure(ProcChange).Call() == Procedure.Success);
            demo.App.Instance.Zeze.Checkpoint();
            demo.Module1.Table1 table = demo.App.Instance.demo_Module1_Module1.Table1;
            ByteBuffer value = table.Storage.DatabaseTable.Find(table.EncodeKey(56));
            Assert.IsNotNull(value);
            Assert.AreEqual(value, bytesInTrans);
        }

        int ProcClear()
        {
            demo.App.Instance.demo_Module1_Module1.Table1.Remove(56);
            return Procedure.Success;
        }

        ByteBuffer bytesInTrans;
        int ProcChange()
        {
            demo.Module1.Value v = demo.App.Instance.demo_Module1_Module1.Table1.GetOrAdd(56);
            v.Int1 = 1;
            bytesInTrans = ByteBuffer.Allocate();
            v.Encode(bytesInTrans);
            return Procedure.Success;
        }
    }
}
