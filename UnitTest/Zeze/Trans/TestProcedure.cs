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

        public Task<long> ProcTrue()
        {
            bean.I = 123;
            Assert.AreEqual(bean.I, 123);
            return Task.FromResult(Procedure.Success);
        }

        public Task<long> ProcFalse()
        {
            bean.I = 456;
            Assert.AreEqual(bean.I, 456);
            return Task.FromResult(Procedure.Unknown);
        }

        public async Task<long> ProcNest()
        {
            Assert.AreEqual(bean.I, 0);
            bean.I = 1;
            Assert.AreEqual(bean.I, 1);
            {
                var r = await demo.App.Instance.Zeze.NewProcedure(ProcFalse, "ProcFalse").CallAsync();
                Assert.IsTrue(r != Procedure.Success);
                Assert.AreEqual(bean.I, 1);
            }

            {
                var r = await demo.App.Instance.Zeze.NewProcedure(ProcTrue, "ProcFalse").CallAsync();
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
            var root = new TableKey(1, 1);
            // 特殊测试，拼凑一个record用来提供需要的信息。
            var table = new MyTable();
            var r = new Record<long, TestBegin.MyBean>(table, 1, bean);
            bean.InitRootInfo(r.CreateRootInfoIfNeed(root), null);
            var rc = demo.App.Instance.Zeze.NewProcedure(ProcNest, "ProcNest").CallSynchronously();
            Assert.IsTrue(rc == Procedure.Success);
            // 最后一个 Call，事务外，bean 已经没法访问事务支持的属性了。直接访问内部变量。
            Assert.AreEqual(bean._i, 123);
        }

        public class MyTable : Table<long, TestBegin.MyBean>
        {
            public MyTable() : base("MyTable_1232")
            {
            }

            public override long DecodeKey(ByteBuffer bb)
            {
                return bb.ReadLong();
            }

            public override ByteBuffer EncodeKey(long key)
            {
                var bb = ByteBuffer.Allocate(16);
                bb.WriteLong(key);
                return bb;
            }
        }
    }
}
